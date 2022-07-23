package io.fluentlabs.jobs.definitions

import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import org.scalatest.funspec.AnyFunSpec

class WiktionaryTest extends AnyFunSpec {
  describe("can correctly generate regexes") {
    it("can repeat a pattern") {
      assert(WiktionaryParser.repeat("=", 6) == "======")
    }

    describe("for a heading of any size") {
      val levelThreeHeading = WiktionaryParser.headingRegex(3)
      it("which match valid headings") {
        assert("=== Title ===".matches(levelThreeHeading))
      }

      // There's some subtle bugs around matching too many and too few
      // This is to prevent regression

      it("does not match larger headings") {
        assert(!"== Title ==".matches(levelThreeHeading))
      }

      it("does not match smaller headings") {
        assert(!"==== Title ====".matches(levelThreeHeading))
      }
    }

    // This is there to cover refactors, feel free to wipe the assertion if the regex materially changes.
    it("for a section") {
      assert(
        "(?s)(?i)== *MyTestSection *==(.*?)(?>(?>== *[A-Za-z0-9]+ *==[ |\n]+)|\\Z)+" == WiktionaryParser
          .sectionRegex("MyTestSection")
      )
    }

    // This is there to cover refactors, feel free to wipe the assertion if the regex materially changes.
    it("for a subsection") {
      assert(
        "(?s)(?i)=== *MyTestSubsection *===(.*?)(?>(?>== *[A-Za-z0-9]+ *==[ |\n]+)|\\Z)+" == WiktionaryParser
          .subSectionRegex("MyTestSubsection")
      )
    }
  }

  describe("can get headings") {
    describe("for a single line") {
      it("on the happy path") {
        val test = "== Single heading =="
        assert(WiktionaryParser.getHeadingFromLine(test, 2) == "single heading")
      }
      it("without error if there is no heading") {
        assert(WiktionaryParser.getHeadingFromLine("", 2) == "")
      }
      it("and returns error if there is bad input") {
        assert(
          WiktionaryParser.getHeadingFromLine(
            null,
            2
          ) == "ERROR" // scalastyle:ignore
        )
      }
    }

    describe("for a document") {
      it("on the happy path") {
        val text =
          """== This is a heading ==
            |another document item
            |=== subheading that should be ignored ===
            |another garbage thing
            |== This is another heading ==
            |= heading level that doesn't exist =
            |== uneven heading ===
            |=== uneven in a different way ==
            |""".stripMargin
        assert(
          WiktionaryParser.getHeadingsFromDocument(text, 2) sameElements Array(
            "this is a heading",
            "this is another heading"
          )
        )
      }
      it("and correctly handles bad input") {
        assert(
          WiktionaryParser
            .getHeadingsFromDocument(null, 2) // scalastyle:ignore
            .isEmpty
        )
      }
    }
  }
}
