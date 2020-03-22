# Мониторинг жизненного цикла и Error Kernel

Единственное наблюдаемое снаружи поведение происходит когда актор останавливается - ответов больше не приходит. Однако, сложно понять - ошибки коммуникации это или актор действительно исчез.

Akka поддерживает мониторинг жизненного цикла - DeathWatch:

- Актор запрашивает мониторинг другого актора через `context.watch(target)`;
- Он получит сообщение `Terminated(target)` когда таргет стопнет;
- Никаких сообщений от target от использования `watch` не придёт.

Есть обратный метод `unwatch`.

Даже если `unwatch` вызван когда в очереди уже есть Terminated - он не будет доставлен.

Сообщение имеет два флага: `Terminated(target)(existenceConfirmed, addressTerminated)`. Первый флаг говорит о том, что система может подтвердить существование таргета (актор когда-либо существовал). Второй флаг говорит о том, что это сообщение было создано системой (система не смогла найти актор, первый флаг false).

`Terminated` расширяет два класса:

- `AutoReceiveMessage` - сигналирует о том, что удаленные сообщения хендлятся контекстом. Это то, что позволяет не доставлять сообщения после unwatch;
- **`PossiblyHarmful`** - посмотрим потом.

## Снова LinkChecker

Дополним Getter с DeathWatch.

```scala
  override def receive: Receive = {  // Сюда результат прилетит после отправки
    case "ACTOR FAKE WEB CLIENT GENERATED FAILURE" => stop()  // специальная ошибка-костыль
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
```

Раньше мы использовали метод stop и Done для того, чтобы уведомить о завершении работы. Это можно убрать:

```scala
 override def receive: Receive = {  // Сюда результат прилетит после отправки
    case "ACTOR FAKE WEB CLIENT GENERATED FAILURE" => context.stop(self) // специальная ошибка-костыль
    case body: String =>
      for (link <- AsyncWebClient.findLinks(body))
        context.parent ! Controller.Check(link, depth)  // вернуть результат родителю
      context.stop(self)
    case Status.Failure => context.stop(self)
    case Abort => context.stop(self)
  }
```

При использовании DeathWatch вместо Done мы будем получать Terminated сообщение.


# Children List

Контекст хранит список всех детей актора:

```scala
trait ActorContext {
  def children: Iterable[ActorRef]
  def child(name: String): Option[ActorRef]
}
```

- child кладётся в коллекцию при возврате `context.actorOf`;
- убирается при получении `Terminated` для этого актора (даже если нет DeathWatch - это произойдёт);
- Одинаковых имён у двух детей быть не может.

Для чего конкретно нужен `context.watch`:

**Только для получения Terminated в качестве сообщения. Из child актор уйдёт сам по себе в любом случае!**

Используя эти знания удалим массив детей, который писали вручную и добавим DeathWatch:

```scala
class Controller extends Actor with ActorLogging {
  import Controller._
  import Getter._

  var cache = Set.empty[String]  // результат - набор ссылок
  //var children = Set.empty[ActorRef]  // Теперь пользуемся context-ом

  context.setReceiveTimeout(10.seconds)  // если истёк - шлёт сам себе ReceiveTimeout
  // обнуляется после каждого сообщения

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 5) {
      case _: Exception => SupervisorStrategy.Restart
    }

  def receive: Receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0)
        context.watch(context.actorOf(Props(new Getter(url, depth - 1))))  // создали геттер через DeathWatch
      cache += url  // считаем, что URL был пройден и записываем его
    case Terminated(_) =>
      if (context.children.isEmpty) context.parent ! Result(cache)  // если все Getter-ы сделали Done - конец
    case ReceiveTimeout => context.children foreach context.stop
  }
}
```

Перепишем тесты:

```scala
{
// Фейковый веб-клиент и т.д.
...

  class StepParent(child: Props, probe: ActorRef) extends Actor {
    // watch чтобы получить Terminated как сообещние
    context.watch(context.actorOf(child, "child")) // создание child
    def receive: Receive = {
      case Terminated(_) =>
        context.stop(self)
      case msg =>
        probe.tell(msg, sender) // пересылка с сохранением первичного отправителя
    }
  }

}

/** Тестирующий зонд */

  "A Getter" must {
    "return the right body" in {
      val getter = watch(system.actorOf(  // watch нужен на всей цепочке
        Props(new StepParent(fakeGetter(firstLink, 2), testActor)),
        "rightBody"
      ))

      for (link <- links(firstLink))
        expectMsg(Controller.Check(link, 2))
      //expectMsg(Getter.Done())  // теперь Done не приходит, можно только через terminated
      expectTerminated(getter)
    }

    "properly finish in case of errors" in {
      val getter = watch(system.actorOf(
        Props(new StepParent(fakeGetter("unknown", 2), testActor)),
        "wrongLink"
      ))
      //expectMsg(Getter.Done())
      expectTerminated(getter)
    }
  }

```

Как видно, watch нужно использовать на протяжении всей цепи акторов.

## Мониторинг жизненного цикла для обработки ошибок

```scala
class Manager extends Actor {


  def prime(): Receive = {
    val db = context.actorOf(Props[DBActor], "db")
    context.watch(db)

    {
      case Terminated("db") => context.become(backup())
    }
  }

  def backup(): Receive = { ... }
  def receive = prime()
}
```

Вот так watch можно использовать для восстановления после ошибок.

## The Error Kernel

Дети актора и их состояние - это состояние родителя. Это означает, что при перезапуске актора с детьми - они тоже должны быть перезапущены. Это называется *рекурсивный перезапуск*.

Из этого следует, что перезапускаться должны как можно более нижние листы (у которых нет детей). 

Чтобы не терять данные - нужно пользоваться определённым принципом. Чем важнее данные - тем выше в иерархии они должны располагаться. Самые нижние акторы должны быть чисто воркерами, не сохраняющими никаких важных данных. Это примитивные расходники которые можно перезапускать.

С другой стороны, рискованные задачи должны делаться на более низких уровнях, чтобы рестарт был не очень страшен.

Иерархия в Akka Actors необходима - акторы могут быть созданы только из других акторов. Поэтому мы интуитивно придерживаемся иерархической структуры.


## Дополнения к Receptionist

Receptionist является супервизором иерархии:

- Останавливает Controller если с ним проблемы;
- Реагирует на Terminated чтобы определять кейзы, когда не было получено Result;
- Отбрасывает Terminated после отправки Result.

Сложный получатель ссылок Getter находится на дне и его не жалко перезапускать.


Добавим функционал в Receptionist:

```scala

class Receptionist extends Actor {
  import Receptionist._

  /** На каждый Failure любого ребенка высылает ему Stop */
  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy

  
  def runNext(queue: Vector[Job]): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting  
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")  
      /** Следим за ребёнком */
      context.watch(controller)
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }


  def running(queue: Vector[Job]): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(context.unwatch(sender))  // остонавливаем контроллер и перестаём следить
      context.become(runNext(queue.tail))

    /** Если ребёнок выслал Terminated - уведомляем и скипаем на следующего */
    case Terminated(_) =>
      val job = queue.head
      job.client ! Failed(job.url)
      context.become(runNext(queue.tail))
    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }
}
```




