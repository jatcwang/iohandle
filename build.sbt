ThisBuild / tlBaseVersion := "0.2" // your current series x.y

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
//ThisBuild / scalaVersion := Scala2
ThisBuild / scalaVersion := Scala3

ThisBuild / tlCiMimaBinaryIssueCheck := false

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
                                     codeLookupAndReplace(
                                       file("modules/iohandle/src/test/scala-2/iohandletest"),
                                       List(
                                         "IOHandleSpec.scala",
                                         "IOExtensionSpec.scala",
                                       ),
                                       (Test / sourceManaged),
                                       editTestSourceCodeForScala3Compilation,
                                     ).taskValue,
                                   )
                                 } else Seq.empty),
    Compile / sourceGenerators ++= (if (tlIsScala3.value) {
                                      Seq(
                                        codeLookupAndReplace(
                                          file("modules/iohandle/src/main/scala-2/iohandle"),
                                          List(
                                            "ioscreen.scala",
                                          ),
                                          (Compile / sourceManaged),
                                          editLibSourceCodeForScala3Compilation,
                                        ).taskValue,
                                      )
                                    } else Seq.empty),
  )
  .settings(commonSettings)

def codeLookupAndReplace(
  baseSrcDir: File,
  sourceFileNames: List[String],
  targetDir: Def.Initialize[File],
  editFunc: String => String,
): Def.Initialize[Task[Seq[File]]] = Def.task {
  val dir = targetDir.value
  sourceFileNames.map { fileName =>
    val content = IO.read(baseSrcDir / fileName)
    val targetFile = dir / fileName
    IO.write(targetFile, editFunc(content))
    targetFile
  }
}

def editTestSourceCodeForScala3Compilation(content: String): String = {
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

def editLibSourceCodeForScala3Compilation(content: String): String = {
  content
    .replaceAll("""(?s)/\* start:scala-2-only.+?/\* end:scala-2-only \*/""", "")
    // Convert Scala 2 implicit class extension methods to Scala 3 extension methods syntax
    // This isn't perfect e.g. doesn't handle nested [] in type parameter blocks
    .replaceAll("""implicit class [^\[]+\[([^\(]+)\]\(val ([^\)]+)\) extends AnyVal""", "extension [$1]($2)")
}

lazy val commonSettings = Seq(
)

lazy val examples = Project("examples", file("modules/examples"))
  .dependsOn(iohandle)
  .settings(
    Compile / fork := true,
  )
  .settings(publish / skip := true)
