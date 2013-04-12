package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

/*
 * TODO: add userId of who created it?
 */
case class Recipe(
		id: String, 
		name: String, 
		shortDesc: String, 
		created: DateTime, 
		by: String, 
		directions: String, 
		phases: Seq[RecipePhase],
		prepTime: String,
		recipeYield: String,
		level: String, 
		tags: Seq[String],
		photos: Seq[S3Photo])

object Recipe extends Function12[String, String, String, DateTime, String, String, Seq[RecipePhase], String, String, String, Seq[String], Seq[S3Photo], Recipe] {
	implicit val recipeWrites = Json.writes[Recipe]
	implicit val recipeReads = Json.reads[Recipe]
}
