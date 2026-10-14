package com.rave.app.domain.food

enum class Verdict { GO, SOMETIMES, SKIP }

/** Why the verdict came out the way it did. [amount] is grams or kcal per 100 g/ml where relevant. */
enum class ReasonType(val positive: Boolean) {
    HIGH_SUGAR(false), HIGH_SALT(false), HIGH_SAT_FAT(false), HIGH_FAT(false), CALORIE_DENSE(false),
    ULTRA_PROCESSED(false), POOR_NUTRISCORE(false), MANY_ADDITIVES(false), CONTAINS_YOUR_ALLERGEN(false), NOT_HALAL(false),
    GOOD_PROTEIN(true), GOOD_FIBER(true), LOW_SUGAR(true), GOOD_NUTRISCORE(true), MINIMALLY_PROCESSED(true),
}

data class FoodReason(val type: ReasonType, val amount: Double? = null, val text: String? = null)

enum class WarningType { ALLERGENS, HALAL_CHECK, HALAL_CERTIFIED, NOT_VEGETARIAN }

data class FoodWarning(val type: WarningType, val items: List<String> = emptyList())

/** Healthier alternatives, picked from the product's category. */
enum class Swap { ROASTED_CHANA, LEMON_WATER, NUTS_OR_FRUIT, DATES, CHANA_CHAAT, FRUIT_CHAAT, PLAIN_YOGURT, AIR_POPCORN }

data class FoodAssessment(
    val verdict: Verdict,
    val score: Int,
    val reasons: List<FoodReason>,
    val warnings: List<FoodWarning>,
    val swaps: List<Swap>,
    /** Share of a day's limit in one portion (0..∞), or null when unknown. */
    val sugarOfDailyLimit: Double?,
    val saltOfDailyLimit: Double?,
)

/**
 * "Should I eat this?" Thresholds follow the UK FSA front-of-pack traffic lights (per 100 g;
 * drinks have half the sugar limits), combined with processing level (NOVA), Nutri-Score,
 * the user's goal, allergies and halal preference. Not medical advice.
 */
object FoodVerdictEngine {
    const val DAILY_SUGAR_G = 50.0
    const val DAILY_SALT_G = 6.0

    private val haram = listOf("pork", "lard", "bacon", "pig ", "pig fat", "ham ", "alcohol", "ethanol", "wine", "beer", "rum ", "brandy", "whisky")
    private val check = listOf("gelatin", "gelatine", "e441", "e120", "carmine", "cochineal", "e542", "rennet")
    private val meat = listOf("chicken", "beef", "mutton", "meat", "fish", "gelatin", "gelatine", "prawn", "shrimp", "tuna")

    fun assess(food: ScannedFood, prefs: FoodPreferences): FoodAssessment {
        val n = food.per100g ?: food.perServing?.let { s -> food.servingGrams?.takeIf { it > 0 }?.let { s.scaled(100.0 / it) } } ?: Nutrients()
        val drink = food.isDrink
        val reasons = mutableListOf<FoodReason>()
        var score = 0

        n.sugar?.let { s ->
            val high = if (drink) 11.25 else 22.5
            val med = if (drink) 2.5 else 5.0
            when {
                s > high -> { score += 2; reasons += FoodReason(ReasonType.HIGH_SUGAR, s) }
                s > med -> { score += 1; reasons += FoodReason(ReasonType.HIGH_SUGAR, s) }
                s <= med -> reasons += FoodReason(ReasonType.LOW_SUGAR, s)
            }
        }
        n.satFat?.let { f -> if (f > (if (drink) 2.5 else 5.0)) { score += 2; reasons += FoodReason(ReasonType.HIGH_SAT_FAT, f) } }
        n.fat?.let { f -> if (f > (if (drink) 8.75 else 17.5)) { score += 1; reasons += FoodReason(ReasonType.HIGH_FAT, f) } }
        n.salt?.let { s ->
            when {
                s > (if (drink) 0.75 else 1.5) -> { score += 2; reasons += FoodReason(ReasonType.HIGH_SALT, s) }
                s > (if (drink) 0.3 else 0.3) && !drink -> { score += 1; reasons += FoodReason(ReasonType.HIGH_SALT, s) }
                else -> Unit
            }
        }
        n.kcal?.let { k ->
            if (k > 450 && prefs.goal == FoodGoal.LOSE) { score += 2; reasons += FoodReason(ReasonType.CALORIE_DENSE, k) }
            else if (k > 500) { score += 1; reasons += FoodReason(ReasonType.CALORIE_DENSE, k) }
        }
        n.protein?.let { p ->
            if (p >= 10) {
                score -= if (prefs.goal == FoodGoal.GAIN && p >= 15) 2 else 1
                reasons += FoodReason(ReasonType.GOOD_PROTEIN, p)
            }
        }
        n.fiber?.let { f -> if (f >= 6) { score -= 1; reasons += FoodReason(ReasonType.GOOD_FIBER, f) } }
        when (food.novaGroup) {
            4 -> { score += 2; reasons += FoodReason(ReasonType.ULTRA_PROCESSED) }
            1 -> { score -= 1; reasons += FoodReason(ReasonType.MINIMALLY_PROCESSED) }
        }
        when (food.nutriScore?.uppercaseChar()) {
            'A', 'B' -> { score -= 1; reasons += FoodReason(ReasonType.GOOD_NUTRISCORE, text = food.nutriScore.uppercase()) }
            'D' -> { score += 1; reasons += FoodReason(ReasonType.POOR_NUTRISCORE, text = "D") }
            'E' -> { score += 2; reasons += FoodReason(ReasonType.POOR_NUTRISCORE, text = "E") }
        }
        if (food.additiveTags.size >= 5) { score += 1; reasons += FoodReason(ReasonType.MANY_ADDITIVES, food.additiveTags.size.toDouble()) }

        // Warnings and hard stops.
        val warnings = mutableListOf<FoodWarning>()
        val text = " " + food.ingredients.lowercase().replace(Regex("[^a-z0-9 ]"), " ") + " "
        var forceSkip = false
        val allergens = Allergen.fromTags(food.allergenTags)
        if (allergens.isNotEmpty()) warnings += FoodWarning(WarningType.ALLERGENS, allergens.map { it.name })
        val mine = allergens intersect prefs.allergies
        if (mine.isNotEmpty()) {
            forceSkip = true
            reasons.add(0, FoodReason(ReasonType.CONTAINS_YOUR_ALLERGEN, text = mine.joinToString { it.name }))
        }
        val certified = food.labelTags.any { it.contains("halal", ignoreCase = true) }
        if (certified) {
            warnings += FoodWarning(WarningType.HALAL_CERTIFIED)
        } else {
            val bad = haram.filter { text.contains(" ${it.trim()} ") || text.contains(" ${it.trim()}s ") }
            val doubtful = check.filter { text.contains(it) }
            if (bad.isNotEmpty() && prefs.halalOnly) {
                forceSkip = true
                reasons.add(0, FoodReason(ReasonType.NOT_HALAL, text = bad.joinToString { it.trim() }))
            }
            if (doubtful.isNotEmpty()) warnings += FoodWarning(WarningType.HALAL_CHECK, doubtful)
        }
        if (prefs.vegetarian) {
            val m = meat.filter { text.contains(" $it") }
            if (m.isNotEmpty()) warnings += FoodWarning(WarningType.NOT_VEGETARIAN, m)
        }

        val verdict = when {
            forceSkip -> Verdict.SKIP
            score >= 4 -> Verdict.SKIP
            score >= 2 -> Verdict.SOMETIMES
            else -> Verdict.GO
        }
        val portion = food.portion
        return FoodAssessment(
            verdict = verdict,
            score = score,
            reasons = reasons.sortedBy { it.type.positive },
            warnings = warnings,
            swaps = if (verdict == Verdict.GO) emptyList() else swapsFor(food),
            sugarOfDailyLimit = portion?.sugar?.div(DAILY_SUGAR_G),
            saltOfDailyLimit = portion?.salt?.div(DAILY_SALT_G),
        )
    }

    fun swapsFor(food: ScannedFood): List<Swap> {
        val hay = (food.categoryTags.joinToString(" ") + " " + food.name).lowercase()
        fun has(vararg k: String) = k.any { hay.contains(it) }
        return when {
            has("chip", "crisp", "nimko", "namkeen", "snack") -> listOf(Swap.ROASTED_CHANA, Swap.AIR_POPCORN)
            food.isDrink || has("soda", "cola", "soft-drink", "juice", "beverage", "drink") -> listOf(Swap.LEMON_WATER, Swap.PLAIN_YOGURT)
            has("biscuit", "cookie", "cake", "wafer", "rusk") -> listOf(Swap.NUTS_OR_FRUIT, Swap.DATES)
            has("chocolate", "candy", "sweet", "confection", "mithai") -> listOf(Swap.DATES, Swap.NUTS_OR_FRUIT)
            has("noodle", "pasta", "instant") -> listOf(Swap.CHANA_CHAAT)
            has("ice-cream", "ice cream", "dessert") -> listOf(Swap.FRUIT_CHAAT, Swap.PLAIN_YOGURT)
            else -> listOf(Swap.NUTS_OR_FRUIT)
        }
    }
}
