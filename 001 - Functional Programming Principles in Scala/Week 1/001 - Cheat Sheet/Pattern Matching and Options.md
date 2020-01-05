# Pattern Matching (сопоставление с образцом)

Это механизм сравнения значений с определённым примером. При успешном сравнении значение может быть разложено на составные части. Это более мощная версия `switch` из Java, которая может быть использована для декомпозиции структур данных и классов (в частности, возможно достать аргументы, переданные в конструктор). Для использования в конструкции `x match`, этот самый x должен быть экземпляром `case` класса. 

```scala 
import scala.util.Random

val x: Int = Random.nextInt(10)

x match {
  case 0 => "zero"
  case 1 => "one"
  case 2 => "two"
  case _ => "many" // дефолтный вариант
}

```

Пример работы со структурами данных:

```scala
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

```

## Options 

PM можно использовать для значений `Option`. Некоторые функции (вроде Map.get) возвращают значение типа `Option[T]`, которое либо тип `Some[T]`, либо `None`:

```scala
def main(args: Array[String]): Unit = {
	val myMap = Map("a" -> 42, "b" -> 43)

	println(getMapValue(myMap, "a"))
	println()
	println(getMapValue(myMap, "c"))

}

def getMapValue(m: Map[String, Int], s: String): String = {
	/*
	println(m.get(s)) // то же самое, что ниже
	m get s match {
	  case Some(nb) => "Value found: " + nb  // Some(42)
	  case None => "No Value found" // None
	}
	*/

	// то же самое:
	m.get(s).map("Value found: " + _).getOrElse("No value found")
}

```

Почти всегда при работе со значением типа `Option` доступен упрощённый вариант записи PM, написанный через комбинаторные методы класса `Option`: `map` и `getOrElse`.


## PM в Анонимных функциях

PM часто используется в анонимных функциях. 

```scala
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

```

Поэтому во втором случае анонимную функцию p => p match {case ...} можно заменить просто на {case ...}.
























