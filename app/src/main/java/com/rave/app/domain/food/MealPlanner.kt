package com.rave.app.domain.food

import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

data class PlanItem(val food: FoodItem, val servings: Int = 1) {
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
    val goal: FoodGoal = FoodGoal.MAINTAIN,
    val dislikedIds: Set<String> = emptySet(),
    val budget: Boolean = false,
    /** Skip dishes built around pork or alcohol. */
    val halalOnly: Boolean = false,
    /** Cuisines to plan from; empty means everyday staples ([Cuisine.GLOBAL]). */
    val cuisines: Set<Cuisine> = setOf(Cuisine.GLOBAL),
)

/**
 * Builds a week of meals from the chosen cuisines that add up to the daily calorie target. Deterministic for a
 * given start date (so a plan doesn't reshuffle on every open); "regenerate" passes a new seed.
 * Lunch and dinner = a main + a matching bread/rice + a side as needed; mains don't repeat on back-to-back
 * days; fried / indulgent food is limited (none when losing weight, once a week otherwise).
 * Any slot or role the chosen cuisines can't fill falls back to everyday staples ([Cuisine.GLOBAL]), then to
 * every cuisine; snacks and breakfast drinks/fruit always include the staples too.
 */
object MealPlanner {
    private val normalSplit = listOf(MealSlot.BREAKFAST to 0.25, MealSlot.LUNCH to 0.35, MealSlot.DINNER to 0.30, MealSlot.SNACK to 0.10)

    fun week(start: LocalDate, options: PlannerOptions, seed: Long = start.toEpochDay()): List<DayPlan> {
        val random = Random(seed)
        val recentMains = ArrayDeque<String>()
        var indulgentLeft = if (options.goal == FoodGoal.LOSE) 0 else 1
        return (0 until 7).map { d ->
            val meals = normalSplit.map { (slot, share) ->
                val budget = (options.dailyKcal * share).toInt()
                val meal = buildMeal(slot, budget, options, random, recentMains, allowIndulgent = indulgentLeft > 0)
                if (meal.items.any { FoodTag.INDULGENT in it.food.tags || FoodTag.FRIED in it.food.tags }) indulgentLeft--
                meal
            }
            DayPlan(start.plusDays(d.toLong()), meals)
        }
    }

    private fun allowed(f: FoodItem, slot: MealSlot, options: PlannerOptions, allowIndulgent: Boolean) =
        slot in f.slots && f.id !in options.dislikedIds &&
            (!options.vegetarian || f.vegetarian) &&
            (!options.halalOnly || f.halal) &&
            (allowIndulgent || (FoodTag.INDULGENT !in f.tags && FoodTag.FRIED !in f.tags)) &&
            (!options.budget || FoodTag.BUDGET in f.tags || f.role != FoodRole.MAIN || FoodTag.LIGHT in f.tags)

    /** Candidate pools for one slot: the chosen cuisines, the everyday staples, and everything (last resort). */
    private class Pools(val chosen: List<FoodItem>, val global: List<FoodItem>, val any: List<FoodItem>) {
        /** Foods of [role] from the chosen cuisines, else staples, else anything. */
        fun role(role: FoodRole): List<FoodItem> =
            chosen.filter { it.role == role }.ifEmpty { global.filter { it.role == role } }.ifEmpty { any.filter { it.role == role } }

        /** Chosen cuisines plus staples (snacks, drinks, fruit). */
        val withStaples: List<FoodItem> get() = (chosen + global).distinctBy { it.id }
    }

    private fun pools(slot: MealSlot, options: PlannerOptions, allowIndulgent: Boolean): Pools {
        val cuisines = options.cuisines.ifEmpty { setOf(Cuisine.GLOBAL) }
        fun build(indulgent: Boolean, opts: PlannerOptions): Pools {
            val ok = FoodCatalog.all.filter { allowed(it, slot, opts, indulgent) }
            return Pools(ok.filter { it.cuisine in cuisines }, ok.filter { it.cuisine == Cuisine.GLOBAL }, ok)
        }
        val strict = build(allowIndulgent, options)
        return if (strict.any.isNotEmpty()) strict else build(true, options.copy(budget = false))
    }

    /** Prefer foods from the same cuisine as the main (roti with karahi, tortillas with fajitas). */
    private fun List<FoodItem>.matching(cuisine: Cuisine) = filter { it.cuisine == cuisine }.ifEmpty { this }

    private fun buildMeal(
        slot: MealSlot, budget: Int, options: PlannerOptions, random: Random,
        recentMains: ArrayDeque<String>, allowIndulgent: Boolean,
    ): PlannedMeal {
        val pools = pools(slot, options, allowIndulgent)
        val items = mutableListOf<PlanItem>()
        fun total() = items.sumOf { it.kcal }

        when (slot) {
            MealSlot.LUNCH, MealSlot.DINNER -> {
                val allMains = pools.role(FoodRole.MAIN)
                val mains = allMains.filter { it.id !in recentMains && it.kcal <= budget }
                    .ifEmpty { allMains.filter { it.id !in recentMains } }
                    .ifEmpty { allMains }
                val main = mains.filter { options.goal != FoodGoal.GAIN || FoodTag.HIGH_PROTEIN in it.tags }.ifEmpty { mains }.random(random)
                items += PlanItem(main)
                recentMains.addLast(main.id)
                while (recentMains.size > 3) recentMains.removeFirst()
                if (FoodTag.COMPLETE !in main.tags) {
                    val carb = pools.role(FoodRole.SIDE_CARB).matching(main.cuisine).randomOrNull(random)
                    val gap = budget - total()
                    if (carb != null && gap >= carb.kcal / 2) {
                        // Small pieces (roti, tortillas, rolls) scale to the gap; plates of rice/potatoes stay at one.
                        val max = when {
                            carb.kcal <= 130 -> 3
                            carb.kcal <= 170 -> 2
                            else -> 1
                        }
                        items += PlanItem(carb, (gap / carb.kcal).coerceIn(1, max))
                    }
                }
                val side = pools.role(FoodRole.SIDE).matching(main.cuisine).randomOrNull(random)
                if (side != null && budget - total() >= side.kcal / 2) items += PlanItem(side)
                // Big targets: a double portion of the main, then extra sides/bread until close to the budget.
                if (budget - total() >= main.kcal * 0.8) items[0] = PlanItem(main, 2)
                // At most one more bread/rice (if the main isn't a full plate and has none yet) or one more side.
                val hasCarb = items.any { it.food.role == FoodRole.SIDE_CARB }
                val extras = (if (hasCarb || FoodTag.COMPLETE in main.tags) emptyList() else pools.role(FoodRole.SIDE_CARB)) +
                    pools.role(FoodRole.SIDE)
                val gap = budget - total()
                if (gap > 120) {
                    extras.matching(main.cuisine).filter { e -> items.none { it.food.id == e.id } && e.kcal <= gap + 40 }
                        .sortedBy { abs(it.kcal - gap) }.take(3).randomOrNull(random)?.let { items += PlanItem(it) }
                }
            }
            MealSlot.SNACK -> {
                val snacks = pools.withStaples.filter { it.role != FoodRole.SIDE_CARB }.ifEmpty { pools.any }
                val light = if (options.goal == FoodGoal.LOSE) snacks.filter { FoodTag.LIGHT in it.tags }.ifEmpty { snacks } else snacks
                light.sortedBy { abs(it.kcal - budget) }.take(4).randomOrNull(random)?.let { items += PlanItem(it) }
            }
            else -> {
                // Breakfast: a main item from the chosen cuisines, then a drink or fruit to reach the budget.
                val allMains = pools.role(FoodRole.MAIN)
                val mains = allMains.filter { it.kcal in 150..budget }.ifEmpty { allMains.filter { it.kcal <= budget } }.ifEmpty { allMains }
                val first = mains.random(random)
                items += PlanItem(first)
                if (budget - total() >= first.kcal * 0.8 && first.kcal <= 250) items[0] = PlanItem(first, 2)
                // Fill up with drinks, fruit and other small items, not a second breakfast dish.
                val extras = pools.withStaples.filter { it.role != FoodRole.MAIN || (FoodTag.LIGHT in it.tags && it.kcal <= 160) }
                var guard = 0
                while (budget - total() > 80 && guard++ < 3) {
                    val gap = budget - total()
                    val extra = extras.filter { e -> items.none { it.food.id == e.id } && e.kcal <= gap + 40 }
                        .sortedBy { abs(it.kcal - gap) }.take(3).randomOrNull(random) ?: break
                    items += PlanItem(extra)
                }
            }
        }
        return PlannedMeal(slot, items)
    }
}
