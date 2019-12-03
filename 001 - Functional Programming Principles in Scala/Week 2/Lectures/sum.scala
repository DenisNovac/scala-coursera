import scala.annotation.tailrec

object Main {
  def main(arguments: Array[String]) = {
    println("Hello world")
    val just_sum = sum((x: Int) => x, 1, 4)
    println(just_sum)

    val sum_of_squares = sum((x: Int) => x * x, 1, 4)
    println(sum_of_squares)

    val sum_of_cubes = sum((x: Int) => x * x * x, 1, 3)
    println(sum_of_cubes)
  }

  /**
   * Передаём функцию, которая преобразовывает элементы между a и b,
   * начальное значение a и границу b.
   * */
  def sum(f: Int => Int, a: Int, b: Int): Int = {
    @tailrec
    def loop(a: Int, acc: Int): Int = {
      if (a > b) acc
      else loop(a+1, f(a)+acc)
    }
    loop(a, 0)
  }
}