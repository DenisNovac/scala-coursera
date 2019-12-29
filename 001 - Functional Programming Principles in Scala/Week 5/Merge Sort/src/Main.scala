object Main extends App {

  val l = List(3,4,1,2,4,7,1,0,-2,4,2)
  println(msort(l))


  def msort(xs: List[Int]): List[Int] = {
    val n = xs.length/2
    if (n == 0) xs
    else {
      def merge(xs: List[Int], ys: List[Int]): List[Int] = (xs, ys) match {
        case (List(), ys) => ys
        case (xs, List()) => xs
        case (x :: xs1, y :: ys1) =>
          if (x < y) x :: merge(xs1, ys)
          else y :: merge(xs, ys1)
      }
      val (fst, snd) = xs splitAt n
      merge(msort(fst), msort(snd))
    }
  }

  /* Старая версия со вложенностью
    def msort(xs: List[Int]): List[Int] = {
    val n = xs.length/2
    if (n == 0) xs
    else {
      def merge(xs: List[Int], ys: List[Int]): List[Int] =
        xs match {
          case Nil =>
            ys
          case x :: xs1 =>
            ys match {
              case Nil =>
                xs
              case y :: ys1 =>
                if (x < y) x :: merge(xs1, ys)
                else y :: merge(xs, ys1)
            }
        }
      val (fst, snd) = xs splitAt n
      merge(msort(fst), msort(snd))
    }
  }*/
}
