package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date
import org.joda.time.DateTime

case class S3PhotoMetadata(
		typeOf: String, 
		originKey: String,
		uploaded: DateTime)

object S3PhotoMetadata extends Function3[String, String, DateTime, S3PhotoMetadata] {
	implicit val s3PhotoMetadataWrites = Json.writes[S3PhotoMetadata]
	implicit val s3PhotoMetadataReads = Json.reads[S3PhotoMetadata]
	val EMPTY = S3PhotoMetadata("", "", DateTime.now())
}
