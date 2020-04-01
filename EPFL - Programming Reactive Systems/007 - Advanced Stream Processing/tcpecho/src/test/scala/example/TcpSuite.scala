package example

import org.scalatest.flatspec.AnyFlatSpec
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.stream.scaladsl.Sink
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

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