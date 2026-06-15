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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.gumah.devnet.data.DevJob
import com.gumah.devnet.data.DevProject
import com.gumah.devnet.data.TechGroup
import com.gumah.devnet.data.DevNetRepository
import com.gumah.devnet.data.CloudinaryHelper
import kotlinx.coroutines.launch

// 1. PROJECTS SCREEN
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    val projects by DevNetRepository.projects.collectAsState()
    var isCreateOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Project Repositories", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Button(
                    onClick = { isCreateOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Project", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No repositories shared by engineers yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { proj ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = proj.avatarUrl.ifEmpty { "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg" },
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(proj.fullName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(proj.username, color = Color(0xFF38BDF8), fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(proj.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(proj.description, color = Color(0xFF94A3B8), fontSize = 13.sp)

                                if (proj.imageUrls.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AsyncImage(
                                        model = proj.imageUrls.first(),
                                        contentDescription = "Project Screenshot",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                if (proj.fileUrls.isNotEmpty()) {
                                    val fileUrl = proj.fileUrls.first()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .clickable {
                                                try {
                                                    uriHandler.openUri(fileUrl)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to download payload: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Archive, contentDescription = "", tint = Color(0xFF2DD4BF), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("project_bundle_payload.zip", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTime(proj.createdAt),
                                        color = Color(0xFF475569),
                                        fontSize = 11.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (proj.githubUrl.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    val url = if (proj.githubUrl.startsWith("http://") || proj.githubUrl.startsWith("https://")) {
                                                        proj.githubUrl
                                                    } else {
                                                        "https://" + proj.githubUrl
                                                    }
                                                    try {
                                                        uriHandler.openUri(url)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not open github: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.background(Color(0xFF0F172A), CircleShape).size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Terminal, contentDescription = "Terminal Link", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (proj.liveUrl.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    val url = if (proj.liveUrl.startsWith("http://") || proj.liveUrl.startsWith("https://")) {
                                                        proj.liveUrl
                                                    } else {
                                                        "https://" + proj.liveUrl
                                                    }
                                                    try {
                                                        uriHandler.openUri(url)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not open live app: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.background(Color(0xFF0F172A), CircleShape).size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Language, contentDescription = "Web Link", tint = Color(0xFF2DD4BF), modifier = Modifier.size(16.dp))
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

        if (isCreateOpen) {
            Dialog(onDismissRequest = { isCreateOpen = false }) {
                var title by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                var githubUrl by remember { mutableStateOf("") }
                var liveUrl by remember { mutableStateOf("") }

                val attachedImageUrls = remember { mutableStateListOf<String>() }
                val attachedFileUrls = remember { mutableStateListOf<String>() }
                val attachedFileNames = remember { mutableStateListOf<String>() }
                var isUploadingCloudinary by remember { mutableStateOf(false) }

                val projectPhotoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        isUploadingCloudinary = true
                        coroutineScope.launch {
                            val url = CloudinaryHelper.uploadUri(context, uri, "image")
                            if (url != null) {
                                attachedImageUrls.add(url)
                            } else {
                                Toast.makeText(context, "Screenshot upload failed", Toast.LENGTH_SHORT).show()
                            }
                            isUploadingCloudinary = false
                        }
                    }
                }

                val projectFilePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        isUploadingCloudinary = true
                        coroutineScope.launch {
                            val url = CloudinaryHelper.uploadUri(context, uri, "raw", "project_asset_${System.currentTimeMillis()}")
                            if (url != null) {
                                attachedFileUrls.add(url)
                                val originalName = uri.path?.substringAfterLast("/") ?: "archive.zip"
                                attachedFileNames.add(originalName)
                            } else {
                                Toast.makeText(context, "Asset upload failed", Toast.LENGTH_SHORT).show()
                            }
                            isUploadingCloudinary = false
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Share Open Source Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Project Title") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Short Description") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = githubUrl,
                            onValueChange = { githubUrl = it },
                            label = { Text("Code Repository URL") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = liveUrl,
                            onValueChange = { liveUrl = it },
                            label = { Text("Live Production URL") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        // Upload triggers
                        Text("Attach Project Assets:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { projectPhotoPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Screen", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            }

                            Button(
                                onClick = { projectFilePicker.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "", tint = Color(0xFF2DD4BF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Zip", color = Color(0xFF2DD4BF), fontSize = 11.sp)
                            }
                        }

                        if (isUploadingCloudinary) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading elements onto Cloudinary...", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            }
                        }

                        // Preview attachments list
                        if (attachedImageUrls.isNotEmpty() || attachedFileNames.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                attachedImageUrls.forEach { url ->
                                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
                                        AsyncImage(model = url, contentDescription = "", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                                attachedFileNames.forEach { name ->
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F172A))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(name, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isCreateOpen = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
                            Button(
                                onClick = {
                                    if (title.isBlank()) return@Button
                                    coroutineScope.launch {
                                        DevNetRepository.createProject(
                                            title = title,
                                            description = desc,
                                            githubUrl = githubUrl,
                                            liveUrl = liveUrl,
                                            imageUrls = attachedImageUrls.toList(),
                                            fileUrls = attachedFileUrls.toList()
                                        )
                                        isCreateOpen = false
                                        Toast.makeText(context, "Project published successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                            ) {
                                Text("Publish Project", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. TECH GROUPS SCREEN
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val groups by DevNetRepository.groups.collectAsState()
    val currentUser by DevNetRepository.currentUser.collectAsState()
    
    var isCreateOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Developer Communities", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Button(
                    onClick = { isCreateOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DD4BF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Construct Node", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No communities discovered yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { g ->
                        val isUserDocNode = g.members.contains(currentUser?.id)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(g.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(g.category, color = Color(0xFF2DD4BF), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(g.description, color = Color(0xFF94A3B8), fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, contentDescription = "", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${g.members.size} programmers active", color = Color(0xFF64748B), fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                DevNetRepository.joinsGroup(g.id)
                                                Toast.makeText(context, if (isUserDocNode) "Left community node." else "Constructed connection to community node!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isUserDocNode) Color(0xFF334155) else Color(0xFF2DD4BF)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isUserDocNode) "Exit Node" else "Connect Node",
                                            color = if (isUserDocNode) Color.White else Color(0xFF0F172A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isCreateOpen) {
            Dialog(onDismissRequest = { isCreateOpen = false }) {
                var name by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                var category by remember { mutableStateOf("Systems Dev") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Assemble Developer Community", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Community Name") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Core Mission / Description") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Technology Node (e.g. AI, Systems, Web)") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isCreateOpen = false }) { Text("Abort", color = Color(0xFF94A3B8)) }
                            Button(
                                onClick = {
                                    if (name.isBlank()) return@Button
                                    coroutineScope.launch {
                                        DevNetRepository.createGroup(name, desc, category, "")
                                        isCreateOpen = false
                                        Toast.makeText(context, "Node initialized!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DD4BF))
                            ) {
                                Text("Construct Node", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. JOBS BOARD SCREEN
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val jobs by DevNetRepository.jobs.collectAsState()
    val currentUser by DevNetRepository.currentUser.collectAsState()
    
    var isCreateOpen by remember { mutableStateOf(false) }
    var locationFilter by remember { mutableStateOf("All Locations") }

    val filteredJobs = remember(jobs, locationFilter) {
        if (locationFilter == "All Locations") jobs
        else jobs.filter { it.location.equals(locationFilter, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Developer Vacancies", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Button(
                    onClick = { isCreateOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.WorkHistory, contentDescription = "")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Announce Role", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Location Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Locations", "Remote", "Onsite", "Hybrid").forEach { filter ->
                    val isSelected = locationFilter == filter
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B), RoundedCornerShape(20.dp))
                            .clickable { locationFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(filter, color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No career vacancies posted on this filter.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredJobs) { job ->
                        val hasApplied = job.appliedUsers.contains(currentUser?.id)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(job.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(job.companyName, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(job.location.uppercase(), color = Color(0xFF2DD4BF), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(job.description, color = Color(0xFF94A3B8), fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    job.tags.forEach { tag ->
                                        Text("#$tag", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = Color(0xFF334155), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("RECOMPENSE", color = Color(0xFF475569), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(job.salary, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (hasApplied) return@Button
                                            coroutineScope.launch {
                                                DevNetRepository.applyForJob(job.id)
                                                Toast.makeText(context, "Developer index payload transmitted to recruiter!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hasApplied) Color(0xFF334155) else Color(0xFF38BDF8)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !hasApplied
                                    ) {
                                        Text(
                                            text = if (hasApplied) "Application Sent" else "Apply Instantly",
                                            color = if (hasApplied) Color(0xFF94A3B8) else Color(0xFF0F172A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isCreateOpen) {
            Dialog(onDismissRequest = { isCreateOpen = false }) {
                var title by remember { mutableStateOf("") }
                var company by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                var salary by remember { mutableStateOf("$100k - $130k") }
                var locType by remember { mutableStateOf("Remote") }
                var tags by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Announce Developer Role", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Role Title (e.g. Kotlin Architect)") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company Name") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Job Description") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = salary,
                            onValueChange = { salary = it },
                            label = { Text("Salary Band") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Skills required (space separated)") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isCreateOpen = false }) { Text("Abort", color = Color(0xFF94A3B8)) }
                            Button(
                                onClick = {
                                    if (title.isBlank() || company.isBlank()) return@Button
                                    coroutineScope.launch {
                                        val parsedTags = tags.split(" ").filter { it.isNotEmpty() }
                                        DevNetRepository.postJob(
                                            title = title,
                                            company = company,
                                            logoUrl = "",
                                            description = desc,
                                            reqs = emptyList(),
                                            tags = parsedTags,
                                            loc = locType,
                                            salary = salary,
                                            exp = 3,
                                            country = "Global"
                                        )
                                        isCreateOpen = false
                                        Toast.makeText(context, "Vacancy registered!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                            ) {
                                Text("Publish Vacancy", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. NOTIFICATIONS TIMELINE SCREEN
@Composable
fun NotificationsScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val notifications by DevNetRepository.notifications.collectAsState()

    LaunchedEffect(Unit) {
        DevNetRepository.markAllNotificationsAsRead()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Workspace Signals",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }
            Divider(color = Color(0xFF334155), thickness = 0.5.dp)

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Inbox quiet. No signals compiled.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(notifications) { notif ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (notif.type == "message") {
                                        onNavigateToChat(notif.referenceId)
                                    } else {
                                        onNavigateToProfile(notif.senderId)
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon indicator
                            val (iconImg, iconTint) = when (notif.type) {
                                "like" -> Icons.Default.Favorite to Color(0xFFFB7185)
                                "comment" -> Icons.Default.Comment to Color(0xFF38BDF8)
                                "message" -> Icons.Default.Chat to Color(0xFF2DD4BF)
                                "follow" -> Icons.Default.PersonAdd to Color(0xFF38BDF8)
                                else -> Icons.Default.Notifications to Color(0xFF94A3B8)
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconImg, contentDescription = "", tint = iconTint, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatTime(notif.createdAt), color = Color(0xFF64748B), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}
