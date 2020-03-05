package scala.main

import java.util.NoSuchElementException

import scala.annotation.tailrec

object Main extends App {
  val list = new Cons[Int](3424, new Cons[Int](123, new Cons[Int](324324, new Nil[Int])))
  println(list.nth(0))
  println(list.nth(1))
  println(list.nth(2))
  println(list.nth(3)) // exception
}


trait List[T] {
  def isEmpty: Boolean
  def head: T
  def tail: List[T]
  def nth(n: Int): T
}

class Cons[T](val head: T, val tail: List[T]) extends List[T] {
  // val head: T и val tail: List[T] определяют абстрактные определения из трейта
  // по сути поэтому можно не писать вручную
  def isEmpty: Boolean = false

  def nth(n: Int): T = {
    @tailrec
    def iter(n: Int, acc: Int, l: List[T]): T = {
      if (n == acc) l.head
      // если элемент последний, то нельзя бросать исключение, пока не удостоверились, что это не нужный элемент
      else if (l.tail.isEmpty) throw new IndexOutOfBoundsException
      else iter(n, acc+1, l.tail)
    }
    iter(n, 0, this)
  }

}

class Nil[T] extends List[T] {
  def isEmpty: Boolean = true
  // Nothing работает, ведь это подтип всех типов, поэтому абстрактный метод всё равно переопределён
  // (Nothing - подтип T, поэтому так можно)
  def head: Nothing = throw new NoSuchElementException("Nil.head")
  def tail: Nothing = throw new NoSuchElementException("Nil.tail")
  def nth(n: Int): Nothing = throw new IndexOutOfBoundsException("Nil.nth")
}

