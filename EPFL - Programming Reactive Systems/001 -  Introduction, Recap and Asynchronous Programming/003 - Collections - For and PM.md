# Коллекции

Повторение `EPFL - Functional Programming in Scala Specialization/001 - Functional Programming Principles in Scala/Week 5`.

## For-Expressions

```scala
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
  if isPrime(i + j)  // filter
} yield (i, j)  // map

```

Как происходит приведение:

- Простой for-expression:

```scala
for (x <- e1) yield e2
// транслируется в 
e1.map(x => e2)
```

- for-expression с генератором и if сразу за ним:

```scala
for (x <- e1 if f; s) yield e2
// транслируется в
for (x <- e1.withFilter(x => f); s) yield e2
```

`withFilter` - это ленивый вариант `filter`, который не выдаёт сразу лист, а просто передаёт отфильтрованные элементы наружу в map, flatMap или другой withFilter.

- for-expression с двумя генераторами:

```scala
for (x <- e1; y <- e2; s) yield e3
// транслируется в 
e1.flatMap(x => for(y <- e2; s) yield e3)
```

Если применять эти правила одно за другим - мы получим последовательность функций `flatMap`, `map` и `withFilter` и совсем избавимся от `for`.


## For-expressions и Паттерн Матчинг

Левая сторона генератора может быть паттерном:

```scala
val names = List(Name("John"), Name("Maria"), Name("Sarah"))
val ages = List(Age(12), Age(20), Age(8))

val pd: List[PersonalData] = names ++ ages

for {
  Name(n) <- pd
  Age(a) <- pd

  if n.nonEmpty
  if a == 20
} yield n  // List(John, Maria, Sarah)
```
