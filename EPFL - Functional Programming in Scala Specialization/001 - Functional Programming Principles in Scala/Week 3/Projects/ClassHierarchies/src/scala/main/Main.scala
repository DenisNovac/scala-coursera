package scala.main

object Main extends App {
  val t1 = new NonEmpty(3, Empty, Empty)
  println(t1)

  val t2 = t1 incl 4
  println(t2) // {.3{.4.}}

  val t3 = new NonEmpty(1, Empty, Empty)
  val t4 = t3 incl 2
  println(t4) //{{.85.}435.}

  // объединить t4 и t2
  println(t4 union t2) //


}

abstract class IntSet {
  def incl(x: Int): IntSet
  def contains(x: Int): Boolean
  def union(other: IntSet): IntSet
}

object Empty extends IntSet {
  def contains(x: Int): Boolean = false
  def incl(x: Int): IntSet = new NonEmpty(x, Empty, Empty)
  def union(other: IntSet): IntSet = other
  override def toString = "."
}

class NonEmpty(elem: Int, left: IntSet, right: IntSet) extends IntSet {

  def contains(x: Int): Boolean =
    if (x < elem) left contains x
    else if (x > elem) right contains x
    else true

  def incl(x: Int): IntSet =
    if (x < elem) new NonEmpty(elem, left incl x, right)
    else if (x > elem) new NonEmpty(elem, left, right incl x)
    else this

  def union(other: IntSet): IntSet = {
    // incl нужно вызывать в конце, иначе - бесконечная рекурсия
    // other.incl(elem).union(left).union(right)
    //other.union(left).union(right).incl(elem) // {{.3{.4.}}85{.435.}}
    ((left union right) union other) incl elem // {.3{.4{.85{.435.}}}}
  }




  override def toString = "{" + left + elem + right + "}"
}

