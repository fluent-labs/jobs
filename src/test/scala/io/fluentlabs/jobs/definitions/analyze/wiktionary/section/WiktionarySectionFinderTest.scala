package io.fluentlabs.jobs.definitions.analyze.wiktionary.section

import io.fluentlabs.jobs.TestWithSpark
import io.fluentlabs.jobs.definitions.source.WiktionaryRawEntry
import org.scalatest.funspec.AnyFunSpec

class WiktionarySectionFinderTest extends AnyFunSpec with TestWithSpark {
  object SectionFinder extends WiktionarySectionFinder("simplewiktionary")

  import spark.implicits._

  val text: String =
    """{{wikipedia}}
      |{{BE850}}
      |
      |=== Pronunciation ===
      |* {{AU}} {{IPA|/fiə/}}, {{SAMPA|/fi@/}}
      |* {{UK}} {{IPA|/fɪə/}}, {{SAMPA|/fI@/}}
      |* {{US}} {{IPA|/fɪr/}}, {{SAMPA|/fir/}}
      |* {{audio|en-us-fear.ogg|Audio (US)}}
      |
      |== Noun ==
      |{{noun}}
      |[[File:Expression of the Emotions Figure 20.png|thumb|A man showing '''fear''']]
      |# {{cu noun}} '''Fear''' is a bad [[feeling]] usually caused by a [[danger]] or a [[worry]] that something bad might happen.
      |#: ''He was struck with '''fear''' when he thought he saw a ghost.''
      |#: ''She has a '''fear''' of spiders.''
      |# {{uncountable}} '''Fear''' is the chance that something might happen.
      |#: ''There's no '''fear''' of her being on time. She's always late.''
      |# {{countable}}; {{singular}} '''Fear''' is a feeling of very high [[respect]], usually towards a [[god]].
      |#: ''Christians who worship in churches have a '''fear''' of [[God]].''
      |
      |=== Synonyms ===
      |* [[worry]]
      |* [[phobia]]
      |
      |== Verb ==
      |{{verb}}
      |# {{ti verb}} If you '''fear''' something, you are [[afraid]] of it.
      |#: ''I '''fear''' the worst will happen.''
      |#: ''She '''feared''' for his safety.''
      |# {{transitive}} You say you '''fear''' something when you're giving someone bad news.
      |#: ''I '''fear''' that she has left already.''
      |# {{transitive}} If you '''fear''' something, usually a god, you show great [[respect]] towards it.
      |#: ''Christians in churches '''fear''' [[God]].''
      |
      |=== See also ===
      |* [[frighten]]
      |* [[scare]]
      |
      |{{emotions}}""".stripMargin
  val fear: WiktionaryRawEntry = WiktionaryRawEntry(42, "fear", text)

  val secondText: String =
    """{{wikipedia}}
      |{{BE850}}
      |
      |=== Pronunciation ===
      |* {{AU}} {{IPA|/fiə/}}, {{SAMPA|/fi@/}}
      |* {{UK}} {{IPA|/fɪə/}}, {{SAMPA|/fI@/}}
      |* {{US}} {{IPA|/fɪr/}}, {{SAMPA|/fir/}}
      |* {{audio|en-us-fear.ogg|Audio (US)}}
      |
      |== Verb ==
      |{{verb}}
      |# {{ti verb}} If you '''fear''' something, you are [[afraid]] of it.
      |#: ''I '''fear''' the worst will happen.''
      |#: ''She '''feared''' for his safety.''
      |# {{transitive}} You say you '''fear''' something when you're giving someone bad news.
      |#: ''I '''fear''' that she has left already.''
      |# {{transitive}} If you '''fear''' something, usually a god, you show great [[respect]] towards it.
      |#: ''Christians in churches '''fear''' [[God]].''
      |
      |=== See also ===
      |* [[frighten]]
      |* [[scare]]
      |
      |{{emotions}}""".stripMargin

  val terror: WiktionaryRawEntry = WiktionaryRawEntry(43, "terror", secondText)

  describe("it can extract a section from an entry") {
    val data = createDataset(Seq(fear, terror))

    it("with heading level 2") {
      val headings =
        SectionFinder
          .getHeadings(data, 2)
          .collect()
          .map(section => (section.heading, section.count.toInt))
          .toMap

      assert(headings.contains("Noun"))
      assert(headings.contains("Verb"))
      assert(headings.get("Noun").contains(1))
      assert(headings.get("Verb").contains(2))
    }

    it("with heading level 3") {
      val headings =
        SectionFinder
          .getHeadings(data, 3)
          .collect()
          .map(section => (section.heading, section.count.toInt))
          .toMap

      assert(headings.contains("Synonyms"))
      assert(headings.contains("Pronunciation"))
      assert(headings.contains("See also"))
      assert(headings.get("Synonyms").contains(1))
      assert(headings.get("See also").contains(2))
    }
  }
}
