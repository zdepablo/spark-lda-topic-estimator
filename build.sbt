name := "spark-lda-topic-estimator"

organization := "com.github.master"

spName := "master/spark-lda-topic-estimator"

version := "0.0.1"

sparkVersion := "1.6.3"

scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6")

spShortDescription := "Spark LDA Topic Estimator"

spDescription := """Spectral parameter recovery for the number LDA topics.""".stripMargin

licenses := Seq("BSD 2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause"))

spDistDirectory := target.value

sparkComponents ++= Seq("mllib", "sql")

parallelExecution := false

spIncludeMaven := true

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test"
)

sonatypeProfileName := "com.master.github"

publishMavenStyle := true

homepage := Some(url("https://github.com/master/spark-lda-topic-estimator"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/master/spark-lda-topic-estimator"),
    "scm:git@github.com:master/spark-lda-topic-estimator.git"
  )
)

developers := List(
  Developer(id="master", name="Oleg Smirnov", email="oleg.smirnov@gmail.com", url=url("http://nord.org.ua"))
)

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
