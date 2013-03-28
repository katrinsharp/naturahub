import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "naturahub"
  val appVersion      = "1.0-SNAPSHOT"
  	
  resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

  val appDependencies = Seq(
    // Add your project dependencies here,
    //jdbc,
    //anorm
  	"org.reactivemongo" %% "play2-reactivemongo" % "0.8",
  	"com.amazonaws" % "aws-java-sdk" % "1.3.11",
    "org.imgscalr" % "imgscalr-lib" % "4.2"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  )

}
