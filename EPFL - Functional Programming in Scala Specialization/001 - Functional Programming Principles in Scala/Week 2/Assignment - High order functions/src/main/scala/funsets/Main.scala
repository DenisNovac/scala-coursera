package funsets

object Main extends App {
  import FunSets._
  println(contains(singletonSet(1), 1))
  val singl = singletonSet(2)
  println(contains(singl, 4))

  val twoNumbers = union(singletonSet(1), singletonSet(2))
  val threeNumbers = union(union(singletonSet(1), singletonSet(2)), singletonSet(3))

  println(contains(threeNumbers, 3))

  println()
  println(intersect(twoNumbers, threeNumbers)(3)) // 3 нет в одном из сетов, это не пересечение
  println(diff(threeNumbers, twoNumbers)(3)) // 3 есть в первом, но нет во втором

  // отфильтровали сет из трёх чисел, забрав только чётные
  def even(x: Int) = if (x % 2 == 0) true else false

  printSet(threeNumbers)
  val filteredSet = filter(threeNumbers, even)
  printSet(filteredSet)
  println(contains(filteredSet,2))
  println(contains(filteredSet, 3))

  // не все элементы threeNumbers чётные
  println(forall(threeNumbers, even))
  // все элементы filteredSet чётные
  println(forall(filteredSet, even))
  // проверяет, есть ли в сете хоть один чётный элемент
  println(exists(threeNumbers, even))
  println(exists(singletonSet(1), even))

  // костыль для создания пустого сета
  printSet( filter(singletonSet(0),(x: Int) => false) )

}
