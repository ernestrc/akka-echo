import com.typesafe.sbt.SbtGit.GitKeys._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.{PathList, MergeStrategy}
import sbtbuildinfo.BuildInfoKeys._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin._
import spray.revolver.RevolverPlugin._

object Build extends sbt.Build {

  val scalaV = "2.11.8"
  val akkaV = "2.4.6"

  val commonSettings = Seq(
    organization := "build.unstable",
    version := "0.1.0",
    scalaVersion := scalaV,
    javaOptions += "-Xmx5G",
    scalacOptions := Seq(
      "-unchecked",
      "-Xlog-free-terms",
      "-deprecation",
      "-feature",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-unused",
      "-encoding", "UTF-8",
      "-target:jvm-1.8"
    )
  )

  val meta = """META.INF(.)*""".r

  val assemblyStrategy = assemblyMergeStrategy in assembly := {
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.discard
    case meta(_) => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }

  val server: Project = Project("echo-akka", file("server"))
    .settings(Revolver.settings: _*)
    .settings(commonSettings: _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .enablePlugins(BuildInfoPlugin)
    .enablePlugins(com.typesafe.sbt.GitVersioning)
    .settings(
      assemblyStrategy,
      assemblyJarName in assembly := "echo-akka-assembly.jar",
      libraryDependencies ++= {
        Seq(
          //core
          "com.typesafe.akka" %% "akka-http-core" % akkaV,
          "ch.qos.logback" % "logback-classic" % "1.0.13",
          "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test"
        )
      }
    )
}
