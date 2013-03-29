package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import javax.imageio.ImageIO
import java.io.File
import org.imgscalr.Scalr
import org.joda.time.DateTime
import java.awt.image.BufferedImage

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class MiscSpec extends Specification {

	"Application" should {

		"scale the image correctly" in {

			val width = 940
			val height = 367
			val ratio = 940*1.0/367
			val originalImage = ImageIO.read(new File("C:\\base\\source\\naturahub\\public\\img\\tmp\\1364530881812.JPG"))
			val origX = originalImage.getWidth()
			val origY = originalImage.getHeight()
			val ovx = origX - width
			val ovy = origY - height
			var mode = Scalr.Mode.AUTOMATIC
			var cropW = width*1.0
			var cropH = height*1.0
			var scaledImage: BufferedImage = null
			
			/*
			 * Same, same
			 */
			if(origX/origY == width/height) {
				scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height)
				println("exactly same size")
			}
			/*
			 * Picture bigger than the window
			 */
			else if(origX >= width && origY >= height) {
				scaledImage = Scalr.crop(originalImage, ovx/2, ovy/2, width, height, null)
				println("both width and height are bigger")
			} 
			/*
			 * Either width or height is smaller, but not both 
			 */
			else {
				val minV = math.min(origX, origY)
				if(minV == origX) {
					cropW = origX
					cropH = origX / ratio
				} else {
					cropH = origY
					cropW = origY*ratio
				}
				if(origY < cropH) {
					println(s"origY < cropH is true: cropW: $cropW, cropH: $cropH")
					cropW = (cropH - origY)*cropW/cropH
					cropH = origY
				}
				if(origX < cropW) {
					println(s"origX < cropW is true: cropW: $cropW, cropH: $cropH")
					cropH = (cropW - origX)*cropH/cropW
					cropW = origX
				}
				scaledImage = Scalr.crop(originalImage, (origX - cropW)/2 toInt, (origY - cropH)/2 toInt, cropW toInt, cropH toInt, null)
				scaledImage = Scalr.resize(scaledImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height)
				println(s"Either width or height is smaller: origX: $origX, origY: $origY")
			}
			
			/*if(ovx < 0 || ovy < 0) {
				val ovx2 = math.abs(originalImage.getWidth() - width)
				val ovy2 = math.abs(originalImage.getHeight() - height)
				val minD = math.m(ovx2, ovy2)
				if(minD == originalImage.getWidth()) {
					cropW = originalImage.getWidth()
					cropH = originalImage.getWidth()*ratio
				} else {
					cropH = originalImage.getHeight()
					cropW = originalImage.getHeight()*ratio
				}
			}
			
			val ovx2 = if(ovx < 0) ? 
			*/
			println(s"cropW: $cropW, cropH; $cropH")

			//val scaledImage = Scalr.resize(originalImage, mode, cropW, cropH)
			/*val scaledImage = Scalr.crop(originalImage, ovx/2, ovy/2, 940, 367, null)
			//val scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height)*/
			val name = DateTime.now().getMillis()
			ImageIO.write(
				scaledImage,
				"jpg",
				new File("C:\\base\\source\\naturahub\\public\\img\\tmp\\test." + name + ".jpg"))
				println("************* "+name)
			true
		}

	}
}