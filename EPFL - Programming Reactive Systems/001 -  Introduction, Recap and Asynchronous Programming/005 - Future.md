# Future

Вспомним трансформацию для создания асинхронных сигнатур:

```scala
def program(a: A): B

def program(a: A, k: B => Unit): Unit
```

По сути - мы перемещаем результат в параметр здесь.

Мы бы могли смоделировать результат асинхронного выполнения в виде Future?

```scala
def program(a: A): Future[B]
```

Это позволит нам явно указать, что B - это результат выполнения. Сделаем это вручную:

```scala
def program(a: A, k: B => Unit): Unit
// каррирование
def program(a: A): (B => Unit) => Unit
// type-альяс
type Future[+T] = (T => Unit) => Unit
def program(a: A): Future[B]

// можно легко внедрить Try
type Future[+T] = (Try[T] => Unit) => Unit
```

В Scala Future - это трейт. Упрощенно он выглядит так:

```scala
trait Future[+A] {
  def onComplete(k: Try[A] => Unit): Unit
  // трансформация успехов
  def map[B](f: A => B): Future[B]
  def flatMap[B](f: A => Future[B]): Future[(A, B)]
  def zip[B](fb: Future[B]): Future[(A, B)]

  // трансформация неудач
  def recover(f: Exception => A): Future[A]
  def recoverWith(f: Exception => Future[A]): Future[A]
}
```

Перепишем пример с `makeCoffee`:

Было:

```scala
def makeCoffee(coffeeDone: Coffee => Unit): Unit = {  // async
  val coffee = ...
  coffeeDone(coffee)
}

def coffeeBreak(): Unit = {
  makeCoffee { coffee => 
    drink(coffee)
  }
  chat()
}
```

Стало:

```scala
def makeCoffee(): Future[Coffee] = ???


def coffeeBreak(): Unit = {
  val eventuallyCoffee = makeCoffee()

  eventuallyCoffee.onComplete { 
    case Success(coffee) => drink(coffee)
    case Failure(reason) => ???
  }
  chat()
}
```

Это больше похоже на обычную программу, где мы делаем вызовы и получаем возвраты. Теперь мы можем именовать вызов makeCoffee() и обрабатывать ошибки. 

*Обычно всё же предполагается как-то трансформить успешный результат и откладывать обработку ошибок на более поздний этап работы программы.*

## map на Future

- Трансформирует успешный Future[A] во Future[B], применяя функцию `f: A => B` **после завершения** вычисления Future[A];
- Автоматически передаёт неудачу Future[A] (если случилась) вщ Future[B].

## flatMap на Future

- Позволяет избавляться от результатов каррирования асинхронных выражений вроде `Future[Future[B]]`;
- Трансформмирует Future[A] во Future[B] по функции `f: A => Future[B]` **после окончания** работы Future[A];
- Возвращает сфейленный Future[B] если Future[A] сфейлился (или сам сфейлился).

## zip на Future

- Кобинирует два успешных Future в одно успешное Future[(A, B)];
- Возвращает неудачу если **любой** из двух Future сфейлил;
- Не создаёт никаких зависимостей между этими двумя Future.

```scala
makeCoffee() zip makeCoffee()  // они работают асинхронно - левая строна не обязательно выполнится раньше правой
```

## For

Наличие flatMap и map позволяет вызывать асинхронные вызовы таким образом:

```scala
// было
def workRoutine(): Future[Work] = {
  work().flatMap { work1 => 
    coffeeBreak().flatMap { _ => 
      work().map { work2 =>
        work1 + work2
      }
    }
  }

}

// стало
def workRoutine(): Future[Work] =
  for {
    work1 <- work()
    _ <- coffeeBreak()
    work2 <- work()
  } yield(work1 + work2)
```

## Future[Unit]

Перепишем программу с использованием этих методов:

```scala
def coffeeBreak(): Future[Unit] = {
  val eventuallyCoffeeDrunk = makeCoffee().flatMap(drink)
  val eventuallyChatted = chatWithColleagues()

  eventuallyCoffeeDrunk.zip(eventuallyChatted)
    .map(_ => ())
}
```

Выход произойдёт, когда обе стороны zip будут выполнены.

Возврат Future[Unit] полезен, в отличие от Unit. Это позволяет определить, когда оно выполнилось или сфейлилось. 

## Обработка ошибок

До сих пор мы не сделали ничего, чтобы обработать ошибки. map, flatMap и zip просто передают их далее. 

- Recover ринимает PartialFunction, которая **может** превратить Throwable в успешный результат;
- RecoverWith делает то же самое, но функция восстановления асинхронная и возвращает Future.

```scala
def recover[B >: A](pf: PartialFunction[Throwable, B]): Future[B]
def recoverWith[B >: A](pf: PartialFunction[Throwable, Future[B]]): Future[B]
```

## Execution Context

На этом этапе мы так и не поговорили о том, где физически вычисляются Future, как мы это контролируем (сколько тредов и т.п.). Где происходятся вычисления `onComplete` и карты?

В Scala возможно предоставить контекст для выполнения "продолжений" (continuations). 

Пользователи могут:

- Использовать один тред для вызова продолжений;
  - Без параллелизма.
- Несколько тредов - параллельное исполнение.

```scala
trait Future[+A] {
  def onComplete(k: Try[A] => Unit)(implicit ec: ExecutionContext): Unit = ???
}
```

Передача контекста происходит через имлисивный `ExecutionContext`. Он берётся из `scala.concurrent.ExecutionContext.Implicits.global`.

Этот контекст создаёт тред с таким размером тредпула, какое количество физических процессоров есть на машине.


## Пример

Одна из частых задач, когда дело касается Future - обернуть асинхронный Unit метод. В этом случае создаётся `Promise`:

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