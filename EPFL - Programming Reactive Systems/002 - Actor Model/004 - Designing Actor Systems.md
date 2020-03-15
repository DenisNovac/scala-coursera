# Разработка систем акторов

Мы увидели, как акторы взаимодействуют и передают сообщения. Но как разработать акторную систему?

Пример приложения: программа переходит по URL, рекурсивно скачивает документ, извлекает ссылки, идёт по ним до какой-то глубины (кроме ссылок, по которым уже ходил). Все использованные ссылки нужно вернуть.

Как бы мы сделали это через группу людей?

У нас было бы несколько должностей:

- Receptionist (принимает запросы, но сам ничего не делает);
- Client (просит что-то);
- Controller (запоминает, куда уже ходил и куда нужно, но сам не ходит);
- Getter (переходит по ссылкам, извлекает все ссылки из документа и возвращает их контроллеру).

Controller создаёт множество геттеров на каждую ссылку в момент, когда он получает ссылки. Getter возвращает ссылку как только извлекает её. Всё крутится асинхронно - пока геттер обрабатывает прошлый документ, следующий геттер уже обрабатывает первую ссылку из него.

Getter возвращает множество ссылок и сообщение Done, когда ссылки кончились.

Глубину лучше всего возвращать вместе с сообщениями, а не хранить в контроллере.

План действий:

- Подключить веб-клиент, который асинхронно принимает URL, а возвращает HTTP-тело;
- Написать `Getter` для обработки тела;
- Написать `Controller`, генерирующий Getter для всех встреченных ссылок;
- Написать `Receptionist`, создающий один `Contoller` на реквест.

## Web-client

Из документации async-http-client:

```scala
val client = new AsyncHttpClient
def get(url: String): String = {
  val response = client.prepareGet(url).execute().get  // Блокирование пока ресурс отдет тело
  if (response.getStatusCode < 400)
    response.getResponseBodyExcerpt(131072)  // Читаем первые 128 МБ тела и возвращаем
  else throw BadStatus(response.getStatusCode)
}
```

Блокирование в этой ситуации создаёт несколько проблем:

- Актор не может ничего делать (даже отменять действия);
- Блокирование съедает один тред.

Клиент называется AsyncHttpClient, поэтому он явно умеет делать асинхронно. И это действительно так.

Строка `client.prepareGet(url).execute()` возвращает Future. Можно вручную запускать её в Runnable вручную:


```scala
import java.util.concurrent.Executor
import com.ning.http.client.AsyncHttpClient
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._

case class BadStatus(i: Int) extends Throwable

val client = new AsyncHttpClient

def get(url: String)(implicit exec: Executor): Future[String] = {
  val f = client.prepareGet(url).execute()  // ListenableFuture
  val p = Promise[String]()  // для заполнения результатом асинхронного листенера
  f.addListener(new Runnable {
    override def run(): Unit = {
      val response = f.get
      if (response.getStatusCode < 400)
        p.success(response.getResponseBodyExcerpt(131072))  // заполнили promise
      else p.failure(BadStatus(response.getStatusCode))
    }
  }, exec)
  p.future  // Из Promise можно получить Future типа Promise
}


implicit val exec: Executor = (command: Runnable) => command.run()

val body = Await.result(get("https://en.wikipedia.org/wiki/Actor"), 30.seconds)
```

`Promise` позволяет обернуть асинхронный запрос, возвращающий Unit (`run` у нас). `ListenableFuture` из библиотеки AsyncHttpClient позволяет написать метод `addListener`. 

Эта библиотека написана на Java и юзает свои Future. Мы смаппили их к Scala Future.

Чему мы научились:

- Реактивное приложение неблокирующее от начала до конца, любой блокер заблокирует все остальные действия.


## Finding Links

Теперь нужно извлечь URL-ы из тела. Для этого мы используем Java-библиотеку Jsoup.

```scala
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._

def findLinks(body: String): Iterator[String] = {
  val document = Jsoup.parse(body)
  val links = document.select("a[href]")
  for {
    link <- links.iterator().asScala
  } yield link.absUrl("href")
}
```

Теперь можно написать первый актор.

## Getter Actor

```scala
class Getter(url: String, depth: Int) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher
  
  val future: Future[String] = WebClient.get(url)
  
  future onComplete {
    case Success(body) => self
    case Failure(err) => self ! Status.Failure(err)
  }
  

}
```

Интересно, что `implicit val exec: ExecutionContextExecutor = context.dispatcher` содержит контексты для акторов, но они умеют работать и с Scala Future, поэтому мы не дополняем работу с Future импортами специальных контекстов.

Разбор фьючи настолько часто используется, что из него сделали специальный метод:

```scala
// было
  
future onComplete {
  case Success(body) => self
  case Failure(err) => self ! Status.Failure(err)
}

// стало
import akka.pattern.pipe

future.pipeTo(self)  // Отправить самому себе результат фьючи
```

Допишем остальной геттер:

```scala
class Getter(url: String, depth: Int) extends Actor {
  override def receive: Receive = {  // Сюда результат прилетит после отправки
    case body: String =>
      for (link <- WebClient.findLinks(body))
        context.parent ! Controller.Check(link, depth)  // вернуть результат родителю
      stop()
    case Status.Failure => stop()
    case Abort => stop()  // для отмены
  }

  def stop(): Unit = {
    context.parent ! Done
    context.stop(self)
  }
}
```

Мы можем получить body или failure из `.pipeTo(self)`, ведь она как бы отправляет самому себе сообщение.

`context.parent` - вызов адреса родителя. Исходя из концепции программы, мы понимаем, что родителем Getter-а всегда будет Controller, который и требует результат чтения страницы.

Здесь мы научились:

- Использовать dispatcher у акторов, который подходит и для Future;

## Controller Actor

Мы бы хотели логировать такой актор. И Akka умеет это делать через трейт ActorLogging:

```scala
class A extends Actor with ActorLogging {
  def receive = {
    case msg => log.debug("received message: {}", msg)
  }
}
```

Сама по себе запись куда-то - это блокирующая операция, поэтому мы бы хотели отправить лог актору, который логирует. Уровень логов выставляется через `akka.loglevel=DEBUG`.

```scala
class Controller extends Actor with ActorLogging {
  var cache = Set.empty[String]  // результат - набор ссылок
  var children = Set.empty[ActorRef]  // все созданные Getter-ы (по одному на ссылку)

  def receive: Receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0)
        children += context.actorOf(Props(new Getter(url, depth - 1)))  // создали геттер
      cache += url  // считаем, что URL был пройден и записываем его
    case Getter.Done =>
      children -= sender
      if (children.isEmpty) context.parent ! Result(cache)  // если все Getter-ы сделали Done - конец
  }
}

```

Мы используем var с иммутабельной структурой данных. Иммутабельные структуры можно легко делить между объектами. Постоянная ссылка val на мутабельную структуру может вызвать нежелательные последствия. 

## Timeout

Controller и Getter хорошо работают, но что, если WebClient перестанет отвечать? Например, будет слишком долго ждать ответа от сайта? Нужен таймаут.

```scala
class Controller extends Actor with ActorLogging {
  var cache = Set.empty[String]  // результат - набор ссылок
  var children = Set.empty[ActorRef]  // все созданные Getter-ы (по одному на ссылку)

  context.setReceiveTimeout(10.seconds)

  def receive: Receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0)
        children += context.actorOf(Props(new Getter(url, depth - 1)))  // создали геттер
      cache += url  // считаем, что URL был пройден и записываем его
    case Getter.Done =>
      children -= sender
      if (children.isEmpty) context.parent ! Result(cache)  // если все Getter-ы сделали Done - конец
    case ReceiveTimeout => children foreach (_ ! Getter.Abort)
  }
}
```

Таймаут обновляется после прихода сообщений. Когда он истекает - он шлёт себе сообщение ReceiveTimeout (это тоже перезапускает таймаут).

### Schedule Once

Или мы можем отсчитывать таймаут не по приходу сообщений, а по началу работы контроллера через scheduleOnce:

```scala
context.system.scheduler.scheduleOnce(10.seconds) {
  children foreach (_ ! Getter.Abort)
}
```

Раз в десять секунд будет запускаться этот блок. Это не очень хороший подход - он не тредсейфовый. 

Этот код не запускается актором и не управляем сообщениями. Он в другом контексте - в контексте менеджера расписаний.

Но есть специальная сигнатура, позволяющая запускать шедулер в контексте:

```scala
implicit private val ec = context.dispatcher
context.system.scheduler.scheduleOnce(10.seconds, self, ReceiveTimeout)
```

Эта сигнатура шлёт сообщение себе и сильно напоминает обычный setReceiveTimeout.


Нельзя обращаться к состоянию акторов из асинхронного кода (шедулер) или из внутренних Future - они могут не поделить мутабельный стейт между собой.


## Receptionist Actor

```scala
class Receptionist extends Actor {
  def receive = waiting

  val waiting: Receive = {
    // на Get(url) запуск
  }

  def running(queue: Vector[Job]): Receive = {
    // Get(url) - добавить к очереди и продолжить работу
    // на Controller.Result(links) вернуть результат клиенту и запустить следующую очередь
  }
}

```

Этот актор должен удостовериться, что только одна работа с сетью идёт за раз. Поэтому у него может быть два состояния - в данный момент работы нет и он готов запуститься (waiting) или работа идёт и он должен ждать завершения (running). Переключения контекстов удобно вынести в отдельные методы.

Итоговый класс:

```scala
class Receptionist extends Actor {
  import Receptionist._

  def receive = waiting

  /** Ожидание запускается при старте актора или при завершении работ */
  val waiting: Receive = {
    case Get(url) => context.become(runNext(Vector(Job(sender, url))))
  }

  def running(queue: Vector[Job]): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(sender)  // остонавливаем контроллер, ведь для следующей ссылки мы сделаем нового
      context.become(runNext(queue.tail))
    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }


  case class Job(client: ActorRef, url: String)
  var reqNo = 0


  /** Запустить следующую работу */
  def runNext(queue: Vector[Job]): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting  // если очередь пустая - ждём
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")  // имя актора - контроллер с номером реквеста
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  /** Зарегистрировать новую работу */
  def enqueueJob(queue: Vector[Job], job: Job): Receive = {
    if (queue.size > 3) {  // лимитируем работы тремя, если пришла новая - фейлим её
      sender ! Failed(job.url)
      running(queue)
    } else running(queue :+ job)
  }
}
```

- Следует использовать context.become для передачи стейтов между поведениями;
- Внутри актора нельзя использовать асинхронный код, как-то обращающийся к его же состоянию - каждый актор должен быть единственной асинхронной единицей, работающей со своим состоянием.

