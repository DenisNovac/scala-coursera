package two

trait Expr {
  def eval: Int
}

class Number(n: Int) extends Expr {
  def eval: Int = n
}

class Summ(e1: Expr, e2: Expr) extends Expr {
  def eval: Int = e1.eval + e2.eval
}

class NumberPrinter(n: Int) extends Number(n: Int) {
  def print: Unit = println(n)
}

object Main extends App {
  val n = new NumberPrinter(10)
  n.print // 10
  val nn = new Number(5)
  println(new Summ(n, nn).eval) // 15
}