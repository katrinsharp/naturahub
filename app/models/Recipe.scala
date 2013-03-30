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
		phases: List[RecipePhase],
		prepTime: String,
		recipeYield: String,
		level: String, 
		tags: List[String],
		photos: List[S3Photo])

object Recipe extends Function12[String, String, String, DateTime, String, String, List[RecipePhase], String, String, String, List[String], List[S3Photo], Recipe] {
	implicit val recipeWrites = Json.writes[Recipe]
	implicit val recipeReads = Json.reads[Recipe]
}
