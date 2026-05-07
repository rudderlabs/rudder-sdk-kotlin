package com.rudderstack.scenarioengine.domain.step

import kotlinx.serialization.json.JsonElement

/**
 * The comparison operator a [FieldMatch] applies between the value at its path and its expected value.
 *
 *  - [EQ] / [NE]              — strict JsonElement equality / inequality.
 *  - [EXISTS] / [NOT_EXISTS]  — value is ignored.
 *  - [GT] / [GTE] / [LT] / [LTE] — numeric comparisons; both sides must parse as numbers.
 *  - [CONTAINS]               — substring match on JSON-string values, or element membership for arrays.
 *  - [REGEX]                  — Java regex match against a JSON string value.
 */
enum class MatchOp { EQ, NE, EXISTS, NOT_EXISTS, GT, GTE, LT, LTE, CONTAINS, REGEX }

/**
 * One predicate over a JSON document, addressed by a dotted path
 * (e.g. `"properties.amount"`, `"context.app.name"`).
 *
 * A `List<FieldMatch>` is combined with implicit AND. If/when OR is needed, promote this
 * to a sealed `MatchExpr { Field, And, Or, Not }` — not before. Keeping the matcher as
 * data (rather than a closure) is what lets [com.rudderstack.scenarioengine.domain.step.Step.WaitForEvent]
 * cross IPC and round-trip through `save_as_dsl`.
 *
 * @param path Dotted path into the event's JSON (segments separated by `.`).
 * @param value Required for value-comparing ops; null only for [MatchOp.EXISTS] / [MatchOp.NOT_EXISTS].
 */
data class FieldMatch(
    val path: String,
    val op: MatchOp,
    val value: JsonElement? = null,
)
