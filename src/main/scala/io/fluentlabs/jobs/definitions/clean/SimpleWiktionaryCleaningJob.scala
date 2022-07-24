package io.fluentlabs.jobs.definitions.clean

import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, DataFrame, Dataset, SparkSession}

import scala.collection.immutable.ArraySeq

case class SimpleWiktionaryDefinitionEntry(
    pronunciation: List[String],
    tag: Option[String],
    examples: Option[List[String]],
    token: String,
    definition: String,
    ipa: String,
    subdefinitions: List[String],
    // Nice extras
    antonyms: List[String],
    homonyms: List[String],
    homophones: List[String],
    notes: List[String],
    otherSpellings: List[String],
    related: List[String],
    synonyms: List[String],
    usage: List[String]
)

object SimpleWiktionaryCleaningJob
    extends WiktionaryCleaningJob[SimpleWiktionaryDefinitionEntry](
      "simplewiktionary"
    ) {
  @transient override lazy val logger: Logger =
    LogManager.getLogger("Simple Wiktionary")

  val metaSections = List("pronunciation", "usage", "usage notes")

  // Parts of speech set here: http://www.lrec-conf.org/proceedings/lrec2012/pdf/274_Paper.pdf
  // TODO can this be the enum?
  val partOfSpeechMapping: Map[String, String] = Map(
    "abbreviation" -> "Noun",
    "acronym" -> "Noun",
    "adjective" -> "Adjective",
    "adjective 1" -> "Adjective",
    "adverb" -> "Adverb",
    "auxiliary verb" -> "Verb",
    "compound determinative" -> "Determiner",
    "conjunction" -> "Conjunction",
    "contraction" -> "Unknown",
    "demonstrative determiner" -> "Determiner",
    "determinative" -> "Determiner",
    "determiner" -> "Determiner",
    "expression" -> "Other",
    "initialism" -> "Noun", // basically acronym
    "interjection" -> "Particle",
    "noun" -> "Noun",
    "noun 1" -> "Noun",
    "noun 2" -> "Noun",
    "noun 3" -> "Noun",
    "prefix" -> "Affix",
    "preposition" -> "Adposition",
    "pronoun" -> "Pronoun",
    "proper noun" -> "Noun",
    "suffix" -> "Affix",
    "symbol" -> "Other",
    "verb" -> "Verb",
    "verb 1" -> "Verb",
    "verb 2" -> "Verb"
  )

  val partsOfSpeech: Array[String] = partOfSpeechMapping.keySet.toArray

  val subsectionMap: Map[String, String] = Map(
    "abbreviation" -> "otherSpellings",
    "alternate spellings" -> "otherSpellings",
    "alternative forms" -> "otherSpellings",
    "alternative spellings" -> "otherSpellings",
    "antonym" -> "antonyms",
    "antonyms" -> "antonyms",
    "homonyms" -> "homonyms",
    "homophone" -> "homophones",
    "homophones" -> "homophones",
    "note" -> "notes",
    "notes" -> "notes",
    "notes of usage" -> "notes",
    "other spelling" -> "otherSpellings",
    "other spellings" -> "otherSpellings",
    "pronounciation" -> "pronunciation",
    "pronunciation" -> "pronunciation",
    "pronunciation 2" -> "pronunciation",
    "related" -> "related",
    "related old words" -> "related",
    "related phrases" -> "related",
    "related terms" -> "related",
    "related word" -> "related",
    "related word and phrases" -> "related",
    "related words" -> "related",
    "related words and expressions" -> "related",
    "related words and phrases" -> "related",
    "related words and terms" -> "related",
    "related words or phrases" -> "related",
    "related words/phrases" -> "related",
    "see also" -> "related",
    "synonym" -> "synonyms",
    "synonyms" -> "synonyms",
    "usage" -> "usage",
    "usage note" -> "usage",
    "usage notes" -> "usage",
    "verb usage" -> "usage"
  )

  val partOfSpeechCols: Column =
    array(partsOfSpeech.head, ArraySeq.unsafeWrapArray(partsOfSpeech.tail): _*)

  def mapWiktionaryPartOfSpeechToDomainPartOfSpeech(
      partOfSpeech: String
  ): String = partOfSpeechMapping.getOrElse(partOfSpeech, "Unknown")

  val leftBracket = "\\{"
  val pipe = "\\|"
  val rightBracket = "\\}"
  val slash = "\\/"
  val anythingButSlash = "([^\\/]+)"
  val optionalSpace = " *"
  val newline = "\\n"

  // It looks like this: {{IPA|/whatWeWant/}}
  val ipaRegex: String =
    leftBracket + leftBracket + "IPA" + pipe + slash + anythingButSlash + slash + rightBracket + rightBracket

  val subdefinitionMarker = "#"
  val examplesMarker = "#:"
  val subdefinitionsRegex: String =
    subdefinitionMarker + optionalSpace + "([^\\n:]*)" + newline
  val examplesRegex: String = examplesMarker + optionalSpace + "([^\\n]*)"

  override def clean(
      data: DataFrame
  )(implicit spark: SparkSession): Dataset[SimpleWiktionaryDefinitionEntry] = {
    import spark.implicits._
    val splitDefinitions = splitWordsByPartOfSpeech(data)
      .withColumn("ipa", regexp_extract(col("text"), ipaRegex, 1))
      .withColumn(
        "subdefinitions",
        regexp_extract_all("definition", subdefinitionsRegex, 1)
      )
      .withColumn(
        "examples",
        regexp_extract_all("definition", examplesRegex, 1)
      )

    addOptionalSections(splitDefinitions)
      .drop("text")
      .as[SimpleWiktionaryDefinitionEntry]
  }

  val mapPartOfSpeech: UserDefinedFunction = udf((index: Integer) =>
    mapWiktionaryPartOfSpeechToDomainPartOfSpeech(partsOfSpeech(index))
  )

  def splitWordsByPartOfSpeech(data: DataFrame): DataFrame =
    extractSections(data, partsOfSpeech)
      .select(col("token"), col("text"), posexplode(partOfSpeechCols))
      .filter("col not like ''")
      .drop(partOfSpeechCols)
      .withColumnRenamed("col", "definition")
      .withColumn("tag", mapPartOfSpeech(col("pos")))
      .drop("pos")

  val subsectionsInverted: Map[String, Set[String]] = subsectionMap
    .groupBy { case (_, normalized) => normalized }
    .view
    .mapValues(_.keySet)
    .toMap

  val subsectionsToDrop: Map[String, Set[String]] =
    subsectionsInverted.map { case (subsectionName, subsectionSet) =>
      // We don't want to lose the subsection we combined things to
      subsectionName -> subsectionSet.filter(!_.equals(subsectionName))
    }

  val subsectionsToCombine: Map[String, Column] =
    subsectionsInverted.view
      .mapValues(subsections =>
        array(
          subsections.head,
          ArraySeq.unsafeWrapArray(subsections.tail.toArray): _*
        )
      )
      .toMap

  def addOptionalSections(data: DataFrame): DataFrame = {
    val extracted =
      extractSubsections(data, subsectionMap.keySet.toArray)
    subsectionsToCombine.foldLeft(extracted)((acc, subsection) => {
      val (subsectionName, subsectionColumns) = subsection
      val columnsToDrop: Array[String] =
        subsectionsToDrop.getOrElse(subsectionName, Set()).toArray
      acc
        .withColumn(subsectionName, array_remove(subsectionColumns, ""))
        .drop(ArraySeq.unsafeWrapArray(columnsToDrop): _*)
    })
  }
}
