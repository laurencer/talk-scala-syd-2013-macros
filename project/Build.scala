import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "com.rouesnel"
  val buildVersion      = "1.0.0"
  val buildScalaVersion = "2.10.2"

  val dependencies = Seq(
    "org.scalaz" %% "scalaz-core" % "7.0.3",
    "org.specs2" %% "specs2" % "2.1.1" % "test"
  )

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    shellPrompt  := ShellPrompt.buildShellPrompt,
    libraryDependencies ++= dependencies
  )
}

/**
 * Displays the current project, git branch and build version.
 */
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
  )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (
        currProject, currBranch, BuildSettings.buildVersion
      )
    }
  }
}

object TalkBuild extends Build {
  import BuildSettings._



  lazy val root = Project(
    "talk",
    file ("."),
    settings = buildSettings) aggregate(macros, examples)

  lazy val macros = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _))
  )

  lazy val examples = Project(
    "examples",
    file("examples"),
    settings = buildSettings) dependsOn(macros)
}