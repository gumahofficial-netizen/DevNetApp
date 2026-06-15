package com.gumah.devnet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gumah.devnet.data.DevNetRepository
import com.gumah.devnet.data.UserProfile

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToProfile: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("Engineers") } // Engineers, Posts, ProjectRepos, Tags

    val developers by DevNetRepository.developers.collectAsState()
    val posts by DevNetRepository.posts.collectAsState()
    val projects by DevNetRepository.projects.collectAsState()

    val popularSuggestions = listOf("Kotlin", "Compose", "Docker", "Room", "Firebase", "Rust", "C++", "C")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Search title
            Text(
                text = "Developer Index",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            )

            // Live Search input field
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
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
                    Icon(Icons.Default.Search, contentDescription = "Locate", tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Locate coder, tag, repository or keyword...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                            inner()
                        }
                    )
                }
            }

            // Quick keywords suggestions row
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                popularSuggestions.forEach { suggest ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                            .clickable { query = suggest }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$suggest",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Filters Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Engineers", "Posts", "ProjectRepos").forEach { cat ->
                    val isSelected = searchCategory == cat
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .border(if (isSelected) 0.dp else 1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .clickable { searchCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))

            // Body lazy lists matching search filters
            Box(modifier = Modifier.weight(1f)) {
                when (searchCategory) {
                    "Engineers" -> {
                        val filteredDevelopers = remember(developers, query) {
                            developers.filter {
                                it.fullName.lowercase().contains(query.lowercase()) ||
                                it.username.lowercase().contains(query.lowercase()) ||
                                it.specialty.lowercase().contains(query.lowercase()) ||
                                it.skills.any { skill -> skill.lowercase().contains(query.lowercase()) }
                            }
                        }

                        if (filteredDevelopers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No engineers detected in sandbox database.", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                                items(filteredDevelopers) { dev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToProfile(dev.id) }
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = dev.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                                            contentDescription = "Dev Avatar",
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dev.fullName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(dev.username, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Normal)
                                            Text(dev.specialty, color = Color(0xFF2DD4BF), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = "", tint = Color(0xFF475569))
                                    }
                                    Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                                }
                            }
                        }
                    }
                    "Posts" -> {
                        val filteredPosts = remember(posts, query) {
                            posts.filter {
                                it.text.lowercase().contains(query.lowercase()) ||
                                it.tags.any { tag -> tag.lowercase().contains(query.lowercase()) } ||
                                it.codeSnippet.lowercase().contains(query.lowercase())
                            }
                        }

                        if (filteredPosts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No compiled codes discovered with input sequence.", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                                items(filteredPosts) { post ->
                                    PostCard(
                                        post = post,
                                        currentUserId = "",
                                        onProfileClick = { onNavigateToProfile(post.userId) },
                                        onLikeClick = {},
                                        onCommentClick = {},
                                        onSaveClick = {}
                                    )
                                }
                            }
                        }
                    }
                    "ProjectRepos" -> {
                        val filteredProjects = remember(projects, query) {
                            projects.filter {
                                it.title.lowercase().contains(query.lowercase()) ||
                                it.description.lowercase().contains(query.lowercase())
                            }
                        }

                        if (filteredProjects.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No shared creative codes / directories uncovered.", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredProjects) { proj ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            onNavigateToProfile(proj.userId)
                                        },
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
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(proj.description, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                AsyncImage(
                                                    model = proj.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                                                    contentDescription = "",
                                                    modifier = Modifier.size(16.dp).clip(CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("By ${proj.username}", color = Color(0xFF2DD4BF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
