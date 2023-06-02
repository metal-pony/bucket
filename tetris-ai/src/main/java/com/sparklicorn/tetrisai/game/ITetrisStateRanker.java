package com.sparklicorn.tetrisai.game;

import com.sparklicorn.bucket.tetris.TetrisGame;

@FunctionalInterface
public interface ITetrisStateRanker {
	/**
	 * Ranks a game state (the ordering of blocks) according to how
	 * desirable it is. A higher value indicates a higher desirability
	 * for the given state.
	 *
	 * @param state - Represents the blocks currently placed on the
	 *     Tetris board, excluding the active piece, if it exists.
	 *     <br>The array indicies correspond with the rows and columns of the
	 *     game board, where (0,0) is the topleft cell.
	 * @param rows - Number of rows on the board.
	 * @param cols - Number of columns on the board.
	 * @param next - The shape of the next piece.
	 * @return A value representing the desirability of the given game state.
	 */
	public abstract double rank(TetrisGame game);
	// public abstract double rank(int[] state, int rows, int cols, Shape next);
}
