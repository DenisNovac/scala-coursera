# Коллекции

## Базовые коллекции

- Iterable (коллекции, по которым можно итерировать);
- Seq (упорядоченные последовательности;
- Set;
- Map (структура для поиска данных).

## Неизменяемые коллекции

- List (ссылочный лист с быстрым доступом);

```scala
val fruitList = List("apples", "oranges", "pears")
// Alternative syntax for lists
val fruit = "apples" :: "oranges" :: ("tomatoes" :: ("pears":: Nil))// parens optional, :: is right-associative
print(fruit) // List(apples, oranges, tomatoes, pears)

fruit.head   // "apples"
fruit.tail   // List(oranges, tomatoes, pears)
val empty = List()
val empty = Nil
```

- Stream (то же что лист, но хвост вычисляется только по запросу);

- Vector (массиво-подобный тип, реализованный в виде дерева блоков, позволяет быстрый доступ к случайному элементу (random access));

```scala
val nums = Vector("louis", "frank", "hiromi")
nums(1)                     // element at index 1, returns "frank", complexity O(log(n))
nums.updated(2, "helena")   // new vector with a different string at index 2, complexity O(log(n))
```

- Range (упорядоченная последовательность Int с равными промежутками);

```scala
val r: Range = 1 until 5 // 1, 2, 3, 4
val s: Range = 1 to 5    // 1, 2, 3, 4, 5
1 to 10 by 3  // 1, 4, 7, 10
6 to 1 by -2  // 6, 4, 2
```

- String (java тип, неявно конвертируемый в последовательность чаров, так что можно представлять каждый String как Seq[Char]);

```scala
val s = "Hello World"
s filter (c => c.isUpper) // returns "HW"; strings can be treated as Seq[Char]
```

- Set (коллекция без дубликатов);

```scala
val fruitSet = Set("apple", "banana", "pear", "banana")
fruitSet.size    // returns 3: there are no duplicates, only one banana
```


## Изменяемые коллекции

- Array (Scala массивы это нативные JVM массивы в runtime, поэтому они очень производительны);
- Maps, Sets (Scala имеет изменяемые версии карт и сетов, но они должны быть использованы только если наблюдаются проблемы с производительностью неизменяемых типов)


## Операции на последовательностях

```scala
val xs = List(...)
xs.length   // number of elements, complexity O(n)
xs.last     // last element (exception if xs is empty), complexity O(n)
xs.init     // all elements of xs but the last (exception if xs is empty), complexity O(n)
xs take n   // first n elements of xs
xs drop n   // the rest of the collection after taking n elements
xs(n)       // the nth element of xs, complexity O(n)
xs ++ ys    // concatenation, complexity O(n)
xs.reverse  // reverse the order, complexity O(n)
xs updated(n, x)  // same list than xs, except at index n where it contains x, complexity O(n)
xs indexOf x      // the index of the first element equal to x (-1 otherwise)
xs contains x     // same as xs indexOf x >= 0
xs filter p       // returns a list of the elements that satisfy the predicate p
xs filterNot p    // filter with negated p 
xs partition p    // same as (xs filter p, xs filterNot p)
xs takeWhile p    // the longest prefix consisting of elements that satisfy p
xs dropWhile p    // the remainder of the list after any leading element satisfying p have been removed
xs span p         // same as (xs takeWhile p, xs dropWhile p)

List(x1, ..., xn) reduceLeft op    // (...(x1 op x2) op x3) op ...) op xn
List(x1, ..., xn).foldLeft(z)(op)  // (...( z op x1) op x2) op ...) op xn
List(x1, ..., xn) reduceRight op   // x1 op (... (x{n-1} op xn) ...)
List(x1, ..., xn).foldRight(z)(op) // x1 op (... (    xn op  z) ...)

xs exists p    // true if there is at least one element for which predicate p is true
xs forall p    // true if p(x) is true for all elements
xs zip ys      // returns a list of pairs which groups elements with same index together
xs unzip       // opposite of zip: returns a pair of two lists
xs.flatMap f   // applies the function to all elements and concatenates the result
xs.sum         // sum of elements of the numeric collection
xs.product     // product of elements of the numeric collection
xs.max         // maximum of collection
xs.min         // minimum of collection
xs.flatten     // flattens a collection of collection into a single-level collection
xs groupBy f   // returns a map which points to a list of elements
xs distinct    // sequence of distinct entries (removes duplicates)

x +: xs  // creates a new collection with leading element x
xs :+ x  // creates a new collection with trailing element x

```

## Операции на картах

```scala
val myMap = Map("I" -> 1, "V" -> 5, "X" -> 10)  // create a map
myMap("I")      // => 1  
myMap("A")      // => java.util.NoSuchElementException  
myMap get "A"   // => None 
myMap get "I"   // => Some(1)
myMap.updated("V", 15)  // returns a new map where "V" maps to 15 (entry is updated)
                        // if the key ("V" here) does not exist, a new entry is added
```


## Операции на потоках (Streams)

```scala

val xs = Stream(1, 2, 3)
val xs = Stream.cons(1, Stream.cons(2, Stream.cons(3, Stream.empty))) // same as above
(1 to 1000).toStream // => Stream(1, ?)
x #:: xs // Same as Stream.cons(x, xs)
         // In the Stream's cons operator, the second parameter (the tail)
         // is defined as a "call by name" parameter.
         // Note that x::xs always produces a List
```













