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

//https://github.com/sgodbillon/reactivemongo-demo-app/blob/master/app/controllers/Application.scala

object RecipeController extends Controller with MongoController {

	val db = ReactiveMongoPlugin.db
	lazy val collection = db("recipes")

	def get(id: Int) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.recipe(recipes.head.as[Recipe]))
			}
		}
	}

	def add() = Action { implicit request =>
		Ok(views.html.recipe_form())
	}

}