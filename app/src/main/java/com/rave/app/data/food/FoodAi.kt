package com.rave.app.data.food

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.rave.app.data.cloud.FirebaseProvider
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.Nutrients
import com.rave.app.domain.food.ScannedFood
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recognises a plate of food from a photo with Gemini (Firebase AI Logic, Gemini Developer API)
 * and estimates its nutrition. Needs Firebase configured and AI Logic enabled in the console.
 */
@Singleton
class FoodAi @Inject constructor(private val firebase: FirebaseProvider) {

    val isAvailable: Boolean get() = firebase.isConfigured

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig { responseMimeType = "application/json" },
        )
    }

    /** Null if the photo doesn't show food. */
    suspend fun analyze(photo: Bitmap): ScannedFood? {
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        return parse(response.text ?: return null)
    }

    /** Reads a photographed nutrition label when on-device OCR couldn't. Null if no label is visible. */
    suspend fun readLabel(photo: Bitmap, name: String): ScannedFood? {
        val response = model.generateContent(content { image(photo); text(LABEL_PROMPT) })
        return parseLabel(response.text ?: return null, name)
    }

    companion object {
        const val MODEL = "gemini-2.5-flash"

        private val PROMPT = """
            You are a nutrition assistant for a fitness app used worldwide.
            Identify the food or drink in the photo (dishes from any cuisine, street food, snacks, packaged food)
            and estimate nutrition for the portion visible. Reply with JSON only:
            {"isFood": true, "name": "short English name", "portion": "e.g. 1 plate", "grams": 250,
             "kcal": 0, "protein": 0, "carbs": 0, "sugar": 0, "fat": 0, "satFat": 0, "fiber": 0, "salt": 0,
             "fried": false, "ultraProcessed": false, "drink": false, "containsPorkOrAlcohol": false,
             "ingredients": "main ingredients, comma separated"}
            Numbers are for the whole visible portion (grams, kcal). If it is not food, reply {"isFood": false}.
        """.trimIndent()

        private val LABEL_PROMPT = """
            Read the nutrition facts label in the photo. Reply with JSON only:
            {"isLabel": true, "name": "product name if printed, else empty", "per100": true,
             "servingGrams": 0, "kcal": 0, "protein": 0, "carbs": 0, "sugar": 0, "fat": 0, "satFat": 0,
             "fiber": 0, "salt": 0, "ingredients": "ingredients text if visible"}
            Use the per 100 g / 100 ml column when the label has one (per100 = true), otherwise the per serving
            column (per100 = false). Salt in grams; if only sodium is printed, salt = sodium × 2.5.
            Leave out any value that isn't printed. If there is no nutrition label, reply {"isLabel": false}.
        """.trimIndent()

        fun parseLabel(json: String, name: String): ScannedFood? {
            val o = runCatching { JSONObject(json.trim().removePrefix("```json").removeSuffix("```")) }.getOrNull() ?: return null
            if (!o.optBoolean("isLabel", false)) return null
            fun d(k: String) = if (o.has(k)) o.optDouble(k).takeIf { !it.isNaN() && it >= 0 } else null
            val n = Nutrients(d("kcal"), d("protein"), d("carbs"), d("sugar"), d("fat"), d("satFat"), d("fiber"), d("salt"))
            if (n.isEmpty) return null
            val per100 = o.optBoolean("per100", true)
            return ScannedFood(
                name = o.optString("name").ifBlank { name },
                per100g = if (per100) n else null,
                perServing = if (per100) null else n,
                servingGrams = d("servingGrams")?.takeIf { it > 0 },
                ingredients = o.optString("ingredients"),
                source = FoodSource.LABEL,
            )
        }

        fun parse(json: String): ScannedFood? {
            val o = runCatching { JSONObject(json.trim().removePrefix("```json").removeSuffix("```")) }.getOrNull() ?: return null
            if (!o.optBoolean("isFood", false)) return null
            fun d(k: String) = o.optDouble(k).takeIf { !it.isNaN() && it >= 0 }
            val grams = d("grams")?.takeIf { it > 0 }
            val portion = Nutrients(d("kcal"), d("protein"), d("carbs"), d("sugar"), d("fat"), d("satFat"), d("fiber"), d("salt"))
            val per100 = grams?.let { portion.scaled(100.0 / it) }
            val ingredients = o.optString("ingredients") + if (o.optBoolean("containsPorkOrAlcohol")) ", pork, alcohol" else ""
            return ScannedFood(
                name = o.optString("name").ifBlank { "Food" },
                per100g = per100,
                perServing = portion,
                servingGrams = grams,
                servingLabel = o.optString("portion"),
                isDrink = o.optBoolean("drink"),
                novaGroup = if (o.optBoolean("ultraProcessed")) 4 else null,
                ingredients = ingredients,
                categoryTags = if (o.optBoolean("fried")) listOf("fried", "snack") else emptyList(),
                source = FoodSource.PHOTO,
            )
        }
    }
}
