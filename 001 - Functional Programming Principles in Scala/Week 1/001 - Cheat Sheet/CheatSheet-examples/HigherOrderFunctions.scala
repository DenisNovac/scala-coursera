object MainHOF {
  def main(args: Array[String]): Unit = {
    val hof = new HigherOrderFunctions

    def cube(x: Int) = x * x * x
    val b = hof.sum1(cube)(1,3) // сумма кубов от 1 до 3
    println("Сумма кубов от 1 до 3: "+b) // 36

    val c = hof.sum2((x: Int) => x * x * x)(1,4) // то же, что выше, но анонимной функцией
    println("Сумма кубов от 1 до 4: "+c) // 100

  }
}


class HigherOrderFunctions {
  // sum принимает на вход функцию f и возвращает сумму её результатов
  // в диапазоне Int, Int
  def sum1(f: Int => Int): (Int, Int) => Int = {
    def sumf(a: Int, b: Int): Int = { // просуммировать результаты f от a до b
      var s = 0
      for (x <- a until b+1) s+=f(x)
      s
    }
    sumf
  }

  // то же что выше, но с явной передачей аргументов
  def sum2(f: Int => Int)(a: Int, b: Int): Int = {
    def sumf(a: Int, b: Int): Int = { // просуммировать результаты f от a до b
      var s = 0
      for (x <- a until b+1) s+=f(x)
      s
    }
    sumf(a,b)
  }
}
