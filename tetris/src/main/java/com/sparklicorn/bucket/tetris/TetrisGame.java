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
import com.sparklicorn.bucket.tetris.util.structs.Coord.FinalCoord;

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
	public static final int DEFAULT_NUM_ROWS = 20;
	public static final int DEFAULT_NUM_COLS = 10;
	public static final int LINES_PER_LEVEL = 10;
	public static final int DEFAULT_ENTRY_COLUMN = calcEntryColumn(DEFAULT_NUM_COLS);
	public static final Coord DEFAULT_ENTRY_COORD = new FinalCoord(1, DEFAULT_ENTRY_COLUMN);
	public static final List<Long> POINTS_BY_LINES_CLEARED = Collections.unmodifiableList(
		Arrays.asList(0L, 40L, 100L, 300L, 1200L)
	);

	/**
	 * Calculates the column where pieces appear, which is the center column,
	 * or just left of center if there are an even number of columns.
	 *
	 * @param cols Number of columns on the board.
	 */
	protected static int calcEntryColumn(int cols) {
		return (cols / 2) - ((cols % 2 == 0) ? 1 : 0);
	}

	/* ****************
	 * STATE AND STATS
	 ******************/
	protected TetrisState state;
	protected int numTimerPushbacks;

	/* ****************
	 * GAME COMPONENTS
	 ******************/
	protected EventBus eventBus;
	protected Timer gameTimer;

	/**
	 * Creates a new Tetris game with standard number of rows and columns.
	 */
	public TetrisGame() {
		this(DEFAULT_NUM_ROWS, DEFAULT_NUM_COLS);
	}

	/**
	 * Creates a new Tetris game with the given number of rows and columns.
	 *
	 * @param numRows - Number of rows on the game board.
	 * @param numCols - Number of columns on the game board.
	 */
	public TetrisGame(int numRows, int numCols) {
		this(numRows, numCols, true);
	}

	/**
	 * Creates a new Tetris game with the given number of rows, columns,
	 * and whether to initialize a timer to control the gameloop.
	 *
	 * @param numRows - Number of rows on the game board.
	 * @param numCols - Number of columns on the game board.
	 * @param useGameloopTimer - Whether the game instance will use a gameloop timer.
	 */
	public TetrisGame(int numRows, int numCols, boolean useGameloopTimer) {
		this(new TetrisState(numRows, numCols));

		if (useGameloopTimer) {
			gameTimer = new Timer(this::gameloop, 1L, TimeUnit.SECONDS, true);
			numTimerPushbacks = 0;
		}
	}

	public TetrisGame(TetrisState state) {
		this.state = new TetrisState(state);
	}

	/**
	 * Creates a new Tetris game as a copy of another.
	 *
	 * NOTE: The new game will not have an event bus or gameloop timer.
	 */
	public TetrisGame(TetrisGame other) {
		state = new TetrisState(other.state);

		if (other.gameTimer != null) {
			gameTimer = new Timer(this::gameloop, 1L, TimeUnit.SECONDS, true);
			numTimerPushbacks = other.numTimerPushbacks;
		}
	}

	/**
	 * Returns points rewarded for clearing lines at a given level.
	 *
	 * @param lines Number of lines cleared.
	 * @param level Current level.
	 * @return Points to reward.
	 */
	protected long calcPointsForClearing(int lines) {
		return POINTS_BY_LINES_CLEARED.get(lines) * (state.level + 1L);
	}

	/**
	 * Calculates the coordinates of blocks that make up the current shape,
	 * in the given position.
	 *
	 * @param coords Contains block coordinates that make up the shape.
	 * The newly calculated block positions will be written here.
	 * @param pos The location and rotation of the shape to calculate block positions for.
	 * @return The modified blockCoords array (for convenience).
	 */
	public Coord[] populateBlockPositions(Coord[] coords, Position pos) {
		int rotationIndex = state.shape.rotationIndex(pos.rotation());
		for (int i = 0; i < coords.length; i++) {
			coords[i].set(pos.offset());
			coords[i].add(state.shape.rotationOffsets[rotationIndex][i]);
		}

		return coords;
	}

	public Coord[] populateBlockPositions(Position move) {
		Coord[] coords = Coord.copyFrom(state.blockLocations);
		int rotationIndex = state.shape.rotationIndex(move.rotation());
		for (int i = 0; i < state.blockLocations.length; i++) {
			coords[i].set(move.offset());
			coords[i].add(state.shape.rotationOffsets[rotationIndex][i]);
		}

		return coords;
	}

	/**
	 * Attempts to rotate the current piece clockwise.
	 * The piece may be shifted left or right to accomodate the rotation.
	 *
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	protected boolean rotate(Move move) {
		Move _move = state.validateRotation(move);

		if (_move.equals(Move.STAND)) {
			return false;
		}

		state.movePiece(_move);

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
			state.movePiece(move);
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
	 * Removes the timer component. This can only be done if the game is over or has
	 * not yet been started.
	 *
	 * @return True if the timer was removed; otherwise false.
	 */
	protected boolean withoutTimer() {
		if (!state.isGameOver) {
			return false;
		}

		this.gameTimer.shutdown();
		this.gameTimer = null;

		return true;
	}

	/**
	 * Adds a timer component for the gameloop. This can only be done if the game
	 * is over or has not yet been started.
	 *
	 * @return True if the timer was added (or already exists); otherwise false.
	 */
	protected boolean withTimer() {
		if (!state.isGameOver) {
			return false;
		}

		if (this.gameTimer == null) {
			this.gameTimer = new Timer(this::gameloop, 1L, TimeUnit.SECONDS, true);
		}

		return true;
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

	protected synchronized void gameloop() {
		if (state.isGameOver || state.isPaused) {
			return;
		}

		numTimerPushbacks = 0;

		if (state.isActive && !state.canPieceMove(Move.DOWN)) {
			//*kerplunk*
			//next loop should attempt to clear lines
			plotPiece();
			state.position = null;
			state.isActive = false;
			state.numPiecesDropped++;

		} else if (!state.isActive) {	// The loop after piece kerplunks
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

		if (state.intersects(state.blockLocations)) {
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

	protected void increaseLevel() {
		state.level++;
		state.linesUntilNextLevel += LINES_PER_LEVEL;
		if (gameTimer != null) {
			gameTimer.setDelay(getTimerDelay(), TimeUnit.MILLISECONDS);
			//loopTickNanos = TimeUnit.MILLISECONDS.toNanos(
			//		Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0));
		}
		throwEvent(TetrisEvent.LEVEL_CHANGE);
	}

	//Returns the amount of time between gameloop ticks for the current level.
	protected long getTimerDelay() {
		return Math.round((Math.pow(0.8 - (state.level) * 0.007, state.level)) * 1000.0);
	}

	protected void reset() {
		state = new TetrisState(state.rows, state.cols);
		numTimerPushbacks = 0;
	}

	@Override
	public synchronized void start(long level) {
		if (state.hasStarted) {
			return;
		}

		state.level = LEVEL_VALIDATOR.confine(level);
		state.hasStarted = true;

		throwEvent(TetrisEvent.START);
		nextPiece();

		if (gameTimer != null) {
			gameTimer.setDelay(
				Math.round((Math.pow(0.8 - (state.level) * 0.007, state.level)) * 1000.0),
				TimeUnit.MILLISECONDS
			);
			gameTimer.start();
		}
	}

	@Override
	public synchronized void stop() {
		state.isPaused = false;
		state.isGameOver = true;
		state.isClearingLines = false;
		state.isActive = false;

		if (gameTimer != null) {
			gameTimer.stop();
		}

		throwEvent(TetrisEvent.STOP);
	}

	@Override
	public synchronized void pause() {
		if (state.isGameOver || state.isPaused || !state.hasStarted) {
			return;
		}

		state.isPaused = true;

		if (gameTimer != null) {
			gameTimer.stop();
		}

		throwEvent(TetrisEvent.PAUSE);
	}

	protected void gameOver() {
		state.isGameOver = true;
		state.isPaused = false;
		state.isClearingLines = false;
		state.isActive = false;

		if (gameTimer != null) {
			gameTimer.stop();
		}

		throwEvent(TetrisEvent.GAME_OVER);
	}

	@Override
	public synchronized void resume() {
		if (state.hasStarted && !state.isGameOver) {
			state.isPaused = false;
			if (gameTimer != null) {
				gameTimer.start();
			}
			throwEvent(TetrisEvent.RESUME);
		}
	}

	public TetrisState getState() {
		return new TetrisState(state);
	}

	// TODO isClearingLines isn't being tracked correctly

	private boolean handleRotation(Move direction) {
		if (!state.isActive) {
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

	public boolean registerEventListener(TetrisEvent event, Consumer<Event> listener) {
		if (eventBus == null) {
			eventBus = new EventBus();
		}

		return eventBus.registerEventListener(event.name(), listener);
	}

	public boolean unregisterEventListener(TetrisEvent event, Consumer<Event> listener) {
		if (eventBus == null) {
			return false;
		}

		return eventBus.unregisterEventListener(event.name(), listener);
	}

	protected void throwEvent(TetrisEvent event) {
		if (eventBus == null) {
			return;
		}

		Event _event = new Event(event.name());
		_event.addProperty("state", getState());
		eventBus.throwEvent(_event);
	}

	public void shutdown() {
		stop();

		if (eventBus != null) {
			eventBus.dispose(false);
		}

		if (gameTimer != null) {
			gameTimer.shutdown();
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

	protected static final Move[] ATOMIC_MOVES = new Move[] {
		Move.DOWN,
		Move.LEFT,
		Move.RIGHT,
		Move.CLOCKWISE,
		Move.COUNTERCLOCKWISE
	};

	/**
	 * TODO This isn't used anywhere yet, so it may be removed at a later date.
	 * Determines whether there is a path of legal moves between the
	 * current piece position and the given goal position.
	 * Uses a breadth-first search of possible moves that result in the goal position,
	 * optimized to prefer movements closer to the goal.
	 *
	 * @param goalPosition Goal position to check whether there is a path to.
	 * @return True if a path exists; otherwise false.
	 */
	// protected boolean doesPathExist(Position goalPosition) {
	// 	// Fail early if either:
	// 	// - board contains blocks at the piece's location
	// 	// - board contains blocks at the goal location
	// 	// - goal is positioned somewhere above the current piece (moving UP is illegal)
	// 	final int goalPositionRow = goalPosition.location().row();
	// 	Coord[] toPositionBlocks = populateBlockPositions(goalPosition);
	// 	if (
	// 		intersects(state.blockLocations) ||
	// 		intersects(toPositionBlocks) ||
	// 		goalPositionRow < state.position.location().row()
	// 	) {
	// 		return false;
	// 	}

	// 	// Determines whether a given entry should be accepted into the queue.
	// 	Function<PQEntry<Position>,Boolean> acceptanceCriteria = (offered) -> (
	// 		offered.data().location().row() <= goalPositionRow && canPieceMove(offered.data())
	// 	);
	// 	PrioritySearchQueue<PQEntry<Position>> searchQueue = new PrioritySearchQueue<>(acceptanceCriteria);
	// 	searchQueue.offer(positionSearchEntry(state.position, goalPosition));

	// 	while (!searchQueue.isEmpty()) {
	// 		Position currentPosition = searchQueue.poll().data();

	// 		if (currentPosition.equals(goalPosition)) {
	// 			return true;
	// 		}

	// 		for (Move move : ATOMIC_MOVES) {
	// 			Position nextPosition = new Position(currentPosition).add(move);
	// 			searchQueue.offer(positionSearchEntry(nextPosition, goalPosition));
	// 		}
	// 	}

	// 	return false;
	// }

	/**
	 * Resets this piece to the specified shape, location, and rotation.
	 *
	 * @param newShape - The new shape this should take.
	 * @param newLocation - The location to reset to.
	 * @param rotationIndex - The rotation of this piece.
	 */
	protected void nextPiece() {
		state.shape = state.nextShapes.poll();
		state.position = new Position(
			new Coord(1, calcEntryColumn(state.cols)),
			0,
			state.shape.getNumRotations()
		);
		state.updateBlockPositions();
		state.isActive = true;
		throwEvent(TetrisEvent.PIECE_CREATE);
	}
}
