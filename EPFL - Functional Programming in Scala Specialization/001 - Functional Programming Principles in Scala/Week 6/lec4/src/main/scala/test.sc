val romanNumerals = Map("I" -> 1, "II" -> 2, "X" -> 10)
val capitals = Map("US" -> "Washington")

romanNumerals("I")

//capitals("Random")

capitals get "Random"
capitals get "US"


def showCapital(country: String) = capitals.get(country) match {
  case Some(capital) => capital
  case None => "missing data"
}

showCapital("US")
showCapital("Random")


val fruit = List("apple", "pear", "orange", "pineapple")
fruit sortWith (_.length < _.length)
fruit.sorted

fruit groupBy (_.head)


val capitalsWithDefault = capitals withDefaultValue "<unknown>"
capitalsWithDefault("Random")