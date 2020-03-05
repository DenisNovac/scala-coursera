package scala.one

trait Expr {
  def eval: Int = this match {
    case Number(n) => n
    case Sum(e1, e2) => e1.eval + e2.eval
  }

  def show: String = this match {
    case Number(n) => n.toString
    case Sum(e1, e2) => e1.show + "+" + e2.show
  }
}


case class Number(n: Int) extends Expr
case class Sum(e1: Expr, e2: Expr) extends Expr

object Main extends App {
  /*def eval(e: Expr): Int = e match {
    case Number(n) => n
    case Sum(e1, e2) => eval(e1) + eval(e2)
  }
  println(eval(Number(10))) // 10
  println(eval(Sum(Number(10), Number(25)))) // 35
  */

  println(Number(10).eval)  // 10
  println(Sum(Number(12), Number(324)).eval) // 336


  println(Sum(Number(12), Number(32)).show) // 12+32
}




