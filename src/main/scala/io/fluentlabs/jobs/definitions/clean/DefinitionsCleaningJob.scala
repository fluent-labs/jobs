package io.fluentlabs.jobs.definitions.clean

import io.fluentlabs.jobs.definitions.DefinitionsJob
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.{Dataset, SparkSession}

abstract class DefinitionsCleaningJob[T, U](source: String)
    extends DefinitionsJob(source, "raw", "clean") {
  @transient override lazy val log: Logger =
    LogManager.getLogger("Definitions parsing job")

  // Methods for subclasses to implement
  def getFilename(source: String, version: String): String
  def load(path: String)(implicit spark: SparkSession): Dataset[T]
  def clean(data: Dataset[T])(implicit spark: SparkSession): Dataset[U]

  override def run(inputPath: String, outputPath: String)(implicit
      spark: SparkSession
  ): Unit = {
    log.info("Loading data")
    val raw = load(inputPath)
    log.info("Loaded data")

    log.info("Cleaning data")
    val cleaned = clean(raw)
    log.info("Cleaning data")

    log.info(s"Saving to path $outputPath")
    cleaned.write.json(outputPath)
    log.info("Finished saving to S3")
  }
}
