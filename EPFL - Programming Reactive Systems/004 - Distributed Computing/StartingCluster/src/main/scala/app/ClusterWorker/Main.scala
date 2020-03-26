package app.ClusterWorker

import akka.actor.{ActorSystem, Props}

object Main extends App {
  val system = ActorSystem("Cluster")
  val app = system.actorOf(Props(new ClusterWorker))
}
