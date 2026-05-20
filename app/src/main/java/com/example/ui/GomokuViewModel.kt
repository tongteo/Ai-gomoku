package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.GameRule
import com.example.engine.GomokuAI
import com.example.engine.GomokuEngine
import com.example.engine.GomokuMove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp

data class GomokuUiState(
    val board: Array<IntArray> = Array(15) { IntArray(15) { 0 } },
    val currentTurn: Int = 1, // 1 = Black, 2 = White
    val activeRule: GameRule = GameRule.GOMOKU,
    val movesHistory: List<GomokuMove> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    
    // UI Configurations
    val showEvaluations: Boolean = true,
    val showCoordinates: Boolean = true,
    val highlightBestMove: Boolean = true,
    
    // Game Modes
    val playVsAi: Boolean = true,
    val aiPlayerColor: Int = 2, // 2 = AI is White. 1 = AI is Black.
    val aiDifficultyDepth: Int = 3, // 1 = Easy, 2 = Medium, 3 = Hard, 4 = Grandmaster
    
    // Engine calculations and live feedback
    val cellScores: Array<FloatArray> = Array(15) { FloatArray(15) { 0f } },
    val bestAiMove: Pair<Int, Int>? = null,
    val isCalculatingAi: Boolean = false,
    val calcTimeMs: Long = 0,
    val winProbability: Float = 0.5f, // 0 to 1 scale, 0 = White wins, 1 = Black wins, 0.5 = Balanced
    
    // Analysis setup helper
    val setupModeActive: Boolean = false,
    val setupStoneColor: Int = 1, // Color to place in setup mode: 1 = Black, 2 = White, 0 = Clear
    
    val winner: Int = 0, // 0 = None, 1 = Black, 2 = White, 3 = Draw
    val renjuViolationMessage: String? = null
) {
    // Override equals/hashCode because of 2D Arrays
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GomokuUiState

        if (!board.contentDeepEquals(other.board)) return false
        if (currentTurn != other.currentTurn) return false
        if (activeRule != other.activeRule) return false
        if (movesHistory != other.movesHistory) return false
        if (canUndo != other.canUndo) return false
        if (canRedo != other.canRedo) return false
        if (showEvaluations != other.showEvaluations) return false
        if (showCoordinates != other.showCoordinates) return false
        if (highlightBestMove != other.highlightBestMove) return false
        if (playVsAi != other.playVsAi) return false
        if (aiPlayerColor != other.aiPlayerColor) return false
        if (aiDifficultyDepth != other.aiDifficultyDepth) return false
        if (!cellScores.contentDeepEquals(other.cellScores)) return false
        if (bestAiMove != other.bestAiMove) return false
        if (isCalculatingAi != other.isCalculatingAi) return false
        if (calcTimeMs != other.calcTimeMs) return false
        if (winProbability != other.winProbability) return false
        if (setupModeActive != other.setupModeActive) return false
        if (setupStoneColor != other.setupStoneColor) return false
        if (winner != other.winner) return false
        if (renjuViolationMessage != other.renjuViolationMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentTurn
        result = 31 * result + activeRule.hashCode()
        result = 31 * result + movesHistory.hashCode()
        result = 31 * result + canUndo.hashCode()
        result = 31 * result + canRedo.hashCode()
        result = 31 * result + showEvaluations.hashCode()
        result = 31 * result + showCoordinates.hashCode()
        result = 31 * result + highlightBestMove.hashCode()
        result = 31 * result + playVsAi.hashCode()
        result = 31 * result + aiPlayerColor
        result = 31 * result + aiDifficultyDepth
        result = 31 * result + cellScores.contentDeepHashCode()
        result = 31 * result + (bestAiMove?.hashCode() ?: 0)
        result = 31 * result + isCalculatingAi.hashCode()
        result = 31 * result + calcTimeMs.hashCode()
        result = 31 * result + winProbability.hashCode()
        result = 31 * result + setupModeActive.hashCode()
        result = 31 * result + setupStoneColor
        result = 31 * result + winner
        result = 31 * result + (renjuViolationMessage?.hashCode() ?: 0)
        return result
    }
}

class GomokuViewModel : ViewModel() {
    private val engine = GomokuEngine(boardSize = 15)
    
    private val _uiState = MutableStateFlow(GomokuUiState())
    val uiState: StateFlow<GomokuUiState> = _uiState.asStateFlow()

    init {
        // Sync engine board with ui state initially
        engine.onBoardChanged = {
            syncStateWithEngine()
            triggerAnalysis()
        }
        syncStateWithEngine()
        triggerAnalysis()
    }

    private fun syncStateWithEngine() {
        val boardCopy = Array(engine.boardSize) { r -> engine.board[r].clone() }
        _uiState.update { state ->
            state.copy(
                board = boardCopy,
                currentTurn = engine.currentTurn,
                movesHistory = engine.movesHistory.toList(),
                canUndo = engine.movesHistory.isNotEmpty(),
                canRedo = engine.redoStack.isNotEmpty(),
                winner = engine.checkWinner()
            )
        }
    }

    fun handleCellTap(row: Int, col: Int) {
        val state = _uiState.value
        if (state.winner != 0 || state.isCalculatingAi) return

        if (state.setupModeActive) {
            // Place stone according to setup color (1, 2, or 0)
            engine.setupStone(row, col, state.setupStoneColor)
        } else {
            // Regular gameplay
            if (engine.board[row][col] != 0) return

            // Check Renju forbidden move
            if (engine.activeRule == GameRule.RENJU && engine.currentTurn == 1) {
                if (engine.isForbiddenMove(row, col, 1)) {
                    _uiState.update { it.copy(renjuViolationMessage = "Forbidden Move! Double-three, double-four, or overline is blocked for Black in Renju.") }
                    return
                }
            }

            _uiState.update { it.copy(renjuViolationMessage = null) }
            val placed = engine.placeStone(row, col)
            
            if (placed) {
                val newState = _uiState.value
                // If Play vs AI is active and it's AI's turn, trigger background calculation
                if (newState.playVsAi && newState.winner == 0 && newState.currentTurn == newState.aiPlayerColor) {
                    computeAiMove()
                }
            }
        }
    }

    fun undo() {
        if (_uiState.value.isCalculatingAi) return
        _uiState.update { it.copy(renjuViolationMessage = null) }
        engine.undo()
    }

    fun redo() {
        if (_uiState.value.isCalculatingAi) return
        _uiState.update { it.copy(renjuViolationMessage = null) }
        engine.redo()
    }

    fun clearBoard() {
        if (_uiState.value.isCalculatingAi) return
        _uiState.update { it.copy(renjuViolationMessage = null) }
        engine.clear()
    }

    fun setGameRule(rule: GameRule) {
        engine.activeRule = rule
        _uiState.update { it.copy(activeRule = rule) }
        engine.clear() // Clear board when rules change to reset state cleanly
    }

    fun togglePlayVsAi() {
        _uiState.update { state ->
            val nextVal = !state.playVsAi
            state.copy(playVsAi = nextVal)
        }
        // If Play vs AI turned on and it is AI turn, trigger compute
        val state = _uiState.value
        if (state.playVsAi && state.winner == 0 && state.currentTurn == state.aiPlayerColor) {
            computeAiMove()
        }
    }

    fun setAiPlayerColor(color: Int) {
        _uiState.update { it.copy(aiPlayerColor = color) }
        val state = _uiState.value
        if (state.playVsAi && state.winner == 0 && state.currentTurn == color) {
            computeAiMove()
        }
    }

    fun setAiDifficulty(depth: Int) {
        _uiState.update { it.copy(aiDifficultyDepth = depth) }
    }

    fun toggleShowEvaluations() {
        _uiState.update { it.copy(showEvaluations = !it.showEvaluations) }
    }

    fun toggleShowCoordinates() {
        _uiState.update { it.copy(showCoordinates = !it.showCoordinates) }
    }

    fun toggleHighlightBestMove() {
        _uiState.update { it.copy(highlightBestMove = !it.highlightBestMove) }
    }

    fun setSetupMode(active: Boolean) {
        _uiState.update { it.copy(setupModeActive = active) }
    }

    fun setSetupStoneColor(color: Int) {
        _uiState.update { it.copy(setupStoneColor = color) }
    }

    fun toggleTurn() {
        engine.toggleMoveOrder()
    }

    fun forceEvaluateBestMove() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingAi = true) }
            val startTime = System.currentTimeMillis()
            
            val bestMoveResult = withContext(Dispatchers.Default) {
                GomokuAI.findBestMove(engine, _uiState.value.aiDifficultyDepth)
            }
            
            val endTime = System.currentTimeMillis()
            _uiState.update { 
                it.copy(
                    isCalculatingAi = false,
                    bestAiMove = bestMoveResult,
                    calcTimeMs = endTime - startTime
                ) 
            }
        }
    }

    /**
     * Compute a move automatically representing the AI player
     */
    fun computeAiMove() {
        val state = _uiState.value
        if (state.isCalculatingAi || state.winner != 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingAi = true) }
            val startTime = System.currentTimeMillis()

            val aiMove = withContext(Dispatchers.Default) {
                GomokuAI.findBestMove(engine, state.aiDifficultyDepth)
            }

            val endTime = System.currentTimeMillis()
            _uiState.update { 
                it.copy(
                    isCalculatingAi = false,
                    calcTimeMs = endTime - startTime
                ) 
            }

            if (aiMove != null) {
                engine.placeStone(aiMove.first, aiMove.second)
            }
        }
    }

    /**
     * Whenever board state updates, trigger background thread board evaluations
     * to compute cell scores (Heatmap weights) and winrate graph calculations dynamically.
     */
    private fun triggerAnalysis() {
        val state = _uiState.value
        viewModelScope.launch {
            val (scores, best, prob) = withContext(Dispatchers.Default) {
                val sc = GomokuAI.calculateCellScores(state.board, state.currentTurn, state.activeRule)
                
                // Find local top candidate and global winner estimation
                var bestCandidate: Pair<Int, Int>? = null
                var maxScore = -Float.MAX_VALUE
                for (r in sc.indices) {
                    for (c in sc[0].indices) {
                        if (sc[r][c] > maxScore) {
                            maxScore = sc[r][c]
                            bestCandidate = Pair(r, c)
                        }
                    }
                }

                // Global evaluation for the Win Rate Slider (Sigmoid normalization)
                val globalScore = GomokuAI.evaluateBoard(state.board, state.activeRule)
                // Map score: 0 -> 0.5f. Large positive (Black wins) -> 1.0f. Large negative (White wins) -> 0.0f
                val prob = (1.0f / (1.0f + exp(-globalScore / 50000.0f)))
                
                Triple(sc, bestCandidate, prob)
            }

            _uiState.update { currentState ->
                currentState.copy(
                    cellScores = scores,
                    bestAiMove = if (currentState.highlightBestMove) best else null,
                    winProbability = prob
                )
            }
        }
    }
}
