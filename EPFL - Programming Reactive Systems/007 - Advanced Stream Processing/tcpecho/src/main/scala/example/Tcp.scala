package example

import akka.actor._
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

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


object Server extends App {
  implicit val system = ActorSystem("tcp-echo")

  val echoLogic = EchoLogic.get
  
  Tcp(system).bindAndHandle(echoLogic, "127.0.0.1", 1337)

}


object Client extends App {
  implicit val system = ActorSystem("client")
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

  val localDataSource = 
    Source.repeat(ByteString("Hello!"))
      .throttle(1, per = 1.second)

  val localDataSink = 
    Sink.foreach[ByteString](data => println(s"From server: $data"))

  localDataSource.via(clientFlow).to(localDataSink).run
}