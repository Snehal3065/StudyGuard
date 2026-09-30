package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TertiaryAmber
import com.example.util.FlashcardDeck
import com.example.util.FlashcardItem
import com.example.util.FlashcardManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { FlashcardManager.getInstance(context) }
    var decks by remember { mutableStateOf(manager.getAllDecks()) }

    var activePracticeDeck by remember { mutableStateOf<FlashcardDeck?>(null) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showAddCardDialogDeck by remember { mutableStateOf<FlashcardDeck?>(null) }

    fun refreshDecks() {
        decks = manager.getAllDecks()
        activePracticeDeck = activePracticeDeck?.let { current ->
            decks.find { it.id == current.id }
        }
    }

    if (activePracticeDeck != null) {
        FlashcardPracticeView(
            deck = activePracticeDeck!!,
            onClose = { activePracticeDeck = null; refreshDecks() },
            onRateCard = { cardId, diff ->
                manager.updateCardDifficulty(activePracticeDeck!!.id, cardId, diff)
                refreshDecks()
            },
            onAddCard = { showAddCardDialogDeck = activePracticeDeck }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🗂️", fontSize = 20.sp)
                        Text("Active Recall Decks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDeckDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Deck", tint = PrimaryIndigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1B4B)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🧠 100% Offline Active Recall",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Testing yourself from memory creates 3x stronger neural connections than passive reading. Tap any deck to begin spaced repetition testing.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Decks List
            items(decks, key = { it.id }) { deck ->
                DeckCard(
                    deck = deck,
                    onOpenPractice = { activePracticeDeck = deck },
                    onAddCard = { showAddCardDialogDeck = deck },
                    onDelete = {
                        manager.deleteDeck(deck.id)
                        refreshDecks()
                        Toast.makeText(context, "Deck deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Create Deck Dialog
    if (showCreateDeckDialog) {
        CreateDeckDialog(
            onConfirm = { title, subject, color ->
                manager.addDeck(title, subject, color)
                refreshDecks()
                showCreateDeckDialog = false
                Toast.makeText(context, "Deck created!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCreateDeckDialog = false }
        )
    }

    // Add Card Dialog
    showAddCardDialogDeck?.let { deck ->
        AddCardDialog(
            deckTitle = deck.title,
            onConfirm = { q, a, hint ->
                manager.addCardToDeck(deck.id, q, a, hint)
                refreshDecks()
                showAddCardDialogDeck = null
                Toast.makeText(context, "Card added to ${deck.title}!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddCardDialogDeck = null }
        )
    }
}

@Composable
private fun DeckCard(
    deck: FlashcardDeck,
    onOpenPractice: () -> Unit,
    onAddCard: () -> Unit,
    onDelete: () -> Unit
) {
    val deckColor = remember(deck.colorHex) {
        try { Color(android.graphics.Color.parseColor(deck.colorHex)) } catch (e: Exception) { PrimaryIndigo }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPractice),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(deckColor)
                    )
                    Text(
                        text = deck.subject.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = deckColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddCard, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Card", tint = deckColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = deck.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Progress & Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${deck.totalCount} cards • ${deck.masteredCount} mastered (${deck.masteryPercent}%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onOpenPractice,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = deckColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Study ▶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { (deck.masteryPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = deckColor,
                trackColor = deckColor.copy(alpha = 0.2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardPracticeView(
    deck: FlashcardDeck,
    onClose: () -> Unit,
    onRateCard: (cardId: String, difficulty: String) -> Unit,
    onAddCard: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val cards = deck.cards

    if (cards.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("📭", fontSize = 48.sp)
                Text("This deck has no flashcards yet.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = onAddCard) {
                    Text("+ Add First Card")
                }
                TextButton(onClick = onClose) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val currentCard = cards[currentIndex.coerceIn(0, cards.lastIndex)]

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "flip"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deck.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Card ${currentIndex + 1} of ${cards.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onAddCard) {
                        Icon(Icons.Default.Add, contentDescription = "Add Card")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive 3D Flip Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { isFlipped = !isFlipped },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) Color(0xFF1E293B) else Color(0xFF1E1B4B)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // Front: Question
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "QUESTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = currentCard.question,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    lineHeight = 26.sp
                                )
                                if (currentCard.hint.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "💡 Hint: ${currentCard.hint}",
                                        fontSize = 12.sp,
                                        color = TertiaryAmber,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Tap to Reveal Answer 🔄",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        } else {
                            // Back: Answer (Counter-rotated so text is right-side up)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { rotationY = 180f }
                            ) {
                                Text(
                                    text = "ANSWER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessEmerald,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = currentCard.answer,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFF8FAFC),
                                    lineHeight = 26.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Rate your recall below to advance ⬇️",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rating Buttons (Active Recall Feedback)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onRateCard(currentCard.id, "HARD")
                        isFlipped = false
                        currentIndex = (currentIndex + 1) % cards.size
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hard 😬", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onRateCard(currentCard.id, "GOOD")
                        isFlipped = false
                        currentIndex = (currentIndex + 1) % cards.size
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Good 👍", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onRateCard(currentCard.id, "EASY")
                        isFlipped = false
                        currentIndex = (currentIndex + 1) % cards.size
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Easy 🌟", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreateDeckDialog(
    onConfirm: (title: String, subject: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Mathematics") }
    val colors = listOf("#6366F1", "#10B981", "#06B6D4", "#F59E0B", "#EC4899", "#8B5CF6")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flashcard Deck", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck Title (e.g. Chapter 4 Formulas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Physics, History)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Accent Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = hex }
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, subject, selectedColor)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCardDialog(
    deckTitle: String,
    onConfirm: (question: String, answer: String, hint: String) -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card to $deckTitle", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question / Concept prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer / Explanation") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        onConfirm(question, answer, hint)
                    }
                },
                enabled = question.isNotBlank() && answer.isNotBlank()
            ) {
                Text("Add Card")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
