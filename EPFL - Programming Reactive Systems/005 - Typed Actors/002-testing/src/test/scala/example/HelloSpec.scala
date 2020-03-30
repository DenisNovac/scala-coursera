package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.typed.ActorRef

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.actor.typed.Behavior
import akka.Done


class HelloSpec extends AnyFlatSpec with Matchers {

  def greeter: Behavior[Greeter] = 
      Behaviors.receiveMessage[Greeter] {

        case Greet(whom) =>
          println(s"Hello, $whom")
          Behaviors.same

        case GreetWithRef(whom, whomRef) => 
          println(s"Answering to $whom")
          whomRef ! Done
          Behaviors.same

        case Stop =>
          println("Shutting down...")
          Behaviors.stopped
      }


  def guardian = Behaviors.receive[Guardian] {
    case (ctx, NewGreeter(replyTo)) =>
      val ref: ActorRef[Greeter] = ctx.spawnAnonymous(greeter)
      replyTo ! ref
      Behaviors.same
    case (_, Shutdown) =>
      Behaviors.stopped
  }


  "Guardian actor" should "return Greeter actorRef" in {
    val guardianKit = BehaviorTestKit(guardian)
    val sessionInbox = TestInbox[ActorRef[Greeter]]()

    guardianKit.ref ! NewGreeter(sessionInbox.ref)
    guardianKit.runOne
    // Излвекли одно сообщение из ящика - это ActorRef[Greeter]
    val greeterRef = sessionInbox.receiveMessage

    assert {
      greeterRef match {
        case _: ActorRef[Greeter] => true
        case _                    => false 
      }
    }
  }


  "An session inbox" should "be empty" in {
    val guardianKit = BehaviorTestKit(guardian)
    val sessionInbox = TestInbox[ActorRef[Greeter]]()

    guardianKit.ref ! NewGreeter(sessionInbox.ref)
    guardianKit.runOne

    sessionInbox.receiveMessage
    // Больше сообщений в ящике нет
    assert(!sessionInbox.hasMessages)
  }


  "Greeter behavior" should "be stopped" in {
    val greeterKit = BehaviorTestKit(greeter)
    greeterKit.ref ! Greet("Helen")
    greeterKit.runOne
    greeterKit.ref ! Stop
    greeterKit.runOne
    assert(!greeterKit.isAlive)
  }



  "Guardian child" should "return done" in {
    val guardianKit = BehaviorTestKit(guardian)
    val sessionInbox = TestInbox[ActorRef[Greeter]]()
    
    // guardian создал нового гритера
    guardianKit.ref ! NewGreeter(sessionInbox.ref)
    guardianKit.runOne

    // Получаем тестер для него
    val greeterRef = sessionInbox.receiveMessage
    val greeterKit = guardianKit.childTestKit(greeterRef)

    val doneInbox = TestInbox[Done]()
    greeterRef ! GreetWithRef("World", doneInbox.ref)
    greeterKit.runOne

    assert( doneInbox.receiveAll match {
      case Seq(Done)    => true
      case _            => false
    })

  }
  
}
