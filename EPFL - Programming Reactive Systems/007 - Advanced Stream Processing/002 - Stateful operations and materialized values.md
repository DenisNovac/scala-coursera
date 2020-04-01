# Stateful operations

Обработка стримов обычно не имеет состояния, т.к. каждый оператор работает только с текущим элементом. Примеры - `map`, `filter` и т.п.

Но иногда нужно описать оператор, который полагается на какое-то состояние, созданное во время обработки предыдущих элементов.

В Akka Streams есть специальный оператор:

```scala
source
  .statefulMapConcat { () => // всегда новый инстанс внутренней функции
    // безопасное хранение стейта
    var state = ???
    // и тут
    // и тут...
    // Конец хранения стейта

    // обработка элементов
    element => {
      val elementsToEmit = List[String]()

      // вернуть лист
      elementsToEmit
    }
  }
```

По сути `mapConcat` это `flatMap` для стримов. В данном случае это стейтфул флатмап. Он имеет специальное место для хранения переменных (похоже на актор).

## Пример 1

Создадим лист элемент + индекс.

```scala
def zipWithIndex[A]: Flow[A, (A, Int), _] = 
  Flow[A].statefulMapConcat{ () => 
    var i = -1  // счетчик индекса
    element => {
      i += 1
      (element -> i) :: Nil
    }
  }
```

## Пример 2

Создадим вычисление среднего значения. После прихода каждого значения вычисляется среднее. Затем они фильтруются и отбрасываются все элементы, что ниже текущего среднего значения.

```scala
val aboveAverage: Flow[Rating, Rating, _] = 
  Flow[Rating].statefulMapConcat{ () => 
    var sum = 0
    var n = 0

    rating => {
      sum += rating.value
      n += 1
      val average = sum/n
      if (rating.value >= average) rating :: Nil
      else Nil
    }
  }
```

## Материализованные значения

Материализованные значения полезны во многих нетривиальных стримах.

Простейший пример:

```scala
val sink: Sink[Int, Future[Int]] = Sink.head
```

sink описывает процесс, который потребляет максимум один элемент. Из Int он материализует Future[Int]. Это значит, что из какого-то потока приходят числа 1, 2, 3. Когда они приходят в Sink. Тогда Sink вычисляет результат во Future[Int] и это `1` (пришла первой).

```scala
val eventuallyResult: Future[Int] = someSource.runWith(sink)
```

Материализованные значения можно использовать чтобы передавать значения в стрим:

```scala
// Описали источник
val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]

// Материализовали
val promise: Promise[Option[Int]] = source.to(Sink.ignore).run()

// Отправили 42 подписчикам потока
promise.complete(Some(42))  // Сразу после этого вышлется onComplete
//promise.complete(None)  // Просто вышлется onComplete
```

`Source.maybe[Int]` - означает, что этот источник (возможно) выдаст значение Int. На это влияет материализованное значение. Если мы его зададим - источник действительно выпустит значение. Вместо complete можно передать и failure с эксепшеном.

## Хранение материализованных значений

Когда мы комбинируем два стейджа (например, Source и Sink) - по дефолту только одно из материализованных значений сохраняется:

```scala
// Хранит материализованное значение Source-а
source.to(sink).run  : Promise[Option[Int]]

// Хранит материаилзованное значение Sink
source.runWith(sink) : Future[Int]
```

Мы можем контролировать материализованные значения:

```scala
source
  .toMat(sink)((sourceMat, sinkMat) => (sourceMat, sinkMat))
  .run(): (Promise[Option[Int]], Future[Int])
```

Функции `(sourceMat, sinkMat) => (sourceMat, sinkMat))` есть в качестве заготовленных:

```scala
source.toMat(sink)(Keep.both)  : RunnableGraph[(Promise[Option[Int]], Future[Int])]
source.toMat(sink)(Keep.right) : RunnableGraph[Future[Int]]
source.toMat(sink)(Keep.left)  : RunnableGraph[Promise[Option[Int]]]
source.toMat(sink)(Keep.none)  : RunnableGraph[NotUsed]
```

`NotUsed` - информирует пользователя (библиотеки, другого разработчика в том же проекте, т.п.) о том, что там нет никакого смысла использовать материализованное значение. 

## Материаилзация "контролирующих" объектов

Иногда материализация может помочь контролировать выполнение потока.

```scala
val ticks: Source[Int, Cancellable] = Source.tick(1.second)
val cancellable = ticks.to(Sink.ignore).run()
// Этот стрим будет бесконечен
// Если не прервать его вручную
cancellable.cancel()
```

`Cancellable` позволяет отменить работу этого источника. Это делается через материализацию этого значения.
