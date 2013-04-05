package utils

import javax.imageio.ImageIO
import java.io.File
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import play.api.Logger
import org.joda.time.DateTime

object Image {
	
	val RECIPE_SLIDER_SIZE = (940, 367)
	val RECIPE_PREVIEW_WIDTH = 300
	
	def saveAsSlider(inputFile: File): String = {
		//resizeAndSave(inputFile, RECIPE_SLIDER_SIZE._1, RECIPE_SLIDER_SIZE._2)
		val originalImage = ImageIO.read(inputFile)
		val scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, RECIPE_SLIDER_SIZE._1)
		val scaledImage2 = Scalr.crop(scaledImage, 0, (scaledImage.getHeight() - RECIPE_SLIDER_SIZE._2)/2 toInt, RECIPE_SLIDER_SIZE._1, RECIPE_SLIDER_SIZE._2, null)
		save(scaledImage2)
		
	}
	
	def saveAsPreview(inputFile: File): String = {
		val originalImage = ImageIO.read(inputFile)
		val scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, RECIPE_PREVIEW_WIDTH)
		save(scaledImage)
	}
	
	def saveAsIs(inputFile: File): String = {
		val originalImage = ImageIO.read(inputFile)
		save(originalImage)
	}
	
	def save(image: BufferedImage): String = {
		val name = DateTime.now().getMillis() + ".jpg"
		ImageIO.write(
			image,
			"jpg",
			new File("C:\\base\\source\\naturahub\\public\\img\\tmp\\" + name))
		Logger.debug("************* "+name)
		name	
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
		if(origX/origY == width/height) {
			scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height)
			Logger.debug("same ratio")
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

		save(scaledImage)	
	}

}