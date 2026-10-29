package com.storagepulse.cleaner.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"

    // Bottom-nav tabs
    const val DASHBOARD = "dashboard"
    const val TREEMAP = "treemap"
    const val AI_CLEAN = "ai_clean"
    const val GALLERY = "gallery"
    const val TOOLS = "tools"

    // Full-screen features opened from the dashboard
    const val SWIPE_DECK = "swipe_deck"
    const val COMPRESSOR = "compressor"
    const val ARG_FOLDER = "folder"
    const val COMPRESSOR_PATTERN = "$COMPRESSOR?$ARG_FOLDER={$ARG_FOLDER}"

    /** Compressor pre-loaded with one folder's media. */
    fun compressor(folder: String) = "$COMPRESSOR?$ARG_FOLDER=${android.net.Uri.encode(folder)}"
    const val CHAT_CLEANER = "chat_cleaner"
    const val SAFE_TRASH = "safe_trash"
    const val VAULT = "vault"
    const val SETTINGS = "settings"

    val tabRoutes = setOf(DASHBOARD, TREEMAP, AI_CLEAN, GALLERY, TOOLS)

    /** Screens with the anchored banner (free users). Never vault, gallery, AI Clean or swipe deck. */
    val bannerRoutes = setOf(DASHBOARD, TREEMAP, TOOLS, CHAT_CLEANER, SAFE_TRASH)
}
