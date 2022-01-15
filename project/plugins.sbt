// Quality
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// Make fat jars for Spark jobs
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.1.0")

// Publishing
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.2")
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")
