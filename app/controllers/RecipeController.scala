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
import views.html.defaultpages.error

//https://github.com/sgodbillon/reactivemongo-demo-app/blob/master/app/controllers/Application.scala

object RecipeController extends Controller with MongoController {

	val recipeForm: Form[Recipe] = Form(
		mapping(
			"id" -> nonEmptyText,
			"name" -> nonEmptyText,
			"shortDesc" -> nonEmptyText.verifying("This field is required", (_.trim().length() > 3)),
			"created" -> jodaDate("yyyy-MM-dd"),
			"by" -> nonEmptyText,
			"directions" -> nonEmptyText.verifying("This field is required", (_.trim().length() > 3)),
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
		
	def submitRecipe = Action {  implicit request =>
		recipeForm.bindFromRequest.fold(
			formWithErrors => {println(formWithErrors);BadRequest(views.html.recipe_form(formWithErrors))},
			value => {
				val id = value._id match {
							case "-1" => BSONObjectID.generate.stringify
							case v => v
						}
				AsyncResult {
					val selector = QueryBuilder().query(Json.obj("_id" -> value._id)).makeQueryDocument
					val modifier = QueryBuilder().query(Json.obj(
							"_id" -> id,
							"name" -> value.name,
							"shortDesc" -> value.shortDesc,
							"created" -> value.created,
							"by" -> value.by,
							"directions" -> value.directions,
							"prepTime" -> value.prepTime,
							"recipeYield" -> value.recipeYield,
							"level" -> value.level,
							"ingredients" -> value.ingredients(0).split(",").map(_.trim()),
							"tags" -> value.tags(0).split(",").map(_.trim()))).makeQueryDocument
					id match {
						case value._id => Application.collection.update(selector, modifier).map {
											e => println(e.toString);Redirect(routes.RecipeController.get(id))
										}
						case _ => Application.collection.insert(modifier).map {
											e => println(e.toString);Redirect(routes.RecipeController.get(id))
										}
					}
				} 
			}
		)
	}
		

	def get(id: String) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("_id" -> id))
			Application.collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.recipe(recipes.head.as[Recipe]))
			}
		}
	}

	def add() = Action { implicit request =>
		Ok(views.html.recipe_form(recipeForm))
	}
	
	def edit(id: String) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("_id" -> id))
			Application.collection.find[JsValue](qb).toList.map { recipes =>
				Ok(views.html.recipe_form(recipeForm.fill(recipes.head.as[Recipe])))
			}
		}
	}
	
	def delete(id: String) = Action { implicit request =>
		Ok
	}

}