package com.rave.app.domain.food

enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK, SUHOOR, IFTAR }

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
    val groceries: List<String> = emptyList(),
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
    private val SU = MealSlot.SUHOOR
    private val IF = MealSlot.IFTAR
    private val LD = setOf(L, D)

    val all: List<DesiFood> = listOf(
        // ---- Breakfast / suhoor ----
        DesiFood("paratha", "Plain paratha", "1 paratha", 260, 5.0, 32.0, 12.0, setOf(B, SU), FoodRole.SIDE_CARB, groceries = listOf("Atta", "Ghee / oil")),
        DesiFood("aloo_paratha", "Aloo paratha", "1 paratha", 300, 6.0, 42.0, 12.0, setOf(B, SU), tags = setOf(FoodTag.BUDGET), groceries = listOf("Atta", "Potatoes", "Ghee / oil")),
        DesiFood("anda_paratha", "Anda paratha", "1 paratha", 400, 13.0, 35.0, 23.0, setOf(B, SU), groceries = listOf("Atta", "Eggs", "Ghee / oil")),
        DesiFood("keema_paratha", "Keema paratha", "1 paratha", 420, 18.0, 38.0, 22.0, setOf(B, SU), vegetarian = false, groceries = listOf("Atta", "Beef mince", "Ghee / oil")),
        DesiFood("boiled_eggs", "Boiled eggs", "2 eggs", 155, 13.0, 1.0, 11.0, setOf(B, SU, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN, FoodTag.LIGHT), groceries = listOf("Eggs")),
        DesiFood("omelette", "Desi omelette", "2 eggs, onion, tomato", 190, 13.0, 3.0, 14.0, setOf(B, SU), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN), groceries = listOf("Eggs", "Onions", "Tomatoes")),
        DesiFood("halwa_puri", "Halwa puri", "2 puri, halwa, chana", 750, 15.0, 90.0, 36.0, setOf(B), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT, FoodTag.COMPLETE), groceries = listOf("Maida", "Suji", "White chana", "Oil")),
        DesiFood("channay", "Channay", "1 cup", 270, 12.0, 40.0, 7.0, setOf(B, L, IF), tags = setOf(FoodTag.BUDGET), groceries = listOf("White chana", "Onions", "Tomatoes")),
        DesiFood("oats_milk", "Oats with milk", "1 bowl", 250, 10.0, 40.0, 6.0, setOf(B, SU), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT), groceries = listOf("Oats", "Milk")),
        DesiFood("daliya", "Daliya", "1 bowl", 220, 7.0, 40.0, 4.0, setOf(B, SU), tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET), groceries = listOf("Daliya", "Milk")),
        DesiFood("bread_butter", "Bread and butter", "2 slices", 220, 6.0, 28.0, 9.0, setOf(B), FoodRole.SIDE_CARB, tags = setOf(FoodTag.QUICK), groceries = listOf("Bread", "Butter")),
        DesiFood("egg_sandwich", "Egg sandwich", "1 sandwich", 300, 15.0, 30.0, 13.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN), groceries = listOf("Bread", "Eggs")),
        DesiFood("cornflakes", "Cornflakes with milk", "1 bowl", 220, 7.0, 40.0, 4.0, setOf(B), tags = setOf(FoodTag.QUICK), groceries = listOf("Cornflakes", "Milk")),
        DesiFood("chai", "Doodh pati chai", "1 cup", 110, 3.0, 14.0, 4.0, setOf(B, S, SU), FoodRole.DRINK, groceries = listOf("Tea leaves", "Milk", "Sugar")),
        DesiFood("green_tea", "Green tea", "1 cup", 2, 0.0, 0.5, 0.0, setOf(B, S, D), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT), groceries = listOf("Green tea")),
        DesiFood("lassi_sweet", "Sweet lassi", "1 glass", 220, 8.0, 35.0, 5.0, setOf(B, SU, IF), FoodRole.DRINK, groceries = listOf("Yogurt", "Sugar")),
        DesiFood("lassi_salt", "Namkeen lassi", "1 glass", 120, 7.0, 10.0, 6.0, setOf(B, SU, L), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT), groceries = listOf("Yogurt")),
        DesiFood("yogurt", "Dahi", "1 cup", 150, 9.0, 11.0, 8.0, setOf(B, SU, S), FoodRole.SIDE, tags = setOf(FoodTag.LIGHT), groceries = listOf("Yogurt")),
        DesiFood("khajla_pheni", "Pheni with milk", "1 bowl", 450, 9.0, 60.0, 19.0, setOf(SU), tags = setOf(FoodTag.INDULGENT), groceries = listOf("Pheni", "Milk")),
        DesiFood("roti_suhoor", "Roti with dahi", "1 roti, 1 cup dahi", 270, 13.0, 36.0, 9.0, setOf(SU), tags = setOf(FoodTag.LIGHT), groceries = listOf("Atta", "Yogurt")),

        // ---- Lunch / dinner mains ----
        DesiFood("daal_masoor", "Masoor daal", "1 bowl", 230, 13.0, 30.0, 7.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.LIGHT), groceries = listOf("Masoor daal", "Onions", "Tomatoes")),
        DesiFood("daal_mash", "Daal mash", "1 bowl", 260, 14.0, 30.0, 9.0, LD, tags = setOf(FoodTag.BUDGET), groceries = listOf("Mash daal", "Onions")),
        DesiFood("chana_daal", "Chana daal", "1 bowl", 260, 13.0, 36.0, 7.0, LD, tags = setOf(FoodTag.BUDGET), groceries = listOf("Chana daal", "Onions", "Tomatoes")),
        DesiFood("daal_chawal", "Daal chawal", "1 plate", 480, 16.0, 80.0, 10.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.COMPLETE), groceries = listOf("Masoor daal", "Rice")),
        DesiFood("chicken_karahi", "Chicken karahi", "1 bowl (200 g)", 380, 30.0, 8.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Chicken", "Tomatoes", "Ginger & garlic", "Oil")),
        DesiFood("chicken_handi", "Chicken handi", "1 bowl", 420, 28.0, 10.0, 30.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Chicken", "Yogurt", "Cream")),
        DesiFood("chicken_tikka", "Chicken tikka", "2 pieces", 300, 35.0, 4.0, 16.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Chicken", "Yogurt", "Tikka masala")),
        DesiFood("chicken_qorma", "Chicken qorma", "1 bowl", 450, 28.0, 12.0, 32.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT), groceries = listOf("Chicken", "Yogurt", "Onions", "Oil")),
        DesiFood("mutton_karahi", "Mutton karahi", "1 bowl", 480, 30.0, 8.0, 36.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT, FoodTag.HIGH_PROTEIN), groceries = listOf("Mutton", "Tomatoes", "Oil")),
        DesiFood("nihari", "Beef nihari", "1 bowl", 520, 32.0, 14.0, 38.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT), groceries = listOf("Beef", "Nihari masala", "Atta")),
        DesiFood("haleem", "Haleem", "1 bowl", 380, 22.0, 40.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Beef", "Wheat", "Mixed daal")),
        DesiFood("aloo_gosht", "Aloo gosht", "1 bowl", 420, 24.0, 20.0, 26.0, LD, vegetarian = false, groceries = listOf("Beef", "Potatoes", "Onions")),
        DesiFood("keema_matar", "Keema matar", "1 bowl", 380, 24.0, 14.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Beef mince", "Peas", "Onions")),
        DesiFood("palak_paneer", "Palak paneer", "1 bowl", 320, 15.0, 10.0, 24.0, LD, groceries = listOf("Spinach", "Paneer", "Cream")),
        DesiFood("saag", "Sarson ka saag", "1 bowl", 220, 6.0, 14.0, 16.0, LD, groceries = listOf("Mustard greens", "Spinach", "Makai atta")),
        DesiFood("bhindi", "Bhindi masala", "1 bowl", 180, 4.0, 16.0, 12.0, LD, tags = setOf(FoodTag.LIGHT), groceries = listOf("Okra", "Onions", "Tomatoes")),
        DesiFood("aloo_matar", "Aloo matar", "1 bowl", 220, 6.0, 30.0, 9.0, LD, tags = setOf(FoodTag.BUDGET), groceries = listOf("Potatoes", "Peas", "Tomatoes")),
        DesiFood("mix_sabzi", "Mixed sabzi", "1 bowl", 180, 5.0, 20.0, 9.0, LD, tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET), groceries = listOf("Mixed vegetables", "Onions")),
        DesiFood("chana_masala", "Chana masala", "1 bowl", 280, 12.0, 38.0, 9.0, LD, tags = setOf(FoodTag.BUDGET), groceries = listOf("White chana", "Onions", "Tomatoes")),
        DesiFood("lobia", "Lobia", "1 bowl", 260, 14.0, 36.0, 7.0, LD, tags = setOf(FoodTag.BUDGET), groceries = listOf("Lobia", "Onions", "Tomatoes")),
        DesiFood("rajma", "Rajma", "1 bowl", 270, 13.0, 38.0, 7.0, LD, tags = setOf(FoodTag.BUDGET), groceries = listOf("Kidney beans", "Onions", "Tomatoes")),
        DesiFood("kadhi", "Kadhi pakora", "1 bowl", 320, 10.0, 25.0, 20.0, LD, tags = setOf(FoodTag.FRIED), groceries = listOf("Yogurt", "Besan", "Oil")),
        DesiFood("egg_curry", "Anda curry", "2 eggs", 300, 15.0, 10.0, 22.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.HIGH_PROTEIN), groceries = listOf("Eggs", "Onions", "Tomatoes")),
        DesiFood("chicken_biryani", "Chicken biryani", "1 plate", 550, 25.0, 70.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE), groceries = listOf("Chicken", "Basmati rice", "Biryani masala", "Yogurt")),
        DesiFood("veg_pulao", "Vegetable pulao", "1 plate", 400, 9.0, 68.0, 10.0, LD, tags = setOf(FoodTag.COMPLETE), groceries = listOf("Basmati rice", "Mixed vegetables")),
        DesiFood("chicken_pulao", "Chicken pulao", "1 plate", 500, 24.0, 65.0, 15.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE), groceries = listOf("Chicken", "Basmati rice")),
        DesiFood("grilled_fish", "Grilled fish", "200 g", 280, 40.0, 2.0, 12.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT), groceries = listOf("Fish", "Lemon")),
        DesiFood("fish_curry", "Fish curry", "1 bowl", 320, 30.0, 8.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Fish", "Tomatoes", "Onions")),
        DesiFood("chicken_salad", "Chicken salad bowl", "1 bowl", 320, 32.0, 15.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT, FoodTag.QUICK), groceries = listOf("Chicken", "Lettuce", "Cucumber", "Tomatoes")),
        DesiFood("grilled_chicken", "Grilled chicken with veggies", "1 plate", 350, 45.0, 15.0, 10.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT), groceries = listOf("Chicken breast", "Mixed vegetables")),
        DesiFood("shami", "Shami kabab", "2 kababs", 260, 16.0, 12.0, 16.0, setOf(L, D, IF), vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Beef", "Chana daal", "Eggs")),
        DesiFood("seekh", "Seekh kabab", "2 kababs", 320, 22.0, 4.0, 24.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN), groceries = listOf("Beef mince", "Onions")),
        DesiFood("chicken_soup", "Chicken corn soup", "1 bowl", 180, 15.0, 15.0, 6.0, setOf(D, IF), vegetarian = false, tags = setOf(FoodTag.LIGHT), groceries = listOf("Chicken", "Corn", "Eggs")),

        // ---- Sides ----
        DesiFood("roti", "Roti", "1 roti", 120, 4.0, 25.0, 1.0, LD + SU, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET), groceries = listOf("Atta")),
        DesiFood("rice", "Boiled rice", "1 cup", 205, 4.0, 45.0, 0.4, LD, FoodRole.SIDE_CARB, groceries = listOf("Rice")),
        DesiFood("brown_rice", "Brown rice", "1 cup", 215, 5.0, 45.0, 1.8, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.LIGHT), groceries = listOf("Brown rice")),
        DesiFood("raita", "Raita", "1 bowl", 80, 4.0, 6.0, 4.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT), groceries = listOf("Yogurt", "Cucumber")),
        DesiFood("salad", "Kachumber salad", "1 plate", 40, 1.5, 8.0, 0.3, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT), groceries = listOf("Cucumber", "Tomatoes", "Onions")),

        // ---- Snacks / iftar ----
        DesiFood("roasted_chana", "Roasted chana", "½ cup", 180, 10.0, 27.0, 3.0, setOf(S), tags = setOf(FoodTag.LIGHT, FoodTag.HIGH_PROTEIN, FoodTag.BUDGET), groceries = listOf("Roasted chana")),
        DesiFood("fruit_chaat", "Fruit chaat", "1 bowl", 150, 2.0, 36.0, 0.5, setOf(S, IF), tags = setOf(FoodTag.LIGHT), groceries = listOf("Seasonal fruit", "Chaat masala")),
        DesiFood("almonds", "Almonds", "15 almonds", 105, 4.0, 4.0, 9.0, setOf(S, SU), tags = setOf(FoodTag.LIGHT), groceries = listOf("Almonds")),
        DesiFood("peanuts", "Peanuts", "30 g", 170, 7.0, 5.0, 14.0, setOf(S), tags = setOf(FoodTag.BUDGET), groceries = listOf("Peanuts")),
        DesiFood("popcorn", "Plain popcorn", "3 cups", 95, 3.0, 19.0, 1.0, setOf(S), tags = setOf(FoodTag.LIGHT), groceries = listOf("Popcorn kernels")),
        DesiFood("samosa", "Samosa", "1 samosa", 260, 5.0, 30.0, 14.0, setOf(S, IF), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT), groceries = listOf("Samosa patti", "Potatoes", "Oil")),
        DesiFood("pakora", "Pakoray", "5 pieces", 250, 6.0, 24.0, 15.0, setOf(S, IF), tags = setOf(FoodTag.FRIED), groceries = listOf("Besan", "Onions", "Potatoes", "Oil")),
        DesiFood("chana_chaat", "Chana chaat", "1 bowl", 220, 10.0, 34.0, 5.0, setOf(S, IF), tags = setOf(FoodTag.LIGHT, FoodTag.HIGH_PROTEIN), groceries = listOf("White chana", "Onions", "Tomatoes", "Chaat masala")),
        DesiFood("dahi_bhalla", "Dahi bhallay", "1 plate", 250, 8.0, 35.0, 9.0, setOf(S, IF), groceries = listOf("Mash daal", "Yogurt")),
        DesiFood("banana", "Banana", "1 banana", 105, 1.3, 27.0, 0.4, setOf(B, S, SU), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT), groceries = listOf("Bananas")),
        DesiFood("apple", "Apple", "1 apple", 95, 0.5, 25.0, 0.3, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT), groceries = listOf("Apples")),
        DesiFood("guava", "Guava", "1 guava", 70, 2.6, 14.0, 1.0, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT), groceries = listOf("Guavas")),
        DesiFood("dates", "Dates", "3 dates", 70, 0.6, 18.0, 0.1, setOf(S, IF, SU), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT), groceries = listOf("Dates")),
        DesiFood("protein_shake", "Banana milk shake", "1 glass", 300, 12.0, 45.0, 8.0, setOf(B, S, SU), FoodRole.DRINK, groceries = listOf("Milk", "Bananas")),
        DesiFood("bhutta", "Bhutta (corn)", "1 cob", 120, 4.0, 27.0, 1.5, setOf(S), tags = setOf(FoodTag.LIGHT), groceries = listOf("Corn")),
        DesiFood("rooh_afza", "Rooh Afza milk", "1 glass", 200, 7.0, 30.0, 6.0, setOf(IF, SU), FoodRole.DRINK, groceries = listOf("Rooh Afza", "Milk")),
        DesiFood("tea_biscuits", "Chai with 2 biscuits", "1 cup + 2", 220, 3.0, 30.0, 9.0, setOf(S), tags = setOf(FoodTag.INDULGENT), groceries = listOf("Tea leaves", "Milk", "Biscuits")),
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): DesiFood? = byId[id]

    fun search(query: String): List<DesiFood> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { it.name.lowercase().contains(q) || it.id.contains(q) }
    }
}
