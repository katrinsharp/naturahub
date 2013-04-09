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
	
	def byCategory(categoryId: String) = Action { implicit request =>	
		Async {	
			val qbAll = QueryBuilder().query(Json.obj("categoryId" -> categoryId)).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map  { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}
	
	def byTag(tagName: String) = Action { implicit request =>	
		Async {	
			val qbAll = QueryBuilder().query(Json.obj("tags" -> Json.obj("$in" -> Seq(tagName)))).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map  { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}
	
	def byUser(userId: String) = Action { implicit request =>	
		Async {	
			val user = UserController.getUser(userId, "nickname")
			val qbAll = QueryBuilder().query(Json.obj("_id" -> Json.obj("$in" -> user.posts))).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map  { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}
	
	def archiveByYear(year: Int) = Action { implicit request =>	
		Async {
			val start = new DateTime(year, 1, 1, 0, 0)
			val end = new DateTime(year, 12, 31, 23, 59)
			val interval = new Interval(start, end)
			val qbAll = QueryBuilder().query(
					Json.obj("created" -> Json.obj(
							"$gte" -> start.toInstant().getMillis(),
							"$lte" -> end.toInstant().getMillis()))).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map  { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}
	
	def get(id: String) = Action { implicit request =>	
		Async {	
			val qb = QueryBuilder().query(Json.obj("_id" -> id))
			Application.blogEntriesCollection.find[JsValue](qb).toList().map  { entries =>
				Ok(views.html.blog.entry(entries.head.as[BlogEntry], categories))
			}
		}
	}
	
	def getRecentEntries(num: Int) = Action { implicit request =>	
		Async {	
			val qb = QueryBuilder().query(Json.obj()).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qb).toList(num).map  { entries =>
				Ok(views.html.blog.recent_posts(entries.map(r => r.as[BlogEntry])))
			}
		}
	}
}