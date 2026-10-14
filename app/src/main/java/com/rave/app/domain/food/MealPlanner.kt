package com.rave.app.domain.food

import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

data class PlanItem(val food: DesiFood, val servings: Int = 1) {
    val kcal: Int get() = food.kcal * servings
    val protein: Double get() = food.protein * servings
}

data class PlannedMeal(val slot: MealSlot, val items: List<PlanItem>) {
    val kcal: Int get() = items.sumOf { it.kcal }
}

data class DayPlan(val date: LocalDate, val meals: List<PlannedMeal>) {
    val kcal: Int get() = meals.sumOf { it.kcal }
    val protein: Double get() = meals.sumOf { m -> m.items.sumOf { it.protein } }
}

data class PlannerOptions(
    val dailyKcal: Int,
    val vegetarian: Boolean = false,
    val ramadan: Boolean = false,
    val goal: FoodGoal = FoodGoal.MAINTAIN,
    val dislikedIds: Set<String> = emptySet(),
    val budget: Boolean = false,
)

/**
 * Builds a week of desi meals that add up to the daily calorie target. Deterministic for a given
 * start date (so a plan doesn't reshuffle on every open); "regenerate" passes a new seed.
 * Lunch and dinner = a main + roti/rice + salad/raita as needed; mains don't repeat on back-to-back
 * days; fried / indulgent food is limited (none when losing weight, once a week otherwise).
 */
object MealPlanner {
    private val normalSplit = listOf(MealSlot.BREAKFAST to 0.25, MealSlot.LUNCH to 0.35, MealSlot.DINNER to 0.30, MealSlot.SNACK to 0.10)
    private val ramadanSplit = listOf(MealSlot.SUHOOR to 0.40, MealSlot.IFTAR to 0.25, MealSlot.DINNER to 0.35)

    fun week(start: LocalDate, options: PlannerOptions, seed: Long = start.toEpochDay()): List<DayPlan> {
        val random = Random(seed)
        val recentMains = ArrayDeque<String>()
        var indulgentLeft = if (options.goal == FoodGoal.LOSE) 0 else 1
        return (0 until 7).map { d ->
            val split = if (options.ramadan) ramadanSplit else normalSplit
            val meals = split.map { (slot, share) ->
                val budget = (options.dailyKcal * share).toInt()
                val meal = buildMeal(slot, budget, options, random, recentMains, allowIndulgent = indulgentLeft > 0)
                if (meal.items.any { FoodTag.INDULGENT in it.food.tags || FoodTag.FRIED in it.food.tags }) indulgentLeft--
                meal
            }
            DayPlan(start.plusDays(d.toLong()), meals)
        }
    }

    private fun candidates(slot: MealSlot, options: PlannerOptions, allowIndulgent: Boolean) = DesiFoods.all.filter { f ->
        slot in f.slots && f.id !in options.dislikedIds &&
            (!options.vegetarian || f.vegetarian) &&
            (allowIndulgent || (FoodTag.INDULGENT !in f.tags && FoodTag.FRIED !in f.tags)) &&
            (!options.budget || FoodTag.BUDGET in f.tags || f.role != FoodRole.MAIN || FoodTag.LIGHT in f.tags)
    }

    private fun buildMeal(
        slot: MealSlot, budget: Int, options: PlannerOptions, random: Random,
        recentMains: ArrayDeque<String>, allowIndulgent: Boolean,
    ): PlannedMeal {
        val pool = candidates(slot, options, allowIndulgent).ifEmpty { candidates(slot, options.copy(budget = false), true) }
        val items = mutableListOf<PlanItem>()
        fun total() = items.sumOf { it.kcal }

        when (slot) {
            MealSlot.LUNCH, MealSlot.DINNER -> {
                val mains = pool.filter { it.role == FoodRole.MAIN && it.id !in recentMains && it.kcal <= budget }
                    .ifEmpty { pool.filter { it.role == FoodRole.MAIN } }
                val main = mains.filter { options.goal != FoodGoal.GAIN || FoodTag.HIGH_PROTEIN in it.tags }.ifEmpty { mains }.random(random)
                items += PlanItem(main)
                recentMains.addLast(main.id)
                while (recentMains.size > 3) recentMains.removeFirst()
                if (FoodTag.COMPLETE !in main.tags) {
                    val carbs = pool.filter { it.role == FoodRole.SIDE_CARB }
                    val roti = carbs.firstOrNull { it.id == "roti" }
                    val rice = carbs.filter { it.id != "roti" }.randomOrNull(random)
                    val gap = budget - total()
                    if (roti != null && (rice == null || random.nextInt(3) != 0)) {
                        items += PlanItem(roti, (gap / roti.kcal).coerceIn(1, 3))
                    } else if (rice != null) {
                        items += PlanItem(rice)
                    }
                }
                val side = pool.filter { it.role == FoodRole.SIDE }.randomOrNull(random)
                if (side != null && budget - total() >= side.kcal / 2) items += PlanItem(side)
            }
            MealSlot.SNACK -> {
                val snacks = pool.filter { it.role != FoodRole.SIDE_CARB }
                val light = if (options.goal == FoodGoal.LOSE) snacks.filter { FoodTag.LIGHT in it.tags }.ifEmpty { snacks } else snacks
                light.sortedBy { abs(it.kcal - budget) }.take(4).randomOrNull(random)?.let { items += PlanItem(it) }
            }
            else -> {
                // Breakfast / suhoor / iftar: a main item, then a drink or fruit to reach the budget.
                val mains = pool.filter { it.role == FoodRole.MAIN && it.kcal <= budget }.ifEmpty { pool.filter { it.role == FoodRole.MAIN } }
                val first = if (slot == MealSlot.IFTAR) pool.firstOrNull { it.id == "dates" } ?: mains.random(random) else mains.random(random)
                items += PlanItem(first)
                var guard = 0
                while (budget - total() > 80 && guard++ < 3) {
                    val gap = budget - total()
                    val extra = pool.filter { e -> items.none { it.food.id == e.id } && e.kcal <= gap + 40 }
                        .sortedBy { abs(it.kcal - gap) }.take(3).randomOrNull(random) ?: break
                    items += PlanItem(extra)
                }
            }
        }
        return PlannedMeal(slot, items)
    }

    /** Shopping list for the plan: each grocery with how many meals use it, most used first. */
    fun groceries(plan: List<DayPlan>): List<Pair<String, Int>> =
        plan.flatMap { d -> d.meals.flatMap { m -> m.items.flatMap { it.food.groceries } } }
            .groupingBy { it }.eachCount().toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
}
