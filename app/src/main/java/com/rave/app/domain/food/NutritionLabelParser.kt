package com.rave.app.domain.food

/**
 * Reads a nutrition panel from OCR text (English labels as printed in Pakistan, UK, EU, US and Gulf).
 * When a row has several numbers (per 100 g | per serving) the first one is used, which on most
 * labels is the per-100 g column. % daily values are skipped. Sodium in mg is converted to salt (× 2.5).
 */
object NutritionLabelParser {
    data class Result(val nutrients: Nutrients, val per100: Boolean) {
        /** How many of the main values were found; fewer than 3 usually means a bad photo. */
        val found: Int get() = with(nutrients) { listOf(kcal, protein, carbs, sugar, fat, satFat, fiber, salt).count { it != null } }
    }

    /** One line of OCR text with its box on the photo. */
    data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val centerY: Int get() = (top + bottom) / 2
        val height: Int get() = (bottom - top).coerceAtLeast(1)
    }

    /**
     * OCR returns a label table column by column ("Protein, Fat, Sugar" then "5 g, 3 g, 12 g").
     * Rebuild the printed rows by grouping lines whose vertical centres line up, left to right.
     */
    fun rows(lines: List<OcrLine>): String {
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            val ref = row?.let { r -> r.sumOf { it.centerY } / r.size }
            if (row != null && ref != null && kotlin.math.abs(line.centerY - ref) <= line.height * 0.6) row += line
            else rows += mutableListOf(line)
        }
        return rows.joinToString("\n") { r -> r.sortedBy { it.left }.joinToString(" ") { it.text } }
    }

    // A number that isn't a % daily value.
    private val number = Regex("(\\d+(?:[.,]\\d+)?)(?!\\s*%)(?![\\d.,])")

    /** Common OCR slips inside numbers: "1O g" → "10 g", "5,2g" stays. */
    private fun clean(line: String): String = line.lowercase()
        .replace(Regex("(?<=\\d)o|o(?=\\d)"), "0")
        .replace(Regex("(?<=\\d)[il](?=\\d)"), "1")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun parse(text: String): Result {
        val lines = text.lines().map(::clean).filter { it.isNotEmpty() }
        val per100 = lines.any { Regex("100\\s?(g|ml)").containsMatchIn(it) }

        fun firstNumber(s: String): Double? {
            val cleaned = s.replace(Regex("(per|/)?\\s?100\\s?(g|ml)"), " ")
            return number.find(cleaned)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        }

        fun valueOn(predicate: (String) -> Boolean): Double? {
            val i = lines.indexOfFirst(predicate)
            if (i < 0) return null
            // Some OCR splits "Protein" and "5.2g" onto separate lines.
            val sameLine = lines[i].substringAfter(':', lines[i]).let { l ->
                // Skip the label's own words, keep what comes after them.
                l.dropWhile { !it.isDigit() }
            }
            return firstNumber(sameLine) ?: lines.getOrNull(i + 1)?.takeIf { next -> next.firstOrNull()?.isDigit() == true }?.let(::firstNumber)
        }

        val kcal = run {
            val direct = lines.firstNotNullOfOrNull { l -> Regex("(\\d+(?:[.,]\\d+)?)\\s*kcal").find(l)?.groupValues?.get(1) }
            direct?.replace(',', '.')?.toDoubleOrNull()
                ?: lines.firstNotNullOfOrNull { l -> Regex("(\\d+(?:[.,]\\d+)?)\\s*kj").find(l)?.groupValues?.get(1) }
                    ?.replace(',', '.')?.toDoubleOrNull()?.div(4.184)
                ?: valueOn { it.startsWith("energy") || it.startsWith("calories") || it.startsWith("calorie") }
        }
        val satFat = valueOn { it.contains("saturat") }
        val fat = valueOn { (it.startsWith("fat") || it.startsWith("total fat") || it.startsWith("fats")) && !it.contains("saturat") && !it.contains("trans") }
        val sugar = valueOn { it.contains("total sugar") } ?: valueOn { it.contains("sugar") && !it.contains("added") } ?: valueOn { it.contains("sugar") }
        val carbs = valueOn { it.contains("carbohydrate") || it.startsWith("total carb") || it.startsWith("carbs") }
        val protein = valueOn { it.contains("protein") }
        val fiber = valueOn { it.contains("fibre") || it.contains("fiber") }
        val salt = valueOn { it.startsWith("salt") }
            ?: lines.indexOfFirst { it.contains("sodium") }.takeIf { it >= 0 }?.let { i ->
                val l = lines[i]
                val v = firstNumber(l.dropWhile { !it.isDigit() }) ?: return@let null
                if (l.contains("mg") || v > 20) v * 2.5 / 1000 else v * 2.5
            }
        return Result(Nutrients(kcal, protein, carbs, sugar, fat, satFat, fiber, salt), per100)
    }
}
