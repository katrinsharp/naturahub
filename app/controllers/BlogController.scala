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
import models.BlogEntry
import org.joda.time.DateTime
import reactivemongo.api.SortOrder.{ Ascending, Descending }
import org.joda.time.Interval
import scala.concurrent.Future
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.S3Photo
import models.photos
import utils.UniqueCode
import utils.Image
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future

case class BlogCategory(id: String, name: String, numberOfEntries: Int)
case class BlogEntrySubmit(entry: BlogEntry, s: Seq[photos] = List())

object BlogController extends Controller with MongoController {

	val categories = Seq(BlogCategory("1", "General", 67), BlogCategory("2", "Video", 10), BlogCategory("3", "Trivia", 3))

	val blogEntryForm: Form[BlogEntrySubmit] = Form(
		mapping(
			"blogentry" -> mapping(
				"id" -> nonEmptyText,
				"name" -> nonEmptyText,
				"created" -> jodaDate("yyyy-MM-dd"),
				"by" -> nonEmptyText,
				"content" -> nonEmptyText.verifying("This field is required", (_.trim().length() > 3)),
				"entryType" -> ignored(""),
				"categoryId" -> ignored("users"),
				"url" -> text,
				"tags" -> seq(nonEmptyText),
				"photos" -> ignored(Seq[S3Photo]()),
				"commentsNum" -> number.verifying(_ >= 0))(BlogEntry.apply)(BlogEntry.unapply),
			"removed" -> seq(mapping(
				"photoId" -> text,
				"originKey" -> text,
				"removedPhoto" -> boolean)(photos.apply)(photos.unapply)))(BlogEntrySubmit.apply)(BlogEntrySubmit.unapply))

	def submitBlogEntry = Action { implicit request =>
		blogEntryForm.bindFromRequest.fold(
			formWithErrors => { println(formWithErrors); BadRequest(views.html.blog.entry_form(formWithErrors)) },
			value => {
				val id = value.entry.id match {
					case "-1" => UniqueCode.getRandomCode
					case v => v
				}
				AsyncResult {

					val newEntry = (id != value.entry.id)

					var photos = List[S3Photo]()
					var originalPhotos = List[S3Photo]()
					var isPreviewSet = false

					if (value.entry.url.isEmpty()) {
						if (!newEntry) {
							val recipe = getBlogEntry(id)
							recipe match {
								case Some(r) => photos = photos ++ r.photos
								case _ =>
							}
							originalPhotos = photos
						}

						Logger.debug("photos: " + photos.length)
						photos.foreach(f => Logger.debug(f.key))
						Logger.debug(s"removed: " + value.s.toString)

						//val removedPhotos = photos.filter(p => {
						//	val found = value.s.find(_.key == p.key)
						//	found.isDefined && found.get.isRemoved 
						//})

						photos = photos.filterNot(p => {
							value.s.find(removed => removed.isRemoved && (removed.originKey == p.key || removed.originKey == p.metadata.originKey)).isDefined
						})

						Logger.debug("photos: " + photos.length)

						val files = request.body.asMultipartFormData.toList

						for (i <- 0 to files.length - 1) {
							files(i).files.map { file =>
								if (file.ref.file.length() != 0) {
									Logger.debug("next file")
									val original = S3Photo.save(Image.asIs(file.ref.file), "original", "")
									photos = photos :+ original
									photos = photos :+ S3Photo.save(Image.asSlider(file.ref.file), "slider", original.key)
									if (!isPreviewSet) {
										photos = photos :+ S3Photo.save(Image.asPreviewBlogEntry(file.ref.file), "preview", original.key)
										isPreviewSet = true
									}
								}
							}
						}

					}

					val selector = QueryBuilder().query(Json.obj("id" -> value.entry.id)).makeQueryDocument
					val modifier = QueryBuilder().query(Json.obj(
						"id" -> id,
						"name" -> value.entry.name,
						"created" -> value.entry.created,
						"by" -> value.entry.by,
						"content" -> value.entry.content.trim(),
						"entryType" -> (value.entry.url.isEmpty() match {
							case false => "Video"
							case true => "Post"
						}),
						"categoryId" -> value.entry.categoryId,
						"url" -> value.entry.url,
						"tags" -> value.entry.tags(0).split(",").map(_.trim()),
						"photos" -> photos,
						"commentsNum" -> value.entry.commentsNum)).makeQueryDocument
					newEntry match {
						case false => Application.blogEntriesCollection.update(selector, modifier).map {
							e =>
								{
									Logger.debug(e.toString)
									/*
										 * delete associated files
										 */

									//for {
									//	files <- Option(new File(path).listFiles)
									//	file <- files if file.getName.endsWith(".jpg")
									//} 
									//file.delete()

									Redirect(routes.BlogController.get(id))
								}
						}
						case true => Application.blogEntriesCollection.insert(modifier).map {
							e => Logger.debug(e.toString); Redirect(routes.BlogController.get(id))
						}
					}
				}
			})
	}

	private def getBlogEntry(id: String): Option[BlogEntry] = {
		val qb = QueryBuilder().query(Json.obj("id" -> id))
		val futureBlogEntry = Application.blogEntriesCollection.find[JsValue](qb).toList.map(
			_.headOption match {
				case Some(h) => Some(h.as[BlogEntry])
				case _ => None
			})
		val duration10000 = Duration(100000, "millis")
		val blogEntry = Await.result(futureBlogEntry, duration10000).asInstanceOf[Option[BlogEntry]]
		blogEntry
	}

	def add() = Action { implicit request =>
		Ok(views.html.blog.entry_form(blogEntryForm))
	}

	def edit(id: String) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			Application.blogEntriesCollection.find[JsValue](qb).toList.map { entries =>
				val blogEntry = entries.head.as[BlogEntry]
				Ok(views.html.blog.entry_form(blogEntryForm.fill(BlogEntrySubmit(blogEntry)), blogEntry.photos.filter(_.metadata.typeOf == "slider")))
			}
		}
	}

	def summary() = Action { implicit request =>

		Async {
			val qbAll = QueryBuilder().query(Json.obj()).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}

	def byCategory(categoryId: String) = Action { implicit request =>
		Async {
			val qbAll = QueryBuilder().query(Json.obj("categoryId" -> categoryId)).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}

	def byTag(tagName: String) = Action { implicit request =>
		Async {
			val qbAll = QueryBuilder().query(Json.obj("tags" -> Json.obj("$in" -> Seq(tagName)))).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}

	def byUser(userId: String) = Action { implicit request =>
		Async {
			val oUser = UserController.getUser(userId, "nickname")

			oUser match {
				case Some(user) => {
					val qbAll = QueryBuilder().query(Json.obj("id" -> Json.obj("$in" -> user.posts))).sort("created" -> Descending)
					Application.blogEntriesCollection.find[JsValue](qbAll).toList().map { entries =>
						Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
					}
				}
				case _ => Future(BadRequest(s"User $userId was not found"))
			}
		}
	}

	def archiveByYear(year: Int) = Action { implicit request =>
		Async {
			val start = new DateTime(year, 1, 1, 0, 0)
			val end = new DateTime(year, 12, 31, 23, 59)
			val interval = new Interval(start, end)
			val qbAll = QueryBuilder().query(
				Json.obj("created" -> Json.obj(
					"$gte" -> start.toInstant().getMillis(),
					"$lte" -> end.toInstant().getMillis()))).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qbAll).toList().map { entries =>
				Ok(views.html.blog.summary(entries.map(r => r.as[BlogEntry]), categories))
			}
		}
	}

	def get(id: String) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj("id" -> id))
			Application.blogEntriesCollection.find[JsValue](qb).toList().map { entries =>
				Ok(views.html.blog.entry(entries.head.as[BlogEntry], categories))
			}
		}
	}

	def getRecentEntries(num: Int) = Action { implicit request =>
		Async {
			val qb = QueryBuilder().query(Json.obj()).sort("created" -> Descending)
			Application.blogEntriesCollection.find[JsValue](qb).toList(num).map { entries =>
				Ok(views.html.blog.recent_posts(entries.map(r => r.as[BlogEntry])))
			}
		}
	}
}