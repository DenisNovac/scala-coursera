package app.ClusterMain

import akka.actor.{ActorSystem, Props}

object Main extends App {
  val system = ActorSystem("Cluster")
  val app = system.actorOf(Props(new ClusterMain))
  val rec = system.actorOf(Props(new ClusterReceptionist))


  var notExited = true
  while (notExited) {
    val link = scala.io.StdIn.readLine().filterNot(c => c == ' ')

    link match {
      case "quit" => notExited = false
      case s: String if s.nonEmpty => system.actorOf(Props(new Messenger(link, rec)))
      case _ => ()
    }
  }
  system.stop(rec)
  system.stop(app)
  system.terminate()
}
