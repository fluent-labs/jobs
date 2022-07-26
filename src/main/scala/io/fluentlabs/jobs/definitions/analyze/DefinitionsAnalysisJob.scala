package io.fluentlabs.jobs.definitions.analyze

import io.fluentlabs.jobs.definitions.DefinitionsJob
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.{Dataset, SparkSession}

abstract class DefinitionsAnalysisJob[T](source: String)
    extends DefinitionsJob(source, "raw", "analysis") {
  @transient override lazy val log: Logger =
    LogManager.getLogger("Definitions analysis job")

  // Methods for subclasses to implement
  def getFilename(source: String, version: String): String
  def load(path: String)(implicit spark: SparkSession): Dataset[T]
  def analyze(data: Dataset[T], outputPath: String)(implicit
      spark: SparkSession
  ): Unit

  def run(inputPath: String, outputPath: String)(implicit
      spark: SparkSession
  ): Unit = {
    log.info(s"Getting file from $inputPath")

    log.info("Loading data")
    val raw = load(inputPath)
    log.info("Loaded data")

    log.info("Analyzing data")
    analyze(raw, outputPath)
    log.info("Analyzed data")
  }
}
