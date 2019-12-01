package recfun

import scala.annotation.tailrec

object Main {
  def main(args: Array[String]): Unit = {
    /*println(buildTriangle(0,2))
    println(buildTriangle(1,2))
    println(buildTriangle(1,3))
    println(buildTriangle(1,4))
    println(buildTriangle(2,5))
    println(buildTriangle(2,6))*/

    for (i <- 0 until 10) {
      for (j <- 0 until i+1)
        print(buildTriangle2(j, i)+" ")
      println()
    }



}



  def buildTriangle(column: Int, row: Int): Int = {
    def factorial(n: Int): Int = {
      @tailrec
      def loop(acc: Int, n: Int): Int =
        if (n == 0) acc
        else loop(acc * n, n-1)
      loop(1, n)
    }
    // можно всегда считать только это
    factorial(row) / (factorial(column) * factorial(row-column))
  }


  def buildTriangle2(column: Int, row: Int): Int = {
    def factorial(n: Int): Int = {
      @tailrec
      def loop(acc: Int, n: Int): Int =
        if (n == 0) acc
        else loop(acc * n, n-1)
      loop(1, n)
    }

    // первое и последнее числа равны 1
    if (column == 0 || column == row) 1
    // второе и предпоследнее числа равны номеру строки r
    else if (column == 1 || column == row-1) row
    // третье число в ряду (r(r-1))/2
    else if (column == 2) (row*(row-1))/2
    // m-e число - всегда биноминальный коэффициент C(m, row), можно считать только по нему
    else {
      factorial(row) / (factorial(column) * factorial(row-column))
    }
  }

}
