package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.engine.GameRule
import com.example.engine.GomokuMove
import com.example.ui.GomokuUiState
import com.example.ui.GomokuViewModel
import com.example.ui.board.GomokuBoard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GomokuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) { // Clean Minimalism light theme
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFFDF8FF) // Elegant clean background
                ) { innerPadding ->
                    GomokuMainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GomokuMainScreen(
    viewModel: GomokuViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Determine responsive sizing based on screen bounds (Canonical layouts support)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape Split Layout (Board on left side, logs and settings side-by-side on right)
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GomokuLogoHeader()
                    Spacer(modifier = Modifier.height(6.dp))
                    WinProbabilityGauge(state.winProbability)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    GomokuBoard(
                        board = state.board,
                        currentTurn = state.currentTurn,
                        movesHistory = state.movesHistory,
                        cellScores = state.cellScores,
                        bestAiMove = state.bestAiMove,
                        showEvaluations = state.showEvaluations,
                        showCoordinates = state.showCoordinates,
                        onCellTapped = { r, c -> viewModel.handleCellTap(r, c) },
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 480.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrentPlayerRibbon(state.currentTurn, state.winner)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Scrollable Settings and Controllers Container (Right Pane)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EngineCalculationBanner(state)
                Spacer(modifier = Modifier.height(8.dp))
                BoardActionButtons(viewModel, state)
                Spacer(modifier = Modifier.height(12.dp))
                RenjuWarningBanner(state.renjuViolationMessage)
                WinnerModal(state.winner) { viewModel.clearBoard() }
                
                GomokuControlDashboard(viewModel, state)
                Spacer(modifier = Modifier.height(8.dp))
                CoordinateHistoryList(state.movesHistory)
            }
        }
    } else {
        // Classic Portrait Stack Layout (Top logo -> Win gauge -> Board -> Controls -> Scroll settings)
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            GomokuLogoHeader()
            Spacer(modifier = Modifier.height(8.dp))
            WinProbabilityGauge(state.winProbability)
            Spacer(modifier = Modifier.height(12.dp))

            // The square board centered nicely
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                contentAlignment = Alignment.Center
            ) {
                GomokuBoard(
                    board = state.board,
                    currentTurn = state.currentTurn,
                    movesHistory = state.movesHistory,
                    cellScores = state.cellScores,
                    bestAiMove = state.bestAiMove,
                    showEvaluations = state.showEvaluations,
                    showCoordinates = state.showCoordinates,
                    onCellTapped = { r, c -> viewModel.handleCellTap(r, c) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            CurrentPlayerRibbon(state.currentTurn, state.winner)
            Spacer(modifier = Modifier.height(8.dp))
            EngineCalculationBanner(state)
            Spacer(modifier = Modifier.height(8.dp))
            BoardActionButtons(viewModel, state)
            Spacer(modifier = Modifier.height(12.dp))

            RenjuWarningBanner(state.renjuViolationMessage)
            WinnerModal(state.winner) { viewModel.clearBoard() }

            GomokuControlDashboard(viewModel, state)
            Spacer(modifier = Modifier.height(12.dp))
            CoordinateHistoryList(state.movesHistory)
            Spacer(modifier = Modifier.height(32.dp)) // Extra pad at bottom to prevent gesture pill overlap
        }
    }
}

@Composable
fun GomokuLogoHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GOMOKU CALCULATOR",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.5.sp,
            color = Color(0xFF1C1B1F), // Elegant deep dark minimalist title
            textAlign = TextAlign.Center
        )
        Text(
            text = "BỘ ĐỊNH GIÁ & TÍNH TOÁN NATIVE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF6750A4), // Modern bold accent subtitle
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * High contrast, stunning visual split bar representing the current balance/winrate of the game
 */
@Composable
fun WinProbabilityGauge(probability: Float) {
    val roundedProb = (probability * 100).roundToInt()
    val blackProb = roundedProb
    val whiteProb = 100 - roundedProb

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Đen (Black): $blackProb%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F) // Deep tone
            )
            Text(
                text = "Trắng (White): $whiteProb%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F) // Cozy charcoal tone
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Double-painted progress bar representing win rate ratio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE8DEF8)) // Soft light background theme indicator
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Black advantage ratio
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(probability.coerceIn(0.01f, 0.99f))
                        .background(Color.Black)
                )
                // White advantage ratio
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight((1f - probability).coerceIn(0.01f, 0.99f))
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Visual banner showing current player sequence or victory alerts
 */
@Composable
fun CurrentPlayerRibbon(currentTurn: Int, winner: Int) {
    if (winner != 0) return // Skip if game concluded

    Card(
        modifier = Modifier
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)) // Beautiful M3 light slate background
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lượt hiện tại: ",
                fontSize = 13.sp,
                color = Color(0xFF49454F),
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (currentTurn == 1) Color.Black else Color.White)
                    .border(1.dp, Color(0xFFCAC4D0), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (currentTurn == 1) "QUÂN ĐEN" else "QUÂN TRẮNG",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentTurn == 1) Color(0xFF1C1B1F) else Color(0xFF6750A4)
            )
        }
    }
}

@Composable
fun EngineCalculationBanner(state: GomokuUiState) {
    AnimatedVisibility(
        visible = state.isCalculatingAi,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val opacity by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "opacity"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEADDFF).copy(alpha = opacity)) // Soft light lavender pulse background
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = Color(0xFF6750A4), // Modern bold purple spinner
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Máy đang phân tích chiều sâu ${state.aiDifficultyDepth}...",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF21005D) // Intense deep purple text
            )
        }
    }
}

@Composable
fun RenjuWarningBanner(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (message != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Cảnh báo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WinnerModal(winner: Int, onRestart: () -> Unit) {
    AnimatedVisibility(
        visible = winner != 0,
        enter = fadeIn()
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Clean minimalist success banner background
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (winner) {
                        1 -> "QUÂN ĐEN CHIẾN THẮNG! 🎉"
                        2 -> "QUÂN TRẮNG CHIẾN THẮNG! 🎉"
                        else -> "HOÀ CỜ! 🤝"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20), // Elegant deep success green
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (winner != 3) "Đã tạo được hàng 5 quân liên tiếp hợp lệ." else "Bàn cờ đã đầy hoàn toàn.",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("PHÂN TÍCH VÁN MỚI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Controller bar containing undo, redo, forced calculations, setups, clear triggers
 */
@Composable
fun BoardActionButtons(
    viewModel: GomokuViewModel,
    state: GomokuUiState
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp)), // Nice clean outline
        color = Color(0xFFF3EDF7) // Pale M3 purple/slate background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button
            IconButton(
                onClick = { viewModel.undo() },
                enabled = state.canUndo,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF6750A4),
                    disabledContentColor = Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Icon(imageVector = Icons.Default.Undo, contentDescription = "Quay lại nước")
            }

            // Redo Button
            IconButton(
                onClick = { viewModel.redo() },
                enabled = state.canRedo,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF6750A4),
                    disabledContentColor = Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Icon(imageVector = Icons.Default.Redo, contentDescription = "Đi tiếp nước")
            }

            // Swipe Turn sequence manually (Useful for study setups)
            IconButton(
                onClick = { viewModel.toggleTurn() },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF1D192B))
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Đổi lượt đi")
            }

            // Custom Setup Position manual Switch
            IconButton(
                onClick = { viewModel.setSetupMode(!state.setupModeActive) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (state.setupModeActive) Color(0xFF2E7D32) else Color(0xFF1D192B)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Chế độ dàn trận",
                    tint = if (state.setupModeActive) Color(0xFF2E7D32) else Color(0xFF1D192B)
                )
            }

            // Force evaluate advice best moves
            IconButton(
                onClick = { viewModel.forceEvaluateBestMove() },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF6750A4))
            ) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = "Gợi ý tối ưu")
            }

            // Clear/Reset Board Trigger
            IconButton(
                onClick = { viewModel.clearBoard() },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFB3261E)) // Material 3 error red
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Bắt đầu lại")
            }
        }
    }
}

/**
 * The core controller options containing difficulty settings, visual toggles, rule selections
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GomokuControlDashboard(
    viewModel: GomokuViewModel,
    state: GomokuUiState
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)), // Light modern M3 lavender surface card
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Section Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = Color(0xFF6750A4), // Modern purple accent icon
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BẢNG ĐIỀU KHIỂN ĐỘNG (DASHBOARD)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4) // Typographic pairing
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFCAC4D0) // Standard clean M3 boundary divider
            )

            // Dynamic Setup Mode details if active
            if (state.setupModeActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9)) // Soft green success card
                        .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🛠️ CHẾ ĐỘ XẾP THẾ CỜ ĐANG HOẠT ĐỘNG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20) // Deep forest green
                    )
                    Text(
                        text = "Nhấp bất kỳ đâu để xếp đá trực tiếp cho phân tích.",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(
                            Triple(1, "Xếp Đen", Color.Black),
                            Triple(2, "Xếp Trắng", Color.White),
                            Triple(0, "Xoá Hạt", Color.Transparent)
                        ).forEach { (colorCode, label, tint) ->
                            val selected = state.setupStoneColor == colorCode
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) Color(0xFFEADDFF) else Color.Transparent) // Highlight in lavender
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) Color(0xFF6750A4) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setSetupStoneColor(colorCode) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(tint)
                                        .border(1.dp, Color(0xFFCAC4D0), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF21005D) else Color(0xFF49454F)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Game play vs AI configuration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "Máy chơi",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Đấu với Máy (Play vs AI)", fontSize = 13.sp, color = Color(0xFF1C1B1F))
                        Text("Cho phép máy tự phản hồi nước đi", fontSize = 11.sp, color = Color(0xFF49454F))
                    }
                }
                Switch(
                    checked = state.playVsAi,
                    onCheckedChange = { viewModel.togglePlayVsAi() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6750A4),
                        checkedTrackColor = Color(0xFFEADDFF),
                        uncheckedThumbColor = Color(0xFF49454F),
                        uncheckedTrackColor = Color(0xFFCAC4D0)
                    )
                )
            }

            // AI Color selection (Visible only if AI is enabled)
            if (state.playVsAi) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chọn bên Máy giữ:",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                    Row(horizontalArrangement = Arrangement.End) {
                        listOf(
                            Pair(1, "Quân Đen (Black)"),
                            Pair(2, "Quân Trắng (White)")
                        ).forEach { (colorCode, label) ->
                            val isSelected = state.aiPlayerColor == colorCode
                            InputChip(
                                selected = isSelected,
                                onClick = { viewModel.setAiPlayerColor(colorCode) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Rules Selection block
            Text(
                text = "Cấu hình thuật toán Luật chơi (Rules):",
                fontSize = 12.sp,
                color = Color(0xFF1C1B1F),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Pair(GameRule.FREESTYLE, "Tự do"),
                    Pair(GameRule.GOMOKU, "Chuẩn Gomoku"),
                    Pair(GameRule.RENJU, "Renju Pro")
                ).forEach { (rule, label) ->
                    val isSelected = state.activeRule == rule
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.setGameRule(rule) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. AI Calculation Depth configurations
            Text(
                text = "Độ sâu phân tích của Máy: Cấp ${state.aiDifficultyDepth}",
                fontSize = 12.sp,
                color = Color(0xFF1C1B1F),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Pair(1, "Dễ (D1)"),
                    Pair(2, "Vừa (D2)"),
                    Pair(3, "Khó (D3)"),
                    Pair(4, "G.M (D4)")
                ).forEach { (d, label) ->
                    val active = state.aiDifficultyDepth == d
                    val btnBg = if (active) Color(0xFF6750A4) else Color(0xFFE8DEF8)
                    val btnFg = if (active) Color.White else Color(0xFF1D192B)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(btnBg)
                            .clickable { viewModel.setAiDifficulty(d) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = btnFg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interface Toggles
            Text(
                text = "Tùy chọn hiển thị màn hình:",
                fontSize = 12.sp,
                color = Color(0xFF1C1B1F),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Toggle scores visibility
            ToggleRow(
                label = "Hiển thị điểm số Calculator overlay",
                checked = state.showEvaluations,
                onCheckedChange = { viewModel.toggleShowEvaluations() }
            )
            // Toggle recommended best target
            ToggleRow(
                label = "Gợi ý vòng sáng nước cờ tối ưu",
                checked = state.highlightBestMove,
                onCheckedChange = { viewModel.toggleHighlightBestMove() }
            )
            // Toggle Coordinate references
            ToggleRow(
                label = "Hiển thị trục toạ độ A-O / 1-15",
                checked = state.showCoordinates,
                onCheckedChange = { viewModel.toggleShowCoordinates() }
            )
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF1C1B1F).copy(alpha = 0.85f) // High contrast clean slate text
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF6750A4),
                checkedTrackColor = Color(0xFFEADDFF),
                uncheckedThumbColor = Color(0xFF49454F),
                uncheckedTrackColor = Color(0xFFCAC4D0)
            )
        )
    }
}

/**
 * Displays a nice, readable scrollable history of coordinates placed so far
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoordinateHistoryList(movesHistory: List<GomokuMove>) {
    if (movesHistory.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)), // Light lavender container surface
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Rule,
                    contentDescription = "Lịch sử",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BIÊN BẢN GHI SỐ NƯỚC ĐI (MOVE GRAPH LOGS - ${movesHistory.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start
            ) {
                movesHistory.forEachIndexed { index, move ->
                    val moveNumStr = if (index % 2 == 0) "${(index / 2) + 1}. " else " "
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (move.player == 1) Color.Black else Color.White)
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$moveNumStr${move.coordinateName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (move.player == 1) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}
