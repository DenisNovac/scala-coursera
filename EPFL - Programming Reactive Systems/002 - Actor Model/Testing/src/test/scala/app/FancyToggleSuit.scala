package app

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}

import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FancyToggleSuit
    extends TestKit(ActorSystem("TestSys"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  // Тестируемый актор
  val toggle: ActorRef = system.actorOf(Props[Toggle])

  // после всех тестов выключиться
  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An Toggle actor" must {
    "toggle between messages" in {

      toggle ! "How are you?"
      expectMsg("happy")
      toggle ! "How are you?"
      expectMsg("sad")
      toggle ! "Unknown"
      expectNoMessage(1.seconds)
    }
  }
}
