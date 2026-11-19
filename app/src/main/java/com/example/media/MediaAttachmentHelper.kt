package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

data class EncodedMediaAttachment(
    val type: String,
    val base64Data: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

object MediaAttachmentHelper {
    private const val MAX_BASE64_RAW_BYTES = 420 * 1024 // Keeps Firestore doc well under 1MB limit

    private val allowedDocMimeTypes = setOf(
        "application/pdf",
        "text/plain",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )

    fun createCameraCaptureUri(context: Context): Uri {
        val file = File(context.cacheDir, "kigo_cam_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun prepareImageFromUri(context: Context, uri: Uri): Result<EncodedMediaAttachment> {
        return try {
            val fileName = queryFileName(context, uri) ?: "image_${System.currentTimeMillis()}.jpg"
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Unable to read selected image."))
            val original = inputStream.use { BitmapFactory.decodeStream(it) }
                ?: return Result.failure(IllegalStateException("Unsupported image format."))

            val scaled = scaleBitmap(original, maxDim = 960)
            var quality = 80
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 12
            } while (bytes.size > MAX_BASE64_RAW_BYTES && quality >= 30)

            if (bytes.size > MAX_BASE64_RAW_BYTES) {
                return Result.failure(IllegalStateException("Image exceeds maximum upload limit after compression."))
            }
            Result.success(
                EncodedMediaAttachment(
                    type = "image",
                    base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    fileName = fileName,
                    mimeType = "image/jpeg",
                    sizeBytes = bytes.size.toLong()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun prepareDocumentOrVideoFromUri(context: Context, uri: Uri, forceType: String? = null): Result<EncodedMediaAttachment> {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            val fileName = queryFileName(context, uri) ?: "attachment_${System.currentTimeMillis()}"
            val type = forceType ?: when {
                mimeType.startsWith("image/") -> return prepareImageFromUri(context, uri)
                mimeType.startsWith("video/") -> "video"
                mimeType in allowedDocMimeTypes || fileName.endsWith(".pdf", true) -> "document"
                else -> return Result.failure(IllegalArgumentException("Unsupported file type ($mimeType). Allowed: Images, Videos, PDF, TXT, DOC/DOCX."))
            }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("Unable to read selected file."))
            if (bytes.size > MAX_BASE64_RAW_BYTES) {
                return Result.failure(
                    IllegalStateException(
                        "File is ${(bytes.size / 1024)} KB. Inline Firestore sync supports up to 420 KB (configure Firebase Storage bucket for larger video/PDF files)."
                    )
                )
            }
            Result.success(
                EncodedMediaAttachment(
                    type = type,
                    base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    fileName = fileName,
                    mimeType = mimeType,
                    sizeBytes = bytes.size.toLong()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeImageBitmap(base64Data: String): Bitmap? {
        if (base64Data.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun saveAttachmentToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        base64Data: String
    ): Result<String> {
        return try {
            val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KigoChat")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Failed to create download entry"))
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                Result.success("Saved to Downloads/KigoChat/$fileName")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KigoChat")
                dir.mkdirs()
                val outFile = File(dir, fileName)
                outFile.writeBytes(bytes)
                Result.success("Saved to ${outFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = w.toFloat() / h.toFloat()
        val (nw, nh) = if (w >= h) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }
}
