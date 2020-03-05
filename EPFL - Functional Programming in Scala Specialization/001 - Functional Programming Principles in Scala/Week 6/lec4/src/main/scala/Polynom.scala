package main.scala

object Main extends App {
  val p1 = new Poly(1 -> 2.0, 3 -> 4.0, 5 -> 6.2)
  val p2 = new Poly(0 -> 3.0, 3 -> 7.0)
  val p3 = p1 + p2
  println(p1)
  println(p2)
  println(p3)  // 2.0x^1 + 7.0x^3 + 6.2x^5 + 3.0x^0

  println(p1.terms(7))
}

class Poly(val terms0: Map[Int, Double])  {
  def this(bindings: (Int, Double)*) = this(bindings.toMap)

  val terms = terms0 withDefaultValue(0.0)

  //def + (other: Poly) = new Poly(terms ++ (other.terms map adjust))

  def + (other: Poly) =
    new Poly((other.terms foldLeft terms)(addTerm))

  def addTerm(terms: Map[Int, Double], term: (Int, Double)) = {
    val coeff = terms(term._1) + term._2
    terms.updated(term._1, coeff)
  }

  def adjust(term: (Int, Double)): (Int, Double) = {
    val (exp, coeff) = term
    exp -> (coeff + terms(exp))
  }

  override def toString =
    (for ((exp, coeff) <- terms.toList.sorted.reverse) yield coeff+"x^"+exp) mkString " + "
}
