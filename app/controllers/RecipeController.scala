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
import utils.UniqueCode
import models.photos
import securesocial.core.{Identity, Authorization}

case class RecipeSubmit(recipe: Recipe, s: Seq[photos] = List())

object RecipeController extends Controller with MongoController with securesocial.core.SecureSocial {

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
		
	/*
	 *  TODO: Add the link of this recipe to the user who submitted it
	 */		
	def submitRecipe = Action {  implicit request =>
		recipeForm.bindFromRequest.fold(
			formWithErrors => {BadRequest(views.html.recipes.recipe_form(formWithErrors))},
			value => {
				val id = value.recipe.id match {
							case "-1" => UniqueCode.getRandomCode
							case v => v
						}
				AsyncResult {
					
					val newRecipe = (id != value.recipe.id)
					
					var photos = List[S3Photo]()
					var originalPhotos = List[S3Photo]()
					var isPreviewSet = false
					
					if(!newRecipe) {
						val recipe = getRecipe(id)
						recipe match {
							case Some(r) => photos = photos ++ r.photos
							case _ =>
						}
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
								val original = S3Photo.save(Image.asIs(file.ref.file), "original", "")
								photos = photos :+ original 
								photos = photos :+ S3Photo.save(Image.asSlider(file.ref.file), "slider", original.key)
								if(!isPreviewSet) {
									photos = photos :+ S3Photo.save(Image.asPreviewRecipe(file.ref.file), "preview", original.key)
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
							val selector = QueryBuilder().query(Json.obj("id" -> value.recipe.id)).makeQueryDocument
							val modifier = QueryBuilder().query(Json.obj(
								"id" -> id,
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
	
	private def getRecipe(id: String): Option[Recipe] = {
		val qb = QueryBuilder().query(Json.obj("id" -> id))
		val futureRecipe = Application.recipeCollection.find[JsValue](qb).toList.map(
				_.headOption match {
					case Some(h) => Some(h.as[Recipe])
					case _ => None
				})				
		val duration10000 = Duration(100000, "millis")
		val recipe = Await.result(futureRecipe, duration10000).asInstanceOf[Option[Recipe]]
		recipe
	}
		

	def get(id: String) = Action { implicit request =>
		Async {
			val oRecipe = getRecipe(id)	
			oRecipe match {
				case Some(recipe) => {
					val qbAll = QueryBuilder().query(Json.obj())
					Application.recipeCollection.find[JsValue](qbAll).toList(4).map  { relatedRecipes =>
						Ok(views.html.recipes.recipe(recipe, relatedRecipes.map(r => r.as[Recipe])))
					}
				}
				case _ => Future(BadRequest(s"Recipe $id was not found"))
			}
		}
	}

	def add() = SecuredAction { implicit request =>
		Ok(views.html.recipes.recipe_form(recipeForm))
	}
	
	def edit(id: String) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			Application.recipeCollection.find[JsValue](qb).toList.map { recipes =>
				val recipe = recipes.head.as[Recipe]
				Ok(views.html.recipes.recipe_form(recipeForm.fill(RecipeSubmit(recipe)), recipe.photos.filter(_.metadata.typeOf == "slider")))
			}
		}
	}
	
	/*
	 * TODO: move it to another collection and save information about who is the original user is along with it
	 */
	def delete() = Action { implicit request =>
		
		UserController.recipeManipulateForm.bindFromRequest.fold(
			formWithErrors => BadRequest("Error deleting a recipe: bad parameter"),
			value => {
				Async {
					val qbUser = QueryBuilder().query(Json.obj("id" -> value.userId)).makeQueryDocument 
					val modifier = QueryBuilder().query(Json.obj("$pull" -> Json.obj("myRecipeIds" -> value.recipeId))).makeQueryDocument
					Application.userCollection.update(qbUser, modifier)
					.map(_ => Redirect(routes.UserController.recipeBook("5158fde3b6f1b919c88a93ac"))
							.withSession("user" -> Json.toJson(UserController.getUser("5158fde3b6f1b919c88a93ac")).toString)
						).recover { case e => 
						{
							Logger.debug(e.getMessage)
							BadRequest(s"Error deleting a recipe: ${e.getMessage}")
						}
					}
				}
			})
	}
}