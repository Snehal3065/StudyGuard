package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber

@Composable
fun ChromeExtensionHelpScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSimulator by remember { mutableStateOf(false) }
    var testUrlInput by remember { mutableStateOf("https://www.youtube.com/shorts/dQw4w9WgXcQ") }
    var simulationResult by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied '$text' to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("help_close_button")) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Chrome Extension Setup",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PrimaryIndigo
                )
            }
            Surface(
                color = Color(0xFF4285F4).copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = "Google Chrome Guide",
                    color = Color(0xFF4285F4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1B4B)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF34A853)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Extension,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "StudyGuard 2.0 on Desktop",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "With 45/15 Marathon & YouTube Focus Shield",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "Protects your deep work on laptops with custom avatar styling, zero-distraction YouTube (subscriptions & recommendation feeds locked), and automated 45m study / 15m rest video marathons!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Feature Highlights Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF6366F1).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🛡️ PFP Avatar",
                            color = Color(0xFFA5B4FC),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🚫 Subscriptions Blocked",
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🎓 45/15 Marathon",
                            color = Color(0xFF6EE7B7),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "STEP-BY-STEP INSTALLATION GUIDE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Step 1
        SetupStepCard(
            stepNumber = "1",
            title = "Locate 'laptop-extension' Folder",
            description = "The project includes a ready-to-use Manifest V3 extension in the 'laptop-extension/' folder with pre-built declarative blocking rules and focus screen.",
            actionLabel = "Folder: /laptop-extension",
            onAction = { copyToClipboard("Folder Path", "laptop-extension") }
        )

        // Step 2
        SetupStepCard(
            stepNumber = "2",
            title = "Open Chrome Extensions Page",
            description = "Launch Google Chrome on your laptop, click the address bar, and type or paste: chrome://extensions",
            actionLabel = "Copy 'chrome://extensions'",
            onAction = { copyToClipboard("Chrome URL", "chrome://extensions") }
        )

        // Step 3
        SetupStepCard(
            stepNumber = "3",
            title = "Turn ON 'Developer mode'",
            description = "In the top-right corner of the Extensions page, switch the toggle labeled 'Developer mode' to ON. This enables the unpacked loader.",
            actionLabel = null,
            onAction = null
        )

        // Step 4
        SetupStepCard(
            stepNumber = "4",
            title = "Click 'Load unpacked'",
            description = "Click the button in the top-left labeled 'Load unpacked'. In the file browser, navigate to and select your 'laptop-extension' folder.",
            actionLabel = null,
            onAction = null
        )

        // Step 5
        SetupStepCard(
            stepNumber = "5",
            title = "Pin to Toolbar & Start Studying!",
            description = "Click the puzzle piece icon 🧩 next to Chrome's address bar and pin StudyGuard 🛡️. Click it anytime to toggle Focus Mode or view paired status!",
            actionLabel = null,
            onAction = null
        )

        // Interactive URL Blocker Simulator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = TertiaryAmber)
                        Text(
                            text = "Extension URL Blocker Simulator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    TextButton(onClick = { showSimulator = !showSimulator }) {
                        Text(if (showSimulator) "Hide" else "Try It")
                    }
                }

                AnimatedVisibility(visible = showSimulator) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Test how the extension handles desktop links:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = testUrlInput,
                            onValueChange = { testUrlInput = it },
                            label = { Text("URL to test") },
                            modifier = Modifier.fillMaxWidth().testTag("simulator_url_input"),
                            singleLine = true
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val url = testUrlInput.lowercase()
                                    simulationResult = when {
                                        url.contains("youtube.com/shorts") ->
                                            "🛑 BLOCKED: YouTube Shorts detected! Redirecting to StudyGuard Focus Screen. (Regular YouTube lectures remain allowed!)"
                                        url.contains("youtube.com/feed/subscriptions") || url.contains("youtube.com/feed/explore") || url.contains("youtube.com/feed/trending") ->
                                            "🛑 BLOCKED: Subscriptions/Explore feed restricted! Subscriptions tab is hidden; only intentional lecture study on Home is permitted."
                                        url.contains("instagram.com") || url.contains("tiktok.com") || url.contains("reddit.com") ->
                                            "🛑 BLOCKED: Social distraction site locked during active study session."
                                        url.contains("youtube.com/watch") ->
                                            "🎓 MARATHON READY: Locked lecture video! 45 min study / 15 min rest intervals active. Recommendations & sidebar removed."
                                        url.contains("wikipedia.org") || url.contains("scholar.google.com") ->
                                            "✅ ALLOWED: Educational research resource permitted."
                                        else ->
                                            "ℹ️ ALLOWED: Domain not in distraction blocklist."
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("simulate_test_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Text("Test URL")
                            }

                            OutlinedButton(
                                onClick = { testUrlInput = "https://www.youtube.com/watch?v=calculus_lecture" }
                            ) {
                                Text("Sample Lecture", fontSize = 11.sp)
                            }
                        }

                        if (simulationResult != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (simulationResult!!.contains("BLOCKED")) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = simulationResult!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (simulationResult!!.contains("BLOCKED")) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Browser Compatibility Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryIndigo)
                Column {
                    Text(
                        text = "Compatible with all Chromium Browsers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Works exactly the same on Microsoft Edge (edge://extensions), Brave (brave://extensions), and Opera!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onAction,
                        modifier = Modifier.testTag("step_${stepNumber}_action")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
