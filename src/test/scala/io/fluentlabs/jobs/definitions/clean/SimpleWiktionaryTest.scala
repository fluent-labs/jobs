package io.fluentlabs.jobs.definitions.clean

import io.fluentlabs.jobs.TestWithSpark
import io.fluentlabs.jobs.definitions.source.WiktionaryRawEntry
import org.scalatest.funspec.AnyFunSpec

class SimpleWiktionaryTest extends AnyFunSpec with TestWithSpark {

  import spark.implicits._

  def runTest(token: String, text: String): SimpleWiktionaryDefinitionEntry = {
    val data = Seq(WiktionaryRawEntry(42, token, text)).toDS()
    val job = SimpleWiktionaryCleaningJob.clean(data)(spark)
    job.first()
  }

  it("can parse a simple definition") {
    val text =
      """=== Pronunciation ===
                 |* {{IPA|/ɪz/}}
                 |* {{SAMPA|/Iz/}}
                 |* {{audio|en-us-is.ogg|Audio (US)}}
                 |
                 |== Verb ==
                 |{{verb3|be|am|is|are|was|were|been|being}}
                 |# {{Auxiliary}} {{linking verb}} A form of the [[verb]] ''[[be]]'' when talking about someone or something else.
                 |#: ''He '''is''' late for class.''
                 |#: '''''Is''' it hot in here?''
                 |
                 |=== Related words ===
                 |* [['s]] - contraction
                 |
                 |[[Category:Auxiliary verbs]]""".stripMargin

    val entryParsed = runTest("Is", text)

    val definition =
      """
                        |{{verb3|be|am|is|are|was|were|been|being}}
                        |# {{Auxiliary}} {{linking verb}} A form of the [[verb]] ''[[be]]'' when talking about someone or something else.
                        |#: ''He '''is''' late for class.''
                        |#: '''''Is''' it hot in here?''
                        |
                        |=== Related words ===
                        |* [['s]] - contraction
                        |
                        |[[Category:Auxiliary verbs]]""".stripMargin

    assert(entryParsed.token == "Is")
    assert(entryParsed.definition == definition)
    assert(entryParsed.tag.contains("Verb"))
    assert(entryParsed.ipa == "ɪz")
    assert(
      entryParsed.subdefinitions === List(
        "{{Auxiliary}} {{linking verb}} A form of the [[verb]] ''[[be]]'' when talking about someone or something else."
      )
    )
    assert(
      entryParsed.examples.contains(
        List(
          "''He '''is''' late for class.''",
          "'''''Is''' it hot in here?''"
        )
      )
    )
    assert(
      entryParsed.pronunciation === List(
        "=\n* {{IPA|/ɪz/}}\n* {{SAMPA|/Iz/}}\n* {{audio|en-us-is.ogg|Audio (US)}}\n\n"
      )
    )
    assert(
      entryParsed.related === Array(
        "=\n* [['s]] - contraction\n\n[[Category:Auxiliary verbs]]"
      )
    )

    assert(entryParsed.synonyms.isEmpty)
    assert(entryParsed.antonyms.isEmpty)
    assert(entryParsed.usage.isEmpty)
    assert(entryParsed.notes.isEmpty)
    assert(entryParsed.homophones.isEmpty)
    assert(entryParsed.homonyms.isEmpty)
    assert(entryParsed.otherSpellings.isEmpty)
  }
}
