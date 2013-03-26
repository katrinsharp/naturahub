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

object Application extends Controller with MongoController {

	val db = ReactiveMongoPlugin.db
	lazy val collection = db("recipes")

	def index = Action { implicit request =>

		Async {
			val qb = QueryBuilder().query(Json.obj())
			Application.collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.index(recipes.map(r => r.as[Recipe])))
			}
		}
	}
}