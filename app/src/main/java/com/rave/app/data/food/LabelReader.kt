package com.rave.app.data.food

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.NutritionLabelParser
import com.rave.app.domain.food.ScannedFood
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Reads a photographed nutrition label on the device (ML Kit, no internet needed). */
@Singleton
class LabelReader @Inject constructor(@ApplicationContext private val context: Context) {

    /** The label's nutrients, or null if no nutrition values could be read. */
    suspend fun read(photo: Uri, name: String): ScannedFood? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = try {
            recognizer.process(InputImage.fromFilePath(context, photo)).await().text
        } finally {
            recognizer.close()
        }
        val parsed = NutritionLabelParser.parse(text)
        if (parsed.nutrients.isEmpty) return null
        return ScannedFood(
            name = name,
            per100g = if (parsed.per100) parsed.nutrients else null,
            perServing = if (parsed.per100) null else parsed.nutrients,
            ingredients = text.substringAfter("ingredients", "").take(600),
            source = FoodSource.LABEL,
        )
    }
}
