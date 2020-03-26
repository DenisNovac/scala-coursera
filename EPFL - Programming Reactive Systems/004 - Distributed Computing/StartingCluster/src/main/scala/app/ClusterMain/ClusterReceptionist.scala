package app.ClusterMain

import java.util.Date

import akka.actor.{Actor, ActorLogging, Address, Props}
import akka.cluster.ClusterEvent.{MemberRemoved, MemberUp}
import akka.cluster.{Cluster, ClusterEvent}

import scala.util.Random

class ClusterReceptionist extends Actor with ActorLogging {
  import ClusterReceptionist._

  val cluster: Cluster = Cluster(context.system)

  /** Ему нужно знать, кто в данный момент вообще есть в кластере */
  cluster.subscribe(self, classOf[MemberUp])
  cluster.subscribe(self, classOf[MemberRemoved])

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  log.info("Starting to wait")
  override def receive: Receive = awaitingMembers

  val awaitingMembers: Receive = {
    // В ответ на cluster.subscribe всегда придёт такое сообщение
    case current: ClusterEvent.CurrentClusterState =>
      val addresses = current.members.toVector map (_.address) // Текущие члены кластера
      val notMe = addresses filter (_ != cluster.selfAddress) // Убираем из массива себя
      if (notMe.nonEmpty) context.become(active(notMe)) // Если есть кто-то ещё - запускаемся, иначе ждём MemberUp

    case MemberUp(member)
        if member.address != cluster.selfAddress => // Пришёл новый актор, можно начать работать с одним вектором
      context.become(active(Vector(member.address)))

    case Get(url) =>
      sender ! Failed(url, "No nodes available") // Пока никого нет - ждём
  }

  def active(addresses: Vector[Address]): Receive = {
    // Продолжаем мониторить пополнение кластера
    case MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(addresses :+ member.address))

    // Удаление из кластера тоже мониторим
    case MemberRemoved(member, _) =>
      val next = addresses filterNot (_ == member.address)
      if (next.isEmpty) context.become(awaitingMembers)
      else context.become(active(next))

    // Используем информацию, которую имеем о кластере
    // Если запущенных детей меньше, чем нод в кластере - есть свободные
    case Get(url) if context.children.size < addresses.size =>
      /** Обязательно завести переменные, т.к. actorOf - асинхронная операция */
      val client = sender
      val address = pick(addresses) // Берём случайный адрес из листа
      context.actorOf(Props(new Customer(client, url, address)))
    case Get(url) =>
      sender ! Failed(url, "Too many parallel queries")
  }


  def pick(value: Vector[Address]): Address = {
    val i = new Random(new Date().getTime).between(0, value.length)
    value(i)
  }

}

object ClusterReceptionist {
  case class Get(url: String)
  case class Failed(url: String, reason: String)
}
