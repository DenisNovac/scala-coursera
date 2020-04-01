# Протоколы стримов, композиция стримов

Протоколы делятся на:

- Потоковые (TCP);
- На сообщениях (UDP, Акторы).

Большинство протоколов в интернете - это потоковые протоколы. 

## TCP в Akka Streams

Реактивные стримы и Akka Streams основаны на сообщениях. Из стрима можно получать по одному элементу, семантика запроса построена на сообщениях.

Но всё равно возможно комбинировать TCP и Akka Streams. В комбинации с механизмом фреймов стримы становится удобным способом работать с сырыми TCP-соединениями в приложениях.

Простейший TCP эхо-сервер и клиент:

```scala
import akka.actor._
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.Future
import scala.concurrent.duration._


object Server extends App {
  implicit val system = ActorSystem("tcp-echo")

  val echoLogic = Flow[ByteString].map { data => 
    println(s"From client: $data")
    data
  }
  
  Tcp(system).bindAndHandle(echoLogic, "127.0.0.1", 1337)

}


object Client extends App {
  implicit val system = ActorSystem("client")

  val clientFlow: Flow[ByteString,ByteString,Future[Tcp.OutgoingConnection]]
    = Tcp().outgoingConnection("127.0.0.1", 1337)

  val localDataSource = 
    Source.repeat(ByteString("Hello!"))
      .throttle(1, per = 1.second)

  val localDataSink = 
    Sink.foreach[ByteString](data => println(s"From server: $data"))

  localDataSource.via(clientFlow).to(localDataSink).run
}
```

`Tcp` - расширение, предоставляемое Akka.

Поток является логикой данного сервера. Это работает так. TCP-сервер принимает что-то, отдаёт в echoLogic и забирает аутпут. Без модификаций стандартный `Flow` становится echo-потоком.

Tcp-соединение становится обычным объектом Flow в этой семантике.

Наконец, необходимо сделать `run` для Flow. Он обязан иметь Source и Sink, поэтому мы определили Sink, который просто печатает.

Что происходит здесь:

```scala
val clientFlow: Flow[
  ByteString,  // input type
  ByteString,  // output type
  Future[Tcp.OutgoingConnection]  // тип материализованного значения
  ]
```

Материализованное значение - это материализованный тип обработчика соединения. Материализованные значения позволяют передавать информацию во внешний мир когда материализованн.

Мы можем вписать туда логирование:

```scala
implicit val ec = ExecutionContext.global  // Для разворота Future

val clientFlow: Flow[ByteString,ByteString,Future[Tcp.OutgoingConnection]]
  = Tcp()
    .outgoingConnection("127.0.0.1", 1337)
    .mapMaterializedValue(_.map { connection =>
      println(s"Connection established; " +
      s" local address ${connection.localAddress}, " +
      s" remote: ${connection.remoteAddress}"
      )
      connection
    })
```

Пример работы:

```log
INFO  Connection established;  local address /127.0.0.1:59916,  remote: 127.0.0.1:1337
INFO  From client: ByteString(72, 101, 108, 108, 111, 33)
INFO  From server: ByteString(72, 101, 108, 108, 111, 33)
INFO  From client: ByteString(72, 101, 108, 108, 111, 33)
```

## Композиция стримов

Реактивные стримы композируемы. Это свойство полезно в тестах.

Напишем простейший тест.

```scala
/**
  * Вынес отдельно для тестирования
  */
object EchoLogic {
  def get =  Flow[ByteString]
    .map { data => 
      println(s"From client: $data")
      data
    }
}

class TcpSuite extends AnyFlatSpec {

  implicit val ac = ActorSystem()
  implicit val ec = ExecutionContext.global

  "server" should "echo a single value" in {
    val source = Source.single("hello").map(ByteString(_))
    val sink = Sink.seq[ByteString]

    
    val futureSeq: Future[Seq[ByteString]] = source.via(EchoLogic.get).runWith(sink)

    
    val r: Seq[ByteString] = Await.result(futureSeq, 10.seconds)
    assert(r == Seq(ByteString("hello")))
  }
}
```

Здесь мы не хотим никакого TCP, просто протестировать саму логику. Наша логика является Flow, поэтому её можно приконнектить к любой последовательности Source-Flow-Sink (с подходящими типами). В тесте мы коннектим его к Sink-у, который просто собирает элементы в коллекцию.

Мы можем тестировать и клиент:

```scala
def run(connection: Flow[ByteString, ByteString, _]) = // Какая-то логика клиента

run(Tcp().outgoingConnection("127.0.0.1", 1337))  // реальный TCP

run(Flow[ByteString]) // передали в run локальный поток вместо TCP
```


