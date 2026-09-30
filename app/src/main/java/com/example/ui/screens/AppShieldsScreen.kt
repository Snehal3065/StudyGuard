package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.BlockedAppEntity
import com.example.ui.StudyUiState
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.ActiveAutoLockStatus
import com.example.util.AutoLockScheduleRule
import java.util.Calendar

@Composable
fun AppShieldsScreen(
    uiState: StudyUiState,
    allApps: List<BlockedAppEntity>,
    autoLockRules: List<AutoLockScheduleRule>,
    activeAutoLockStatus: ActiveAutoLockStatus?,
    onToggleYouTubeShorts: (Boolean) -> Unit,
    onToggleAppDistraction: (packageName: String, isDistraction: Boolean) -> Unit,
    onBlockAllSocialAndGames: () -> Unit,
    onAllowAllApps: () -> Unit,
    onAddCustomApp: (packageName: String, appName: String, category: String) -> Unit,
    onSimulateLockOverlay: (appName: String) -> Unit,
    onAddAutoLockRule: (name: String, days: List<Int>, startH: Int, startM: Int, endH: Int, endM: Int, studyMins: Int) -> Unit,
    onUpdateAutoLockRule: (AutoLockScheduleRule) -> Unit = {},
    onToggleAutoLockRule: (id: String, isEnabled: Boolean) -> Unit,
    onDeleteAutoLockRule: (id: String) -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterChange: (String) -> Unit = {},
    initialSubTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filters = listOf("All", "Locked (Distractions)", "Allowed (Study Tools)", "Social", "Games")

    val filteredApps = remember(allApps, uiState.searchQuery, uiState.selectedCategoryFilter) {
        allApps.filter { app ->
            val matchesSearch = app.appName.contains(uiState.searchQuery, ignoreCase = true) ||
                    app.packageName.contains(uiState.searchQuery, ignoreCase = true)

            val matchesFilter = when (uiState.selectedCategoryFilter) {
                "Locked (Distractions)" -> app.isDistraction
                "Allowed (Study Tools)" -> !app.isDistraction
                "Social" -> app.category.equals("Social", ignoreCase = true)
                "Games" -> app.category.equals("Games", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val lockedCount = remember(allApps) { allApps.count { it.isDistraction } }
    val allowedCount = remember(allApps) { allApps.count { !it.isDistraction } }

    var activeSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) } // 0: Custom Locks, 1: Blocked Apps, 2: Laptop & Web
    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AutoLockScheduleRule?>(null) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Sub-tabs: Custom Locks vs Blocked Apps vs Laptop & Web Guard
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.LockClock, contentDescription = null, modifier = Modifier.size(16.dp), tint = TertiaryAmber)
                        Text("Custom Locks (${autoLockRules.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Smartphone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text("Blocked Apps ($lockedCount)", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Laptop, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text("Laptop Guard", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            )
        }

        when (activeSubTab) {
            2 -> LaptopWebGuardScreen()
            0 -> {
                // Custom Locks (Auto-Lock Schedules: Study-to-Unlock Gate)
                AutoLockSchedulesTab(
                    rules = autoLockRules,
                    activeStatus = activeAutoLockStatus,
                    onAddNewRule = { showAddScheduleDialog = true },
                    onEditRule = { rule -> editingRule = rule },
                    onToggleRule = onToggleAutoLockRule,
                    onDeleteRule = { id ->
                        onDeleteAutoLockRule(id)
                        Toast.makeText(context, "Lock schedule deleted!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            else -> {
                // Blocked Phone Apps Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active Scheduled Gate Banner if currently restricting
                    if (activeAutoLockStatus != null) {
                        Surface(
                            color = TertiaryAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TertiaryAmber.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TertiaryAmber)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Active Gate: ${activeAutoLockStatus.rule.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Must study for ${activeAutoLockStatus.remainingStudyMinutes} more mins today to unlock apps!",
                                        fontSize = 12.sp,
                                        color = TertiaryAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Special YouTube Shorts Blocker Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF0000).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SmartDisplay,
                                            contentDescription = "YouTube",
                                            tint = Color(0xFFFF0000)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "YouTube Shorts Shield",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Study Videos Allowed • Shorts Blocked",
                                            fontSize = 11.sp,
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isYouTubeShortsBlocked,
                                    onCheckedChange = onToggleYouTubeShorts,
                                    modifier = Modifier.testTag("youtube_shorts_switch")
                                )
                            }
                        }
                    }

                    // Quick Batch Actions & Custom App Add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$lockedCount locked • $allowedCount allowed",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onBlockAllSocialAndGames,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(32.dp).testTag("lock_social_games_button")
                            ) {
                                Text("Lock Social & Games", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = onAllowAllApps,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(32.dp).testTag("allow_all_apps_button")
                            ) {
                                Text("Allow All", fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { showAddCustomAppDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Custom App", tint = PrimaryIndigo)
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search installed or popular apps...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("app_search_field"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Filter chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { f ->
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == f,
                                onClick = { onFilterChange(f) },
                                label = { Text(f, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Apps List
                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No apps match filter",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppShieldRow(
                                    app = app,
                                    onToggle = { isChecked ->
                                        onToggleAppDistraction(app.packageName, isChecked)
                                    },
                                    onTestLock = {
                                        onSimulateLockOverlay(app.appName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom App Dialog
    if (showAddCustomAppDialog) {
        var appName by remember { mutableStateOf("") }
        var pkgName by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Social") }

        AlertDialog(
            onDismissRequest = { showAddCustomAppDialog = false },
            title = { Text("Add Custom App / Package", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("App Name (e.g. My Game)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pkgName,
                        onValueChange = { pkgName = it },
                        label = { Text("Package Name (e.g. com.game.pkg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (appName.isNotBlank() && pkgName.isNotBlank()) {
                            onAddCustomApp(pkgName.trim(), appName.trim(), category)
                            showAddCustomAppDialog = false
                            Toast.makeText(context, "Added $appName to locked apps!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Add & Block")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCustomAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Auto-Lock Schedule Rule Dialog
    if (showAddScheduleDialog) {
        AddScheduleRuleDialog(
            onConfirm = { name, days, startH, startM, endH, endM, studyMins ->
                onAddAutoLockRule(name, days, startH, startM, endH, endM, studyMins)
                showAddScheduleDialog = false
                Toast.makeText(context, "Auto-lock rule created!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddScheduleDialog = false }
        )
    }

    // Edit Auto-Lock Schedule Rule Dialog
    editingRule?.let { rule ->
        AddScheduleRuleDialog(
            initialName = rule.name,
            initialDays = rule.daysOfWeek.toSet(),
            initialStartH = rule.startHour,
            initialStartM = rule.startMinute,
            initialEndH = rule.endHour,
            initialEndM = rule.endMinute,
            initialStudyMins = rule.requiredStudyMinutes,
            title = "Edit Lock Schedule",
            confirmButtonText = "Save Changes",
            onConfirm = { name, days, startH, startM, endH, endM, studyMins ->
                onUpdateAutoLockRule(
                    rule.copy(
                        name = name,
                        daysOfWeek = days,
                        startHour = startH,
                        startMinute = startM,
                        endHour = endH,
                        endMinute = endM,
                        requiredStudyMinutes = studyMins
                    )
                )
                editingRule = null
                Toast.makeText(context, "Lock schedule updated!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { editingRule = null }
        )
    }
}

@Composable
private fun AppShieldRow(
    app: BlockedAppEntity,
    onToggle: (Boolean) -> Unit,
    onTestLock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isDistraction)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (app.isDistraction) Color(0xFFEF4444).copy(alpha = 0.15f)
                            else SecondaryTeal.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isDistraction) Icons.Default.Lock else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (app.isDistraction) Color(0xFFEF4444) else SecondaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (app.isDistraction) {
                            Text(
                                text = "• Locked",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (app.isDistraction) {
                    IconButton(
                        onClick = onTestLock,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Test Lock Screen",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Switch(
                    checked = app.isDistraction,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("app_switch_${app.packageName}")
                )
            }
        }
    }
}

@Composable
private fun AutoLockSchedulesTab(
    rules: List<AutoLockScheduleRule>,
    activeStatus: ActiveAutoLockStatus?,
    onAddNewRule: () -> Unit,
    onEditRule: (AutoLockScheduleRule) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onDeleteRule: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        Icon(Icons.Filled.LockClock, contentDescription = null, tint = TertiaryAmber)
                    }
                    Column {
                        Text(
                            text = "Auto-Lock: Study-to-Unlock",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Automatic distraction lockdown with study hours requirement",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "Locks your distraction apps at set times on designated days. The apps only unlock once you finish your required study goal for that day!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Active Status Indicator
        if (activeStatus != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB45309))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔒 Rule Active Now: ${activeStatus.rule.name}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Studied today: ${activeStatus.completedStudyMinutes}m / ${activeStatus.rule.requiredStudyMinutes}m required (${activeStatus.remainingStudyMinutes}m remaining)",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONFIGURED RULES (${rules.count { it.isEnabled }} active)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Button(
                onClick = onAddNewRule,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ New Lock Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (rules.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.LockClock,
                        contentDescription = null,
                        tint = TertiaryAmber,
                        modifier = Modifier.size(42.dp)
                    )
                    Text("No Custom Locks Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Set daily or weekly schedules (e.g. Weekdays 8:30 AM with a 2-hour study requirement before distraction apps unlock).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Button(
                        onClick = onAddNewRule,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Custom Lock")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    ScheduleRuleCard(
                        rule = rule,
                        onToggle = { isChecked -> onToggleRule(rule.id, isChecked) },
                        onEdit = { onEditRule(rule) },
                        onDelete = { onDeleteRule(rule.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRuleCard(
    rule: AutoLockScheduleRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TertiaryAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TertiaryAmber, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = rule.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = rule.daysFormatted,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LOCK WINDOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${rule.formattedStartTime} – ${rule.formattedEndTime}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "REQUIRED STUDY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rule.formattedRequiredStudy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = TertiaryAmber
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AddScheduleRuleDialog(
    initialName: String = "Morning Grind Gate",
    initialDays: Set<Int> = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
    initialStartH: Int = 8,
    initialStartM: Int = 30,
    initialEndH: Int = 13,
    initialEndM: Int = 0,
    initialStudyMins: Int = 120,
    title: String = "New Auto-Lock Schedule",
    confirmButtonText: String = "Create Gate",
    onConfirm: (name: String, days: List<Int>, startH: Int, startM: Int, endH: Int, endM: Int, studyMins: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedDays by remember { mutableStateOf(initialDays) }
    var startHour by remember { mutableIntStateOf(initialStartH) }
    var startMinute by remember { mutableIntStateOf(initialStartM) }
    var endHour by remember { mutableIntStateOf(initialEndH) }
    var endMinute by remember { mutableIntStateOf(initialEndM) }
    var studyHours by remember { mutableIntStateOf(initialStudyMins / 60) }
    var studyMinutes by remember { mutableIntStateOf(initialStudyMins % 60) }

    val daysMap = listOf(
        Pair("M", Calendar.MONDAY),
        Pair("T", Calendar.TUESDAY),
        Pair("W", Calendar.WEDNESDAY),
        Pair("T", Calendar.THURSDAY),
        Pair("F", Calendar.FRIDAY),
        Pair("S", Calendar.SATURDAY),
        Pair("S", Calendar.SUNDAY)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Schedule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Active Days of Week:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysMap.forEach { (label, dayCode) ->
                        val isSelected = selectedDays.contains(dayCode)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        if (selectedDays.size > 1) selectedDays - dayCode else selectedDays
                                    } else {
                                        selectedDays + dayCode
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Start Time & End Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time: ${String.format("%02d:%02d", startHour, startMinute)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { startHour = (startHour + 1) % 24 }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(String.format("%02d", startHour), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { startHour = if (startHour == 0) 23 else startHour - 1 }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(":")
                            IconButton(onClick = { startMinute = (startMinute + 15) % 60 }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(String.format("%02d", startMinute), fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time: ${String.format("%02d:%02d", endHour, endMinute)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { endHour = (endHour + 1) % 24 }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(String.format("%02d", endHour), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { endHour = if (endHour == 0) 23 else endHour - 1 }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(":")
                            IconButton(onClick = { endMinute = (endMinute + 15) % 60 }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(String.format("%02d", endMinute), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Study Requirement: Hours & Mins
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Required Study to Unlock Apps:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { studyHours = (studyHours + 1).coerceAtMost(10) }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text("$studyHours hrs", fontWeight = FontWeight.Black, color = TertiaryAmber)
                                IconButton(onClick = { studyHours = (studyHours - 1).coerceAtLeast(0) }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { studyMinutes = (studyMinutes + 15) % 60 }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text("$studyMinutes mins", fontWeight = FontWeight.Black, color = TertiaryAmber)
                                IconButton(onClick = { studyMinutes = if (studyMinutes < 15) 45 else studyMinutes - 15 }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalMins = (studyHours * 60 + studyMinutes).coerceAtLeast(15)
                    onConfirm(name, selectedDays.toList(), startHour, startMinute, endHour, endMinute, totalMins)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
