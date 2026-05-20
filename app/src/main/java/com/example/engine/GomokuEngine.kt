package com.example.engine

data class GomokuMove(
    val row: Int,
    val col: Int,
    val player: Int, // 1 = Black, 2 = White
    val moveNumber: Int
) {
    val coordinateName: String
        get() {
            val colChar = ('A' + col).let { if (it >= 'I') it + 1 else it } // Traditional Go coordinates skip 'I'
            val rowNum = 15 - row
            return "$colChar$rowNum"
        }
}

enum class GameRule {
    FREESTYLE, // 5 or more wins, no restrictions
    GOMOKU,    // Exactly 5 wins, no restrictions
    RENJU      // Strict tournament rules: Black has forbidden moves (double-three, double-four, overline)
}

class GomokuEngine(val boardSize: Int = 15) {
    // 0 = Empty, 1 = Black, 2 = White
    val board = Array(boardSize) { IntArray(boardSize) { 0 } }
    
    var currentTurn = 1 // 1 = Black, 2 = White
        private set
        
    val movesHistory = mutableListOf<GomokuMove>()
    val redoStack = mutableListOf<GomokuMove>()
    
    var activeRule = GameRule.GOMOKU
    
    // Listeners can be notified on board changes
    var onBoardChanged: (() -> Unit)? = null

    fun getStone(row: Int, col: Int): Int {
        if (row in 0 until boardSize && col in 0 until boardSize) {
            return board[row][col]
        }
        return 0
    }

    /**
     * Manually setup a stone (for custom analysis setups)
     */
    fun setupStone(row: Int, col: Int, player: Int) {
        if (row in 0 until boardSize && col in 0 until boardSize) {
            val prev = board[row][col]
            if (prev != player) {
                board[row][col] = player
                // Clear redo when we make custom changes
                redoStack.clear()
                onBoardChanged?.invoke()
            }
        }
    }

    fun toggleMoveOrder() {
        currentTurn = if (currentTurn == 1) 2 else 1
        onBoardChanged?.invoke()
    }

    fun setTurn(turn: Int) {
        if (turn == 1 || turn == 2) {
            currentTurn = turn
            onBoardChanged?.invoke()
        }
    }

    /**
     * Attempts to place a stone at the target row/col of the current turn
     */
    fun placeStone(row: Int, col: Int): Boolean {
        if (row !in 0 until boardSize || col !in 0 until boardSize) return false
        if (board[row][col] != 0) return false

        // Check Renju forbidden moves before placing
        if (activeRule == GameRule.RENJU && currentTurn == 1) {
            if (isForbiddenMove(row, col, 1)) {
                return false
            }
        }

        val moveNum = movesHistory.size + 1
        val move = GomokuMove(row, col, currentTurn, moveNum)
        board[row][col] = currentTurn
        movesHistory.add(move)
        redoStack.clear()

        // Switch turn
        currentTurn = if (currentTurn == 1) 2 else 1
        onBoardChanged?.invoke()
        return true
    }

    fun undo(): Boolean {
        if (movesHistory.isEmpty()) return false
        val removed = movesHistory.removeLast()
        board[removed.row][removed.col] = 0
        redoStack.add(removed)
        currentTurn = removed.player
        onBoardChanged?.invoke()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val restored = redoStack.removeLast()
        board[restored.row][restored.col] = restored.player
        movesHistory.add(restored)
        currentTurn = if (restored.player == 1) 2 else 1
        onBoardChanged?.invoke()
        return true
    }

    fun clear() {
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                board[r][c] = 0
            }
        }
        movesHistory.clear()
        redoStack.clear()
        currentTurn = 1
        onBoardChanged?.invoke()
    }

    /**
     * Check if there is a winner on the board
     * Returns: 0 = None, 1 = Black, 2 = White, 3 = Draw
     */
    fun checkWinner(): Int {
        val directions = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal Down-Right
            Pair(1, -1)   // Diagonal Down-Left
        )

        var hasEmpty = false
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val stone = board[r][c]
                if (stone == 0) {
                    hasEmpty = true
                    continue
                }

                for ((dr, dc) in directions) {
                    var count = 1
                    
                    // Match line forward
                    var nr = r + dr
                    var nc = c + dc
                    while (nr in 0 until boardSize && nc in 0 until boardSize && board[nr][nc] == stone) {
                        count++
                        nr += dr
                        nc += dc
                    }

                    if (activeRule == GameRule.GOMOKU || activeRule == GameRule.RENJU) {
                        // Under Gomoku/Renju rule, exactly 5 is a win.
                        // For Black under Renju, overline (6+) is forbidden and does not win.
                        if (stone == 1) {
                            if (count == 5) {
                                // Double check if it's strictly 5 (not part of a larger line)
                                // We check the opposite direction
                                var pr = r - dr
                                var pc = c - dc
                                var beforeCount = 0
                                while (pr in 0 until boardSize && pc in 0 until boardSize && board[pr][pc] == stone) {
                                    beforeCount++
                                    pr -= dr
                                    pc -= dc
                                }
                                if (count + beforeCount == 5) {
                                    return stone
                                }
                            }
                        } else {
                            // White can win with 5 or more (overline is allowed for White under Renju)
                            if (count >= 5) {
                                return stone
                            }
                        }
                    } else {
                        // Freestyle: 5 or more wins
                        if (count >= 5) {
                            return stone
                        }
                    }
                }
            }
        }

        return if (hasEmpty) 0 else 3 // 3 = Draw (No empty spots left)
    }

    /**
     * Checks if a move by player 1 (Black) is forbidden under Renju rules.
     * Forbidden moves for Black:
     * - Double Three: Making more than one open-three simultaneously.
     * - Double Four: Making more than one four simultaneously.
     * - Overline: Making a line of 6 or more black stones.
     */
    fun isForbiddenMove(row: Int, col: Int, player: Int): Boolean {
        if (player != 1) return false // Renju restrictions apply only to Black
        
        // Count lines
        val directions = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal Down-Right
            Pair(1, -1)   // Diagonal Down-Left
        )

        var overline = false
        var openThreesCount = 0
        var foursCount = 0

        for ((dr, dc) in directions) {
            // Find length and endpoints of the line passing through (row, col)
            // We scan up to 5 steps in both directions to see the exact pattern
            val lineStones = mutableListOf<Int>() // -1 for border / opponent, 0 for empty, 1 for self
            
            // Let's analyze local neighborhood of length 9 centered at row/col
            for (i in -4..4) {
                val nr = row + i * dr
                val nc = col + i * dc
                if (nr in 0 until boardSize && nc in 0 until boardSize) {
                    if (nr == row && nc == col) {
                        lineStones.add(1) // Simulate placing Black
                    } else {
                        lineStones.add(board[nr][nc])
                    }
                } else {
                    lineStones.add(-1) // Out of bounds
                }
            }

            // Check Overline (exactly 6 or more stones in a contiguous block)
            var maxContiguous = 0
            var currentContiguous = 0
            for (cell in lineStones) {
                if (cell == 1) {
                    currentContiguous++
                    if (currentContiguous > maxContiguous) {
                        maxContiguous = currentContiguous
                    }
                } else {
                    currentContiguous = 0
                }
            }
            if (maxContiguous >= 6) {
                overline = true
            }

            // Simple check for fours and open threes
            // Open Three is: 0 1 1 1 0 (exactly three stones, open at both ends and can be extended to 4)
            // Four is: 1 1 1 1 (four stones) which can become five.
            val patternString = lineStones.joinToString("") { 
                when(it) {
                    1 -> "1"
                    0 -> "0"
                    else -> "X"
                }
            }

            // Match open three configurations: "01110", "010110", "011010"
            if (patternString.contains("01110") || patternString.contains("010110") || patternString.contains("011010")) {
                openThreesCount++
            }

            // Match four configurations (both open and closed): "1111", "11011", "10111", "11101"
            if (patternString.contains("1111") || 
                patternString.contains("11011") || 
                patternString.contains("10111") || 
                patternString.contains("11101")) {
                foursCount++
            }
        }

        if (overline) return true
        if (openThreesCount >= 2) return true // Double Three (3x3)
        if (foursCount >= 2) return true     // Double Four (4x4)

        return false
    }
}
