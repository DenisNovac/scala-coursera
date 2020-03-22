package app

import java.util.concurrent.Executor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

object GetterSuit {
  val firstLink = "http://www.rkuhn.info/1"

  val bodies = Map(
    firstLink ->
      """<html>
        |  <head><title>Page 1</title></head>
        |  <body>
        |    <h1>A Link</h1>
        |    <a href="http://rkuhn.info/2">click here</a>
        |  </body>
        |</html>""".stripMargin
  )

  val links = Map(firstLink -> Seq("http://rkuhn.info/2"))

  /**
    * Фейковый клиент, который не ходит по сайтам
    */
  object FakeWebClient extends WebClient {
    override def get(url: String)(implicit exec: Executor): Future[String] =
      bodies.get(url) match {
        case None =>
          Future.successful("ACTOR FAKE WEB CLIENT GENERATED FAILURE")
        //Future.failed(BadStatus(404))  // Failed не пересылается

        case Some(body) => Future.successful(body)
      }
  }

  /**
    * Создание Getter-а с фейковым клиентом
    */
  def fakeGetter(url: String, depth: Int): Props =
    Props(new Getter(url, depth) {
      override def client: WebClient = FakeWebClient
    })

  /**
    * Специальный псевдо-родитель для Getter, возвращающий всё в TestProbe
    */
  class StepParent(child: Props, probe: ActorRef) extends Actor {
    context.actorOf(child, "child") // создание child
    def receive: Receive = {
      case msg =>
        probe.tell(msg, sender) // пересылка с сохранением первичного отправителя
    }
  }

}

/** Тестирующий зонд */
class GetterSpec
    extends TestKit(ActorSystem("TestSys"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import app.GetterSuit._

  override def afterAll(): Unit = system.terminate()

  "A Getter" must {
    "return the right body" in {
      val getter = system.actorOf(
        Props(new StepParent(fakeGetter(firstLink, 2), testActor)),
        "rightBody"
      )

      for (link <- links(firstLink))
        expectMsg(Controller.Check(link, 2))
      expectMsg(Getter.Done())
    }

    "properly finish in case of errors" in {
      val getter = system.actorOf(
        Props(new StepParent(fakeGetter("unknown", 2), testActor)),
        "wrongLink"
      )
      expectMsg(Getter.Done())
    }
  }

}
