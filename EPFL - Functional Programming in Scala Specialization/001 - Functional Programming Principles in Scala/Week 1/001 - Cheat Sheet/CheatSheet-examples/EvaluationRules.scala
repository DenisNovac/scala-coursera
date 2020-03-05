object MainER {
  def main(args: Array[String]): Unit = {
    val ch = new EvaluationRules()
    ch.myFct(1,2,3,4)
  }
}

class EvaluationRules {
  def exampleDef = 2 // вычисляется когда вызывается
  val exampleVal = 2 // выислятеся мгновенно
  lazy val exampleLazy = 2 // вычисляется только когда требуется

  def squareValue(x: Double) = {}// вызов по значению
  def squareName(x: => Double) = {}// вызов по имени

  def myFct(bindings: Int*) = { // bindings - это последовательность Int с неизвестным количеством аргументов
    for (n <- 0 until bindings.length)
      print(bindings(n))
  }
}
