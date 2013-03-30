package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

case class RecipePhase(
		description: String, 
		ingredients: Seq[String])

object RecipePhase extends Function2[String, Seq[String], RecipePhase] {
	implicit val RecipePhaseWrites = Json.writes[RecipePhase]
	implicit val RecipePhaseReads = Json.reads[RecipePhase]
	val EMPTY = RecipePhase("", Seq[String]())
}