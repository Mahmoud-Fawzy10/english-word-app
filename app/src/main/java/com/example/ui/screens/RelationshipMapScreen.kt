package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntry
import com.example.data.repository.WordRepository
import com.example.domain.LearningAnalyticsEngine
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class RelationNodeKind(
    val badgeAr: String,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color
) {
    SYNONYM("مرادف", Color(0xFFDCFCE7), Color(0xFF14532D), Color(0xFF22C55E)),
    ANTONYM("مضاد", Color(0xFFFEE2E2), Color(0xFF991B1B), Color(0xFFEF4444)),
    DERIVATIVE("مشتق", Color(0xFFD1FAE5), Color(0xFF065F46), Color(0xFF10B981)),
    EXPRESSION("عبارة / حرف جر", Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFF59E0B)),
    RELATED("كلمة مرتبطة", Color(0xFFDBEAFE), Color(0xFF1E3A8A), Color(0xFF3B82F6))
}

private data class GraphSatelliteNode(
    val title: String,
    val kind: RelationNodeKind,
    val linkedWordId: Long? = null
)

@Composable
fun RelationshipMapScreen(
    allWords: List<WordEntry>,
    initialWordId: Long,
    onBack: () -> Unit,
    onOpenWordDetail: (Long) -> Unit,
    onAddMissingWord: (String) -> Unit,
    onSpeakWord: (String) -> Unit
) {
    BackHandler { onBack() }

    var centerWordId by remember(initialWordId, allWords) {
        mutableStateOf(
            allWords.firstOrNull { it.id == initialWordId }?.id
                ?: allWords.firstOrNull()?.id
                ?: 0L
        )
    }
    var compareMode by remember { mutableStateOf(false) }
    var compareSecondId by remember { mutableStateOf(0L) }
    var showCenterDropdown by remember { mutableStateOf(false) }
    var showCompareDropdown by remember { mutableStateOf(false) }

    val centerWord = remember(centerWordId, allWords) {
        allWords.firstOrNull { it.id == centerWordId }
    }
    val relatedInDict = remember(centerWord, allWords) {
        if (centerWord == null) emptyList()
        else {
            val cKey = WordRepository.normalizeKey(centerWord.word)
            val synKeys = centerWord.synonyms.map { WordRepository.normalizeKey(it) }.toSet()
            val antKeys = centerWord.antonyms.map { WordRepository.normalizeKey(it) }.toSet()
            val unitKeys = centerWord.locations.map { it.unit.trim() }.filter { it.isNotBlank() }.toSet()
            allWords.filter { other ->
                other.id != centerWord.id && (
                    synKeys.contains(WordRepository.normalizeKey(other.word)) ||
                        antKeys.contains(WordRepository.normalizeKey(other.word)) ||
                        other.synonyms.any { WordRepository.normalizeKey(it) == cKey && cKey.isNotEmpty() } ||
                        other.antonyms.any { WordRepository.normalizeKey(it) == cKey && cKey.isNotEmpty() } ||
                        (unitKeys.isNotEmpty() && other.locations.any { unitKeys.contains(it.unit.trim()) })
                    )
            }
        }
    }

    val satellites = remember(centerWord, relatedInDict, allWords) {
        if (centerWord == null) emptyList()
        else buildList {
            centerWord.synonyms.take(2).forEach { syn ->
                val hit = allWords.firstOrNull { WordRepository.normalizeKey(it.word) == WordRepository.normalizeKey(syn) }
                add(GraphSatelliteNode(syn, RelationNodeKind.SYNONYM, hit?.id))
            }
            centerWord.antonyms.take(2).forEach { ant ->
                val hit = allWords.firstOrNull { WordRepository.normalizeKey(it.word) == WordRepository.normalizeKey(ant) }
                add(GraphSatelliteNode(ant, RelationNodeKind.ANTONYM, hit?.id))
            }
            if (centerWord.derivatives.isNotBlank()) {
                val firstDeriv = centerWord.derivatives.split(',', '،', ';', '\n').firstOrNull()?.trim().orEmpty()
                if (firstDeriv.isNotBlank()) {
                    add(GraphSatelliteNode(firstDeriv, RelationNodeKind.DERIVATIVE))
                }
            }
            val expr = centerWord.collocations.firstOrNull()
                ?: centerWord.prepositions.firstOrNull()
                ?: centerWord.expressions.lineSequence().firstOrNull()?.trim().orEmpty()
            if (expr.isNotBlank()) {
                add(GraphSatelliteNode(expr, RelationNodeKind.EXPRESSION))
            }
            if (size < 6) {
                relatedInDict.take(6 - size).forEach { rel ->
                    if (none { it.linkedWordId == rel.id }) {
                        add(GraphSatelliteNode(rel.displayTitle(), RelationNodeKind.RELATED, rel.id))
                    }
                }
            }
        }.take(6)
    }

    val secondWord = remember(compareSecondId, allWords, relatedInDict, centerWord) {
        allWords.firstOrNull { it.id == compareSecondId }
            ?: relatedInDict.firstOrNull()
            ?: allWords.firstOrNull { it.id != centerWord?.id }
    }

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
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QamusiCurvedBlueHeader(
                        title = "خريطة العلاقات بين الكلمات",
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onLeadingClick = onBack,
                        trailingIcon = Icons.Rounded.CompareArrows,
                        trailingContentDescription = "مقارنة الكلمات المتشابهة",
                        onTrailingClick = { compareMode = !compareMode },
                        bottomCornerRadius = 28.dp
                    )
                }

                if (centerWord == null) {
                    item {
                        ReferenceWhiteCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "أضف كلمات إلى قاموسك لعرض شبكة العلاقات بين المرادفات والمضادات والاشتقاقات.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Focus Word Selector Dropdown
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { showCenterDropdown = true },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "الكلمة المحورية: ${centerWord.displayTitle()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            DropdownMenu(
                                expanded = showCenterDropdown,
                                onDismissRequest = { showCenterDropdown = false }
                            ) {
                                allWords.take(40).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("${w.displayTitle()} — ${w.arabicMeaning}") },
                                        onClick = {
                                            centerWordId = w.id
                                            showCenterDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Radial Graph Canvas matching Reference Screen 8
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(330.dp)
                                    .padding(12.dp)
                            ) {
                                val widthPx = constraints.maxWidth.toFloat()
                                val heightPx = constraints.maxHeight.toFloat()
                                val cx = widthPx / 2f
                                val cy = heightPx / 2f
                                val rx = widthPx * 0.34f
                                val ry = heightPx * 0.34f

                                val angleOffsets = listOf(-90.0, -25.0, 28.0, 90.0, 152.0, 205.0)
                                val positions = satellites.mapIndexed { idx, _ ->
                                    val rad = Math.toRadians(angleOffsets.getOrElse(idx) { idx * 60.0 })
                                    Offset(
                                        x = (cx + rx * cos(rad)).toFloat(),
                                        y = (cy + ry * sin(rad)).toFloat()
                                    )
                                }

                                val lineColor = MaterialTheme.colorScheme.outline
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    positions.forEachIndexed { idx, pos ->
                                        val kindColor = satellites[idx].kind.borderColor.copy(alpha = 0.55f)
                                        drawLine(
                                            color = if (kindColor.alpha > 0f) kindColor else lineColor,
                                            start = Offset(cx, cy),
                                            end = pos,
                                            strokeWidth = 5f
                                        )
                                    }
                                }

                                // Outer Satellite Bubbles
                                satellites.forEachIndexed { idx, node ->
                                    val pt = positions[idx]
                                    val bubbleW = 116.dp
                                    val bubbleH = 54.dp
                                    Surface(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    x = (pt.x - (bubbleW.toPx() / 2f)).roundToInt(),
                                                    y = (pt.y - (bubbleH.toPx() / 2f)).roundToInt()
                                                )
                                            }
                                            .size(width = bubbleW, height = bubbleH)
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                if (node.linkedWordId != null) {
                                                    centerWordId = node.linkedWordId
                                                } else {
                                                    onAddMissingWord(node.title)
                                                }
                                            }
                                            .testTag("graph_node_$idx"),
                                        shape = RoundedCornerShape(50),
                                        color = node.kind.bgColor,
                                        border = BorderStroke(1.5.dp, node.kind.borderColor),
                                        shadowElevation = 2.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = node.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = node.kind.textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "(${node.kind.badgeAr})",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = node.kind.textColor.copy(alpha = 0.85f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // Center Cobalt Hub Node
                                val hubSize = 114.dp
                                Surface(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = (cx - (hubSize.toPx() / 2f)).roundToInt(),
                                                y = (cy - (hubSize.toPx() / 2f)).roundToInt()
                                            )
                                        }
                                        .size(hubSize)
                                        .clip(CircleShape)
                                        .clickable { onOpenWordDetail(centerWord.id) }
                                        .testTag("graph_center_hub"),
                                    shape = CircleShape,
                                    shadowElevation = 6.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(CobaltDark, CobaltPrimary))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Text(
                                                text = centerWord.displayTitle(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (centerWord.arabicMeaning.isNotBlank() && centerWord.word.isNotBlank()) {
                                                Text(
                                                    text = centerWord.arabicMeaning,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Toggle Pill Matching Reference Screen 8 («شبكة العلاقات بين الكلمات» / «مقارنة كلمتين»)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { compareMode = !compareMode }
                                .testTag("toggle_compare_words_btn"),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (compareMode) "إخفاء مقارنة الكلمات المتشابهة" else "شبكة العلاقات ومقارنة الكلمات المتشابهة",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Quick Action Row for Center Word
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (centerWord.word.isNotBlank()) onSpeakWord(centerWord.word)
                                },
                                enabled = centerWord.word.isNotBlank(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نطق الكلمة")
                            }
                            Button(
                                onClick = { onOpenWordDetail(centerWord.id) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("فتح التفاصيل")
                            }
                        }
                    }

                    // Smart Side-by-Side Comparison Card
                    item {
                        AnimatedVisibility(visible = compareMode && secondWord != null) {
                            if (secondWord != null) {
                                ReferenceWhiteCard(
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "مقارنة الكلمات المتشابهة",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box {
                                            OutlinedButton(
                                                onClick = { showCompareDropdown = true },
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("تغيير الكلمة الثانية")
                                            }
                                            DropdownMenu(
                                                expanded = showCompareDropdown,
                                                onDismissRequest = { showCompareDropdown = false }
                                            ) {
                                                allWords.filter { it.id != centerWord.id }.take(35).forEach { w ->
                                                    DropdownMenuItem(
                                                        text = { Text("${w.displayTitle()} — ${w.arabicMeaning}") },
                                                        onClick = {
                                                            compareSecondId = w.id
                                                            showCompareDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    CompareMetricRow(
                                        label = "الكلمة",
                                        leftVal = centerWord.displayTitle(),
                                        rightVal = secondWord.displayTitle(),
                                        bold = true
                                    )
                                    CompareMetricRow(
                                        label = "المعنى",
                                        leftVal = centerWord.arabicMeaning.ifBlank { "—" },
                                        rightVal = secondWord.arabicMeaning.ifBlank { "—" }
                                    )
                                    CompareMetricRow(
                                        label = "المرادفات",
                                        leftVal = centerWord.synonyms.joinToString("، ").ifBlank { "—" },
                                        rightVal = secondWord.synonyms.joinToString("، ").ifBlank { "—" }
                                    )
                                    CompareMetricRow(
                                        label = "حروف الجر",
                                        leftVal = centerWord.prepositions.joinToString("، ").ifBlank { "—" },
                                        rightVal = secondWord.prepositions.joinToString("، ").ifBlank { "—" }
                                    )
                                    CompareMetricRow(
                                        label = "المتلازمات",
                                        leftVal = centerWord.collocations.joinToString("، ").ifBlank { "—" },
                                        rightVal = secondWord.collocations.joinToString("، ").ifBlank { "—" }
                                    )
                                    val intelA = LearningAnalyticsEngine.computeWordIntelligence(centerWord)
                                    val intelB = LearningAnalyticsEngine.computeWordIntelligence(secondWord)
                                    CompareMetricRow(
                                        label = "مؤشر الإتقان",
                                        leftVal = "${intelA.masteryScorePercent}%",
                                        rightVal = "${intelB.masteryScorePercent}%"
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

@Composable
private fun CompareMetricRow(
    label: String,
    leftVal: String,
    rightVal: String,
    bold: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = leftVal,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text(
                    text = rightVal,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
