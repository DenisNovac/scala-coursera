package example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior

sealed trait Greeter
final case class Greet(whom: String) extends Greeter
final case object Stop extends Greeter


object Hello extends App {

  // Собственно поведение
  val greeter: Behavior[Greeter] = 
    Behaviors.receiveMessage[Greeter] {
      case Greet(whom) =>
        println(s"Hello, $whom")
        Behaviors.same
      case Stop =>
        println("Shutting down...")
        Behaviors.stopped
    }


  // Акторная система с поведением
  ActorSystem[Nothing](Behaviors.setup[Nothing] { ctx =>
    val greeterRef = ctx.spawn(greeter, "greeter")
    ctx.watch(greeterRef)  // death pact

    greeterRef ! Greet("world")
    greeterRef ! Stop

    Behaviors.empty
  }, "helloworld-system")
}


