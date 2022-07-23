package io.fluentlabs.jobs.definitions

object DefinitionSource extends Enumeration {
  type DefinitionSource = Value

  val CEDICT: Value = Value("CEDICT")
  val WIKTIONARY_CHINESE: Value = Value("WIKTIONARY_CHINESE")
  val WIKTIONARY_DANISH: Value = Value("WIKTIONARY_DANISH")
  val WIKTIONARY_ENGLISH: Value = Value("WIKTIONARY_ENGLISH")
  val WIKTIONARY_SIMPLE_ENGLISH: Value = Value("WIKTIONARY_SIMPLE_ENGLISH")
  val WIKTIONARY_SPANISH: Value = Value("WIKTIONARY_SPANISH")
}
