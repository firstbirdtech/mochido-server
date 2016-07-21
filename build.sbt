name := "mochido-server"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo

libraryDependencies ++= {
  val akkaV = "2.4.4"
  val scalaTestV = "2.2.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "com.iheart" %% "ficus" % "1.2.3"
  )
}

assemblyJarName in assembly := "mochido-server.jar"
mainClass in assembly := Some("mochido.server.Server")

herokuFatJar in Compile := Some((assemblyOutputPath in assembly).value)