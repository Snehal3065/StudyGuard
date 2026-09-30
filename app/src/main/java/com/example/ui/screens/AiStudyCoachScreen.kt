package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.ChatMessage
import com.example.util.CoachRole
import com.example.util.MessageSender
import kotlinx.coroutines.launch

@Composable
fun AiStudyCoachScreen(
    chatMessages: List<ChatMessage>,
    isLoading: Boolean,
    selectedRole: CoachRole,
    isLiveApi: Boolean,
    onSendMessage: (String) -> Unit,
    onSelectRole: (CoachRole) -> Unit,
    onClearChat: () -> Unit,
    onStartStudyWithSubject: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val presetPrompts = listOf(
        "Generate a 3-hour deep work timetable for my exams",
        "I have a strong urge to open Instagram/YouTube, stop me",
        "Create 3 active-recall quiz questions on Photosynthesis",
        "How do I use the Feynman technique to master hard physics?",
        "Give me an urgent, direct kick to start studying right now"
    )

    // Auto-scroll to bottom on new messages
    LaunchedEffect(chatMessages.size, isLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("ai_coach_back_button")) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "StudyGuard AI Chatbot",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PrimaryIndigo,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isLiveApi) "Powered by Gemini (${selectedRole.model})" else "Offline Mode (Local AI Heuristics)",
                    fontSize = 11.sp,
                    color = if (isLiveApi) SecondaryTeal else TertiaryAmber,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = if (isLiveApi) Color(0xFF10B981).copy(alpha = 0.2f) else TertiaryAmber.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (isLiveApi) Color(0xFF10B981) else TertiaryAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isLiveApi) "Cloud AI" else "Offline AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveApi) Color(0xFF10B981) else TertiaryAmber
                    )
                }
            }

            IconButton(onClick = onClearChat) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Persona / Role Selection Chips (System Instructions)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "AI COACH PERSONA & MODEL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CoachRole.values()) { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { onSelectRole(role) },
                        label = {
                            Text(role.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Scrollable Chat Thread
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onStartTimer = {
                            onStartStudyWithSubject("AI Study Plan")
                            onDismiss()
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryIndigo
                            )
                            Text(
                                text = "StudyGuard AI is thinking...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(presetPrompts) { prompt ->
                AssistChip(
                    onClick = {
                        promptInput = prompt
                        onSendMessage(prompt)
                        promptInput = ""
                    },
                    label = { Text(prompt, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(12.dp), tint = TertiaryAmber)
                    }
                )
            }
        }

        // Chat Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask study question, timetable, or motivation...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_coach_input"),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )

            FloatingActionButton(
                onClick = {
                    if (promptInput.isNotBlank() && !isLoading) {
                        val text = promptInput
                        promptInput = ""
                        onSendMessage(text)
                    }
                },
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_coach_send_button")
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onStartTimer: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = if (isUser) PrimaryIndigo else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                tonalElevation = if (isUser) 0.dp else 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (message.isLiveApi) "Gemini Cloud" else "Offline Engine",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = "Copy text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (message.text.contains("Block") || message.text.contains("Schedule") || message.text.contains("Timer")) {
                                    IconButton(onClick = onStartTimer, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Start Timer",
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
