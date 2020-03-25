# Акторы - распределённые

Обычно до этого разные акторы мы выполняли на разных ядрах процессора. Но ничего не мешает выполнять их на разных компьютерах в разных сетях.

По сравнению с вычислениями локально:

- Данные больше не в общей памяти программы, ими можно обменяться только по значению (требуется сериализация);
- Ширина канала ниже;
- Высокая задержка;
- Частичные фейлы (сообщение ушло, а ответ потерялся и т.п.)
- Повреждения данных.

На одной машине примерно те же проблемы, когда дело касается нескольких процессов. Но импакта не так много.

Инкапсуляция акторов заставляет их выглядеть одинаково независимо от положения. Снаружи они всегда ActorRef.

`Location Transparency` - локационная прозрачность.

## Actor Paths

Каждый актор имеет имя:

```scala
val system = ActorSystem("Hello world")
val ref = system.actorOf(Props[Greeter], "greeter")
println(ref.path)
// akka://HelloWorld/user/greeter
```

В пути `/user/` лежат акторы, созданные через `actorOf`.

`akka://` означает локальный актор.

Пример удалённого:

```
akka.tcp://HelloWorld@10.2.4.6:6565/user/greeter
```

## ActorRef и ActorPath

- ActorRef - указатель на актор, который был запущен;
  - Можно использовать watch.
- ActorPath - полное имя, независимо от существования актора.
  - Можно только верить.

Пример ActorRef:

```
akka.tcp://HelloWorld@10.2.4.6:6565/user/greeter#2134234
```

## Resolving ActorPath

При коннекте к удалённым системам есть способ получить ActorRef используя ActorPath. Для этого есть метод `context.actorSelection(path: ActorPath)`.  

```scala
import akka.actor.{ Identify, ActorIdentity }
case class Resolve(path: ActorPath)
case class Resolved(path: ActorPath, ref: ActorRef)
case class NotResolved(path: ActorPath)

class Resolver extends Actor {
  def receive = {
    case Resolve(path) =>
      context.actorSelection(path) ! Identify((path, sender))

    case ActorIdentity((path, client), Some(ref)) => 
      client ! Resolved(path, ref)

    case ActorIdentity((path, client), None) =>
      client ! NotResolved(path)
  }
}
```

Этот метод создаёт *нечто*, чему можно отсылать что-то. Все акторы в akka по умолчанию умеют отвечать на сообщение `Identify((path, sender))`. В ответ на это сюда же может прилететь несколько вариантов:

```scala
case ActorIdentity((path, client), Some(ref)) => // Если актор существовал
  client ! Resolved(path, ref)

case ActorIdentity((path, client), None) =>  // Если не существовал
  client ! NotResolved(path)
```

ActorPath может быть относителен:

```scala
context.actorSelection("child/grandchild")
context.actorSelection("../sibling")
context.actorSelection("/user/app")  // текущий корень акторов
context.actorSelection("/user/controllers/*")  // бродкаст 
```

