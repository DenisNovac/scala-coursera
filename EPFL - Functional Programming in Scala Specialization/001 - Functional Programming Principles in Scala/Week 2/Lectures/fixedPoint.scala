import math.abs
import scala.annotation.tailrec

object Main {
  def main(arguments: Array[String]) = {
    val point = 1 // стартовое значение
    //println( fixedPoint(x => 1+x/2)(point) ) // близко к 2, как и ожидалось

    println(sqrt(64))
  }


  def sqrt(x: Double) = 
    // x передался прямо в averageDump, поэтому усреднение функции 
    // считалось только по y, а x было известным.
    fixedPoint( averageDamp(y => x/y) )(1)

    //fixedPoint(y => (y + x / y)/2)(1.0)

  val tolerance = 0.0001
  def isCloseEnough(x: Double, y: Double) =
    abs((x-y)/x)/x < tolerance

  def fixedPoint(f: Double => Double)(firstGuess: Double) = {
    @tailrec
    def iterate(guess: Double): Double = {
      println(guess)
      val next = f(guess)
      if (isCloseEnough(guess, next)) next
      else iterate(next)
    }
    iterate(firstGuess)
  }

  // функция возвращает функцию с усреднением
  def averageDamp(f: Double => Double): Double => Double =
    x => (x + f(x)) / 2

}