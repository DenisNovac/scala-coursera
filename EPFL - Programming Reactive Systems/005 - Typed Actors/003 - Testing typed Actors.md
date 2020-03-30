# Тестирование акторов

Тестирование акторов может быть сложной задачей, ведь:

- Акторы используют асинхронные сообщения;
- Асинхронные ожидания означают использование таймаутов;
- Тестовые процедуры сложно детерминировать.

Тестирование поведений могло бы облегчить задачу:

- Это функции, принимающие сообщения и возвращающие поведения;
- Эффекты происходят во время вычисления функций поведения синхронно;
- Арсенал тестирования для функций полностью доступен.

Повдеение можно помещать в TestKit, эмулирующий ActorContext:

```scala
val guardianKit = BehaviorTestKit(guardian)
```

Передача сообщения. Обработка сообщения (эффекта) происходит по запросу:

```scala
guardianKit.ref ! msg
guardianKit.runOne()
```

Дополнительные методы:

```scala
guardianKit.isAlive must be (true) // Актор все ещё обрабатывает сообщения?
guardianKit.retrieveAllEffects() must be (Seq(Stopped("Bob")))  // Вытаскивает из очереди все эффекты, которые уже произошли (в виде объектов)
```

Эти методы полезны, но нарушают логику работы акторов. Лучше всего посылать сообщения и смотреть, что возвращается.

## TypedTestinbox

Мы увидели, что посылка сообщений теперь производится не в контексте, а через `guardianKit`. Поэтому и приём сообщений должен осуществляться сторонним средством.

```scala
val sessionInbox = TestInbox[ActorRef[Command]]()  // Параметризованная очередь сообщений
```

Этот ящик параметризован типом, который он может принять и предоставляет ActorRef для посылки сообщений.

## Тестирование

Используя эти приспособления, мы можем обмениваться сообщениями между тестовой процедурой и тестируемым поведением.

```scala
"Guardian actor" should "return Greeter actorRef" in {
  val guardianKit = BehaviorTestKit(guardian)
  val sessionInbox = TestInbox[ActorRef[Greeter]]()

  guardianKit.ref ! NewGreeter(sessionInbox.ref)
  guardianKit.runOne
  // Излвекли одно сообщение из ящика - это ActorRef[Greeter]
  val greeterRef = sessionInbox.receiveMessage

  assert {
    greeterRef match {
      case _: ActorRef[Greeter] => true
      case _                    => false 
    }
  }
}
```


## Тестирование дочерних акторов


