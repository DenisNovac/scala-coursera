package app
import akka.actor.{Actor, ActorSystem, Props}


object SimpleActorCounter extends App {
  val system = ActorSystem("system")
  val counter = system.actorOf(Props[MainActor], "main")

}


class Counter extends Actor {

  def counter(n: Int): Receive = {
    case "incr" => context.become(counter(n + 1))
    case "get" => sender ! n
  }

  override def receive: Receive = counter(0)
}


class MainActor extends Actor {

  val counter = context.actorOf(Props[Counter], "counter")

  counter ! "incr"
  counter ! "incr"
  counter ! "incr"
  counter ! "incr"

  counter ! "get"  // прокнет receive пушто Counter сделает ! в нас

  override def receive = {
    case count: Int =>
      println(count)  // 4
      context.stop(self)
  }
}


