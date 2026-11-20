package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyHistoryPoint
import com.example.data.model.WordEntry
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.SkyAccent

/**
 * Shared ocean-blue curved top header matching the Qamusi reference screens.
 * Uses pure Canvas subtle decorative arcs for zero bitmap overhead on Android 9.
 */
@Composable
fun QamusiCurvedBlueHeader(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowBack,
    leadingContentDescription: String = "رجوع",
    onLeadingClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String = "",
    trailingTint: Color = Color.White,
    onTrailingClick: (() -> Unit)? = null,
    secondaryTrailingIcon: ImageVector? = null,
    secondaryTrailingDescription: String = "",
    onSecondaryTrailingClick: (() -> Unit)? = null,
    bottomCornerRadius: Dp = 28.dp,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = bottomCornerRadius, bottomEnd = bottomCornerRadius),
        shadowElevation = 6.dp,
        color = CobaltPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(CobaltDark, CobaltPrimary, Color(0xFF1D74F5))
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.width * 0.36f,
                        center = Offset(size.width * 0.9f, size.height * 0.22f)
                    )
                    drawCircle(
                        color = SkyAccent.copy(alpha = 0.16f),
                        radius = size.width * 0.28f,
                        center = Offset(size.width * 0.12f, size.height * 0.85f)
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (title != null || onLeadingClick != null || onTrailingClick != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leadingIcon != null && onLeadingClick != null) {
                            IconButton(
                                onClick = onLeadingClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("header_leading_btn")
                            ) {
                                Icon(
                                    imageVector = leadingIcon,
                                    contentDescription = leadingContentDescription,
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!title.isNullOrBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (secondaryTrailingIcon != null && onSecondaryTrailingClick != null) {
                                IconButton(
                                    onClick = onSecondaryTrailingClick,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = secondaryTrailingIcon,
                                        contentDescription = secondaryTrailingDescription,
                                        tint = Color.White
                                    )
                                }
                            }
                            if (trailingIcon != null && onTrailingClick != null) {
                                IconButton(
                                    onClick = onTrailingClick,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("header_trailing_btn")
                                ) {
                                    Icon(
                                        imageVector = trailingIcon,
                                        contentDescription = trailingContentDescription,
                                        tint = trailingTint
                                    )
                                }
                            } else if (secondaryTrailingIcon == null) {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                content?.invoke(this)
            }
        }
    }
}

@Composable
fun GoldTrophyArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE68A).copy(alpha = 0.7f), Color.Transparent)
                ),
                radius = this.size.minDimension * 0.5f
            )
            // Golden laurel base arcs
            drawArc(
                color = Color(0xFFD97706),
                startAngle = 140f,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = 6f, cap = StrokeCap.Round),
                topLeft = Offset(this.size.width * 0.1f, this.size.height * 0.18f),
                size = Size(this.size.width * 0.8f, this.size.height * 0.68f)
            )
            drawArc(
                color = Color(0xFFD97706),
                startAngle = 330f,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = 6f, cap = StrokeCap.Round),
                topLeft = Offset(this.size.width * 0.1f, this.size.height * 0.18f),
                size = Size(this.size.width * 0.8f, this.size.height * 0.68f)
            )
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFFFEF3C7),
            border = BorderStroke(2.dp, Color(0xFFF59E0B)),
            shadowElevation = 3.dp,
            modifier = Modifier.size(size * 0.68f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(size * 0.42f)
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
        )
    }
}

@Composable
fun QuizClipboardArtwork(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(width = 128.dp, height = 104.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Soft sky backdrop blob
            drawCircle(
                color = Color(0xFFDBEAFE),
                radius = size.minDimension * 0.44f,
                center = Offset(size.width * 0.48f, size.height * 0.52f)
            )
            // Rounded clipboard card
            val boardLeft = size.width * 0.22f
            val boardTop = size.height * 0.12f
            val boardW = size.width * 0.52f
            val boardH = size.height * 0.78f
            drawRoundRect(
                color = Color(0xFF60A5FA),
                topLeft = Offset(boardLeft, boardTop),
                size = Size(boardW, boardH),
                cornerRadius = CornerRadius(18f, 18f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(boardLeft + 8f, boardTop + 16f),
                size = Size(boardW - 16f, boardH - 24f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Checklist lines
            val x1 = boardLeft + 22f
            val x2 = boardLeft + boardW - 18f
            listOf(0.36f, 0.54f, 0.72f).forEach { yRatio ->
                val y = size.height * yRatio
                drawCircle(color = Color(0xFF155EEF), radius = 5f, center = Offset(x1, y))
                drawLine(
                    color = Color(0xFF93C5FD),
                    start = Offset(x1 + 14f, y),
                    end = Offset(x2, y),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }
        // Floating stopwatch badge on side
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, Color(0xFF155EEF)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 6.dp)
                .size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = Color(0xFF155EEF),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun DonutMasteryRing(
    percent: Int,
    label: String = "معدل الإتقان",
    modifier: Modifier = Modifier,
    ringSize: Dp = 128.dp
) {
    val safePct = percent.coerceIn(0, 100)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = Color(0xFF16A34A)

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 14.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (safePct > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(accentColor, Color(0xFF0EA5E9), primaryColor, accentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = (safePct / 100f) * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$safePct%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyMasteryLineChart(
    points: List<DailyHistoryPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotFill = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (points.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Percentages row above dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = if (pt.masteryPercent > 0 || pt.reviewsCount > 0) "${pt.masteryPercent}%" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pt.masteryPercent > 0) lineColor else labelColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // 3 subtle horizontal guide lines
            listOf(0.15f, 0.5f, 0.88f).forEach { yRatio ->
                val y = size.height * yRatio
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }

            val count = points.size
            val stepX = if (count > 1) size.width / (count - 1) else 0f
            val coords = points.mapIndexed { idx, pt ->
                val x = idx * stepX
                val ratio = (pt.masteryPercent.coerceIn(0, 100)) / 100f
                val y = size.height - (ratio * (size.height - 16f)) - 8f
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val path = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = dotFill, radius = 9f, center = pt)
                drawCircle(color = lineColor, radius = 9f, center = pt, style = Stroke(width = 4f))
            }
        }

        // Day names row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.dayLabelAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactReferenceWordRow(
    entry: WordEntry,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSpeakWord: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    badgeNote: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("word_card_${entry.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "محدد" else "غير محدد",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("word_fav_${entry.id}")
                ) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = if (entry.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (entry.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entry.word.isNotBlank()) {
                IconButton(
                    onClick = { onSpeakWord(entry.word) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("word_speak_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "نطق ${entry.word}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.displayTitle(),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = when {
                    entry.arabicMeaning.isNotBlank() && entry.word.isNotBlank() -> entry.arabicMeaning
                    entry.definition.isNotBlank() -> entry.definition
                    else -> "بدون معنى مسجل بعد"
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!badgeNote.isNullOrBlank()) {
                    Text(
                        text = badgeNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (entry.difficulty >= 3 || entry.lastWrongAt > 0L) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = "صعبة",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (entry.locations.firstOrNull()?.unit?.isNotBlank() == true) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "و${entry.locations.first().unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReferenceWhiteCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
