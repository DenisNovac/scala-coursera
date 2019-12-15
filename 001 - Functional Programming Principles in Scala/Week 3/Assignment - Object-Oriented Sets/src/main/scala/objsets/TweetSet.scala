package objsets

import java.util.NoSuchElementException

import TweetReader._

import scala.annotation.tailrec
import scala.runtime.Nothing$

/**
 * A class to represent tweets.
 */
class Tweet(val user: String, val text: String, val retweets: Int) {
  override def toString: String =
    "User: " + user + "\n" +
    "Text: " + text + " [" + retweets + "]"
}

/**
 * This represents a set of objects of type `Tweet` in the form of a binary search
 * tree. Every branch in the tree has two children (two `TweetSet`s). There is an
 * invariant which always holds: for every branch `b`, all elements in the left
 * subtree are smaller than the tweet at `b`. The elements in the right subtree are
 * larger.
 *
 * Note that the above structure requires us to be able to compare two tweets (we
 * need to be able to say which of two tweets is larger, or if they are equal). In
 * this implementation, the equality / order of tweets is based on the tweet's text
 * (see `def incl`). Hence, a `TweetSet` could not contain two tweets with the same
 * text from different users.
 *
 *
 * The advantage of representing sets as binary search trees is that the elements
 * of the set can be found quickly. If you want to learn more you can take a look
 * at the Wikipedia page [1], but this is not necessary in order to solve this
 * assignment.
 *
 * [1] http://en.wikipedia.org/wiki/Binary_search_tree
 */
abstract class TweetSet extends TweetSetInterface {

  /**
   * This method takes a predicate and returns a subset of all the elements
   * in the original set for which the predicate is true.
   *
   * Question: Can we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
  def filter(p: Tweet => Boolean): TweetSet

  /**
   * This is a helper method for `filter` that propagetes the accumulated tweets.
   */
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet

  /**
   * Returns a new `TweetSet` that is the union of `TweetSet`s `this` and `that`.
   *
   * Question: Should we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
  def union(that: TweetSet): TweetSet

  /**
   * Returns the tweet from this set which has the greatest retweet count.
   *
   * Calling `mostRetweeted` on an empty set should throw an exception of
   * type `java.util.NoSuchElementException`.
   *
   * Question: Should we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
  def mostRetweeted: Tweet
  def mostRetweeted(t: Tweet): Tweet

  /**
   * Returns a list containing all tweets of this set, sorted by retweet count
   * in descending order. In other words, the head of the resulting list should
   * have the highest retweet count.
   *
   * Hint: the method `remove` on TweetSet will be very useful.
   * Question: Should we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
  def descendingByRetweet: TweetList

  /**
   * The following methods are already implemented
   */

  /**
   * Returns a new `TweetSet` which contains all elements of this set, and the
   * the new element `tweet` in case it does not already exist in this set.
   *
   * If `this.contains(tweet)`, the current set is returned.
   */
  def incl(tweet: Tweet): TweetSet

  /**
   * Returns a new `TweetSet` which excludes `tweet`.
   */
  def remove(tweet: Tweet): TweetSet

  /**
   * Tests if `tweet` exists in this `TweetSet`.
   */
  def contains(tweet: Tweet): Boolean

  /**
   * This method takes a function and applies it to every element in the set.
   */
  def foreach(f: Tweet => Unit): Unit
}

class Empty extends TweetSet {
  def filter(p: Tweet => Boolean): TweetSet = this

  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet = acc

  def union(that: TweetSet): TweetSet = that

  def mostRetweeted: Tweet = throw new NoSuchElementException("No elements in empty set")

  def mostRetweeted(t: Tweet): Tweet = throw new NoSuchElementException("No elements in empty set")

  def descendingByRetweet: TweetList = throw new NoSuchElementException("No elements in empty set")

  /**
   * The following methods are already implemented
   */

  def contains(tweet: Tweet): Boolean = false

  def incl(tweet: Tweet): TweetSet = new NonEmpty(tweet, new Empty, new Empty)

  def remove(tweet: Tweet): TweetSet = this

  def foreach(f: Tweet => Unit): Unit = ()
}

class NonEmpty(elem: Tweet, left: TweetSet, right: TweetSet) extends TweetSet {

  def filter(p: Tweet => Boolean): TweetSet = filterAcc(p, new Empty)

  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet = {
    /*
    Так нельзя, т.к. каждая отдельная команда работает через new
    и в результате мы не воздействуем на оригинальный acc по ссылке,
    а создаём новый объект.

    if (p(elem)) acc.incl(elem)
    left.filterAcc(p, acc)
    right.filterAcc(p, acc)*/

    if (p(elem)) left.filterAcc(p, right.filterAcc(p, acc.incl(elem)))
    else left.filterAcc(p, right.filterAcc(p, acc))
  }

  def union(that: TweetSet): TweetSet =
    // тот же принцип - передаём одно в другое
    left.union(right.union(that.incl(elem)))

  def mostRetweeted: Tweet = {
    mostRetweeted(new Tweet(user = "", text = "", retweets = 0))
  }

  def mostRetweeted(acc: Tweet): Tweet = {
    // сохраняем величину и прогоняем ветки по очереди
    // этот метод - вроде итератора, но является перегрузкой
    def rightIter(acc: Tweet): Tweet = {
      if (elem.retweets > acc.retweets) {
        try {
          right.mostRetweeted(elem)
        } catch {
          case e: NoSuchElementException => elem
        }
      } else {
        try {
          right.mostRetweeted(acc)
        } catch {
          case e: NoSuchElementException => acc
        }
      }
    }

    def leftIter(acc: Tweet): Tweet = {
      if (elem.retweets > acc.retweets) {
        try {
          left.mostRetweeted(elem)
        } catch {
          case e: NoSuchElementException => elem
        }
      } else {
        try {
          left.mostRetweeted(acc)
        } catch {
          case e: NoSuchElementException => acc
        }
      }
    }

    val r = rightIter(acc)
    val l = leftIter(r)
    if (r.retweets > l.retweets) r
    else l
  }

  /**
   * Метод основан на преобразовании одной структуры в другую. Опирается на mostRetweeted.
   * Проходим по дереву - берём самый частый элемент (справа, бинарное дерево ж) и затем
   * удаляем этот элемент методом remove.
   *
   * Remove даёт новый сет, уже в нём ищем следующий mostRetweeted.
   * */
  def descendingByRetweet: TweetList = {

    def iter(l: TweetList, s: TweetSet): TweetList = {
      try {
        val t = s.mostRetweeted // бросит эксепшен на Empty лист
        if (t.retweets == 0) l // вот так стак оверфлоу не ловится
        else
        //println(t)
          //iter(new Cons(t,l), s.remove(t)) получится по возрастанию
          new Cons(t, iter(l, s.remove(t)))  // а вот так по убыванию
      } catch {
        case e: NoSuchElementException => l
      }
    }

    iter(Nil, this)
  }



  /**
   * The following methods are already implemented
   */

  def contains(x: Tweet): Boolean =
    if (x.text < elem.text) left.contains(x)
    else if (elem.text < x.text) right.contains(x)
    else true

  def incl(x: Tweet): TweetSet = {
    if (x.text < elem.text) new NonEmpty(elem, left.incl(x), right)
    else if (elem.text < x.text) new NonEmpty(elem, left, right.incl(x))
    else this
  }

  def remove(tw: Tweet): TweetSet =
    if (tw.text < elem.text) new NonEmpty(elem, left.remove(tw), right)
    else if (elem.text < tw.text) new NonEmpty(elem, left, right.remove(tw))
    else left.union(right)

  def foreach(f: Tweet => Unit): Unit = {
    f(elem)
    left.foreach(f)
    right.foreach(f)
  }



}

trait TweetList {
  def head: Tweet
  def tail: TweetList
  def isEmpty: Boolean
  def foreach(f: Tweet => Unit): Unit =
    if (!isEmpty) {
      f(head)
      tail.foreach(f)
    }
}

object Nil extends TweetList {
  def head = throw new java.util.NoSuchElementException("head of EmptyList")
  def tail = throw new java.util.NoSuchElementException("tail of EmptyList")
  def isEmpty = true
}

class Cons(val head: Tweet, val tail: TweetList) extends TweetList {
  def isEmpty = false
}


object GoogleVsApple {
  val google = List("android", "Android", "galaxy", "Galaxy", "nexus", "Nexus")
  val apple = List("ios", "iOS", "iphone", "iPhone", "ipad", "iPad")
  val tweets: TweetSet = TweetReader.allTweets
  val googleTweets: TweetSet = tweets.filter(x => google.exists(x.text.contains))
  val appleTweets: TweetSet = tweets.filter(x => apple.exists(x.text.contains))

  /**
   * A list of all tweets mentioning a keyword from either apple or google,
   * sorted by the number of retweets.
   */
  val googleOrApple = googleTweets.union(appleTweets)

  val trending: TweetList = googleOrApple.descendingByRetweet
}

object Main extends App {
  // Print the trending tweets
  GoogleVsApple.trending foreach println
  //GoogleVsApple.googleOrApple foreach println
}
