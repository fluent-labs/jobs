package io.fluentlabs.jobs.definitions.clean

import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import org.apache.spark.sql.{DataFrame, SparkSession}

abstract class WiktionaryCleaningJob[T](source: String)
    extends DefinitionsCleaningJob[T](source)
    with WiktionaryParser {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit spark: SparkSession): DataFrame =
    loadWiktionaryDump(path)
}
