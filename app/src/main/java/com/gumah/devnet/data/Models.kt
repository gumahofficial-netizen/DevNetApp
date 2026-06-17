package com.gumah.devnet.data

import java.util.UUID

data class UserProfile(
    val id: String = "",
    val fullName: String = "",
    val username: String = "", // starts with @
    val email: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val bio: String = "",
    val specialty: String = "", // Mobile, Backend, Frontend, Fullstack, AI, DevOps, etc.
    val skills: List<String> = emptyList(),
    val githubUrl: String = "",
    val linkedinUrl: String = "",
    val websiteUrl: String = "",
    val followers: List<String> = emptyList(), // follower userIds
    val following: List<String> = emptyList(), // following userIds
    val postsCount: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)

data class DevPost(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val text: String = "",
    val mediaUrls: List<String> = emptyList(),
    val mediaType: String = "", // "image", "video", "raw", "none"
    val fileNames: List<String> = emptyList(), // Store ZIP, PDF, APK names
    val fileUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val codeSnippet: String = "", // Markdown syntax or code block
    val codeLanguage: String = "kotlin",
    val likes: List<String> = emptyList(), // userIds who liked
    val commentsCount: Int = 0,
    val repostsCount: Int = 0,
    val savedBy: List<String> = emptyList(), // userIds who saved
    val repoUrl: String = "",
    val liveUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DevComment(
    val id: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val replies: List<DevReply> = emptyList(),
    val likes: List<String> = emptyList()
)

data class DevReply(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastSenderId: String = "",
    val isRead: Map<String, Boolean> = emptyMap() // userId -> isRead
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "", // "image", "video", "pdf", "zip", "apk", "none"
    val codeSnippet: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
)

data class DevProject(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val fileUrls: List<String> = emptyList(),
    val githubUrl: String = "",
    val liveUrl: String = "",
    val likes: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class TechGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val creatorId: String = "",
    val members: List<String> = emptyList(), // userIds
    val pinnedPostId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DevJob(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val companyName: String = "",
    val companyLogoUrl: String = "",
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val location: String = "", // Remote, Hybrid, Onsite
    val salary: String = "",
    val experienceYears: Int = 0,
    val country: String = "",
    val postedBy: String = "", // userId
    val postedAt: Long = System.currentTimeMillis(),
    val appliedUsers: List<String> = emptyList() // userIds
)

data class DevNotification(
    val id: String = UUID.randomUUID().toString(),
    val receiverId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val type: String = "", // "like", "comment", "follow", "message", "mention"
    val referenceId: String = "", // postId, conversationId etc
    val text: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Draft(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val text: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class BadgeType(
    val englishName: String,
    val arabicName: String,
    val descriptionName: String,
    val hexColor: String,
    val iconName: String
) {
    TOP_CONTRIBUTOR("Top Contributor", "بطل المحتوى", "نشر 3 منشورات برمجية أو أكثر لشرح أكواد أو استعراض مهاراته", "#EF4444", "Star"),
    SENIOR_DEV("Senior Dev", "مطور محترف", "عضو خبير ذو مهارات قيادية أو تقنية متقدمة", "#10B981", "WorkspacePremium"),
    TECH_GURU("Tech Guru", "خبير التقنية", "أتقن 5 لغات برمجة أو تقنيات حديثة أو أكثر", "#8B5CF6", "Psychology"),
    COMMUNITY_STAR("Community Star", "نجم المجتمع", "يتابعه العديد من المطورين والزملاء في مجتمع DevNet", "#F59E0B", "Favorite"),
    FULLSTACK_SPECIALIST("Fullstack Specialist", "خبير شامل", "يتميز بمهارات برمجية كاملة تغطي السيرفرات والواجهات", "#3B82F6", "Layers"),
    ANDROID_WIZARD("Android Wizard", "ساحر أندرويد", "مطور برمجيات أندرويد وهواتف ذكية محترف", "#22C55E", "Android"),
    AI_ENTHUSIAST("AI Enthusiast", "عاشق الذكاء", "يساعد في دمج نماذج الذكاء الاصطناعي وهندسة البيانات", "#D946EF", "AutoAwesome")
}

fun getBadgesForProfile(profile: UserProfile): List<BadgeType> {
    val list = mutableListOf<BadgeType>()
    if (profile.postsCount >= 3 || profile.id == "user_linus") {
        list.add(BadgeType.TOP_CONTRIBUTOR)
    }
    val specLower = profile.specialty.lowercase()
    if (specLower.contains("senior") || specLower.contains("lead") || specLower.contains("architect") || specLower.contains("principal") || specLower.contains("kernel") || profile.skills.size >= 4) {
        list.add(BadgeType.SENIOR_DEV)
    }
    if (profile.skills.size >= 5) {
        list.add(BadgeType.TECH_GURU)
    }
    if (profile.followers.size >= 1 || profile.id == "user_linus" || profile.id == "user_ada") {
        list.add(BadgeType.COMMUNITY_STAR)
    }
    if (specLower.contains("fullstack") || specLower.contains("full stack") || specLower.contains("شامل")) {
        list.add(BadgeType.FULLSTACK_SPECIALIST)
    }
    if (specLower.contains("android") || specLower.contains("mobile") || specLower.contains("أندرويد") || specLower.contains("هواتف")) {
        list.add(BadgeType.ANDROID_WIZARD)
    }
    if (specLower.contains("ai") || specLower.contains("intelligence") || specLower.contains("ml") || specLower.contains("ذكاء")) {
        list.add(BadgeType.AI_ENTHUSIAST)
    }
    return list
}
