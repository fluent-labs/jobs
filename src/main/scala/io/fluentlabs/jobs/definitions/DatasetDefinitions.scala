package io.fluentlabs.jobs.definitions

/*
 * Spark jobs can be typed, here are type definitions for all the intermediate stages
 */

// Common
case class WiktionaryRawEntry(id: Long, token: String, text: String)
case class WiktionaryRawText(text: String)
