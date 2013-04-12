package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

case class Comment(
		id: String,
		postId: String,
		content: String,
		created: DateTime, 
		by: String,
		slug: String)

object Comment extends Function6[String, String, String, DateTime, String, String, Comment] {
	implicit val writes = Json.writes[Comment]
	implicit val reads = Json.reads[Comment]
}
