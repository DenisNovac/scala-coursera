package scala.main

import java.util.NoSuchElementException

object Main extends App {
  println("Hi there")

  val one = new Succ(Zero)
  //val zerominus = Zero.predecessor // эксепшен
  val zerogood = Zero - Zero // 0 - 0 это 0
  val two = one.successor
  val three = one + two
  val one_again = three - two
  // val exception = one_again - three
}

abstract class Nat {
  def isZero: Boolean // проверка на ноль
  def predecessor: Nat // предыдущий. Если 0 - экспешен
  def - (that: Nat): Nat // если ответ 0 или меньше - эксепшен

  // а эти методы окажутся общими для всех

  def successor: Nat = new Succ(this) // последующий

  // для Zero можно упростить
  def + (that: Nat): Nat = {
    def iter(res: Nat, acc: Nat): Nat =
      if (!acc.isZero)
        iter(res.successor, acc.predecessor)
      else
        res
    iter(this, that)
  }
  // преподаватель написал + так:
  // def + (that: Nat): Nat = new Succ(n + that),
}

object Zero extends Nat {
  override def isZero: Boolean = true

  override def predecessor: Nat = throw new NoSuchElementException("No predecessor to zero!")
  // проще, хотя и то, что записано в самом классе - сработает
  override def + (that: Nat): Nat = that

  override def -(that: Nat): Nat = {
    if (that.isZero) Zero
    else throw new NoSuchElementException("No minus on zero!")
  }
}

// succ как бы обозначает НАСЛЕДНИК, поэтому он хранит предыдущее значение
class Succ(n: Nat) extends Nat {
  override def isZero: Boolean = false

  override def predecessor: Nat = n

  override def -(that: Nat): Nat = {
    def iter(res: Nat, acc: Nat): Nat =
      if (!acc.isZero)
        iter(res.predecessor, acc.predecessor)
      else
        res
    iter(this, that)
  }
  // преподаватель написал так:
  // def -(that: Nat): Nat = if (that.isZero) this else n - that.predecessor
}