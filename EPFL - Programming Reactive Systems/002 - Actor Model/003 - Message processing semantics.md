# Message processing semantics

В этой лекции рассмотрен процесс передачи сообщений между акторами.

К внутренним свойствам акторов нет доступа. Акторам можно отправить сообщения только через адрес. Создание актора возвращает его адрес.

Акторы - это полностью независимые агенты вычислений. Нет глобальной синхронизации, работа конкурентена полностью.

Акторы работают в одном треде:

- Сообщения приходят по очереди;
- Изменение поведения происходит перед следующим сообщением;
- Обработка одного сообщения - атомарная операция.

Блокирование заменено на очередь сообщений, при этом методы по природе синхронны, ведь они в одном потоке.

Все возможные сообщения для акторов лучше всего оформлять в объекте-компаньоне:

```scala
/** Сообщения актора */
object BankAccount {
  case class Deposit(amount: BigInt) {
    require(amount > 0)
  }

  case class Withdraw(amount: BigInt) {
    require(amount > 0)
  }

  case object Done
  case object Failed
  case object Print
}
```


```scala
/** Собственно актор */
class BankAccount extends Actor {
  import BankAccount._

  var balance = BigInt(0)

  def receive = {
    case Deposit(amount) =>
      balance += amount
      sender ! Done

    case Withdraw(amount) if amount <= balance =>
      balance -= amount
      sender ! Done

    case Print =>
      println(balance)

    case _ => sender ! Failed
  }
}
```

Это аналог примера с банковским аккаунтом, гарантирующий синхронизацию. Каждое изменение в аккаунте будет производиться по очереди.

## Взаимодействие акторов

Предположим, мы хотим передавать деньги между аккаунтами. Мы могли бы вписать код внутрь самих аккаунтов. Но лучше сделать внешний актор. Он будет сначала посылать сообщение Withdraw одному аккаунту. Потом он дождётся Done и пошлёт на второй аккаунт Deposit. И дождётся Done. 


```scala
object WireTransfer {
  case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
  case object Done
  case object Failed
}

class WireTransfer extends Actor {
  import WireTransfer._

  override def receive: Receive = {
    case Transfer(from, to, amount) =>
      from ! BankAccount.Withdraw(amount)
      context.become(awaitWithdraw(to, amount, sender))
  }

  def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive = {
    case BankAccount.Done =>
      to ! BankAccount.Deposit(amount)
      context.become(awaitDeposit(client))

    case BankAccount.Failed =>
      client ! Failed
      context.stop(self)
  }

  def awaitDeposit(client: ActorRef): Receive = {
    case BankAccount.Done =>
      client ! Done
      context.stop(self)
  }
}

```

Это одноразовый WireTransfer, который создаётся для каждой передачи средств. Сначала он получает указания - от кого, кому и сколько передать. Затем снимает средства с первого аккаунта и переклчюает контекст на ожидание списания. Затем, если это было успешно, делает депозит и ожидает уже его. Наконец, если и он прошёл успешно, отсылает отправителю Done. Начального `sender` нужно передавать по поведениям (`client`), т.к. в качестве `sender` он выступает только когда создаёт запрос на передачу.

```scala
object BankOnActors extends App {
  import BankAccount._
  import WireTransfer._

  val system = ActorSystem("system")
  val account1 = system.actorOf(Props[BankAccount], "acc1")
  val account2 = system.actorOf(Props[BankAccount], "acc2")
  val transfer = system.actorOf(Props[WireTransfer], name = "transfer_query")

  account1 ! Deposit(10_000)
  account1 ! Print  // 10000


  account2 ! Deposit(20_000)
  account2 ! Print  // 20000

  transfer ! Transfer(account1, account2, 5000)

  Thread.sleep(200)
  account1 ! Print  // 5000
  account2 ! Print  // 25000
}
```

Нужно заметить, что эти сообщения 

```scala
account1 ! Deposit(10_000)
account1 ! Print  // 10000


account2 ! Deposit(20_000)
account2 ! Print  // 20000
```

Могут вывестись как 

```scala
20000
10000
```

Однако, никогда не возникнет ситуации

```scala
  account2 ! Deposit(20_000)
  account2 ! Print  // 0
```

Акторы гарантируют правильную последовательность обработки сообщений внутри себя.

С другой стороны, вот здесь есть узкое место:

```scala
account1 ! Deposit(10_000)
account2 ! Deposit(20_000)
transfer ! Transfer(account1, account2, 5000)
```

Если актор transfer начнёт работать раньше и раньше отправит сообщение account1, чем придёт Deposit - программа не отработает как нужно. Это связано с тем, что верхняя часть нашей программы написана не на акторах и не обрабатывает возвращаемые сообщения. Для упрощения там воткнут `Thread.sleep(200)`. 


## Гарантии доставки сообщений

Отправки сообщений не гарантируют их получения получателем. 

Доставка сообщения требует:

- Наличия канала;
- Наличия получателя.

Мы можем представить три модели отправки сообщений:

- Максимум единожды (**at-most-once delivery**) - доставка `[0,1]` раз;
- Как минимум единожды (**at-least-once**) - переотправка пока не будет получено подтверждение - доставка может произойти `[1, inf.+)` раз;
- Только раз (**exactly-once**) - обработка. Это самая дорогая опция.

Первая опция позволяет не сохранять никакого состояния. Во второй нужно хранить сообщение для переотправки. В третеьй нужно хранить сообщение и ответы, чтобы определить, было ли сообщение обработано. 


## Свойства сообщений

- Сообщения можно копировать и хранить;
- Сообщения могут содержать уникальный ID;
- Сообщения можно переотправлять сколько угодно.

Всего этого не достаточно для надёжности. Акторы должны явно уведомлять о том, что получили и обработали сообщение. 

Как применить все эти принципы к банковскому приложению?

- Логировать активность WireTransfer в хранилище;
- Каждый transfer должен иметь уникальный ID;
- Добавить ID для каждого Withdraw и Deposit;
- Хранить ID вычисленных действий внутри BankAccount.

## Порядок сообщений

Если актор шлёт множество сообщений в *одно направление* - они придут в правильном порядке (сами по себе Акторы этого не гарантируют, но Akka умеет).


# Выводы

- Акторы - это инкапсулированные независимые агенты вычислений;
- Сообщения - единственный путь интеракции с ними;
- Явные сообщения позволяют явно судить о надёжности;
- Порядок обработки сообщений не управляется и неизвестен (кроме отправки в одно направление).
