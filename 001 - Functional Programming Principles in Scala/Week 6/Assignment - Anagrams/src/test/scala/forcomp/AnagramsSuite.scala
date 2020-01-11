package forcomp

import org.junit._
import org.junit.Assert.assertEquals


class AnagramsSuite {
  import Anagrams._

  @Test def `wordOccurrences: abcd (3pts)`: Unit =
    assertEquals(List(('a', 1), ('b', 1), ('c', 1), ('d', 1)), wordOccurrences("abcd"))

  @Test def `wordOccurrences: Robert (3pts)`: Unit =
    assertEquals(List(('b', 1), ('e', 1), ('o', 1), ('r', 2), ('t', 1)), wordOccurrences("Robert"))


  @Test def `sentenceOccurrences: abcd e (5pts)`: Unit =
    assertEquals(List(('a', 1), ('b', 1), ('c', 1), ('d', 1), ('e', 1)), sentenceOccurrences(List("abcd", "e")))


  @Test def `dictionaryByOccurrences.get: eat (10pts)`: Unit =
    assertEquals(Some(Set("ate", "eat", "tea")), dictionaryByOccurrences.get(List(('a', 1), ('e', 1), ('t', 1))).map(_.toSet))


  @Test def `wordAnagrams married (2pts)`: Unit =
    assertEquals(Set("married", "admirer"), wordAnagrams("married").toSet)

  @Test def `wordAnagrams player (2pts)`: Unit =
    assertEquals(Set("parley", "pearly", "player", "replay"), wordAnagrams("player").toSet)



  @Test def `subtract: lard - r (10pts)`: Unit = {
    val lard = List(('a', 1), ('d', 1), ('l', 1), ('r', 1))
    val r = List(('r', 1))
    val lad = List(('a', 1), ('d', 1), ('l', 1))
    assertEquals(lad, subtract(lard, r))
  }




  // метод должен вернуть все подсеты для одного кортежа, кроме нулевого и не исключая изначальный
  @Test def `calculate subsetForOccurrence`: Unit = {
    val test = ('a',2)
    assertEquals(List('a'->1, 'a'->2), subset(test))
  }

  // метод должен вернуть все подсеты
  @Test def `calculate subsets for many Occurrences`: Unit = {
    val test: Occurrences = List(('a',2), ('b',2))
    assertEquals(List(List('a'->1, 'a'->2), List('b'->1, 'b'->2)), subsetsForOccurrences(test))
  }

  @Test def `Check dublicate`: Unit = {
    val test: Occurrences = List(('a',2), ('b',2), ('a', 3))
    assertEquals(true, containsDub(test))
    val test2: Occurrences = List(('a',2), ('b',2))
    assertEquals(false, containsDub(test2))
    val test3: Occurrences = List(('a',2), ('a', 3))
    assertEquals(true, containsDub(test3))
  }



  @Test def `combinations: [] (8pts)`: Unit =
    assertEquals(List(Nil), combinations(Nil))

  @Test def `combinations: abba (8pts)`: Unit = {
    val abba = List(('b', 2), ('a', 2))  // проверяем и сортировку таким образом
    val abbacomb = List(
      List(),
      List(('a', 1)),
      List(('a', 2)),
      List(('b', 1)),
      List(('a', 1), ('b', 1)),
      List(('a', 2), ('b', 1)),
      List(('b', 2)),
      List(('a', 1), ('b', 2)),
      List(('a', 2), ('b', 2))
    )
    assertEquals(abbacomb.toSet, combinations(abba).toSet)
  }

  @Test def `is subset`: Unit = {
    val abc = List(('a', 2), ('b', 2), ('c', 8))
    assertEquals(true, isSubset(abc, List(('b', 2))))
    assertEquals(true, isSubset(abc, List(('b', 1))))

    assertEquals(false, isSubset(abc, List(('b',3))))
    assertEquals(false, isSubset(abc, List(('d',1))))
  }

  @Test def `subtractions`: Unit = {
    val abc = List(('a', 2), ('b', 2), ('c', 8))
    val b = List(('b', 1), ('c', 8))
    assertEquals(List(('a', 2), ('b', 1)), subtract(abc, b))
  }


  @Test def `sentence anagrams: [] (10pts)`: Unit = {
    val sentence = List()
    assertEquals(List(Nil), sentenceAnagrams(sentence))
  }

  @Test def `sentence anagrams: Linux rulez (10pts)`: Unit = {
    val sentence = List("Linux", "rulez")
    //val sentence = List("Yes", "man")
    //val sentence = List("i", "love", "you")
    val anas = List(
      List("Rex", "Lin", "Zulu"),
      List("nil", "Zulu", "Rex"),
      List("Rex", "nil", "Zulu"),
      List("Zulu", "Rex", "Lin"),
      List("null", "Uzi", "Rex"),
      List("Rex", "Zulu", "Lin"),
      List("Uzi", "null", "Rex"),
      List("Rex", "null", "Uzi"),
      List("null", "Rex", "Uzi"),
      List("Lin", "Rex", "Zulu"),
      List("nil", "Rex", "Zulu"),
      List("Rex", "Uzi", "null"),
      List("Rex", "Zulu", "nil"),
      List("Zulu", "Rex", "nil"),
      List("Zulu", "Lin", "Rex"),
      List("Lin", "Zulu", "Rex"),
      List("Uzi", "Rex", "null"),
      List("Zulu", "nil", "Rex"),
      List("rulez", "Linux"),
      List("Linux", "rulez")
    )
    assertEquals(anas.toSet, sentenceAnagrams(sentence).toSet)
    //assertEquals(1, getDictionary(sentence))
  }


  @Rule def individualTestTimeout = new org.junit.rules.Timeout(10 * 1000)
}
