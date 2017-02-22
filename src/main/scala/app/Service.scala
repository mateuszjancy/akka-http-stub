package app

import java.util.concurrent.Executors

import akka.NotUsed
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, ZipWith}
import app.Repository.{Item, User}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

trait Service {
  def routes: Route
}

object Service {

  case class UsersWithItems(users: Seq[User], items: Seq[Item])
  case class UserWithItem(users: Option[User], items: Option[Item])

  private implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(repository: Repository, client: Client): Service = new Service with DefaultJsonProtocol with SprayJsonSupport {

    implicit val userJson = jsonFormat1(User)
    implicit val itemJson = jsonFormat1(Item)
    implicit val usersWithItemsJson = jsonFormat2(UsersWithItems)
    implicit val userWithItemJson = jsonFormat2(UserWithItem)
    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    def usersWithItems: Route = get {
      val userResponse: Future[Seq[User]] = Future(repository.users)
      val itemsResponse: Future[Seq[Item]] = client.get[Seq[Item]]("http://localhost:8080/items")
      val result: Future[UsersWithItems] = for {
        users <- userResponse
        items <- itemsResponse
      } yield UsersWithItems(users, items)

      complete(result)
    }

    def userWithItemStream: Route = get {
      path("userwithitem" / IntNumber) { id =>
        val userFlow: Flow[Int, Option[User], NotUsed] =
          Flow[Int]
            .map(id => repository.users.filter(_.name.endsWith(id.toString)))
            .map(_.headOption)

        val itemsFlow: Flow[Int, Option[Item], NotUsed] =
          Flow[Int]
            .map(id => repository.items.filter(_.name.endsWith(id.toString)))
            .map(_.headOption)

        val source = Source.single(id)

        val flow: Flow[Int, UserWithItem, NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
          import GraphDSL.Implicits._

          val bcast = builder.add(Broadcast[Int](2))
          val merge = builder.add(ZipWith[Option[User], Option[Item], UserWithItem]{case (user, item) => UserWithItem(user, item)})


          bcast ~> userFlow  ~> merge.in0
          bcast ~> itemsFlow ~> merge.in1

          FlowShape(bcast.in, merge.out)
        })

        complete(source.via(flow))
      }
    }


    def items: Route = get {
      val response: Source[Item, NotUsed] = Source(repository.items)
      complete(response)
    }

    override def routes = path("items")(items) ~ path("userswithitems")(usersWithItems) ~ userWithItemStream
  }
}