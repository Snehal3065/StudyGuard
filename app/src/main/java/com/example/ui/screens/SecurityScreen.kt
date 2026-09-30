package com.example.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.receiver.StudyDeviceAdminReceiver
import com.example.ui.StudyUiState
import com.example.ui.lock.BlockedOverlayActivity
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.UsageStatsManagerHelper

@Composable
fun SecurityScreen(
    uiState: StudyUiState,
    onToggleStrictLock: (Boolean) -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ironclad Study Shields",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uninstall Protection & Active Enforcement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "To guarantee you cannot quit or delete the app during study sessions, StudyGuard uses Android Device Administration, Accessibility services, and Usage Stats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Permission 1: Device Admin (Prevents Uninstall)
        PermissionActionCard(
            title = "Uninstall Protection (Device Admin)",
            description = "Prevents uninstalling StudyGuard during active study hours. Android OS blocks uninstallation of active Device Administrators.",
            isGranted = uiState.isDeviceAdminEnabled,
            actionButtonText = if (uiState.isDeviceAdminEnabled) "Protected (Active)" else "Enable Device Admin",
            onActionClick = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        ComponentName(context, StudyDeviceAdminReceiver::class.java)
                    )
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.device_admin_description)
                    )
                }
                context.startActivity(intent)
            }
        )

        // Permission 2: Accessibility Service (Monitors distraction apps & YouTube Shorts)
        PermissionActionCard(
            title = "Focus & Shorts Service (Accessibility)",
            description = "Monitors app switching to block distractions and detects YouTube Shorts immediately while preserving normal study videos.",
            isGranted = uiState.isAccessibilityEnabled,
            actionButtonText = if (uiState.isAccessibilityEnabled) "Monitoring (Active)" else "Enable Accessibility",
            onActionClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )

        // Permission 3: Usage Stats Access (UsageStatsManager)
        PermissionActionCard(
            title = "Usage Stats Manager Access",
            description = "Allows StudyGuard to query foreground app usage time, calculate distraction time, and provide screen time metrics.",
            isGranted = uiState.isUsageStatsEnabled,
            actionButtonText = if (uiState.isUsageStatsEnabled) "Granted (Active)" else "Enable Usage Access",
            onActionClick = {
                UsageStatsManagerHelper.openUsageAccessSettings(context)
            }
        )

        // Permission 4: Draw Over Other Apps (Floating Focus Widget)
        PermissionActionCard(
            title = "Floating Overlay Widget Permission",
            description = "Required to display the floating countdown bubble and quick study toggle while using other educational apps or YouTube.",
            isGranted = uiState.isOverlayEnabled,
            actionButtonText = if (uiState.isOverlayEnabled) "Allowed (Active)" else "Enable Display Over Apps",
            onActionClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    context.startActivity(intent)
                }
            }
        )

        // Strict Anti-Tamper Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anti-Tamper Lockdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Intercept attempts to access System Settings or app managers to kill or disable StudyGuard during study hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isStrictUninstallLockEnabled,
                    onCheckedChange = onToggleStrictLock,
                    modifier = Modifier.testTag("strict_lock_switch")
                )
            }
        }

        // Test & Verify Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Verify System Shields",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val ytIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                            if (ytIntent != null) {
                                context.startActivity(ytIntent)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("test_youtube_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OndemandVideo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open YouTube", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, BlockedOverlayActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_APP_NAME, "Distraction App Test")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).testTag("test_overlay_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Lock Screen", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PermissionActionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionButtonText: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                Surface(
                    color = if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isGranted) "ACTIVE" else "REQUIRED",
                        color = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permission_button_${title.take(6).lowercase()}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) SecondaryTeal else PrimaryIndigo
                )
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionButtonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
