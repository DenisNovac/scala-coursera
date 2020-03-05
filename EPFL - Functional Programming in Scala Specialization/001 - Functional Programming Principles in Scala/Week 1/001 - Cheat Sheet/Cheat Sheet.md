# Cheat Sheet

[Источник](https://github.com/lampepfl/progfun-wiki/blob/gh-pages/CheatSheet.md)

## Правила вычисления (evaluation rules)

- Вызов по значению: аргументы функции вычисляются до вызова функции;
- Вызов по имени: значение параметра вычисляется только в момент вызова параметра. Для вызова по имени необходимо указать `=>` перед его типом.

Преимущество вызова по имени заключается в том, что они не вычисляются если не используются в теле функции.

С другой стороны, преимущество вызова по значению заключается в том, что они вычисляются только один раз.

```scala
class CheatSheet {
  def exampleDef = 2 // вычисляется когда вызывается
  val exampleVal = 2 // выислятеся мгновенно
  lazy val exampleLazy = 2 // вычисляется только когда требуется

  def squareValue(x: Double) = {}// вызов по значению
  def squareName(x: => Double) = {}// вызов по имени

  def myFct(bindings: Int*) = { // bindings - это последовательность Int с неизвестным количеством аргументов
    for (n <- 0 until bindings.length)
      print(bindings(n))
  }
}
```

## Функции высшего порядка (High order functions)

Это функции, которые принимают функции как параметры или отдают функции.

```scala
object MainHOF {
  def main(args: Array[String]): Unit = {
    val hof = new HigherOrderFunctions

    def cube(x: Int) = x * x * x
    val b = hof.sum1(cube)(1,3) // сумма кубов от 1 до 3
    println("Сумма кубов от 1 до 3: "+b) // 36

    val c = hof.sum2((x: Int) => x * x * x)(1,4) // то же, что выше, но анонимной функцией
    println("Сумма кубов от 1 до 4: "+c) // 100

  }
}


class HigherOrderFunctions {
  // sum принимает на вход функцию f и возвращает сумму её результатов
  // в диапазоне Int, Int
  def sum1(f: Int => Int): (Int, Int) => Int = {
    def sumf(a: Int, b: Int): Int = { // просуммировать результаты f от a до b
      var s = 0
      for (x <- a until b+1) s+=f(x)
      s
    }
    sumf
  }

  // то же что выше, но с явной передачей аргументов
  def sum2(f: Int => Int)(a: Int, b: Int): Int = {
    def sumf(a: Int, b: Int): Int = { // просуммировать результаты f от a до b
      var s = 0
      for (x <- a until b+1) s+=f(x)
      s
    }
    sumf(a,b)
  }
}

```

## Каррирование (Currying) функций

Каррирование - преобразование функции от многих аргументов в набор функций, каждая из которых является функцией от одного аргумента.

```scala
object Currying {
  def main(args: Array[String]): Unit = {
    def fn(a: Int, b: Int): Int = { a + b } // некаррированна: тип (Int, Int) = Int
    def fс(a: Int)(b: Int): Int = { a + b } // каррированная функция (тип Int => Int => Int)

    println(fn(1,2)) // 3
    println(fn(3,4)) // 7

  }
}
```

## Классы

Ну тут всё ясно, только стоит помнить, что дефолтный конструктор - это тело метода + его аргументы, а дополнительный конструктор определяется через def this и всё равно как-то использует дефолтный.

```scala

class MyClass(x: Int, y: Int) {
  require(y > 0, "y must be positive")  // precondition, triggering an IllegalArgumentException if not met

  def this (x: Int) = { // вспомогательный (auxiliary) конструктор
    this(x+1, x-1)
  }

  def nb1 = x  // публичный метод вычисляется при каждом вызове
  def nb2 = y

  private def test(a: Int): Int = { // приватный метод
    if (a>0)
      a
    else
      0
  }

  def method(a: Int): Int = a
  // перегрузка метода
  def method(a: Int, b: Int): Int = a+b

  val nb3 = x + y // эта переменная вычисляется единожды

  // переопределение метода
  override def toString: String = {
    "("+x.toString+", "+y.toString+", "+nb3.toString+")"
  }
  //override def toString = member1 + ", " + member2 // overridden method
}


```


## Операторы

Оператор - это то же, что функция в Scala. 

Одно и то же:

```scala
mc method 1
mc.method(1)
```

Имена функций могут быть алфавитными, символическими (x1, *, +?&, vector_++, counter_=).

Старшинство оператора определяется первым символом, с следующим порядком по возрастанию:

```scala
(all letters)
|
^
&
< >
= !
:
+ -
* / %
(all other special characters)

```

Ассоциативность операторов определена последним символом. 

Операторы присваивания имеют низший приоритет.

## Иерархия классов

```scala

abstract class TopLevel {     // abstract class  
  def method1(x: Int): Int   // abstract method  
  def method2(x: Int): Int = { ... }  // обычный метод
}

class Level1 extends TopLevel {  
  def method1(x: Int): Int = { ... }  // реализованный метод
  override def method2(x: Int): Int = { ...} // этот метод должен быть явно переопределён словом override, т.к. он был у родителя
}

object MyObject extends TopLevel { ... } // defines a singleton object. No other instance can be created

```

Минимальное запускаемое приложение Scala:


```scala
object Hello {  
  def main(args: Array[String]) = println("Hello world")  
}

// или

object Hello extends App {
  println("Hello World")
}
```

## Организация классов

- Классы и объекты организованы по пакетам (`package myPackage`);
- Они могут быть вызваны через `import` (например, `import myPackage.myClass`);
- Они могут быть напрямую вызваны через полное имя (`myPackage.myClass`);
- Все члены пакетов `scala` и `java.lang`, а также члены объекта `scala.Predef` автоматически импортируются;
- `Traits` (Трейты) - это вроде интерфейсов в Java, **кроме того, что** они могут иметь неабстрактые части.

**Общая иерархия объектов**:

Смотри [эту ссылку](https://github.com/DenisNovac/scala/blob/master/Scala%20Tour/001%20-%20%D0%95%D0%B4%D0%B8%D0%BD%D0%BE%D0%BE%D0%B1%D1%80%D0%B0%D0%B7%D0%B8%D0%B5%20%D1%82%D0%B8%D0%BF%D0%BE%D0%B2.md)

- `scala.Any` это базовый тип всех типов. Имеет методы `hashCode` и `toString`;
- `scala.AnyVal` - это базовый тип для примитивных типов (вроде `scala.Double`);
- `scala.AnyRef` - это базовый тип всех ссылочных типов (альяс `java.lang.Object`, супертип для таких ссылочных типов как `java.lang.String` и `scala.List`);
- `scala.Null` - это подтип любого подтипа `scala.AnyRef` (`null` это единственный его инстанс);
- `scala.Nothing` - это подтип всех подтипов (**bottom(нижний) тип**), у него никогда нет инстансов.


## Типизированные параметры

Похожи на генерики Java:

```scala
class MyClass[T](arg1: T) { ... }  
new MyClass[Int](1)  
new MyClass(1)   // the type is being inferred, i.e. determined based on the value arguments  

// можно ограничивать типы
def myFct[T <: TopLevel](arg: T): T = { ... } // T must derive from TopLevel or be TopLevel
def myFct[T >: Level1](arg: T): T = { ... }   // T must be a supertype of Level1
def myFct[T >: Level1 <: Top Level](arg: T): T = { ... }

```


## Variance (Вариантность)

Вариантность - это указание определённой специфики взаимосвязи между связанными типами. Scala поддерживает вариантную аннотацию типов у обобщённых классов, что позволяет им быть ковариантными, контрвариантными или инвариантными (если нет указания на вариантность). Использование вариантности в системе типов позволяет устанавливать понятные взаимосвязи между сложными типами, в то время как отсутствие вариантности может ограничить повторное использование абстракции класса.

```scala
class Foo[+A] // ковариантный класс
class Bar[-A] // контрвариантный класс
class Baz[A]  // инвариантными класс

```

A <: B (A - подтип B или сама является B)

Если

C[A] <: C[B], C - ковариантный
C[A] >: C[B], C - контрвариантный

Иначе C инвариантен

```scala
class C[+A] { ... } // C is covariant
class C[-A] { ... } // C is contravariant
class C[A]  { ... } // C is nonvariant
```

Функции должны быть контрвариантны в своих аргументах и ковариантны в результатах (аргументы могут быть супертипа, а результаты - только подтипа):

```scala
trait Function1[-T, +U] {
  def apply(x: T): U
} // Variance check is OK because T is contravariant and U is covariant

class Array[+T] {
  def update(x: T)
} // variance checks fails

```

+A - расширяет A, extends A
-A - супертипа над A










