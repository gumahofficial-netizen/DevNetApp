package com.gumah.devnet.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.gumah.devnet.data.CloudinaryHelper
import com.gumah.devnet.data.DevPost
import com.gumah.devnet.data.DevComment
import com.gumah.devnet.data.DevNetRepository
import com.gumah.devnet.data.MediaDownloader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSignals: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentUser by DevNetRepository.currentUser.collectAsState()
    val posts by DevNetRepository.posts.collectAsState()
    val isFeedLoading by DevNetRepository.isFeedLoading.collectAsState()
    val notifications by DevNetRepository.notifications.collectAsState()
    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }
    
    var activeTab by remember { mutableStateOf("Latest") } // Latest, Following, Followers, Trending, Communities, Careers
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    
    var isNewPostOpen by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<DevPost?>(null) }

    // Filtered posts calculation
    val filteredPosts = remember(posts, activeTab, searchQuery, selectedTag, currentUser) {
        var baseList = when (activeTab) {
            "Following" -> {
                val followingIds = currentUser?.following ?: emptyList()
                posts.filter { followingIds.contains(it.userId) || it.userId == currentUser?.id }
            }
            "Followers" -> {
                val followersIds = currentUser?.followers ?: emptyList()
                posts.filter { followersIds.contains(it.userId) || it.userId == currentUser?.id }
            }
            "Trending" -> {
                posts.sortedByDescending { it.likes.size + it.commentsCount }
            }
            else -> posts // Latest
        }

        if (selectedTag != null) {
            baseList = baseList.filter {
                it.tags.any { tag -> tag.equals(selectedTag, ignoreCase = true) }
            }
        }

        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            baseList = baseList.filter {
                it.text.lowercase().contains(query) || 
                it.tags.any { tag -> tag.lowercase().contains(query) } ||
                it.codeSnippet.lowercase().contains(query) ||
                it.fullName.lowercase().contains(query)
            }
        }
        baseList
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header panel with branding logo and user thumbnail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppLogo(modifier = Modifier.scale(0.85f))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Theme Switcher Toggle (Sun/Moon Switcher)
                    val isDark by com.gumah.devnet.data.DevNetRepository.isDarkThemeSystem.collectAsState()
                    IconButton(onClick = { com.gumah.devnet.data.DevNetRepository.isDarkThemeSystem.value = !isDark }) {
                        Icon(
                            imageVector = if (isDark) androidx.compose.material.icons.Icons.Default.LightMode else androidx.compose.material.icons.Icons.Default.DarkMode,
                            contentDescription = "Switch Visual Palette",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Inbox / DM Icon
                    IconButton(onClick = onNavigateToInbox) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Inbox",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Signals / Notifications icon with a dynamic badge
                    IconButton(onClick = onNavigateToSignals) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(containerColor = Color(0xFFFB7185)) {
                                        Text(unreadNotificationsCount.toString(), color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Signals",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            currentUser?.id?.let { onNavigateToProfile(it) }
                        }
                    ) {
                        AsyncImage(
                            model = currentUser?.avatarUrl ?: "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg",
                            contentDescription = "My Profile Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search terminal commands, posts, #tags...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // Category Chips Selection Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Latest", "Following", "Followers", "Trending", "Communities", "Careers").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { activeTab = tab }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dynamic Trending Tags bar (Fast filter options)
            val popularTags = listOf("kotlin", "compose", "ai", "android", "firebase", "backend", "web")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending:",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                popularTags.forEach { tag ->
                    val isTagSelected = selectedTag == tag
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isTagSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isTagSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedTag = if (isTagSelected) null else tag
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = if (isTagSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Central content block switching based on active feed category tab
            when (activeTab) {
                "Communities" -> {
                    Box(modifier = Modifier.weight(1f)) {
                        GroupsScreen()
                    }
                }
                "Careers" -> {
                    Box(modifier = Modifier.weight(1f)) {
                        JobsScreen()
                    }
                }
                else -> {
                    // Posts lazy list rendering
                    if (isFeedLoading) {
                        FeedSkeletonList(modifier = Modifier.weight(1f))
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            // Active hashtag selection indicator banner
                            if (selectedTag != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { selectedTag = null }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = "Tag filter",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Filtering posts by: #$selectedTag",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss filter",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (filteredPosts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CodeOff,
                                            contentDescription = "No Posts",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No codes compiled on this branch yet.",
                                            color = Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (selectedTag != null) "Try clearing the #$selectedTag hashtag filter." else "Be the first to push a post to main!",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    items(filteredPosts, key = { it.id }) { post ->
                                        PostCard(
                                            post = post,
                                            currentUserId = currentUser?.id ?: "",
                                            onProfileClick = { onNavigateToProfile(post.userId) },
                                            onLikeClick = {
                                                coroutineScope.launch {
                                                    DevNetRepository.toggleLikePost(post.id)
                                                }
                                            },
                                            onCommentClick = {
                                                selectedPostForComments = post
                                            },
                                            onSaveClick = {
                                                coroutineScope.launch {
                                                    DevNetRepository.toggleSavePost(post.id)
                                                }
                                            },
                                            onTagClick = { tag ->
                                                selectedTag = tag
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to post code (only on Feed tabs)
        if (activeTab != "Communities" && activeTab != "Careers") {
            FloatingActionButton(
                onClick = { isNewPostOpen = true },
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF0F172A),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 86.dp, end = 20.dp)
                    .testTag("submit_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Post")
            }
        }

        // Fullscreen Create Post Workspace Dialog
        if (isNewPostOpen) {
            CreatePostDialog(
                onDismiss = { isNewPostOpen = false },
                onPostSuccess = {
                    isNewPostOpen = false
                    Toast.makeText(context, "Post pushed successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Comments BottomSheet Dialog
        if (selectedPostForComments != null) {
            CommentsDialog(
                post = selectedPostForComments!!,
                onDismiss = { selectedPostForComments = null }
            )
        }
    }
}

@Composable
fun FeedSkeletonList(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        items(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Profile Block skeleton
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerItem(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ShimmerItem(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .height(14.dp)
                            )
                            ShimmerItem(
                                modifier = Modifier
                                    .fillMaxWidth(0.25f)
                                    .height(10.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Post text skeleton lines
                    ShimmerItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerItem(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerItem(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Code Block content square replica
                    ShimmerItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Interactive actions buttons line skeleton
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerItem(modifier = Modifier.size(20.dp), shape = CircleShape)
                            Spacer(modifier = Modifier.width(4.dp))
                            ShimmerItem(modifier = Modifier.width(32.dp).height(12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerItem(modifier = Modifier.size(20.dp), shape = CircleShape)
                            Spacer(modifier = Modifier.width(4.dp))
                            ShimmerItem(modifier = Modifier.width(32.dp).height(12.dp))
                        }
                        ShimmerItem(modifier = Modifier.size(20.dp), shape = CircleShape)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPostDialog(
    post: DevPost,
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>) -> Unit
) {
    var text by remember { mutableStateOf(post.text) }
    var codeSnippet by remember { mutableStateOf(post.codeSnippet) }
    var codeLanguage by remember { mutableStateOf(post.codeLanguage) }
    var tagsInput by remember { mutableStateOf(post.tags.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Refactor Post Payload",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Post content", color = Color(0xFF38BDF8)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                OutlinedTextField(
                    value = codeSnippet,
                    onValueChange = { codeSnippet = it },
                    label = { Text("Source Code Block (Optional)", color = Color(0xFF38BDF8)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                if (codeSnippet.isNotEmpty()) {
                    OutlinedTextField(
                        value = codeLanguage,
                        onValueChange = { codeLanguage = it },
                        label = { Text("Code syntax compiler language", color = Color(0xFF38BDF8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                }

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags / Hashtags (separated by comma)", color = Color(0xFF38BDF8)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abrupt Exit", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val parsedTags = tagsInput.split(",")
                                .map { it.trim().removePrefix("#") }
                                .filter { it.isNotEmpty() }
                            onSave(text, codeSnippet, codeLanguage, parsedTags)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("Deploy Refactor", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: DevPost,
    currentUserId: String,
    onProfileClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onTagClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val isLiked = post.likes.contains(currentUserId)
    val isSaved = post.savedBy.contains(currentUserId)
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .testTag("task_item_card"), // Assign standard test tag
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF38BDF8), CircleShape)
                        .clickable { onProfileClick() },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.fullName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { onProfileClick() }
                    )
                    Text(
                        text = post.username,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onProfileClick() }
                    )
                }

                if (post.userId == currentUserId) {
                    var isPostMenuOpen by remember { mutableStateOf(false) }
                    var isEditingPostOpen by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { isPostMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Post Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        DropdownMenu(
                            expanded = isPostMenuOpen,
                            onDismissRequest = { isPostMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Post") },
                                onClick = {
                                    isPostMenuOpen = false
                                    isEditingPostOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    isPostMenuOpen = false
                                    coroutineScope.launch {
                                        DevNetRepository.deletePost(post.id)
                                    }
                                }
                            )
                        }
                    }

                    if (isEditingPostOpen) {
                        EditPostDialog(
                            post = post,
                            onDismiss = { isEditingPostOpen = false },
                            onSave = { updatedText, updatedSnippet, updatedLang, updatedTags ->
                                coroutineScope.launch {
                                    DevNetRepository.editPost(
                                        postId = post.id,
                                        newText = updatedText,
                                        newCodeSnippet = updatedSnippet,
                                        newCodeLanguage = updatedLang,
                                        newTags = updatedTags
                                    )
                                    isEditingPostOpen = false
                                }
                            }
                        )
                    }
                }

                Text(
                    text = formatTime(post.createdAt),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            // Post Text content
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = post.text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Dynamic code styling block if code snippet is added
            if (post.codeSnippet.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                CodeTerminalBlock(code = post.codeSnippet, language = post.codeLanguage)
            }

            // Attachment layouts (ZIP, PDF, APK, etc.)
            if (post.fileNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                post.fileNames.forEachIndexed { idx, name ->
                    val fileUrl = post.fileUrls.getOrNull(idx) ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                if (fileUrl.isNotEmpty()) {
                                    try {
                                        uriHandler.openUri(fileUrl)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open file link: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Opening file: $name", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconImg = when {
                            name.endsWith(".zip", ignoreCase = true) -> Icons.Default.FolderZip
                            name.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
                            name.endsWith(".apk", ignoreCase = true) -> Icons.Default.Android
                            else -> Icons.Default.Attachment
                        }
                        Icon(iconImg, contentDescription = "Doc", tint = Color(0xFF2DD4BF), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Click to download payload", color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Smart YouTube video detection (either explicit mediaType or inline text URL)
            val youtubeVideoId = remember(post) {
                var id = if (post.mediaType == "youtube" && post.mediaUrls.isNotEmpty()) {
                    extractYouTubeVideoId(post.mediaUrls.first())
                } else null
                if (id == null) {
                    id = extractYouTubeVideoId(post.text)
                }
                id
            }

            if (youtubeVideoId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                YouTubePlayerView(videoId = youtubeVideoId)
            } else if (post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val firstUrl = post.mediaUrls.first()
                if (post.mediaType == "video") {
                    var isPlaying by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (!isPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .clickable { isPlaying = true },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = firstUrl,
                                    contentDescription = "Video preview thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                        .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play Feed Video", tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                                }
                            }
                        } else {
                            DevNetVideoPlayer(
                                videoUrl = firstUrl,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Overlay Download Button for Video
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    MediaDownloader.downloadMediaFile(context, firstUrl, "video_${post.id}.mp4")
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "حفظ الفيديو",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = firstUrl,
                            contentDescription = "Post Media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Overlay Download Button for Image
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    MediaDownloader.downloadMediaFile(context, firstUrl, "image_${post.id}.jpg")
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "حفظ الصورة",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Link boxes: Repository/Live
            if (post.repoUrl.isNotEmpty() || post.liveUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.repoUrl.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable {
                                    val url = if (post.repoUrl.startsWith("http://") || post.repoUrl.startsWith("https://")) {
                                        post.repoUrl
                                    } else {
                                        "https://" + post.repoUrl
                                    }
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open GitHub repo link: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = "Repo", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Source Repo", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (post.liveUrl.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable {
                                    val url = if (post.liveUrl.startsWith("http://") || post.liveUrl.startsWith("https://")) {
                                        post.liveUrl
                                    } else {
                                        "https://" + post.liveUrl
                                    }
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open production live link: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Web", tint = Color(0xFF2DD4BF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live App", color = Color(0xFF2DD4BF), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Tags layout
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    post.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            color = Color(0xFF2DD4BF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onTagClick(tag) }
                        )
                    }
                }
            }

            // Interaction buttons (Like, Comments, Repost, Save, Share)
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF334155), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes button
                Row(
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFB7185) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(post.likes.size.toString(), color = Color(0xFF94A3B8), fontSize = 13.sp)
                }

                // Comments button
                Row(
                    modifier = Modifier
                        .clickable { onCommentClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(post.commentsCount.toString(), color = Color(0xFF94A3B8), fontSize = 13.sp)
                }

                // Repost Icon button
                IconButton(onClick = {
                    Toast.makeText(context, "Reposted on your timeline", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repost", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }

                // Bookmark Icon Save button
                IconButton(onClick = { onSaveClick() }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share link
                IconButton(onClick = {
                    Toast.makeText(context, "DevNet link copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// Custom code editor terminal box
@Composable
fun CodeTerminalBlock(code: String, language: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF020617)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFB7185), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFBBF24), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF34D399), CircleShape))
                }
                Text(
                    text = language.uppercase(),
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Code text highlighter (simplistic keyword matcher for stunning visual feel)
            val highlightedText = buildAnnotatedString {
                val keywords = listOf("val", "var", "fun", "class", "import", "package", "return", "if", "else", "true", "false", "for", "while")
                val words = code.split(Regex("(?<=\\b)|(?=\\b)"))
                words.forEach { word ->
                    when {
                        keywords.contains(word) -> {
                            withStyle(style = SpanStyle(color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        }
                        word.startsWith("\"") || word.endsWith("\"") -> {
                            withStyle(style = SpanStyle(color = Color(0xFF34D399))) {
                                append(word)
                            }
                        }
                        else -> {
                            withStyle(style = SpanStyle(color = Color(0xFFE2E8F0))) {
                                append(word)
                            }
                        }
                    }
                }
            }
            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }
}

// Create Post Workspace dialog implementation
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var postText by remember { mutableStateOf("") }
    var codeSnippet by remember { mutableStateOf("") }
    var codeLanguage by remember { mutableStateOf("kotlin") }
    var tagsInput by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }
    var liveUrl by remember { mutableStateOf("") }
    var youtubeUrl by remember { mutableStateOf("") }
    
    // Media attachment states (Dynamic Cloudinary uploads)
    val attachedUrls = remember { mutableStateListOf<String>() }
    var attachedMediaType by remember { mutableStateOf("none") }
    val attachedFileNames = remember { mutableStateListOf<String>() }
    val attachedFileUrls = remember { mutableStateListOf<String>() }
    
    var isUploadingCloudinary by remember { mutableStateOf(false) }
    var isPostingToFirestore by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingCloudinary = true
            coroutineScope.launch {
                val cloudUrl = CloudinaryHelper.uploadUri(context, uri, "image")
                if (cloudUrl != null) {
                    attachedUrls.add(cloudUrl)
                    attachedMediaType = "image"
                } else {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
                isUploadingCloudinary = false
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingCloudinary = true
            coroutineScope.launch {
                val cloudUrl = CloudinaryHelper.uploadUri(context, uri, "video")
                if (cloudUrl != null) {
                    attachedUrls.add(cloudUrl)
                    attachedMediaType = "video"
                }
                isUploadingCloudinary = false
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingCloudinary = true
            coroutineScope.launch {
                val cloudUrl = CloudinaryHelper.uploadUri(context, uri, "raw", "report_archive_${System.currentTimeMillis()}")
                if (cloudUrl != null) {
                    val originalName = uri.path?.substringAfterLast("/") ?: "archive.zip"
                    attachedFileNames.add(originalName)
                    attachedFileUrls.add(cloudUrl)
                }
                isUploadingCloudinary = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Workspace Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Workspace", tint = Color.White)
                    }
                    Text(
                        text = if (isPreviewMode) "PROD PREVIEW" else "NEW POST BRANCH",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Text(
                            text = if (isPreviewMode) "Edit Code" else "Preview",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                if (isPreviewMode) {
                    // Preview branch layout
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val previewPost = DevPost(
                            text = postText,
                            codeSnippet = codeSnippet,
                            codeLanguage = codeLanguage,
                            tags = tagsInput.split(" ").filter { it.isNotEmpty() },
                            mediaUrls = attachedUrls.toList(),
                            mediaType = attachedMediaType,
                            fileNames = attachedFileNames.toList(),
                            fileUrls = attachedFileUrls.toList(),
                            repoUrl = repoUrl,
                            liveUrl = liveUrl
                        )
                        PostCard(
                            post = previewPost,
                            currentUserId = "preview",
                            onProfileClick = {},
                            onLikeClick = {},
                            onCommentClick = {},
                            onSaveClick = {}
                        )
                    }
                } else {
                    // Inputs form mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Main text description
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { Text("What is compiling today, developer?", color = Color(0xFF94A3B8)) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 10
                        )

                        // Code editing sub-terminal layout
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("IDE SNIPPET", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    
                                    // Custom input box for compiler language
                                    BasicTextField(
                                        value = codeLanguage,
                                        onValueChange = { codeLanguage = it },
                                        textStyle = LocalTextStyle.current.copy(
                                            color = Color(0xFF2DD4BF),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        decorationBox = { inner ->
                                            if (codeLanguage.isEmpty()) Text("kotlin", color = Color(0xFF475569), fontSize = 11.sp)
                                            inner()
                                        }
                                    )
                                }
                                OutlinedTextField(
                                    value = codeSnippet,
                                    onValueChange = { codeSnippet = it },
                                    placeholder = { Text("// Paste code snippet or block", color = Color(0xFF475569), fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                            }
                        }

                        // Attachments selector panel
                        Text("Add payloads to bundle:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { photoPicker.launch("image/*") },
                                modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            ) { Icon(Icons.Default.Image, contentDescription = "Add Photo", tint = Color(0xFF38BDF8)) }

                            IconButton(
                                onClick = { videoPicker.launch("video/*") },
                                modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            ) { Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = Color(0xFF2DD4BF)) }

                            IconButton(
                                onClick = { documentPicker.launch("*/*") },
                                modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            ) { Icon(Icons.Default.Archive, contentDescription = "Add ZIP/PDF", tint = Color(0xFFFB7185)) }
                        }

                        // Show Cloudinary loader when uploading
                        if (isUploadingCloudinary) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Uploading payload elements to Cloudinary CDN...", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Display uploaded lists
                        if (attachedUrls.isNotEmpty() || attachedFileNames.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                attachedUrls.forEach { url ->
                                    Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))) {
                                        AsyncImage(model = url, contentDescription = "Thumb", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                                attachedFileNames.forEach { name ->
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F172A))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(name, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // Optional developer fields
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Tags (space-separated, no # needed)") },
                            placeholder = { Text("Kotlin Mobile Security AI") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = repoUrl,
                            onValueChange = { repoUrl = it },
                            label = { Text("Associated Github Repository") },
                            placeholder = { Text("https://github.com/torvalds/linux") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = liveUrl,
                            onValueChange = { liveUrl = it },
                            label = { Text("Production Live Website / App Link") },
                            placeholder = { Text("https://devnet.app") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = youtubeUrl,
                            onValueChange = { youtubeUrl = it },
                            label = { Text("رابط فيديو يوتيوب (YouTube Video Link)") },
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFFEF4444),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.PlayCircle, contentDescription = "", tint = Color(0xFFEF4444))
                            }
                        )
                    }
                }

                // Compile build action trigger footer
                Button(
                    onClick = {
                        if (postText.isBlank() && codeSnippet.isBlank() && attachedUrls.isEmpty() && attachedFileUrls.isEmpty() && youtubeUrl.isBlank()) {
                            Toast.makeText(context, "Input at least some statements, code snippet, or video link to broadcast!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isPostingToFirestore = true
                        coroutineScope.launch {
                            try {
                                val splitTags = tagsInput.split(" ")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                var finalMediaType = attachedMediaType
                                var finalMediaUrls = attachedUrls.toList()
                                
                                val parsedYtId = extractYouTubeVideoId(youtubeUrl.trim())
                                if (parsedYtId != null) {
                                    finalMediaType = "youtube"
                                    finalMediaUrls = listOf("https://www.youtube.com/watch?v=$parsedYtId")
                                }

                                DevNetRepository.createPost(
                                    text = postText.trim(),
                                    codeSnippet = codeSnippet.trim(),
                                    codeLanguage = codeLanguage.trim(),
                                    tags = splitTags,
                                    customMediaUrls = finalMediaUrls,
                                    customMediaType = finalMediaType,
                                    fileNames = attachedFileNames.toList(),
                                    fileUrls = attachedFileUrls.toList(),
                                    githubLink = repoUrl.trim(),
                                    liveLink = liveUrl.trim()
                                )
                                onPostSuccess()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Push code run crash: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isPostingToFirestore = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPostingToFirestore
                ) {
                    if (isPostingToFirestore) {
                        CircularProgressIndicator(color = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Push", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PUSH CODE (BROADCAST)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Inner Comments sheet Dialog
@Composable
fun CommentsDialog(
    post: DevPost,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by DevNetRepository.currentUser.collectAsState()
    val commentsMap by DevNetRepository.comments.collectAsState()
    val comments = commentsMap[post.id] ?: emptyList()

    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var editingCommentText by remember { mutableStateOf("") }

    LaunchedEffect(post.id) {
        DevNetRepository.loadComments(post.id)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Code Reviews (${comments.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reviews", tint = Color.White)
                    }
                }
                Divider(color = Color(0xFF334155))

                // List comments
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No peer reviews compiled. Submitting PR is allowed!", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(comments) { comment ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = comment.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                                            contentDescription = "User",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(comment.fullName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(comment.username, color = Color(0xFF38BDF8), fontSize = 11.sp)
                                        }

                                        // Comment actions for author of comment
                                        if (comment.userId == currentUser?.id) {
                                            var isCommentMenuOpen by remember { mutableStateOf(false) }
                                            Box {
                                                IconButton(
                                                    onClick = { isCommentMenuOpen = true },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = isCommentMenuOpen,
                                                    onDismissRequest = { isCommentMenuOpen = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Edit Comment", fontSize = 12.sp) },
                                                        onClick = {
                                                            isCommentMenuOpen = false
                                                            editingCommentId = comment.id
                                                            editingCommentText = comment.text
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Delete Comment", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            isCommentMenuOpen = false
                                                            coroutineScope.launch {
                                                                DevNetRepository.deleteComment(post.id, comment.id)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(formatTime(comment.createdAt), color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Inline review edit text field vs text representation
                                    if (editingCommentId == comment.id) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                            BasicTextField(
                                                value = editingCommentText,
                                                onValueChange = { editingCommentText = it },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF1E293B))
                                                    .padding(8.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { editingCommentId = null }) {
                                                    Text("Cancel", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = {
                                                        if (editingCommentText.isNotBlank()) {
                                                            coroutineScope.launch {
                                                                DevNetRepository.editComment(post.id, comment.id, editingCommentText.trim())
                                                                editingCommentId = null
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Save", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Text(comment.text, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Bottom row with Comment Like Action & count display
                                    val isCommentLiked = comment.likes.contains(currentUser?.id ?: "")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    DevNetRepository.toggleLikeComment(post.id, comment.id)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCommentLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like Feedback",
                                            tint = if (isCommentLiked) Color(0xFFFB7185) else Color(0xFF64748B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (comment.likes.isNotEmpty()) "${comment.likes.size} Likes" else "Like",
                                            color = if (isCommentLiked) Color(0xFFFB7185) else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = if (isCommentLiked) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add comment row footer
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            if (commentText.isEmpty()) Text("Add tech review statement...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp)
                            inner()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isBlank()) return@IconButton
                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    DevNetRepository.addComment(post.id, commentText.trim())
                                    commentText = ""
                                } catch (e: Exception) {}
                                isSubmitting = false
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .background(Color(0xFF38BDF8), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Review Commit", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// Global Time-formatter helper
fun formatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val netDate = Date(timestamp)
        sdf.format(netDate)
    } catch (e: Exception) {
        "Just now"
    }
}

fun extractYouTubeVideoId(text: String): String? {
    if (text.isBlank()) return null
    // List of prioritized patterns for matching YouTube URL schemes including shorts, live and mobile formats
    val patterns = listOf(
        "youtube\\.com\\/shorts\\/([a-zA-Z0-9_-]{11})",
        "youtube\\.com\\/live\\/([a-zA-Z0-9_-]{11})",
        "youtube\\.com\\/watch\\?v=([a-zA-Z0-9_-]{11})",
        "youtu\\.be\\/([a-zA-Z0-9_-]{11})",
        "youtube\\.com\\/embed\\/([a-zA-Z0-9_-]{11})",
        "youtube\\.com\\/v\\/([a-zA-Z0-9_-]{11})",
        "[?&]v=([a-zA-Z0-9_-]{11})"
    )
    for (pattern in patterns) {
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }
    }
    return null
}

@Composable
fun YouTubePlayerView(videoId: String, modifier: Modifier = Modifier) {
    var isLaunched by remember { mutableStateOf(false) }
    
    if (!isLaunched) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .clickable { isLaunched = true },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                contentDescription = "يوتيوب",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Red play box overlay matching YouTube's authentic design branding
            Box(
                modifier = Modifier
                    .size(68.dp, 48.dp)
                    .background(Color(0xFFEF4444), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    } else {
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    // Transparent background to prevent white flashes
                    setBackgroundColor(0)
                    
                    // Enable standard web settings required for modern hardware-accelerated video
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.databaseEnabled = true
                    
                    // Allow mixed content for proper embed resource loading
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        try {
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        } catch (e: Exception) {}
                    }
                    
                    // Set up WebView and WebChrome clients so video controls, fullscreen, and playbacks work
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            return false // keeps the playback inside our WebView
                        }
                    }
                    webChromeClient = android.webkit.WebChromeClient()
                    
                    // Explicitly use hardware layer type to ensure video plays back beautifully
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    
                    // Load the official YouTube Embed URL directly which is much more reliable and handles video controls beautifully
                    loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&vq=hd720&playsinline=1&rel=0&controls=1")
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
        )
    }
}
