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
        val result = try {
            recognizer.process(InputImage.fromFilePath(context, photo)).await()
        } finally {
            recognizer.close()
        }
        // Rebuild the table rows from the line positions; OCR reads label columns one after another.
        val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            line.boundingBox?.let { b -> NutritionLabelParser.OcrLine(line.text, b.left, b.top, b.right, b.bottom) }
        }
        val text = if (lines.isNotEmpty()) NutritionLabelParser.rows(lines) else result.text
        val parsed = NutritionLabelParser.parse(text).takeIf { it.found >= MIN_VALUES }
            ?: NutritionLabelParser.parse(result.text).takeIf { it.found >= MIN_VALUES }
            ?: return null
        return ScannedFood(
            name = name,
            per100g = if (parsed.per100) parsed.nutrients else null,
            perServing = if (parsed.per100) null else parsed.nutrients,
            ingredients = result.text.indexOf("ingredients", ignoreCase = true)
                .let { i -> if (i < 0) "" else result.text.substring(i + "ingredients".length) }.take(600),
            source = FoodSource.LABEL,
        )
    }

    private companion object {
        /** Fewer values than this is usually a blurry or partial photo; the AI reader gets a try instead. */
        const val MIN_VALUES = 3
    }
}
