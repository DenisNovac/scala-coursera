

object Main extends App {
  println(sum(List(1,2,3,4,5))) // 15

  println(List(1,2,3,4,5) reduceLeft (_ + _))

  println((List(1,2) foldLeft 25)(_ + _)) // 28

  def sum(xs: List[Int]): Int = xs match {
    case Nil => 0
    case y :: ys => y + sum(ys)
  }

  val l = new MyList(List(1,2,3,4,5))
  println(l.reduceLeft(_ * _)) // 120
  println(l.foldLeft(0)(_ * _)) // 0
  println(l.foldLeft(1)(_ * _)) // 120
  println(l.foldRight(1)(_ * _)) // 120

  val m = 5 :: List(1)
  val n = List(1) :: 5 // cannot resolve ::

}


class MyList[T](l: List[T]){


  def reduceLeft(op: (T,T) => T): T = l match {
    case Nil => throw new Error ("Nil.reduceLeft")
    case x :: xs => (xs foldLeft x)(op) // мы делаем первый элемент аккумулятором
  }

  def foldLeft[U](z: U)(op: (U, T) => U): U = l match {
    case Nil => z
    case x :: xs => (xs foldLeft op(z, x))(op)
  }

  def reduceRight(op: (T,T) => T): T = l match {
    case Nil => throw new Error ("Nil.reduceLeft")
    case x::Nil => x
    case x::xs => op(x, xs.reduceRight(op))
  }

  def foldRight[U](z:U)(op: (T,U) => U): U = l match {
    case Nil => z
    case x :: xs => op(x, (xs foldRight z)(op))
  }

}