package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

case class Recipe(
		_id: String, 
		name: String, 
		shortDesc: String, 
		created: DateTime, 
		by: String, 
		directions: String, 
		ingredients: List[String],
		prepTime: String,
		recipeYield: String,
		level: String, 
		tags: List[String])

object Recipe extends Function11[String, String, String, DateTime, String, String, List[String], String, String, String, List[String], Recipe] {
	implicit val recipeWrites = Json.writes[Recipe]
	implicit val recipeReads = Json.reads[Recipe]
}
