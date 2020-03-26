package app.ClusterMain

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, ClusterEvent}


// однонодный кластер на порту 2552
class ClusterMain extends Actor with ActorLogging {
  val cluster: Cluster = Cluster(context.system)  // Подключает расширение кластера к акторной системе
  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])  // подписывается на события добавления
  cluster.join(cluster.selfAddress)  // присоединяет себя
  log.info(s"Cluster address is ${cluster.selfAddress}")

  override def receive: Receive = {
    case current: ClusterEvent.CurrentClusterState =>
      log.info("Got it")

    case ClusterEvent.MemberUp(member) =>
      log.info(s"Got new member request: $member")
      if (member.address != cluster.selfAddress) {
        // someone joined
      }
  }
}
