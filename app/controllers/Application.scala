package controllers

import play.api._
import play.api.mvc._

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._

// Reactive Mongo plugin
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

// Play Json imports
import play.api.libs.json._

import play.api.Play.current

//https://github.com/sgodbillon/reactivemongo-demo-app/blob/master/app/controllers/Application.scala

object Application extends Controller with MongoController {

	val db = ReactiveMongoPlugin.db
	lazy val collection = db("recipes")

	def index = Action {
		Ok(views.html.index())
	}

	def recipe(id: Int) = Action {
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.recipe(recipes.headOption.getOrElse(JsString(""))))
			}
		}
	}

}