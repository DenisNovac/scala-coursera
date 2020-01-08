package main.scala

import scala.annotation.tailrec

/*object Main extends App {
  val nqueens = NQueens
  println(nqueens.queens(4))

  println(nqueens.queens(4).map(nqueens.show).mkString("\n"))
}*/

object NQueens extends App {
  println(queens(4))

  val show1 = queens(4) map show
  //println(show1)

  val show2 = queens(4) map show mkString("\n")  // прописали разделитель между строками
  //println(show2)

  val show3 = queens(8) take 2 map show mkString("\n")
  println(show3)

  // n - количество рядов на шахматной доске
  def queens(n: Int): Set[List[Int]] = {
    def placeQueens(k: Int): Set[List[Int]] =
      if (k == 0) Set(List())
      else
        for {
          queens <- placeQueens(k-1)  // первая проблема - поместить k-1 королев
          col <- 0 until n  // вторая проблема - проитерировать через все возможные колонки (доска квадратная, колонки=рядам)
          if isSafe(col, queens)  // проверка безопасности колонки, учитывая всех предыдущих королев
        } yield col :: queens  // прибавили решение спереди
    placeQueens(n)
  }

  def isSafe(col: Int, queens: List[Int]): Boolean = {
    val row = queens.length
    val queensWithRow = (row-1 to 0 by -1) zip queens
    queensWithRow forall {
      case (r,c) => col != c && math.abs(col -c) != row-r  // после && следует проверка диагонали
    }
  }

  /*@tailrec
  def isSafe(col: Int, queens: List[Int]): Boolean = queens match {
    case Nil => true  // просто пустой лист
    case x :: xs if x==col => false  // даже неважно, если xs на деле Nil
    case x :: Nil => true
    case x :: xs => isSafe(col, xs)
  }*/


  def show(queens: List[Int]) = {
    val lines =
      for (col <- queens.reverse)
      yield Vector.fill(queens.length)("* ").updated(col, "X ").mkString
        "\n" + (lines mkString "\n")
  }


}
