package com.gumah.devnet.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object CloudinaryHelper {
    private const val TAG = "CloudinaryHelper"
    private const val CLOUD_NAME = "dqgsepaus"
    private const val API_KEY = "799621938941556"
    private const val API_SECRET = "tMdtOXb6egJ2_0LJyIDTLmffPvY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun sha1(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "SHA1 generation error", e)
            ""
        }
    }

    /**
     * Uploads bytes directly to Cloudinary.
     * @param fileType "image", "video", or "raw" (for PDF, ZIP, APK, etc.)
     */
    suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        fileType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            
            // To sign: alphabetical sort of variables + api_secret at the end
            // timestamp=<timestamp><api_secret>
            val stringToSign = "timestamp=$timestamp$API_SECRET"
            val signature = sha1(stringToSign)

            val endpoint = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$fileType/upload"

            val mediaTypeStr = when (fileType) {
                "image" -> "image/*"
                "video" -> "video/*"
                else -> "application/octet-stream"
            }

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .addFormDataPart(
                    "file", 
                    fileName, 
                    fileBytes.toRequestBody(mediaTypeStr.toMediaTypeOrNull())
                )

            val requestBody = requestBodyBuilder.build()
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Upload failed with code ${response.code}: $errBody")
                    return@withContext null
                }
                val responseBodyStr = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Uploaded response: $responseBodyStr")
                val json = JSONObject(responseBodyStr)
                return@withContext json.optString("secure_url", null)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error uploading to Cloudinary", e)
            return@withContext null
        }
    }

    /**
     * Uploads a file via Android content Uri
     */
    suspend fun uploadUri(
        context: Context,
        uri: Uri,
        fileType: String,
        customFileName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.readBytes()
            val fileName = customFileName ?: "file_${System.currentTimeMillis()}"
            uploadFile(bytes, fileName, fileType)
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading Uri bytes", e)
            null
        }
    }
}
