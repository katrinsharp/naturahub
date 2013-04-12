package utils

import scala.util.Random

object UniqueCode {
	
	def getRandomCode(): String = {
		val next = math.abs((new Random()).nextInt())
		Base62ToString(next)
	}
		
	private def ValToChar(value: Long): Char = {  
	
		value > 9 match {
			case true => {
				var ascii = (65 + (value.toInt - 10))
				if (ascii > 90) ascii += 6  
				ascii toChar  
			}
			case _ => value.toString().charAt(0)
		}
	}
	
	private def Base62ToString(value: Long): String = {
		
		val x = value % 62
		val y =  value / 62
		y > 0 match {
			case true => Base62ToString(y) + ValToChar(x).toString()
			case _ => ValToChar(x).toString()
		}
		
	}  
}