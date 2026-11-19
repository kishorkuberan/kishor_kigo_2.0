package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserSettings
import com.example.ui.components.KigoAvatar
import com.example.ui.components.KigoIdChip
import com.example.ui.components.KigoSubScreenTopBar
import com.example.ui.theme.KigoAmber
import com.example.ui.theme.KigoChatTheme
import com.example.ui.theme.KigoCyan
import com.example.ui.theme.KigoEmerald
import com.example.ui.theme.KigoRose
import com.example.ui.theme.KigoViolet
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.parseBubbleColor

@Composable
fun CustomisationStudioScreen(
    currentSettings: UserSettings,
    displayName: String,
    kigoId: String,
    onSaveSettings: (UserSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var draft by remember(currentSettings) { mutableStateOf(currentSettings) }

    fun applyDraft(newSettings: UserSettings) {
        draft = newSettings
        onSaveSettings(newSettings)
    }

    Scaffold(
        topBar = {
            KigoSubScreenTopBar(
                title = "Customisation Studio",
                subtitle = "Live preview · Synced across devices",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Text(
                    text = "LIVE THEME PREVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                KigoChatTheme(settings = draft) {
                    val bubbleColor = parseBubbleColor(draft.bubbleColorHex)
                    val bubbleRadius = when (draft.messageLayout) {
                        "compact" -> 10.dp
                        "classic" -> 14.dp
                        else -> 20.dp
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when (draft.chatWallpaper) {
                                        "aurora" -> Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                            )
                                        )
                                        "grid" -> Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                        else -> Brush.verticalGradient(
                                            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
                                        )
                                    }
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    KigoAvatar(displayName = displayName, photoUrl = "", size = 40.dp, isOnline = true, showOnlineIndicator = true)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName.ifBlank { "KIGO User" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        KigoIdChip(kigoId = kigoId)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = draft.profileTheme.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(bubbleRadius),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text(
                                        text = "Hey! Loving this ${draft.themeMode} theme & ${draft.fontStyle} font.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(bubbleRadius),
                                    color = bubbleColor,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Synced in real time to Firestore!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                StudioSectionCard(title = "Theme Mode") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("light" to "Light", "dark" to "Dark", "amoled" to "AMOLED").forEach { (key, label) ->
                            FilterChip(
                                selected = draft.themeMode == key,
                                onClick = { applyDraft(draft.copy(themeMode = key)) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_mode_$key")
                            )
                        }
                    }
                }
            }

            item {
                StudioSectionCard(title = "Accent Colour") {
                    val accents = listOf(
                        "cyan" to KigoCyan,
                        "violet" to KigoViolet,
                        "emerald" to KigoEmerald,
                        "amber" to KigoAmber,
                        "rose" to KigoRose
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accents) { (key, color) ->
                            val selected = draft.accentPreset == key
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { applyDraft(draft.copy(accentPreset = key)) }
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = key, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item {
                StudioSectionCard(title = "Chat Bubble Colour") {
                    val bubbles = listOf("#00B8D4", "#7C4DFF", "#00A152", "#FF4081", "#FFB300", "#2563EB")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(bubbles) { hex ->
                            val selected = draft.bubbleColorHex.equals(hex, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(parseBubbleColor(hex))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { applyDraft(draft.copy(bubbleColorHex = hex)) }
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = hex, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item {
                StudioSectionCard(title = "Typography & Font Size") {
                    Text("Font Family", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("jakarta" to "Jakarta Sans", "grotesk" to "Space Grotesk", "mono" to "JetBrains Mono", "serif" to "Editorial Serif").forEach { (key, label) ->
                            item {
                                FilterChip(
                                    selected = draft.fontStyle == key,
                                    onClick = { applyDraft(draft.copy(fontStyle = key)) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Text Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("compact" to "Compact", "standard" to "Standard", "large" to "Large", "xl" to "Extra Large").forEach { (key, label) ->
                            item {
                                FilterChip(
                                    selected = draft.fontScale == key,
                                    onClick = { applyDraft(draft.copy(fontScale = key)) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                StudioSectionCard(title = "Layouts, Navigation & Profile Theme") {
                    Text("Message Bubble Layout", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("modern" to "Modern", "classic" to "Classic", "compact" to "Compact").forEach { (key, label) ->
                            FilterChip(
                                selected = draft.messageLayout == key,
                                onClick = { applyDraft(draft.copy(messageLayout = key)) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Navigation Appearance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("docked" to "Docked Bar", "floating" to "Floating Pill").forEach { (key, label) ->
                            FilterChip(
                                selected = draft.navigationStyle == key,
                                onClick = { applyDraft(draft.copy(navigationStyle = key)) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Chat Wallpaper", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("aurora" to "Aurora", "solid" to "Clean Solid", "grid" to "Slate Mesh").forEach { (key, label) ->
                            FilterChip(
                                selected = draft.chatWallpaper == key,
                                onClick = { applyDraft(draft.copy(chatWallpaper = key)) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Profile Card Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("aurora" to "Aurora", "obsidian" to "Obsidian", "neon" to "Neon", "minimal" to "Minimal").forEach { (key, label) ->
                            item {
                                FilterChip(
                                    selected = draft.profileTheme == key,
                                    onClick = { applyDraft(draft.copy(profileTheme = key)) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                StudioSectionCard(title = "Notification Sound") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("pulse" to "KIGO Pulse", "chime" to "Crystal Chime", "soft" to "Soft Wave", "silent" to "Silent").forEach { (key, label) ->
                            item {
                                FilterChip(
                                    selected = draft.notificationSound == key,
                                    onClick = { applyDraft(draft.copy(notificationSound = key)) },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onSaveSettings(draft)
                        Toast.makeText(context, "Theme preferences saved to Firestore!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("studio_save_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Customisation Profile")
                }
            }
        }
    }
}

@Composable
private fun StudioSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}
