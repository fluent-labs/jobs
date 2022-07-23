package io.fluentlabs.jobs.definitions.analyze

import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import org.apache.spark.sql.{DataFrame, SparkSession}

// Use this when you want to know what kind of sections a backup has. Good for getting the rough structure of the dump
class WiktionarySectionFinder(source: String)
    extends DefinitionsAnalysisJob(source)
    with WiktionaryParser {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit spark: SparkSession): DataFrame =
    loadWiktionaryDump(path)

  def analyze(data: DataFrame, outputPath: String)(implicit
      spark: SparkSession
  ): Unit = getHeadings(data, 1).write.csv(outputPath)
}
