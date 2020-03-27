# Отзывчивость

Система может ответить на инпут за какое-то время. Если она не отвечает - она недоступна. Resilience (гибкость) бесполезна, если приложение восстановилось но осталось недоступным (под высокой нагрузкой). 

Паттерны отзывчивости.

Пример из 004 - Actor Composition:

```scala
class PostSummary(...) extends Actor {
  implicit val timeout = Timeout(500.millis)

  def receive = {
    case Get(postId, user, password) => 
      val response = for {
        status <- (publisher ? GetStatus(postId)).mapTo[PostStatus]
        text   <- (postStore ? Get(postId)).mapTo[Post]
        auth   <- (authService ? Login(user, password)).mapTo[AuthStatus]
      } yield
        if (auth.successful) Result(status, text)
        else Failure("not authorized")
    response pipeTo sender
  }
}
```

Система ждёт ответа от каждого из трёх акторов, ведь мы использовали for (монады выполняются последовательно). Мы бы добились большего быстродействия, запустив задачи параллельно:

```scala
class PostSummary(...) extends Actor {
  implicit val timeout = Timeout(500.millis)

  def receive = {
    case Get(postId, user, password) => 
        val status = (publisher ? GetStatus(postId)).mapTo[PostStatus]
        val text   = (postStore ? Get(postId)).mapTo[Post]
        val auth   = (authService ? Login(user, password)).mapTo[AuthStatus]

      val response = for {
        s <- status
        t <- text
        a <- auth
      } yield
        if (a.successful) Result(s, t)
        else Failure("not authorized")
    response pipeTo sender
  }
}
```

Любая система имеет лимиты. Их можно отодвинуть, используя паттерны из 005-Scalability. Но однажды клиенты начнут падать по таймауту. В этих случаях воркеры могут продолжать делать работу, которую уже никто не ждёт.

## Circuit Breaker

Акка предоставляет паттерн Circuit Breaker. Предположим, есть некоторый userService, который перегружен или упал. 

```scala
class Retriever(userService: ActorRef) extends Actor {
  implicit val timeout = Timeout(2.seconds)
  val cb = CircuitBreaker(
    context.system.scheduler,
    maxFailures = 3,  // Перепроверки Future
    callTimeout = 1.second,  // Каждую секунду проверяет, готова ли Future из result, maxFailures раз
    resetTimeout = 30.seconds  // после фейла принимает запросы только с таким интервалом, чтобы подождать, пока userService разгрузится
  )

  def receive = {
    case Get(user) =>
      val result = cb.withCircuitBreaker(userService ? user).mapTo[String]  // получает Future
      ... // Если maxFailures упали - ничего дальше не вычисляется, все запросы считаюстя Falure
  }
}
```

## Разделение ресурсов

Разные части системы должны иметь разные области памяти.

- Вычислительные части следует отделять от частей работы с клиентами;
- Изоляции акторов недостаточно - механизм выполнение тоже нужно разделять;
- Определять непересекающиеся ресурсы для разных частей системы.

`Props[MyActor].withDispatcher("compute-jobs")` - При работе на одном хосте разделение достигается за счёт разных диспетчеров.

Настройки дефолтного диспетчера: `akka.actor.default-dispacther`.

