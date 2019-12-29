package Main

object Main extends App {
  val l = List(1,2,3,4,5)
  println(Task1.squareList(l))
  println(Task1.squareListMap(l))


  val list2 = List(-2, -1, 0, 1, 2)
  println(list2.filter(x => x>0)) // 1, 2

  println(myFilter(list2)(x => x>0))


  println(pack(List("a", "a", "a", "b", "c", "c", "a")))
  println(encode(List("a", "a", "a", "b", "c", "c", "a")))
  // List(List(a, a, a), List(b), List(c, c), List(a))
  def myFilter[T](xs: List[T])(implicit f: T => Boolean): List[T] = xs match {
    case Nil => Nil
    case y :: ys => if (f(y)) y :: myFilter(ys) else myFilter(ys)
  }

  def pack[T](xs: List[T]): List[List[T]] = xs match {
    case Nil => Nil
    case x :: xs1 => {
      val (a, b) = xs span(y => y==x)
      a :: pack(b)
    }
  }

  def encode[T](xs: List[T]): List[(T, Int)] = {
    val xsPacked = pack(xs)
    def iter(xs: List[List[T]]): List[(T, Int)] = xs match {
      case Nil => Nil
      case x :: xs1 => (x(0), x.length) :: iter(xs1)
    }
    iter(xsPacked)
  }

}

object Task1 {
  def squareList(xs: List[Int]): List[Int] =
    xs match {
      case Nil => Nil
      case y :: ys => y*y :: squareList(ys)
    }

  def squareListMap(xs: List[Int]): List[Int] =
    xs map (x => x * x)
}