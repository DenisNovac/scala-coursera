# Интеграция стримов и типизированных акторов

Всё описанное будет работать и для обычных акторов, но в курсе затрагивались типизированные.

Хотя многие вещи можно описать через операции стримов (или кастомные GraphStages), иногда хочется интегрироваться с акторами. Акторы могут быть источниками или стоками.

Можно конечно использовать стандартный ! напрямую, но тогда будут потеряны возможности Back-pressure. А если мы хотим иметь этот механизм - нужно делать дополнительные вещи. 


## Пример - типизированный актор как Source

```scala
val ref: ActorRef[String] = 
  ActorSource.actorRef[String] (
    completionMatcher = { case "complete" => },
    failureMatcher = PartialFunction.empty,
    bufferSize = 256,
    OverflowStrategy.dropNew
  )
  .to(Sink.ignore).run()


ref ! "one"
ref ! "two"
ref ! "complete"
```

## Альтернативы для создания источника из внешних данных

`Source.queue[T]` материализует `SourceQueueWithComplete[T]`, которую можно использовать для предложения элементов в стрим. Это удобно использовать и изнутри акторов.

```scala
val queue: SourceQueueWithComplete[Int] =
  Source.queue[Int](bufferSize = 1024, OverflowStrategy.dropBuffer)
  .to(Sink.ignore)
  .run()

val r1: Future[QueueOfferResult] = queue.offer(1)
```

## Пример - типизированный актор как Sink

```scala
val ref = spawn(Behaviors.receiveMessage[String] {
  case msg if msg.startsWith("FAILED: ") => 
    throw new Exception(s"Stream failed: $msg")
  case "DONE" =>
    Behaviors.stopped
  case msg =>
    Behaviors.same
})

Source(1 to 10).map(_ + "!")
  to(ActorSink.actorRef(
    ref - ref, onCompleteMessage = "DONE",
    onFailureMessage = ex => "FAILED: " + ex.getMessage)
  ).run()
```

Простой актор-приёмник сообщений, который используется как Sink.

По сути это аналог `foreach{m => ref ! m}` (но с onCompleteMessage и onFailureMessage), опять же никакого back-pressure. Мейлбокс актора может переполниться.

## Актор как Sink с back-pressure

Работа с back-pressure возможна и в акторах, но требует от акторов дополнительного функционала. Нужно описать протокол, поддерживающий back-pressure.

Сигнал ACK будет записан как Ack. По сути это запрос для следующего сообщения:

```scala
sealed trait Ack
case object Ack extends Ack
```

Нужен дополнительный протокол для получения элементов из стрима:

```scala
sealed trait AckProtocol

case class Init(streamSender: ActorRef[Ack]) extends AckProtocol
case class Msg(streamSender: ActorRef[Ack], msg: String) extends AckProtocol
case object Complete extends AckProtocol
case object Failed(ex: Throwable) extends AckProtocol
```

Поведение, работающее с сообщениями:

```scala
val pilot: Behaviors.Receive[AckProtocol] =
  Behaviors.receiveMessage[AckProtocol] {

    case m @ Init(sender) =>
      sender ! Ack
      Behaviors.same

    case m @ Msg(sender, _) =>
      sender ! Ack
      Behaviors.same

    case m  =>
      // Специально игнорим остальные типы
      Behaviors.ignore
  }



val targetRef: ActorRef[AckProtocol] = spawn(pilot)  // spawn из TestKit
val source: Source[String, NotUsed] = Source.single("")

val in: NotUsed = 
  source
    .runWith(ActorSink.actorRefWithAck(
      ref = targetRef,
      // Конвертируем сообщения стрима в понятные актору
      messageAdapter    = Msg(_, _),  
      onInitMessage     = Init(_),  
      ackMessage        = Ack,  // Теперь стрим знает, как выглядит запрос от актора
      onCompleteMessage = Complete,
      onFailureMessage  = Failed(_))
    )
```

Самое главное отличие - мы передаём ссылку новым методом `ActorSink.actorRefWithAck`. Он позволяет задать, какой тип сообщения является запросом следующей порции данных из стрима. 

## Asking Actors in a Flow

В потоках бывает удобно выполнить Ask какого-нибудь актора. 

```scala
// эхо-актор
val replier = spawn(Behaviors.receiveMessage[Asking]{
  case asking =>
    asking.replyTo ! Reply(asking.s + "!!!")
    Behaviors.same
})

val in: Future[immutable.Seq[Reply]] = 
  Source.repeat("hello")
    .via(
      ActorFlow.ask(replier)(
        (el, replyTo: ActorRef[Reply]) => Asking(el, replyTo)
      )
    )
    .take(3) // : Source[Reply, _]
```