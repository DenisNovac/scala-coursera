package forcomp

import forcomp.Anagrams.dictionaryByOccurrences

import scala.annotation.tailrec

object Anagrams extends AnagramsInterface {

  /** A word is simply a `String`. */
  type Word = String

  /** A sentence is a `List` of words. */
  type Sentence = List[Word]

  /** `Occurrences` is a `List` of pairs of characters and positive integers saying
   *  how often the character appears.
   *  This list is sorted alphabetically w.r.t. to the character in each pair.
   *  All characters in the occurrence list are lowercase.
   *
   *  Any list of pairs of lowercase characters and their frequency which is not sorted
   *  is **not** an occurrence list.
   *
   *  Note: If the frequency of some character is zero, then that character should not be
   *  in the list.
   */
  type Occurrences = List[(Char, Int)]

  /** The dictionary is simply a sequence of words.
   *  It is predefined and obtained as a sequence using the utility method `loadDictionary`.
   */
  val dictionary: List[Word] = Dictionary.loadDictionary

  /** Converts the word into its character occurrence list.
   *
   *  Note: the uppercase and lowercase version of the character are treated as the
   *  same character, and are represented as a lowercase character in the occurrence list.
   *
   *  Note: you must use `groupBy` to implement this method!
   */

  def wordOccurrences(w: Word): Occurrences = {
    val lowerCaseNoSpacesSeq = w.toLowerCase.filter((c: Char) => c != ' ').toSeq
    val groupByCharacter = lowerCaseNoSpacesSeq.groupBy((c: Char) => c)  // Карта вида (e -> ee, a -> a, l -> llll ...)
    val occurrencesMap = groupByCharacter.map{case (c,s) => c->s.length}  // маппим карту в другую карту уже по длинам
    occurrencesMap.toList.sorted  // перегоняем в лист (получится лист вида ((e->2), (a->1))) и сортируем
  }

  /** Converts a sentence into its character occurrence list. */
  def sentenceOccurrences(s: Sentence): Occurrences = {
    val onlyCharacters = s.mkString("").filter(_.isLetter)
    wordOccurrences(onlyCharacters)
  }

  /** The `dictionaryByOccurrences` is a `Map` from different occurrences to a sequence of all
   *  the words that have that occurrence count.
   *  This map serves as an easy way to obtain all the anagrams of a word given its occurrence list.
   *
   *  For example, the word "eat" has the following character occurrence list:
   *
   *     `List(('a', 1), ('e', 1), ('t', 1))`
   *
   *  Incidentally, so do the words "ate" and "tea".
   *
   *  This means that the `dictionaryByOccurrences` map will contain an entry:
   *
   *    List(('a', 1), ('e', 1), ('t', 1)) -> Seq("ate", "eat", "tea")
   *
   */

  lazy val dictionaryByOccurrences: Map[Occurrences, List[Word]] =
    dictionary.groupBy((w: Word) => wordOccurrences(w)) withDefaultValue List()


  /** Returns all the anagrams of a given word. */
  def wordAnagrams(word: Word): List[Word] = dictionaryByOccurrences(wordOccurrences(word))



  /** Returns the list of all subsets of the occurrence list.
   *  This includes the occurrence itself, i.e. `List(('k', 1), ('o', 1))`
   *  is a subset of `List(('k', 1), ('o', 1))`.
   *  It also include the empty subset `List()`.
   *
   *  Example: the subsets of the occurrence list `List(('a', 2), ('b', 2))` are:
   *
   *    List(
   *      List(),
   *      List(('a', 1)),
   *      List(('a', 2)),
   *      List(('b', 1)),
   *      List(('a', 1), ('b', 1)),
   *      List(('a', 2), ('b', 1)),
   *      List(('b', 2)),
   *      List(('a', 1), ('b', 2)),
   *      List(('a', 2), ('b', 2))
   *    )
   *
   *  Note that the order of the occurrence list subsets does not matter -- the subsets
   *  in the example above could have been displayed in some other order.
   */


  // вычислить подсеты каждого сета отдельно
  // перечислить их комбинации
  def combinations(occurrences: Occurrences): List[Occurrences] = {
    val variances = subsetsForOccurrences(occurrences)
    val flat = variances.flatten   // List((a,1), (a,2), (b,1), (b,2))
    //val all = flat.toSet.subsets().map(_.toList).toList  // все возможные комбинации вместе с дубликатами вроде List((a,1), (a,2)) и List((a,1), (a,2), (b,1), (b,2))
    //val all = flat.combinations(2).toList ::: flat.combinations(1).toList ::: List(Nil)
    val all = List(Nil) ::: (for (n <- 1 until flat.length) yield flat.combinations(n).toList).flatten.toList

    all.filterNot((o: Occurrences) => containsDub(o)) map (_.sorted)// прогоняем через фильтр, отсеиваем дубликаты и сортируем
  }

  def subsetsForOccurrences(occurrences: Occurrences): List[Occurrences] =
    /**
     * Сборка всех подсетов в виде: (отдельный лист для каждой occurrences типа char->int)
     * List(List((a,1), (a,2)), List((b,1), (b,2)))
     */
    occurrences match {
      case Nil => Nil
      case x :: Nil => subset(x) :: Nil
      case x :: xs => subset(x) :: subsetsForOccurrences(xs)
    }

  // просчитывает все подсеты для одного кортежа
  // a -> 2 даст a->2, a->1
  def subset(occurence: (Char, Int)): Occurrences = {
    for (i <- 1 to occurence._2)
      yield occurence._1->i
    }.toList

  def containsDub(combination: Occurrences): Boolean =
    /**
     * Проверка на наличие дубликата
     * */
    combination.groupBy(_._1).exists(_._2.length>1)
    // вот такую простыню заменяет оператор _
    // x.groupBy((t:(Char, Int))=>t._1).exists((t:(Char, List[(Char, Int)]))=>t._2.length>1)





  /** Subtracts occurrence list `y` from occurrence list `x`.
   *
   *  The precondition is that the occurrence list `y` is a subset of
   *  the occurrence list `x` -- any character appearing in `y` must
   *  appear in `x`, and its frequency in `y` must be smaller or equal
   *  than its frequency in `x`.
   *
   *  Note: the resulting value is an occurrence - meaning it is sorted
   *  and has no zero-entries.
   */
  def subtract(x: Occurrences, y: Occurrences): Occurrences =
    // проверка if нужна, чтобы не пытаться вычитать из x те элементы, которых нет в y (y.filter упадёт)
    x map ((t:(Char, Int)) => if(y.exists(_._1 == t._1)) ((t._1 -> (t._2 - y.filter(_._1 == t._1).head._2))) else t) filterNot(_._2 == 0)

  @tailrec
  def isSubset(x: Occurrences, sub: Occurrences): Boolean =
    if (x.isEmpty & sub.nonEmpty) false  // не бывает подсетов у пустоты
    else
      sub match {
        case Nil => true
        case y :: Nil => x.exists((t:(Char, Int)) => (t._1 == y._1) & (t._2 >= y._2) )
        case y :: ys => if (x.exists((t:(Char, Int)) => (t._1 == y._1) & (t._2 >= y._2) )) isSubset(x, ys) else false
      }

  /** Returns a list of all anagram sentences of the given sentence.
   *
   *  An anagram of a sentence is formed by taking the occurrences of all the characters of
   *  all the words in the sentence, and producing all possible combinations of words with those characters,
   *  such that the words have to be from the dictionary.
   *
   *  The number of words in the sentence and its anagrams does not have to correspond.
   *  For example, the sentence `List("I", "love", "you")` is an anagram of the sentence `List("You", "olive")`.
   *
   *  Also, two sentences with the same words but in a different order are considered two different anagrams.
   *  For example, sentences `List("You", "olive")` and `List("olive", "you")` are different anagrams of
   *  `List("I", "love", "you")`.
   *
   *  Here is a full example of a sentence `List("Yes", "man")` and its anagrams for our dictionary:
   *
   *    List(
   *      List(en, as, my),
   *      List(en, my, as),
   *      List(man, yes),
   *      List(men, say),
   *      List(as, en, my),
   *      List(as, my, en),
   *      List(sane, my),
   *      List(Sean, my),
   *      List(my, en, as),
   *      List(my, as, en),
   *      List(my, sane),
   *      List(my, Sean),
   *      List(say, men),
   *      List(yes, man)
   *    )
   *
   *  The different sentences do not have to be output in the order shown above - any order is fine as long as
   *  all the anagrams are there. Every returned word has to exist in the dictionary.
   *
   *  Note: in case that the words of the sentence are in the dictionary, then the sentence is the anagram of itself,
   *  so it has to be returned in this list.
   *
   *  Note: There is only one anagram of an empty sentence.
   */
  // List[(Occurrences, List[Word])]
  def sentenceAnagrams(sentence: Sentence): List[Sentence] = {
    if (sentence.isEmpty) List(List())
    else {

      val occ = sentenceOccurrences(sentence) // List((e,1), (i,1), (l,2), (n,1), (r,1), (u,2), (x,1), (z,1))
      val dict: List[(Occurrences, List[Word])] = getDictionaryFromOccurences(occ)

      val words = for (d <- dict) yield d._2  // просто все слова

      // все комбинации слов длиной (от 1 слова до 4 в массиве)
      val all_com =
        (for (n <- 1 to 4) yield words.flatten.combinations(n).toList.filter(sentenceOccurrences(_) == occ))
          .toList.flatten

      (for (combination <- all_com) yield combination.permutations.toList).flatten

      // permutations - перестановки. Комбинации включают только уникальные вхождения,
      // а перестановки - уникальные вхождения по индексам
    }
  }


  def getDictionaryFromOccurences(occ: Occurrences): List[(Occurrences, List[Word])] = {for {
      c <- combinations(occ)
  } yield c -> dictionaryByOccurrences(c) }.filter(_._2.nonEmpty)


  //def sentenceAnagrams(sentence: Sentence): List[Sentence] = ???
}

object Dictionary {
  def loadDictionary: List[String] = {
    val wordstream = Option {
      getClass.getResourceAsStream(List("forcomp", "linuxwords.txt").mkString("/", "/", ""))
    } getOrElse {
      sys.error("Could not load word list, dictionary file not found")
    }
    try {
      val s = scala.io.Source.fromInputStream(wordstream)
      s.getLines.toList
    } catch {
      case e: Exception =>
        println("Could not load word list: " + e)
        throw e
    } finally {
      wordstream.close()
    }
  }
}
