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
import models.User
import securesocial.core.{Identity, Authorization}

case class recipeSave(userId: String, recipeId: String)

object UserController extends Controller with MongoController with securesocial.core.SecureSocial {
	
	val recipeManipulateForm: Form[recipeSave] = Form(
		mapping(
			"userId" -> nonEmptyText, 	
			"recipeId" -> nonEmptyText
			)(recipeSave.apply)(recipeSave.unapply))

	def getUser(propValue: String, propName: String = "id"): Option[User] = {
		val qb = QueryBuilder().query(Json.obj(propName -> propValue))
		val future = Application.userCollection.find[JsValue](qb).toList.map(
				_.headOption match {
					case Some(h) => Some(h.as[User])
					case _ => None
				})
		val duration100000 = Duration(100000, "millis")
		val user = Await.result(future, duration100000).asInstanceOf[Option[User]]
		user
	}

	def get(id: String) = Action { implicit request =>
		val user = getUser(id)	
		Ok(user.toString)
	}
	
	def recipeBook(id: String) = Action { implicit request =>
			
		Async {
			val oUser = getUser(id)
			oUser match {
				case Some(user) => {
					val allNeededRecipes = user.myRecipeIds ++ user.savedRecipeIds
					val qbAll = QueryBuilder().query(Json.obj("id" -> Json.obj("$in" -> allNeededRecipes)))
					Application.recipeCollection.find[JsValue](qbAll).toList.map  { resultedRecipes =>	
						//val myRecipes = resultedRecipes.filter(p => user.myRecipeIds.exists(p.id))
						val partitionedRecipes = resultedRecipes.map(r => r.as[Recipe]).partition(p => user.myRecipeIds.exists(_ == p.id))
						Ok(views.html.recipe_book.summary(partitionedRecipes._1, partitionedRecipes._2))
					}
				}
				case _ => Future(BadRequest(s"User $id was not found"))
			}
		}
	}
	
	def saveToFavorites() = Action { implicit request =>
		
		recipeManipulateForm.bindFromRequest.fold(
			formWithErrors => BadRequest("Error saving to favorites: bad parameter"),
			value => {
				Async {
					//db.blogs.update({id:"001"}, {$push:{comments:{title:"commentX",content:".."}}});
					val qbUser = QueryBuilder().query(Json.obj("id" -> value.userId)).makeQueryDocument 
					val modifier = QueryBuilder().query(Json.obj("$push" -> Json.obj("savedRecipeIds" -> value.recipeId))).makeQueryDocument
					Application.userCollection.update(qbUser, modifier)
					.map(_ => Redirect(routes.RecipeController.get(value.recipeId))
							.withSession("user" -> Json.toJson(UserController.getUser("5158fde3b6f1b919c88a93ac")).toString)
						).recover { case e => 
						{
							Logger.debug(e.getMessage)
							BadRequest(s"Error saving to favorites: ${e.getMessage}")
						}
					}
				}
			})
	}
	
	def deleteFromFavorites() = Action { implicit request =>
		
		recipeManipulateForm.bindFromRequest.fold(
			formWithErrors => BadRequest("Error deleting from favorites: bad parameter"),
			value => {
				Async {
					//db.blogs.update({id:"001"}, {$push:{comments:{title:"commentX",content:".."}}});
					val qbUser = QueryBuilder().query(Json.obj("id" -> value.userId)).makeQueryDocument 
					val modifier = QueryBuilder().query(Json.obj("$pull" -> Json.obj("savedRecipeIds" -> value.recipeId))).makeQueryDocument
					Logger.debug("deleteFromFavorites")
					Application.userCollection.update(qbUser, modifier)
					.map(_ => Redirect(routes.RecipeController.get(value.recipeId))
							.withSession("user" -> Json.toJson(UserController.getUser("5158fde3b6f1b919c88a93ac")).toString)
						).recover { case e => 
						{
							Logger.debug(e.getMessage)
							BadRequest(s"Error deleting from favorites: ${e.getMessage}")
						}
					}
				}
			})
	}
}