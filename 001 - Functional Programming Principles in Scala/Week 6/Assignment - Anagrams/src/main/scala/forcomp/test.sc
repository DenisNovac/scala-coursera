import forcomp.Anagrams.{Occurrences, Word, dictionary, wordOccurrences}

val w = "abcd hello hello"


val lowerCaseNoSpacesSeq = w.toLowerCase.filter((c: Char) => c != ' ').toSeq
val groupByCharacter = lowerCaseNoSpacesSeq.groupBy((c: Char) => c)
val occurrencesMap = groupByCharacter.map{case (c,s) => c->s.length}
occurrencesMap.toList.sorted
