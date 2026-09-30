package com.example.util

import com.example.data.model.AchievementEntity

object AchievementsManager {

    val INITIAL_ACHIEVEMENTS = listOf(
        AchievementEntity(
            id = "FIRST_SESSION",
            title = "First Step",
            description = "Complete your first locked study session.",
            category = "FOCUS",
            targetValue = 1,
            iconKey = "star"
        ),
        AchievementEntity(
            id = "SHORTS_SLAYER",
            title = "Shorts Slayer",
            description = "Intercept and block 5 addictive YouTube Shorts during study hours.",
            category = "SHIELD",
            targetValue = 5,
            iconKey = "shield"
        ),
        AchievementEntity(
            id = "CENTURION",
            title = "Focus Centurion",
            description = "Accumulate 100 total minutes of deep study focus.",
            category = "FOCUS",
            targetValue = 100,
            iconKey = "trophy"
        ),
        AchievementEntity(
            id = "DISTRACTION_DEFENDER",
            title = "Digital Fortress",
            description = "Intercept 10 distraction app launch attempts.",
            category = "SHIELD",
            targetValue = 10,
            iconKey = "crown"
        ),
        AchievementEntity(
            id = "MUSIC_MAESTRO",
            title = "Audio Sanctuary",
            description = "Save and play local study music or binaural beats.",
            category = "MUSIC",
            targetValue = 1,
            iconKey = "music"
        ),
        AchievementEntity(
            id = "IRON_DISCIPLINE",
            title = "Iron Discipline",
            description = "Complete 3 study sessions without a single distraction attempt.",
            category = "DISCIPLINE",
            targetValue = 3,
            iconKey = "fire"
        ),
        AchievementEntity(
            id = "DEEP_DIVER",
            title = "Deep Diver",
            description = "Accumulate 500 total minutes of verified study time.",
            category = "FOCUS",
            targetValue = 500,
            iconKey = "trophy"
        ),
        AchievementEntity(
            id = "UNSHAKEABLE",
            title = "Unshakeable Guard",
            description = "Enable Device Admin uninstall protection during study mode.",
            category = "DISCIPLINE",
            targetValue = 1,
            iconKey = "shield"
        )
    )
}
