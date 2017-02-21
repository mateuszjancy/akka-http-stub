package app

import java.util.concurrent.Executors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import app.Repository.{Item, User}
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

trait Service {
  def routes: Route
}

object Service {
  case class UsersWithItems(users: Seq[User], items: Seq[Item])

  private implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(repository: Repository, client: Client): Service = new Service with DefaultJsonProtocol with SprayJsonSupport {

    implicit val userJson = jsonFormat1(User)
    implicit val itemJson = jsonFormat1(Item)
    implicit val usersWithItemsJson = jsonFormat2(UsersWithItems)
    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    def streamUsersWithItems: Route = get {

    }

    def usersWithItems: Route = get {
      val userResponse: Future[Seq[User]] = Future(repository.users)
      val itemsResponse: Future[Seq[Item]] = client.get[Seq[Item]]("http://localhost:8080/items")
      val result: Future[UsersWithItems] = for {
        users <- userResponse
        items <- itemsResponse
      } yield UsersWithItems(users, items)

      complete(result)
    }

    def items: Route = get {
      val response: Source[Item, NotUsed] = Source(repository.items)
      complete(response)
    }

    override def routes = path("items")(items) ~ path("userswithitems")(usersWithItems)
  }
}