package com.rave.app.ui.share

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.rave.app.BuildConfig
import com.rave.app.R
import java.io.File
import java.io.FileOutputStream

/** Saving + sharing images and text through FileProvider and explicit share targets. */
object ShareHelper {
    private const val INSTAGRAM = "com.instagram.android"
    private const val WHATSAPP = "com.whatsapp"
    private const val IG_STORY_ACTION = "com.instagram.share.ADD_TO_STORY"

    fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** Writes [bitmap] as PNG (keeps transparency) into cache/shared and returns a content Uri. */
    fun cacheImage(context: Context, bitmap: Bitmap, name: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    fun cacheFile(context: Context, file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

    fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    /**
     * Instagram Story: full-bleed background image, or for the transparent sticker an
     * interactive sticker asset. Falls back to the system sheet if Instagram isn't installed.
     */
    fun shareToInstagramStory(context: Context, uri: Uri, sticker: Boolean, fallbackText: String) {
        val intent = Intent(IG_STORY_ACTION).apply {
            if (sticker) {
                setType("image/png")
                putExtra("interactive_asset_uri", uri)
                putExtra("top_background_color", "#F26B1D")
                putExtra("bottom_background_color", "#3B2219")
            } else {
                setDataAndType(uri, "image/png")
            }
            if (BuildConfig.FACEBOOK_APP_ID.isNotBlank() && BuildConfig.FACEBOOK_APP_ID != "YOUR_FACEBOOK_APP_ID") putExtra("source_application", BuildConfig.FACEBOOK_APP_ID)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.grantUriPermission(INSTAGRAM, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            shareImage(context, uri, fallbackText)
        }
    }

    fun shareToWhatsApp(context: Context, uri: Uri?, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(WHATSAPP)
            if (uri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (uri != null) shareImage(context, uri, text) else shareText(context, text)
        }
    }

    fun shareImage(context: Context, uri: Uri, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareSms(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            shareText(context, text)
        }
    }

    /** Saves to Pictures/<app name>. On Android 9 and below the caller must hold WRITE_EXTERNAL_STORAGE. */
    fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
        val appName = context.getString(R.string.app_name)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$appName")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrDefault(false)
}
