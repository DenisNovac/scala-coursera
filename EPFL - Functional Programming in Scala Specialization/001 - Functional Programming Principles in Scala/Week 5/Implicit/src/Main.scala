import math.Ordering

object Main extends App {

  val l = List(1,23,2,-3,0,1,7,4,9,0,192,-23,39,43,5,1,-93)
  val ls = msort(l)((x: Int, y: Int) => x < y)
  println(ls)

  val fruits = List("apple", "orange", "pineapple", "banana", "cherry", "fruit", "oak")
  val fs = msort(fruits)((x, y) => x.length < y.length)

  val fsor = msortOrd(fruits)(Ordering.String)
  println(fsor)  // oak, fruit, apple, cherry, banana, orange, pineapple

  val fsorimpl = msortOrdImp(fruits)
  val lsortimpl = msortOrdImp(l)
  println(lsortimpl) //-93, -23, -3, 0, 0, 1, 1, 1, 2, 4, 5, 7, 9, 23, 39, 43, 192
  println(fsorimpl)  // oak, fruit, apple, cherry, banana, orange, pineapple

  def msort[T](xs: List[T])(lt: (T, T) => Boolean): List[T] = {
    val n = xs.length/2
    if (n == 0) xs
    else {
      def merge(xs: List[T], ys: List[T]): List[T] = (xs, ys) match {
        case (List(), ys) => ys
        case (xs, List()) => xs
        case (x :: xs1, y :: ys1) =>
          if (lt(x, y)) x :: merge(xs1, ys)
          else y :: merge(xs, ys1)
      }
      val (fst, snd) = xs splitAt n
      merge(msort(fst)(lt), msort(snd)(lt))
    }
  }


  def msortOrd[T](xs: List[T])(ord: Ordering[T]): List[T] = {
    val n = xs.length/2
    if (n == 0) xs
    else {
      def merge(xs: List[T], ys: List[T]): List[T] = (xs, ys) match {
        case (List(), ys) => ys
        case (xs, List()) => xs
        case (x :: xs1, y :: ys1) =>
          if (ord.lt(x, y)) x :: merge(xs1, ys)
          else y :: merge(xs, ys1)
      }
      val (fst, snd) = xs splitAt n
      merge(msortOrd(fst)(ord), msortOrd(snd)(ord))
    }
  }

  def msortOrdImp[T](xs: List[T])(implicit ord: Ordering[T]): List[T] = {
    val n = xs.length/2
    if (n == 0) xs
    else {
      def merge(xs: List[T], ys: List[T]): List[T] = (xs, ys) match {
        case (List(), ys) => ys
        case (xs, List()) => xs
        case (x :: xs1, y :: ys1) =>
          if (ord.lt(x, y)) x :: merge(xs1, ys)
          else y :: merge(xs, ys1)
      }
      val (fst, snd) = xs splitAt n
      merge(msortOrdImp(fst), msortOrdImp(snd))
    }
  }

}
