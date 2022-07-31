// Quality
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.1")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// Make fat jars for Spark jobs
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")

// Publishing
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.0")
