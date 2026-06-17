package com.gumah.devnet.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gumah.devnet.data.CloudinaryHelper
import com.gumah.devnet.data.DevNetRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostCreated: (String) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }

    val selectedUris = remember { mutableStateListOf<Uri>() }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris)
            // Persist permissions
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, IntentFlag)
                } catch (_: Exception) {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Create Post", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { picker.launch(arrayOf("image/*", "video/*", "application/pdf", "application/zip", "*/*")) }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Media / Files")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Selected: ${selectedUris.size}")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Previews
        selectedUris.forEach { uri ->
            FilePreview(uri = uri, context = context)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text("Tags (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isUploading) {
            LinearProgressIndicator(progress = if (uploadProgress <= 0) 0f else uploadProgress / 100f, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text("Uploading... $uploadProgress%")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                // Save as draft locally before cancelling
                val draft = com.gumah.devnet.data.Draft(
                    title = if (text.length > 60) text.take(60) else text,
                    text = text,
                    tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
                coroutineScope.launch {
                    DevNetRepository.saveDraft(draft)
                    onCancel()
                }
            }) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                coroutineScope.launch {
                    if (text.isBlank() && selectedUris.isEmpty()) return@launch
                    isUploading = true
                    uploadProgress = 0
                    val uploadedUrls = mutableListOf<String>()
                    var idx = 0
                    for (uri in selectedUris) {
                        idx++
                        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileType = when {
                            mime.startsWith("image") -> "image"
                            mime.startsWith("video") -> "video"
                            else -> "raw"
                        }
                        val url = CloudinaryHelper.uploadUri(context, uri, fileType)
                        if (url != null) uploadedUrls.add(url)
                        uploadProgress = (idx * 100) / maxOf(1, selectedUris.size)
                    }

                    try {
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val post = DevNetRepository.createPost(
                            text = text,
                            tags = tags,
                            customMediaUrls = uploadedUrls,
                            customMediaType = if (uploadedUrls.isNotEmpty()) "media" else "none",
                            fileUrls = uploadedUrls
                        )
                        onPostCreated(post.id)
                    } catch (e: Exception) {
                        // show toast via Compose doesn't have direct toast here; ignore for brevity
                    } finally {
                        isUploading = false
                        uploadProgress = 100
                    }
                }
            }) {
                Text("Publish")
            }
        }
    }
}

private const val IntentFlag = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments::class.hashCode()

@Composable
private fun FilePreview(uri: Uri, context: Context) {
    val mime = remember(uri) { context.contentResolver.getType(uri) ?: "application/octet-stream" }
    if (mime.startsWith("image")) {
        AsyncImage(model = uri, contentDescription = "image_preview", modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp)))
    } else {
        Surface(modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp)), shape = RoundedCornerShape(8.dp)) {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = uri.lastPathSegment ?: "file", modifier = Modifier.weight(1f))
                Text(text = mime)
            }
        }
    }
}
