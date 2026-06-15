package com.gumah.devnet.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.gumah.devnet.data.CloudinaryHelper
import com.gumah.devnet.data.UserProfile
import com.gumah.devnet.data.DevNetRepository
import com.gumah.devnet.data.BadgeType
import com.gumah.devnet.data.getBadgesForProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by DevNetRepository.currentUser.collectAsState()
    val developers by DevNetRepository.developers.collectAsState()
    val posts by DevNetRepository.posts.collectAsState()
    val projects by DevNetRepository.projects.collectAsState()
    val isProfileLoading by DevNetRepository.isProfileLoading.collectAsState()

    val profile = remember(developers, userId, currentUser) {
        developers.find { it.id == userId } ?: currentUser
    }

    var selectedTab by remember { mutableStateOf("Posts") } // Posts, Projects, Settings/Info
    var isEditingOpen by remember { mutableStateOf(false) }

    if (profile == null || isProfileLoading) {
        ProfileSkeletonScreen(onNavigateBack = onNavigateBack)
        return
    }

    val isOwnProfile = profile.id == currentUser?.id
    val isFollowing = currentUser?.following?.contains(profile.id) == true

    // Filtered data for this profiles
    val userFeedPosts = remember(posts, profile.id) {
        posts.filter { it.userId == profile.id }
    }
    val userProjects = remember(projects, profile.id) {
        projects.filter { it.userId == profile.id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Cover Image Canvas Space Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
            ) {
                if (profile.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profile.coverUrl,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Modern Terminal Background drawing canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF1E1E38), Color(0xFF0F172A))
                            )
                        )
                    }
                }

                // Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }

            // Profile info body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 20.dp)
            ) {
                // Large Avatar + Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AsyncImage(
                        model = profile.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                        contentDescription = "Avatar Detail",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF0F172A), CircleShape)
                            .border(4.dp, Color(0xFF38BDF8), CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isOwnProfile) {
                            Button(
                                onClick = { isEditingOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Hub", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Direct Message send
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val conv = DevNetRepository.startConversation(profile.id)
                                        onNavigateToChat(conv.id)
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.dp, Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Direct Msg", tint = Color(0xFF38BDF8))
                            }

                            // Follow/Unfollow Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        DevNetRepository.toggleFollowUser(profile.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFF1E293B) else Color(0xFF2DD4BF)
                                ),
                                border = if (isFollowing) BorderStroke(1.dp, Color(0xFF334155)) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isFollowing) "Unfollow" else "Follow Dev",
                                    color = if (isFollowing) Color.White else Color(0xFF0F172A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name & Spec
                Text(profile.fullName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(profile.username, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = "", tint = Color(0xFF2DD4BF), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(profile.specialty, color = Color(0xFF2DD4BF), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                // Dynamic Badges Row
                val badges = remember(profile) { getBadgesForProfile(profile) }
                if (badges.isNotEmpty()) {
                    var selectedBadgeForDialog by remember { mutableStateOf<BadgeType?>(null) }
                    
                    Text(
                        text = "الأوسمة والجوائز البرمجية:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        badges.forEach { badge ->
                            BadgeChip(badge = badge, onClick = { selectedBadgeForDialog = badge })
                        }
                    }
                    
                    // Show Badge details dialogue when clicked
                    selectedBadgeForDialog?.let { badge ->
                        AlertDialog(
                            onDismissRequest = { selectedBadgeForDialog = null },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val parsedColor = remember(badge.hexColor) {
                                        try { Color(android.graphics.Color.parseColor(badge.hexColor)) } catch (e: Exception) { Color(0xFF38BDF8) }
                                    }
                                    val badgeIcon = when (badge.iconName) {
                                        "Star" -> Icons.Default.Star
                                        "Favorite" -> Icons.Default.Favorite
                                        "Layers" -> Icons.Default.Terminal
                                        "Android" -> Icons.Default.Android
                                        else -> Icons.Default.Star
                                    }
                                    Icon(badgeIcon, contentDescription = null, tint = parsedColor, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(badge.arabicName, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            text = {
                                Column {
                                    Text(badge.englishName, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(badge.descriptionName, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { selectedBadgeForDialog = null }) {
                                    Text("حسناً", color = Color(0xFF38BDF8))
                                }
                            },
                            containerColor = Color(0xFF1E293B),
                            titleContentColor = Color.White,
                            textContentColor = Color.White
                        )
                    }
                }

                // Web links
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (profile.githubUrl.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val url = if (profile.githubUrl.startsWith("http://") || profile.githubUrl.startsWith("https://")) {
                                    profile.githubUrl
                                } else {
                                    "https://" + profile.githubUrl
                                }
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = "GitHub", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GitHub", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                    if (profile.linkedinUrl.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val url = if (profile.linkedinUrl.startsWith("http://") || profile.linkedinUrl.startsWith("https://")) {
                                    profile.linkedinUrl
                                } else {
                                    "https://" + profile.linkedinUrl
                                }
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "LinkedIn", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LinkedIn", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                    if (profile.websiteUrl.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val url = if (profile.websiteUrl.startsWith("http://") || profile.websiteUrl.startsWith("https://")) {
                                    profile.websiteUrl
                                } else {
                                    "https://" + profile.websiteUrl
                                }
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Website", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Web", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = profile.followers.size.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Followers", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = profile.following.size.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Following", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = userFeedPosts.size.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Posts", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                // Dev Bio Intro
                if (profile.bio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile.bio,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Skills grid list chips
                if (profile.skills.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        profile.skills.forEach { skill ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(skill, color = Color(0xFF38BDF8), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Tab rows: Posts, Projects, settings/options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Posts", "Projects Hub", "Settings Panel").forEach { tab ->
                    val isSelected = selectedTab == tab
                    val colorBrush = if (isSelected) Color(0xFF38BDF8) else Color.Transparent

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(2.dp)
                                .background(colorBrush)
                        )
                    }
                }
            }

            Divider(color = Color(0xFF334155), thickness = 0.5.dp)

            // Dynamic contents based on selected tab
            when (selectedTab) {
                "Posts" -> {
                    if (userFeedPosts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No posts broadcasted on this profile.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                            items(userFeedPosts) { post ->
                                PostCard(
                                    post = post,
                                    currentUserId = currentUser?.id ?: "",
                                    onProfileClick = {},
                                    onLikeClick = {
                                        coroutineScope.launch { DevNetRepository.toggleLikePost(post.id) }
                                    },
                                    onCommentClick = {},
                                    onSaveClick = {
                                        coroutineScope.launch { DevNetRepository.toggleSavePost(post.id) }
                                    }
                                )
                            }
                        }
                    }
                }
                "Projects Hub" -> {
                    if (userProjects.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No repositories shared in this project hub.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(userProjects) { proj ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(proj.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Icon(Icons.Default.Folder, contentDescription = "", tint = Color(0xFF38BDF8))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(proj.description, color = Color(0xFF94A3B8), fontSize = 12.sp)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (proj.githubUrl.isNotEmpty()) {
                                                IconButton(onClick = { val url = if (proj.githubUrl.startsWith("http://") || proj.githubUrl.startsWith("https://")) {
                                                         proj.githubUrl
                                                     } else {
                                                         "https://" + proj.githubUrl
                                                     }
                                                     try {
                                                         uriHandler.openUri(url)
                                                     } catch (e: Exception) {
                                                         Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                                     } }) {
                                                    Icon(Icons.Default.Terminal, contentDescription = "GitHub", tint = Color(0xFF38BDF8))
                                                }
                                            }
                                            if (proj.liveUrl.isNotEmpty()) {
                                                IconButton(onClick = { val url = if (proj.liveUrl.startsWith("http://") || proj.liveUrl.startsWith("https://")) {
                                                         proj.liveUrl
                                                     } else {
                                                         "https://" + proj.liveUrl
                                                     }
                                                     try {
                                                         uriHandler.openUri(url)
                                                     } catch (e: Exception) {
                                                         Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                                     } }) {
                                                    Icon(Icons.Default.Language, contentDescription = "URL", tint = Color(0xFF2DD4BF))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Settings Panel" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("WORKSPACE PREFERENCES", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                        // Report Abusive option
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Report registered. Security team triaging profile...", Toast.LENGTH_LONG).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Report, contentDescription = "", tint = Color(0xFFFB7185))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Report Abusive Content", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Report policy violations or harmful files", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }
                        }

                        // Block developer option
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Coder blocked successfully.", Toast.LENGTH_LONG).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Block, contentDescription = "", tint = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Block Developer", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Disallow this account from calling your main threads", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }
                        }

                        // Exit/Logout button panel
                        if (isOwnProfile) {
                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "", tint = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("TERMINATE SESSION (SIGN OUT)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "Permanently auditing user storage... Account terminated.", Toast.LENGTH_LONG).show()
                                    onLogout()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("DESTRUCT ACCOUNT (PERMANENT)", color = Color(0xFFE11D48), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Edit Profile Dialog Modal
        if (isEditingOpen) {
            EditProfileDialog(
                profile = currentUser ?: profile,
                onDismiss = { isEditingOpen = false },
                onSave = { updated ->
                    coroutineScope.launch {
                        DevNetRepository.updateUserProfile(updated)
                        isEditingOpen = false
                        Toast.makeText(context, "Developer credentials synced to Firestore!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(profile.fullName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var skills by remember { mutableStateOf(profile.skills.joinToString(", ")) }
    var specialty by remember { mutableStateOf(profile.specialty) }
    var avatar by remember { mutableStateOf(profile.avatarUrl) }
    var cover by remember { mutableStateOf(profile.coverUrl) }

    var isUploading by remember { mutableStateOf(false) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            coroutine.launch {
                val url = CloudinaryHelper.uploadUri(context, uri, "image")
                if (url != null) avatar = url
                isUploading = false
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            coroutine.launch {
                val url = CloudinaryHelper.uploadUri(context, uri, "image")
                if (url != null) cover = url
                isUploading = false
            }
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modify Profile Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "", tint = Color.White)
                    }
                }
                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                // Photo pickers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .clickable { logoPicker.launch("image/*") }
                        ) {
                            AsyncImage(model = avatar, contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("Avatar", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .clickable { coverPicker.launch("image/*") }
                        ) {
                            if (cover.isNotEmpty()) {
                                AsyncImage(model = cover, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("Cover Banner", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                if (isUploading) {
                    LinearProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Display Name") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Technical Specialty") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = skills,
                    onValueChange = { skills = it },
                    label = { Text("Developer Skills (comma separated)") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biography Text") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        val parsedSkillsList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onSave(
                            profile.copy(
                                fullName = fullName.trim(),
                                bio = bio.trim(),
                                specialty = specialty.trim(),
                                skills = parsedSkillsList,
                                avatarUrl = avatar,
                                coverUrl = cover
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Code Sync", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileSkeletonScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Cover Image Canvas Space Banner
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(0.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-45).dp)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Avatar outline shimmer
                    ShimmerItem(
                        modifier = Modifier
                            .size(90.dp)
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                        shape = CircleShape
                    )
                    
                    // Top buttons placeholder
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerItem(
                            modifier = Modifier
                                .width(90.dp)
                                .height(36.dp),
                            shape = RoundedCornerShape(20.dp)
                        )
                        ShimmerItem(
                            modifier = Modifier
                                .width(40.dp)
                                .height(36.dp),
                            shape = CircleShape
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name tag / Bio Shimmer
                ShimmerItem(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerItem(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(13.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                ShimmerItem(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bio Description lines
                ShimmerItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerItem(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stats rows Shimmer block
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ShimmerItem(modifier = Modifier.width(36.dp).height(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                ShimmerItem(modifier = Modifier.width(52.dp).height(10.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tabs outline shimmer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) {
                        ShimmerItem(
                            modifier = Modifier
                                .width(70.dp)
                                .height(32.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Posts skeletal preview inside profile page
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerItem(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
                        ShimmerItem(modifier = Modifier.fillMaxWidth().height(44.dp))
                        ShimmerItem(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp))
                    }
                }
            }
        }

        // Back action button flotation block 
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

@Composable
fun BadgeChip(badge: BadgeType, onClick: () -> Unit) {
    val parsedColor = remember(badge.hexColor) {
        try {
            Color(android.graphics.Color.parseColor(badge.hexColor))
        } catch (e: Exception) {
            Color(0xFF38BDF8)
        }
    }
    
    val badgeIcon = when (badge.iconName) {
        "Star" -> Icons.Default.Star
        "Favorite" -> Icons.Default.Favorite
        "Layers" -> Icons.Default.Terminal
        "Android" -> Icons.Default.Android
        else -> Icons.Default.Star
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(parsedColor.copy(alpha = 0.15f))
            .border(1.dp, parsedColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = badgeIcon,
            contentDescription = null,
            tint = parsedColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = badge.arabicName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
