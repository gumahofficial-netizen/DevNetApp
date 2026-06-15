package com.gumah.devnet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gumah.devnet.data.DevNetRepository
import com.gumah.devnet.ui.screens.*
import com.gumah.devnet.ui.theme.DevNetTheme

sealed class Screen {
    object Login : Screen()
    object SignUp : Screen()
    object MainApp : Screen()
    data class ProfileDetail(val userId: String) : Screen()
    data class ChatRoom(val conversationId: String) : Screen()
    object Inbox : Screen()
    object Signals : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DevNetRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val isDark by com.gumah.devnet.data.DevNetRepository.isDarkThemeSystem.collectAsState()
            DevNetTheme(darkTheme = isDark) {
                DevNetNavigationRoot()
            }
        }
    }
}

@Composable
fun DevNetNavigationRoot() {
    val initialScreen = remember {
        if (DevNetRepository.currentUser.value != null) Screen.MainApp else Screen.Login
    }
    val screenStack = remember { mutableStateListOf<Screen>(initialScreen) }
    val currentScreen = screenStack.last()

    val currentUser by DevNetRepository.currentUser.collectAsState()

    fun navigateTo(screen: Screen) {
        screenStack.add(screen)
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    BackHandler(enabled = screenStack.size > 1) {
        navigateBack()
    }

    // Auto navigate to Feed if user is already signed in
    LaunchedEffect(currentUser) {
        if (currentUser != null && (currentScreen is Screen.Login || currentScreen is Screen.SignUp)) {
            // Clear stack and make MainApp root
            screenStack.clear()
            screenStack.add(Screen.MainApp)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Login -> {
                    LoginScreen(
                        onNavigateToSignUp = { navigateTo(Screen.SignUp) },
                        onLoginSuccess = {
                            // Automatically handled by LaunchedEffect(currentUser)
                        }
                    )
                }
                is Screen.SignUp -> {
                    SignUpScreen(
                        onNavigateToLogin = { navigateTo(Screen.Login) },
                        onSignUpSuccess = {
                            // Automatically handled by LaunchedEffect(currentUser)
                        }
                    )
                }
                is Screen.MainApp -> {
                    MainAppScaffold(
                        onNavigateToProfile = { userId -> navigateTo(Screen.ProfileDetail(userId)) },
                        onNavigateToChat = { convId -> navigateTo(Screen.ChatRoom(convId)) },
                        onNavigateToInbox = { navigateTo(Screen.Inbox) },
                        onNavigateToSignals = { navigateTo(Screen.Signals) },
                        onLogout = {
                            DevNetRepository.logout()
                            screenStack.clear()
                            screenStack.add(Screen.Login)
                        }
                    )
                }
                is Screen.ProfileDetail -> {
                    ProfileScreen(
                        userId = screen.userId,
                        onNavigateBack = { navigateBack() },
                        onLogout = {
                            DevNetRepository.logout()
                            screenStack.clear()
                            screenStack.add(Screen.Login)
                        },
                        onNavigateToChat = { uid ->
                            navigateTo(Screen.ChatRoom(uid))
                        }
                    )
                }
                is Screen.ChatRoom -> {
                    ChatRoomScreen(
                        conversationId = screen.conversationId,
                        onNavigateBack = { navigateBack() }
                    )
                }
                is Screen.Inbox -> {
                    ConversationsScreen(
                        onNavigateToChatRoom = { convId -> navigateTo(Screen.ChatRoom(convId)) },
                        onNavigateBack = { navigateBack() }
                    )
                }
                is Screen.Signals -> {
                    NotificationsScreen(
                        onNavigateToChat = { convId -> navigateTo(Screen.ChatRoom(convId)) },
                        onNavigateToProfile = { userId -> navigateTo(Screen.ProfileDetail(userId)) },
                        onNavigateBack = { navigateBack() }
                    )
                }
            }
        }
    }
}

// Scaffold hosting the beautiful modern Material 3 Bottom Navigation bar managing multiple tabs
@Composable
fun MainAppScaffold(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSignals: () -> Unit,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf("Feed") } // Feed, Search, Projects, MyProfile
    val currentUser by DevNetRepository.currentUser.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .height(64.dp),
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                // Home/Feed
                NavigationBarItem(
                    selected = activeTab == "Feed",
                    onClick = { activeTab = "Feed" },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Feed") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF0F172A)
                    )
                )

                // Index/Search
                NavigationBarItem(
                    selected = activeTab == "Search",
                    onClick = { activeTab = "Search" },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF0F172A)
                    )
                )

                // Projects Board
                NavigationBarItem(
                    selected = activeTab == "Projects",
                    onClick = { activeTab = "Projects" },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Projects") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF0F172A)
                    )
                )

                // Profile card item
                NavigationBarItem(
                    selected = activeTab == "MyProfile",
                    onClick = { activeTab = "MyProfile" },
                    icon = {
                        AsyncImage(
                            model = currentUser?.avatarUrl ?: "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg",
                            contentDescription = "My Avatar",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF0F172A)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "Feed" -> {
                    FeedScreen(
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToInbox = onNavigateToInbox,
                        onNavigateToSignals = onNavigateToSignals
                    )
                }
                "Search" -> {
                    SearchScreen(
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
                "Projects" -> {
                    ProjectsScreen()
                }
                "MyProfile" -> {
                    currentUser?.id?.let { uid ->
                        ProfileScreen(
                            userId = uid,
                            onNavigateBack = { activeTab = "Feed" },
                            onLogout = onLogout,
                            onNavigateToChat = onNavigateToChat
                        )
                    }
                }
            }
        }
    }
}
