package com.gumah.devnet.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object MediaDownloader {
    suspend fun downloadMediaFile(context: Context, downloadUrl: String, customFileName: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                if (downloadUrl.isBlank()) {
                    throw Exception("الرابط فارغ")
                }

                // Convert HTTP to HTTPS for Cloudinary/remote URLs to avoid security blocks completely
                var safeUrl = downloadUrl.trim()
                if (safeUrl.startsWith("http://", ignoreCase = true)) {
                    safeUrl = "https://" + safeUrl.substring(7)
                }
                
                // Set standard file name
                val finalFileName = customFileName ?: safeUrl.substringAfterLast("/").substringBefore("?")
                val fileExtension = finalFileName.substringAfterLast(".", "jpg").lowercase()
                
                val isVideo = fileExtension == "mp4" || fileExtension == "mkv" || fileExtension == "avi" || fileExtension == "3gp" || fileExtension == "mov"
                
                // Explicitly map MIME-types so they are accepted correctly by all Android system engines
                val mimeType = if (isVideo) {
                    when (fileExtension) {
                        "mkv" -> "video/x-matroska"
                        "avi" -> "video/x-msvideo"
                        "3gp" -> "video/3gpp"
                        "mov" -> "video/quicktime"
                        else -> "video/mp4"
                    }
                } else {
                    when (fileExtension) {
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        "webp" -> "image/webp"
                        "bmp" -> "image/bmp"
                        else -> "image/jpeg"
                    }
                }

                // Establish Connection with Redirect Following (Multi-tier CDNs)
                var connection = URL(safeUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                
                var responseCode = connection.responseCode
                var redirectedUrl = safeUrl
                var redirectCount = 0
                
                // Follow up to 5 tiers of redirects (HTTP 301, 302, 303, 307, 308)
                while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) && redirectCount < 5) {
                    val location = connection.getHeaderField("Location") ?: break
                    redirectedUrl = URL(URL(redirectedUrl), location).toString()
                    connection.disconnect()
                    
                    connection = URL(redirectedUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    responseCode = connection.responseCode
                    redirectCount++
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("خطأ في الخادم (رمز الاستجابة: $responseCode)")
                }

                val contentResolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    
                    // Save in beautiful dedicated "DevNet Social" folder
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val directory = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/DevNet Social")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                
                val collectionUri = if (isVideo) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }
                
                val uri = contentResolver.insert(collectionUri, contentValues)
                if (uri != null) {
                    val inputStream: InputStream = connection.inputStream
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.close()
                        inputStream.close()
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(uri, contentValues, null, null)
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "تم حفظ الملف بنجاح بمجلد DevNet Social!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("فشل إدراج الملف في سجلات الوسائط")
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "فشل حفظ الملف: ${e.message ?: e.toString()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
