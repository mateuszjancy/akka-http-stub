package app

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.javadsl.Flow
import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait Client {
  def get[T](url: String)(implicit unmarshaller: akka.http.scaladsl.unmarshalling.Unmarshaller[ResponseEntity, T]): Future[T]
  def getFlow[T](url: String)(implicit unmarshaller: akka.http.scaladsl.unmarshalling.Unmarshaller[ResponseEntity, T]): Flow[T]
}

object Client {
  private implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(actorSystem: ActorSystem, materializer: Materializer): Client = new Client {
    implicit val as = actorSystem
    implicit val m = materializer

    def get[T](url: String)(implicit unmarshaller: akka.http.scaladsl.unmarshalling.Unmarshaller[ResponseEntity, T]): Future[T] = for {
      response <- Http().singleRequest(HttpRequest(uri = url))
      items <- Unmarshal(response.entity).to[T]
    } yield items

    def getFlow[T](url: String)(implicit unmarshaller: akka.http.scaladsl.unmarshalling.Unmarshaller[ResponseEntity, T]) = {
      val client: Flow[(HttpRequest, Nothing), (Try[HttpResponse], Nothing), HostConnectionPool] = Http().cachedHostConnectionPoolHttps("localhost", 8080)
    }

  }
}
