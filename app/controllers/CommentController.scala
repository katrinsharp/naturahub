package controllers

import play.api._
import play.api.mvc._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import play.api.libs.json._
import play.api.Play.current
import models.BlogEntry
import org.joda.time.DateTime
import reactivemongo.api.SortOrder.{ Ascending, Descending }
import org.joda.time.Interval
import models.Comment
import scala.util.matching.Regex
import securesocial.core.{Identity, Authorization}


object CommentController extends Controller with MongoController with securesocial.core.SecureSocial {
	
	def getByPostId(id: String) = Action { implicit request =>
		Async {	
			//val qb = QueryBuilder().query(Json.obj("postId" -> id))
			val qb = QueryBuilder().query(
					Json.obj("slug" -> Json.obj("$regex" -> (new Regex(s"^$id\\/*")).toString())))
					.sort("created" -> Descending)
			Application.commentsCollection.find[JsValue](qb).toList().map  { comments =>
				Ok(views.html.comments.comments(comments.map(r => r.as[Comment])))
			}
		}
	}
}