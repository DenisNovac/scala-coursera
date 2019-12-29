

object Main extends App {
  val l = List(1,2,3)
  println(l) // List(1, 2, 3)
  //println(l.updated(1,3)) // List(1, 3, 3)
  //println(l.updated(0,3)) // List(3, 2, 3)
  ////println(last(l))
  //println(init(l))
  println(reverse(l))

  val ll = List(0,1,2,3,4,5,6)
  println(removeAt(ll, 5))



  def last[T](xs: List[T]): T = xs match {
    case List() => throw new Error("last of empty list")
    case List(x) => x
    case y :: ys => last(ys)
  }

  def init[T](xs: List[T]): List[T] = xs match {
    case List() => throw new Error("init of empty list")
    case List(x) => Nil  // вместо последнего элемента возвращаем Nil, чем и срезаем его
    case y :: ys => y :: init(ys) // возвратить нужно именно лист, поэтому тут ::
  }

  def reverse[T](xs: List[T]): List[T] = xs match {
    case List() => xs
    case y :: ys => reverse(ys) ++ List(y)
  }

  def removeAt[T](xs: List[T], n: Int): List[T] = {
    def iter(xs: List[T], n: Int, acc: Int): List[T] = xs match {
      case List() => Nil
      case y :: ys => {
        if (acc == n) iter(ys, n, acc+1)
        else y :: iter(ys, n, acc+1)
      }
    }
    iter(xs, n, 0)
  }
}


