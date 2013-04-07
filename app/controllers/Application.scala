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
import models.Recipe
import models.User

object Application extends Controller with MongoController {

	val db = ReactiveMongoPlugin.db
	lazy val recipeCollection = db("recipes")
	lazy val userCollection = db("users")
	lazy val blogEntriesCollection = db("blog")

	def index = Action { implicit request =>

		Async {
			val qb = QueryBuilder().query(Json.obj())
			Application.recipeCollection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.index(recipes.map(r => r.as[Recipe]))).withSession("user" -> Json.toJson(UserController.getUser("5158fde3b6f1b919c88a93ac")).toString)
			}
		}
	}
}