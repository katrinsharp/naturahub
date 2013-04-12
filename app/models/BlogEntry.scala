package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

/*
 * TODO: handle the comments
 */
case class BlogEntry(
		id: String, 
		name: String, 
		content: String,
		created: DateTime, 
		by: String,
		entryType: String,
		categoryId: String,
		url: String,
		tags: Seq[String])

object BlogEntry extends Function9[String, String, String, DateTime, String, String, String, String, Seq[String], BlogEntry] {
	implicit val recipeWrites = Json.writes[BlogEntry]
	implicit val recipeReads = Json.reads[BlogEntry]
}
