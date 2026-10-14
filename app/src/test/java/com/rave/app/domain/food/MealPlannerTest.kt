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
    fun `ramadan plans use suhoor and iftar with dates first`() {
        val week = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000, ramadan = true))
        week.forEach { d ->
            assertEquals(listOf(MealSlot.SUHOOR, MealSlot.IFTAR, MealSlot.DINNER), d.meals.map { it.slot })
            assertEquals("dates", d.meals[1].items.first().food.id)
        }
    }

    @Test
    fun `plan is stable for a date`() {
        val a = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000))
        val b = MealPlanner.week(monday, PlannerOptions(dailyKcal = 2000))
        assertEquals(a, b)
    }
}
