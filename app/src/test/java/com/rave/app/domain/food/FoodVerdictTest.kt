package com.rave.app.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodVerdictTest {
    private val prefs = FoodPreferences()

    @Test
    fun `sugary ultra processed snack is a skip`() {
        val chocolateBiscuit = ScannedFood(
            name = "Chocolate cream biscuits",
            per100g = Nutrients(kcal = 495.0, sugar = 34.0, fat = 22.0, satFat = 11.0, salt = 0.6, protein = 6.0),
            novaGroup = 4, nutriScore = 'E',
        )
        val a = FoodVerdictEngine.assess(chocolateBiscuit, prefs)
        assertEquals(Verdict.SKIP, a.verdict)
        assertTrue(a.reasons.any { it.type == ReasonType.HIGH_SUGAR })
        assertTrue(a.swaps.isNotEmpty())
    }

    @Test
    fun `plain yogurt is a go`() {
        val yogurt = ScannedFood(name = "Plain yogurt", per100g = Nutrients(kcal = 61.0, sugar = 4.7, fat = 3.3, satFat = 2.1, salt = 0.1, protein = 3.5), novaGroup = 1, nutriScore = 'A')
        assertEquals(Verdict.GO, FoodVerdictEngine.assess(yogurt, prefs).verdict)
    }

    @Test
    fun `drinks use stricter sugar limits`() {
        val cola = ScannedFood(name = "Cola", per100g = Nutrients(kcal = 42.0, sugar = 10.6), isDrink = true, novaGroup = 4)
        val a = FoodVerdictEngine.assess(cola, prefs)
        assertTrue(a.verdict != Verdict.GO)
        assertEquals(listOf(Swap.LEMON_WATER, Swap.PLAIN_YOGURT), a.swaps)
    }

    @Test
    fun `your allergen forces a skip`() {
        val bar = ScannedFood(name = "Peanut bar", per100g = Nutrients(kcal = 300.0, sugar = 4.0, protein = 20.0), allergenTags = listOf("en:peanuts"))
        val a = FoodVerdictEngine.assess(bar, prefs.copy(allergies = setOf(Allergen.PEANUTS)))
        assertEquals(Verdict.SKIP, a.verdict)
        assertEquals(ReasonType.CONTAINS_YOUR_ALLERGEN, a.reasons.first().type)
    }

    @Test
    fun `pork is not halal and gelatin asks to check`() {
        val sausage = ScannedFood(name = "Sausage", ingredients = "Pork (80%), water, salt", per100g = Nutrients(kcal = 250.0))
        assertEquals(Verdict.SKIP, FoodVerdictEngine.assess(sausage, prefs).verdict)
        val gummies = ScannedFood(name = "Gummies", ingredients = "Glucose syrup, sugar, gelatine, citric acid", per100g = Nutrients(kcal = 340.0, sugar = 46.0))
        assertTrue(FoodVerdictEngine.assess(gummies, prefs).warnings.any { it.type == WarningType.HALAL_CHECK })
        val certified = gummies.copy(labelTags = listOf("en:halal"))
        assertTrue(FoodVerdictEngine.assess(certified, prefs).warnings.any { it.type == WarningType.HALAL_CERTIFIED })
    }

    @Test
    fun `high protein helps when gaining`() {
        val whey = ScannedFood(name = "Protein bar", per100g = Nutrients(kcal = 380.0, sugar = 4.0, protein = 30.0, fat = 12.0, satFat = 6.0))
        val gain = FoodVerdictEngine.assess(whey, prefs.copy(goal = FoodGoal.GAIN))
        assertTrue(gain.reasons.any { it.type == ReasonType.GOOD_PROTEIN })
        assertTrue(gain.score < FoodVerdictEngine.assess(whey, prefs.copy(goal = FoodGoal.LOSE)).score)
    }

    @Test
    fun `daily limit share uses the portion`() {
        val juice = ScannedFood(name = "Mango juice", per100g = Nutrients(sugar = 12.0), servingGrams = 250.0, isDrink = true)
        assertEquals(0.6, FoodVerdictEngine.assess(juice, prefs).sugarOfDailyLimit!!, 1e-9)
    }
}
