package com.sparklicorn.bucket.games.tetris;

import java.util.function.Consumer;

import com.sparklicorn.bucket.util.event.Event;
import com.sparklicorn.bucket.games.tetris.util.structs.Coord;
import com.sparklicorn.bucket.games.tetris.util.structs.Shape;

/**
 * An interface to the game of Tetris.
 * <br>
 * Describes functionality necessary for the controlling the game.
 * <br><br>
 * Assumptions: <ul>
 * <li>Piece shapes are represented by integers. {@link Shape} is provided
 * for this purpose.</li>
 * <li>Blocks on the game board are represented by a 1D array of integers.
 * Each element should correspond with a Shape index or 0 to denote an empty
 * block.</li>
 * <li>Locations on the game board are represented by row, column coordinates.
 * {@link Coord} is provided for this purpose.</li>
 * </ul>
 * <br>
 */
public interface ITetrisGame {

	//Constants
	public static final int NUM_SHAPES = Shape.NUM_SHAPES;

	//Game actions

	/** Creates a new game of Tetris. Does not automatically start the game.*/
	public void newGame();

	/**
	 * Starts the game.
	 * @param level - The level to start with.
	 */
	public void start(long level);

	/** Stops the game.*/
	public void stop();

	/** Pauses the game.*/
	public void pause();

	/** Resumes the game from the paused state.*/
	public void resume();

	//Game state

	/**
	 * Returns true if the game is over. If the game was just created
	 * but not yet started, then this should return false.
	 * @return True if the game is over; otherwise false.
	 */
	public boolean isGameOver();

	/**
	 * Returns true if the game has been started.
	 * @return True if the game has started; otherwise false.
	 */
	public boolean hasStarted();

	/**
	 * Returns true if the game is in a paused state. If the game was
	 * just created but not yet started, then this should return false.
	 * @return True if the game is paused; otherwise false.
	 */
	public boolean isPaused();

	/**
	 * Returns true if the game is actively clearing lines.
	 * @return True if the game is clearing lines; otherwise false.
	 */
	public boolean isClearingLines();

	/**
	 * Returns an array representing the blocks currently on the game board.
	 * @return <code>int[]</code> containing the block data.
	 */
	public int[] getBlocksOnBoard();

	/**
	 * Returns an array representing the blocks currently on the game board.
	 * @param blocks - A pre-allocated array to populate with the block data.
	 * @return <code>int[]</code> containing the block data.
	 */
	public int[] getBlocksOnBoard(int[] blocks);

	//Game stats

	/**
	 * Returns the current level.
	 * @return The current level.
	 */
	public long getLevel();

	/**
	 * Returns the current score.
	 * @return The current score.
	 */
	public long getScore();

	/**
	 * Returns the number of lines cleared so far in the active game.
	 * @return The number of lines cleared so far.
	 */
	public long getLinesCleared();

	/**
	 * Returns the number of line clears needed to progress to the next level.
	 * @return The number of line clears needed to progress to the next level.
	 */
	public int getLinesUntilNextLevel();

	/**
	 * Returns the current shape distribution in the active game.
	 * Each index in the returned array corresponds to the value of a
	 * specific Shape, found by <code>(someShape).value</code>.
	 * @return An array containing the distribution of shapes.
	 */
	public long[] getDistribution();

	/**
	 * Returns the number of pieces that have been placed.
	 * @return The number of pieces that have been placed.
	 */
	public long getNumPiecesDropped();

	//Game configuration

	/**
	 * Returns the number of rows on the game board.
	 * @return The number of rows on the game board.
	 */
	public int getNumRows();

	/**
	 * Returns the number of columns on the game board.
	 * @return The number of columns on the game board.
	 */
	public int getNumCols();

	//Game Piece state (Piece being the player controlled game piece)

	/**
	 * Returns true if the player piece is currently active.
	 * The piece is active if it is movable by the player. If the game
	 * has not yet been started, is over, is paused, or if lines are being
	 * cleared, then this should return false.
	 * @return True if the player piece is active and can be moved;
	 * otherwise false.
	 */
	public boolean isPieceActive();

	/**
	 * Returns the locations of the blocks that make up the player piece.
	 * @return Coordinates of the blocks that make up the player piece.
	 */
	public Coord[] getPieceBlocks();

	/**
	 * Return the shape of the next piece.
	 * @return The shape of the next piece.
	 */
	public Shape getNextShape();

	/**
	 * Return the shape of the current piece.
	 * @return The shape of the current piece.
	 */
	public Shape getCurrentShape();

	/**
	 * Return the location of the current piece. Since the piece
	 * is made of several blocks, this should return the location
	 * of the pivotal block, that is, the block on which the piece
	 * rotates.
	 * @return Coordinates of the current piece.
	 */
	public Coord getLocation();

	//Piece actions

	/**
	 * Attempts to rotate the active piece clockwise.
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	public boolean rotateClockwise();

	/**
	 * Attempts to rotate the active piece counter-clockwise.
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	public boolean rotateCounterClockwise();

	/**
	 * Attempts to shift the piece the specified number of rows and columns.
	 * @param rowOffset - Number of rows to shift.
	 * @param colOffset - Number of columns to shift.
	 * @return True if the piece shifted successfully; otherwise false.
	 */
	public boolean shift(int rowOffset, int colOffset);


	//Game Events

	/**
	 * Registers some listener code to be run when the specified event occurs.
	 * @param event - The TetrisEvent to listen for.
	 * @param listener - The listener to run when event occurs.
	 * @return True if the event was successfully registered; otherwise false
	 * for any other reason.
	 */
	public boolean registerEventListener(TetrisEvent event, Consumer<Event> listener);

	/**
	 * Unregisters some listener object from the specified event.
	 * @param event - The event that the listener is registered for.
	 * @param listener - The listener to unregister.
	 * @return True if the listener was successfully unregistered; otherwise false.
	 */
	public boolean unregisterEventListener(TetrisEvent event, Consumer<Event> listener);

	/**
	 * Let's the implementation know that it should terminate any active resources
	 * for a graceful shutdown.
	 */
	public void shutdown();

}
