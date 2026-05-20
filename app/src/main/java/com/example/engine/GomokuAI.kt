package com.example.engine

import kotlin.math.max

object GomokuAI {

    // Pattern weight values
    const val SCORE_FIVE = 10000000 // Five in a row (Win)
    const val SCORE_OPEN_FOUR = 500000 // Open Four (e.g. .oooo. - guaranteed win next turn)
    const val SCORE_CLOSED_FOUR = 10000 // Closed Four (e.g. xooo. or .oo.oo. - can make 5)
    const val SCORE_OPEN_THREE = 8000 // Open Three (e.g. .ooo. - can become open four)
    const val SCORE_CLOSED_THREE = 1000 // Closed Three (e.g. xooo..)
    const val SCORE_OPEN_TWO = 800 // Open Two (e.g. ..oo..)
    const val SCORE_CLOSED_TWO = 100 // Closed Two
    const val SCORE_ONE = 10 // Single stone

    /**
     * Evaluates the global strength of the board.
     * Positive scores favor Black (Player 1), Negative scores favor White (Player 2).
     */
    fun evaluateBoard(board: Array<IntArray>, activeRule: GameRule): Int {
        val size = board.size
        var blackScore = 0
        var whiteScore = 0

        // We check every cell and evaluate patterns centered there.
        // To avoid double-counting, we can evaluate each direction globally.
        val directions = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal Down-Right
            Pair(1, -1)   // Diagonal Down-Left
        )

        for (r in 0 until size) {
            for (c in 0 until size) {
                val stone = board[r][c]
                if (stone == 0) continue

                for ((dr, dc) in directions) {
                    // Check if this is the start of a sequence of this stone color
                    // (meaning the preceding cell was not of the same color, to avoid overlapping counts)
                    val pr = r - dr
                    val pc = c - dc
                    if (pr in 0 until size && pc in 0 until size && board[pr][pc] == stone) {
                        continue // Skip, already counted from the previous cell
                    }

                    // Count consecutive stones
                    var count = 0
                    var tr = r
                    var tc = c
                    while (tr in 0 until size && tc in 0 until size && board[tr][tc] == stone) {
                        count++
                        tr += dr
                        tc += dc
                    }

                    // Check boundaries at both ends of this stone line
                    val leftR = r - dr
                    val leftC = c - dc
                    val rightR = tr // tr is already incremented past the end
                    val rightC = tc

                    val leftOpen = leftR in 0 until size && leftC in 0 until size && board[leftR][leftC] == 0
                    val rightOpen = rightR in 0 until size && rightC in 0 until size && board[rightR][rightC] == 0

                    val score = when {
                        count >= 5 -> {
                            if (activeRule == GameRule.RENJU && stone == 1) {
                                // For Black in Renju, exactly 5 wins. Overline (6+) is a forbidden move.
                                if (count == 5) SCORE_FIVE else -SCORE_FIVE // Overline is bad for Black
                            } else {
                                SCORE_FIVE
                            }
                        }
                        count == 4 -> {
                            if (leftOpen && rightOpen) SCORE_OPEN_FOUR else if (leftOpen || rightOpen) SCORE_CLOSED_FOUR else 0
                        }
                        count == 3 -> {
                            if (leftOpen && rightOpen) SCORE_OPEN_THREE else if (leftOpen || rightOpen) SCORE_CLOSED_THREE else 0
                        }
                        count == 2 -> {
                            if (leftOpen && rightOpen) SCORE_OPEN_TWO else if (leftOpen || rightOpen) SCORE_CLOSED_TWO else 0
                        }
                        else -> SCORE_ONE
                    }

                    if (stone == 1) {
                        blackScore += score
                    } else {
                        whiteScore += score
                    }
                }
            }
        }

        return blackScore - whiteScore
    }

    /**
     * Calculates local scoring weight for every empty cell.
     * This defines the "Calculator Overlay" shown on the screen.
     *
     * How it works:
     * For each empty cell, simulate placing a Black stone and evaluate,
     * and simulate placing a White stone and evaluate.
     * Under the hood, we return a score that is a combination of:
     * - Attacking value (how much it helps the active player build a pattern)
     * - Defending value (how much it blocks the opponent's highly threatening patterns)
     */
    fun calculateCellScores(
        board: Array<IntArray>,
        activePlayer: Int,
        activeRule: GameRule
    ): Array<FloatArray> {
        val size = board.size
        val scores = Array(size) { FloatArray(size) { 0f } }
        val opponent = if (activePlayer == 1) 2 else 1

        // Optimization: Only score empty cells that are within a distance of 2 from any existing stone
        // This is extremely standard for Gomoku to avoid unnecessary calculations and keep the UI lightning-fast.
        val hasStones = hasAnyStones(board)

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] != 0) {
                    continue // Already occupied
                }

                if (hasStones && !isNeighbour(board, r, c, range = 2)) {
                    continue // Skip isolated cells
                }

                // If Renju rule and player is Black (1), check if this is forbidden
                if (activeRule == GameRule.RENJU && activePlayer == 1) {
                    // Let's check if playing here is a forbidden move
                    // If so, assign a negative indicator score so it's highlighted as blocked
                    if (isForbiddenSim(board, r, c)) {
                        scores[r][c] = -999.0f // Blocked move indicator
                        continue
                    }
                }

                // Simulate Attack
                val attackScore = evaluateCellMoveValue(board, r, c, activePlayer, activeRule)
                // Simulate Defend
                val defendScore = evaluateCellMoveValue(board, r, c, opponent, activeRule)

                // The combined heuristic weight: Attack is prioritized slightly, but defending against high threats is critical
                val combined = (attackScore * 1.1) + (defendScore * 0.95)
                
                // Let's normalize/format the score for the numbers shown on the screen
                // We want readable values from 0.1 up to 999.0+
                scores[r][c] = (combined / 100.0).toFloat()
            }
        }

        return scores
    }

    private fun hasAnyStones(board: Array<IntArray>): Boolean {
        for (r in board.indices) {
            for (c in board[r].indices) {
                if (board[r][c] != 0) return true
            }
        }
        return false
    }

    private fun isForbiddenSim(board: Array<IntArray>, row: Int, col: Int): Boolean {
        // Evaluate the lines as if there is a black stone at (row, col) without actually mutating the board
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )
        var overline = false
        var openThreesCount = 0
        var foursCount = 0

        for ((dr, dc) in directions) {
            val lineStones = mutableListOf<Int>()
            for (i in -4..4) {
                val nr = row + i * dr
                val nc = col + i * dc
                if (nr in board.indices && nc in board[0].indices) {
                    if (nr == row && nc == col) {
                        lineStones.add(1) // Simulate placing Black
                    } else {
                        lineStones.add(board[nr][nc])
                    }
                } else {
                    lineStones.add(-1)
                }
            }

            var maxContiguous = 0
            var currentContiguous = 0
            for (cell in lineStones) {
                if (cell == 1) {
                    currentContiguous++
                    if (currentContiguous > maxContiguous) maxContiguous = currentContiguous
                } else {
                    currentContiguous = 0
                }
            }
            if (maxContiguous >= 6) {
                overline = true
            }

            val patternString = lineStones.joinToString("") { 
                when(it) {
                    1 -> "1"
                    0 -> "0"
                    else -> "X"
                }
            }

            if (patternString.contains("01110") || patternString.contains("010110") || patternString.contains("011010")) {
                openThreesCount++
            }

            if (patternString.contains("1111") || 
                patternString.contains("11011") || 
                patternString.contains("10111") || 
                patternString.contains("11101")) {
                foursCount++
            }
        }

        if (overline) return true
        if (openThreesCount >= 2) return true
        if (foursCount >= 2) return true

        return false
    }

    /**
     * Helper to verify if cell is adjacent to any placed stones
     */
    private fun isNeighbour(board: Array<IntArray>, row: Int, col: Int, range: Int): Boolean {
        for (dr in -range..range) {
            for (dc in -range..range) {
                val nr = row + dr
                val nc = col + dc
                if (nr in board.indices && nc in board[0].indices && board[nr][nc] != 0) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Evaluates the local value of a move at (row, col) for specified player.
     * Evaluates patterns created along horizontal, vertical, and diagonals.
     */
    private fun evaluateCellMoveValue(
        board: Array<IntArray>,
        row: Int,
        col: Int,
        player: Int,
        rule: GameRule
    ): Double {
        var totalScore = 0.0
        val directions = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal Down-Right
            Pair(1, -1)   // Diagonal Down-Left
        )

        // For each of the 4 directions, we scan local window centered at row, col
        for ((dr, dc) in directions) {
            // Let's inspect sliding windows of 5 cells containing (row, col)
            // There are 5 such windows: start from i = -4 to 0
            for (i in -4..0) {
                var selfCount = 0
                var emptyCount = 0
                var opponentCount = 0
                var validWindow = true

                for (j in 0..4) {
                    val nr = row + (i + j) * dr
                    val nc = col + (i + j) * dc

                    if (nr !in board.indices || nc !in board[0].indices) {
                        validWindow = false
                        break
                    }

                    val cell = board[nr][nc]
                    if (nr == row && nc == col) {
                        // The cell itself counts as self stone in this hypothetical move
                        selfCount++
                    } else {
                        when (cell) {
                            player -> selfCount++
                            0 -> emptyCount++
                            else -> opponentCount++
                        }
                    }
                }

                if (!validWindow) continue

                // Assign scoring weights matching standard Gomoku structures
                if (opponentCount == 0) {
                    val winScore = when (selfCount) {
                        5 -> SCORE_FIVE.toDouble()
                        4 -> SCORE_OPEN_FOUR.toDouble() * 0.95
                        3 -> SCORE_OPEN_THREE.toDouble() * 0.9
                        2 -> SCORE_OPEN_TWO.toDouble() * 0.8
                        1 -> SCORE_ONE.toDouble()
                        else -> 0.0
                    }
                    totalScore += winScore
                } else if (opponentCount == 1) {
                    // Closed patterns (one end blocked)
                    val blockScore = when (selfCount) {
                        5 -> SCORE_FIVE.toDouble()
                        4 -> SCORE_CLOSED_FOUR.toDouble() * 0.5
                        3 -> SCORE_CLOSED_THREE.toDouble() * 0.4
                        2 -> SCORE_CLOSED_TWO.toDouble() * 0.3
                        else -> 0.0
                    }
                    totalScore += blockScore
                }
            }
        }

        return totalScore
    }

    /**
     * Computes the absolute best move using minimax with alpha-beta search.
     * Falls back to high-heuristic scoring cell if search yields no difference.
     */
    fun findBestMove(engine: GomokuEngine, difficultyDepth: Int): Pair<Int, Int>? {
        val size = engine.boardSize
        val board = engine.board
        val currentPlayer = engine.currentTurn
        val activeRule = engine.activeRule

        // Find candidate moves with the highest scores first to drastically speed up pruning
        val cellScores = calculateCellScores(board, currentPlayer, activeRule)
        val candidates = mutableListOf<Pair<Pair<Int, Int>, Float>>()

        for (r in 0 until size) {
            for (c in 0 until size) {
                val score = cellScores[r][c]
                if (score > 0) {
                    candidates.add(Pair(Pair(r, c), score))
                }
            }
        }

        if (candidates.isEmpty()) {
            // First move: usually place in center (7, 7) of a 15x15 board
            if (board[7][7] == 0) {
                return Pair(7, 7)
            }
            // Or find first open spot
            for (r in 0 until size) {
                for (c in 0 until size) {
                    if (board[r][c] == 0) return Pair(r, c)
                }
            }
            return null
        }

        // Sort candidates descending by heuristic score
        candidates.sortByDescending { it.second }

        // If simple settings or first level depth, just return the highest heuristic score move
        if (difficultyDepth <= 1) {
            return candidates.first().first
        }

        // Run bounded minimax search on the top 8 candidates to find the most tactical move!
        val topCandidates = candidates.take(8)
        var bestMove = topCandidates.first().first
        var bestVal = if (currentPlayer == 1) Int.MIN_VALUE else Int.MAX_VALUE

        val alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        // Local copy of the board to perform fast search states
        val searchBoard = Array(size) { board[it].clone() }

        for ((pos, _) in topCandidates) {
            val (r, c) = pos
            searchBoard[r][c] = currentPlayer

            val nextPlayer = if (currentPlayer == 1) 2 else 1
            val moveVal = minimax(
                searchBoard,
                difficultyDepth - 1,
                alpha,
                beta,
                nextPlayer,
                currentPlayer == 1, // isMaximizing depends on whether black is active
                activeRule
            )

            // Undo move
            searchBoard[r][c] = 0

            if (currentPlayer == 1) {
                // Maximizing player (Black)
                if (moveVal > bestVal) {
                    bestVal = moveVal
                    bestMove = pos
                }
            } else {
                // Minimizing player (White)
                if (moveVal < bestVal) {
                    bestVal = moveVal
                    bestMove = pos
                }
            }
        }

        return bestMove
    }

    /**
     * Classic Minimax Search with Alpha-Beta pruning
     */
    private fun minimax(
        board: Array<IntArray>,
        depth: Int,
        alpha: Int,
        beta: Int,
        currentPlayer: Int,
        isMaximizing: Boolean,
        activeRule: GameRule
    ): Int {
        // Evaluate terminal state or depth limit
        val boardValue = evaluateBoard(board, activeRule)
        if (depth == 0) {
            return boardValue
        }

        // Quick win checks (if score is extremely high/low, we prune immediately)
        if (boardValue > SCORE_FIVE / 2) return boardValue
        if (boardValue < -SCORE_FIVE / 2) return boardValue

        val size = board.size
        // Gather candidate coordinates
        val candidates = mutableListOf<Triple<Int, Int, Double>>()
        val opponent = if (currentPlayer == 1) 2 else 1

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] == 0 && isNeighbour(board, r, c, range = 2)) {
                    // Use a simple, fast heuristic to pre-evaluate this local move
                    val score = evaluateCellMoveValue(board, r, c, currentPlayer, activeRule) +
                                evaluateCellMoveValue(board, r, c, opponent, activeRule)
                    candidates.add(Triple(r, c, score))
                }
            }
        }

        // Sort descending to optimize alpha-beta cutoffs
        candidates.sortByDescending { it.third }
        val topCandidates = candidates.take(6) // Only search top 6 branching nodes to guarantee under-200ms speeds

        if (topCandidates.isEmpty()) {
            return boardValue
        }

        var mutableAlpha = alpha
        var mutableBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for ((r, c, _) in topCandidates) {
                // Check if Black violates Renju rule
                if (activeRule == GameRule.RENJU && currentPlayer == 1) {
                    if (isForbiddenSim(board, r, c)) continue
                }

                board[r][c] = currentPlayer
                val eval = minimax(board, depth - 1, mutableAlpha, mutableBeta, opponent, false, activeRule)
                board[r][c] = 0

                maxEval = max(maxEval, eval)
                mutableAlpha = max(mutableAlpha, eval)
                if (mutableBeta <= mutableAlpha) {
                    break // Beta Cutoff
                }
            }
            return if (maxEval == Int.MIN_VALUE) boardValue else maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for ((r, c, _) in topCandidates) {
                board[r][c] = currentPlayer
                val eval = minimax(board, depth - 1, mutableAlpha, mutableBeta, opponent, true, activeRule)
                board[r][c] = 0

                minEval = kotlin.math.min(minEval, eval)
                mutableBeta = kotlin.math.min(mutableBeta, eval)
                if (mutableBeta <= mutableAlpha) {
                    break // Alpha Cutoff
                }
            }
            return if (minEval == Int.MAX_VALUE) boardValue else minEval
        }
    }
}
