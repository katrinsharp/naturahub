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

object UserController extends Controller with MongoController {

	private def getUser(id: String): User = {
		val qb = QueryBuilder().query(Json.obj("_id" -> id))
		val future = Application.userCollection.find[JsValue](qb).toList.map(_.head.as[User])				
		val duration100 = Duration(1000, "millis")
		val user = Await.result(future, duration100).asInstanceOf[User]
		user
	}

	def get(id: String) = Action { implicit request =>
		val user = getUser(id)	
		Ok(user.toString)
	}
	
	def recipeBook(id: String) = Action { implicit request =>
			
		Async {
			val user = getUser(id)	
			val allNeededRecipes = user.myRecipeIds ++ user.savedRecipeIds
			val qbAll = QueryBuilder().query(Json.obj("_id" -> Json.obj("$in" -> allNeededRecipes)))
			Application.recipeCollection.find[JsValue](qbAll).toList.map  { resultedRecipes =>	
				//val myRecipes = resultedRecipes.filter(p => user.myRecipeIds.exists(p._id))
				val partitionedRecipes = resultedRecipes.map(r => r.as[Recipe]).partition(p => user.myRecipeIds.exists(_ == p._id))
				Ok(views.html.recipe_book.summary(partitionedRecipes._1, partitionedRecipes._2))
			}
		}
	}
}