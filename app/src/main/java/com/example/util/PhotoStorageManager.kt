package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object PhotoStorageManager {

    /**
     * Saves or compresses a dress photo into the dedicated catalog folder (context.filesDir/catalog_photos/).
     * Performs light JPEG compression (85%) and scales down if dimension exceeds 1280px.
     * Only saved ONCE in the catalog. Orders then reference this exact path with zero duplication.
     */
    suspend fun saveCatalogPhoto(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uriString = sourceUri.toString()
                // If it's already in catalog_photos or an android.resource://, reuse directly
                if ((uriString.startsWith("file://") && uriString.contains("catalog_photos")) ||
                    uriString.startsWith("android.resource://")
                ) {
                    return@withContext uriString
                }

                val catalogDir = File(context.filesDir, "catalog_photos").apply {
                    if (!exists()) mkdirs()
                }

                val fileName = "catalog_dress_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                val destinationFile = File(catalogDir, fileName)

                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val maxDimension = 1280
                    val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val ratio = minOf(
                            maxDimension.toFloat() / bitmap.width,
                            maxDimension.toFloat() / bitmap.height
                        )
                        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    } else {
                        bitmap
                    }

                    FileOutputStream(destinationFile).use { output ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    }

                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()

                    Uri.fromFile(destinationFile).toString()
                } else {
                    // Fallback to direct stream copy
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Uri.fromFile(destinationFile).toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sourceUri.toString()
            }
        }
    }

    /**
     * Saves order-specific photo if needed.
     * ZERO DUPLICATION: If the photo is already from catalog_photos or drawable, it directly returns
     * that uri without creating any extra copy!
     */
    suspend fun savePhotoToInternalStorage(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uriString = sourceUri.toString()
                // Zero duplication: If already a catalog photo, internal photo, or drawable, reuse directly!
                if (uriString.startsWith("android.resource://") ||
                    (uriString.startsWith("file://") && uriString.contains("catalog_photos")) ||
                    (uriString.startsWith("file://") && uriString.contains("order_photos"))
                ) {
                    return@withContext uriString
                }

                val photoDir = File(context.filesDir, "order_photos").apply {
                    if (!exists()) mkdirs()
                }

                val fileName = "order_photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                val destinationFile = File(photoDir, fileName)

                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val maxDimension = 1280
                    val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val ratio = minOf(
                            maxDimension.toFloat() / bitmap.width,
                            maxDimension.toFloat() / bitmap.height
                        )
                        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    } else {
                        bitmap
                    }

                    FileOutputStream(destinationFile).use { output ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    }

                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()

                    Uri.fromFile(destinationFile).toString()
                } else {
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Uri.fromFile(destinationFile).toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sourceUri.toString()
            }
        }
    }

    fun deleteCatalogPhotoIfLocal(context: Context, photoUriString: String) {
        try {
            if (photoUriString.startsWith("file://") && photoUriString.contains("catalog_photos")) {
                val path = Uri.parse(photoUriString).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
