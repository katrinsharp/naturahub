package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

case class User(
		_id: String, 
		username: String,
		password: String,
		name: String, 
		lastName: String, 
		created: DateTime,
		myRecipeIds: Seq[String],
		savedRecipeIds: Seq[String]
		)

object User extends Function8[String, String, String, String, String, DateTime, Seq[String], Seq[String], User] {
	implicit val userWrites = Json.writes[User]
	implicit val userReads = Json.reads[User]
}