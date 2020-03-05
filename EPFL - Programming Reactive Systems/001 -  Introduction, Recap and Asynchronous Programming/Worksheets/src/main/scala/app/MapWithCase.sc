
// лист кортежей
type Binding = (Int, String)
val l = List[Binding](1 -> "Josh", 2 -> "Josh", 3 -> "Stan")


val newl = l map {
  case (i, "Josh") => 0 -> "Josh"
  case (i, s) => i -> s
}

newl  // сделает для всех Josh-ей пару 0:  List((0,Josh), (0,Josh), (3,Stan))

/** Почему в map можно передавать такие конструкции? */
/** Какой тип у штук вроде { case x => x }? */

// тип case - это функция
val f: String=> String = { case "ping" => "pong" }
f("ping")
//f("abc")  // match error

/** Хотелось бы заранее узнавать, что матч провалится, а не в рантайме */

val f2: PartialFunction[String, String] = { case "ping" => "pong" }
f2.isDefinedAt("ping")  // true
f2.isDefinedAt("abc") // false

/** PartialFunction позволяет проверить, определена ли функция для выбранного аргумента */

// f2("abc")  // match error



