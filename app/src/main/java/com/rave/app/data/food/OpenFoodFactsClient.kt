package com.rave.app.data.food

import com.rave.app.BuildConfig
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.Nutrients
import com.rave.app.domain.food.ScannedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown when the barcode lookup can't reach Open Food Facts (offline, timeout). */
class FoodLookupException(cause: Throwable) : Exception(cause)

/**
 * Open Food Facts: free, open product database (ingredients, nutrition, Nutri-Score, NOVA,
 * allergens, labels). Their API asks for an identifying User-Agent and no bulk scraping.
 */
@Singleton
class OpenFoodFactsClient @Inject constructor() {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val fields = listOf(
        "code", "product_name", "brands", "nutriments", "serving_size", "serving_quantity", "quantity",
        "nova_group", "nutriscore_grade", "ingredients_text", "ingredients_text_en", "allergens_tags",
        "additives_tags", "labels_tags", "categories_tags", "image_front_small_url",
    ).joinToString(",")

    /** The product, or null if the barcode isn't in the database. */
    suspend fun lookup(barcode: String): ScannedFood? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=$fields")
            .header("User-Agent", "Rave/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})")
            .build()
        val body = try {
            http.newCall(request).execute().use { r -> if (r.code == 404) return@withContext null else r.body?.string() }
        } catch (e: Exception) {
            throw FoodLookupException(e)
        } ?: return@withContext null
        parse(body, barcode)
    }

    companion object {
        fun parse(json: String, barcode: String): ScannedFood? {
            val root = JSONObject(json)
            if (root.optInt("status", 0) != 1) return null
            val p = root.optJSONObject("product") ?: return null
            val n = p.optJSONObject("nutriments") ?: JSONObject()
            fun num(key: String): Double? = if (n.has(key)) n.optDouble(key).takeIf { !it.isNaN() } else null
            fun tags(key: String): List<String> = p.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
            fun nutrients(suffix: String) = Nutrients(
                kcal = num("energy-kcal$suffix") ?: num("energy$suffix")?.div(4.184),
                protein = num("proteins$suffix"), carbs = num("carbohydrates$suffix"), sugar = num("sugars$suffix"),
                fat = num("fat$suffix"), satFat = num("saturated-fat$suffix"), fiber = num("fiber$suffix"), salt = num("salt$suffix"),
            ).takeIf { !it.isEmpty }
            val categories = tags("categories_tags")
            val quantity = p.optString("quantity").lowercase()
            return ScannedFood(
                name = p.optString("product_name").ifBlank { "Unknown product" },
                brand = p.optString("brands").substringBefore(','),
                per100g = nutrients("_100g"),
                perServing = nutrients("_serving"),
                servingGrams = p.optDouble("serving_quantity").takeIf { !it.isNaN() && it > 0 },
                servingLabel = p.optString("serving_size"),
                isDrink = categories.any { it.contains("beverages") || it.contains("drinks") } || quantity.endsWith("ml") || quantity.endsWith(" l"),
                novaGroup = p.optInt("nova_group", 0).takeIf { it in 1..4 },
                nutriScore = p.optString("nutriscore_grade").firstOrNull()?.takeIf { it in 'a'..'e' },
                ingredients = p.optString("ingredients_text_en").ifBlank { p.optString("ingredients_text") },
                allergenTags = tags("allergens_tags"),
                additiveTags = tags("additives_tags"),
                labelTags = tags("labels_tags"),
                categoryTags = categories,
                barcode = barcode,
                imageUrl = p.optString("image_front_small_url").ifBlank { null },
                source = FoodSource.BARCODE,
            )
        }
    }
}
