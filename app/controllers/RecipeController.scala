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
import models.S3Photo
import utils.S3Blob
import java.io.File
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import utils.Image
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import play.api.data.FormError
import models.S3PhotoMetadata
import models.RecipePhase
import org.joda.time.DateTime

//https://github.com/sgodbillon/reactivemongo-demo-app/blob/master/app/controllers/Application.scala

case class RecipeSubmit(recipe: Recipe, s: Seq[photos] = List())
case class photos(key: String, originKey: String, isRemoved: Boolean)

object RecipeController extends Controller with MongoController {

	val recipeForm: Form[RecipeSubmit] = Form(
		mapping(
			"recipe" -> mapping(	
			"id" -> nonEmptyText,			
			"name" -> nonEmptyText,			
			"shortDesc" -> nonEmptyText.verifying("This field is required", (_.trim().length() > 3)),			
			"created" -> jodaDate("yyyy-MM-dd"),			
			"by" -> nonEmptyText,			
			"directions" -> nonEmptyText.verifying("This field is required", (_.trim().length() > 3)),			
			"phases" -> seq(mapping(
					"description" -> text,
					"ingredients" -> seq(nonEmptyText)
					)(RecipePhase.apply)(RecipePhase.unapply)),			
			"prepTime" -> nonEmptyText,			
			"recipeYield" -> nonEmptyText,			
			"level" -> text.verifying("beginner, intermediate or advanced", {_.matches("""^beginner|intermediate|advanced""")}),			
			"tags" -> seq(nonEmptyText),			
			"photos" -> ignored(Seq[S3Photo]())
			)(Recipe.apply)(Recipe.unapply),
			"removed" -> seq(mapping(
					"photoId" -> text,
					"originKey" -> text,
					"removedPhoto" -> boolean
					)(photos.apply)(photos.unapply))
			)(RecipeSubmit.apply)(RecipeSubmit.unapply))
		
	def submitRecipe = Action {  implicit request =>
		recipeForm.bindFromRequest.fold(
			formWithErrors => {println(formWithErrors);BadRequest(views.html.recipes.recipe_form(formWithErrors))},
			value => {
				val id = value.recipe._id match {
							case "-1" => BSONObjectID.generate.stringify
							case v => v
						}
				AsyncResult {
					
					val newRecipe = (id != value.recipe._id)
					
					var photos = List[S3Photo]()
					var originalPhotos = List[S3Photo]()
					var isPreviewSet = false
					
					if(!newRecipe) {
						val recipe = getRecipe(id)
						photos = photos ++ recipe.photos
						originalPhotos = photos
					}
					
					Logger.debug("photos: "+photos.length)
					photos.foreach(f => Logger.debug(f.key))
					Logger.debug(s"removed: "+value.s.toString)
					
					//val removedPhotos = photos.filter(p => {
					//	val found = value.s.find(_.key == p.key)
					//	found.isDefined && found.get.isRemoved 
					//})
					
					photos = photos.filterNot(p => {
						value.s.find(removed => removed.isRemoved && (removed.originKey == p.key || removed.originKey == p.metadata.originKey)).isDefined
					})
					
					Logger.debug("photos: "+photos.length)
					
					val files = request.body.asMultipartFormData.toList
					
					for(i <- 0 to files.length - 1) {
						files(i).files.map { file =>
							if(file.ref.file.length() != 0) {
								Logger.debug("next file")
								val original = S3Photo(S3Blob.s3Bucket, Image.saveAsIs(file.ref.file), S3PhotoMetadata("", "original", "", DateTime.now()))
								photos = photos :+ original
								photos = photos :+ S3Photo(S3Blob.s3Bucket, Image.saveAsSlider(file.ref.file), S3PhotoMetadata("", "slider", original.key, DateTime.now()))
								if(!isPreviewSet) {
									photos = photos :+ S3Photo(S3Blob.s3Bucket, Image.saveAsPreview(file.ref.file), S3PhotoMetadata("", "preview", original.key, DateTime.now()))
									isPreviewSet = true
								} 
							}
						}
					}
					
					photos.length match {
						case 0 => Future(BadRequest(views.html.recipes.recipe_form(
								recipeForm.fill(value).withError(FormError("recipe.photos", "Minimum one photo is required")), 
								originalPhotos.filter(_.metadata.typeOf == "slider")
								)))
						case _ => {
							val selector = QueryBuilder().query(Json.obj("_id" -> value.recipe._id)).makeQueryDocument
							val modifier = QueryBuilder().query(Json.obj(
								"_id" -> id,
								"name" -> value.recipe.name,
								"shortDesc" -> value.recipe.shortDesc.trim(),
								"created" -> value.recipe.created,
								"by" -> value.recipe.by,
								"directions" -> value.recipe.directions.trim(),
								"prepTime" -> value.recipe.prepTime,
								"recipeYield" -> value.recipe.recipeYield,
								"level" -> value.recipe.level,
								"phases" -> value.recipe.phases.map(ph => RecipePhase(ph.description, ph.ingredients(0).split(",").map(_.trim()))),
								"tags" -> value.recipe.tags(0).split(",").map(_.trim()),
								"photos" -> photos)).makeQueryDocument
							newRecipe match {
								case false => Application.recipeCollection.update(selector, modifier).map {
									e => {
										Logger.debug(e.toString)
										/*
										 * delete associated files
										 */
										
										
										//for {
										//	files <- Option(new File(path).listFiles)
										//	file <- files if file.getName.endsWith(".jpg")
										//} 
										//file.delete()
										
										
										Redirect(routes.RecipeController.get(id))
									}
								}
								case true => Application.recipeCollection.insert(modifier).map {
									e => Logger.debug(e.toString);Redirect(routes.RecipeController.get(id))
								}
							}
						}	
					}
					
				} 
			}
		)
	}
	
	private def getRecipe(id: String): Recipe = {
		val qb = QueryBuilder().query(Json.obj("_id" -> id))
		val futureRecipe = Application.recipeCollection.find[JsValue](qb).toList.map(_.head.as[Recipe])				
		val duration100 = Duration(1000, "millis")
		val recipe = Await.result(futureRecipe, duration100).asInstanceOf[Recipe]
		recipe
	}
		

	def get(id: String) = Action { implicit request =>
		Async {
			val recipe = getRecipe(id)	
			val qbAll = QueryBuilder().query(Json.obj())
			Application.recipeCollection.find[JsValue](qbAll, QueryOpts(batchSizeN=4)).toList.map  { relatedRecipes =>
				Ok(views.html.recipes.recipe(recipe, relatedRecipes.map(r => r.as[Recipe])))
			}
		}
	}

	def add() = Action { implicit request =>
		Ok(views.html.recipes.recipe_form(recipeForm))
	}
	
	def edit(id: String) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("_id" -> id))
			Application.recipeCollection.find[JsValue](qb).toList.map { recipes =>
				val recipe = recipes.head.as[Recipe]
				Ok(views.html.recipes.recipe_form(recipeForm.fill(RecipeSubmit(recipe)), recipe.photos.filter(_.metadata.typeOf == "slider")))
			}
		}
	}
	
	def delete(id: String) = Action { implicit request =>
		Ok
	}

}