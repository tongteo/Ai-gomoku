package com.example.ui.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.example.engine.GomokuMove
import kotlin.math.roundToInt

@Composable
fun GomokuBoard(
    board: Array<IntArray>,
    currentTurn: Int,
    movesHistory: List<GomokuMove>,
    cellScores: Array<FloatArray>,
    bestAiMove: Pair<Int, Int>?,
    showEvaluations: Boolean,
    showCoordinates: Boolean,
    onCellTapped: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val boardSize = board.size

    // Determine wood color palettes for background - matching Clean Minimalism theme
    val boardWoodColor = Color(0xFFF3E5D8) // Traditional light cream sand Kaya board
    val gridLineColor = Color(0xFF4E342E).copy(alpha = 0.25f)  // Soft warm brown with 25% opacity
    val labelColor = Color(0xFF4E342E).copy(alpha = 0.7f)     // Subtle labels

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .border(6.dp, Color(0xFFD7CCC8), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(boardWoodColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Determine board drawing metrics
        val coordinateMarginPx = if (showCoordinates) with(density) { 20.dp.toPx() } else 0f
        val boardPaddingPx = with(density) { 12.dp.toPx() }
        
        val gridWidth = widthPx - (2 * boardPaddingPx) - (2 * coordinateMarginPx)
        val step = gridWidth / (boardSize - 1)
        
        val startX = boardPaddingPx + coordinateMarginPx
        val startY = boardPaddingPx + coordinateMarginPx

        // Paint configurations for canvas drawing
        val paintTextDark = remember(labelColor) {
            Paint().apply {
                color = labelColor.toArgb()
                textSize = 24f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
        }

        val paintScoreText = remember {
            Paint().apply {
                textSize = 20f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boardSize, showCoordinates, onCellTapped) {
                    detectTapGestures { offset ->
                        // Reverse coordinates map from screen offset to closest board cell indices
                        val colDouble = (offset.x - startX) / step
                        val rowDouble = (offset.y - startY) / step
                        
                        val col = colDouble.roundToInt()
                        val row = rowDouble.roundToInt()
                        
                        if (row in 0 until boardSize && col in 0 until boardSize) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCellTapped(row, col)
                        }
                    }
                }
        ) {
            // 1. Draw Board Grid Lines
            for (i in 0 until boardSize) {
                val currentLineCoordX = startX + i * step
                val currentLineCoordY = startY + i * step

                // Horizontal grid line
                drawLine(
                    color = gridLineColor,
                    start = Offset(startX, currentLineCoordY),
                    end = Offset(startX + gridWidth, currentLineCoordY),
                    strokeWidth = 2f
                )

                // Vertical grid line
                drawLine(
                    color = gridLineColor,
                    start = Offset(currentLineCoordX, startY),
                    end = Offset(currentLineCoordX, startY + gridWidth),
                    strokeWidth = 2f
                )
            }

            // 2. Draw standard Go Board Star Points (On a 15x15 board, typically index 3, 7, 11)
            val starPoints = listOf(3, 7, 11)
            for (sr in starPoints) {
                for (sc in starPoints) {
                    val px = startX + sc * step
                    val py = startY + sr * step
                    drawCircle(
                        color = gridLineColor,
                        radius = 6f,
                        center = Offset(px, py)
                    )
                }
            }

            // 3. Draw Coordinate Labels around the edges (Letters at Top/Bottom, Numbers at Left/Right)
            if (showCoordinates) {
                for (i in 0 until boardSize) {
                    // Match letters A-O skipping 'I' to resemble traditional Go game coordinators
                    val colChar = ('A' + i).let { if (it >= 'I') it + 1 else it }
                    val labelY = 15 - i // Rows numbered from 1 at bottom to 15 at top

                    val px = startX + i * step
                    val py = startY + i * step

                    // Draw letters at top edge
                    drawContext.canvas.nativeCanvas.drawText(
                        colChar.toString(),
                        px,
                        startY - 12f,
                        paintTextDark
                    )

                    // Draw letters at bottom edge
                    drawContext.canvas.nativeCanvas.drawText(
                        colChar.toString(),
                        px,
                        startY + gridWidth + 30f,
                        paintTextDark
                    )

                    // Draw numbers at left edge
                    drawContext.canvas.nativeCanvas.drawText(
                        labelY.toString(),
                        startX - 22f,
                        py + 8f,
                        paintTextDark
                    )

                    // Draw numbers at right edge
                    drawContext.canvas.nativeCanvas.drawText(
                        labelY.toString(),
                        startX + gridWidth + 24f,
                        py + 8f,
                        paintTextDark
                    )
                }
            }

            // 4. Draw Heatmap / Move Scores if enabled
            if (showEvaluations) {
                for (r in 0 until boardSize) {
                    for (c in 0 until boardSize) {
                        val scoreValue = cellScores[r][c]
                        if (scoreValue == 0f || board[r][c] != 0) continue

                        val px = startX + c * step
                        val py = startY + r * step

                        if (scoreValue == -999.0f) {
                            // Forbidden move indicator (draw a red cross/X icon)
                            drawLine(
                                color = Color.Red.copy(alpha = 0.8f),
                                start = Offset(px - 10f, py - 10f),
                                end = Offset(px + 10f, py + 10f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.Red.copy(alpha = 0.8f),
                                start = Offset(px + 10f, py - 10f),
                                end = Offset(px - 10f, py + 10f),
                                strokeWidth = 3f
                            )
                        } else {
                            // Format score and decide visual color
                            val scoreStr = if (scoreValue >= 1000f) {
                                (scoreValue / 1000f).roundToInt().toString() + "k"
                            } else if (scoreValue >= 10f) {
                                scoreValue.roundToInt().toString()
                            } else {
                                String.format("%.1f", scoreValue)
                            }

                            // Choose text color based on magnitude (offensive priority heatmap)
                            val displayColor = when {
                                scoreValue >= 1500f -> Color(0xFFD50000) // Huge threat/Five - dark red
                                scoreValue >= 400f -> Color(0xFFFF6D00)  // Significant move (four/three) - vivid orange
                                scoreValue >= 80f -> Color(0xFFE65100)   // Medium - deep orange
                                scoreValue >= 10f -> Color(0xFF00701A)   // Safe - green
                                else -> Color(0xFF3E2723).copy(alpha = 0.6f) // Minor - transparent brown
                            }

                            paintScoreText.color = displayColor.toArgb()
                            // Center offset offset adjusting
                            drawContext.canvas.nativeCanvas.drawText(
                                scoreStr,
                                px,
                                py + 8f,
                                paintScoreText
                            )
                        }
                    }
                }
            }

            // 5. Draw Target AI Best Move Suggestion Highlight - Modern Minimalist Purple Theme
            if (bestAiMove != null) {
                val (br, bc) = bestAiMove
                if (board[br][bc] == 0) {
                    val px = startX + bc * step
                    val py = startY + br * step
                    
                    // Draw outer dashed/glowing ring in active purple #6750A4
                    drawCircle(
                        color = Color(0xFF6750A4),
                        radius = step * 0.44f,
                        center = Offset(px, py),
                        style = Stroke(width = 3.5f)
                    )
                    
                    // Draw inner soft light lavender fill
                    drawCircle(
                        color = Color(0xFFEADDFF).copy(alpha = 0.45f),
                        radius = step * 0.35f,
                        center = Offset(px, py)
                    )
                }
            }

            // 6. Draw Placed Stones on the grid intersections
            val lastMove = movesHistory.lastOrNull()
            val stoneRadius = step * 0.44f

            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    val stone = board[r][c]
                    if (stone == 0) continue

                    val px = startX + c * step
                    val py = startY + r * step

                    if (stone == 1) {
                        // Glossy Black Stone using subtle radial brush
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF424242), Color(0xFF000000)),
                                center = Offset(px - stoneRadius * 0.3f, py - stoneRadius * 0.3f),
                                radius = stoneRadius * 1.2f
                            ),
                            radius = stoneRadius,
                            center = Offset(px, py)
                        )
                        // Dark shadow/border
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.8f),
                            radius = stoneRadius,
                            center = Offset(px, py),
                            style = Stroke(width = 1f)
                        )
                    } else {
                        // Glossy White/Ivory Stone with highlight
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFB0BEC5)),
                                center = Offset(px - stoneRadius * 0.25f, py - stoneRadius * 0.25f),
                                radius = stoneRadius * 1.1f
                            ),
                            radius = stoneRadius,
                            center = Offset(px, py)
                        )
                        // Soft perimeter border
                        drawCircle(
                            color = Color(0xFF90A4AE),
                            radius = stoneRadius,
                            center = Offset(px, py),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Highlight the absolute last-placed stone to help users track gameplay
                    if (lastMove != null && lastMove.row == r && lastMove.col == c) {
                        val markerColor = if (stone == 1) Color.White else Color.Black
                        drawCircle(
                            color = markerColor,
                            radius = 4f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}
