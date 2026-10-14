package com.rave.app.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieAndLabelTest {

    @Test
    fun `mifflin st jeor bmr`() {
        assertEquals(1648.75, CalorieTarget.bmr(70f, 175f, 30, Sex.MALE), 1e-6)
        assertEquals(1482.75, CalorieTarget.bmr(70f, 175f, 30, Sex.FEMALE), 1e-6)
    }

    @Test
    fun `goal changes the target and macros add up`() {
        val keep = CalorieTarget.target(70f, 175f, 30, Sex.MALE, 6_000, FoodGoal.MAINTAIN)
        val lose = CalorieTarget.target(70f, 175f, 30, Sex.MALE, 6_000, FoodGoal.LOSE)
        val gain = CalorieTarget.target(70f, 175f, 30, Sex.MALE, 6_000, FoodGoal.GAIN)
        assertTrue(lose.kcal < keep.kcal && keep.kcal < gain.kcal)
        assertEquals(2390, keep.kcal) // 1648.75 × 1.45 ≈ 2391
        val macroKcal = keep.proteinG * 4 + keep.carbsG * 4 + keep.fatG * 9
        assertTrue(kotlin.math.abs(macroKcal - keep.kcal) < 40)
    }

    @Test
    fun `lose target never drops below a safe floor`() {
        val t = CalorieTarget.target(45f, 150f, 60, Sex.FEMALE, 2_000, FoodGoal.LOSE)
        assertTrue(t.kcal >= 1200)
    }

    @Test
    fun `parses a typical label`() {
        val text = """
            NUTRITION INFORMATION
            Typical values per 100g
            Energy 2100kJ / 502kcal
            Fat 26.0g
            of which saturates 12.5g
            Carbohydrate 60.1g
            of which sugars 31.2g
            Fibre 2.1g
            Protein 6.3g
            Salt 0.55g
        """.trimIndent()
        val r = NutritionLabelParser.parse(text)
        assertTrue(r.per100)
        with(r.nutrients) {
            assertEquals(502.0, kcal!!, 1e-9)
            assertEquals(26.0, fat!!, 1e-9)
            assertEquals(12.5, satFat!!, 1e-9)
            assertEquals(31.2, sugar!!, 1e-9)
            assertEquals(60.1, carbs!!, 1e-9)
            assertEquals(6.3, protein!!, 1e-9)
            assertEquals(2.1, fiber!!, 1e-9)
            assertEquals(0.55, salt!!, 1e-9)
        }
    }

    @Test
    fun `sodium in mg becomes salt and kj only is converted`() {
        val r = NutritionLabelParser.parse("Energy 836 kJ\nTotal Fat: 5 g\nSodium 400mg\nProtein\n7g")
        assertEquals(836 / 4.184, r.nutrients.kcal!!, 1e-6)
        assertEquals(1.0, r.nutrients.salt!!, 1e-9)
        assertEquals(7.0, r.nutrients.protein!!, 1e-9)
        assertEquals(5.0, r.nutrients.fat!!, 1e-9)
    }
}
