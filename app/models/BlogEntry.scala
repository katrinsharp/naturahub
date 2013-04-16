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
		created: DateTime,
		by: String, 
		content: String,
		entryType: String,
		categoryId: String,
		url: String,
		tags: Seq[String],
		photos: Seq[S3Photo],
		commentsNum: Int)

object BlogEntry extends Function11[String, String, DateTime, String, String, String, String, String, Seq[String], Seq[S3Photo], Int, BlogEntry] {
	implicit val writes = Json.writes[BlogEntry]
	implicit val reads = Json.reads[BlogEntry]
}
