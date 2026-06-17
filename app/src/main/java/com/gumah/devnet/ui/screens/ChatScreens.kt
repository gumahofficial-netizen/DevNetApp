package com.gumah.devnet.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import coil.compose.AsyncImage
import com.gumah.devnet.data.CloudinaryHelper
import com.gumah.devnet.data.Conversation
import com.gumah.devnet.data.Message
import com.gumah.devnet.data.UserProfile
import com.gumah.devnet.data.DevNetRepository
import com.gumah.devnet.data.MediaDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onNavigateToChatRoom: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val conversations by DevNetRepository.conversations.collectAsState()
    val developers by DevNetRepository.developers.collectAsState()
    val currentUser by DevNetRepository.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isNewChatSelectorOpen by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, searchQuery, currentUser) {
        conversations.filter { conv ->
            val recipientId = conv.participantIds.find { it != currentUser?.id } ?: ""
            val recipientName = conv.participantNames[recipientId] ?: ""
            recipientName.lowercase().contains(searchQuery.lowercase()) ||
            conv.lastMessageText.lowercase().contains(searchQuery.lowercase())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Developer DMs",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(
                    onClick = { isNewChatSelectorOpen = true },
                    modifier = Modifier.background(Color(0xFF38BDF8), CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New DM", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                }
            }

            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search conversations...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conversations List
            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Empty", tint = Color(0xFF475569), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No coding threads initialized", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Tap compiler edit icon to ping other programmers", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredConversations) { conv ->
                        val recipientId = conv.participantIds.find { it != currentUser?.id } ?: ""
                        val recipientName = conv.participantNames[recipientId] ?: "Programmer"
                        val recipientAvatar = conv.participantAvatars[recipientId] ?: ""
                        val isUnread = conv.isRead[currentUser?.id ?: ""] == false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChatRoom(conv.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AsyncImage(
                                    model = recipientAvatar.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                // Active green circle indicator
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0xFF2DD4BF), CircleShape)
                                        .border(2.dp, Color(0xFF0F172A), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = recipientName,
                                        color = if (isUnread) Color.White else Color(0xFFE2E8F0),
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = formatChatTime(conv.lastMessageTime),
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.lastMessageText,
                                        color = if (isUnread) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF38BDF8), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        // New Chat Creator modal Dialog
        if (isNewChatSelectorOpen) {
            Dialog(onDismissRequest = { isNewChatSelectorOpen = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Initialize Coding Thread", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { isNewChatSelectorOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val chatAvailableList = developers.filter { it.id != currentUser?.id }
                            items(chatAvailableList) { dev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                val conv = DevNetRepository.startConversation(dev.id)
                                                isNewChatSelectorOpen = false
                                                onNavigateToChatRoom(conv.id)
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = dev.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(dev.fullName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(dev.specialty, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, contentDescription = "", tint = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversationId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val conversations by DevNetRepository.conversations.collectAsState()
    val messagesMap by DevNetRepository.messages.collectAsState()
    val currentUser by DevNetRepository.currentUser.collectAsState()

    val conversation = remember(conversations, conversationId) {
        conversations.find { it.id == conversationId }
    }
    
    val messages = messagesMap[conversationId] ?: emptyList()

    var messageInput by remember { mutableStateOf("") }
    var codeSnippet by remember { mutableStateOf("") }
    var attachedFileUrl by remember { mutableStateOf("") }
    var attachedFileType by remember { mutableStateOf("none") }
    var attachedFilename by remember { mutableStateOf("") }
    
    var isUploading by remember { mutableStateOf(false) }
    var isSnippetOpen by remember { mutableStateOf(false) }
    var isTypingSimulator by remember { mutableStateOf(false) }

    var isVoiceRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<java.io.File?>(null) }
    var voiceRecordingDurationSec by remember { mutableStateOf(0) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required to record audio messages", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            voiceRecordingDurationSec = 0
            while (isVoiceRecording) {
                delay(1000)
                voiceRecordingDurationSec++
            }
        }
    }

    // Search inside Current Room messages!
    var localSearchQuery by remember { mutableStateOf("") }
    var isSearchEnabled by remember { mutableStateOf(false) }

    val filteredMessages = remember(messages, localSearchQuery, isSearchEnabled) {
        if (!isSearchEnabled || localSearchQuery.isEmpty()) messages
        else messages.filter { 
            it.text.lowercase().contains(localSearchQuery.lowercase()) ||
            it.codeSnippet.lowercase().contains(localSearchQuery.lowercase())
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(conversationId) {
        DevNetRepository.listenToMessages(conversationId)
    }

    // Scroll automatically on message length change
    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    // Attachment Launcher
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            coroutineScope.launch {
                try {
                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    val resourceType = when {
                        mimeType.startsWith("image/") -> "image"
                        mimeType.startsWith("video/") -> "video"
                        mimeType.startsWith("audio/") -> "video"
                        else -> "raw"
                    }
                    val detectedType = when {
                        mimeType.startsWith("image/") -> "image"
                        mimeType.startsWith("video/") -> "video"
                        mimeType.startsWith("audio/") -> "audio"
                        else -> "file"
                    }
                    
                    val cloudUrl = CloudinaryHelper.uploadUri(context, uri, resourceType)
                    if (cloudUrl != null) {
                        attachedFileUrl = cloudUrl
                        attachedFilename = uri.path?.substringAfterLast("/") ?: "file_attachment"
                        attachedFileType = detectedType
                    } else {
                        Toast.makeText(context, "Upload failed. Please verify credentials.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Throwable) {
                    Toast.makeText(context, "Error uploading attachment: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    val recipientId = conversation?.participantIds?.find { it != currentUser?.id } ?: ""
    val recipientName = conversation?.participantNames?.get(recipientId) ?: "Developer Chat"
    val recipientAvatar = conversation?.participantAvatars?.get(recipientId) ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Room detail Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1F2C34),
                border = BorderStroke(1.dp, Color(0xFF2C3E4B))
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        AsyncImage(
                            model = recipientAvatar.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                            contentDescription = "Receiver",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recipientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (isTypingSimulator) "compiling thoughts..." else "Developer active branch",
                                color = if (isTypingSimulator) Color(0xFF2DD4BF) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Search Toggle icon (البحث داخل المحادثات)
                        IconButton(onClick = { isSearchEnabled = !isSearchEnabled }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchEnabled) Color(0xFF38BDF8) else Color.White)
                        }
                    }

                    // Search panel inline
                    if (isSearchEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = localSearchQuery,
                                onValueChange = { localSearchQuery = it },
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (localSearchQuery.isEmpty()) Text("Locate keyword in message history...", color = Color(0xFF475569), fontSize = 13.sp)
                                    inner()
                                }
                            )
                            if (localSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { localSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Message Bubble list lazy
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMessages) { msg ->
                    val isOwn = msg.senderId == currentUser?.id

                    ChatBubble(
                        message = msg,
                        isOwn = isOwn,
                        onDeleteClick = {
                            coroutineScope.launch {
                                DevNetRepository.deleteMessage(conversationId, msg.id)
                            }
                        }
                    )
                }

                // Temporary Typing animation indicator placeholder
                if (isTypingSimulator) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("...", color = Color(0xFF2DD4BF), fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // Snippet input drawer
        if (isSnippetOpen) {
            Dialog(onDismissRequest = { isSnippetOpen = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add Monospace Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = codeSnippet,
                            onValueChange = { codeSnippet = it },
                            placeholder = { Text("// Input code snippet", fontFamily = FontFamily.Monospace, color = Color(0xFF64748B)) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 12.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isSnippetOpen = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
                            Button(
                                onClick = { isSnippetOpen = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                            ) { Text("Ready", color = Color(0xFF0F172A)) }
                        }
                    }
                }
            }
        }

        // Messenger floating WhatsApp styled footer layout
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = Color.Transparent,
            border = null
        ) {
            Column(modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp)) {
                if (attachedFileUrl.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            .background(Color(0xFF1F2C34), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF2C3E4B), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "", tint = Color(0xFF00A884))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(attachedFilename, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { attachedFileUrl = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "", tint = Color(0xFFFB7185))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVoiceRecording) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1F2C34), RoundedCornerShape(26.dp))
                                .border(1.dp, Color(0xFF2C3E4B), RoundedCornerShape(26.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFB7185))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جاري التسجيل: ${voiceRecordingDurationSec}s",
                                    color = Color(0xFFFB7185),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        try {
                                            mediaRecorder?.let {
                                                it.stop()
                                                it.release()
                                            }
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                        mediaRecorder = null
                                        isVoiceRecording = false
                                        recordedFile?.delete()
                                        recordedFile = null
                                    },
                                    modifier = Modifier
                                        .background(Color(0xFF2C3E4B), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Cancel Recording", tint = Color(0xFFFB7185), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        try {
                                            mediaRecorder?.let {
                                                it.stop()
                                                it.release()
                                            }
                                            mediaRecorder = null
                                            isVoiceRecording = false

                                            val file = recordedFile
                                            if (file != null && file.exists()) {
                                                val bytes = file.readBytes()
                                                isUploading = true
                                                coroutineScope.launch {
                                                    try {
                                                        val cloudUrl = CloudinaryHelper.uploadFile(
                                                            fileBytes = bytes,
                                                            fileName = file.name,
                                                            fileType = "video"
                                                        )
                                                        if (cloudUrl != null) {
                                                            DevNetRepository.sendMessage(
                                                                conversationId = conversationId,
                                                                text = "Voice Transmission Thread",
                                                                mediaUrl = cloudUrl,
                                                                mediaType = "audio"
                                                            )
                                                        } else {
                                                            Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Throwable) {
                                                        Toast.makeText(context, "Error sending voice: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        isUploading = false
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isVoiceRecording = false
                                            mediaRecorder = null
                                            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .background(Color(0xFF00A884), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Send Voice Message", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        // WhatsApp style capsule bar (left side)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1F2C34), RoundedCornerShape(26.dp))
                                .border(1.dp, Color(0xFF2C3E4B), RoundedCornerShape(26.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isSnippetOpen = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Code, contentDescription = "Insert Code", tint = if (codeSnippet.isNotEmpty()) Color(0xFF00A884) else Color(0xFF8696A0), modifier = Modifier.size(20.dp))
                            }

                            IconButton(onClick = { picker.launch("*/*") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.AttachFile, contentDescription = "File Attachment", tint = Color(0xFF8696A0), modifier = Modifier.size(20.dp))
                            }

                            BasicTextField(
                                value = messageInput,
                                onValueChange = { 
                                    messageInput = it
                                    if (it.isNotEmpty() && !isTypingSimulator) {
                                        isTypingSimulator = true
                                        coroutineScope.launch {
                                            delay(4000)
                                            isTypingSimulator = false
                                        }
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 15.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                decorationBox = { inner ->
                                    if (messageInput.isEmpty()) Text("اكتب رسالة...", color = Color(0xFF8696A0), fontSize = 14.sp)
                                    inner()
                                }
                            )

                            IconButton(
                                onClick = {
                                    try {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (!hasPermission) {
                                            recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            val file = java.io.File(context.cacheDir, "voice_ref_${System.currentTimeMillis()}.mp4")
                                            recordedFile = file

                                            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                android.media.MediaRecorder(context)
                                            } else {
                                                @Suppress("DEPRECATION")
                                                android.media.MediaRecorder()
                                            }

                                            recorder.apply {
                                                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                                setOutputFile(file.absolutePath)
                                                prepare()
                                                start()
                                            }
                                            mediaRecorder = recorder
                                            isVoiceRecording = true
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Voice trigger issue: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Record voice", tint = Color(0xFF8696A0), modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Green floating circle send button (right side)
                        if (isUploading) {
                            Box(
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00A884), modifier = Modifier.size(24.dp))
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageInput.isBlank() && codeSnippet.isBlank() && attachedFileUrl.isEmpty()) return@IconButton
                                    coroutineScope.launch {
                                        try {
                                            DevNetRepository.sendMessage(
                                                conversationId = conversationId,
                                                text = messageInput.trim(),
                                                mediaUrl = attachedFileUrl,
                                                mediaType = attachedFileType,
                                                codeSnippet = codeSnippet
                                            )
                                            messageInput = ""
                                            codeSnippet = ""
                                            attachedFileUrl = ""
                                            attachedFileType = "none"
                                        } catch (e: Throwable) {
                                            Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xFF00A884), CircleShape)
                                    .size(46.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Transmit", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Chat rendering bubble design
@Composable
fun ChatBubble(
    message: Message,
    isOwn: Boolean,
    onDeleteClick: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isOwn) Color(0xFF005C4B) else Color(0xFF1F2C34),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isOwn) 12.dp else 2.dp,
                            bottomEnd = if (isOwn) 2.dp else 12.dp
                        )
                    )
                    .clickable { 
                        if (isOwn && !message.isDeleted) expandedMenu = true 
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (message.isDeleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFF8696A0),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تم حذف هذه الرسالة",
                                color = Color(0xFF8696A0),
                                fontSize = 13.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    } else {
                        // Regular message
                        if (message.text.isNotEmpty()) {
                            Text(
                                text = message.text,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        // Code snippets inside messages!
                        if (message.codeSnippet.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                                color = Color(0xFF020617),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = message.codeSnippet,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF34D399),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                        }

                        // Attachments inside messages!
                        if (message.mediaUrl.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (message.mediaType == "audio") {
                                CustomVoiceMessagePlayer(audioUrl = message.mediaUrl)
                            } else if (message.mediaType == "video") {
                                Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                                    DevNetVideoPlayer(videoUrl = message.mediaUrl)
                                    
                                    // Chat Video Download Overlay Action Button
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                MediaDownloader.downloadMediaFile(context, message.mediaUrl, "chat_video_${message.id}.mp4")
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "تحميل الفيديو",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Chat Image Download Overlay Action Button
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                MediaDownloader.downloadMediaFile(context, message.mediaUrl, "chat_image_${message.id}.jpg")
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "تحميل الصورة",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Sequence", color = Color(0xFFFB7185)) },
                        onClick = {
                            onDeleteClick()
                            expandedMenu = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatChatTime(message.createdAt),
                    color = Color(0xFF8696A0),
                    fontSize = 9.sp
                )
                if (isOwn) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Read status",
                        tint = Color(0xFF53BDEB), // WhatsApp beautiful blue double ticks color
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(audioUrl: String, modifier: Modifier = Modifier) {
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    currentPosition = mediaPlayer?.currentPosition ?: 0
                } catch (e: Exception) {
                    // Ignore state sync errors
                }
                delay(200)
            }
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                try {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            mediaPlayer = android.media.MediaPlayer().apply {
                                setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                                setDataSource(audioUrl)
                                prepareAsync()
                                setOnPreparedListener {
                                    duration = it.duration
                                    it.start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = 0
                                }
                            }
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    }
                } catch (e: Exception) {
                    isPlaying = false
                }
            },
            modifier = Modifier
                .background(Color(0xFF38BDF8), CircleShape)
                .size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.width(160.dp)) {
            Text(
                text = "Voice transmission",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF334155),
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatVoiceDuration(currentPosition),
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
                Text(
                    text = if (duration > 0) formatVoiceDuration(duration) else "0:00",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }
        }
    }
}

fun formatVoiceDuration(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%d:%02d", mins, secs)
}

// Compact helper for conversations time format
fun formatChatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Active"
    }
}

@Composable
fun ChatVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .width(220.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPlaying = true },
                contentAlignment = Alignment.Center
            ) {
                // Play circle and instructions
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Inline Video",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Tap to load video stream",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoURI(android.net.Uri.parse(videoUrl))
                        val mediaController = android.widget.MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { videoView ->
                    videoView.stopPlayback()
                }
            )
        }
    }
}
