package com.rave.app.domain.food

/**
 * Reads a nutrition panel from OCR text (English labels as printed in Pakistan, UK, EU and Gulf).
 * When a line has several numbers (per 100 g | per serving) the first one is used, which on most
 * labels is the per-100 g column. Sodium in mg is converted to salt (× 2.5).
 */
object NutritionLabelParser {
    data class Result(val nutrients: Nutrients, val per100: Boolean)

    private val number = Regex("(\\d+(?:[.,]\\d+)?)")

    fun parse(text: String): Result {
        val lines = text.lines().map { it.lowercase().trim() }.filter { it.isNotEmpty() }
        val per100 = lines.any { it.contains("100 g") || it.contains("100g") || it.contains("100 ml") || it.contains("100ml") }

        fun valueOn(predicate: (String) -> Boolean): Double? {
            val i = lines.indexOfFirst(predicate)
            if (i < 0) return null
            // Some OCR splits "Protein" and "5.2g" onto separate lines.
            val candidates = listOf(lines[i].substringAfter(':', lines[i])) + lines.drop(i + 1).take(1)
            for (c in candidates) {
                val cleaned = c.replace(Regex("100\\s?(g|ml)"), " ")
                number.find(cleaned)?.let { return it.value.replace(',', '.').toDoubleOrNull() }
            }
            return null
        }

        val kcal = run {
            val direct = lines.firstNotNullOfOrNull { l -> Regex("(\\d+(?:[.,]\\d+)?)\\s*kcal").find(l)?.groupValues?.get(1) }
            direct?.replace(',', '.')?.toDoubleOrNull()
                ?: lines.firstNotNullOfOrNull { l -> Regex("(\\d+(?:[.,]\\d+)?)\\s*kj").find(l)?.groupValues?.get(1) }
                    ?.replace(',', '.')?.toDoubleOrNull()?.div(4.184)
                ?: valueOn { it.startsWith("energy") || it.startsWith("calories") }
        }
        val satFat = valueOn { it.contains("saturat") }
        val fat = valueOn { (it.startsWith("fat") || it.startsWith("total fat")) && !it.contains("saturat") && !it.contains("trans") }
        val sugar = valueOn { it.contains("sugar") }
        val carbs = valueOn { it.contains("carbohydrate") }
        val protein = valueOn { it.contains("protein") }
        val fiber = valueOn { it.contains("fibre") || it.contains("fiber") }
        val salt = valueOn { it.startsWith("salt") }
            ?: lines.indexOfFirst { it.contains("sodium") }.takeIf { it >= 0 }?.let { i ->
                val l = lines[i]
                val v = number.find(l)?.value?.replace(',', '.')?.toDoubleOrNull() ?: return@let null
                if (l.contains("mg")) v * 2.5 / 1000 else v * 2.5
            }
        return Result(Nutrients(kcal, protein, carbs, sugar, fat, satFat, fiber, salt), per100)
    }
}
