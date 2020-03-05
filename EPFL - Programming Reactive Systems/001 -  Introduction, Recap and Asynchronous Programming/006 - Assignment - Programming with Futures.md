# Assignment

Этот набор задач был посвящён Future.

Сначала нужно было написать метод `transormSuccess(x: Future[Int]): Future[Boolean]`, который возвращает true, если число чётное и Future с ошибкой, если во входном Future была ошибка.

```scala
def transformSuccess(eventuallyX: Future[Int]): Future[Boolean] = for {
    x <- eventuallyX
} yield x % 2 == 0
```

Future в for учитывает возможность failure (ведь внутренний объект там Try). yield работает как map, но map на Future срабатывает только при его успешном выполнении. 

Затем нужно было сделать recover для сфейленных future, который заменяет внутренний эксепшен на -1:

```scala
def recoverFailure(eventuallyX: Future[Int]): Future[Int] =
  eventuallyX.recover {
    case _ => -1
  }
```

Последовательный вызов слева-направо осуществляется через yield:

```scala
def sequenceComputations[A, B](
  makeAsyncComputation1: () => Future[A],
  makeAsyncComputation2: () => Future[B]
): Future[(A, B)] = for {
  a <- makeAsyncComputation1()
  b <- makeAsyncComputation2()
} yield (a,b)
```

Конкуррентный вызов можно делать через zip. Тогда значения слева и справа вычисляются раздельно, а не друг за другом.

```scala
def concurrentComputations[A, B](
  makeAsyncComputation1: () => Future[A],
  makeAsyncComputation2: () => Future[B]
): Future[(A, B)] =
  makeAsyncComputation1() zip makeAsyncComputation2()
```

Перезапуск в пределах максимального количества попыток:

```scala
def insist[A](makeAsyncComputation: () => Future[A], maxAttempts: Int): Future[A] = {
  makeAsyncComputation()
    .recoverWith { case _ if maxAttempts > 1 =>  // именно больше одного, иначе тест insist should retry failed computations фейлил
      insist(makeAsyncComputation, maxAttempts -1)
    }
}
```

Обёртка над API:

```scala
def futurize(callbackBasedApi: CallbackBasedApi): FutureBasedApi = {
  val p = Promise[Int]

  callbackBasedApi.computeIntAsync(
    continuation => p.tryComplete(continuation)
  )
  new FutureBasedApi {
    override def computeIntAsync(): Future[Int] = p.future
  }
}




trait CallbackBasedApi {
  def computeIntAsync(continuation: Try[Int] => Unit): Unit
}

trait FutureBasedApi {
  def computeIntAsync(): Future[Int]
}
```

Нужно обернуть CallbackBasedApi так, чтобы возвращать Future. Из лекции это делается так:

```scala
// из библиотеки callback-based функция
def makeCoffee(coffeeDone: Coffee => Unit, 
               onFailure: Exception => Unit): Unit

// обёртка
def makeCoffee2(): Future[Coffee] = {
  val p = Promise[Coffee]()
  makeCoffee(
    coffee => p.trySuccess(coffee),
    reason => p.tryFailure(reason)
  )
  p.future
}
```

Мы поступаем так же, но, т.к. Failure может быть в continuation - используем TryComplete, который позволяет "проглотить" как успех, так и неудачу.