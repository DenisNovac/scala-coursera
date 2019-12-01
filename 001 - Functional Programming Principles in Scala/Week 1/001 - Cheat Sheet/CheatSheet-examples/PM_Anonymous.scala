object PM_Anonymous {
  def main(args: Array[String]): Unit = {

    val pairs: List[(Char, Int)] = ('a', 2) :: ('b', 3) :: Nil
    // этот лист будет состоять только из чаров листа выше
    val chars: List[Char] = pairs.map(p => p match {
      case (ch, num) => ch
    })
    println(chars) // List(a,b)

    // то же самое
    val chars2: List[Char] = pairs map {
      case (ch, num) => ch
    }
    println(chars2)
  }
}


