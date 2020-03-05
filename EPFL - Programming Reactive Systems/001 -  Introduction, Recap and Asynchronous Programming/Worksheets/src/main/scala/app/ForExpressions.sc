

def isPrime(i: Int): Boolean = i match {
  case x if x > 0 => true
  case _ => false
}

/** Вместо комбинации flatMap, filter и map */

(1 until 10) flatMap (i =>
  (1 until i) filter (j => isPrime(i + j)) map
    (j => (i, j))
  )

/** Можно использовать более читабельную конструкцию */

for {
  i <- 1 until 10  // flatMap
  j <- 1 until i  // flatMap
  if isPrime(i + j)  // filter (lazy)
} yield (i, j)  // map

class PersonalData
case class Name(name: String) extends PersonalData
case class Age(age: Int) extends PersonalData


val names = List(Name("John"), Name("Maria"), Name("Sarah"))
val ages = List(Age(12), Age(20), Age(8))

val pd: List[PersonalData] = names ++ ages

for {
  Name(n) <- pd
  Age(a) <- pd

  if n.nonEmpty
  if a == 20
} yield n  // List(John, Maria, Sarah)

