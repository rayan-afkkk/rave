package com.rave.app.domain.food

enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK }

enum class FoodRole { MAIN, SIDE_CARB, SIDE, DRINK }

enum class FoodTag { QUICK, BUDGET, LIGHT, HIGH_PROTEIN, FRIED, INDULGENT, COMPLETE }

/** One serving of a common Pakistani / South-Asian food. Values are typical home-cooked estimates. */
data class DesiFood(
    val id: String,
    val name: String,
    val serving: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val slots: Set<MealSlot>,
    val role: FoodRole = FoodRole.MAIN,
    val vegetarian: Boolean = true,
    val tags: Set<FoodTag> = emptySet(),
)

/**
 * Built-in food list for the meal planner and quick logging. Calories are per the serving shown,
 * based on standard nutrition tables for home-style recipes (oil varies by kitchen, ±15%).
 */
object DesiFoods {
    private val B = MealSlot.BREAKFAST
    private val L = MealSlot.LUNCH
    private val D = MealSlot.DINNER
    private val S = MealSlot.SNACK
    private val LD = setOf(L, D)

    val all: List<DesiFood> = listOf(
        // ---- Breakfast / suhoor ----
        DesiFood("paratha", "Plain paratha", "1 paratha", 260, 5.0, 32.0, 12.0, setOf(B), FoodRole.SIDE_CARB),
        DesiFood("aloo_paratha", "Aloo paratha", "1 paratha", 300, 6.0, 42.0, 12.0, setOf(B), tags = setOf(FoodTag.BUDGET)),
        DesiFood("anda_paratha", "Anda paratha", "1 paratha", 400, 13.0, 35.0, 23.0, setOf(B)),
        DesiFood("keema_paratha", "Keema paratha", "1 paratha", 420, 18.0, 38.0, 22.0, setOf(B), vegetarian = false),
        DesiFood("boiled_eggs", "Boiled eggs", "2 eggs", 155, 13.0, 1.0, 11.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        DesiFood("omelette", "Desi omelette", "2 eggs, onion, tomato", 190, 13.0, 3.0, 14.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        DesiFood("halwa_puri", "Halwa puri", "2 puri, halwa, chana", 750, 15.0, 90.0, 36.0, setOf(B), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT, FoodTag.COMPLETE)),
        DesiFood("channay", "Channay", "1 cup", 270, 12.0, 40.0, 7.0, setOf(B, L), tags = setOf(FoodTag.BUDGET)),
        DesiFood("oats_milk", "Oats with milk", "1 bowl", 250, 10.0, 40.0, 6.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        DesiFood("daliya", "Daliya", "1 bowl", 220, 7.0, 40.0, 4.0, setOf(B), tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        DesiFood("bread_butter", "Bread and butter", "2 slices", 220, 6.0, 28.0, 9.0, setOf(B), FoodRole.SIDE_CARB, tags = setOf(FoodTag.QUICK)),
        DesiFood("egg_sandwich", "Egg sandwich", "1 sandwich", 300, 15.0, 30.0, 13.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        DesiFood("cornflakes", "Cornflakes with milk", "1 bowl", 220, 7.0, 40.0, 4.0, setOf(B), tags = setOf(FoodTag.QUICK)),
        DesiFood("chai", "Doodh pati chai", "1 cup", 110, 3.0, 14.0, 4.0, setOf(B, S), FoodRole.DRINK),
        DesiFood("green_tea", "Green tea", "1 cup", 2, 0.0, 0.5, 0.0, setOf(B, S, D), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        DesiFood("lassi_sweet", "Sweet lassi", "1 glass", 220, 8.0, 35.0, 5.0, setOf(B), FoodRole.DRINK),
        DesiFood("lassi_salt", "Namkeen lassi", "1 glass", 120, 7.0, 10.0, 6.0, setOf(B, L), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        DesiFood("yogurt", "Dahi", "1 cup", 150, 9.0, 11.0, 8.0, setOf(B, S), FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        // ---- Lunch / dinner mains ----
        DesiFood("daal_masoor", "Masoor daal", "1 bowl", 230, 13.0, 30.0, 7.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.LIGHT)),
        DesiFood("daal_mash", "Daal mash", "1 bowl", 260, 14.0, 30.0, 9.0, LD, tags = setOf(FoodTag.BUDGET)),
        DesiFood("chana_daal", "Chana daal", "1 bowl", 260, 13.0, 36.0, 7.0, LD, tags = setOf(FoodTag.BUDGET)),
        DesiFood("daal_chawal", "Daal chawal", "1 plate", 480, 16.0, 80.0, 10.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.COMPLETE)),
        DesiFood("chicken_karahi", "Chicken karahi", "1 bowl (200 g)", 380, 30.0, 8.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("chicken_handi", "Chicken handi", "1 bowl", 420, 28.0, 10.0, 30.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("chicken_tikka", "Chicken tikka", "2 pieces", 300, 35.0, 4.0, 16.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("chicken_qorma", "Chicken qorma", "1 bowl", 450, 28.0, 12.0, 32.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT)),
        DesiFood("mutton_karahi", "Mutton karahi", "1 bowl", 480, 30.0, 8.0, 36.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT, FoodTag.HIGH_PROTEIN)),
        DesiFood("nihari", "Beef nihari", "1 bowl", 520, 32.0, 14.0, 38.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT)),
        DesiFood("haleem", "Haleem", "1 bowl", 380, 22.0, 40.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("aloo_gosht", "Aloo gosht", "1 bowl", 420, 24.0, 20.0, 26.0, LD, vegetarian = false),
        DesiFood("keema_matar", "Keema matar", "1 bowl", 380, 24.0, 14.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("palak_paneer", "Palak paneer", "1 bowl", 320, 15.0, 10.0, 24.0, LD),
        DesiFood("saag", "Sarson ka saag", "1 bowl", 220, 6.0, 14.0, 16.0, LD),
        DesiFood("bhindi", "Bhindi masala", "1 bowl", 180, 4.0, 16.0, 12.0, LD, tags = setOf(FoodTag.LIGHT)),
        DesiFood("aloo_matar", "Aloo matar", "1 bowl", 220, 6.0, 30.0, 9.0, LD, tags = setOf(FoodTag.BUDGET)),
        DesiFood("mix_sabzi", "Mixed sabzi", "1 bowl", 180, 5.0, 20.0, 9.0, LD, tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        DesiFood("chana_masala", "Chana masala", "1 bowl", 280, 12.0, 38.0, 9.0, LD, tags = setOf(FoodTag.BUDGET)),
        DesiFood("lobia", "Lobia", "1 bowl", 260, 14.0, 36.0, 7.0, LD, tags = setOf(FoodTag.BUDGET)),
        DesiFood("rajma", "Rajma", "1 bowl", 270, 13.0, 38.0, 7.0, LD, tags = setOf(FoodTag.BUDGET)),
        DesiFood("kadhi", "Kadhi pakora", "1 bowl", 320, 10.0, 25.0, 20.0, LD, tags = setOf(FoodTag.FRIED)),
        DesiFood("egg_curry", "Anda curry", "2 eggs", 300, 15.0, 10.0, 22.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.HIGH_PROTEIN)),
        DesiFood("chicken_biryani", "Chicken biryani", "1 plate", 550, 25.0, 70.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        DesiFood("veg_pulao", "Vegetable pulao", "1 plate", 400, 9.0, 68.0, 10.0, LD, tags = setOf(FoodTag.COMPLETE)),
        DesiFood("chicken_pulao", "Chicken pulao", "1 plate", 500, 24.0, 65.0, 15.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        DesiFood("grilled_fish", "Grilled fish", "200 g", 280, 40.0, 2.0, 12.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        DesiFood("fish_curry", "Fish curry", "1 bowl", 320, 30.0, 8.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("chicken_salad", "Chicken salad bowl", "1 bowl", 320, 32.0, 15.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT, FoodTag.QUICK)),
        DesiFood("grilled_chicken", "Grilled chicken with veggies", "1 plate", 350, 45.0, 15.0, 10.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        DesiFood("shami", "Shami kabab", "2 kababs", 260, 16.0, 12.0, 16.0, setOf(L, D), vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("seekh", "Seekh kabab", "2 kababs", 320, 22.0, 4.0, 24.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        DesiFood("chicken_soup", "Chicken corn soup", "1 bowl", 180, 15.0, 15.0, 6.0, setOf(D), vegetarian = false, tags = setOf(FoodTag.LIGHT)),

        // ---- Sides ----
        DesiFood("roti", "Roti", "1 roti", 120, 4.0, 25.0, 1.0, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        DesiFood("rice", "Boiled rice", "1 cup", 205, 4.0, 45.0, 0.4, LD, FoodRole.SIDE_CARB),
        DesiFood("brown_rice", "Brown rice", "1 cup", 215, 5.0, 45.0, 1.8, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.LIGHT)),
        DesiFood("raita", "Raita", "1 bowl", 80, 4.0, 6.0, 4.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        DesiFood("salad", "Kachumber salad", "1 plate", 40, 1.5, 8.0, 0.3, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        // ---- Snacks / iftar ----
        DesiFood("roasted_chana", "Roasted chana", "½ cup", 180, 10.0, 27.0, 3.0, setOf(S), tags = setOf(FoodTag.LIGHT, FoodTag.HIGH_PROTEIN, FoodTag.BUDGET)),
        DesiFood("fruit_chaat", "Fruit chaat", "1 bowl", 150, 2.0, 36.0, 0.5, setOf(S), tags = setOf(FoodTag.LIGHT)),
        DesiFood("almonds", "Almonds", "15 almonds", 105, 4.0, 4.0, 9.0, setOf(S), tags = setOf(FoodTag.LIGHT)),
        DesiFood("peanuts", "Peanuts", "30 g", 170, 7.0, 5.0, 14.0, setOf(S), tags = setOf(FoodTag.BUDGET)),
        DesiFood("popcorn", "Plain popcorn", "3 cups", 95, 3.0, 19.0, 1.0, setOf(S), tags = setOf(FoodTag.LIGHT)),
        DesiFood("samosa", "Samosa", "1 samosa", 260, 5.0, 30.0, 14.0, setOf(S), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT)),
        DesiFood("pakora", "Pakoray", "5 pieces", 250, 6.0, 24.0, 15.0, setOf(S), tags = setOf(FoodTag.FRIED)),
        DesiFood("chana_chaat", "Chana chaat", "1 bowl", 220, 10.0, 34.0, 5.0, setOf(S), tags = setOf(FoodTag.LIGHT, FoodTag.HIGH_PROTEIN)),
        DesiFood("dahi_bhalla", "Dahi bhallay", "1 plate", 250, 8.0, 35.0, 9.0, setOf(S)),
        DesiFood("banana", "Banana", "1 banana", 105, 1.3, 27.0, 0.4, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        DesiFood("apple", "Apple", "1 apple", 95, 0.5, 25.0, 0.3, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        DesiFood("guava", "Guava", "1 guava", 70, 2.6, 14.0, 1.0, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        DesiFood("dates", "Dates", "3 dates", 70, 0.6, 18.0, 0.1, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        DesiFood("protein_shake", "Banana milk shake", "1 glass", 300, 12.0, 45.0, 8.0, setOf(B, S), FoodRole.DRINK),
        DesiFood("bhutta", "Bhutta (corn)", "1 cob", 120, 4.0, 27.0, 1.5, setOf(S), tags = setOf(FoodTag.LIGHT)),
        DesiFood("tea_biscuits", "Chai with 2 biscuits", "1 cup + 2", 220, 3.0, 30.0, 9.0, setOf(S), tags = setOf(FoodTag.INDULGENT)),
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): DesiFood? = byId[id]

    fun search(query: String): List<DesiFood> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { it.name.lowercase().contains(q) || it.id.contains(q) }
    }
}
