package example

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.Done

sealed trait Greeter

final case class Greet(whom: String) extends Greeter
final case object Stop extends Greeter
final case class GreetWithRef(whom: String, whomRef: ActorRef[Done]) extends Greeter


sealed trait Guardian
case class NewGreeter(replyTo: ActorRef[ActorRef[Greeter]]) extends Guardian
case object Shutdown extends Guardian

