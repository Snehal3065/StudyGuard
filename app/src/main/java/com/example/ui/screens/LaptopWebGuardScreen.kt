package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class WebBlockItem(
    val domain: String,
    val name: String,
    val isShortsSpecial: Boolean = false,
    val isEnabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaptopWebGuardScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Default websites list
    var websiteList by remember {
        mutableStateOf(
            listOf(
                WebBlockItem("youtube.com/shorts", "YouTube Shorts (Preserves Study Videos)", isShortsSpecial = true, isEnabled = true),
                WebBlockItem("instagram.com", "Instagram", isEnabled = true),
                WebBlockItem("tiktok.com", "TikTok", isEnabled = true),
                WebBlockItem("reddit.com", "Reddit", isEnabled = true),
                WebBlockItem("twitter.com", "Twitter / X", isEnabled = true),
                WebBlockItem("netflix.com", "Netflix", isEnabled = true),
                WebBlockItem("twitch.tv", "Twitch", isEnabled = true),
                WebBlockItem("discord.com", "Discord Web", isEnabled = true),
                WebBlockItem("facebook.com", "Facebook", isEnabled = true)
            )
        )
    }

    var newDomainInput by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf(0) } // 0: Blocker Scripts, 1: Blocked Sites, 2: Chrome Extension

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }

    fun shareText(title: String, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Laptop Blocker"))
    }

    // Generate Windows Batch Script
    val enabledDomains = websiteList.filter { it.isEnabled }
    val windowsScript = remember(enabledDomains) {
        val sb = StringBuilder()
        sb.append("@echo off\n")
        sb.append(":: StudyGuard Laptop Blocker for Windows\n")
        sb.append(":: Run this as Administrator to block distraction websites\n\n")
        sb.append("set HOSTS_FILE=%WINDIR%\\System32\\drivers\\etc\\hosts\n")
        sb.append("echo. >> \"%HOSTS_FILE%\"\n")
        sb.append("echo # StudyGuard Active Focus Blocklist >> \"%HOSTS_FILE%\"\n")
        enabledDomains.forEach { item ->
            if (!item.isShortsSpecial) {
                val cleanDomain = item.domain.replace("https://", "").replace("http://", "").trimEnd('/')
                sb.append("echo 127.0.0.1 $cleanDomain www.$cleanDomain >> \"%HOSTS_FILE%\"\n")
            }
        }
        sb.append("ipconfig /flushdns\n")
        sb.append("echo [SUCCESS] Distraction websites are now blocked on your laptop!\n")
        sb.append("pause\n")
        sb.toString()
    }

    // Generate Mac / Linux Command
    val macLinuxScript = remember(enabledDomains) {
        val domainsFormatted = enabledDomains
            .filter { !it.isShortsSpecial }
            .joinToString(" ") { item ->
                val clean = item.domain.replace("https://", "").replace("http://", "").trimEnd('/')
                "127.0.0.1 $clean www.$clean"
            }
        "sudo sh -c 'printf \"\\n# StudyGuard Focus Blocklist\\n$domainsFormatted\\n\" >> /etc/hosts && dscacheutil -flushcache && killall -HUP mDNSResponder' && echo '✅ Laptop distraction websites blocked!'"
    }

    // Hosts file entries
    val hostsEntries = remember(enabledDomains) {
        enabledDomains
            .filter { !it.isShortsSpecial }
            .joinToString("\n") { item ->
                val clean = item.domain.replace("https://", "").replace("http://", "").trimEnd('/')
                "127.0.0.1  $clean\n127.0.0.1  www.$clean"
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryIndigo
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Laptop,
                                contentDescription = "Laptop",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "Laptop & Browser Shield",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Block websites on Chrome, Edge, Safari & Brave",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "Take StudyGuard's distraction protection to your Windows, Mac, or Chromebook laptop during study sessions!",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section Selector Tabs
        item {
            TabRow(
                selectedTabIndex = selectedSection,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("1-Click Scripts", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("Websites (${enabledDomains.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = { Text("Chrome Extension", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Section 0: 1-Click Scripts for Windows & Mac
        if (selectedSection == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Terminal,
                                contentDescription = null,
                                tint = PrimaryIndigo
                            )
                            Text(
                                text = "Windows 1-Click Blocker Script",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Save as 'block.bat' and run as Administrator on Windows. It redirects all distraction websites across Chrome, Edge, and Firefox instantly!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = windowsScript.lines().take(6).joinToString("\n") + "\n...",
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { copyToClipboard("Windows Script", windowsScript) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_windows_script_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Script", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { shareText("StudyGuard Windows Script", windowsScript) },
                                modifier = Modifier.testTag("share_windows_script_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = SecondaryTeal
                            )
                            Text(
                                text = "Mac & Linux Terminal Command",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Paste this single command into Terminal on your Mac or Linux laptop to lock distraction sites in /etc/hosts.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = macLinuxScript,
                                color = Color(0xFF34D399),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { copyToClipboard("Mac Terminal Command", macLinuxScript) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_mac_command_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Command", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { shareText("StudyGuard Mac Command", macLinuxScript) },
                                modifier = Modifier.testTag("share_mac_command_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Raw Hosts File Rules (Universal)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Can be pasted into any /etc/hosts file or router DNS filter to block across all devices on your Wi-Fi.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { copyToClipboard("Hosts Entries", hostsEntries) },
                            modifier = Modifier.fillMaxWidth().testTag("copy_raw_hosts_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Raw Hosts Rules")
                        }
                    }
                }
            }
        }

        // Section 1: Blocked Websites Manager
        if (selectedSection == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newDomainInput,
                        onValueChange = { newDomainInput = it },
                        placeholder = { Text("e.g. roblox.com, 9gag.com") },
                        label = { Text("Add Custom Website to Block") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_domain_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val domainClean = newDomainInput.trim().lowercase()
                                .removePrefix("https://")
                                .removePrefix("http://")
                                .removePrefix("www.")
                                .trimEnd('/')
                            if (domainClean.isNotEmpty()) {
                                websiteList = websiteList + WebBlockItem(domain = domainClean, name = domainClean, isEnabled = true)
                                newDomainInput = ""
                                Toast.makeText(context, "Added $domainClean to blocklist", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("add_custom_domain_button"),
                        enabled = newDomainInput.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            }

            items(websiteList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isEnabled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                        if (item.isShortsSpecial) Color(0xFFFF0000).copy(alpha = 0.15f)
                                        else if (item.isEnabled) PrimaryIndigo.copy(alpha = 0.15f)
                                        else Color.Gray.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isShortsSpecial) Icons.Filled.PlayArrow else Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = if (item.isShortsSpecial) Color(0xFFFF0000)
                                    else if (item.isEnabled) PrimaryIndigo
                                    else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = item.domain,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = item.isEnabled,
                            onCheckedChange = { checked ->
                                websiteList = websiteList.map {
                                    if (it.domain == item.domain) it.copy(isEnabled = checked) else it
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section 2: Chrome Extension Guide
        if (selectedSection == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Extension,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4)
                                )
                            }
                            Column {
                                Text(
                                    text = "StudyGuard Chrome Extension",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Ready to load in Chrome, Edge & Brave",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "We built a complete Manifest V3 Chrome Extension in `laptop-extension/` featuring your StudyGuard avatar, an aggressive YouTube distraction shield (blocking the Subscriptions tab & recommendations so only Home/Search works), and a 45m Study / 15m Break Marathon mode for lectures!",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        // 3 Key Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🛡️ Avatar PFP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🚫 Subscriptions Blocked", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🎓 45/15 Marathon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "3-Step Setup on Your Laptop:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StepItem(
                                stepNumber = "1",
                                title = "Open Extensions Page",
                                description = "In your laptop browser, visit: chrome://extensions (or edge://extensions)"
                            )
                            StepItem(
                                stepNumber = "2",
                                title = "Enable Developer Mode",
                                description = "Toggle the 'Developer mode' switch in the top-right corner of the page."
                            )
                            StepItem(
                                stepNumber = "3",
                                title = "Load Unpacked",
                                description = "Click 'Load unpacked' and select the 'laptop-extension' folder from your downloaded project!"
                            )
                        }

                        Button(
                            onClick = {
                                copyToClipboard("Chrome Extension URL", "chrome://extensions")
                            },
                            modifier = Modifier.fillMaxWidth().testTag("copy_extension_link_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy 'chrome://extensions' URL")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                        Column {
                            Text(
                                text = "Chromebooks & Windows (WSA)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "If running StudyGuard directly on a Chromebook or Windows 11 Subsystem for Android, this Android app monitors and blocks distractions automatically!",
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PrimaryIndigo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
