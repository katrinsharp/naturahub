package utils

import javax.imageio.ImageIO
import java.io.File
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import play.api.Logger
import org.joda.time.DateTime

object Image {
	
	val RECIPE_SLIDER_SIZE = (940, 367)
	
	def saveAsSlider(inputFile: File): String = {
		resizeAndSave(inputFile, RECIPE_SLIDER_SIZE._1, RECIPE_SLIDER_SIZE._2)
	}
	
	def resizeAndSave(inputFile: File, width: Int, height: Int): String = {
		
		val ratio = width*1.0/height
		val originalImage = ImageIO.read(inputFile)
		val origX = originalImage.getWidth()
		val origY = originalImage.getHeight()
		val ovx = origX - width
		val ovy = origY - height
		var cropW = width*1.0
		var cropH = height*1.0
		var scaledImage: BufferedImage = null
		
		/*
		 * Same, same
		 */
		if(origX == width && origY == height) {
			scaledImage = originalImage
			Logger.debug("exactly same size")
		}
		/*
		 * Picture bigger than the window
		 */
		else if(origX >= width && origY >= height) {
			scaledImage = Scalr.crop(originalImage, ovx/2, ovy/2, width, height, null)
			Logger.debug("both width and height are bigger")
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
				Logger.debug(s"origY < cropH is true: cropW: $cropW, cropH: $cropH")
				cropW = (cropH - origY)*cropW/cropH
				cropH = origY
			}
			if(origX < cropW) {
				Logger.debug(s"origX < cropW is true: cropW: $cropW, cropH: $cropH")
				cropH = (cropW - origX)*cropH/cropW
				cropW = origX
			}
			scaledImage = Scalr.crop(originalImage, (origX - cropW)/2 toInt, (origY - cropH)/2 toInt, cropW toInt, cropH toInt, null)
			scaledImage = Scalr.resize(scaledImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height)
			Logger.debug(s"Either width or height is smaller: origX: $origX, origY: $origY")
		}
		
		Logger.debug(s"cropW: $cropW, cropH; $cropH")

		val name = DateTime.now().getMillis() + ".jpg"
		ImageIO.write(
			scaledImage,
			"jpg",
			new File("C:\\base\\source\\naturahub\\public\\img\\tmp\\" + name))
		Logger.debug("************* "+name)
		name	
	}

}