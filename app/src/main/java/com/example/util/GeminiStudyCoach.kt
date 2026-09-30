package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class MessageSender {
    USER, AI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLiveApi: Boolean = true
)

enum class CoachRole(
    val displayName: String,
    val description: String,
    val systemInstruction: String,
    val model: String
) {
    GENERAL(
        displayName = "Study Coach 🎓",
        description = "Balanced focus advice, study habits & encouragement",
        systemInstruction = """
            You are StudyGuard AI, an elite academic cognitive coach and high-performance learning specialist.
            Your mission is to help students maximize focus, eliminate procrastination, and master difficult subjects with scientifically proven methods (Active Recall, Spaced Repetition, Feynman Technique, Pomodoro intervals, and Interleaving).
            Always provide structured, highly practical, and motivating answers using bullet points, concrete time estimates, and clear next steps.
        """.trimIndent(),
        model = "gemini-3.5-flash"
    ),
    DRILL_SERGEANT(
        displayName = "Strict Drill Sergeant ⚡",
        description = "No-nonsense anti-procrastination intervention",
        systemInstruction = """
            You are an intense, highly disciplined academic drill sergeant.
            The user is procrastinating, making excuses, or feeling the urge to scroll through social media or play games.
            Demand immediate action, call out digital dopamine addictions without sugarcoating, and give them a 3-step immediate focus challenge to get off their phone and begin studying right now.
        """.trimIndent(),
        model = "gemini-3.1-flash-lite-preview"
    ),
    EXAM_PLANNER(
        displayName = "Deep Work & Exam Strategist 📚",
        description = "Complex STEM reasoning, Feynman technique & active recall plans",
        systemInstruction = """
            You are an expert academic strategist, STEM tutor, and cognitive psychologist.
            You excel at deconstructing dense, complex syllabuses into step-by-step revision roadmaps, generating difficult active-recall questions, explaining tough technical concepts simply (Feynman technique), and optimizing 3-to-4-hour deep study marathons with strategic breaks.
        """.trimIndent(),
        model = "gemini-3.1-pro-preview"
    )
}

sealed class AiCoachResult {
    data class Success(val responseText: String, val isLiveApi: Boolean) : AiCoachResult()
    data class Error(val message: String) : AiCoachResult()
}

object GeminiStudyCoach {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun sendChatMessage(
        history: List<ChatMessage>,
        newMessage: String,
        role: CoachRole = CoachRole.GENERAL
    ): AiCoachResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localResponse = generateLocalAiResponse(newMessage, role)
            return@withContext AiCoachResult.Success(localResponse, isLiveApi = false)
        }

        try {
            val url = "$BASE_URL/${role.model}:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                // System Instruction to set persona
                val sysInst = JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        put(JSONObject().put("text", role.systemInstruction))
                    }
                    put("parts", sysParts)
                }
                put("system_instruction", sysInst)

                // Multi-turn contents thread
                val contents = JSONArray()
                // Take up to last 10 messages for context
                val recentHistory = history.takeLast(10)
                for (msg in recentHistory) {
                    val turnObj = JSONObject().apply {
                        put("role", if (msg.sender == MessageSender.USER) "user" else "model")
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        }
                        put("parts", parts)
                    }
                    contents.put(turnObj)
                }

                // Append current user message
                val currentTurn = JSONObject().apply {
                    put("role", "user")
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", newMessage))
                    }
                    put("parts", parts)
                }
                contents.put(currentTurn)

                put("contents", contents)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallback = generateLocalAiResponse(newMessage, role)
                    return@withContext AiCoachResult.Success(
                        "$fallback\n\n*(Note: Live Gemini returned HTTP ${response.code}. Served via StudyGuard Offline Engine)*",
                        isLiveApi = false
                    )
                }

                val responseBodyStr = response.body?.string() ?: ""
                val json = JSONObject(responseBodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        textBuilder.append(parts.getJSONObject(i).optString("text", ""))
                    }
                    val output = textBuilder.toString().trim()
                    if (output.isNotEmpty()) {
                        return@withContext AiCoachResult.Success(output, isLiveApi = true)
                    }
                }

                val fallback = generateLocalAiResponse(newMessage, role)
                return@withContext AiCoachResult.Success(fallback, isLiveApi = false)
            }
        } catch (e: Exception) {
            val fallback = generateLocalAiResponse(newMessage, role)
            return@withContext AiCoachResult.Success(
                "$fallback\n\n*(Offline Mode: Running on local StudyGuard heuristics)*",
                isLiveApi = false
            )
        }
    }

    // Backwards-compatible single-query overload
    suspend fun askStudyCoach(prompt: String): AiCoachResult {
        return sendChatMessage(emptyList(), prompt, CoachRole.GENERAL)
    }

    private fun generateLocalAiResponse(prompt: String, role: CoachRole = CoachRole.GENERAL): String {
        val lower = prompt.lowercase()

        // 1. Strict Drill Sergeant Mode
        if (role == CoachRole.DRILL_SERGEANT || lower.contains("kick") || lower.contains("stop me") || lower.contains("discipline")) {
            return """
                ⚡ **DROP THE PHONE & COMMENCE DEEP WORK IMMEDIATELY.**
                
                • **Rule 1: No Bargaining.** You promised yourself you would study today. A quick 10-second scroll turns into 2 lost hours.
                • **Rule 2: The Physical Barrier.** Put your phone screen-down at least 10 feet away or inside a drawer right now.
                • **Rule 3: Start 1 Page.** Open your book or notes. Read the first three sentences. Action creates motivation, NOT the other way around.
                
                ⏱️ *Challenge:* Start a 25-minute Pomodoro timer RIGHT NOW. Do not look up until the timer rings. **Go!**
            """.trimIndent()
        }

        // 2. Deep Work & Exam Strategist Mode
        if (role == CoachRole.EXAM_PLANNER || lower.contains("exam") || lower.contains("syllabus") || lower.contains("feynman") || lower.contains("complex")) {
            return """
                📚 **High-Yield Exam Mastery Framework (Cognitive Strategy)**
                
                • **Phase 1: The Feynman Simplification (40 mins)**
                  - Take your hardest concept. Explain it on a blank page as if teaching a 12-year-old.
                  - Any point where you use textbook jargon is a gap in your fundamental understanding. Highlight it.
                
                • **Phase 2: Closed-Book Active Retrieval (45 mins)**
                  - Write 5 core test problems or exam-style prompts.
                  - Solve them completely from memory with no references open.
                
                • **Phase 3: Interleaved Error Calibration (20 mins)**
                  - Review your missed points. Record *why* you missed them in your Error Log (Conceptual vs Calculation vs Recall error).
                
                🎯 *Exam Tip: Spacing 3 x 45-minute blocks with 10-minute non-screen breaks yields 300% higher retention than a 4-hour cram session.*
            """.trimIndent()
        }

        // 3. Subject-Specific Detection
        if (lower.contains("math") || lower.contains("calculus") || lower.contains("physics") || lower.contains("algebra")) {
            return """
                📐 **STEM Deep Practice Protocol (Math & Physics)**
                
                • **1. Formula Anchoring (15 mins)**:
                  - Don't memorize formulas passively. Derive the base equation once from first principles.
                • **2. The Rule of 5 Problems (45 mins)**:
                  - 2 Standard problems (build procedural fluency).
                  - 2 Multi-concept problems (combine theorems).
                  - 1 Challenge problem (tests deep intuition).
                • **3. Zero Peek Rule**:
                  - If stuck, spend at least 4 full minutes trying alternative algebraic approaches before glancing at the answer key.
            """.trimIndent()
        }

        if (lower.contains("bio") || lower.contains("chem") || lower.contains("history") || lower.contains("photosynthesis")) {
            return """
                🧬 **Active Recall Drill & Conceptual Mapping**
                
                • **1. Blank Page Brain Dump (15 mins)**:
                  - Write down every keyword, mechanism, enzyme, or timeline event you remember from this topic without notes.
                • **2. Dual-Coding Mechanism (30 mins)**:
                  - Convert verbal descriptions into hand-drawn flowcharts and cycle diagrams.
                • **3. 3-Question Active Recall Drill**:
                  - *Q1 (Mechanism):* What triggers the initial step in this process?
                  - *Q2 (Bottleneck):* What happens if the limiting reagent/factor is removed?
                  - *Q3 (Comparison):* How does this contrast with related biological/historical phenomena?
            """.trimIndent()
        }

        // 4. Timetable / Schedule Queries
        if (lower.contains("plan") || lower.contains("schedule") || lower.contains("timetable") || lower.contains("routine") || lower.contains("hour")) {
            return """
                🗓️ **AI Optimized Deep Work Study Schedule**
                
                • **Block 1: Deep Absorption (45 mins)**
                  - Tackle your highest-friction concept first when dopamine & willpower are peaked.
                  - Zero social media; phone on Do Not Disturb.
                
                • **Strategic Recharge (10 mins)**
                  - Hydrate with cold water, stretch spinal muscles, avoid screens.
                
                • **Block 2: Active Recall & Problem Sets (50 mins)**
                  - Close reference books. Solve 5 problems or teach the topic aloud (Feynman technique).
                
                • **Block 3: Rapid Spaced Review (20 mins)**
                  - Write 3 bullet summary takeaways to lock concepts into long-term memory.
                
                🔥 *Pro-Tip: Keep StudyGuard's Scheduled Auto-Lock enabled so distraction temptation is physically blocked.*
            """.trimIndent()
        }

        // 5. Procrastination / Urges
        if (lower.contains("procrastinat") || lower.contains("distract") || lower.contains("phone") || lower.contains("urge") || lower.contains("lazy") || lower.contains("tired")) {
            return """
                🛡️ **Cognitive Override: Crushing Procrastination**
                
                1. **The 5-Minute Rule**: Tell yourself: *"I will only review this chapter for 5 minutes. If I still hate it, I can pause."* In 90% of cases, breaking initial inertia unlocks effortless momentum.
                2. **Dopamine Detox Mechanism**: Your brain wants a quick hit from scrolling. Delay it by taking 3 box-breaths (4s inhale, 4s hold, 4s exhale, 4s hold).
                3. **Friction Architecture**: StudyGuard has your distraction apps locked. Use this barrier as your personal shield.
                
                ⚡ *“The cost of discipline is always less than the pain of regret.” Get started right now!*
            """.trimIndent()
        }

        // 6. Quizzes & Tests
        if (lower.contains("quiz") || lower.contains("test") || lower.contains("flashcard") || lower.contains("question")) {
            return """
                📝 **AI Rapid Focus Check: Active Recall**
                
                Before granting leisure access, answer these 3 quick recall prompts:
                
                1. **Core Mechanism**: In your own words, what is the single most important definition or theorem from this topic?
                2. **Edge Case**: What is one common mistake or trap students make when solving questions in this area?
                3. **Real-world Application**: Where would you observe or apply this principle in practice?
                
                💡 *Tip: Explaining the answer out loud solidifies neural pathways 3x faster than passive re-reading.*
            """.trimIndent()
        }

        // Default General Focus Advice
        return """
            🧠 **StudyGuard AI Focus Strategy**
            
            • **Focus State**: To enter cognitive flow, your working memory requires ~12 uninterrupted minutes. Every time you switch to social media, your brain suffers 23 minutes of attention residue.
            • **Execution Target**: Commit to completing today's required study session. Turn on Binaural Alpha Beats in the Music tab.
            • **Today's Habit**: *“I am capable of immense focus. Distractions have no power over my ambition.”*
            
            Tell me what subject you're working on, and I'll generate a personalized revision breakdown!
        """.trimIndent()
    }
}
