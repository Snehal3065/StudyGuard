package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FlashcardItem(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answer: String,
    val hint: String = "",
    val difficulty: String = "NEW", // NEW, HARD, GOOD, EASY
    val reviewCount: Int = 0,
    val lastReviewed: Long = 0L
)

data class FlashcardDeck(
    val id: String,
    val title: String,
    val subject: String,
    val colorHex: String,
    val cards: List<FlashcardItem>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalCount: Int get() = cards.size
    val masteredCount: Int get() = cards.count { it.difficulty == "EASY" }
    val masteryPercent: Int get() = if (cards.isEmpty()) 0 else (masteredCount * 100 / cards.size)
}

class FlashcardManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_flashcards_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DECKS_JSON = "flashcard_decks_json"
        private const val KEY_INITIALIZED = "flashcards_init_v1"

        @Volatile
        private var instance: FlashcardManager? = null

        fun getInstance(context: Context): FlashcardManager {
            return instance ?: synchronized(this) {
                instance ?: FlashcardManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val defaultDecks = listOf(
        FlashcardDeck(
            id = "deck_bio_respiration",
            title = "Cellular Respiration & Bioenergetics",
            subject = "Biology",
            colorHex = "#10B981",
            cards = listOf(
                FlashcardItem(
                    question = "Where does Glycolysis occur in eukaryotic cells, and what is the net ATP yield?",
                    answer = "In the Cytosol (Cytoplasm). The net yield is 2 ATP and 2 NADH per glucose molecule.",
                    hint = "Occurs outside the mitochondria"
                ),
                FlashcardItem(
                    question = "What is the final electron acceptor in the mitochondrial Electron Transport Chain (ETC)?",
                    answer = "Molecular Oxygen (O₂). It binds with free protons to form Water (H₂O).",
                    hint = "Crucial for aerobic respiration"
                ),
                FlashcardItem(
                    question = "What enzyme synthesizes ATP driven by the proton motive force?",
                    answer = "ATP Synthase. Protons flow from the intermembrane space back into the mitochondrial matrix.",
                    hint = "Acts like a molecular turbine"
                ),
                FlashcardItem(
                    question = "How many total ATP molecules are produced theoretically from 1 glucose in aerobic respiration?",
                    answer = "Approximately 30 to 32 ATP molecules (accounting for proton leak and NADH transport).",
                    hint = "Standard textbook range is 30-32"
                )
            )
        ),
        FlashcardDeck(
            id = "deck_calculus_derivatives",
            title = "Calculus: Essential Derivatives & Rules",
            subject = "Mathematics",
            colorHex = "#6366F1",
            cards = listOf(
                FlashcardItem(
                    question = "What is the Product Rule for finding the derivative of f(x) · g(x)?",
                    answer = "(f · g)' = f'(x)g(x) + f(x)g'(x)",
                    hint = "Derivative of the first times second, plus first times derivative of second"
                ),
                FlashcardItem(
                    question = "State the Chain Rule for composite functions: [f(g(x))]'",
                    answer = "f'(g(x)) · g'(x)",
                    hint = "Differentiate outside function, then multiply by inner derivative"
                ),
                FlashcardItem(
                    question = "What is the derivative of ln(x) for x > 0?",
                    answer = "1 / x",
                    hint = "Reciprocal function"
                ),
                FlashcardItem(
                    question = "What is the derivative of e^(kx)?",
                    answer = "k · e^(kx)",
                    hint = "Use Chain Rule with inner function kx"
                )
            )
        ),
        FlashcardDeck(
            id = "deck_cs_algorithms",
            title = "Algorithms & Data Structures Complexity",
            subject = "Computer Science",
            colorHex = "#06B6D4",
            cards = listOf(
                FlashcardItem(
                    question = "What is the average and worst-case time complexity of QuickSort?",
                    answer = "Average: O(N log N). Worst case: O(N²) (occurs with poorly chosen pivots on sorted arrays).",
                    hint = "Divide-and-conquer partitioning"
                ),
                FlashcardItem(
                    question = "What is the average time complexity of lookups in a balanced Binary Search Tree (AVL / Red-Black)?",
                    answer = "O(log N) for Search, Insert, and Delete.",
                    hint = "Tree height is guaranteed logarithmic"
                ),
                FlashcardItem(
                    question = "What is the difference between BFS and DFS queue/stack usage?",
                    answer = "BFS uses a FIFO Queue (level-by-level exploration). DFS uses a LIFO Stack / Recursion.",
                    hint = "Queue vs Call Stack"
                )
            )
        ),
        FlashcardDeck(
            id = "deck_physics_laws",
            title = "Core Physics Laws & Thermodynamics",
            subject = "Physics",
            colorHex = "#F59E0B",
            cards = listOf(
                FlashcardItem(
                    question = "State the First Law of Thermodynamics in equation form.",
                    answer = "ΔU = Q - W (Change in internal energy equals heat added to system minus work done by system).",
                    hint = "Conservation of Energy"
                ),
                FlashcardItem(
                    question = "What does the Second Law of Thermodynamics state about entropy?",
                    answer = "The total entropy of an isolated system always increases over time in any spontaneous process (ΔS_total > 0).",
                    hint = "Disorder of the universe increases"
                ),
                FlashcardItem(
                    question = "What is Snell's Law of refraction?",
                    answer = "n₁ · sin(θ₁) = n₂ · sin(θ₂)",
                    hint = "Relates refractive indices and angles of incidence/refraction"
                )
            )
        )
    )

    fun getAllDecks(): List<FlashcardDeck> {
        val isInitialized = prefs.getBoolean(KEY_INITIALIZED, false)
        if (!isInitialized) {
            saveDecks(defaultDecks)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
            return defaultDecks
        }

        val json = prefs.getString(KEY_DECKS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<FlashcardDeck>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cardsArr = obj.getJSONArray("cards")
                val cards = mutableListOf<FlashcardItem>()
                for (j in 0 until cardsArr.length()) {
                    val cObj = cardsArr.getJSONObject(j)
                    cards.add(
                        FlashcardItem(
                            id = cObj.getString("id"),
                            question = cObj.getString("question"),
                            answer = cObj.getString("answer"),
                            hint = cObj.optString("hint", ""),
                            difficulty = cObj.optString("difficulty", "NEW"),
                            reviewCount = cObj.optInt("reviewCount", 0),
                            lastReviewed = cObj.optLong("lastReviewed", 0L)
                        )
                    )
                }
                list.add(
                    FlashcardDeck(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subject = obj.getString("subject"),
                        colorHex = obj.optString("colorHex", "#6366F1"),
                        cards = cards,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultDecks
        }
    }

    fun saveDecks(decks: List<FlashcardDeck>) {
        val arr = JSONArray()
        decks.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("subject", d.subject)
                put("colorHex", d.colorHex)
                put("createdAt", d.createdAt)
                val cardsArr = JSONArray()
                d.cards.forEach { c ->
                    val cObj = JSONObject().apply {
                        put("id", c.id)
                        put("question", c.question)
                        put("answer", c.answer)
                        put("hint", c.hint)
                        put("difficulty", c.difficulty)
                        put("reviewCount", c.reviewCount)
                        put("lastReviewed", c.lastReviewed)
                    }
                    cardsArr.put(cObj)
                }
                put("cards", cardsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DECKS_JSON, arr.toString()).apply()
    }

    fun addDeck(title: String, subject: String, colorHex: String): FlashcardDeck {
        val newDeck = FlashcardDeck(
            id = "deck_${System.currentTimeMillis()}",
            title = title.trim().ifBlank { "Study Deck" },
            subject = subject.trim().ifBlank { "General" },
            colorHex = colorHex,
            cards = emptyList()
        )
        val current = getAllDecks().toMutableList().apply { add(0, newDeck) }
        saveDecks(current)
        return newDeck
    }

    fun addCardToDeck(deckId: String, question: String, answer: String, hint: String = "") {
        val card = FlashcardItem(
            question = question.trim(),
            answer = answer.trim(),
            hint = hint.trim()
        )
        val current = getAllDecks().map { d ->
            if (d.id == deckId) {
                d.copy(cards = d.cards + card)
            } else d
        }
        saveDecks(current)
    }

    fun updateCardDifficulty(deckId: String, cardId: String, difficulty: String) {
        val current = getAllDecks().map { d ->
            if (d.id == deckId) {
                d.copy(cards = d.cards.map { c ->
                    if (c.id == cardId) {
                        c.copy(
                            difficulty = difficulty,
                            reviewCount = c.reviewCount + 1,
                            lastReviewed = System.currentTimeMillis()
                        )
                    } else c
                })
            } else d
        }
        saveDecks(current)
    }

    fun deleteDeck(deckId: String) {
        val current = getAllDecks().filterNot { it.id == deckId }
        saveDecks(current)
    }
}
