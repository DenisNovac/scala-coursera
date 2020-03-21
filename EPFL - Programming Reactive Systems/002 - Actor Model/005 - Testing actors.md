# Тестирование акторов

Основной смысл тестов в том, что они проверяют эффекты, видимые снаружи.

Акторы позволяют только взаимодействия через сообщения.

Пример актора:

```scala
class Toggle extends Actor {
  override def receive: Receive = happy

  def happy: Receive = {
    case "How are you?" =>
      sender ! "happy"
      context become sad
  }


  def sad: Receive = {
    case "How are you?" =>
    sender ! "sad"
    context become happy
  }

}
```

Пример теста для него:

```scala
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}

import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ToggleSuit extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  /**
    * Классический тест
    * */
  def testToggle(): Unit = {
    implicit val system: ActorSystem = ActorSystem("TestSys")
    val toggle = system.actorOf(Props[Toggle])
    val p = TestProbe() // внутри является актором

    p.send(toggle, "How are you?")
    p.expectMsg("happy")

    p.send(toggle, "How are you?")
    p.expectMsg("sad")

    p.send(toggle, "unknown")
    p.expectNoMessage(1.seconds)

    /** Выключение системы даёт фьючу */
    Await.result(system.terminate(), 10.second)
  }


  "An Toggle actor" must {
    "toggle between messages" in {
      testToggle()
    }
  }

  /**
    * Можно не создавать TestProbe, а использовать TestKit, который уже имеет его
    * */
  def testWithinAProbe(): Unit = {
    new TestKit(ActorSystem("TestSys")) with ImplicitSender {  // оно красное но работает
      val toggle: ActorRef = system.actorOf(Props[Toggle])
      toggle ! "How are you?" // имя внутреннего актора - testActor
      expectMsg("happy")
      toggle ! "How are you?"
      expectMsg("sad")
      toggle ! "Unknown"
      expectNoMessage(1.seconds)

      Await.result(system.terminate(), 10.second)
    }
  }


  "An Toggle actor" must {
    "toggle between messages while being tested within a probe" in {
      testWithinAProbe()
    }
  }
}
```

Можно переписать эти тесты. Станет не так очевидна структура, зато куда чище:

```scala
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}

import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FancyToggleSuit
    extends TestKit(ActorSystem("TestSys"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  // Тестируемый актор
  val toggle: ActorRef = system.actorOf(Props[Toggle])

  // после всех тестов выключиться
  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An Toggle actor" must {
    "toggle between messages" in {

      toggle ! "How are you?"
      expectMsg("happy")
      toggle ! "How are you?"
      expectMsg("sad")
      toggle ! "Unknown"
      expectNoMessage(1.seconds)
    }
  }
}

```

## Тестирование акторов со внешними зависимостями

Некоторые акторы могут иметь внешние зависимости. Например, базу данных или веб-сервис Когда мы хотим тестировать доступ к БД или серверам - использовать реальные нельзя. Традиционное решение - использовать DI для переключения хендлеров. 

Решение попроще - добавить оверрайдабл фабричные методы.

Например, посмотрим на Receptionist:

```scala

class Receptionist extends Actor {
  def controllerProps: Props = Props[Controller]

  def receive = {
    val controller = context.actorOf(controllerProps, "controller")
  }
}
```

Он создаёт контроллер. И если мы захардкодим этот контроллер - мы не сможем тестировать его. Но если мы добавим `def controllerProps`, мы сможем переписать этот метод во время тестирования. Тогда он создаст другой актор, который не ходит в БД или в веб.

```scala
class Getter extends Actor {
  def client: WebClient = AsyncWebClient
  client get url pipeTo self
}
```

Другой пример. Тут можно заменить клиент на фейковый в оверрайде.

## Тестирование Link Checker

Напишем тесты для предыдущего задания.

Добавим к нашему асинхронному клиенту WebClient интерфейс, а сам клиент переименуем в AsyncWebClient:

```scala
trait WebClient {
  def get(url: String)(implicit exec: Executor): Future[String]
}

case class BadStatus(i: Int) extends Throwable

object AsyncWebClient extends WebClient {

  private val client = new AsyncHttpClient

  override def get(url: String)(implicit exec: Executor): Future[String] = {
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


  def findLinks(body: String): Iterator[String] = {
    val document = Jsoup.parse(body)
    val links = document.select("a[href]")
    for {
      link <- links.iterator().asScala
    } yield link.absUrl("href")
  }
}
```

Измени класс Getter и добавим туда метод, позволяющий оверрайдить клиент:

```scala

class Getter(url: String, depth: Int) extends Actor {
  import Getter._


  // Имплисивные контекст и экзекьютор, необходимые для Future
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  // Этот метод можно переписать в тесте для смены клиента
  def client: WebClient = AsyncWebClient

  // клиент используется здесь
  val future: Future[String] = client.get(url)

  // дальше без изменений
  
  
  future.pipeTo(self)  // Отправить самому себе результат фьючи

  override def receive: Receive = {  // Сюда результат прилетит после отправки
    case body: String =>
      for (link <- AsyncWebClient.findLinks(body))
        context.parent ! Controller.Check(link, depth)  // вернуть результат родителю
      stop()
    case Status.Failure => stop()
    case Abort => stop()
  }

  def stop(): Unit = {
    context.parent ! Done()
    context.stop(self)
  }
}
```

Напишем фейковый клиент, который не ходит на сайты:

```scala
class GetterSuit {
  val firstLink = "http://www.rkuhn.info/1"

  val bodies = Map(
    firstLink ->
      """<html>
        |  <head><title>Page 1</title></head>
        |  <body>
        |    <h1>A Link</h1>
        |  </body>
        |</html>""".stripMargin
  )

  val links = Map(firstLink -> Seq("http://rkuhn.info/2"))

  object FakeWebClient extends WebClient {
    override def get(url: String)(implicit exec: Executor): Future[String] =
      bodies.get(url) match {
        case None       => Future.failed(BadStatus(404))
        case Some(body) => Future.successful(body)
      }
  }
}
```

Вот так будет создаваться геттер с фейковым клиентом:

```scala
  /**
    * Создание актора с фейковым клиентом
    */
  def fakeGetter(url: String, depth: Int): Props =
    Props(new Getter(url, depth) {
      override def client: WebClient = FakeWebClient
    })
```


### Тестирование взаимосвязей

Getter возвращает результаты родителю. Это нужно учесть в тестах и создать особый актор, который возвращает ссылки родителю (который уже будет тестовым зондом):

```scala
  /**
    * Специальный псевдо-родитель для Getter, возвращающий всё в TestProbe
    */
  class StepParent(child: Props, probe: ActorRef) extends Actor {
    context.actorOf(child, "child") // создание child
    def receive: Receive = {
      case msg =>
        probe.tell(msg, sender) // пересылка с сохранением первичного отправителя
    }
  }
```


### Тестовый класс

```scala
class GetterSpec
  extends TestKit(ActorSystem("TestSys"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import app.GetterSuit._

  override def afterAll(): Unit = system.terminate()

  "A Getter" must {
    "return the right body" in {
      val getter = system.actorOf(
        Props(new StepParent(fakeGetter(firstLink, 2), testActor)),
        "rightBody"
      )

      for (link <- links(firstLink))
        expectMsg(Controller.Check(link, 2))
      expectMsg(Getter.Done())
    }
  }
}
```

Таким образом мы протестировали Getter изолированно от других акторов.

