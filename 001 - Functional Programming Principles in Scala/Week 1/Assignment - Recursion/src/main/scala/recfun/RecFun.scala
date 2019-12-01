package recfun

import java.security.KeyStore.TrustedCertificateEntry

import scala.annotation.tailrec

object RecFun extends RecFunInterface {

  def main(args: Array[String]): Unit = {
    println("Pascal's Triangle")
    for (row <- 0 to 10) {
      for (col <- 0 to row)
        print(s"${pascal(col, row)} ")
      println()
    }
  }

  /**
   * Exercise 1
   */
  def pascal(c: Int, r: Int): Int = {
    def factorial(n: Int): Int = {
      @tailrec
      def loop(acc: Int, n: Int): Int =
        if (n == 0) acc
        else loop(acc * n, n-1)
      loop(1, n)
    }
    // формула для подсчёта элемента треугольника
    factorial(r) / (factorial(c) * factorial(r-c))
  }

  /**
   * Exercise 2
   */
  def balance(chars: List[Char]): Boolean = {
    /**
     * Для каждой ( нужно пройти до конца строки и найти ). Если где-то не нашлось - вернуть false
     */
    def iterBalance(chars: List[Char], counter: Int): Boolean = (chars, counter) match {
      case (cs, 0) if cs.isEmpty => true
      case (cs, _) if cs.isEmpty => false
      case (cs, c) => cs.head match {
        case '(' => iterBalance(cs.tail, c+1)
        case ')' if c>0 => iterBalance(cs.tail, c-1)
        case ')' => false
        case _ => iterBalance(cs.tail, c)
      }
    }
    iterBalance(chars, 0)
  }

  /**
   * Exercise 3
   * 3 ways to give change for 4 if you have coins with denomination 1 and 2:
   * 1+1+1+1,
   * 1+1+2,
   * 2+2.
   *
   */
  def countChange(money: Int, coins: List[Int]): Int = (money, coins) match {
    case (0, _) => 1
    case (m, _) if m < 0 => 0
    case (_, cs)  if cs.isEmpty => 0
    case (m, cs) => countChange(m - cs.head, cs) + countChange(m, cs.tail)
  }
}
