ThisBuild / tlBaseVersion := "0.1" // your current series x.y

ThisBuild / organization := "com.github.jatcwang"
ThisBuild / organizationName := "IOHandle"
ThisBuild / startYear := Some(2025)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers ++= List(
  tlGitHubDev("jatcwang", "Jacob Wang"),
)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

val Scala2 = "2.13.16"
val Scala3 = "3.3.6"
ThisBuild / crossScalaVersions := Seq(Scala2, Scala3)
ThisBuild / scalaVersion := Scala2
//ThisBuild / scalaVersion := Scala3

lazy val iohandleRoot = Project("root", file("."))
  .aggregate(iohandle, examples)
  .settings(
    name := "IOHandle",
    publish / skip := true,
  )
lazy val iohandle = Project("iohandle", file("modules/iohandle"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-mtl" % "1.5.0",
      "org.typelevel" %% "cats-effect" % "3.6.0",
    ),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.1.0",
      "org.typelevel" %% "munit-cats-effect" % "2.1.0",
    ).map(_ % Test),
  )
  .settings(
    Test / sourceGenerators ++= (if (tlIsScala3.value) {
                                   Seq(
                                     lookupAndReplace(
                                       file("modules/iohandle/src/test/scala-2/iohandletest"),
                                       List(
                                         "IOHandleSpec.scala",
                                         "IOExtensionSpec.scala",
                                       ),
                                     ).taskValue,
                                   )
                                 } else Seq.empty),
  )

def lookupAndReplace(baseSrcDir: File, sourceFileNames: List[String]): Def.Initialize[Task[Seq[File]]] = Def.task {
  val targetDir = (Test / sourceManaged).value
  sourceFileNames.map { fileName =>
    val content = IO.read(baseSrcDir / fileName)
    val targetFile = targetDir / fileName
    IO.write(targetFile, editSourceCodeForScala3Compilation(content))
    targetFile
  }
}

def editSourceCodeForScala3Compilation(content: String): String = {
  content
    .replaceAll("implicit handle =>", "")
    .replaceAll(
      "(package iohandletest)",
      """$1
        |/* This file is generated from Scala 2.13, with textual replacements
        |  to make context functions compatible */""".stripMargin,
    )
    .replaceAll("""(?s)/\* start:scala-2-only.+?/\* end:scala-2-only \*/""", "")
}

lazy val examples = Project("examples", file("modules/examples"))
  .dependsOn(iohandle)
  .settings(
    Compile / fork := true,
  )
  .settings(publish / skip := true)

lazy val commonSettings = Seq(
)
