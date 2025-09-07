package agdesigns.elevatefitness.ui.screens.view_exercises

import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.ui.common.ExerciseSearchResult
import agdesigns.elevatefitness.ui.common.FieldHighlight
import agdesigns.elevatefitness.ui.common.SearchField
import java.text.Normalizer
import kotlin.text.get

private fun normalizeForSearch(input: String): String =
    Normalizer.normalize(input, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")    // strip diacritics
        .lowercase()
        .replace("\\s+".toRegex(), " ")
        .trim()

private fun tokenize(query: String): List<String> =
    normalizeForSearch(query)
        .split("[^\\p{L}\\p{Nd}]+".toRegex())
        .filter { it.isNotBlank() }

/** Very small Levenshtein for fuzzy <=1 edits on short tokens */
private fun editDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val dp = IntArray(b.length + 1) { it }
    for (i in a.indices) {
        var prev = i
        var cur = i + 1
        for (j in b.indices) {
            val cost = if (a[i] == b[j]) 0 else 1
            val next = minOf(
                dp[j + 1] + 1,   // deletion
                cur + 1,         // insertion
                dp[j] + cost     // substitution
            )
            dp[j] = prev
            prev = next
            cur = next
        }
        dp[b.length] = cur
    }
    return dp[b.length]
}

/** Build a normalized string and an index map from normalized char index -> original index */
private data class NormalizedIndex(val normalized: String, val indexMap: IntArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NormalizedIndex

        if (normalized != other.normalized) return false
        if (!indexMap.contentEquals(other.indexMap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = normalized.hashCode()
        result = 31 * result + indexMap.contentHashCode()
        return result
    }
}

private fun buildNormalizedIndex(original: String): NormalizedIndex {
    val nfd = Normalizer.normalize(original, Normalizer.Form.NFD)
    val sb = StringBuilder()
    val map = ArrayList<Int>()
    for ((i, ch) in nfd.withIndex()) {
        val isMark = Character.getType(ch) == Character.NON_SPACING_MARK.toInt()
        if (!isMark) {
            // lowercasing here keeps lengths aligned in most cases (ß->ss is rare; acceptable)
            sb.append(ch.lowercaseChar())
            map.add(i.coerceAtMost(original.lastIndex))
        }
    }
    // collapse spaces in normalized while mapping back to original indices
    val collapsed = StringBuilder()
    val collapsedMap = ArrayList<Int>()
    var lastWasSpace = false
    for (k in sb.indices) {
        val c = sb[k]
        val isSpace = c.isWhitespace()
        if (isSpace) {
            if (!lastWasSpace) {
                collapsed.append(' ')
                collapsedMap.add(map[k].coerceAtMost(original.lastIndex))
            }
            lastWasSpace = true
        } else {
            collapsed.append(c)
            collapsedMap.add(map[k].coerceAtMost(original.lastIndex))
            lastWasSpace = false
        }
    }
    val trimmed = collapsed.toString().trim()
    // trim the map edges to match
    val startTrim = collapsed.indexOf(trimmed).takeIf { it >= 0 } ?: 0
    val endTrim = startTrim + trimmed.length
    val finalMap = collapsedMap.subList(startTrim, endTrim).toIntArray()
    return NormalizedIndex(trimmed, finalMap)
}

/** Find all occurrences of a normalized needle in a normalized haystack, then map back to original indices. */
private fun findNormalizedRanges(original: String, normIdx: NormalizedIndex, normalizedNeedle: String): List<IntRange> {
    if (normalizedNeedle.isBlank()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var start = 0
    while (true) {
        val pos = normIdx.normalized.indexOf(normalizedNeedle, startIndex = start)
        if (pos < 0) break
        val end = pos + normalizedNeedle.length - 1
        if (pos in normIdx.indexMap.indices && end in normIdx.indexMap.indices) {
            val origStart = normIdx.indexMap[pos]
            val origEnd = normIdx.indexMap[end]
            // be defensive
            if (origStart <= origEnd && origStart in original.indices && origEnd in original.indices) {
                ranges += IntRange(origStart, origEnd)
            }
        }
        start = pos + 1
    }
    return ranges
}


// Optional synonyms (extend as needed)
private val SYNONYMS = mapOf(
    "abs" to listOf("abdominals", "core"),
    "delts" to listOf("shoulders", "deltoids"),
    "hams" to listOf("hamstrings"),
    "pec" to listOf("chest", "pectorals"),
    "tris" to listOf("triceps"),
    "bis" to listOf("biceps")
)

private data class FieldTexts(
    val name: String,
    val variations: List<String>,
)

private fun fieldsOf(ex: Exercise): FieldTexts = FieldTexts(
    name = ex.name,
    variations = ex.variations,
)

fun matchExercise(ex: Exercise, rawQuery: String): ExerciseSearchResult? {
    val query = rawQuery.trim()
    if (query.isBlank()) return null

    val tokens = tokenize(query).flatMap { t -> listOf(t) + (SYNONYMS[t] ?: emptyList()).map(::normalizeForSearch) }
    if (tokens.isEmpty()) return null

    val f = fieldsOf(ex)

    // Build normalized indices for mapping highlights back
    val nameIdx = buildNormalizedIndex(f.name)
    val varIdxs = f.variations.map(::buildNormalizedIndex)

    var score = 0
    val highlights = mutableListOf<FieldHighlight>()
    val reasonLabels = linkedSetOf<String>() // unique in order

    // Phrase match on name (full normalized query)
    run {
        val normPhrase = normalizeForSearch(query)
        val ranges = findNormalizedRanges(f.name, nameIdx, normPhrase)
        if (ranges.isNotEmpty()) {
            val nameLength = if (f.name.isEmpty()) 1 else f.name.length // should not happen but better be safe
            // give score on percentage of name matched, e.g., search = "squa" should score "squat" higher than "hack squat"
            score += (ranges.sumOf { it.last - it.first + 1 } * 100) / nameLength
            highlights += FieldHighlight(SearchField.Name, null, ranges, "Name (phrase)")
            reasonLabels += "Name phrase"
        }
    }

    fun bump(field: SearchField, label: String, boost: Int, ranges: List<IntRange>, index: Int? = null) {
        if (ranges.isEmpty()) return
        score += boost
        highlights += FieldHighlight(field, index, ranges, label)
        reasonLabels += label
    }

    // Token-by-token scoring
    for (tok in tokens) {
        // Name
        val nameRanges = findNormalizedRanges(f.name, nameIdx, tok)
        if (nameRanges.isNotEmpty()) bump(SearchField.Name, "Name", 35, nameRanges)

        // Variations
        f.variations.forEachIndexed { i, v ->
            val ranges = findNormalizedRanges(v, varIdxs[i], tok)
            if (ranges.isNotEmpty()) bump(SearchField.Variation, "Variation: ${f.variations[i]}", 22, ranges, i)
        }

        // Light fuzzy bonus on name words (distance ≤ 1)
        if (tok.length >= 3) {
            val nameWords = f.name.split("\\s+".toRegex()).map(::normalizeForSearch)
            if (nameWords.any { editDistance(it, tok) == 1 }) {
                score += 6
                reasonLabels += "Name ≈"
            }
        }
    }

    if (score == 0) return null
    return ExerciseSearchResult(
        exercise = ex,
        score = score,
        highlights = highlights,
        reasons = reasonLabels.toList()
    )
}