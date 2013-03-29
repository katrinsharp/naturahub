package models;

import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.imgscalr.Scalr
import play.Logger
import utils.S3Blob

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io._
import java.net.MalformedURLException
import java.net.URL
import java.util.UUID

import play.api.libs.json._
import play.api.libs.functional.syntax._

class S3Photo(var bucket: String, var key: String, var metadata: String) {
    
    def getUrl(path: String): String = {
        "https://s3.amazonaws.com/" + S3Blob.s3Bucket + "/" + path;
    }
	
	def getPrefix: String = {
		"https://s3.amazonaws.com/" + S3Blob.s3Bucket + "/"
	}

    def apply(file: File, maxWidthOrHeight: Int) = {
        
    	// store this here so the bucket can change without breaking stuff
        this.bucket = S3Blob.s3Bucket
        this.key = UUID.randomUUID().toString() + ".jpg"
        
        // resize the image
        val bufferedImage = ImageIO.read(file)

        val thumbnail = Scalr.resize(bufferedImage, maxWidthOrHeight)

        val byteArrayOutputStream = new ByteArrayOutputStream()
        ImageIO.write(thumbnail, "jpg", byteArrayOutputStream)
        val inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())

        val objectMetadata = new ObjectMetadata()
        objectMetadata.setContentType("image/jpg")

        // save the image in S3
        val putObjectRequest = new PutObjectRequest(bucket, key, inputStream, objectMetadata)
        putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
        
        if (S3Blob.amazonS3 == null) {
            Logger.error("Cloud not save Photo because amazonS3 was null")
        }
        else {
            S3Blob.amazonS3.putObject(putObjectRequest)
        }
    }   
}

object S3Photo extends Function3[String, String, String, S3Photo] {	
	implicit val recipeWrites = Json.writes[S3Photo]
	implicit val recipeReads = Json.reads[S3Photo]
	def unapply(photo: S3Photo) = new Some(photo.bucket, photo.key, photo.metadata)
	def apply(bucket: String, key: String, metadata: String) = new S3Photo(bucket, key, metadata)
}
