# Средства Akka Typed

Обычно один актор будет пользоваться несколькими протоколами. 

Вернёмся к протоколу покупки книги. Педположим, что за книгой пришёл секретарь, который должен эту книгу передать. Это значит, что у него два протокола - свой и протокол покупки книги, который был ранее.



```scala
/** Протокол покупки книги*/
case class RequestQuote(title: String, buyer: ActorRef[Quote])
case class Quote(price: BigDecimal, seller: ActorRef[BuyOrQuit])

sealed trait BuyOrQuit
case class Buy(address: Address, buyer: ActorRef[Shipping]) extends BuyOrQuit
case object Quit extends BuyOrQuit

case class Shipping(date: Date)



/** Секретарь */
sealed trait Secretary
case class BuyBook(title: String, maxprice: BigDecimal, seller: ActorRef[RequestQuote]) extends Secretary

def secretary(address: Address): Behavior[Secretary] =
  Behaviors.receiveMessage {
    case BuyBook(title, maxPrice, seller) =>
      seller ! RequestQuote(title, ???)
      ...
  }
```

BuyBook может использовать начальник, который просит секретаря купить книгу. RequestQuote должен описывать, куда слать ответ. Что же написать в нём, если Secretary реализовывается отдельно и продавец не может посылать ему никаких ответов типа `Behavior[Secretary]`. Продавец отсылает Quote, а это не часть Secretary.

Для этого используются **Адаптеры сообщений**

## Akka Typed Adapters

Нужно завернуть внешний протокол во внутренний:

```scala
sealed trait Secretary
case class BuyBook(title: String, maxprice: BigDecimal, seller: ActorRef[RequestQuote]) extends Secretary

case class QuoteWrapper(msg: Quote) extends Secretary
case class ShippingWrapper(msg: Shipping) extends Secretary
```

Тогда поведение секретаря будет выглядеть таким:

```scala
/** Поведение, описывающее получение заказа на покупку */
def secretary(address: Address): Behavior[Secretary] =
  Behaviors.receivePartial {
    case (ctx, BuyBook(title, maxPrice, seller)) =>
      val quote: ActorRef[Quote] = ctx.messageAdapter(QuoteWrapper)
      seller ! RequestQuote(title, quote)
      buyBook(maxPrice, address)
  }

```

`ctx.messageAdapter(QuoteWrapper)` - описываем, куда должен быть выслан ответ. Принимает на вход функцию для оборачивания. Case Class - это тоже функция (apply). Мы получаем ActorRef нужного типа таким образом. 


```scala
/** Поведение для покупки */
def buyBook(maxPrice: BigDecimal, address: Address): Behavior[Secretary] =
  Behaviors.receivePartial {

    case (ctx, QuoteWrapper(Quote(price, seller))) =>
      if (price > maxPrice) {
        seller ! Quit 
        Behaviors.stopped
      } else {
        val shipping: ActorRef[Shipping] = ctx.messageAdapter(ShippingWrapper)
        seller ! Buy(address, shipping)
        Behaviors.same
      }

    case (ctx, ShippingWrapper(Shipping(date))) =>
      Behaviors.stopped
  }
```

## Альтернатива для описания сообщений - определение сообщений для участников протокола

Протокол может быть устроен иначе. Например, можно закреплять роли за сообщениями:

- RequestQuote и BuyOrQuit могут extend BuyerToSeller;
- Quote и Shipping - SellerToBuyer.

Это позволяет создавать более понятные обёртки:

```scala
case class WrapFromSeller(msg: SellerToBuyer) extends Secretary
```

## Альтернатива для обёрток - использовать дочерние акторы для обработки разных протоколов

Секретарь мог бы создать дополнительный актор для передачи сообщений продавцу.

```scala
case class BuyBook(title: String, maxprice: BigDecimal, seller: ActorRef[RequestQuote]) extends Secretary
case class Bought(shippingDate: Date) extends Secretary
case object NotBought extends Secretary

def secretary(address: Address): Behavior[Secretary] =
  Behaviors.receive {
    case (ctx, BuyBook(title, maxPrice, seller)) =>
      // Создание дочернего актора с поведением buyBook
      val session = ctx.spawnAnonymous(buyBook(maxPrice,address, ctx.self))
      // Передача его адреса продавцу
      seller ! RequestQuote(title, session)
      // DeathWatch с дефолтным сообщением 
      ctx.watchWith(session, NotBought)
      Behaviors.same

    case (ctx, Bought(shoppingDate)) => Behaviors.stopped
    case (ctx, NotBought)            => Behaviors.stopped
  }
```

Метод `watchWith(session, NotBought)` - это тот же `context.watch`. Однако, если секретарь получит сообщение `Terminated(_)` - оно заменится на `NotBought`. Таким образом мы можем выслать сами себе дефолтное сообщение в случае фейла дочернего актора.

Имплементация покупателя:

```scala
def buyBook(maxPrice: BigDecimal, address: Address, replyTo: ActorRef[Bought]) =
  Behaviors.receive[SellerToBuyer] {
    case (ctx, Quote(price, session)) =>
      if (price > maxPrice) {
        session ! Quit
        Behaviors.stopped
      } else {
        session ! Buy(address, ctx.self)
        Behaviors.same
      }
    case (ctx, Shipping(date)) =>
      replyTo ! Bought(date)
      Behaviors.stopped
  }
```


## Отложенный приём сообщения

Порядок приёма сообщений иногда может быть неопределённыым, но актору может быть нужно принять определённое сообщение первым.

Например, инициализация актора может требовать каких-то ресурсов до обработки самих сообщений.

Решение: сохранять сообщения в буфер.

```scala
val initial = Behaviors.setup[String] { ctx => 
  val buffer = StashBuffer[String](100)  // Тип сообщений и размер, кидает эксепшен

  Behaviors.receiveMessage {
    case "first" =>
      buffer.unstashAll(ctx, running)  // running - следующее поведение

    case other =>
      buffer.stash(other)  // Помещаем всё сюда
      Behaviors.same
  }
}
```


## Типобезопасное обнаружение сервисов

Актор A предоставляет протокол P.
Актор B хочет коммуницировать с актором, имплементирующим протокол P. Актор B не знает актор A напрямую.

- В локальной системе зависимости можно инжектировать создав A и затем передав ActorRef[P] в B;
- Схема зависимостей может стать неуправляемой;
- Этот подход не работает в кластере.

Решение: использовать реестр сервисов ActorSystem. Это протокол `Receptionoist`, позволяющий акторной системе предоставлять акторы.

```scala
val ctx: ActorContext[_] = ???
ctx.system.receptionist: ActorRef[Receptionist.Command]
```

Для работы в кластере - нужно иметь описание протокола, которое можно сериализовать:

```scala
val key = ServiceKey[Greeter]("greeter")  // имя протокола greeter
```

Этот ключ можно использовать для регистрации сервисов или запроса существующих:

```scala
val key = ServiceKey[Greeter]("greeter")
val greeter = ctx.spawn(Greeter.behavior, "greeter")

ctx.system.receptionist ! Register(key, greeter)
```

Ключ и `ActorRef` должны иметь один тип.

Пример поиска по такому реестру:

```scala
sealed trait FriendlyCommand
case class Intro(friend: String) extends FriendlyCommand
case class SetGreeter(listing: Listing) extends FriendlyCommand

val friendly = Behaviors.setup[FriendlyCommand] { ctx => 
  val receptionist = ctx.system.receptionist
  val listingRef = ctx.messageAdapter(SetGreeter)
  receptionist ! Find(key, listingRef)

  /** Ждём поиска по реестру */
  val buffer = StashBuffer[FriendlyCommand](100)
  Behaviors.receiveMessage {
    // Если не пришло никаких ссылок
    case SetGreeter(key.Listing(refs)) if refs.isEmpty =>
      ctx.schedule(3.seconds, receptionist, Find(key, listingRef))
      Behaviors.same
    
    /** Передать в поведение friendlyRunning ActorRef[Greeter] */
    case SetGreeter(key.Listing(refs)) =>
      buffer.unstashAll(ctx, friendlyRunning(refs.head))

    case other =>
      buffer.stash(other)
      Behaviors.same
  }
}
```

