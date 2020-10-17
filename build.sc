// -*- mode: scala -*-

import mill._, scalalib._, publish._, scalajslib._
import ammonite.ops._
import scala.util.Properties

object meta {

  val crossVersions = for { 
   scala <- Seq("2.13.2")
   scalaJS <- Seq("1.2.0")
  } yield (scala, scalaJS)

  implicit val wd: os.Path = os.pwd

  def nonEmpty(s: String): Option[String] = s.trim match {
    case v if v.isEmpty => None
    case v => Some(v)
  }

  val versionFromEnv = Properties.propOrNone("PUBLISH_VERSION")
  val gitSha = nonEmpty(%%("git", "rev-parse", "--short", "HEAD").out.trim)
  val gitTag = nonEmpty(%%("git", "tag", "-l", "-n0", "--points-at", "HEAD").out.trim)
  val publishVersion = (versionFromEnv orElse gitTag orElse gitSha).getOrElse("latest")
}

object cyclone extends Cross[Cyclone](meta.crossVersions: _*)
class Cyclone(val crossScalaVersion: String, val crossScalaJSVersion: String) extends PublishModule with ScalaJSModule with CrossScalaModule { self =>
  def publishVersion = meta.publishVersion

  override def millSourcePath: Path = super.millSourcePath / os.up

  override def artifactName = "cyclone"

  def scalaJSVersion = T(crossScalaJSVersion)

  def pomSettings = PomSettings(
    description = "Cyclic AirStream stateful components for Laminar UI",
    organization = "com.github.vic",
    url = "https://github.com/vic/cyclone",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("vic", "cyclone"),
    developers = Seq(
      Developer("vic", "Victor Borja", "https://github.com/vic")
    )
  )

  override def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"com.raquo::laminar::0.11.0"
  )

//  object tests extends Tests {
//    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.4") ++ self.compileIvyDeps()
//    def testFrameworks = Seq("utest.runner.Framework")
//  }
}
