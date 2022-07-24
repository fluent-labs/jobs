package io.fluentlabs.jobs.definitions.analyze

import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import io.fluentlabs.jobs.definitions.{
  WiktionaryTemplate,
  WiktionaryTemplateInstance
}
import org.apache.spark.sql.functions.{arrays_zip, col, explode}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

class WiktionaryTemplateExtractor(source: String)
    extends DefinitionsAnalysisJob(source)
    with WiktionaryParser {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit spark: SparkSession): DataFrame =
    loadWiktionaryDump(path)

  def analyze(data: DataFrame, outputPath: String)(implicit
      spark: SparkSession
  ): Unit = {
    val templateInstances =
      extractTemplateInstances(data).cache()
    templateInstances.write.csv(s"$outputPath/instances.csv")

    val templates = extractTemplateCount(templateInstances)
    templates.write.csv(s"$outputPath/templates.csv")
  }

  val leftBrace = "\\{"
  val rightBrace = "\\}"
  val pipe = "\\|"
  val notPipeCaptureGroup: String = "([^" + pipe + rightBrace + "]+)"
  val notRightBraceCaptureGroup: String =
    "(" + pipe + "[^" + rightBrace + "]*)?"

  // {{a|b}}
  val templateRegex: String =
    leftBrace + leftBrace + notPipeCaptureGroup + notRightBraceCaptureGroup + rightBrace + rightBrace

  def extractTemplateInstances(
      data: DataFrame
  )(implicit spark: SparkSession): Dataset[WiktionaryTemplateInstance] = {
    import spark.implicits._

    data
      .withColumn("name", regexp_extract_all("text", templateRegex, 1))
      .withColumn("arguments", regexp_extract_all("text", templateRegex, 2))
      .select(
        explode(arrays_zip(col("name"), col("arguments")))
          .alias("template")
      )
      .select(col("template.*"))
      .sort("name")
      .as[WiktionaryTemplateInstance]
  }

  def extractTemplateCount(
      data: Dataset[WiktionaryTemplateInstance]
  )(implicit spark: SparkSession): Dataset[WiktionaryTemplate] = {
    import spark.implicits._

    data
      .groupBy("name")
      .count()
      .sort(col("count").desc)
      .as[WiktionaryTemplate]
  }
}
