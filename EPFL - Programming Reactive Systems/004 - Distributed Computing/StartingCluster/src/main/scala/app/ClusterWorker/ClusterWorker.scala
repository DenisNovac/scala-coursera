package app.ClusterWorker

import akka.actor.{Actor, ActorLogging, Address}
import akka.cluster.{Cluster, ClusterEvent}

// Для него конфигурация akka.remote.netty.tcp.port = 0
class ClusterWorker extends Actor with ActorLogging {
  val cluster: Cluster = Cluster(context.system)
  cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])
  val main: Address = cluster.selfAddress.copy(port = Some(25520))
  log.info(s"Cluster address is $main")
  cluster.join(main)

  override def receive: Receive = {
    case ClusterEvent.MemberRemoved(m, _) =>
      log.info(s"Got new member removed message: $m")
      if (m.address == main) context.stop(self)
  }
}