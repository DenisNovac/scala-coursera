package app

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}

import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ToggleSuit extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  /**
    * Классический тест
    * */
  def testToggle(): Unit = {
    implicit val system: ActorSystem = ActorSystem("TestSys")
    val toggle = system.actorOf(Props[Toggle])
    val p = TestProbe() // внутри является актором

    p.send(toggle, "How are you?")
    p.expectMsg("happy")

    p.send(toggle, "How are you?")
    p.expectMsg("sad")

    p.send(toggle, "unknown")
    p.expectNoMessage(1.seconds)

    /** Выключение системы даёт фьючу */
    Await.result(system.terminate(), 10.second)
  }


  "An Toggle actor" must {
    "toggle between messages" in {
      testToggle()
    }
  }

  /**
    * Можно не создавать TestProbe, а использовать TestKit, который уже имеет его
    * */
  def testWithinAProbe(): Unit = {
    new TestKit(ActorSystem("TestSys")) with ImplicitSender {  // оно красное но работает
      val toggle: ActorRef = system.actorOf(Props[Toggle])
      toggle ! "How are you?" // имя внутреннего актора - testActor
      expectMsg("happy")
      toggle ! "How are you?"
      expectMsg("sad")
      toggle ! "Unknown"
      expectNoMessage(1.seconds)

      Await.result(system.terminate(), 10.second)
    }
  }


  "An Toggle actor" must {
    "toggle between messages while being tested within a probe" in {
      testWithinAProbe()
    }
  }
}
