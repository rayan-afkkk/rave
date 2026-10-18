package com.rave.app.domain.food

enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK }

enum class FoodRole { MAIN, SIDE_CARB, SIDE, DRINK }

enum class FoodTag { QUICK, BUDGET, LIGHT, HIGH_PROTEIN, FRIED, INDULGENT, COMPLETE }

/** Where a dish comes from. [GLOBAL] = everyday staples eaten almost everywhere (eggs, fruit, rice, oats…). */
enum class Cuisine { GLOBAL, SOUTH_ASIAN, MIDDLE_EASTERN, MEDITERRANEAN, EAST_ASIAN, LATIN_AMERICAN, AMERICAN_EUROPEAN }

/**
 * One serving of a common food. Values are typical home-cooked / restaurant estimates per the serving shown.
 * [halal] is false for dishes that are built around pork or alcohol (skipped when the user wants halal only).
 */
data class FoodItem(
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
    val halal: Boolean = true,
    val cuisine: Cuisine = Cuisine.GLOBAL,
)

/**
 * Built-in food list for the meal planner and quick logging, grouped by cuisine. Calories are per the
 * serving shown, based on standard nutrition tables (USDA and similar) for typical recipes (oil varies by kitchen, ±15%).
 */
object FoodCatalog {
    private val B = MealSlot.BREAKFAST
    private val L = MealSlot.LUNCH
    private val D = MealSlot.DINNER
    private val S = MealSlot.SNACK
    private val LD = setOf(L, D)

    private fun cuisine(c: Cuisine, vararg items: FoodItem): List<FoodItem> = items.map { it.copy(cuisine = c) }

    // ---------------------------------------------------------------- Everyday staples
    private val global = cuisine(
        Cuisine.GLOBAL,
        // Breakfast
        FoodItem("boiled_eggs", "Boiled eggs", "2 eggs", 155, 13.0, 1.0, 11.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("eggs_toast", "Scrambled eggs on toast", "2 eggs, 1 slice", 300, 16.0, 16.0, 19.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        FoodItem("veg_omelette", "Vegetable omelette", "3 eggs", 280, 20.0, 6.0, 20.0, setOf(B, L, D), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN, FoodTag.BUDGET)),
        FoodItem("oats_milk", "Oatmeal with milk", "1 bowl", 250, 10.0, 40.0, 6.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("overnight_oats", "Overnight oats with fruit", "1 jar", 350, 14.0, 52.0, 10.0, setOf(B), tags = setOf(FoodTag.QUICK)),
        FoodItem("yogurt_granola", "Yogurt with granola and berries", "1 bowl", 320, 14.0, 45.0, 10.0, setOf(B, S), tags = setOf(FoodTag.QUICK)),
        FoodItem("greek_yogurt", "Greek yogurt, plain", "170 g", 150, 17.0, 7.0, 4.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("cornflakes", "Cereal with milk", "1 bowl", 220, 7.0, 40.0, 4.0, setOf(B), tags = setOf(FoodTag.QUICK)),
        FoodItem("bread_butter", "Toast and butter", "2 slices", 220, 6.0, 28.0, 9.0, setOf(B), FoodRole.SIDE_CARB, tags = setOf(FoodTag.QUICK)),
        FoodItem("peanut_butter_toast", "Peanut butter toast", "2 slices, 2 tbsp", 350, 13.0, 32.0, 19.0, setOf(B, S), tags = setOf(FoodTag.QUICK)),
        FoodItem("avocado_toast", "Avocado toast", "1 slice, ½ avocado", 250, 6.0, 25.0, 15.0, setOf(B), tags = setOf(FoodTag.QUICK)),
        FoodItem("egg_sandwich", "Egg sandwich", "1 sandwich", 300, 15.0, 30.0, 13.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        FoodItem("beans_toast", "Beans on toast", "1 plate", 350, 15.0, 55.0, 6.0, setOf(B, L), tags = setOf(FoodTag.BUDGET, FoodTag.QUICK, FoodTag.COMPLETE)),
        // Drinks
        FoodItem("coffee_milk", "Coffee with milk", "1 cup", 40, 2.0, 4.0, 2.0, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("black_coffee", "Black coffee", "1 cup", 5, 0.3, 0.0, 0.0, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("green_tea", "Green tea", "1 cup", 2, 0.0, 0.5, 0.0, setOf(B, S, D), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("orange_juice", "Orange juice", "1 glass (250 ml)", 110, 2.0, 26.0, 0.5, setOf(B), FoodRole.DRINK),
        FoodItem("milk", "Glass of milk", "250 ml", 150, 8.0, 12.0, 8.0, setOf(B, S), FoodRole.DRINK),
        FoodItem("smoothie", "Fruit smoothie", "1 glass", 200, 4.0, 45.0, 1.0, setOf(B, S), FoodRole.DRINK),
        FoodItem("protein_shake_whey", "Protein shake", "1 scoop with water", 120, 24.0, 3.0, 1.5, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT, FoodTag.QUICK)),
        FoodItem("protein_shake", "Banana milkshake", "1 glass", 300, 12.0, 45.0, 8.0, setOf(B, S), FoodRole.DRINK),
        // Mains
        FoodItem("grilled_chicken", "Grilled chicken with veggies", "1 plate", 350, 45.0, 15.0, 10.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("chicken_salad", "Grilled chicken salad", "1 bowl", 320, 32.0, 15.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT, FoodTag.QUICK)),
        FoodItem("chicken_rice_bowl", "Chicken and rice bowl", "1 bowl", 550, 40.0, 60.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.COMPLETE)),
        FoodItem("chicken_sandwich", "Grilled chicken sandwich", "1 sandwich", 400, 30.0, 40.0, 13.0, LD, vegetarian = false, tags = setOf(FoodTag.QUICK, FoodTag.COMPLETE, FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_wrap", "Chicken wrap", "1 wrap", 450, 30.0, 40.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.QUICK, FoodTag.COMPLETE)),
        FoodItem("grilled_fish", "Grilled fish", "200 g", 280, 40.0, 2.0, 12.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("baked_salmon", "Baked salmon", "150 g", 310, 34.0, 0.0, 19.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("tuna_salad", "Tuna salad", "1 bowl", 280, 30.0, 10.0, 13.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT, FoodTag.QUICK)),
        FoodItem("veg_soup", "Vegetable soup", "1 bowl", 120, 4.0, 20.0, 3.0, LD, tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("chickpea_salad", "Chickpea salad bowl", "1 bowl", 400, 15.0, 50.0, 15.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.BUDGET)),
        FoodItem("grain_bowl", "Veggie grain bowl", "1 bowl", 450, 15.0, 60.0, 16.0, LD, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("veggie_pasta", "Pasta with vegetables", "1 plate", 450, 15.0, 75.0, 10.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.BUDGET)),
        // Carbs and sides
        FoodItem("rice", "Boiled rice", "1 cup", 205, 4.0, 45.0, 0.4, LD, FoodRole.SIDE_CARB),
        FoodItem("brown_rice", "Brown rice", "1 cup", 215, 5.0, 45.0, 1.8, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.LIGHT)),
        FoodItem("wholewheat_bread", "Whole-wheat bread", "2 slices", 160, 8.0, 28.0, 2.0, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        FoodItem("baked_potato", "Baked potato", "1 medium", 160, 4.0, 37.0, 0.2, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        FoodItem("quinoa", "Quinoa", "1 cup", 220, 8.0, 39.0, 3.5, LD, FoodRole.SIDE_CARB),
        FoodItem("green_salad", "Green salad", "1 bowl", 80, 2.0, 8.0, 5.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("steamed_veg", "Steamed vegetables", "1 cup", 60, 3.0, 12.0, 0.5, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        // Snacks
        FoodItem("banana", "Banana", "1 banana", 105, 1.3, 27.0, 0.4, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("apple", "Apple", "1 apple", 95, 0.5, 25.0, 0.3, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("orange", "Orange", "1 orange", 62, 1.2, 15.0, 0.2, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("grapes", "Grapes", "1 cup", 104, 1.1, 27.0, 0.2, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("berries", "Mixed berries", "1 cup", 85, 1.0, 21.0, 0.5, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("almonds", "Almonds", "15 almonds", 105, 4.0, 4.0, 9.0, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("mixed_nuts", "Mixed nuts", "30 g", 175, 5.0, 6.0, 15.0, setOf(S)),
        FoodItem("peanuts", "Peanuts", "30 g", 170, 7.0, 5.0, 14.0, setOf(S), tags = setOf(FoodTag.BUDGET)),
        FoodItem("popcorn", "Plain popcorn", "3 cups", 95, 3.0, 19.0, 1.0, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("protein_bar", "Protein bar", "1 bar (60 g)", 220, 20.0, 23.0, 7.0, setOf(S), tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.QUICK)),
        FoodItem("rice_cakes", "Rice cakes", "2 cakes", 70, 1.5, 15.0, 0.5, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("dark_chocolate", "Dark chocolate", "2 squares (20 g)", 120, 1.5, 9.0, 9.0, setOf(S), tags = setOf(FoodTag.INDULGENT)),
    )

    // ---------------------------------------------------------------- Pakistan, India, Bangladesh…
    private val southAsian = cuisine(
        Cuisine.SOUTH_ASIAN,
        FoodItem("paratha", "Plain paratha", "1 paratha", 260, 5.0, 32.0, 12.0, setOf(B), FoodRole.SIDE_CARB),
        FoodItem("aloo_paratha", "Aloo paratha", "1 paratha", 300, 6.0, 42.0, 12.0, setOf(B), tags = setOf(FoodTag.BUDGET)),
        FoodItem("anda_paratha", "Anda paratha", "1 paratha", 400, 13.0, 35.0, 23.0, setOf(B)),
        FoodItem("keema_paratha", "Keema paratha", "1 paratha", 420, 18.0, 38.0, 22.0, setOf(B), vegetarian = false),
        FoodItem("omelette", "Masala omelette", "2 eggs, onion, tomato", 190, 13.0, 3.0, 14.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        FoodItem("halwa_puri", "Halwa puri", "2 puri, halwa, chana", 750, 15.0, 90.0, 36.0, setOf(B), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT, FoodTag.COMPLETE)),
        FoodItem("channay", "Channay", "1 cup", 270, 12.0, 40.0, 7.0, setOf(B, L), tags = setOf(FoodTag.BUDGET)),
        FoodItem("daliya", "Daliya", "1 bowl", 220, 7.0, 40.0, 4.0, setOf(B), tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("poha", "Poha", "1 plate", 250, 5.0, 45.0, 6.0, setOf(B), tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("idli_sambar", "Idli with sambar", "3 idli, 1 bowl", 300, 11.0, 58.0, 3.0, setOf(B), tags = setOf(FoodTag.LIGHT, FoodTag.COMPLETE)),
        FoodItem("masala_dosa", "Masala dosa", "1 dosa", 390, 8.0, 55.0, 15.0, setOf(B, L), tags = setOf(FoodTag.COMPLETE)),
        FoodItem("chai", "Milk tea (chai)", "1 cup", 110, 3.0, 14.0, 4.0, setOf(B, S), FoodRole.DRINK),
        FoodItem("lassi_sweet", "Sweet lassi", "1 glass", 220, 8.0, 35.0, 5.0, setOf(B), FoodRole.DRINK),
        FoodItem("lassi_salt", "Salted lassi", "1 glass", 120, 7.0, 10.0, 6.0, setOf(B, L), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("yogurt", "Dahi (yogurt)", "1 cup", 150, 9.0, 11.0, 8.0, setOf(B, S), FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("daal_masoor", "Masoor daal", "1 bowl", 230, 13.0, 30.0, 7.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.LIGHT)),
        FoodItem("daal_mash", "Daal mash", "1 bowl", 260, 14.0, 30.0, 9.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("chana_daal", "Chana daal", "1 bowl", 260, 13.0, 36.0, 7.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("daal_chawal", "Daal chawal", "1 plate", 480, 16.0, 80.0, 10.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.COMPLETE)),
        FoodItem("chicken_karahi", "Chicken karahi", "1 bowl (200 g)", 380, 30.0, 8.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_handi", "Chicken handi", "1 bowl", 420, 28.0, 10.0, 30.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_tikka", "Chicken tikka", "2 pieces", 300, 35.0, 4.0, 16.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_qorma", "Chicken qorma", "1 bowl", 450, 28.0, 12.0, 32.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT)),
        FoodItem("butter_chicken", "Butter chicken", "1 bowl", 490, 30.0, 14.0, 35.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT)),
        FoodItem("mutton_karahi", "Mutton karahi", "1 bowl", 480, 30.0, 8.0, 36.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT, FoodTag.HIGH_PROTEIN)),
        FoodItem("nihari", "Beef nihari", "1 bowl", 520, 32.0, 14.0, 38.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT)),
        FoodItem("haleem", "Haleem", "1 bowl", 380, 22.0, 40.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("aloo_gosht", "Aloo gosht", "1 bowl", 420, 24.0, 20.0, 26.0, LD, vegetarian = false),
        FoodItem("keema_matar", "Keema matar", "1 bowl", 380, 24.0, 14.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("palak_paneer", "Palak paneer", "1 bowl", 320, 15.0, 10.0, 24.0, LD),
        FoodItem("saag", "Sarson ka saag", "1 bowl", 220, 6.0, 14.0, 16.0, LD),
        FoodItem("bhindi", "Bhindi masala", "1 bowl", 180, 4.0, 16.0, 12.0, LD, tags = setOf(FoodTag.LIGHT)),
        FoodItem("aloo_matar", "Aloo matar", "1 bowl", 220, 6.0, 30.0, 9.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("mix_sabzi", "Mixed sabzi", "1 bowl", 180, 5.0, 20.0, 9.0, LD, tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("chana_masala", "Chana masala", "1 bowl", 280, 12.0, 38.0, 9.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("lobia", "Lobia", "1 bowl", 260, 14.0, 36.0, 7.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("rajma", "Rajma", "1 bowl", 270, 13.0, 38.0, 7.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("kadhi", "Kadhi pakora", "1 bowl", 320, 10.0, 25.0, 20.0, LD, tags = setOf(FoodTag.FRIED)),
        FoodItem("egg_curry", "Egg curry", "2 eggs", 300, 15.0, 10.0, 22.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_biryani", "Chicken biryani", "1 plate", 550, 25.0, 70.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("veg_pulao", "Vegetable pulao", "1 plate", 400, 9.0, 68.0, 10.0, LD, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("chicken_pulao", "Chicken pulao", "1 plate", 500, 24.0, 65.0, 15.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("fish_curry", "Fish curry", "1 bowl", 320, 30.0, 8.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("shami", "Shami kabab", "2 kababs", 260, 16.0, 12.0, 16.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("seekh", "Seekh kabab", "2 kababs", 320, 22.0, 4.0, 24.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_soup", "Chicken corn soup", "1 bowl", 180, 15.0, 15.0, 6.0, setOf(D), vegetarian = false, tags = setOf(FoodTag.LIGHT)),

        FoodItem("roti", "Roti", "1 roti", 120, 4.0, 25.0, 1.0, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        FoodItem("naan", "Naan", "1 naan", 260, 9.0, 45.0, 5.0, LD, FoodRole.SIDE_CARB),
        FoodItem("basmati_rice", "Basmati rice", "1 cup", 210, 4.4, 46.0, 0.5, LD, FoodRole.SIDE_CARB),
        FoodItem("raita", "Raita", "1 bowl", 80, 4.0, 6.0, 4.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("salad", "Kachumber salad", "1 plate", 40, 1.5, 8.0, 0.3, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("roasted_chana", "Roasted chana", "½ cup", 180, 10.0, 27.0, 3.0, setOf(S), tags = setOf(FoodTag.LIGHT, FoodTag.HIGH_PROTEIN, FoodTag.BUDGET)),
        FoodItem("fruit_chaat", "Fruit chaat", "1 bowl", 150, 2.0, 36.0, 0.5, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("samosa", "Samosa", "1 samosa", 260, 5.0, 30.0, 14.0, setOf(S), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT)),
        FoodItem("pakora", "Pakoras", "5 pieces", 250, 6.0, 24.0, 15.0, setOf(S), tags = setOf(FoodTag.FRIED)),
        FoodItem("chana_chaat", "Chana chaat", "1 bowl", 220, 10.0, 34.0, 5.0, setOf(S), tags = setOf(FoodTag.LIGHT, FoodTag.HIGH_PROTEIN)),
        FoodItem("dahi_bhalla", "Dahi bhallay", "1 plate", 250, 8.0, 35.0, 9.0, setOf(S)),
        FoodItem("guava", "Guava", "1 guava", 70, 2.6, 14.0, 1.0, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("bhutta", "Roasted corn (bhutta)", "1 cob", 120, 4.0, 27.0, 1.5, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("tea_biscuits", "Chai with 2 biscuits", "1 cup + 2", 220, 3.0, 30.0, 9.0, setOf(S), tags = setOf(FoodTag.INDULGENT)),
    )

    // ---------------------------------------------------------------- Arab world, Turkey, Iran
    private val middleEastern = cuisine(
        Cuisine.MIDDLE_EASTERN,
        FoodItem("shakshuka", "Shakshuka", "2 eggs in tomato sauce", 300, 15.0, 18.0, 18.0, setOf(B, L), tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.BUDGET)),
        FoodItem("ful_medames", "Ful medames", "1 bowl", 300, 15.0, 40.0, 9.0, setOf(B), tags = setOf(FoodTag.BUDGET, FoodTag.HIGH_PROTEIN)),
        FoodItem("labneh_pita", "Labneh with za'atar and pita", "1 plate", 330, 12.0, 38.0, 15.0, setOf(B, S)),
        FoodItem("manakish", "Za'atar manakish", "1 flatbread", 360, 8.0, 45.0, 16.0, setOf(B)),
        FoodItem("menemen", "Menemen (Turkish eggs)", "1 pan", 280, 14.0, 10.0, 20.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        FoodItem("turkish_tea", "Turkish tea", "1 glass, 1 sugar", 20, 0.0, 5.0, 0.0, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("arabic_coffee", "Arabic coffee", "1 small cup", 5, 0.1, 1.0, 0.0, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("ayran", "Ayran", "1 glass", 75, 4.0, 6.0, 4.0, setOf(B, L, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),

        FoodItem("falafel_wrap", "Falafel wrap", "1 wrap", 520, 16.0, 60.0, 24.0, LD, tags = setOf(FoodTag.FRIED, FoodTag.COMPLETE)),
        FoodItem("shawarma_wrap", "Chicken shawarma wrap", "1 wrap", 550, 35.0, 50.0, 22.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.HIGH_PROTEIN)),
        FoodItem("lentil_soup", "Red lentil soup", "1 bowl", 230, 13.0, 34.0, 5.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.LIGHT)),
        FoodItem("shish_tawook", "Shish tawook", "2 skewers (200 g)", 330, 45.0, 5.0, 14.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("lamb_kofta", "Lamb kofta", "3 skewers", 400, 26.0, 6.0, 30.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("mujadara", "Mujadara (lentils and rice)", "1 plate", 450, 16.0, 70.0, 12.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.COMPLETE)),
        FoodItem("koshari", "Koshari", "1 plate", 550, 18.0, 100.0, 9.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.COMPLETE)),
        FoodItem("chicken_kabsa", "Chicken kabsa", "1 plate", 600, 32.0, 70.0, 20.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("chicken_tagine", "Chicken tagine", "1 bowl", 420, 32.0, 25.0, 20.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("veg_tagine", "Vegetable and chickpea tagine", "1 bowl", 320, 11.0, 48.0, 10.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("stuffed_peppers", "Stuffed peppers (dolma)", "2 peppers", 360, 18.0, 35.0, 16.0, LD, vegetarian = false),

        FoodItem("pita", "Pita bread", "1 pita", 165, 5.5, 33.0, 0.7, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        FoodItem("bulgur", "Bulgur pilaf", "1 cup", 190, 6.0, 36.0, 3.5, LD, FoodRole.SIDE_CARB),
        FoodItem("couscous", "Couscous", "1 cup", 176, 6.0, 36.0, 0.3, LD, FoodRole.SIDE_CARB),
        FoodItem("vermicelli_rice", "Rice with vermicelli", "1 cup", 250, 5.0, 48.0, 4.0, LD, FoodRole.SIDE_CARB),
        FoodItem("hummus", "Hummus", "¼ cup (60 g)", 100, 4.7, 8.6, 5.8, setOf(L, D, S), FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("tabbouleh", "Tabbouleh", "1 cup", 150, 3.0, 14.0, 10.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("fattoush", "Fattoush salad", "1 bowl", 170, 3.0, 18.0, 10.0, LD, FoodRole.SIDE),
        FoodItem("baba_ganoush", "Baba ganoush", "¼ cup", 90, 2.0, 7.0, 7.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("dates", "Dates", "3 dates", 70, 0.6, 18.0, 0.1, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("pistachios", "Pistachios", "30 g", 160, 6.0, 8.0, 13.0, setOf(S)),
        FoodItem("hummus_veg", "Hummus with veggie sticks", "1 plate", 180, 6.0, 18.0, 9.0, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("baklava", "Baklava", "1 piece", 230, 3.0, 25.0, 14.0, setOf(S), tags = setOf(FoodTag.INDULGENT)),
    )

    // ---------------------------------------------------------------- Italy, Greece, Spain…
    private val mediterranean = cuisine(
        Cuisine.MEDITERRANEAN,
        FoodItem("yogurt_honey", "Greek yogurt with honey and walnuts", "1 bowl", 280, 17.0, 25.0, 13.0, setOf(B, S), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        FoodItem("spanish_tortilla", "Spanish tortilla", "1 slice", 290, 10.0, 20.0, 19.0, setOf(B, L)),
        FoodItem("pan_tomate", "Pan con tomate", "2 slices", 250, 6.0, 34.0, 10.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.BUDGET)),
        FoodItem("feta_omelette", "Spinach and feta omelette", "2 eggs", 300, 20.0, 4.0, 23.0, setOf(B), tags = setOf(FoodTag.QUICK, FoodTag.HIGH_PROTEIN)),
        FoodItem("espresso", "Espresso", "1 shot", 2, 0.1, 0.0, 0.0, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("cappuccino", "Cappuccino", "1 cup", 110, 6.0, 9.0, 5.0, setOf(B, S), FoodRole.DRINK),

        FoodItem("pasta_bolognese", "Pasta bolognese", "1 plate", 600, 30.0, 70.0, 20.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("pasta_pomodoro", "Pasta al pomodoro", "1 plate", 450, 14.0, 75.0, 11.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.BUDGET)),
        FoodItem("pasta_carbonara", "Pasta carbonara", "1 plate", 700, 28.0, 70.0, 33.0, LD, vegetarian = false, halal = false, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("pizza_margherita", "Margherita pizza", "2 slices", 550, 22.0, 66.0, 20.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("lasagna", "Beef lasagna", "1 piece", 600, 32.0, 45.0, 32.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("mushroom_risotto", "Mushroom risotto", "1 plate", 450, 11.0, 65.0, 15.0, LD, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("minestrone", "Minestrone soup", "1 bowl", 200, 8.0, 30.0, 5.0, LD, tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("greek_chicken_salad", "Greek salad with chicken", "1 bowl", 400, 35.0, 12.0, 23.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("souvlaki", "Chicken souvlaki", "2 skewers", 350, 40.0, 6.0, 17.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("moussaka", "Moussaka", "1 portion", 520, 25.0, 30.0, 33.0, LD, vegetarian = false, tags = setOf(FoodTag.INDULGENT)),
        FoodItem("paella", "Seafood paella", "1 plate", 520, 28.0, 65.0, 15.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("sea_bass", "Baked sea bass with lemon", "200 g", 260, 45.0, 1.0, 8.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("gigantes", "Baked giant beans (gigantes)", "1 bowl", 300, 13.0, 40.0, 10.0, LD, tags = setOf(FoodTag.BUDGET)),
        FoodItem("chicken_cacciatore", "Chicken cacciatore", "1 plate", 380, 35.0, 12.0, 20.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),

        FoodItem("crusty_bread", "Crusty bread", "2 slices", 160, 6.0, 30.0, 1.5, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        FoodItem("roast_potatoes", "Roasted potatoes", "1 cup", 200, 4.0, 30.0, 8.0, LD, FoodRole.SIDE_CARB),
        FoodItem("orzo", "Orzo with herbs", "1 cup", 220, 7.0, 42.0, 2.5, LD, FoodRole.SIDE_CARB),
        FoodItem("tzatziki", "Tzatziki", "¼ cup", 60, 3.0, 3.0, 4.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("greek_salad", "Greek salad", "1 bowl", 200, 6.0, 10.0, 16.0, LD, FoodRole.SIDE),
        FoodItem("caprese", "Caprese salad", "1 plate", 220, 12.0, 5.0, 17.0, LD, FoodRole.SIDE),
        FoodItem("grilled_veg", "Grilled vegetables", "1 cup", 120, 3.0, 14.0, 7.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("olives", "Olives", "10 olives", 50, 0.4, 2.0, 5.0, setOf(S), tags = setOf(FoodTag.LIGHT)),
        FoodItem("bruschetta", "Bruschetta", "2 pieces", 200, 5.0, 26.0, 8.0, setOf(S)),
        FoodItem("figs", "Fresh figs", "2 figs", 75, 0.8, 19.0, 0.3, setOf(S), tags = setOf(FoodTag.QUICK, FoodTag.LIGHT)),
        FoodItem("gelato", "Gelato", "1 scoop", 200, 4.0, 25.0, 10.0, setOf(S), tags = setOf(FoodTag.INDULGENT)),
    )

    // ---------------------------------------------------------------- China, Japan, Korea, South-East Asia
    private val eastAsian = cuisine(
        Cuisine.EAST_ASIAN,
        FoodItem("congee", "Chicken congee", "1 bowl", 250, 15.0, 35.0, 5.0, setOf(B, D), vegetarian = false, tags = setOf(FoodTag.LIGHT)),
        FoodItem("japanese_breakfast", "Rice, miso soup and grilled fish", "1 tray", 450, 28.0, 55.0, 12.0, setOf(B), vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.HIGH_PROTEIN)),
        FoodItem("tamagoyaki", "Tamagoyaki (rolled omelette)", "3 eggs", 250, 18.0, 6.0, 17.0, setOf(B), tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("veg_bao", "Vegetable bao buns", "2 buns", 280, 8.0, 46.0, 6.0, setOf(B, S)),
        FoodItem("jianbing", "Jianbing (egg crepe)", "1 crepe", 420, 14.0, 55.0, 16.0, setOf(B)),
        FoodItem("kaya_toast", "Kaya toast with soft eggs", "2 slices, 2 eggs", 420, 16.0, 42.0, 21.0, setOf(B), tags = setOf(FoodTag.INDULGENT)),
        FoodItem("nasi_lemak", "Nasi lemak", "1 plate", 600, 18.0, 70.0, 28.0, setOf(B, L), vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("onigiri", "Tuna onigiri", "1 rice ball", 200, 8.0, 38.0, 2.0, setOf(B, S), vegetarian = false, tags = setOf(FoodTag.QUICK)),
        FoodItem("soy_milk", "Soy milk", "1 glass", 100, 7.0, 8.0, 4.0, setOf(B, S), FoodRole.DRINK),
        FoodItem("matcha_latte", "Matcha latte", "1 cup", 150, 7.0, 20.0, 5.0, setOf(B, S), FoodRole.DRINK),

        FoodItem("chicken_stir_fry", "Chicken and vegetable stir-fry", "1 plate", 350, 32.0, 15.0, 17.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.QUICK)),
        FoodItem("tofu_stir_fry", "Tofu and vegetable stir-fry", "1 plate", 300, 18.0, 15.0, 18.0, LD, tags = setOf(FoodTag.QUICK, FoodTag.BUDGET)),
        FoodItem("teriyaki_salmon", "Teriyaki salmon", "150 g", 350, 34.0, 12.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("poke_bowl", "Salmon sushi bowl", "1 bowl", 550, 30.0, 65.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.HIGH_PROTEIN)),
        FoodItem("sushi_rolls", "Sushi rolls", "8 pieces", 400, 16.0, 60.0, 10.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("chicken_ramen", "Chicken ramen", "1 bowl", 550, 30.0, 65.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("tonkotsu_ramen", "Tonkotsu (pork) ramen", "1 bowl", 700, 30.0, 70.0, 32.0, LD, vegetarian = false, halal = false, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("bibimbap", "Bibimbap", "1 bowl", 560, 25.0, 80.0, 15.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("kimchi_fried_rice", "Kimchi fried rice with egg", "1 plate", 480, 14.0, 70.0, 15.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.BUDGET)),
        FoodItem("pad_thai", "Chicken pad thai", "1 plate", 600, 25.0, 75.0, 22.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("beef_broccoli", "Beef and broccoli", "1 plate", 330, 30.0, 15.0, 16.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("kung_pao", "Kung pao chicken", "1 plate", 430, 30.0, 18.0, 26.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("green_curry", "Thai green curry with chicken", "1 bowl", 450, 28.0, 12.0, 32.0, LD, vegetarian = false),
        FoodItem("pho", "Beef pho", "1 bowl", 450, 30.0, 55.0, 10.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("veg_dumplings", "Steamed vegetable dumplings", "8 dumplings", 330, 10.0, 52.0, 8.0, setOf(L, D, S)),
        FoodItem("chicken_dumplings", "Steamed chicken dumplings", "8 dumplings", 400, 22.0, 45.0, 14.0, LD, vegetarian = false),

        FoodItem("jasmine_rice", "Steamed jasmine rice", "1 cup", 205, 4.3, 45.0, 0.4, LD, FoodRole.SIDE_CARB),
        FoodItem("egg_noodles", "Egg noodles", "1 cup", 220, 7.0, 40.0, 3.3, LD, FoodRole.SIDE_CARB),
        FoodItem("miso_soup", "Miso soup", "1 bowl", 40, 3.0, 5.0, 1.5, LD, FoodRole.SIDE, vegetarian = false, tags = setOf(FoodTag.LIGHT)),
        FoodItem("bok_choy", "Stir-fried bok choy", "1 cup", 70, 3.0, 6.0, 4.5, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("kimchi", "Kimchi", "½ cup", 20, 1.0, 4.0, 0.5, LD, FoodRole.SIDE, vegetarian = false, tags = setOf(FoodTag.LIGHT)),
        FoodItem("seaweed_salad", "Seaweed salad", "1 small bowl", 70, 1.0, 8.0, 4.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("edamame", "Edamame", "1 cup", 190, 17.0, 15.0, 8.0, setOf(S), tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("spring_rolls", "Spring rolls", "2 rolls", 200, 4.0, 22.0, 10.0, setOf(S), tags = setOf(FoodTag.FRIED)),
        FoodItem("mochi", "Mochi", "1 piece", 100, 1.5, 22.0, 0.5, setOf(S)),
        FoodItem("bubble_tea", "Bubble tea", "1 cup (500 ml)", 300, 2.0, 60.0, 6.0, setOf(S), FoodRole.DRINK, tags = setOf(FoodTag.INDULGENT)),
    )

    // ---------------------------------------------------------------- Mexico, Central and South America
    private val latinAmerican = cuisine(
        Cuisine.LATIN_AMERICAN,
        FoodItem("chilaquiles", "Chilaquiles with egg", "1 plate", 500, 18.0, 50.0, 26.0, setOf(B), tags = setOf(FoodTag.FRIED)),
        FoodItem("huevos_rancheros", "Huevos rancheros", "1 plate", 450, 20.0, 40.0, 23.0, setOf(B), tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("arepa_queso", "Arepa with cheese", "1 arepa", 350, 11.0, 40.0, 16.0, setOf(B, S)),
        FoodItem("breakfast_burrito", "Egg and bean breakfast burrito", "1 burrito", 450, 20.0, 50.0, 18.0, setOf(B), tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.COMPLETE)),
        FoodItem("acai_bowl", "Açaí bowl", "1 bowl", 400, 6.0, 65.0, 14.0, setOf(B, S)),
        FoodItem("pao_de_queijo", "Pão de queijo (cheese bread)", "3 pieces", 200, 5.0, 25.0, 9.0, setOf(B, S)),
        FoodItem("cafe_con_leche", "Café con leche", "1 cup", 90, 4.0, 9.0, 4.0, setOf(B, S), FoodRole.DRINK),
        FoodItem("horchata", "Horchata", "1 glass", 180, 1.0, 38.0, 2.0, setOf(S, L), FoodRole.DRINK, tags = setOf(FoodTag.INDULGENT)),

        FoodItem("burrito_bowl", "Chicken burrito bowl", "1 bowl", 650, 40.0, 70.0, 22.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.HIGH_PROTEIN)),
        FoodItem("bean_burrito_bowl", "Bean and veggie burrito bowl", "1 bowl", 550, 18.0, 85.0, 15.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.BUDGET)),
        FoodItem("chicken_tacos", "Chicken tacos", "3 tacos", 500, 30.0, 45.0, 21.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("fish_tacos", "Fish tacos", "3 tacos", 480, 26.0, 48.0, 20.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("tacos_al_pastor", "Tacos al pastor (pork)", "3 tacos", 520, 27.0, 45.0, 25.0, LD, vegetarian = false, halal = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("black_bean_soup", "Black bean soup", "1 bowl", 250, 14.0, 40.0, 4.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.LIGHT)),
        FoodItem("fajitas", "Chicken fajitas (no tortillas)", "1 plate", 380, 35.0, 15.0, 20.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("quesadilla", "Cheese quesadilla", "1 quesadilla", 500, 20.0, 40.0, 28.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("ceviche", "Ceviche", "1 cup", 200, 28.0, 12.0, 4.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("arroz_con_pollo", "Arroz con pollo", "1 plate", 550, 32.0, 60.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("enchiladas", "Chicken enchiladas", "2 enchiladas", 550, 32.0, 40.0, 28.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("feijoada", "Feijoada (pork and bean stew)", "1 bowl", 600, 35.0, 40.0, 32.0, LD, vegetarian = false, halal = false, tags = setOf(FoodTag.INDULGENT)),
        FoodItem("lomo_saltado", "Lomo saltado", "1 plate", 600, 35.0, 55.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.FRIED)),
        FoodItem("veg_chili", "Vegetarian bean chili", "1 bowl", 300, 15.0, 45.0, 7.0, LD, tags = setOf(FoodTag.BUDGET, FoodTag.HIGH_PROTEIN)),

        FoodItem("corn_tortillas", "Corn tortillas", "2 tortillas", 110, 3.0, 23.0, 1.4, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.BUDGET)),
        FoodItem("mexican_rice", "Mexican rice", "1 cup", 260, 5.0, 47.0, 6.0, LD, FoodRole.SIDE_CARB),
        FoodItem("fried_plantains", "Fried plantains", "1 cup", 360, 2.0, 58.0, 14.0, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.FRIED)),
        FoodItem("black_beans", "Black beans", "½ cup", 115, 7.6, 20.0, 0.5, LD, FoodRole.SIDE, tags = setOf(FoodTag.BUDGET)),
        FoodItem("guacamole", "Guacamole", "¼ cup", 100, 1.0, 5.0, 9.0, setOf(L, D, S), FoodRole.SIDE),
        FoodItem("pico_de_gallo", "Pico de gallo", "½ cup", 20, 1.0, 4.0, 0.1, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("elote", "Elote (street corn)", "1 cob", 220, 6.0, 27.0, 11.0, setOf(S)),
        FoodItem("chips_salsa", "Tortilla chips and salsa", "1 small bowl", 200, 3.0, 25.0, 10.0, setOf(S), tags = setOf(FoodTag.FRIED)),
        FoodItem("churros", "Churros", "2 churros", 230, 3.0, 26.0, 13.0, setOf(S), tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT)),
        FoodItem("mango_chili", "Mango with chili and lime", "1 cup", 100, 1.4, 25.0, 0.6, setOf(S), tags = setOf(FoodTag.LIGHT, FoodTag.QUICK)),
    )

    // ---------------------------------------------------------------- North America, UK, Europe
    private val americanEuropean = cuisine(
        Cuisine.AMERICAN_EUROPEAN,
        FoodItem("pancakes", "Pancakes with syrup", "3 pancakes", 520, 12.0, 90.0, 12.0, setOf(B), tags = setOf(FoodTag.INDULGENT)),
        FoodItem("french_toast", "French toast", "2 slices", 400, 14.0, 50.0, 16.0, setOf(B), tags = setOf(FoodTag.INDULGENT)),
        FoodItem("bacon_eggs", "Bacon and eggs", "2 eggs, 3 rashers", 400, 24.0, 2.0, 32.0, setOf(B), vegetarian = false, halal = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("eggs_turkey_sausage", "Eggs with chicken sausage", "2 eggs, 2 sausages", 350, 26.0, 3.0, 25.0, setOf(B), vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("full_english", "Full English breakfast", "1 plate", 850, 40.0, 50.0, 55.0, setOf(B), vegetarian = false, halal = false, tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT, FoodTag.COMPLETE)),
        FoodItem("muesli", "Muesli with milk", "1 bowl", 330, 11.0, 55.0, 7.0, setOf(B), tags = setOf(FoodTag.QUICK)),
        FoodItem("bagel_cream_cheese", "Bagel with cream cheese", "1 bagel", 360, 12.0, 56.0, 10.0, setOf(B), tags = setOf(FoodTag.QUICK)),
        FoodItem("croissant", "Croissant", "1 croissant", 230, 5.0, 26.0, 12.0, setOf(B, S), tags = setOf(FoodTag.INDULGENT)),
        FoodItem("tea_milk", "Black tea with milk", "1 cup", 30, 1.0, 3.0, 1.0, setOf(B, S), FoodRole.DRINK, tags = setOf(FoodTag.LIGHT)),
        FoodItem("hot_chocolate", "Hot chocolate", "1 mug", 190, 8.0, 27.0, 6.0, setOf(S), FoodRole.DRINK, tags = setOf(FoodTag.INDULGENT)),

        FoodItem("turkey_sandwich", "Turkey sandwich", "1 sandwich", 350, 25.0, 38.0, 10.0, LD, vegetarian = false, tags = setOf(FoodTag.QUICK, FoodTag.COMPLETE)),
        FoodItem("ham_sandwich", "Ham and cheese sandwich", "1 sandwich", 350, 20.0, 38.0, 12.0, LD, vegetarian = false, halal = false, tags = setOf(FoodTag.QUICK, FoodTag.COMPLETE)),
        FoodItem("cheeseburger", "Cheeseburger", "1 burger", 550, 30.0, 40.0, 29.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("veggie_burger", "Veggie burger", "1 burger", 450, 20.0, 50.0, 18.0, LD, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("steak", "Grilled steak", "200 g", 450, 50.0, 0.0, 27.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("roast_chicken", "Roast chicken leg", "1 quarter", 330, 30.0, 0.0, 22.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("caesar_salad", "Chicken Caesar salad", "1 bowl", 450, 35.0, 15.0, 28.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("mac_cheese", "Mac and cheese", "1 cup", 500, 20.0, 50.0, 24.0, LD, tags = setOf(FoodTag.COMPLETE, FoodTag.INDULGENT)),
        FoodItem("fish_chips", "Fish and chips", "1 portion", 850, 35.0, 80.0, 42.0, LD, vegetarian = false, tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT, FoodTag.COMPLETE)),
        FoodItem("beef_stew", "Beef stew with potatoes", "1 bowl", 400, 30.0, 30.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.COMPLETE, FoodTag.HIGH_PROTEIN)),
        FoodItem("chili_con_carne", "Chili con carne", "1 bowl", 400, 28.0, 30.0, 18.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN)),
        FoodItem("chicken_schnitzel", "Chicken schnitzel", "1 schnitzel", 450, 35.0, 20.0, 25.0, LD, vegetarian = false, tags = setOf(FoodTag.FRIED)),
        FoodItem("tomato_soup", "Tomato soup", "1 bowl", 160, 4.0, 25.0, 5.0, LD, tags = setOf(FoodTag.LIGHT, FoodTag.BUDGET)),
        FoodItem("pierogi", "Potato and cheese pierogi", "6 pierogi", 450, 12.0, 65.0, 15.0, LD, tags = setOf(FoodTag.COMPLETE)),
        FoodItem("baked_cod", "Baked cod", "200 g", 210, 46.0, 0.0, 2.0, LD, vegetarian = false, tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),

        FoodItem("mashed_potatoes", "Mashed potatoes", "1 cup", 240, 4.0, 35.0, 9.0, LD, FoodRole.SIDE_CARB),
        FoodItem("dinner_roll", "Dinner roll", "1 roll", 110, 3.5, 19.0, 2.5, LD, FoodRole.SIDE_CARB),
        FoodItem("sweet_potato", "Baked sweet potato", "1 large", 160, 3.6, 37.0, 0.3, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.LIGHT)),
        FoodItem("french_fries", "French fries", "1 medium portion", 365, 4.0, 48.0, 17.0, LD, FoodRole.SIDE_CARB, tags = setOf(FoodTag.FRIED, FoodTag.INDULGENT)),
        FoodItem("coleslaw", "Coleslaw", "½ cup", 150, 1.0, 13.0, 11.0, LD, FoodRole.SIDE),
        FoodItem("side_salad", "Side salad with vinaigrette", "1 bowl", 90, 1.5, 6.0, 7.0, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),
        FoodItem("roasted_broccoli", "Roasted broccoli", "1 cup", 90, 4.0, 10.0, 4.5, LD, FoodRole.SIDE, tags = setOf(FoodTag.LIGHT)),

        FoodItem("cottage_cheese", "Cottage cheese", "½ cup", 110, 12.0, 5.0, 5.0, setOf(B, S), tags = setOf(FoodTag.HIGH_PROTEIN, FoodTag.LIGHT)),
        FoodItem("apple_pb", "Apple with peanut butter", "1 apple, 2 tbsp", 270, 7.0, 28.0, 16.0, setOf(S)),
        FoodItem("trail_mix", "Trail mix", "30 g", 140, 4.0, 13.0, 9.0, setOf(S)),
        FoodItem("cheese_crackers", "Cheese and crackers", "1 small plate", 200, 8.0, 15.0, 12.0, setOf(S)),
        FoodItem("granola_bar", "Granola bar", "1 bar", 190, 4.0, 29.0, 7.0, setOf(S), tags = setOf(FoodTag.QUICK)),
        FoodItem("potato_chips", "Potato chips", "1 bag (28 g)", 150, 2.0, 15.0, 10.0, setOf(S), tags = setOf(FoodTag.FRIED)),
        FoodItem("cookies", "Chocolate chip cookies", "2 cookies", 160, 2.0, 22.0, 8.0, setOf(S), tags = setOf(FoodTag.INDULGENT)),
    )

    val all: List<FoodItem> = global + southAsian + middleEastern + mediterranean + eastAsian + latinAmerican + americanEuropean

    private val byId = all.associateBy { it.id }
    fun byId(id: String): FoodItem? = byId[id]

    fun of(cuisine: Cuisine): List<FoodItem> = all.filter { it.cuisine == cuisine }

    /**
     * Searches every cuisine (name or id). Foods from [preferred] cuisines come first, then everyday staples, then
     * the rest; non-halal dishes are hidden when [halalOnly] is set.
     */
    fun search(query: String, preferred: Set<Cuisine> = emptySet(), halalOnly: Boolean = false): List<FoodItem> {
        val q = query.trim().lowercase()
        val hits = all.filter { (!halalOnly || it.halal) && (q.isEmpty() || it.name.lowercase().contains(q) || it.id.contains(q)) }
        return hits.sortedBy { f ->
            when {
                f.cuisine in preferred -> 0
                f.cuisine == Cuisine.GLOBAL -> 1
                else -> 2
            }
        }
    }
}
