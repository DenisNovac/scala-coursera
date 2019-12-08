package funsets

import scala.annotation.tailrec

/**
 * 2. Purely Functional Sets.
 */
trait FunSets extends FunSetsInterface {
  /**
   * We represent a set by its characteristic function, i.e.
   * its `contains` predicate.
   *
   * Сетом мы не считаем просто набор элементов. Мы охватываем такие сеты, как
   * "все положительные числа". Их невозможно перечислить, но мы можем легко вычислить,
   * относится ли "-1" к этому сету. Поэтому мы определяем функциональные сеты.
   */
  override type FunSet = Int => Boolean

  /**
   * Indicates whether a set contains a given element.
   */
  def contains(s: FunSet, elem: Int): Boolean = s(elem)

  /**
   * Returns the set of the one given element.
   * Тут всё просто. Сет одного элемента. Чтобы сравнить, содержится ли элемент в таком сете,
   * нужно сравнить этот элемент с единственным элементом в нём.
   */
  def singletonSet(elem: Int): FunSet = (x: Int) => if (x==elem) true else false


  /**
   * Returns the union of the two given sets,
   * the sets of all elements that are in either `s` or `t`.
   */
  def union(s: FunSet, t: FunSet): FunSet = (x: Int) => if (contains(s,x) | contains(t,x)) true else false

  /**
   * Returns the intersection of the two given sets,
   * the set of all elements that are both in `s` and `t`.
   */
  def intersect(s: FunSet, t: FunSet): FunSet = (x: Int) => if (contains(s,x) & contains(t,x)) true else false

  /**
   * Returns the difference of the two given sets,
   * the set of all elements of `s` that are not in `t`.
   */
  def diff(s: FunSet, t: FunSet): FunSet = (x: Int) => if (contains(s,x) & !contains(t,x)) true else false

  /**
   * Returns the subset of `s` for which `p` holds.
   */
  def filter(s: FunSet, p: Int => Boolean): FunSet = (x: Int) => if (contains(s, x) & p(x)) true else false


  /**
   * The bounds for `forall` and `exists` are +/- 1000.
   */
  val bound = 1000

  /**
   * Returns whether all bounded integers within `s` satisfy `p`.
   *
   * Проверяет, для всех ли элементов сета справедливо p
   */
  def forall(s: FunSet, p: Int => Boolean): Boolean = {
    @tailrec
    def iter(a: Int): Boolean = {
      if (a == bound) true
      // так мы проверяем даже элементы, которых нет в сете
      //else if (!contains(filter(s,p),a)) false
      else if (contains(s,a) && !contains(filter(s,p),a)) false
      else iter(a+1)
    }
    iter(0)
  }

  /**
   * Returns whether there exists a bounded integer within `s`
   * that satisfies `p`.
   *
   * Проверяет, есть ли хоть один элемент, удовлетворяющий p
   */
  def exists(s: FunSet, p: Int => Boolean): Boolean = {
    @tailrec
    def iter(a: Int): Boolean = {
      if (a == bound) false
      // так мы проверяем даже элементы, которых нет в сете
      //else if (!contains(filter(s,p),a)) false
      else if (contains(s,a) && contains(filter(s,p),a)) true
      else iter(a+1)
    }
    iter(0)
  }

  /**
   * Returns a set transformed by applying `f` to each element of `s`.
   */
  def map(s: FunSet, f: Int => Int): FunSet = {
    @tailrec
    def iter(a: Int, sr: FunSet): FunSet = {
      if (a == bound) sr
      else if (contains(s,a)) iter(  a+1, union(sr, singletonSet(f(a)))  )
      else iter(a+1, sr)
    }
    iter(0, filter(singletonSet(0),(x: Int) => false))

  }

  /**
   * Displays the contents of a set
   */
  def toString(s: FunSet): String = {
    val xs = for (i <- -bound to bound if contains(s, i)) yield i
    xs.mkString("{", ",", "}")
  }

  /**
   * Prints the contents of a set on the console.
   */
  def printSet(s: FunSet): Unit = {
    println(toString(s))
  }
}

object FunSets extends FunSets
