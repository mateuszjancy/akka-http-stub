import akka.actor.ActorSystem
import akka.stream.Materializer
import app.{Client, Repository, Service}

trait Context {
  val actorSystem: ActorSystem
  val materializer: Materializer
  val repository = Repository()
  val client = Client(actorSystem, materializer)
  val service = Service(repository, client)
}
