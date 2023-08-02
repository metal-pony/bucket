package com.sparklicorn.bucket.tetris;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sparklicorn.bucket.tetris.util.Timer;
import com.sparklicorn.bucket.util.Validator;
import com.sparklicorn.bucket.util.event.*;
import com.sparklicorn.bucket.tetris.util.structs.*;

// TODO #43 Design new feature: Multiplayer co-op
// Side-by-side boards, with light visual border between.
// Standard tetris rules apply.
// Players can place pieces anywhere.
// How to handle collisions?

//Implementation of Tetris that uses the 7-bag random piece generator.
//The number of rows and columns can be specified by the user.
public class TetrisGame implements ITetrisGame {
	public static final Validator<Integer> ROWS_VALIDATOR = new Validator<>(8, 200, "Rows");
	public static final Validator<Integer> COLS_VALIDATOR = new Validator<>(8, 200, "Columns");
	public static final Validator<Long> LEVEL_VALIDATOR = new Validator<>(0L, 255L, "Level");
	public static final int LINES_PER_LEVEL = 10;
	public static final List<Long> POINTS_BY_LINES_CLEARED = Collections.unmodifiableList(
		Arrays.asList(0L, 40L, 100L, 300L, 1200L)
	);

	/* ****************
	 * STATE AND STATS
	 ******************/
	protected TetrisState state;
	protected int numTimerPushbacks;

	/* ****************
	 * GAME COMPONENTS
	 ******************/
	protected EventBus eventBus;
	protected Timer gravityTimer;

	/**
	 * Creates a new Tetris game with standard number of rows and columns.
	 */
	public TetrisGame() {
		this(TetrisState.DEFAULT_NUM_ROWS, TetrisState.DEFAULT_NUM_COLS);
	}

	/**
	 * Creates a new Tetris game with the given number of rows and columns.
	 *
	 * @param numRows - Number of rows on the game board.
	 * @param numCols - Number of columns on the game board.
	 */
	public TetrisGame(int numRows, int numCols) {
		this(new TetrisState(numRows, numCols));
	}

	/**
	 * Creates a new Tetris game as a copy of another.
	 *
	 * NOTE: The new game will not have an event bus or gameloop timer.
	 */
	public TetrisGame(TetrisGame other) {
		this(other.state);
	}

	/**
	 * Creates a new Tetris game with a copy of the given state.
	 *
	 * @param state The state to copy.
	 */
	public TetrisGame(TetrisState state) {
		this.state = new TetrisState(state);
	}

	/**
	 * Returns whether the game is currently running.
	 */
	public boolean isRunning() {
		return state.hasStarted && !state.isGameOver;
	}

	/**
	 * Returns points rewarded for clearing lines at a given level.
	 *
	 * @param lines Number of lines cleared.
	 * @return Points to reward.
	 */
	protected long calcPointsForClearing(int lines) {
		return POINTS_BY_LINES_CLEARED.get(lines) * (state.level + 1L);
	}

	/**
	 * Attempts to rotate the current piece clockwise.
	 * The piece may be shifted left or right to accomodate the rotation.
	 *
	 * @param move The rotation to attempt.
	 * Should be either Move.CLOCKWISE or Move.COUNTERCLOCKWISE.
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	protected boolean rotate(Move move) {
		Move _move = state.validateRotation(move);

		if (_move.equals(Move.STAND)) {
			return false;
		}

		state.piece.move(_move);

		return true;
	}

	/**
	 * Attempts to shift the current piece with the given offset.
	 *
	 * @return True if the piece was successfully moved; otherwise false.
	 */
	//! NOTE: move.rotation is ignored
	protected boolean shiftPiece(Move move) {
		if (state.canPieceMove(move)) {
			state.piece.move(move);
			return true;
		}

		return false;
	}

	/**
	 * Plots the piece's block data to the board.
	 */
	protected void plotPiece() {
		state.placePiece();
		throwEvent(TetrisEvent.PIECE_PLACED);
		throwEvent(TetrisEvent.BLOCKS);
	}

	// TODO #44 Removes full rows from the board, shifting remaining rows down.
	protected void removeRows(int[] rows) {
		// TODO #44 implement and use below
	}

	/**
	 * Clears full lines and shift remaining blocks down.
	 *
	 * @return List of cleared rows, or null if no rows were cleared.
	 */
	protected List<Integer> clearLines() {
		List<Integer> fullRows = state.getFullRows();

		if (!fullRows.isEmpty()) {
			int numRowsToDrop = 1;
			int i = fullRows.size() - 1;
			//start above the last row in the clearing list
			for (int row = fullRows.get(i--) - 1; row > 0; row--) {
				//if this row is in the clearing list too...
				if (i >= 0 && row == fullRows.get(i)) {
					numRowsToDrop++;
					i--;
				} else {
					//Row 'row' needs to be shifted down.
					int k = row * state.cols; //starting index for blocks in row 'row'.

					//j = starting index for blocks in row 'row + numRowsToDrop'.
					//replace blocks in 'row + numRowsToDrop' with blocks in 'row'
					for (int j = (row + numRowsToDrop) * state.cols; j < (row + numRowsToDrop + 1) * state.cols; j++) {
						state.board[j] = state.board[k];
						state.board[k++] = 0;
					}
				}
			}
		}

		return (fullRows.isEmpty() ? null : fullRows);
	}

	/**
	 * Returns whether the game has gravity enabled.
	 */
	public boolean isGravityEnabled() {
		return gravityTimer != null;
	}

	/**
	 * Disables gravity if it is currently enabled.
	 */
	protected void disableGravity() {
		if (isGravityEnabled()) {
			this.gravityTimer.shutdownNow();
			this.gravityTimer = null;
		}
	}

	/**
	 * Enables gravity if it is currently disabled.
	 */
	protected void enableGravity() {
		if (!isGravityEnabled()) {
			gravityTimer = new Timer(this::gameloop, updateGravityTimerDelayMs(), TimeUnit.MILLISECONDS, true);
			if (state.hasStarted && !state.isGameOver && !state.isPaused) {
				gravityTimer.start();
			}
		}
	}

	/**
	 * Calculates and updates the amount of time between gravity ticks for the current level.
	 *
	 * @return The calculated delay (ms).
	 */
	protected long updateGravityTimerDelayMs() {
		long delay = Math.round((Math.pow(0.8 - (state.level) * 0.007, state.level)) * 1000.0);
		if (isGravityEnabled()) {
			gravityTimer.setDelay(delay, TimeUnit.MILLISECONDS);
		}
		return delay;
	}

	/**
	 * Attempts to clear full rows.
	 *
	 * @return True if any row was cleared; otherwise false.
	 */
	protected boolean attemptClearLines() {
		// TODO #44 refactor after clearLines() is refactored
		List<Integer> lines = clearLines();

		if (lines != null) {	//lines were cleared!
			state.linesCleared += lines.size();
			state.score += calcPointsForClearing(lines.size());
			state.linesUntilNextLevel -= lines.size();

			throwEvent(TetrisEvent.LINE_CLEAR);
			throwEvent(TetrisEvent.SCORE_UPDATE);
			throwEvent(TetrisEvent.BLOCKS);

			//check if we need to update level
			if (state.linesUntilNextLevel <= 0) { //level up!!
				increaseLevel();
			}
		}

		return lines != null;
	}

	/**
	 * Executes the game loop logic.
	 * If gravity is being used, this method is called automatically by the gravity timer.
	 * Otherwise, this method must be called manually.
	 */
	public void gameloop() {
		if (state.isGameOver || state.isPaused) {
			return;
		}

		numTimerPushbacks = 0;

		if (state.piece.isActive() && !state.canPieceMove(Move.DOWN)) {
			//*kerplunk*
			//next loop should attempt to clear lines
			plotPiece();
		} else if (!state.piece.isActive()) {	// The loop after piece kerplunks
			if (!attemptClearLines()) {
				nextPiece();
				if (checkGameOver()) {
					return;
				}
			}
		} else {	//piece alive && not at bottom
			shiftPiece(Move.DOWN);
			throwEvent(TetrisEvent.PIECE_SHIFT);
		}

		throwEvent(TetrisEvent.GAMELOOP);
	}

	/**
	 * Determines whether the active piece is overlapped with any other blocks,
	 * which is the lose condition. If detected, the gameOver handler is called.
	 *
	 * @return True if the game is over; otherwise false.
	 */
	protected boolean checkGameOver() {
		if (state.isGameOver) {
			return true;
		}

		if (state.pieceOverlapsBlocks()) {
			gameOver();
			return true;
		}

		return false;
	}

	@Override
	public synchronized void newGame() {
		reset();
		throwEvent(TetrisEvent.NEW_GAME);
	}

	/**
	 * Increases the level by 1, and updates the gravity timer delay.
	 */
	protected void increaseLevel() {
		state.level++;
		state.linesUntilNextLevel += LINES_PER_LEVEL;
		updateGravityTimerDelayMs();
		throwEvent(TetrisEvent.LEVEL_CHANGE);
	}

	/**
	 * Resets the game state.
	 */
	protected void reset() {
		state = new TetrisState(state.rows, state.cols);
		numTimerPushbacks = 0;
		throwEvent(TetrisEvent.RESET);
	}

	@Override
	public synchronized void start(long level, boolean useGravity) {
		if (state.hasStarted) {
			return;
		}

		state.level = LEVEL_VALIDATOR.confine(level);
		state.hasStarted = true;

		nextPiece();

		if (useGravity) {
			enableGravity();
			updateGravityTimerDelayMs();
		}

		throwEvent(TetrisEvent.START);
	}

	@Override
	public synchronized void stop() {
		state.isPaused = false;
		state.isGameOver = true;
		state.isClearingLines = false;
		state.placePiece();

		if (isGravityEnabled()) {
			gravityTimer.stop();
		}

		throwEvent(TetrisEvent.STOP);
	}

	@Override
	public synchronized void pause() {
		if (state.isGameOver || state.isPaused || !state.hasStarted) {
			return;
		}

		state.isPaused = true;

		if (isGravityEnabled()) {
			gravityTimer.stop();
		}

		throwEvent(TetrisEvent.PAUSE);
	}

	/**
	 * Ends the game.
	 * This method is called when the active piece overlaps with any other blocks.
	 * The gravity timer is stopped.
	 */
	protected void gameOver() {
		state.isGameOver = true;
		state.isPaused = false;
		state.isClearingLines = false;
		state.placePiece();

		if (isGravityEnabled()) {
			gravityTimer.stop();
		}

		throwEvent(TetrisEvent.GAME_OVER);
	}

	@Override
	public synchronized void resume() {
		if (state.hasStarted && !state.isGameOver) {
			state.isPaused = false;
			if (isGravityEnabled()) {
				gravityTimer.start();
			}
			throwEvent(TetrisEvent.RESUME);
		}
	}

	@Override
	public TetrisState getState() {
		return new TetrisState(state);
	}

	/**
	 * If the piece is active, rotation is attempted. If the piece cannot rotate in place,
	 * then it may be shifted left or right by a couple blocks to allow rotation.
	 * If the piece cannot rotate at all, then nothing happens.
	 *
	 * @param direction The direction to rotate.
	 * Must be either Move.CLOCKWISE or Move.COUNTERCLOCKWISE.
	 * @return True if the piece was rotated; otherwise false.
	 */
	private boolean handleRotation(Move direction) {
		if (!state.piece.isActive() || state.isPaused || state.isGameOver) {
			return false;
		}

		if (rotate(direction)) {
			// If next gravity tick will plop the piece, maybe rotation should delay that
			// a little to give the user time to make final adjustments.
			// This is an anti-frustration technique.
			if (!state.canPieceMove(Move.DOWN) && numTimerPushbacks < 4) {
				// if (gameTimer != null) {
				// 	gameTimer.resetTickDelay();
					// System.out.println(gameTimer.resetTickDelay());
				// }
				numTimerPushbacks++;
			}

			throwEvent(TetrisEvent.PIECE_ROTATE);
			return true;
		}

		return false;
	}

	@Override
	public synchronized boolean rotateClockwise() {
		return handleRotation(Move.CLOCKWISE);
	}

	@Override
	public synchronized boolean rotateCounterClockwise() {
		return handleRotation(Move.COUNTERCLOCKWISE);
	}

	@Override
	public synchronized boolean shift(int rowOffset, int colOffset) {
		Move move = new Move(new Coord(rowOffset, colOffset), 0);
		if (shiftPiece(move)) {
			if (!state.canPieceMove(Move.DOWN) && numTimerPushbacks < 4) {
				numTimerPushbacks++;
			}

			throwEvent(TetrisEvent.PIECE_SHIFT);
			return true;
		}

		return false;
	}

	/*********************
		Event Handling
	**********************/

	/**
	 * Registers an event listener for the given event.
	 * Initializes the event bus if it has not been initialized yet.
	 *
	 * @param event The event to listen for.
	 * @param listener The listener to register.
	 * @return True if the listener was registered; otherwise false.
	 */
	public boolean registerEventListener(TetrisEvent event, Consumer<Event> listener) {
		if (eventBus == null) {
			eventBus = new EventBus();
		}

		return eventBus.registerEventListener(event.name(), listener);
	}

	/**
	 * Unregisters an event listener for the given event.
	 * If the event bus has not been initialized, then nothing happens.
	 *
	 * @param event The event to unregister from.
	 * @param listener The listener to unregister.
	 * @return True if the listener was unregistered; otherwise false.
	 */
	public boolean unregisterEventListener(TetrisEvent event, Consumer<Event> listener) {
		if (eventBus == null) {
			return false;
		}

		return eventBus.unregisterEventListener(event.name(), listener);
	}

	/**
	 * Throws an event. Attaches the current game state as a property to the event.
	 * If the event bus has not been initialized, then nothing happens.
	 *
	 * @param event The event to throw.
	 */
	protected void throwEvent(TetrisEvent event) {
		if (eventBus == null) {
			return;
		}

		Event _event = new Event(event.name());
		_event.addProperty("state", getState());
		eventBus.throwEvent(_event);
	}

	/**
	 * Signals the game to stop and gracefully shut down.
	 */
	public void shutdown() {
		stop();

		if (eventBus != null) {
			eventBus.dispose(false);
		}

		if (isGravityEnabled()) {
			gravityTimer.shutdown();
		}
	}

	protected static record PQEntry<T>(T data, int priority) implements Comparable<PQEntry<T>> {
		@Override
		public int compareTo(PQEntry<T> o) {
			return (priority - o.priority);
		}
	}

	/**
	 * Helper to create a new search queue entry.
	 * The priority value is the sqrdist between given positions.
	 *
	 * @param newPosition Data value for the queue entry
	 * @param goalPosition Used to calculate the priority value.
	 * @return A new PQEntry with the given value and calculated priority.
	 */
	protected PQEntry<Position> positionSearchEntry(Position newPosition, Position goalPosition) {
		return new PQEntry<Position>(
			new Position(newPosition),
			newPosition.sqrdist(goalPosition)
		);
	}

	/**
	 * Resets the piece to the top of the board with the next shape.
	 */
	protected void nextPiece() {
		state.resetPiece();
		throwEvent(TetrisEvent.PIECE_CREATE);
	}
}
