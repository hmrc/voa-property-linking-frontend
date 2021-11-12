import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.Keys._
import play.sbt.PlayImport.PlayKeys

/**
 * Build of UI in JavaScript
 */
object JavaScriptBuild {

  val uiDirectory = SettingKey[File]("ui-directory")

  val gulpBuild = TaskKey[Int]("gulp-build")
  val gulpWatch = TaskKey[Int]("gulp-watch")
  val npmInstall = TaskKey[Int]("npm-install")


  val javaScriptUiSettings = Seq(

    // the JavaScript application resides in "ui"
    uiDirectory := (baseDirectory in Compile) { _ /"app" / "assets" }.value,

    // add "npm" and "gulp" commands in sbt
    commands ++= uiDirectory { base => Seq(Gulp.gulpCommand(base), npmCommand(base))}.value,

    npmInstall := {
      val result = Gulp.npmProcess(uiDirectory.value, "install").run().exitValue()
      if (result != 0)
        throw new Exception("Npm install failed.")
      result
    },
    gulpBuild := {
      val result = Gulp.gulpProcess(uiDirectory.value, "default").run().exitValue()
      if (result != 0)
        throw new Exception("Gulp build failed.")
      result
    },

    (Test / test) := {(Test / test) dependsOn gulpBuild}.value,
    gulpBuild := {gulpBuild dependsOn npmInstall}.value,

    // runs gulp before staging the application
    dist := {dist dependsOn gulpBuild}.value,

    // Turn off play's internal less compiler
    //lessEntryPoints := Nil,

    // Turn off play's internal JavaScript and CoffeeScript compiler
    //javascriptEntryPoints := Nil,
    //coffeescriptEntryPoints := Nil,

    // integrate JavaScript build into play build
    PlayKeys.playRunHooks += {uiDirectory.map(ui => Gulp(ui))}.value
  )

  def npmCommand(base: File) = Command.args("npm", "<npm-command>") { (state, args) =>
    if (sys.props("os.name").toLowerCase contains "windows") {
      scala.sys.process.Process("cmd" :: "/c" :: "npm" :: args.toList, base) !
    }
    else {
      scala.sys.process.Process("npm" :: args.toList, base) !
    }
    state
  }

}
