# Акторы

Акторы - это объекты. Модель акторов описывает взаимодействие объектов.

Актор:

- Объект с идентификатором;
- Имеет поведение;
- Позволяет взаимодействовать с ним только через *асинхронные* сообщения.

Актор не позволяет вызывать методы внутри себя напрямую.

Пример:

Актор А хочет отослать сообщение Б. Он посылает сообщение и затем может продолжить делать то, что делал раньше, не ожидая ответа. Актор Б обработает сообщение и пошлёт ответ Актору А тем же способом.

## Actor Trait

В Akka используется трейт:

```scala
type Receive = PartialFunction[Any, Unit]

trait Actor {
  def receive: Receive

}
```

Receive - это partial-функция, описывающая ответ на сообщение. Сама по себе функция ничего не *возвращает*. В качестве возврата используется ответное сообщение, а не возврат из функции. 

Простейший пример:

```scala
import akka.actor.Actor

class Counter extends Actor {
  var count = 0

  override def receive: Receive = {
    case "incr" => count += 1
  }
}
```

Получили сообщение со строкой `"incr"`. Count не нужно синхронизировать - он всегда обрабатывается синхронно в одном потоке сам по себе.

Это неинтересный актор, ведь он не отвечает.

```scala
class Counter extends Actor {
  var count = 0

  override def receive: Receive = {
    case "incr" => count += 1
    case ("get", customer: ActorRef) => customer ! count
  }
}
```

Оператор `!` - это метод `tell`.

## Как посылаются сообщения

```scala
trait Actor {
  implicit val self: ActorRef  // каждый актор знает свой адрес
  def sender: ActorRef
  // ...
}

abstract class ActorRef {
  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit  // имплисивно вытаскивает ActorRef. Если используется внутри другого актора - берёт его адрес
  def tell(msg: Any, sender: ActorRef) = this.!(msg)(sender)  // синтаксис для Java
  // ...
}
```

Посылка сообщения от одного актора другому неявно пересылает адрес отправителя. Подписывать сообщение обратным адресом - это очень распространённый паттерн, поэтому в Akka его сделали автоматическим через имплиситы.

Получается, что в любом пришедшем сообщении по умолчанию есть обратный адрес и его не обязательно требовать вручную:

```scala
import akka.actor.Actor

class Counter extends Actor {
  var count = 0

  override def receive: Receive = {
    case "incr" => count += 1
    case "get" => sender ! count
  }
}
```

## Контекст Акторов

Акторы могут не только посылать сообщения. Они могут создавать новые акторы и менять своё поведение. Эти функции выполняются в контексте акторов.

Сам по себе тип Actor имеет только метод receive для описания поведения. Вычисления предоставляются уже контекстом.

```scala
trait ActorContext {
  def become(behavior: Receive, discardOld: Boolean = true): Unit
  def unbecome(): Unit
  // ...
}

trait Actor {
  implicit val context: ActorContext
  // ...
}
```

## Поведение акторов. Изменение поведения

Каждый актор имеет стек поведений. Активное в данный момент - это верхнее поведение. Дефолтное поведение `become` - заменять верхушку стека новым, но можно и запоминать предыдущее. Чтобы использовать контекст - нужно всего лишь вызывать его через переменную `context` внутри актора:

```scala
import akka.actor.Actor

class Counter extends Actor {

  def counter(n: Int): Receive = {
    case "incr" => context.become(counter(n + 1))
    case "get" => sender ! n
  }

  override def receive: Receive = counter(0)
}
```

Теперь мы избавились от var вообще. Сначала нужно определить метод, который отдаёт поведение. Поведение - это функция, описываемая Receive. 

Стартовое поведение определяется методом receive. Мы передаём в него counter с нулём.

Это похоже на рекурсивную функцию (tail-recursion) внешне и имеет преимущества:

- Явное изменение состояния;
- Состояние описано через поведение актора.

## Создание и остановка акторов

```scala
trait ActorContext {
  def actorOf(p: Props, name: String): ActorRef
  def stop(a: ActorRef): Unit
  // ...
}
```

Создание актора происходит через `actorOf`. Он принимает `Props` (описание того, как создавать актор) и имя. 

`Stop` может быть вызван не только на себе (`self`), но и на других акторах. Но обычно акторы выключают только сами себя.

Акторы всегда создаются и управляются другими акторами. Получается, что они формируют иерархию. 

## Работающий пример

Воркшиты IDEA плохо работают с акторами, поэтому пришлось писать в классе:

```scala
package app
import akka.actor.{Actor, ActorSystem, Props}

/** Главный класс-запускатель */
object SimpleActorCounter extends App {
  val system = ActorSystem("system")  // позволяет создать актор не будучи актором
  val counter = system.actorOf(Props[MainActor], "main")
}


class Counter extends Actor {

  def counter(n: Int): Receive = {
    case "incr" => context.become(counter(n + 1))
    case "get" => sender ! n
  }

  override def receive: Receive = counter(0)
}


class MainActor extends Actor {

  val counter = context.actorOf(Props[Counter], "counter")

  counter ! "incr"
  counter ! "incr"
  counter ! "incr"
  counter ! "incr"

  counter ! "get"  // прокнет receive пушто Counter сделает ! в нас

  override def receive = {
    case count: Int =>
      println(count)  // 4
      context.stop(self)
  }
}
```

В итоге выведется 4.