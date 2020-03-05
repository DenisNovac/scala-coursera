object PatternMatching {
  def main(args: Array[String]): Unit = {
    // PM используется для декомпозиции структур данных

    val someList = List()
    (someList: List[Int]) match {
      case Nil => println("Empty list")
    }

    val someList2 = List(1)
    someList2 match {
      //case x :: Nil => println("Only one element")
      case List(x) => println("Only one element")
    }

    val someList3 = List(1,2)
    someList3 match {
      //case List(x,y) => println("Two elements")
      //case x :: y :: Nil => println("Two elements") // то же, что выше

      // причём если List подходит к первому кейзу (что угодно = конец листа), то он выполнится
      // поэтому по логике их нужно поменять местами - сначала проверить 1 2 Nil, а потом
      // всё, что угодно
      //case 1 :: 2 :: x => println("Листа начинается с 1, потом идёт 2, потом что угодно")
      case 1 :: 2 :: Nil => println("Листа начинается с 1, потом идёт 2 и всё")
    }

    val someList4 = List((1,2), 3, 4, 5)
    someList4 match {
      case (x,y)::z => println("Сначала кортеж, а потом что угодно")
    }



  }

}

