package example

import akka.actor._
import akka.stream.ActorMaterializer
import scala.concurrent.Future
import akka.stream.scaladsl.{Source, Flow, Sink}
import akka.NotUsed


object Hello extends App {
  implicit val system = ActorSystem()  // Для ActorMaterializer
  //implicit val mat = ActorMaterializer()  // Теперь имплиситно лежит в самой системе

  /*val eventuallyResult: Future[Int] = 
    Source(1 to 10)
      .map(_ * 2)
      .runFold(0)((acc, x) => acc + x)  // До этой линии поток ещё не запустился
      // методы с run обычно являются запускателями*/

  /** Полное написание */
  val numbers: Source[Int,NotUsed]    = Source(1 to 10)
  val doubling: Flow[Int,Int,NotUsed] = Flow.fromFunction((x: Int) => x * 2)
  val sum: Sink[Int,Future[Int]]      = Sink.fold(0)((acc: Int, x: Int) => acc + x)

  val eventuallyResult: Future[Int] =
    numbers.via(doubling).runWith(sum)
}