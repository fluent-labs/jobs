package io.fluentlabs.jobs.definitions

import io.fluentlabs.jobs.SparkSessionBuilder
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.SparkSession

abstract class DefinitionsJob(
    source: String,
    previousState: String,
    nextState: String
) {
  @transient lazy val log: Logger =
    LogManager.getLogger("Definitions analysis job")

  // Methods for subclasses to implement
  def run(inputPath: String, outputPath: String)(implicit
      spark: SparkSession
  ): Unit

  def main(args: Array[String]): Unit = {
    val (definitionsBucketPath, version) = args match {
      case Array(path, version) => (path, version)
      case _ =>
        throw new IllegalArgumentException(
          "Invalid arguments, require bucket path and version"
        )
    }
    log.info(
      s"Running with arguments bucket path: $definitionsBucketPath and version: $version"
    )

    implicit val spark: SparkSession = SparkSessionBuilder
      .build(s"$source $nextState job")
    log.info("Created spark session")

    // Values we calculated
    val inputPath: String =
      s"$definitionsBucketPath/$source/$version/$previousState/"
    val outputPath: String =
      s"$definitionsBucketPath/$source/$version/$nextState/}"

    run(inputPath, outputPath)
  }
}
