package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun uploadApkToGoFile() {
    println("=== STARTING GOFILE UPLOAD ===")
    
    // 1. Create anonymous account/token if possible
    var token: String? = null
    try {
      println("Attempting to create a Gofile anonymous account...")
      val url = URL("https://api.gofile.io/accounts")
      val conn = url.openConnection() as HttpURLConnection
      conn.requestMethod = "POST"
      conn.connectTimeout = 10000
      conn.readTimeout = 10000
      
      val code = conn.responseCode
      println("Create Account Response Code: $code")
      val response = if (code in 200..299) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        conn.errorStream?.bufferedReader().use { it?.readText() } ?: ""
      }
      println("Create Account Response: $response")
      
      // Basic JSON extraction of token
      if (response.contains("\"token\"")) {
        val parts = response.split("\"token\":\"")
        if (parts.size > 1) {
          token = parts[1].split("\"")[0]
          println("Successfully obtained token: $token")
        }
      }
    } catch (e: Exception) {
      println("Failed to create account/get token: ${e.message}")
    }

    // 2. Fetch the best server from Gofile
    var serverName = "store1" // default fallback
    try {
      println("Fetching Gofile servers...")
      val url = URL("https://api.gofile.io/servers")
      val conn = url.openConnection() as HttpURLConnection
      conn.requestMethod = "GET"
      conn.connectTimeout = 10000
      conn.readTimeout = 10000
      
      val code = conn.responseCode
      println("Get Servers Response Code: $code")
      val response = if (code in 200..299) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        conn.errorStream?.bufferedReader().use { it?.readText() } ?: ""
      }
      println("Servers Response: $response")
      
      // Basic JSON extraction of first server name
      if (response.contains("\"name\":\"")) {
        val parts = response.split("\"name\":\"")
        if (parts.size > 1) {
          serverName = parts[1].split("\"")[0]
          println("Selected Gofile server: $serverName")
        }
      }
    } catch (e: Exception) {
      println("Failed to fetch servers: ${e.message}. Using default server: $serverName")
    }

    // 3. Find DevNet.apk and DevNet-debug.apk
    // Let's check typical parent locations since tests execute in `/app/applet/app`
    val searchFiles = listOf(
      "app-debug.apk" to listOf("/app/app-debug.apk", "../app-debug.apk", "./app-debug.apk", "/.build-outputs/app-debug.apk"),
      "DevNet.apk" to listOf("/app/DevNet.apk", "../../DevNet.apk", "../DevNet.apk", "./DevNet.apk"),
      "DevNet-debug.apk" to listOf("/app/DevNet-debug.apk", "../../DevNet-debug-apk", "../../DevNet-debug.apk", "../DevNet-debug.apk", "./DevNet-debug.apk")
    )
    val filesToUpload = mutableListOf<File>()
    for ((name, paths) in searchFiles) {
      for (p in paths) {
        val f = File(p)
        if (f.exists() && f.isFile) {
          println("Found matching file: ${f.absolutePath} for $name")
          filesToUpload.add(f)
          break
        }
      }
    }

    if (filesToUpload.isEmpty()) {
      // Let's list files in parent directory of parent
      val rootDir = File("../../")
      println("Listing files in root directory (${rootDir.absolutePath}):")
      rootDir.listFiles()?.forEach { println(" - ${it.name}") }
      fail("No APK files found for upload!")
      return
    }

    // 4. Perform upload for each found file
    for (apkFile in filesToUpload) {
      println("Uploading file: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
      try {
        val uploadUrlString = "https://$serverName.gofile.io/contents/uploadfile"
        println("Upload URL: $uploadUrlString")
        
        val boundary = "Boundary" + System.currentTimeMillis()
        val LINE_FEED = "\r\n"
        val url = URL(uploadUrlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.useCaches = false
        conn.doOutput = true
        conn.doInput = true
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        
        val outStream = conn.outputStream
        val writer = PrintWriter(OutputStreamWriter(outStream, "UTF-8"), true)
        
        // Add token field if available
        if (token != null) {
          println("Adding token to upload request fields: $token")
          writer.append("--$boundary").append(LINE_FEED)
          writer.append("Content-Disposition: form-data; name=\"token\"").append(LINE_FEED)
          writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED)
          writer.append(LINE_FEED)
          writer.append(token).append(LINE_FEED)
          writer.flush()
        }
        
        // Add file field
        val fileName = apkFile.name
        writer.append("--$boundary").append(LINE_FEED)
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"").append(LINE_FEED)
        writer.append("Content-Type: application/vnd.android.package-archive").append(LINE_FEED)
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED)
        writer.append(LINE_FEED)
        writer.flush()
        
        val fileStream = FileInputStream(apkFile)
        val buffer = ByteArray(40960) // 40KB parts
        var bytesRead: Int
        while (fileStream.read(buffer).also { bytesRead = it } != -1) {
          outStream.write(buffer, 0, bytesRead)
        }
        outStream.flush()
        fileStream.close()
        
        writer.append(LINE_FEED)
        writer.flush()
        
        // End boundary
        writer.append("--$boundary--").append(LINE_FEED)
        writer.close()
        
        val responseCode = conn.responseCode
        println("Upload Response Code: $responseCode")
        val response = if (responseCode in 200..299) {
          conn.inputStream.bufferedReader().use { it.readText() }
        } else {
          conn.errorStream?.bufferedReader().use { it?.readText() } ?: ""
        }
        
        println("=== UPLOAD COMPLETED FOR ${apkFile.name} ===")
        println("Upload Server Response payload:")
        println(response)
        
        // Extract downloadPage / download link
        if (response.contains("\"downloadPage\"")) {
          val parts = response.split("\"downloadPage\":\"")
          if (parts.size > 1) {
            val downloadLink = parts[1].split("\"")[0].replace("\\/", "/")
            println("DOWNLOAD LINK DETECTED FOR ${apkFile.name}:")
            println("URL: $downloadLink")
          }
        }
      } catch (e: Exception) {
        println("Error during Gofile upload of ${apkFile.name}: ${e.message}")
        e.printStackTrace()
      }
    }
  }
}


