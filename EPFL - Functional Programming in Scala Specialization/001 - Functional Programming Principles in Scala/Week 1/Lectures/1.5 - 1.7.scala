import math.abs
import scala.annotation.tailrec

object Exercise extends App {


  def sqrt(x: Double) = {
    def sqrtIter(guess: Double): Double =
      if (isGoodEnough(guess)) guess
      else sqrtIter(improve(guess))

    def isGoodEnough(guess: Double) =
    // берем guess и находим квадрат, смотрим на x
      abs(guess * guess - x) / x < 0.001

    def improve(guess: Double) =
      (guess + x / guess) / 2

    sqrtIter(1.0)
  }


  def factorial(n: Int): Int =
    if (n==0) 1 else n * factorial(n-1)

  // аннотация требует рекурсию быть хвостовой
  @tailrec
  def factorial2(n: Int, last_step: Int = 1): Int = {
    if (n == 0) last_step else {
      val step = n * last_step
      factorial2(n-1, step)
    }
  }


  def factorial3(n: Int): Int = {
    @tailrec
    def loop(acc: Int, n: Int): Int =
      if (n == 0) acc
      else loop(acc * n, n-1)
    loop(1, n)
  }

  println(factorial(12))
  println(factorial2(12))
  println(factorial3(12))
}


