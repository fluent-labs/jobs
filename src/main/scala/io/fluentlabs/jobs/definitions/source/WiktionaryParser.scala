package io.fluentlabs.jobs.definitions.source

import com.databricks.spark.xml.XmlDataFrameReader
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, explode, regexp_extract, udf}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.util.{Failure, Success, Try}

trait WiktionaryParser {
  @transient lazy val logger: Logger =
    LogManager.getLogger(this.getClass.getName)

  def loadWiktionaryDump(
      path: String
  )(implicit spark: SparkSession): DataFrame = {
    spark.read
      .option("rowTag", "page")
      .xml(path)
      .select("revision.text._VALUE", "title", "id")
      .withColumnRenamed("title", "token")
      .withColumnRenamed("_VALUE", "text")
      .filter(row => filterMetaArticles(row))
  }

  val metaArticleTitles: Set[String] =
    Set(
      "MediaWiki:",
      "MediaWiki talk:",
      "Template:",
      "Template talk:",
      "Wiktionary:",
      "Wiktionary talk:",
      "User:",
      "User talk:",
      "Category:",
      "Category talk:",
      "Help:",
      "File:",
      "File talk:",
      "Appendix:",
      "Module:",
      "Module talk:",
      "Talk:"
    )

  def filterMetaArticles(row: Row): Boolean = {
    val title = row.getAs[String]("token")
    metaArticleTitles.forall(prefix => !title.startsWith(prefix))
  }

  // String.repeat is only implemented on some JVMs
  // Removing this has burned me twice.
  // Run the unit tests in the docker container before trying to remove it again.
  def repeat(token: String, count: Int): String = {
    (0 until count).map(_ => token).mkString
  }

  val caseInsensitiveFlag = "(?i)"
  val periodMatchesNewlineFlag = "(?s)"
  val oneOrMoreEqualsSign = "=+"
  val doubleEqualsSign = "=="
  val tripleEqualsSign = "==="
  val optionalWhiteSpace = " *"
  val anythingButEqualsSign = "[^=]*"
  val lazyMatchAnything = "(.*?)"
  val spaceOrNewline = "[ |\n]+"
  val nextSection = s"(?>== *[A-Za-z0-9]+ *==$spaceOrNewline)"
  val nextSectionOrEndOfFile = s"(?>$nextSection|\\Z)+"

  def headingRegex(equalsCount: Int): String =
    repeat(
      "=",
      equalsCount
    ) + optionalWhiteSpace + anythingButEqualsSign + optionalWhiteSpace + repeat(
      "=",
      equalsCount
    ) + anythingButEqualsSign // Needed or else outer equals will be ignored
  // Subtle but '== Test ==' will match '=== Test ===' at this point: '="== Test =="='

  // == name ==
  def sectionRegex(sectionName: String): String =
    periodMatchesNewlineFlag + caseInsensitiveFlag + doubleEqualsSign + optionalWhiteSpace + sectionName + optionalWhiteSpace + doubleEqualsSign + lazyMatchAnything + nextSectionOrEndOfFile

  // === name ===
  def subSectionRegex(sectionName: String): String =
    periodMatchesNewlineFlag + caseInsensitiveFlag + tripleEqualsSign + optionalWhiteSpace + sectionName + optionalWhiteSpace + tripleEqualsSign + lazyMatchAnything + nextSectionOrEndOfFile

  // Heading extraction below here

  def getHeadings(
      data: DataFrame,
      equalsCount: Integer
  ): DataFrame = {
    data
      .select(explode(getHeadingsUDF(equalsCount)(col("text"))))
      .distinct()
      .coalesce(1)
      .sort(col("col"))
  }

  def getHeadingsUDF(equalsCount: Int): UserDefinedFunction =
    udf((text: String) => getHeadingsFromDocument(text, equalsCount))

  def getHeadingsFromDocument(text: String, equalsCount: Int): Array[String] = {
    Try(
      text.linesIterator
        .filter(line => line.matches(headingRegex(equalsCount)))
        .map(line => getHeadingFromLine(line, equalsCount))
        .toArray
    ) match {
      case Success(headings) => headings
      case Failure(e)        =>
        // Most likely means an issue with the filter clause above
        logger.error(s"Failed to parse document $text", e)
        Array()
    }
  }

  def getHeadingFromLine(line: String, equalsCount: Int): String = {
    Try(line.replaceAll(repeat("=", equalsCount), "").trim.toLowerCase) match {
      case Success(heading) => heading
      case Failure(e) =>
        logger.error(s"Failed to parse line $line", e)
        "ERROR"
    }
  }

  // Section extraction down here

  def extractSection(data: DataFrame, name: String): DataFrame =
    data.withColumn(
      name.toLowerCase(),
      regexp_extract(col("text"), sectionRegex(name), 1)
    )

  def extractSections(
      data: DataFrame,
      sections: Array[String]
  ): DataFrame = {
    sections
      .foldLeft(data.toDF())((data, section) => extractSection(data, section))
  }

  def extractSubsection(data: DataFrame, name: String): DataFrame =
    data.withColumn(
      name.toLowerCase(),
      regexp_extract(col("text"), subSectionRegex(name), 1)
    )
  def extractSubsections(
      data: DataFrame,
      sections: Array[String]
  ): DataFrame = {
    sections.foldLeft(data)((data, section) => extractSection(data, section))
  }
}
