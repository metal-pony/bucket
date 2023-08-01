package com.sparklicorn.bucket.tetris;

import com.sparklicorn.bucket.tetris.util.structs.Shape;

/**
 * An interface to the game of Tetris.
 */
public interface ITetrisGame {
	public static final int NUM_SHAPES = Shape.NUM_SHAPES;

	/**
	 * Gets the current state of the game.
	 */
	public TetrisState getState();

	/********************
	    Game Controls
	*********************/

	/**
	 * Sets up a new game of Tetris.
	 */
	public void newGame();

	/**
	 * Starts the game at the given level.
	 *
	 * @param level - The level to begin on.
	 */
	public void start(long level);

	/**
	 * Stops the game.
	 */
	public void stop();

	/**
	 * Pauses the game.
	 */
	public void pause();

	/**
	 * Resumes the game from the paused state.
	 */
	public void resume();

	/**************************
	    Game Piece Controls
	***************************/

	/**
	 * Attempts to rotate the active piece clockwise.
	 *
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	public boolean rotateClockwise();

	/**
	 * Attempts to rotate the active piece counter-clockwise.
	 *
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	public boolean rotateCounterClockwise();

	/**
	 * Attempts to shift the piece the specified number of rows and columns.
	 *
	 * @param rowOffset - Number of rows to shift.
	 * @param colOffset - Number of columns to shift.
	 * @return True if the piece shifted successfully; otherwise false.
	 */
	public boolean shift(int rowOffset, int colOffset);
}
