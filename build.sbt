name := "Facebook_Rest_Api"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

//resolvers ++= Seq(
//  "spray repo" at "http://repo.spray.io/"
//)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
//resolvers += Seq("spray repo" at "http://repo.spray.io/")
//libraryDependencies ++= {
//  val sprayVersion = "1.3.3"
//  val akkaVersion = "2.3.13"
//  Seq(
//    "io.spray" % "spray-can" % sprayVersion,
//    "io.spray" % "spray-routing" % sprayVersion,
//    "io.spray" % "spray-testkit" % sprayVersion,
//    "io.spray" % "spray-client" % sprayVersion,
//    "io.spray" %%  "spray-json" % "1.3.2",
//    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
//    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
//    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
//    "ch.qos.logback" % "logback-classic" % "1.1.3",
//    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
//  )
//}


libraryDependencies ++= {
  val akkaV = "2.3.13"
  val sprayV = "1.3.3"
  val sprayJsonV = "1.3.2"
  val specs2V = "3.6.5"
  val scalazV = "7.1.5"

  Seq(
    "io.spray"            %%  "spray-can"     % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-routing" % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-client" % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-json"    % sprayJsonV withSources() withJavadoc(),
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % specs2V % "test",
    "org.scalaz"          %%  "scalaz-core"   % scalazV,
    "org.json4s" %% "json4s-native" % "3.2.10",
    "com.typesafe.play"   %%  "play-json"     % "2.3.0"

  )
}
