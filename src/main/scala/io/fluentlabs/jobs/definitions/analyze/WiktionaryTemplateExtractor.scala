package io.fluentlabs.jobs.definitions.analyze

import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import io.fluentlabs.jobs.definitions.{
  WiktionaryTemplate,
  WiktionaryTemplateInstance
}
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, element_at, posexplode, udf}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

import scala.util.{Failure, Success, Try}

class WiktionaryTemplateExtractor(source: String)
    extends DefinitionsAnalysisJob(source)
    with WiktionaryParser {

  @transient override lazy val logger: Logger =
    LogManager.getLogger("Wiktionary Template Extractor")

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

  val templateRegex: String =
    leftBrace + leftBrace + notPipeCaptureGroup + notRightBraceCaptureGroup + rightBrace + rightBrace

  def extractTemplateInstances(
      data: DataFrame
  )(implicit spark: SparkSession): Dataset[WiktionaryTemplateInstance] = {
    import spark.implicits._

    data
      .select(posexplode(regexp_extract_templates(col("text"))))
      .select(
        element_at(col("col"), 1),
        element_at(col("col"), 2)
      ) // Columns start at 1 not 0
      .withColumnRenamed("element_at(col, 1)", "name")
      .withColumnRenamed("element_at(col, 2)", "arguments")
      .sort("name")
      .as[WiktionaryTemplateInstance]
  }

  def extractTemplateCount(
      data: Dataset[WiktionaryTemplateInstance]
  )(implicit spark: SparkSession): Dataset[WiktionaryTemplate] = {
    import spark.implicits._

    data.groupBy("name").count().sort(col("count").desc).as[WiktionaryTemplate]
  }

  val extractTemplatesFromString: String => Array[Array[String]] =
    (input: String) =>
      Try(
        templateRegex.r
          .findAllIn(input)
          .matchData
          .map(m => {
            val templateName = m.group(1)
            val arguments = if (m.groupCount == 2) m.group(2) else ""
            Array(templateName, arguments)
          })
          .toArray
      ) match {
        case Success(value) => value
        case Failure(_)     => Array(Array("Error", input))
      }

  val regexp_extract_templates: UserDefinedFunction = udf(
    extractTemplatesFromString
  )
}
