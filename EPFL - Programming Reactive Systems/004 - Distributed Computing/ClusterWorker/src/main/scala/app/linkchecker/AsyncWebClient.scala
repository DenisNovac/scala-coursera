package app.ClusterWorker.linkchecker

import java.util.concurrent.Executor

import com.ning.http.client.AsyncHttpClient
import org.jsoup.Jsoup

import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters._


trait WebClient {
  def get(url: String)(implicit exec: Executor): Future[String]
}

case class BadStatus(i: Int) extends Throwable

object AsyncWebClient extends WebClient {

  private val client = new AsyncHttpClient

  override def get(url: String)(implicit exec: Executor): Future[String] = {
    val f = client.prepareGet(url).execute()  // ListenableFuture
    val p = Promise[String]()  // для заполнения результатом асинхронного листенера
    f.addListener(new Runnable {
      override def run(): Unit = {
        val response = f.get
        if (response.getStatusCode < 400)
          p.success(response.getResponseBodyExcerpt(131072))  // заполнили promise
        else p.failure(BadStatus(response.getStatusCode))
      }
    }, exec)
    p.future  // Из Promise можно получить Future типа Promise
  }


  def findLinks(body: String): Iterator[String] = {
    val document = Jsoup.parse(body)
    val links = document.select("a[href]")
    for {
      link <- links.iterator().asScala
    } yield link.absUrl("href")
  }
}



