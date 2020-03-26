package app.ClusterMain

import akka.actor.{Actor, ActorLogging, ActorRef}
import ClusterReceptionist._
import app.ClusterWorker.linkchecker.Receptionist

class Messenger(link: String, receptionist: ActorRef) extends Actor with ActorLogging {

  receptionist ! Get(link)
  log.info("Starting to wait")

  override def receive: Receive = {
    case Receptionist.Result(url, links) =>
      log.info(s"Result for $url: ${links.map(s => s + "\n")}")
      context.stop(self)
    case ClusterReceptionist.Failed(url, reason) =>
      log.error(s"Error for $url: $reason")
      context.stop(self)
    case Receptionist.Failed(url, reason) =>
      log.info(s"Result for $url: $reason")
      context.stop(self)
  }

}
