package app.ClusterMain

import akka.actor.{Actor, ActorRef, Address, Deploy, Props, ReceiveTimeout, SupervisorStrategy, Terminated}
import akka.remote.RemoteScope

import scala.concurrent.duration._
import app.ClusterWorker.linkchecker.Controller.Check
import app.ClusterWorker.linkchecker.{Controller, Getter, Receptionist}

class Customer(client: ActorRef, url: String, node: Address) extends Actor {

  /**
    * В Actor уже есть имплисит ActorRef, указывающий на самого себя. Мы оверрайдим его, чтобы ремот актор слал ответы
    * не сюда, а родителю этого актора. Этот актор как бы становится прозрачным для тех, кто снаружи.
    */
  implicit val s: ActorRef = context.parent

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  /**
    * Ранее мы использовали Props только для локального создания акторов:
    * context.actorOf(Props(new Getter(...)))). Но Props позволяет указать аргументы.
    * Указание withDeploy - это единственное, что отличает создание локального актора от удаленного
    * */
  val props = Props[Controller].withDeploy(Deploy(scope = RemoteScope(node)))
  val controller = context.actorOf(props, "controller")

  context.watch(controller)
  context.setReceiveTimeout(60.seconds)
  controller ! Check(url, 2)

  override def receive: Receive =
    ({
      case r: ReceiveTimeout =>
        context.unwatch(controller)
        client ! Receptionist.Failed(url, "controller timed out")

      case Terminated(_) =>
        client ! Receptionist.Failed(url, "Controller dead")

      case Controller.Result(links) =>
        context.unwatch(controller)
        client ! Receptionist.Result(url, links)

    }: Receive) andThen (_ => context.stop(self))
}
