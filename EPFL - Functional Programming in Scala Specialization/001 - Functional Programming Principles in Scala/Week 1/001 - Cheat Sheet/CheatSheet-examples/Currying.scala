object Currying {
  def main(args: Array[String]): Unit = {
    def fn(a: Int, b: Int): Int = { a + b } // некаррированна: тип (Int, Int) = Int
    def fс(a: Int)(b: Int): Int = { a + b } // каррированная функция (тип Int => Int => Int)

    println(fn(1,2)) // 3
    println(fn(3,4)) // 7

  }
}
