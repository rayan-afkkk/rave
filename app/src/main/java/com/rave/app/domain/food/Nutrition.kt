package com.rave.app.domain.food

/** Nutrient amounts for some quantity of food (per 100 g/ml or per serving). Unknown values are null. */
data class Nutrients(
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val sugar: Double? = null,
    val fat: Double? = null,
    val satFat: Double? = null,
    val fiber: Double? = null,
    val salt: Double? = null,
) {
    fun scaled(factor: Double) = Nutrients(
        kcal?.times(factor), protein?.times(factor), carbs?.times(factor), sugar?.times(factor),
        fat?.times(factor), satFat?.times(factor), fiber?.times(factor), salt?.times(factor),
    )

    val isEmpty: Boolean get() = listOf(kcal, protein, carbs, sugar, fat, satFat, fiber, salt).all { it == null }
}

enum class FoodGoal { LOSE, MAINTAIN, GAIN }

enum class Allergen(val offTag: String) {
    MILK("en:milk"), GLUTEN("en:gluten"), PEANUTS("en:peanuts"), NUTS("en:nuts"), EGGS("en:eggs"),
    SOY("en:soybeans"), FISH("en:fish"), SHELLFISH("en:crustaceans"), SESAME("en:sesame-seeds");

    companion object {
        fun fromTags(tags: List<String>): Set<Allergen> = entries.filter { a -> tags.any { it.equals(a.offTag, ignoreCase = true) } }.toSet()
    }
}

data class FoodPreferences(
    val goal: FoodGoal = FoodGoal.MAINTAIN,
    val halalOnly: Boolean = true,
    val vegetarian: Boolean = false,
    val allergies: Set<Allergen> = emptySet(),
)

enum class FoodSource { BARCODE, LABEL, PHOTO, MANUAL, PLAN }

/** A food as returned by a scan (barcode database, label OCR or AI photo). */
data class ScannedFood(
    val name: String,
    val brand: String = "",
    val per100g: Nutrients? = null,
    val perServing: Nutrients? = null,
    val servingGrams: Double? = null,
    val servingLabel: String = "",
    val isDrink: Boolean = false,
    /** NOVA 1 (unprocessed) … 4 (ultra-processed). */
    val novaGroup: Int? = null,
    /** Nutri-Score A…E. */
    val nutriScore: Char? = null,
    val ingredients: String = "",
    val allergenTags: List<String> = emptyList(),
    val additiveTags: List<String> = emptyList(),
    val labelTags: List<String> = emptyList(),
    val categoryTags: List<String> = emptyList(),
    val barcode: String? = null,
    val imageUrl: String? = null,
    val source: FoodSource = FoodSource.BARCODE,
) {
    /** Best guess at what one portion contains (for logging and daily-limit percentages). */
    val portion: Nutrients?
        get() = perServing ?: per100g?.let { p -> servingGrams?.let { p.scaled(it / 100.0) } ?: p }
}
