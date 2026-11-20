package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AchievementBadge
import com.example.domain.UserRankInfo
import com.example.ui.components.GoldTrophyArtwork
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.viewmodel.DictionaryUiState

@Composable
fun AchievementsScreen(
    uiState: DictionaryUiState,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAllAchievements by remember { mutableStateOf(false) }
    val featured = uiState.achievements.take(3)
    val rank: UserRankInfo = uiState.userRank
    val unlockedCount = uiState.achievements.count { it.isUnlocked }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    QamusiCurvedBlueHeader(
                        title = "الإنجازات",
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onLeadingClick = onBack,
                        trailingIcon = Icons.Rounded.EmojiEvents,
                        trailingTint = Color(0xFFFDE047),
                        onTrailingClick = { showAllAchievements = !showAllAchievements },
                        bottomCornerRadius = 28.dp
                    )
                }

                // Gold Trophy & Rank Card matching Reference Screen 9
                item {
                    ReferenceWhiteCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoldTrophyArtwork(size = 104.dp)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = rank.rankTitleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp)
                                )
                            }

                            Text(
                                text = "لقد أضفت ${uiState.allWordsCount} كلمة في قاموسي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            val rankRatio = (uiState.allWordsCount.toFloat() / rank.nextRankThreshold.coerceAtLeast(1))
                                .coerceIn(0f, 1f)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "المستوى التالي: ${rank.nextRankThreshold} كلمة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "مفتوح: $unlockedCount / ${uiState.achievements.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { rankRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }

                // 3 Featured Milestone Badges Row matching Reference Screen 9
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val medalStyles = listOf(
                            Triple(Icons.Rounded.WorkspacePremium, Color(0xFF7C3AED), Color(0xFFEDE9FE)),
                            Triple(Icons.Rounded.EmojiEvents, Color(0xFFD97706), Color(0xFFFEF3C7)),
                            Triple(Icons.Rounded.MilitaryTech, Color(0xFF0284C7), Color(0xFFE0F2FE))
                        )
                        featured.forEachIndexed { idx, badge ->
                            val style = medalStyles.getOrElse(idx) { medalStyles.first() }
                            FeaturedBadgeTile(
                                badge = badge,
                                icon = style.first,
                                accentColor = style.second,
                                bgColor = style.third,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Full-Width Cobalt CTA «عرض جميع الإنجازات»
                item {
                    Button(
                        onClick = { showAllAchievements = !showAllAchievements },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp)
                            .testTag("toggle_all_achievements_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (showAllAchievements) "إخفاء قائمة الإنجازات الكاملة" else "عرض جميع الإنجازات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showAllAchievements) {
                    items(uiState.achievements, key = { it.id }) { badge ->
                        AchievementDetailRow(
                            badge = badge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedBadgeTile(
    badge: AchievementBadge,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (badge.isUnlocked) 1.5.dp else 1.dp,
            color = if (badge.isUnlocked) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (badge.isUnlocked) bgColor else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (badge.isUnlocked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Text(
                text = badge.titleAr,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (badge.isUnlocked) "مكتمل" else "${badge.current}/${badge.target}",
                style = MaterialTheme.typography.labelSmall,
                color = if (badge.isUnlocked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AchievementDetailRow(
    badge: AchievementBadge,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (badge.isUnlocked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (badge.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (badge.isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (badge.isUnlocked) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badge.titleAr,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = badge.descriptionAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${badge.current}/${badge.target}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { badge.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}
