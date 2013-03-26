package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date

case class Recipe(id: Int, 
		name: String, 
		about: String, 
		created: Date, 
		by: String, 
		content: String, 
		ingredients: Seq[String],
		prep_time: String,
		recipe_yield: String,
		level: String, 
		tags: Seq[String])

object EmptyRecipe {
	val default = Recipe(-1, "", "", new Date(), "", "", Seq(), "", "", "", Seq())
}

object Recipe extends Function11[Int, String, String, Date, String, String, Seq[String], String, String, String, Seq[String], Recipe] {
	implicit val recipeWrites = Json.writes[Recipe]
	implicit val recipeReads = Json.reads[Recipe]
}
