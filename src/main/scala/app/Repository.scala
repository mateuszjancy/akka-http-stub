package app
import app.Repository.{Item, User}

import scala.collection.immutable.Seq

trait Repository {
  def users: Seq[User]
  def items: Seq[Item]
}

object Repository {
  case class User(name: String)
  case class Item(name: String)

  def apply(): Repository = new Repository {
    def users: Seq[User] = (0 to 10).map(id => User(s"User_$id"))
    def items: Seq[Item] = (0 to 10).map(id => Item(s"Item_$id"))
  }
}