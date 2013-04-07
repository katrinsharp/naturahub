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

case class BlogCategory(id: String, name: String, numberOfEntries: Int) 

object BlogController extends Controller with MongoController {
	
	val categories = Seq(BlogCategory("1", "General", 67), BlogCategory("2", "Video", 10), BlogCategory("3", "Trivia", 3))

	def summary() = Action { implicit request =>	
		
		Async {	
			val qbAll = QueryBuilder().query(Json.obj()).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map  { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}
	
	def category(categoryId: String) = Action { implicit request =>	
		Async {	
			val qbAll = QueryBuilder().query(Json.obj("categoryId" -> categoryId)).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map  { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}
}