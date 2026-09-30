package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.SmartReminderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRemindersScreen(
    reminders: List<SmartReminderItem>,
    onToggleReminder: (String, Boolean) -> Unit,
    onUpdateTime: (String, Int, Int) -> Unit,
    onAddCustomReminder: (title: String, hour: Int, minute: Int, message: String, iconTag: String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onSendTestReminder: (SmartReminderItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<SmartReminderItem?>(null) }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted! Reminders will alert on time.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please allow notifications in Settings for study reminders.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar - Fixed single line header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("reminders_back_button")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Study Reminders",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = PrimaryIndigo,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_custom_reminder_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TertiaryAmber.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Alarm, contentDescription = null, tint = TertiaryAmber)
                    }
                    Column {
                        Text(
                            text = "Never Miss a Study Session",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Flexible daily nudges • Custom times & messages",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "High-priority alarms ring with sound & vibration to kickstart deep work, beat slumps, and enforce study discipline.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCHEDULED NUDGES (${reminders.count { it.isEnabled }} active)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = {
                    if (reminders.isNotEmpty()) {
                        onSendTestReminder(reminders.first())
                        Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = SecondaryTeal)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Notification", fontSize = 11.sp, color = SecondaryTeal, fontWeight = FontWeight.Bold)
            }
        }

        // Reminders List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggle = { isChecked -> onToggleReminder(reminder.id, isChecked) },
                    onEdit = { editingReminder = reminder },
                    onDelete = { onDeleteReminder(reminder.id) },
                    onTest = {
                        onSendTestReminder(reminder)
                        Toast.makeText(context, "Notification sent: ${reminder.title}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Add Reminder Dialog
    if (showAddDialog) {
        AddOrEditReminderDialog(
            initialReminder = null,
            onConfirm = { title, hour, minute, message, iconTag ->
                onAddCustomReminder(title, hour, minute, message, iconTag)
                showAddDialog = false
                Toast.makeText(context, "Reminder added for ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Reminder Dialog
    if (editingReminder != null) {
        AddOrEditReminderDialog(
            initialReminder = editingReminder,
            onConfirm = { title, hour, minute, message, iconTag ->
                onUpdateTime(editingReminder!!.id, hour, minute)
                editingReminder = null
                Toast.makeText(context, "Time updated to ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { editingReminder = null }
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: SmartReminderItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (reminder.iconTag) {
                        "morning" -> Icons.Default.WbSunny
                        "afternoon" -> Icons.Default.Bolt
                        "evening" -> Icons.Default.Nightlight
                        "hydrate" -> Icons.Default.WaterDrop
                        "target" -> Icons.Default.CrisisAlert
                        else -> Icons.Default.MenuBook
                    }
                    val iconColor = when (reminder.iconTag) {
                        "morning" -> TertiaryAmber
                        "afternoon" -> Color(0xFFF59E0B)
                        "evening" -> PrimaryIndigo
                        "hydrate" -> SecondaryTeal
                        else -> PrimaryIndigo
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = reminder.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = reminder.formattedTime,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "• Daily",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("reminder_switch_${reminder.id}")
                )
            }

            Text(
                text = reminder.customMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            // Card Bottom Actions: Edit Time, Send Test, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Time", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilledTonalButton(
                    onClick = onTest,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alert", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOrEditReminderDialog(
    initialReminder: SmartReminderItem?,
    onConfirm: (title: String, hour: Int, minute: Int, message: String, iconTag: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "Focus Study Block") }
    var hour by remember { mutableIntStateOf(initialReminder?.hour ?: 9) }
    var minute by remember { mutableIntStateOf(initialReminder?.minute ?: 0) }
    var message by remember {
        mutableStateOf(
            initialReminder?.customMessage
                ?: "Put away distractions, grab your books, and lock in for a high-focus session!"
        )
    }
    var iconTag by remember { mutableStateOf(initialReminder?.iconTag ?: "study") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialReminder == null) "Add Custom Reminder" else "Edit Reminder Time",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Flexible Time Stepper (Hour & Minute)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set Time (24h or AM/PM)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hour controls
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { hour = (hour + 1) % 24 }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Hour Up")
                                }
                                Text(
                                    text = String.format("%02d", hour),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryIndigo
                                )
                                IconButton(onClick = { hour = if (hour == 0) 23 else hour - 1 }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hour Down")
                                }
                                Text("Hour", fontSize = 10.sp, color = Color.Gray)
                            }

                            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp))

                            // Minute controls
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { minute = (minute + 5) % 60 }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Minute Up")
                                }
                                Text(
                                    text = String.format("%02d", minute),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryIndigo
                                )
                                IconButton(onClick = { minute = if (minute < 5) 55 else minute - 5 }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minute Down")
                                }
                                Text("Minute", fontSize = 10.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            val period = if (hour >= 12) "PM" else "AM"
                            val displayHour = when {
                                hour == 0 -> 12
                                hour > 12 -> hour - 12
                                else -> hour
                            }
                            Surface(
                                color = TertiaryAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = String.format("%d:%02d %s", displayHour, minute, period),
                                    fontWeight = FontWeight.Bold,
                                    color = TertiaryAmber,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Motivational Message") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, hour, minute, message, iconTag) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(if (initialReminder == null) "Save Reminder" else "Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
