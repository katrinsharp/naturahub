package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date

case class Recipe(id: Int, 
		name: String, 
		about: String, 
		created: String, 
		by: String, 
		content: String, 
		ingredients: List[String],
		prepTime: String,
		recipeYield: String,
		level: String, 
		tags: List[String])

object Recipe extends Function11[Int, String, String, String, String, String, List[String], String, String, String, List[String], Recipe] {
	implicit val recipeWrites = Json.writes[Recipe]
	implicit val recipeReads = Json.reads[Recipe]
}
