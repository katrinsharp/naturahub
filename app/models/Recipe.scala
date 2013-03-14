package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date

case class Recipe(id: Int, name: String, about: String, created: Date, by: String, content: String, level: String, tags: Seq[String])

object Recipe extends Function8[Int, String, String, Date, String, String, String, Seq[String], Recipe] {
	implicit val recipeWrites = Json.writes[Recipe]
	implicit val recipeReads = Json.reads[Recipe]
}
