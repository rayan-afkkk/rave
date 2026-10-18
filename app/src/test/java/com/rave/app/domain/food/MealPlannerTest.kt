package com.rave.app.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealPlannerTest {
    private val monday = LocalDate.of(2026, 10, 12)

    @Test
    fun `every day lands near the calorie target`() {
        for (target in listOf(1600, 2000, 2600)) {
            val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = target))
            assertEquals(7, week.size)
            week.forEach { day ->
                val off = kotlin.math.abs(day.kcal - target) / target.toDouble()
                assertTrue("day ${day.date} has ${day.kcal} kcal for $target", off < 0.25)
            }
        }
    }

    @Test
    fun `vegetarian plans contain no meat`() {
        val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000, vegetarian = true))
        assertTrue(week.all { d -> d.meals.all { m -> m.items.all { it.food.vegetarian } } })
    }

    @Test
    fun `mains do not repeat on consecutive days`() {
        val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2200))
        val lunchDinnerMains = week.map { d ->
            d.meals.filter { it.slot == MealSlot.LUNCH || it.slot == MealSlot.DINNER }.map { it.items.first().food.id }.toSet()
        }
        lunchDinnerMains.zipWithNext().forEach { (a, b) -> assertTrue("repeat $a / $b", (a intersect b).isEmpty()) }
    }

    @Test
    fun `losing weight plans skip fried and indulgent food`() {
        val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = 1700, goal = FoodGoal.LOSE))
        assertTrue(week.all { d -> d.meals.all { m -> m.items.none { FoodTag.FRIED in it.food.tags || FoodTag.INDULGENT in it.food.tags } } })
    }

    @Test
    fun `plan is stable for a date`() {
        val a = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000))
        val b = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000))
        assertEquals(a, b)
    }

    private fun assertCompleteWeek(week: List<DayPlan>, target: Int, label: String) {
        assertEquals(7, week.size)
        week.forEach { day ->
            assertEquals("$label ${day.date}: meals", MealSlot.entries.toList(), day.meals.map { it.slot })
            day.meals.forEach { m -> assertTrue("$label ${day.date} ${m.slot} is empty", m.items.isNotEmpty()) }
            day.meals.filter { it.slot == MealSlot.LUNCH || it.slot == MealSlot.DINNER }.forEach { m ->
                assertEquals("$label ${day.date} ${m.slot} starts with a main", FoodRole.MAIN, m.items.first().food.role)
            }
            val off = kotlin.math.abs(day.kcal - target) / target.toDouble()
            assertTrue("$label ${day.date} has ${day.kcal} kcal for $target", off < 0.3)
        }
    }

    @Test
    fun `every single cuisine builds complete days`() {
        for (cuisine in Cuisine.entries) for (target in listOf(1600, 2200, 2800)) {
            val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = target, cuisines = setOf(cuisine)))
            assertCompleteWeek(week, target, "$cuisine/$target")
            val mains = week.flatMap { d -> d.meals.filter { it.slot == MealSlot.LUNCH || it.slot == MealSlot.DINNER }.map { it.items.first().food } }
            assertTrue("$cuisine mains come from that cuisine", mains.all { it.cuisine == cuisine })
        }
    }

    @Test
    fun `vegetarian halal plans work for every cuisine and goal`() {
        for (cuisine in Cuisine.entries) for (goal in FoodGoal.entries) for (seed in 0L until 5L) {
            val week = MealPlanner.week(
                monday,
                PlannerOptions(dailyKcal = 2000, vegetarian = true, halalOnly = true, goal = goal, cuisines = setOf(cuisine)),
                seed,
            )
            assertCompleteWeek(week, 2000, "$cuisine/$goal/$seed")
            assertTrue(week.all { d -> d.meals.all { m -> m.items.all { it.food.vegetarian && it.food.halal } } })
        }
    }

    @Test
    fun `halal plans skip pork and alcohol dishes`() {
        for (seed in 0L until 20L) {
            val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2400, halalOnly = true, cuisines = Cuisine.entries.toSet()), seed)
            assertTrue(week.all { d -> d.meals.all { m -> m.items.all { it.food.halal } } })
        }
    }

    @Test
    fun `empty cuisine selection falls back to staples`() {
        val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000, cuisines = emptySet()))
        assertCompleteWeek(week, 2000, "empty")
    }

    @Test
    fun `catalog ids are unique and every cuisine covers every slot and role`() {
        assertEquals(FoodCatalog.all.size, FoodCatalog.all.map { it.id }.toSet().size)
        for (cuisine in Cuisine.entries) {
            val foods = FoodCatalog.of(cuisine)
            for (slot in MealSlot.entries) assertTrue("$cuisine has $slot food", foods.any { slot in it.slots })
            for (role in listOf(FoodRole.MAIN, FoodRole.SIDE_CARB, FoodRole.SIDE)) {
                assertTrue("$cuisine has a lunch/dinner $role", foods.any { it.role == role && MealSlot.DINNER in it.slots })
            }
            assertTrue("$cuisine has a vegetarian main", foods.any { it.role == FoodRole.MAIN && it.vegetarian && MealSlot.DINNER in it.slots })
        }
        // Macros roughly add up to the stated calories (4/4/9 kcal per g, ±35% for fibre, alcohol and rounding).
        FoodCatalog.all.filter { it.kcal >= 50 }.forEach { f ->
            val est = f.protein * 4 + f.carbs * 4 + f.fat * 9
            assertTrue("${f.id}: ${f.kcal} kcal vs macros $est", kotlin.math.abs(est - f.kcal) / f.kcal < 0.35)
        }
    }

    @Test
    fun `search ranks preferred cuisines first and hides non-halal when asked`() {
        val r = FoodCatalog.search("", preferred = setOf(Cuisine.EAST_ASIAN))
        assertEquals(Cuisine.EAST_ASIAN, r.first().cuisine)
        assertEquals(FoodCatalog.all.size, r.size)
        assertTrue(FoodCatalog.search("", halalOnly = true).all { it.halal })
        assertTrue(FoodCatalog.search("taco").isNotEmpty())
    }
}
