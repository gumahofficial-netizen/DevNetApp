package com.gumah.devnet.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gumah.devnet.data.CloudinaryHelper
import com.gumah.devnet.data.DevNetRepository
import kotlinx.coroutines.launch

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    val themeColor = MaterialTheme.colorScheme.primary
    val alternateColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // High-tech circular neural ring design
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(themeColor.copy(alpha = 0.9f), alternateColor.copy(alpha = 0.9f))
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Brush.linearGradient(listOf(alternateColor, themeColor)), CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖", // AI Platform Symbol
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DevNet",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp,
                    color = onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                // AI Badge
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFFC084FC))),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AI",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Studio AI Platform",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var passwordVisible by remember { mutableStateFlowOf(false) }
    var rememberMe by remember { mutableStateFlowOf(true) }
    var isLoading by remember { mutableStateFlowOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(modifier = Modifier.padding(bottom = 32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Access your programmer workspace",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 24.dp)
                    )

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Developer Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "EmailIcon", tint = Color(0xFF38BDF8)) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Security Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockIcon", tint = Color(0xFF38BDF8)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "TogglePassword",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                            )
                            Text("Remember me", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                        Text(
                            text = "Forgot Code?",
                            color = Color(0xFF38BDF8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Redirecting to password recovery...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please enter correct email and password", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    DevNetRepository.loginUser(email.trim(), password)
                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error logging in: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Initialize Session", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New developer here?", color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign Up Workspace",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateFlowOf("") }
    var username by remember { mutableStateFlowOf("") }
    var email by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var confirmPassword by remember { mutableStateFlowOf("") }
    var bio by remember { mutableStateFlowOf("") }
    var specialty by remember { mutableStateFlowOf("Fullstack Engineer") }
    var skillsInput by remember { mutableStateFlowOf("") }
    var githubUrl by remember { mutableStateFlowOf("") }
    var linkedinUrl by remember { mutableStateFlowOf("") }
    var websiteUrl by remember { mutableStateFlowOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUrl by remember { mutableStateFlowOf("https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.png") }
    var isLoading by remember { mutableStateFlowOf(false) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            isLoading = true
            coroutineScope.launch {
                val cloudUrl = CloudinaryHelper.uploadUri(context, uri, "image")
                if (cloudUrl != null) {
                    avatarUrl = cloudUrl
                    // Silent success, no intrusive toast popped up
                } else {
                    Toast.makeText(context, "Upload failed. Please check network.", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }
        }
    }

    val specialties = listOf(
        "Fullstack Engineer", "Backend Architect", "Frontend Dev", 
        "Android/iOS Mobile Dev", "AI/ML Scientist", "DevOps & Cloud Specialist", "Systems Programmer"
    )
    var expandedSpecialtyDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AppLogo(modifier = Modifier.padding(bottom = 24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create Workspace",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Build your developer profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 20.dp)
                    )

                    // Avatar compiler
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clickable { logoPicker.launch("image/*") }
                            .border(3.dp, Color(0xFF38BDF8), CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Default Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color.White
                            )
                        }
                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(36.dp))
                        }
                    }
                    Text(
                        "Upload avatar",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )

                    // Inputs block
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
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
                        value = username,
                        onValueChange = { 
                            // prefix checking helper
                            username = if (!it.startsWith("@") && it.isNotEmpty()) "@$it" else it
                        },
                        label = { Text("Username starts with @") },
                        placeholder = { Text("@username") },
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
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Compile Password") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    // Specialty Selector
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)) {
                        OutlinedTextField(
                            value = specialty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Programming Specialty") },
                            trailingIcon = {
                                IconButton(onClick = { expandedSpecialtyDropdown = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", tint = Color(0xFF38BDF8))
                                }
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedSpecialtyDropdown,
                            onDismissRequest = { expandedSpecialtyDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B))
                        ) {
                            specialties.forEach { spec ->
                                DropdownMenuItem(
                                    text = { Text(spec, color = Color.White) },
                                    onClick = {
                                        specialty = spec
                                        expandedSpecialtyDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = skillsInput,
                        onValueChange = { skillsInput = it },
                        label = { Text("Skills (comma-separated, e.g. Kotlin, Docker)") },
                        placeholder = { Text("Kotlin, Compose, Room, Rails") },
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
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Developer Bio / Intro") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        maxLines = 3
                    )

                    // Extra credentials
                    OutlinedTextField(
                        value = githubUrl,
                        onValueChange = { githubUrl = it },
                        label = { Text("GitHub Profile Link") },
                        placeholder = { Text("https://github.com/torvalds") },
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
                        value = linkedinUrl,
                        onValueChange = { linkedinUrl = it },
                        label = { Text("LinkedIn Link") },
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
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Developer Website") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (fullName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "All primary fields are required!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (!username.startsWith("@")) {
                                Toast.makeText(context, "Username must start with @", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val skillsList = skillsInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    
                                    DevNetRepository.registerUser(
                                        emailAddress = email.trim(),
                                        passwordRaw = password,
                                        fullName = fullName.trim(),
                                        username = username.trim(),
                                        bio = bio.trim(),
                                        specialty = specialty,
                                        skills = skillsList,
                                        avatarUrl = avatarUrl,
                                        githubUrl = githubUrl.trim(),
                                        linkedinUrl = linkedinUrl.trim(),
                                        websiteUrl = websiteUrl.trim()
                                    )
                                    Toast.makeText(context, "Developer registration success!", Toast.LENGTH_LONG).show()
                                    onSignUpSuccess()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Registration error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DD4BF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Compile & Register", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already registered?", color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign In Workspace",
                    color = Color(0xFF2DD4BF),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Utility state helper
private fun <T> mutableStateFlowOf(value: T): MutableState<T> {
    return mutableStateOf(value)
}
