package main.scala

import scala.io.Source



object Translator extends App {
  // Этот файл был перевыложен на моём гитхабе
  // Словарь полезных слов
  val in = Source.fromURL("https://raw.githubusercontent.com/DenisNovac/scala-coursera/master/001%20-%20Functional%20Programming%20Principles%20in%20Scala/Week%206/Lectures/linuxwords.txt")

  val words = in.getLines.toList filter (word => word forall (chr => chr.isLetter)) // список для чтения строк одной за другой

  val mnem = Map (  // просто ассоциации с мобильного телефона
    '2' -> "ABC", '3' -> "DEF", '4' -> "GHI", '5' -> "JKL",
    '6' -> "MNO", '7' -> "PQRS", '8' -> "TUV", '9' -> "WXYZ")

  val charCode: Map[Char, Char] =  // обратная карта
    for ((digit, str) <- mnem; ltr <- str) yield ltr -> digit

  // "Java" -> "5282"
  def wordCode(word: String): String =  // переводчик слов в цифры
    word.toUpperCase map charCode


  // "5285" -> List("Java", "Kata", "Lava", ...)
  def wordsForNum: Map[String, Seq[String]] =
    words groupBy wordCode withDefaultValue Seq()


  def encode(number: String): Set[List[String]] =
    if (number.isEmpty) Set(List())
    else {
      for {
        split <- 1 to number.length  // мы пробуем с каждым символом номера (первый, первые два, первые три)
        word <- wordsForNum(number take split)
        rest <- encode(number drop split) // кодируем остаток слова
      } yield word :: rest
    }.toSet  // из-за того, что сначала юзали рендж - привелось сначала к IndexedSeq


  def translate(number: String): Set[String] =
    encode(number) map (_ mkString (" "))

  println(wordCode("Java"))  // 5282
  println(wordsForNum("5282"))
  println(translate("7225247386"))
  /*
  HashSet(pack bird to, Scala ire to, Scala is fun, rack ah re to, pack air fun, sack air fun, pack ah re to, sack bird to, rack bird to, sack ah re to, rack air fun)
  */



}