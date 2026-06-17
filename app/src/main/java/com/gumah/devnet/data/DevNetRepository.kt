package com.gumah.devnet.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object DevNetRepository {
    private const val TAG = "DevNetRepository"

    private fun saveUserToPrefs(profile: UserProfile?) {
        val prefs = appContext?.getSharedPreferences("devnet_prefs", Context.MODE_PRIVATE) ?: return
        if (profile == null) {
            prefs.edit().clear().apply()
            return
        }
        prefs.edit().apply {
            putString("user_id", profile.id)
            putString("user_fullName", profile.fullName)
            putString("user_username", profile.username)
            putString("user_email", profile.email)
            putString("user_avatarUrl", profile.avatarUrl)
            putString("user_coverUrl", profile.coverUrl)
            putString("user_bio", profile.bio)
            putString("user_specialty", profile.specialty)
            putString("user_skills", profile.skills.joinToString(","))
            putString("user_githubUrl", profile.githubUrl)
            putString("user_linkedinUrl", profile.linkedinUrl)
            putString("user_websiteUrl", profile.websiteUrl)
            putString("user_followers", profile.followers.joinToString(","))
            putString("user_following", profile.following.joinToString(","))
            putInt("user_postsCount", profile.postsCount)
            putLong("user_joinedAt", profile.joinedAt)
            apply()
        }
    }

    private fun loadUserFromPrefs(): UserProfile? {
        val prefs = appContext?.getSharedPreferences("devnet_prefs", Context.MODE_PRIVATE) ?: return null
        val id = prefs.getString("user_id", null) ?: return null
        val fullName = prefs.getString("user_fullName", "") ?: ""
        val username = prefs.getString("user_username", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val avatarUrl = prefs.getString("user_avatarUrl", "") ?: ""
        val coverUrl = prefs.getString("user_coverUrl", "") ?: ""
        val bio = prefs.getString("user_bio", "") ?: ""
        val specialty = prefs.getString("user_specialty", "") ?: ""
        val skillsStr = prefs.getString("user_skills", "") ?: ""
        val skills = if (skillsStr.isEmpty()) emptyList() else skillsStr.split(",")
        val githubUrl = prefs.getString("user_githubUrl", "") ?: ""
        val linkedinUrl = prefs.getString("user_linkedinUrl", "") ?: ""
        val websiteUrl = prefs.getString("user_websiteUrl", "") ?: ""
        val followersStr = prefs.getString("user_followers", "") ?: ""
        val followers = if (followersStr.isEmpty()) emptyList() else followersStr.split(",")
        val followingStr = prefs.getString("user_following", "") ?: ""
        val following = if (followingStr.isEmpty()) emptyList() else followingStr.split(",")
        val postsCount = prefs.getInt("user_postsCount", 0)
        val joinedAt = prefs.getLong("user_joinedAt", System.currentTimeMillis())

        return UserProfile(
            id = id,
            fullName = fullName,
            username = username,
            email = email,
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            bio = bio,
            specialty = specialty,
            skills = skills,
            githubUrl = githubUrl,
            linkedinUrl = linkedinUrl,
            websiteUrl = websiteUrl,
            followers = followers,
            following = following,
            postsCount = postsCount,
            joinedAt = joinedAt
        )
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }

    private fun saveConversationsToLocal() {
        val context = appContext ?: return
        try {
            val type = Types.newParameterizedType(List::class.java, Conversation::class.java)
            val adapter = moshi.adapter<List<Conversation>>(type)
            val jsonString = adapter.toJson(memoryConversations)
            context.openFileOutput("local_conversations.json", Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
            Log.d(TAG, "Successfully saved conversations locally.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving conversations locally", e)
        }
    }

    private fun loadConversationsFromLocal() {
        val context = appContext ?: return
        try {
            val file = context.getFileStreamPath("local_conversations.json")
            if (file != null && file.exists()) {
                val jsonString = file.readText()
                val type = Types.newParameterizedType(List::class.java, Conversation::class.java)
                val adapter = moshi.adapter<List<Conversation>>(type)
                val loaded = adapter.fromJson(jsonString)
                if (loaded != null) {
                    memoryConversations.clear()
                    memoryConversations.addAll(loaded)
                    _conversations.value = loaded
                    Log.d(TAG, "Loaded conversations locally: ${loaded.size}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading conversations locally", e)
        }
    }

    private fun saveMessagesToLocal() {
        val context = appContext ?: return
        try {
            val type = Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Types.newParameterizedType(List::class.java, Message::class.java)
            )
            val adapter = moshi.adapter<Map<String, List<Message>>>(type)
            val jsonString = adapter.toJson(memoryMessages.mapValues { it.value.toList() })
            context.openFileOutput("local_messages.json", Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
            Log.d(TAG, "Successfully saved messages locally.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving messages locally", e)
        }
    }

    private fun loadMessagesFromLocal() {
        val context = appContext ?: return
        try {
            val file = context.getFileStreamPath("local_messages.json")
            if (file != null && file.exists()) {
                val jsonString = file.readText()
                val type = Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Types.newParameterizedType(List::class.java, Message::class.java)
                )
                val adapter = moshi.adapter<Map<String, List<Message>>>(type)
                val loaded = adapter.fromJson(jsonString)
                if (loaded != null) {
                    memoryMessages.clear()
                    loaded.forEach { (k, v) ->
                        memoryMessages[k] = v.toMutableList()
                    }
                    _messages.value = loaded
                    Log.d(TAG, "Loaded messages locally: ${loaded.size} conversations cached")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading messages locally", e)
        }
    }

    private fun savePostsToLocal() {
        val context = appContext ?: return
        try {
            val type = Types.newParameterizedType(List::class.java, DevPost::class.java)
            val adapter = moshi.adapter<List<DevPost>>(type)
            val jsonString = adapter.toJson(memoryPosts)
            context.openFileOutput("local_posts.json", Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
            Log.d(TAG, "Successfully saved posts locally.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving posts locally", e)
        }
    }

    private fun loadPostsFromLocal() {
        val context = appContext ?: return
        try {
            val file = context.getFileStreamPath("local_posts.json")
            if (file != null && file.exists()) {
                val jsonString = file.readText()
                val type = Types.newParameterizedType(List::class.java, DevPost::class.java)
                val adapter = moshi.adapter<List<DevPost>>(type)
                val loaded = adapter.fromJson(jsonString)
                if (loaded != null && loaded.isNotEmpty()) {
                    memoryPosts.clear()
                    memoryPosts.addAll(loaded)
                    _posts.value = loaded
                    Log.d(TAG, "Loaded posts locally: ${loaded.size}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading posts locally", e)
        }
    }

    // Firebase instances with lazy safe access
    val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth not configured/available. Using local fallback mode.", e)
            null
        }
    }

    val db: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore not configured/available. Using local fallback mode.", e)
            null
        }
    }

    private val fcm: FirebaseMessaging? by lazy {
        try { FirebaseMessaging.getInstance() } catch (e: Exception) {
            Log.d(TAG, "FirebaseMessaging not available.")
            null
        }
    }

    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    var remoteConfig: FirebaseRemoteConfig? = null

    // Real-time State Flows for reactive Compose UI updates
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _posts = MutableStateFlow<List<DevPost>>(emptyList())
    val posts: StateFlow<List<DevPost>> = _posts

    private val _comments = MutableStateFlow<Map<String, List<DevComment>>>(emptyMap()) // postId -> Comments
    val comments: StateFlow<Map<String, List<DevComment>>> = _comments

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap()) // convId -> Messages
    val messages: StateFlow<Map<String, List<Message>>> = _messages

    private val _projects = MutableStateFlow<List<DevProject>>(emptyList())
    val projects: StateFlow<List<DevProject>> = _projects

    private val _groups = MutableStateFlow<List<TechGroup>>(emptyList())
    val groups: StateFlow<List<TechGroup>> = _groups

    private val _jobs = MutableStateFlow<List<DevJob>>(emptyList())
    val jobs: StateFlow<List<DevJob>> = _jobs

    private val _notifications = MutableStateFlow<List<DevNotification>>(emptyList())
    val notifications: StateFlow<List<DevNotification>> = _notifications

    private val _developers = MutableStateFlow<List<UserProfile>>(emptyList())
    val developers: StateFlow<List<UserProfile>> = _developers

    // Global real-time theme selector (supports toggle in top-bar of feeds / profile screens)
    val isDarkThemeSystem = MutableStateFlow(true)

    val isFeedLoading = MutableStateFlow(true)
    val isProfileLoading = MutableStateFlow(true)

    // In-memory Fallback Database (ensures 100% operation even if Firebase fails setup)
    private val memoryUsers = mutableMapOf<String, UserProfile>()
    private val memoryPosts = mutableListOf<DevPost>()
    private val memoryComments = mutableMapOf<String, MutableList<DevComment>>()
    private val memoryConversations = mutableListOf<Conversation>()
    private val memoryMessages = mutableMapOf<String, MutableList<Message>>()
    private val memoryProjects = mutableListOf<DevProject>()
    private val memoryGroups = mutableListOf<TechGroup>()
    private val memoryJobs = mutableListOf<DevJob>()
    private val memoryNotifications = mutableListOf<DevNotification>()
    private var appContext: Context? = null

    fun init(context: Context) {
        this.appContext = context.applicationContext
        try {
            analytics = FirebaseAnalytics.getInstance(context)
            crashlytics = FirebaseCrashlytics.getInstance()
            remoteConfig = FirebaseRemoteConfig.getInstance()
            
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig?.setConfigSettingsAsync(configSettings)
            remoteConfig?.setDefaultsAsync(mapOf("app_title" to "DevNet", "is_registration_allowed" to true))
            remoteConfig?.fetchAndActivate()
            
            fcm?.token?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Registration Token: $token")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing firebase metrics plugins", e)
        }

        // Initialize Mock DevNet data in Memory so feed is never boring or empty at start
        seedInitialDemoData()
        
        // Load cache data if available
        loadPostsFromLocal()
        loadConversationsFromLocal()
        loadMessagesFromLocal()

        syncStateFlows()
        listenToFirebaseData()

        // Load cached user profile from SharedPreferences immediately
        val cachedUserProfile = loadUserFromPrefs()
        if (cachedUserProfile != null) {
            _currentUser.value = cachedUserProfile
            memoryUsers[cachedUserProfile.id] = cachedUserProfile
        }
    }

    private fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        try {
            val bundle = android.os.Bundle()
            params.forEach { (key, valString) -> bundle.putString(key, valString) }
            analytics?.logEvent(name, bundle)
        } catch (e: Exception) {
            Log.d(TAG, "Analytics logEvent offline: $name")
        }
    }

    private fun seedInitialDemoData() {
        val dev1 = UserProfile(
            id = "user_linus",
            fullName = "Linus Torvalds",
            username = "@linus_kernel",
            email = "linus@devnet.com",
            avatarUrl = "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/linus.jpg",
            coverUrl = "",
            bio = "I create operating systems for fun. Author of Linux and Git.",
            specialty = "Kernel Development & C",
            skills = listOf("C", "Git", "Assembly", "Architecture"),
            githubUrl = "https://github.com/torvalds",
            linkedinUrl = "https://linkedin.com",
            websiteUrl = "https://kernel.org"
        )
        val dev2 = UserProfile(
            id = "user_ada",
            fullName = "Ada Lovelace",
            username = "@ada_coder",
            email = "ada@devnet.com",
            avatarUrl = "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/ada.jpg",
            coverUrl = "",
            bio = "First computer programmer. Mathematician. Analytical Engine enthusiast.",
            specialty = "Mathematical Computing",
            skills = listOf("Algorithms", "Math", "Analytical Engines"),
            githubUrl = "https://github.com/ada",
            linkedinUrl = "https://linkedin.com",
            websiteUrl = "https://computes.org"
        )

        memoryUsers[dev1.id] = dev1
        memoryUsers[dev2.id] = dev2

        val post1 = DevPost(
            id = "demo_post_1",
            userId = dev1.id,
            fullName = dev1.fullName,
            username = dev1.username,
            avatarUrl = dev1.avatarUrl,
            text = "Guys, I am thinking of rewriting git in Kotlin Compose... just kidding, C is still king! Check out this kernel macro code.",
            codeSnippet = "#define list_entry(ptr, type, member) \\\n    container_of(ptr, type, member)",
            codeLanguage = "c",
            tags = listOf("C", "Kernel", "Git"),
            likes = listOf(dev2.id),
            repoUrl = "https://github.com/torvalds/linux",
            createdAt = System.currentTimeMillis() - 3600000
        )

        val post2 = DevPost(
            id = "demo_post_2",
            userId = dev2.id,
            fullName = dev2.fullName,
            username = dev2.username,
            avatarUrl = dev2.avatarUrl,
            text = "Excited to share our newest mathematical compute logic! Ready for developers to review. Download the compiled report specs ZIP.",
            fileNames = listOf("analytical_spec.zip"),
            fileUrls = listOf("https://res.cloudinary.com/dqgsepaus/raw/upload/v1718310000/spec.zip"),
            tags = listOf("Algorithms", "AnalyticalEngine"),
            createdAt = System.currentTimeMillis() - 7200000
        )

        memoryPosts.add(post1)
        memoryPosts.add(post2)

        val group1 = TechGroup(
            id = "group_kernel",
            name = "Kernel Hackers Association",
            description = "Low-level system programming, C macros, memory drivers and assembly optimizing.",
            category = "Systems Programming",
            imageUrl = "",
            creatorId = dev1.id,
            members = listOf(dev1.id, dev2.id)
        )

        val group2 = TechGroup(
            id = "group_compose",
            name = "Android UI Artisans",
            description = "Material 3, Edge to Edge layouts, customizable canvas and high performance Compose design.",
            category = "Mobile Dev",
            creatorId = "system",
            members = listOf(dev2.id)
        )

        memoryGroups.add(group1)
        memoryGroups.add(group2)

        val job1 = DevJob(
            id = "job_android",
            title = "Senior Android Compose Engineer",
            companyName = "DevNet Inc.",
            location = "Remote",
            salary = "$120k - $150k",
            experienceYears = 5,
            country = "United States / Global",
            description = "We are seeking an Android engineer expert in Jetpack Compose, Edge-to-Edge styling, and Firestore real-time synchronization.",
            requirements = listOf("Jetpack Compose", "Coroutines", "Room Database", "Material 3"),
            tags = listOf("Kotlin", "Compose", "Android")
        )

        memoryJobs.add(job1)

        val project1 = DevProject(
            id = "project_linux",
            userId = dev1.id,
            fullName = dev1.fullName,
            username = dev1.username,
            avatarUrl = dev1.avatarUrl,
            title = "The Linux Kernel",
            description = "The free and open-source operating system kernel used on servers, devices, and cloud.",
            githubUrl = "https://github.com/torvalds/linux",
            liveUrl = "https://kernel.org",
            likes = listOf(dev2.id)
        )

        memoryProjects.add(project1)
    }

    private fun syncStateFlows() {
        _developers.value = memoryUsers.values.toList()
        _posts.value = memoryPosts.sortedByDescending { it.createdAt }
        _projects.value = memoryProjects.sortedByDescending { it.createdAt }
        _groups.value = memoryGroups
        _jobs.value = memoryJobs.sortedByDescending { it.postedAt }
        _notifications.value = memoryNotifications.sortedByDescending { it.createdAt }
        _conversations.value = memoryConversations.sortedByDescending { it.lastMessageTime }

        // Persist records to local cache sandbox storage asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            savePostsToLocal()
            saveConversationsToLocal()
            saveMessagesToLocal()
        }
    }

    private fun listenToFirebaseData() {
        val firestore = db ?: run {
            isFeedLoading.value = false
            isProfileLoading.value = false
            return
        }

        // Graceful timeout to ensure skeleton screen finishes on network issues
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isFeedLoading.value = false
            isProfileLoading.value = false
        }, 2200)

        // Realtime sync users
        firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error syncing users", error)
                isProfileLoading.value = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = mutableListOf<UserProfile>()
                for (doc in snapshot.documents) {
                    try {
                        val profile = doc.toObject(UserProfile::class.java)
                        if (profile != null) {
                            val finalProfile = if (profile.id.isEmpty()) profile.copy(id = doc.id) else profile
                            list.add(finalProfile)
                            // sync to local fallback map
                            memoryUsers[finalProfile.id] = finalProfile
                            if (finalProfile.id == auth?.currentUser?.uid) {
                                _currentUser.value = finalProfile
                                saveUserToPrefs(finalProfile)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user profile document ${doc.id}", e)
                    }
                }
                _developers.value = list
                isProfileLoading.value = false
            }
        }

        // Realtime sync posts
        firestore.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error syncing posts", error)
                    isFeedLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<DevPost>()
                    for (doc in snapshot.documents) {
                        try {
                            val post = doc.toObject(DevPost::class.java)
                            if (post != null) {
                                val finalPost = if (post.id.isEmpty()) post.copy(id = doc.id) else post
                                list.add(finalPost)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing post document ${doc.id}", e)
                        }
                    }
                    if (list.isNotEmpty()) {
                        memoryPosts.clear()
                        memoryPosts.addAll(list)
                        _posts.value = list
                    } else {
                        _posts.value = memoryPosts.sortedByDescending { it.createdAt }
                    }
                    isFeedLoading.value = false
                }
            }

        // Realtime sync projects
        firestore.collection("projects").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error syncing projects", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<DevProject>()
                    for (doc in snapshot.documents) {
                        try {
                            val project = doc.toObject(DevProject::class.java)
                            if (project != null) {
                                val finalProject = if (project.id.isEmpty()) project.copy(id = doc.id) else project
                                list.add(finalProject)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing project document ${doc.id}", e)
                        }
                    }
                    if (list.isNotEmpty()) {
                        memoryProjects.clear()
                        memoryProjects.addAll(list)
                        _projects.value = list
                    } else {
                        _projects.value = memoryProjects.sortedByDescending { it.createdAt }
                    }
                }
            }

        // Realtime sync jobs
        firestore.collection("jobs").orderBy("postedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error syncing jobs", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<DevJob>()
                    for (doc in snapshot.documents) {
                        try {
                            val job = doc.toObject(DevJob::class.java)
                            if (job != null) {
                                val finalJob = if (job.id.isEmpty()) job.copy(id = doc.id) else job
                                list.add(finalJob)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing job document ${doc.id}", e)
                        }
                    }
                    if (list.isNotEmpty()) {
                        memoryJobs.clear()
                        memoryJobs.addAll(list)
                        _jobs.value = list
                    } else {
                        _jobs.value = memoryJobs.sortedByDescending { it.postedAt }
                    }
                }
            }

        // Realtime sync groups
        firestore.collection("groups").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error syncing groups", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = mutableListOf<TechGroup>()
                for (doc in snapshot.documents) {
                    try {
                        val group = doc.toObject(TechGroup::class.java)
                        if (group != null) {
                            val finalGroup = if (group.id.isEmpty()) group.copy(id = doc.id) else group
                            list.add(finalGroup)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing group document ${doc.id}", e)
                    }
                }
                if (list.isNotEmpty()) {
                    memoryGroups.clear()
                    memoryGroups.addAll(list)
                    _groups.value = list
                } else {
                    _groups.value = memoryGroups
                }
            }
        }

        // Realtime sync notifications for current logged-in user
        firestore.collection("notifications").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val allNotifs = snapshot.toObjects(DevNotification::class.java)
                val currentUid = auth?.currentUser?.uid ?: _currentUser.value?.id
                if (currentUid != null) {
                    val myNotifs = allNotifs.filter { it.receiverId == currentUid }
                        .sortedByDescending { it.createdAt }
                    
                    // Show actual native tray notification if an unread notification arrives that we don't already have
                    val newUnread = myNotifs.filter { !it.isRead && !memoryNotifications.any { m -> m.id == it.id } }
                    newUnread.forEach {
                        showSystemNotification(
                            title = when (it.type) {
                                "message" -> "New message on DevNet"
                                "comment" -> "Reviewer commented on your post"
                                "like" -> "Colleague liked your post"
                                else -> "DevNet Alert"
                            },
                            content = it.text
                        )
                    }
                    
                    memoryNotifications.clear()
                    memoryNotifications.addAll(myNotifs)
                    _notifications.value = myNotifs
                }
            }
        }

        // Realtime sync conversations for current logged-in user
        firestore.collection("conversations").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error syncing conversations", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = mutableListOf<Conversation>()
                val currentUid = auth?.currentUser?.uid ?: _currentUser.value?.id
                for (doc in snapshot.documents) {
                    try {
                        val conv = doc.toObject(Conversation::class.java)
                        if (conv != null) {
                            val finalConv = if (conv.id.isEmpty()) conv.copy(id = doc.id) else conv
                            if (currentUid != null && finalConv.participantIds.contains(currentUid)) {
                                list.add(finalConv)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing conversation document ${doc.id}", e)
                    }
                }
                memoryConversations.clear()
                memoryConversations.addAll(list)
                _conversations.value = list.sortedByDescending { it.lastMessageTime }
                
                // Save updated conversations locally
                CoroutineScope(Dispatchers.IO).launch {
                    saveConversationsToLocal()
                }
            }
        }
    }

    fun listenToMessages(conversationId: String) {
        val firestore = db ?: return
        firestore.collection("conversations").document(conversationId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Message::class.java).distinctBy { it.id }
                    memoryMessages[conversationId] = list.toMutableList()
                    val currentMap = _messages.value.toMutableMap()
                    currentMap[conversationId] = list
                    _messages.value = currentMap
                    
                    // Also save loaded messages to local cache immediately
                    CoroutineScope(Dispatchers.IO).launch {
                        saveMessagesToLocal()
                    }
                }
            }
    }

    // AUTHENTICATION APIs
    suspend fun registerUser(
        emailAddress: String,
        passwordRaw: String,
        fullName: String,
        username: String, // starts with @
        bio: String,
        specialty: String,
        skills: List<String>,
        avatarUrl: String,
        githubUrl: String = "",
        linkedinUrl: String = "",
        websiteUrl: String = ""
    ): UserProfile = withContext(Dispatchers.IO) {
        logEvent("user_register_attempt", mapOf("username" to username))
        
        // 1. Verify unique username
        val finalUsername = if (username.startsWith("@")) username else "@$username"
        val lowercaseUsername = finalUsername.lowercase()

        val firestore = db
        if (firestore != null) {
            val duplicateQuery = firestore.collection("users")
                .whereEqualTo("username", finalUsername)
                .get()
                .await()
            if (!duplicateQuery.isEmpty) {
                throw Exception("Username is already taken!")
            }
        } else {
            // Memory check fallback
            val isDuplicate = memoryUsers.values.any { it.username.lowercase() == lowercaseUsername }
            if (isDuplicate) {
                throw Exception("Username is already taken!")
            }
        }

        // 2. Create Firebase Auth account
        val firebaseUser = auth?.createUserWithEmailAndPassword(emailAddress, passwordRaw)?.await()?.user
        val uId = firebaseUser?.uid ?: "user_mock_${System.currentTimeMillis()}"

        val profile = UserProfile(
            id = uId,
            fullName = fullName,
            username = finalUsername,
            email = emailAddress,
            avatarUrl = avatarUrl,
            bio = bio,
            specialty = specialty,
            skills = skills,
            githubUrl = githubUrl,
            linkedinUrl = linkedinUrl,
            websiteUrl = websiteUrl
        )

        // 3. Save to Firestore / local Cache
        if (firestore != null) {
            firestore.collection("users").document(uId).set(profile).await()
        }

        memoryUsers[uId] = profile
        _currentUser.value = profile
        saveUserToPrefs(profile)
        syncStateFlows()
        
        logEvent("user_register_success", mapOf("userId" to uId))
        return@withContext profile
    }

    suspend fun loginUser(emailAddress: String, passwordRaw: String): UserProfile = withContext(Dispatchers.IO) {
        logEvent("user_login_attempt")
        val firebaseAuth = auth
        val firestore = db

        if (firebaseAuth != null && firestore != null) {
            val authResult = firebaseAuth.signInWithEmailAndPassword(emailAddress, passwordRaw).await()
            val uId = authResult.user?.uid ?: throw Exception("Auth failed: User identifier is null")
            
            val doc = firestore.collection("users").document(uId).get().await()
            val profile = doc.toObject(UserProfile::class.java) 
                ?: UserProfile(id = uId, fullName = authResult.user?.displayName ?: "DevNet Developer", email = emailAddress)
            
            _currentUser.value = profile
            saveUserToPrefs(profile)
            syncStateFlows()
            logEvent("user_login_success", mapOf("userId" to uId))
            return@withContext profile
        } else {
            // Local Memory Auth fallback
            val profile = memoryUsers.values.find { it.email.lowercase() == emailAddress.lowercase() }
                ?: UserProfile(
                    id = "user_demo_host",
                    fullName = "Master Programmer",
                    username = "@lead_coder",
                    email = emailAddress,
                    avatarUrl = "https://res.cloudinary.com/dqgsepaus/image/upload/v1718310000/avatar.jpg",
                    bio = "Unbeatable software developer and system architect.",
                    specialty = "Backend Security",
                    skills = listOf("Kotlin", "Docker", "Node.js", "K8s")
                )
            
            memoryUsers[profile.id] = profile
            _currentUser.value = profile
            saveUserToPrefs(profile)
            syncStateFlows()
            logEvent("user_login_success_fallback", mapOf("userId" to profile.id))
            return@withContext profile
        }
    }

    fun logout() {
        logEvent("user_logout")
        auth?.signOut()
        _currentUser.value = null
        saveUserToPrefs(null)
        // Delete token references if needed
    }

    suspend fun updateUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        val firestore = db
        if (firestore != null) {
            firestore.collection("users").document(profile.id).set(profile).await()
        }
        memoryUsers[profile.id] = profile
        _currentUser.value = profile
        saveUserToPrefs(profile)
        syncStateFlows()
        logEvent("user_profile_updated", mapOf("userId" to profile.id))
    }

    // POST MANAGEMENT APIs
    suspend fun createPost(
        text: String,
        codeSnippet: String = "",
        codeLanguage: String = "kotlin",
        tags: List<String> = emptyList(),
        customMediaUrls: List<String> = emptyList(),
        customMediaType: String = "none",
        fileNames: List<String> = emptyList(),
        fileUrls: List<String> = emptyList(),
        githubLink: String = "",
        liveLink: String = ""
    ): DevPost = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("You must be logged in to post!")
        val post = DevPost(
            userId = user.id,
            fullName = user.fullName,
            username = user.username,
            avatarUrl = user.avatarUrl,
            text = text,
            codeSnippet = codeSnippet,
            codeLanguage = codeLanguage,
            tags = tags,
            mediaUrls = customMediaUrls,
            mediaType = customMediaType,
            fileNames = fileNames,
            fileUrls = fileUrls,
            repoUrl = githubLink,
            liveUrl = liveLink
        )

        val firestore = db
        if (firestore != null) {
            firestore.collection("posts").document(post.id).set(post).await()
            
            // Increment postsCount on user
            val updatedUser = user.copy(postsCount = user.postsCount + 1)
            firestore.collection("users").document(user.id).set(updatedUser).await()
            _currentUser.value = updatedUser
            saveUserToPrefs(updatedUser)
            memoryUsers[user.id] = updatedUser
        }

        memoryPosts.add(0, post)
        syncStateFlows()
        logEvent("post_created", mapOf("postId" to post.id))
        return@withContext post
    }

    suspend fun toggleLikePost(postId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db
        
        val localList = memoryPosts.map { post ->
            if (post.id == postId) {
                val isLiked = post.likes.contains(user.id)
                val newLikes = if (isLiked) post.likes - user.id else post.likes + user.id
                if (firestore != null) {
                    firestore.collection("posts").document(postId).update("likes", newLikes)
                }
                post.copy(likes = newLikes)
            } else post
        }
        memoryPosts.clear()
        memoryPosts.addAll(localList)
        syncStateFlows()
        logEvent("post_like_toggled", mapOf("postId" to postId))
    }

    suspend fun toggleSavePost(postId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db

        val localList = memoryPosts.map { post ->
            if (post.id == postId) {
                val isSaved = post.savedBy.contains(user.id)
                val newSavedBy = if (isSaved) post.savedBy - user.id else post.savedBy + user.id
                if (firestore != null) {
                    firestore.collection("posts").document(postId).update("savedBy", newSavedBy)
                }
                post.copy(savedBy = newSavedBy)
            } else post
        }
        memoryPosts.clear()
        memoryPosts.addAll(localList)
        syncStateFlows()
    }

    // COMMENTS MANAGEMENT
    suspend fun addComment(postId: String, text: String): DevComment = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("Must be logged in to comment")
        val comment = DevComment(
            postId = postId,
            userId = user.id,
            fullName = user.fullName,
            username = user.username,
            avatarUrl = user.avatarUrl,
            text = text
        )

        val firestore = db
        if (firestore != null) {
            firestore.collection("posts").document(postId).collection("comments").document(comment.id).set(comment).await()
            
            // update comment counter
            val postRef = firestore.collection("posts").document(postId)
            val currentPost = postRef.get().await().toObject(DevPost::class.java)
            if (currentPost != null) {
                postRef.update("commentsCount", currentPost.commentsCount + 1)
            }
        }

        val list = (memoryComments[postId] ?: mutableListOf()).filter { it.id != comment.id }.toMutableList()
        list.add(0, comment)
        memoryComments[postId] = list
        
        // update posts in memory too
        val newPosts = memoryPosts.map {
            if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it
        }
        memoryPosts.clear()
        memoryPosts.addAll(newPosts)

        // Add Notification
        val postOwnerId = memoryPosts.find { it.id == postId }?.userId
        if (postOwnerId != null && postOwnerId != user.id) {
            createNotification(
                receiverId = postOwnerId,
                type = "comment",
                referenceId = postId,
                text = "${user.fullName} commented on your post: \"${text.take(30)}\""
            )
        }

        syncStateFlows()
        
        // Update comments Flow list directly
        val currentCommentsMap = _comments.value.toMutableMap()
        currentCommentsMap[postId] = list
        _comments.value = currentCommentsMap

        logEvent("comment_added", mapOf("postId" to postId))
        return@withContext comment
    }

    fun loadComments(postId: String) {
        val firestore = db ?: return
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(DevComment::class.java).distinctBy { it.id }
                    memoryComments[postId] = list.toMutableList()
                    val currentMap = _comments.value.toMutableMap()
                    currentMap[postId] = list
                    _comments.value = currentMap
                }
            }
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("posts").document(postId).delete().await()
                // Safely decrement post count
                val updatedUser = user.copy(postsCount = maxOf(0, user.postsCount - 1))
                firestore.collection("users").document(user.id).set(updatedUser).await()
                _currentUser.value = updatedUser
                saveUserToPrefs(updatedUser)
                memoryUsers[user.id] = updatedUser
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting post in Firestore", e)
            }
        }
        memoryPosts.removeAll { it.id == postId }
        syncStateFlows()
        logEvent("post_deleted", mapOf("postId" to postId))
    }

    suspend fun editPost(
        postId: String,
        newText: String,
        newCodeSnippet: String = "",
        newCodeLanguage: String = "kotlin",
        newTags: List<String> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val firestore = db
        if (firestore != null) {
            try {
                val postRef = firestore.collection("posts").document(postId)
                postRef.update(
                    mapOf(
                        "text" to newText,
                        "codeSnippet" to newCodeSnippet,
                        "codeLanguage" to newCodeLanguage,
                        "tags" to newTags
                    )
                ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error editing post in Firestore", e)
            }
        }
        val localList = memoryPosts.map { post ->
            if (post.id == postId) {
                post.copy(
                    text = newText,
                    codeSnippet = newCodeSnippet,
                    codeLanguage = newCodeLanguage,
                    tags = newTags
                )
            } else post
        }
        memoryPosts.clear()
        memoryPosts.addAll(localList)
        syncStateFlows()
        logEvent("post_edited", mapOf("postId" to postId))
    }

    suspend fun deleteComment(postId: String, commentId: String) = withContext(Dispatchers.IO) {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("posts").document(postId)
                    .collection("comments").document(commentId).delete().await()
                // Update post comments count
                val postRef = firestore.collection("posts").document(postId)
                val currentPost = postRef.get().await().toObject(DevPost::class.java)
                if (currentPost != null) {
                    postRef.update("commentsCount", maxOf(0, currentPost.commentsCount - 1)).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting comment in Firestore", e)
            }
        }
        val list = memoryComments[postId] ?: mutableListOf()
        list.removeAll { it.id == commentId }
        memoryComments[postId] = list

        val newPosts = memoryPosts.map {
            if (it.id == postId) it.copy(commentsCount = maxOf(0, it.commentsCount - 1)) else it
        }
        memoryPosts.clear()
        memoryPosts.addAll(newPosts)

        syncStateFlows()

        val currentMap = _comments.value.toMutableMap()
        currentMap[postId] = list
        _comments.value = currentMap
        logEvent("comment_deleted", mapOf("commentId" to commentId))
    }

    suspend fun editComment(postId: String, commentId: String, newText: String) = withContext(Dispatchers.IO) {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("posts").document(postId)
                    .collection("comments").document(commentId).update("text", newText).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error editing comment in Firestore", e)
            }
        }
        val list = memoryComments[postId] ?: mutableListOf()
        val commentIndex = list.indexOfFirst { it.id == commentId }
        if (commentIndex != -1) {
            val updatedComment = list[commentIndex].copy(text = newText)
            list[commentIndex] = updatedComment
        }
        memoryComments[postId] = list
        syncStateFlows()

        val currentMap = _comments.value.toMutableMap()
        currentMap[postId] = list
        _comments.value = currentMap
        logEvent("comment_edited", mapOf("commentId" to commentId))
    }

    suspend fun toggleLikeComment(postId: String, commentId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db
        val list = memoryComments[postId] ?: mutableListOf()
        val commentIndex = list.indexOfFirst { it.id == commentId }
        if (commentIndex != -1) {
            val comment = list[commentIndex]
            val isLiked = comment.likes.contains(user.id)
            val newLikes = if (isLiked) comment.likes - user.id else comment.likes + user.id
            val updatedComment = comment.copy(likes = newLikes)
            list[commentIndex] = updatedComment

            if (firestore != null) {
                try {
                    firestore.collection("posts").document(postId)
                        .collection("comments").document(commentId).update("likes", newLikes).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing comment like", e)
                }
            }

            // Create notification for comment owner if a new like is registered
            if (!isLiked && comment.userId != user.id) {
                createNotification(
                    receiverId = comment.userId,
                    type = "like",
                    referenceId = postId,
                    text = "${user.fullName} liked your comment: \"${comment.text.take(30)}\""
                )
            }
        }
        memoryComments[postId] = list
        syncStateFlows()

        val currentMap = _comments.value.toMutableMap()
        currentMap[postId] = list
        _comments.value = currentMap
        logEvent("comment_like_toggled", mapOf("commentId" to commentId))
    }

    // FOLLOW / UNFOLLOW SYSTEM
    suspend fun toggleFollowUser(targetUserId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        if (user.id == targetUserId) return@withContext // Cannot follow yourself!
        
        val firestore = db
        val targetProfile = memoryUsers[targetUserId] ?: return@withContext

        val isFollowing = user.following.contains(targetUserId)
        val newFollowing = if (isFollowing) user.following - targetUserId else user.following + targetUserId
        val newFollowers = if (isFollowing) targetProfile.followers - user.id else targetProfile.followers + user.id

        val updatedUser = user.copy(following = newFollowing)
        val updatedTarget = targetProfile.copy(followers = newFollowers)

        if (firestore != null) {
            firestore.collection("users").document(user.id).set(updatedUser)
            firestore.collection("users").document(targetUserId).set(updatedTarget)
        }

        memoryUsers[user.id] = updatedUser
        memoryUsers[targetUserId] = updatedTarget
        _currentUser.value = updatedUser
        saveUserToPrefs(updatedUser)
        
        if (!isFollowing) {
            createNotification(
                receiverId = targetUserId,
                type = "follow",
                referenceId = user.id,
                text = "${user.fullName} started following you!"
            )
        }

        syncStateFlows()
        logEvent("user_follow_toggled", mapOf("target" to targetUserId, "active" to (!isFollowing).toString()))
    }

    // PROJECTS ENDPOINTS
    suspend fun createProject(
        title: String,
        description: String,
        githubUrl: String,
        liveUrl: String,
        imageUrls: List<String> = emptyList(),
        fileUrls: List<String> = emptyList()
    ): DevProject = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("Unauthorized to publish project")
        val project = DevProject(
            userId = user.id,
            fullName = user.fullName,
            username = user.username,
            avatarUrl = user.avatarUrl,
            title = title,
            description = description,
            githubUrl = githubUrl,
            liveUrl = liveUrl,
            imageUrls = imageUrls,
            fileUrls = fileUrls
        )

        val firestore = db
        if (firestore != null) {
            firestore.collection("projects").document(project.id).set(project).await()
        }

        memoryProjects.add(0, project)
        syncStateFlows()
        logEvent("project_created", mapOf("title" to title))
        return@withContext project
    }

    // JOBS ENDPOINTS
    suspend fun postJob(
        title: String,
        company: String,
        logoUrl: String,
        description: String,
        reqs: List<String>,
        tags: List<String>,
        loc: String,
        salary: String,
        exp: Int,
        country: String
    ): DevJob = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("Must be logged in to post job")
        val job = DevJob(
            title = title,
            companyName = company,
            companyLogoUrl = logoUrl,
            description = description,
            requirements = reqs,
            tags = tags,
            location = loc,
            salary = salary,
            experienceYears = exp,
            country = country,
            postedBy = user.id
        )

        val firestore = db
        if (firestore != null) {
            firestore.collection("jobs").document(job.id).set(job).await()
        }

        memoryJobs.add(0, job)
        syncStateFlows()
        logEvent("job_posted", mapOf("title" to title))
        return@withContext job
    }

    suspend fun applyForJob(jobId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db

        val updatedList = memoryJobs.map { job ->
            if (job.id == jobId) {
                val newApplied = if (job.appliedUsers.contains(user.id)) job.appliedUsers else job.appliedUsers + user.id
                if (firestore != null) {
                    firestore.collection("jobs").document(jobId).update("appliedUsers", newApplied)
                }
                job.copy(appliedUsers = newApplied)
            } else job
        }
        memoryJobs.clear()
        memoryJobs.addAll(updatedList)
        syncStateFlows()
        logEvent("job_applied", mapOf("jobId" to jobId))
    }

    // GROUPS ENDPOINTS
    suspend fun createGroup(name: String, desc: String, category: String, imageUrl: String): TechGroup = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("Login required to create a Tech Group")
        val group = TechGroup(
            name = name,
            description = desc,
            category = category,
            imageUrl = imageUrl,
            creatorId = user.id,
            members = listOf(user.id)
        )

        val firestore = db
        if (firestore != null) {
            firestore.collection("groups").document(group.id).set(group).await()
        }

        memoryGroups.add(group)
        syncStateFlows()
        return@withContext group
    }

    suspend fun joinsGroup(groupId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db

        val updated = memoryGroups.map { g ->
            if (g.id == groupId) {
                val alreadyMember = g.members.contains(user.id)
                val newMembers = if (alreadyMember) g.members - user.id else g.members + user.id
                if (firestore != null) {
                    firestore.collection("groups").document(groupId).update("members", newMembers)
                }
                g.copy(members = newMembers)
            } else g
        }
        memoryGroups.clear()
        memoryGroups.addAll(updated)
        syncStateFlows()
    }

    // CONVERSATIONS & CHAT SYSTEM
    suspend fun startConversation(targetUserId: String): Conversation = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("Must be logged in to chat")
        val targetProfile = memoryUsers[targetUserId] ?: throw Exception("Recipient not found")

        val firestore = db
        val existingId1 = "${user.id}_$targetUserId"
        val existingId2 = "${targetUserId}_${user.id}"

        var existingConv: Conversation? = null
        if (firestore != null) {
            val check1 = firestore.collection("conversations").document(existingId1).get().await()
            if (check1.exists()) existingConv = check1.toObject(Conversation::class.java)
            if (existingConv == null) {
                val check2 = firestore.collection("conversations").document(existingId2).get().await()
                if (check2.exists()) existingConv = check2.toObject(Conversation::class.java)
            }
        } else {
            existingConv = memoryConversations.find {
                it.participantIds.contains(user.id) && it.participantIds.contains(targetUserId)
            }
        }

        if (existingConv != null) return@withContext existingConv

        // Create new one if none exists
        val convId = existingId1
        val newConv = Conversation(
            id = convId,
            participantIds = listOf(user.id, targetUserId),
            participantNames = mapOf(user.id to user.fullName, targetUserId to targetProfile.fullName),
            participantAvatars = mapOf(user.id to user.avatarUrl, targetUserId to targetProfile.avatarUrl),
            lastMessageText = "Start coding conversation",
            lastMessageTime = System.currentTimeMillis()
        )

        if (firestore != null) {
            firestore.collection("conversations").document(convId).set(newConv).await()
        }

        memoryConversations.add(0, newConv)
        syncStateFlows()
        return@withContext newConv
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "none",
        codeSnippet: String = ""
    ): Message = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw Exception("Must be logged in to send messages")
        val message = Message(
            conversationId = conversationId,
            senderId = user.id,
            senderName = user.fullName,
            senderAvatarUrl = user.avatarUrl,
            text = text,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            codeSnippet = codeSnippet
        )

        val firestore = db
        if (firestore != null) {
            // Add message to subcollection
            firestore.collection("conversations").document(conversationId)
                .collection("messages").document(message.id).set(message).await()

            // Update last metrics
            val convRef = firestore.collection("conversations").document(conversationId)
            convRef.update(
                mapOf(
                    "lastMessageText" to if (text.isNotEmpty()) text else "Sent a attachment",
                    "lastMessageTime" to message.createdAt,
                    "lastSenderId" to user.id
                )
            ).await()
        }

        // Local cache sync
        val list = (memoryMessages[conversationId] ?: mutableListOf()).filter { it.id != message.id }.toMutableList()
        list.add(message)
        memoryMessages[conversationId] = list

        val updatedConvs = memoryConversations.map {
            if (it.id == conversationId) {
                it.copy(
                    lastMessageText = if (text.isNotEmpty()) text else "Sent an attachment",
                    lastMessageTime = message.createdAt,
                    lastSenderId = user.id
                )
            } else it
        }
        memoryConversations.clear()
        memoryConversations.addAll(updatedConvs)

        // Trigger Notification
        val otherUserId = conversationId.split("_").find { it != user.id }
        if (otherUserId != null) {
            createNotification(
                receiverId = otherUserId,
                type = "message",
                referenceId = conversationId,
                text = "New message from ${user.fullName}: \"${text.take(30)}\""
            )
        }

        syncStateFlows()

        val currentMessagesMap = _messages.value.toMutableMap()
        currentMessagesMap[conversationId] = list
        _messages.value = currentMessagesMap

        return@withContext message
    }

    suspend fun deleteMessage(conversationId: String, messageId: String) = withContext(Dispatchers.IO) {
        val firestore = db
        if (firestore != null) {
            firestore.collection("conversations").document(conversationId)
                .collection("messages").document(messageId).update("deleted", true)
        }
        val list = memoryMessages[conversationId] ?: return@withContext
        val updated = list.map {
            if (it.id == messageId) it.copy(isDeleted = true, text = "Message deleted") else it
        }
        memoryMessages[conversationId] = updated.toMutableList()
        syncStateFlows()

        val currentMessagesMap = _messages.value.toMutableMap()
        currentMessagesMap[conversationId] = updated
        _messages.value = currentMessagesMap
    }

    // NOTIFICATION CREATIONS
    suspend fun createNotification(receiverId: String, type: String, referenceId: String, text: String) {
        val actor = _currentUser.value ?: return
        val notif = DevNotification(
            receiverId = receiverId,
            senderId = actor.id,
            senderName = actor.fullName,
            senderAvatarUrl = actor.avatarUrl,
            type = type,
            referenceId = referenceId,
            text = text
        )

        val firestore = db
        if (firestore != null) {
            try { firestore.collection("notifications").document(notif.id).set(notif) } catch (e: Exception) {}
        }
        memoryNotifications.add(0, notif)
        syncStateFlows()
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val firestore = db
        if (firestore != null) {
            val querySnapshot = firestore.collection("notifications")
                .whereEqualTo("receiverId", user.id)
                .whereEqualTo("read", false)
                .get()
                .await()
            for (doc in querySnapshot.documents) {
                doc.reference.update("read", true)
            }
        }
        val list = memoryNotifications.map { it.copy(isRead = true) }
        memoryNotifications.clear()
        memoryNotifications.addAll(list)
        syncStateFlows()
    }

    private fun showSystemNotification(title: String, content: String) {
        val ctx = appContext ?: return
        try {
            val channelId = "devnet_alerts"
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "DevNet Professional Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time communication and review signals"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error generating local status notification", e)
        }
    }
}
