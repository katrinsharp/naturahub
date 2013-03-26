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
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.format.Formats._
import views.html.defaultpages.badRequest
import play.api.data.FormError

//https://github.com/sgodbillon/reactivemongo-demo-app/blob/master/app/controllers/Application.scala

object RecipeController extends Controller with MongoController {

	val recipeForm: Form[Recipe] = Form(
		mapping(
			"id" -> number,
			"name" -> nonEmptyText,
			"shortDesc" -> nonEmptyText,
			"created" -> nonEmptyText,
			"by" -> text,
			"directions" -> nonEmptyText,
			"ingredients" -> list(nonEmptyText),
			"prepTime" -> nonEmptyText,
			"recipeYield" -> nonEmptyText,
			"level" -> text.verifying("beginner, intermediate or advanced", {_.matches("""^beginner|intermediate|advanced""")}),
			"tags" -> list(nonEmptyText)
		)(Recipe.apply)(Recipe.unapply))
		
		//( 
		//	(id, name, about, created, by, directions, ingredients, prep_time, recipe_yield, level, tags) => Recipe(id, name, about, created, by, directions, List(ingredients), prep_time, recipe_yield, level, List(tags)),
		//	(r: Recipe) => Some(r.id, r.name, r.about, r.created, r.by, r.content, r.ingredients, r.prep_time, r.recipe_yield, r.level, r.tags)
		//))
		
		//(Recipe.apply)(Recipe.unapply))
		
	def submitEdit = Action {  implicit request =>
		recipeForm.bindFromRequest.fold(
			formWithErrors => {println(formWithErrors);BadRequest(views.html.recipe_form(formWithErrors))},
			value => {
				println(value.tags)
				/*AsyncResult {
					val selector = QueryBuilder().query(Json.obj("id" -> value.id)).makeQueryDocument
					val modifier = QueryBuilder().query(Json.obj(
							/*"id" -> value.id,*/ 
							"name" -> value.name,
							"about" -> value.about,
							/*"created" -> value.id,
							"by" -> value.by,*/
							"content" -> value.content,
							"prepTime" -> value.prepTime,
							"recipeYield" -> value.recipeYield,
							"level" -> value.level,
							"ingredients" -> value.ingredients(0).split(", "),
							"tags" -> value.tags(0).split(", "))).makeQueryDocument
					
					Application.collection.update(selector, modifier).map {
						e => println(e.toString);Ok	
	        		}
				}*/
				Ok 
			}
		)
	}
		

	def get(id: Int) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			Application.collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.recipe(recipes.head.as[Recipe]))
			}
		}
	}

	def add() = Action { implicit request =>
		Ok(views.html.recipe_form(recipeForm))
	}
	
	def edit(id: Int) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			Application.collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.recipe_form(recipeForm.fill(recipes.head.as[Recipe])))
			}
		}
	}
	
	def delete(id: Int) = Action { implicit request =>
		Ok
	}

}