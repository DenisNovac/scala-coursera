object Options {
  def main(args: Array[String]): Unit = {
      val myMap = Map("a" -> 42, "b" -> 43)

      println(getMapValue(myMap, "a"))
      println()
      println(getMapValue(myMap, "c"))

  }

  def getMapValue(m: Map[String, Int], s: String): String = {
    /*
    println(m.get(s)) // то же самое, что ниже
    m get s match {
      case Some(nb) => "Value found: " + nb  // Some(42)
      case None => "No Value found" // None
    }
    */

    // то же самое:
    m.get(s).map("Value found: " + _).getOrElse("No value found")
  }
}
