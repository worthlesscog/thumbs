name := "Thumbs"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:postfixOps",
    "-language:reflectiveCalls",
    "-unchecked"
)

lazy val osName = System.getProperty("os.name") match {
    case n if n.startsWith("Linux")   => "linux"
    case n if n.startsWith("Mac")     => "mac"
    case n if n.startsWith("Windows") => "win"
    case _                            => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "graphics") //, "fxml", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map(m =>
    "org.openjfx" % s"javafx-$m" % "11" classifier osName
)

lazy val monkeys = Seq("core", "bmp", "jpeg") //, "hdr", "icns", "ico", "iff", "pcx", "pict", "pnm", "psd", "sgi", "tga", "tiff")
libraryDependencies ++= monkeys.map(monkey =>
    "com.twelvemonkeys.imageio" % s"imageio-$monkey" % "3.4.1"
)

libraryDependencies += "com.google.guava" % "guava" % "27.0.1-jre"

assemblyMergeStrategy in assembly := {
    case PathList("module-info.class") => MergeStrategy.rename
    case x                             =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}

mainClass in assembly := Some("com.worthlesscog.thumbs.Launcher")
