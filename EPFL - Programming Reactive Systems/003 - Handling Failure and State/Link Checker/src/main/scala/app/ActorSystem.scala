package app

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, ReceiveTimeout, Status, SupervisorStrategy, Terminated}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.pattern.pipe
import akka.routing.ActorRefRoutee

class ActorSystem {}

/**
  * Идёт по URL и возвращает тело
  * @param url Нужный URL
  * @param depth Текущая глубина
  */
class Getter(url: String, depth: Int) extends Actor {
  import Getter._


  // Имплисивные контекст и экзекьютор, необходимые для Future
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  // Этот метод можно переписать в тесте для смены клиента
  def client: WebClient = AsyncWebClient

  // клиент используется здесь
  val future: Future[String] = client.get(url)


  // Future.Failed не пересылается после получения
  future.pipeTo(self)  // Отправить самому себе результат фьючи

  override def receive: Receive = {  // Сюда результат прилетит после отправки
    case "ACTOR FAKE WEB CLIENT GENERATED FAILURE" => context.stop(self) // специальная ошибка-костыль
    case body: String =>
      for (link <- AsyncWebClient.findLinks(body))
        context.parent ! Controller.Check(link, depth)  // вернуть результат родителю
      context.stop(self)
    case Status.Failure => context.stop(self)
    case Abort => context.stop(self)
  }

  /*def stop(): Unit = {
    context.parent ! Done()
    context.stop(self)
  }*/
}

object Getter {
  case class Abort()
  case class Done()
}


class Controller extends Actor with ActorLogging {
  import Controller._
  import Getter._

  var cache = Set.empty[String]  // результат - набор ссылок
  //var children = Set.empty[ActorRef]  // Теперь пользуемся context-ом

  context.setReceiveTimeout(10.seconds)  // если истёк - шлёт сам себе ReceiveTimeout
  // обнуляется после каждого сообщения

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 5) {
      case _: Exception => SupervisorStrategy.Restart
    }

  def receive: Receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0)
        context.watch(context.actorOf(Props(new Getter(url, depth - 1))))  // создали геттер через DeathWatch
      cache += url  // считаем, что URL был пройден и записываем его
    case Terminated(_) =>
      if (context.children.isEmpty) context.parent ! Result(cache)  // если все Getter-ы сделали Done - конец
    case ReceiveTimeout => context.children foreach context.stop
  }
}

object Controller {
  case class Check(url: String, depth: Int)
  case class Result(value: Set[String])

}


class Receptionist extends Actor {
  import Receptionist._

  /** На каждый Failure любого ребенка высылает ему Stop */
  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy



  def receive = waiting

  /** Ожидание запускается при старте актора или при завершении работ */
  val waiting: Receive = {
    case Get(url) => context.become(runNext(Vector(Job(sender, url))))
  }

  def running(queue: Vector[Job]): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(context.unwatch(sender))  // остонавливаем контроллер и перестаём следить
      context.become(runNext(queue.tail))

    case Terminated(_) =>
      val job = queue.head
      job.client ! Failed(job.url)
      context.become(runNext(queue.tail))
    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }


  case class Job(client: ActorRef, url: String)
  var reqNo = 0


  /** Запустить следующую работу */
  def runNext(queue: Vector[Job]): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting  // если очередь пустая - ждём
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")  // имя актора - контроллер с номером реквеста
      context.watch(controller)  // начали следить за ребёнком
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  /** Зарегистрировать новую работу */
  def enqueueJob(queue: Vector[Job], job: Job): Receive = {
    if (queue.size > 3) {  // лимитируем работы тремя, если пришла новая - фейлим её
      sender ! Failed(job.url)
      running(queue)
    } else running(queue :+ job)
  }
}


object Receptionist {
  case class Get(url: String)
  case class Result(url: String, links: Set[String])
  case class Failed(url: String)
}

























