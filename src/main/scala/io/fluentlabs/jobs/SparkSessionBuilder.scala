package io.fluentlabs.jobs

import org.apache.spark.sql.SparkSession

// $COVERAGE-OFF$
object SparkSessionBuilder {
  def build(name: String): SparkSession = {
    val s3Endpoint =
      sys.env.getOrElse("s3Endpoint", "https://fra1.digitaloceanspaces.com")

    SparkSession
      .builder()
      .appName(name)
      .config("fs.s3a.access.key", sys.env("AWS_ACCESS_KEY_ID"))
      .config("fs.s3a.secret.key", sys.env("AWS_SECRET_ACCESS_KEY"))
      .config("fs.s3a.endpoint", s3Endpoint)
      .getOrCreate()
  }
}
// $COVERAGE-ON$
