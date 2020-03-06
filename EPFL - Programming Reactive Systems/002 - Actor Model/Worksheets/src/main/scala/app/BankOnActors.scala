package app

import akka.actor.AbstractActor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive

object BankOnActors extends App {
  import BankAccount._
  import WireTransfer._

  val system = ActorSystem("system")
  val account1 = system.actorOf(Props[BankAccount], "acc1")
  val account2 = system.actorOf(Props[BankAccount], "acc2")
  val transfer = system.actorOf(Props[WireTransfer], name = "transfer_query")

  account1 ! Deposit(10_000)
  account1 ! Print  // 10000



  account2 ! Deposit(20_000)
  account2 ! Print  // 20000

  transfer ! Transfer(account1, account2, 50000)

  Thread.sleep(200)
  account1 ! Print  // 5000
  account2 ! Print  // 25000
}



/** Сообщения актора */
object BankAccount {

  case class Deposit(amount: BigInt) {
    require(amount > 0)
  }

  case class Withdraw(amount: BigInt) {
    require(amount > 0)
  }

  case object Done
  case object Failed
  case object Print
}

/** Собственно актор */
class BankAccount extends Actor {
  import BankAccount._

  var balance = BigInt(0)

  def receive = {
    case Deposit(amount) =>
      balance += amount
      sender ! Done

    case Withdraw(amount) if amount <= balance =>
      balance -= amount
      sender ! Done

    case Print =>
      println(balance)

    case _ => sender ! Failed
  }
}


/** Транзакционные сущности */

object WireTransfer {
  case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
  case object Done
  case object Failed
}

class WireTransfer extends Actor {
  import WireTransfer._

  override def receive: Receive = {
    case Transfer(from, to, amount) =>
      from ! BankAccount.Withdraw(amount)
      context.become(awaitWithdraw(to, amount, sender))
  }

  def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive = {
    case BankAccount.Done =>
      to ! BankAccount.Deposit(amount)
      context.become(awaitDeposit(client))

    case BankAccount.Failed =>
      client ! Failed
      context.stop(self)
  }

  def awaitDeposit(client: ActorRef): Receive = {
    case BankAccount.Done =>
      client ! Done
      context.stop(self)
  }
}
