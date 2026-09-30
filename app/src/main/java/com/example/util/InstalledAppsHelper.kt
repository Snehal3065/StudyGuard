package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.model.BlockedAppEntity

object InstalledAppsHelper {

    // Common distraction package signatures and friendly names
    val POPULAR_DISTRACTIONS = listOf(
        Triple("com.instagram.android", "Instagram", "Social"),
        Triple("com.zhiliaoapp.musically", "TikTok", "Social"),
        Triple("com.twitter.android", "X (Twitter)", "Social"),
        Triple("com.facebook.katana", "Facebook", "Social"),
        Triple("com.snapchat.android", "Snapchat", "Social"),
        Triple("com.reddit.frontpage", "Reddit", "Social"),
        Triple("com.netflix.mediaclient", "Netflix", "Entertainment"),
        Triple("tv.twitch.android.app", "Twitch", "Entertainment"),
        Triple("com.pinterest", "Pinterest", "Social"),
        Triple("com.discord", "Discord", "Social"),
        Triple("com.valvesoftware.android.steam.community", "Steam", "Games"),
        Triple("com.roblox.client", "Roblox", "Games"),
        Triple("com.supercell.brawlstars", "Brawl Stars", "Games"),
        Triple("com.supercell.clashofclans", "Clash of Clans", "Games"),
        Triple("com.supercell.clashroyale", "Clash Royale", "Games"),
        Triple("com.dts.freefireth", "Free Fire", "Games"),
        Triple("com.tencent.ig", "PUBG Mobile", "Games"),
        Triple("com.king.candycrushsaga", "Candy Crush", "Games")
    )

    val KNOWN_DISTRACTIONS = POPULAR_DISTRACTIONS.map { it.first }.toSet()

    // Common study tools / productive apps that should be allowed
    val KNOWN_STUDY_APPS = setOf(
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.slides",
        "com.google.android.keep",
        "com.google.android.calculator",
        "com.google.android.calendar",
        "com.google.android.apps.classroom",
        "org.khanacademy.android",
        "com.duolingo",
        "com.quizlet.quizletandroid",
        "com.instructure.candroid", // Canvas
        "com.notion.id",
        "md.obsidian",
        "com.wolfram.android.alpha"
    )

    fun getInstalledLauncherApps(context: Context): List<BlockedAppEntity> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val selfPackage = context.packageName

        val apps = mutableListOf<BlockedAppEntity>()
        val seen = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == selfPackage) continue
            if (seen.contains(pkg)) continue
            seen.add(pkg)

            val appName = resolveInfo.loadLabel(pm).toString()
            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }

            val category = when {
                KNOWN_DISTRACTIONS.contains(pkg) -> "Social"
                KNOWN_STUDY_APPS.contains(pkg) -> "Study Tool"
                pkg == "com.google.android.youtube" -> "YouTube (Study Videos)"
                pkg.contains("game") || pkg.contains("play") -> "Games"
                isSystem -> "System"
                else -> "App"
            }

            val isDistraction = when {
                pkg == "com.google.android.youtube" -> false
                KNOWN_DISTRACTIONS.contains(pkg) -> true
                KNOWN_STUDY_APPS.contains(pkg) -> false
                isSystem -> false
                else -> false
            }

            apps.add(
                BlockedAppEntity(
                    packageName = pkg,
                    appName = appName,
                    isDistraction = isDistraction,
                    category = category,
                    isProtectedSystemApp = isSystem
                )
            )
        }

        return apps.sortedWith(compareByDescending<BlockedAppEntity> { it.isDistraction }.thenBy { it.appName })
    }
}
