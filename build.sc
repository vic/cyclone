// -*- mode: scala -*-

import mill._
import scalalib._
import publish._
import scalajslib._
import ammonite.ops._
import coursier.{MavenRepository, Repository}
import mill.define.Cross

import scala.util.Properties

object meta {

  val crossVersions = for {
    scala   <- Seq("2.13.2")
    scalaJS <- Seq("1.1.0")
  } yield (scala, scalaJS)

  implicit val wd: os.Path = os.pwd

  def nonEmpty(s: String): Option[String] = s.trim match {
    case v if v.isEmpty => None
    case v              => Some(v)
  }

  val versionFromEnv = Properties.propOrNone("PUBLISH_VERSION")
  val gitSha         = nonEmpty(%%("git", "rev-parse", "--short", "HEAD").out.trim)
  val gitTag         = nonEmpty(%%("git", "tag", "-l", "-n0", "--points-at", "HEAD").out.trim)
  val publishVersion = (versionFromEnv orElse gitTag orElse gitSha).getOrElse("latest")
}

object cyclone extends Cross[Cyclone](meta.crossVersions: _*)
class Cyclone(val crossScalaVersion: String, val crossScalaJSVersion: String)
    extends PublishModule
    with ScalaJSModule
    with CrossScalaModule { self =>
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

  override def repositories: Seq[Repository] = Seq(
    MavenRepository("https://jitpack.io")
 ) ++ super.repositories

  override def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"com.raquo::laminar::0.11.0",
    ivy"com.raquo::airstream::0.11.1",
    ivy"dev.zio::zio::1.0.3",
    ivy"dev.zio::zio-streams::1.0.3"
//    ivy"com.github.zio-mesh::zio-arrow::63e61c9"
  )

  object tests extends Tests with ScalaJSModule {
    override def scalaJSVersion = crossScalaJSVersion
    def ivyDeps                 = Agg(ivy"com.lihaoyi::utest::0.7.4")
    def testFrameworks          = Seq("utest.runner.Framework")
  }
}

object docs extends ScalaModule with ScalaJSModule {
  val cross                              = meta.crossVersions.map { case (a, b) => Seq(a, b) }.head
  override def scalaVersion: T[String]   = cross.head
  override def scalaJSVersion: T[String] = cross.last
  override def moduleDeps                = Seq(cyclone(cross.head, cross.last))
}
