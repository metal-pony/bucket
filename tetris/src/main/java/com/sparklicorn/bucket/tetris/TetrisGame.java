package com.sparklicorn.bucket.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sparklicorn.bucket.tetris.util.Timer;
import com.sparklicorn.bucket.util.Array;
import com.sparklicorn.bucket.util.PrioritySearchQueue;
import com.sparklicorn.bucket.util.event.*;
import com.sparklicorn.bucket.tetris.util.structs.*;
import com.sparklicorn.bucket.tetris.util.structs.Coord.FinalCoord;

// TODO Feature: Multiplayer co-op
// Side-by-side boards, with light visual border between.
// Standard tetris rules apply.
// Players can place pieces anywhere.
// How to handle collisions?

//Implementation of Tetris that uses the 7-bag random piece generator.
//The number of rows and columns can be specified by the user.
public class TetrisGame implements ITetrisGame {
	public static final int DEFAULT_NUM_ROWS = 20;
	public static final int DEFAULT_NUM_COLS = 10;
	public static final int DEFAULT_ENTRY_COLUMN = calcEntryColumn(DEFAULT_NUM_COLS);
	public static final Coord DEFAULT_ENTRY_COORD = new FinalCoord(1, DEFAULT_ENTRY_COLUMN);

	public static final int MINIMUM_ROWS = 8;
	public static final int MAXIMUM_ROWS = 200;

	public static final int MINIMUM_COLS = 8;
	public static final int MAXIMUM_COLS = 200;

	public static final List<Long> POINTS_BY_LINES_CLEARED = Collections.unmodifiableList(
		Arrays.asList(0L, 40L, 100L, 300L, 1200L)
	);

	public static final long MIN_LEVEL = 0L;
	public static final long MAX_LEVEL = 255L;
	public static final int LINES_PER_LEVEL = 10;

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
	protected int[] board;

	// TODO keep track of full rows as pieces are placed
	protected boolean[] fullRows;

	protected boolean isGameOver, isPaused, isClearingLines, hasStarted;
	protected long level, score, linesCleared, numPiecesDropped;
	protected int rows, cols, linesUntilNextLevel;
	protected long[] dist;

	protected int numTimerPushbacks;

	protected ShapeQueue nextShapes;

	/* ****************
	 * GAME COMPONENTS
	 ******************/
	protected EventBus eventBus;
	protected Timer gameTimer;

	protected Shape shape;
	protected Position position;
	protected Coord[] blockLocations;
	protected boolean isActive;

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
		if (numRows < MINIMUM_ROWS || numRows > MAXIMUM_ROWS) {
			numRows = DEFAULT_NUM_ROWS;
		}

		if (numCols < MINIMUM_COLS || numCols > MAXIMUM_COLS) {
			numCols = DEFAULT_NUM_COLS;
		}

		rows = numRows;
		cols = numCols;

		nextShapes = new ShapeQueue();
		eventBus = new EventBus();
		if (useGameloopTimer) {
			gameTimer = new Timer(this::gameloop, 1L, TimeUnit.SECONDS, true);
		}
		numTimerPushbacks = 0;

		blockLocations = new Coord[4];
		for (int i = 0; i < blockLocations.length; i++) {
			blockLocations[i] = new Coord(DEFAULT_ENTRY_COORD);
		}

		newGame();
	}

	/**
	 * Creates a new Tetris game as a copy of another.
	 *
	 * NOTE: The new game will not have an event bus or gameloop timer.
	 */
	public TetrisGame(TetrisGame other) {
		rows = other.rows;
		cols = other.cols;
		board = Array.copy(other.board);
		level = other.level;
		linesCleared = other.linesCleared;
		linesUntilNextLevel = other.linesUntilNextLevel;
		score = other.score;
		dist = Array.copy(other.dist);
		hasStarted = other.hasStarted;
		isGameOver = other.isGameOver;
		isClearingLines = other.isClearingLines;
		isPaused = other.isPaused;
		numPiecesDropped = other.numPiecesDropped;
		shape = other.shape;
		nextShapes = new ShapeQueue(other.nextShapes);
		position = new Position(other.position);
		blockLocations = Coord.copyFrom(other.blockLocations);
		isActive = other.isActive;
	}

	/**
	 * Returns points rewarded for clearing lines at a given level.
	 *
	 * @param lines Number of lines cleared.
	 * @param level Current level.
	 * @return Points to reward.
	 */
	protected long calcPointsForClearing(int lines) {
		return POINTS_BY_LINES_CLEARED.get(lines) * (level + 1L);
	}

	/**
	 * Calculates the coordinates of blocks that make up the current shape,
	 * in the given position.
	 *
	 * @param blockCoords Contains block coordinates that make up the shape.
	 * The newly calculated block positions will be written here.
	 * @param position The location and rotation of the shape to calculate block positions for.
	 * @return The modified blockCoords array (for convenience).
	 */
	public Coord[] populateBlockPositions(Coord[] blockCoords, Position position) {
		int rotationIndex = shape.rotationIndex(position.rotation());
		for (int i = 0; i < blockCoords.length; i++) {
			blockCoords[i].set(position.offset());
			blockCoords[i].add(shape.rotationOffsets[rotationIndex][i]);
		}

		return blockCoords;
	}

	/**
	 * Checks whether the current piece can move with the given rotation.
	 * If the piece cannot be rotated in place, it will check whether it can be shifted first.
	 *
	 * If shifting doesn't allow rotation, then this returns an adjusted move of STAND, indicating
	 * the rotation is not possible.
	 *
	 * @param move CLOCKWISE or COUNTERCLOCKWISE
	 * @return A Move representing the rotation, it may be adjusted to include a left
	 * or right shift that is required to accomodate the rotation.
	 * If the rotation is not possible, returns Move.STAND.
	 */
	protected Move validateRotation(Move move) {
		if (!(move.equals(Move.CLOCKWISE) || move.equals(Move.COUNTERCLOCKWISE))) {
			return new Move(Move.STAND);
		}

		if (canPieceMove(move)) {
			return new Move(move);
		}

		// Attempt to "kick off" an edge or block if rotating too close.
		Move kickLeft = new Move(move);
		Move kickRight = new Move(move);
		for (int colOffset = 1; colOffset < 3; colOffset++) {
			if (canPieceMove(kickLeft.add(Move.LEFT))) {
				return kickLeft;
			}

			if (canPieceMove(kickRight.add(Move.RIGHT))) {
				return kickRight;
			}
		}

		return new Move(Move.STAND);
	}

	/**
	 * Checks whether the given coordinates are within bounds of the board.
	 */
	protected boolean validateBlockPosition(Coord coords) {
		return (
			coords.row() >= 0 && coords.row() < rows &&
			coords.col() >= 0 && coords.col() < cols
		);
	}

	/**
	 * ! it's more like validation of the resulting location and does not imply that a path exists to it unless the
	 * ! magnitude of the move is (exclusively) one unit of rotation or of offset.
	 * ! however you wanna fit that into a name... is great
	 * Checks that the given move is in bounds of the board, that there are no blocks occupying
	 * the spaces, and that the resulting position does not end up higher on the board.
	 *
	 * @param move
	 * @return True if the active piece can move to the specified position; otherwise false.
	 */
	protected boolean canPieceMove(Move move) {
		if (move.rowOffset() < 0) {
			return false;
		}

		return isPositionValid(new Position(position).add(move));
	}

	protected boolean isPositionValid(Position position) {
		Coord[] newBlockCoords = populateBlockPositions(
			Coord.copyFrom(blockLocations),
			position
		);
		int minCol = cols - 1;
		int maxCol = 0;

		for (Coord c : newBlockCoords) {
			minCol = Math.min(minCol, c.col());
			maxCol = Math.max(maxCol, c.col());

			if (
				!validateBlockPosition(c) ||
				!isCellEmpty(c) ||

				// A large gap between cell columns means the piece wrapped around the board.
				(maxCol - minCol) > 4
			) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Attempts to rotate the current piece clockwise.
	 * The piece may be shifted left or right to accomodate the rotation.
	 *
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	protected boolean rotate(Move move) {
		Move _move = validateRotation(move);

		if (_move.equals(Move.STAND)) {
			return false;
		}

		position.add(_move);
		populateBlockPositions(blockLocations, position);

		return true;
	}

	/**
	 * Attempts to shift the current piece with the given offset.
	 *
	 * @return True if the piece was successfully moved; otherwise false.
	 */
	//! NOTE: move.rotation is ignored
	protected boolean shiftPiece(Move move) {
		if (canPieceMove(move)) {
			position.add(move);
			populateBlockPositions(blockLocations, position);
			return true;
		}

		return false;
	}

	/**
	 * Plots the piece's block data to the board.
	 */
	protected void plotPiece() {
		for (Coord c : blockLocations) {
			board[c.row() * cols + c.col()] = shape.value;
		}
		throwEvent(TetrisEvent.PIECE_PLACED);
		throwEvent(TetrisEvent.BLOCKS);
	}

	/**
	 * Gets whether the cell is empty at the given coordinates.
	 */
	protected boolean isCellEmpty(Coord location) {
		return !containsBlock(location);
	}

	/**
	 * Gets whether the cell is empty at the given coordinates.
	 */
	protected boolean isCellEmpty(int row, int col) {
		return board[row * cols + col] == 0;
	}

	private boolean containsBlock(Coord location) {
		return board[location.row() * cols + location.col()] != 0;
	}

	// TODO Removes full rows from the board, shifting remaining rows down.
	protected void removeRows(int[] rows) {
		// TODO implement and use below
	}

	/**
	 * Clears full lines and shift remaining blocks down.
	 *
	 * @return List of cleared rows, or null if no rows were cleared.
	 */
	protected List<Integer> clearLines() {
		List<Integer> fullRows = getFullRows();

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
					int k = row * cols; //starting index for blocks in row 'row'.

					//j = starting index for blocks in row 'row + numRowsToDrop'.
					//replace blocks in 'row + numRowsToDrop' with blocks in 'row'
					for (int j = (row + numRowsToDrop) * cols; j < (row + numRowsToDrop + 1) * cols; j++) {
						board[j] = board[k];
						board[k++] = 0;
					}
				}
			}
		}

		return (fullRows.isEmpty() ? null : fullRows);
	}

	/**
	 * Determines whether the given row is full of blocks.
	 */
	protected boolean isRowFull(int row) {
		for (int col = 0; col < cols; col++) {
			if (isCellEmpty(row, col)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns a list of rows that are full of blocks, in ascending order.
	 */
	//todo Should only need to test rows that have changed since the last check.
	protected List<Integer> getFullRows() {
		List<Integer> lines = new ArrayList<>();

		for (int row = 0; row < rows; row++) {
			if (isRowFull(row)) {
				lines.add(row);
			}
		}

		return lines;
	}

	/**
	 * Determines if the piece is sharing the same cell as any other block.
	 *
	 * @return True if the piece is overlapping with other blocks.
	 */
	protected boolean intersects(Coord[] locations) {
		return Stream.of(locations).anyMatch(this::containsBlock);
	}

	/**
	 * Removes the timer component. This can only be done if the game is over or has
	 * not yet been started.
	 *
	 * @return True if the timer was removed; otherwise false.
	 */
	protected boolean withoutTimer() {
		if (!isGameOver()) {
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
		if (!isGameOver()) {
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
		// TODO refactor after clearLines() is refactored
		List<Integer> lines = clearLines();

		if (lines != null) {	//lines were cleared!
			linesCleared += lines.size();
			score += calcPointsForClearing(lines.size());
			linesUntilNextLevel -= lines.size();

			throwEvent(TetrisEvent.LINE_CLEAR);
			throwEvent(TetrisEvent.SCORE_UPDATE);
			throwEvent(TetrisEvent.BLOCKS);

			//check if we need to update level
			if (linesUntilNextLevel <= 0) { //level up!!
				increaseLevel();
			}
		}

		return lines != null;
	}

	protected synchronized void gameloop() {
		if (isGameOver || isPaused) {
			return;
		}

		numTimerPushbacks = 0;

		if (isActive && !canPieceMove(Move.DOWN)) {
			//*kerplunk*
			//next loop should attempt to clear lines
			plotPiece();
			isActive = false;
			numPiecesDropped++;

		} else if (!isActive) {	// The loop after piece kerplunks
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
		if (isGameOver) {
			return true;
		}

		if (intersects(blockLocations)) {
			gameOver();
			return true;
		}

		return false;
	}

	@Override public long getNumPiecesDropped() {
		return numPiecesDropped;
	}

	@Override public synchronized void newGame() {
		reset();
		throwEvent(TetrisEvent.NEW_GAME);
	}

	protected void increaseLevel() {
		level++;
		linesUntilNextLevel += LINES_PER_LEVEL;
		if (gameTimer != null) {
			gameTimer.setDelay(getTimerDelay(), TimeUnit.MILLISECONDS);
			//loopTickNanos = TimeUnit.MILLISECONDS.toNanos(
			//		Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0));
		}
		throwEvent(TetrisEvent.LEVEL_CHANGE);
	}

	//Returns the amount of time between gameloop ticks for the current level.
	protected long getTimerDelay() {
		return Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0);
	}

	protected void reset() {
		hasStarted = false;
		isGameOver = false;
		isClearingLines = false;
		isPaused = false;

		level = MIN_LEVEL;
		linesCleared = 0;
		score = 0;
		numPiecesDropped = 0;
		numTimerPushbacks = 0;
		linesUntilNextLevel = LINES_PER_LEVEL;
		dist = new long[NUM_SHAPES];

		//This will cause the queue to re-populate next time poll() is called.
		nextShapes.clear();

		board = new int[rows * cols];

		nextPiece();
	}

	@Override public synchronized void start(long level) {
		if (!hasStarted) {
			reset();
			this.level = (level >= 0) ? level : MIN_LEVEL;
			this.level = (level <= MAX_LEVEL) ? level : MAX_LEVEL;
			hasStarted = true;

			if (gameTimer != null) {
				gameTimer.setDelay(
					Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0),
					TimeUnit.MILLISECONDS
				);
				gameTimer.start();
			}

			throwEvent(TetrisEvent.START);
		}
	}

	@Override public synchronized void stop() {
		isPaused = false;
		isGameOver = true;
		isClearingLines = false;
		if (gameTimer != null) {
			gameTimer.stop();
		}
		throwEvent(TetrisEvent.STOP);
	}

	@Override public synchronized void pause() {
		if (hasStarted && !isGameOver) {
			isPaused = true;
			if (gameTimer != null) {
				gameTimer.stop();
			}
			throwEvent(TetrisEvent.PAUSE);
		}
	}

	protected void gameOver() {
		isGameOver = true;
		isPaused = false;
		isClearingLines = false;
		if (gameTimer != null) {
			gameTimer.stop();
		}
		throwEvent(TetrisEvent.GAME_OVER);
	}

	@Override public synchronized void resume() {
		if (hasStarted && !isGameOver) {
			isPaused = false;
			if (gameTimer != null) {
				gameTimer.start();
			}
			throwEvent(TetrisEvent.RESUME);
		}
	}

	// A bunch of Getters

	@Override public int[] getBlocksOnBoard() {
		return Array.copy(board);
	}
	@Override public int[] getBlocksOnBoard(int[] arr) {
		if (arr == null || arr.length != rows * cols) {
			return getBlocksOnBoard();
		}
		System.arraycopy(board, 0, arr, 0, board.length);
		return arr;
	}
	@Override public boolean isGameOver() 			{return isGameOver;}
	@Override public boolean hasStarted() 			{return hasStarted;}
	@Override public boolean isPaused() 			{return isPaused;}
	@Override public boolean isClearingLines() 		{return isClearingLines;}
	@Override public long getLevel() 				{return level;}
	@Override public long getScore() 				{return score;}
	@Override public long getLinesCleared() 		{return linesCleared;}
	@Override public int getLinesUntilNextLevel() 	{return linesUntilNextLevel;}
	@Override public int getNumRows() 				{return rows;}
	@Override public int getNumCols() 				{return cols;}
	@Override public Coord[] getPieceBlocks() 		{return Coord.copyFrom(blockLocations);}
	@Override public Shape getNextShape() 			{return nextShapes.peek();}
	@Override public Shape getCurrentShape() 		{return shape;}
	@Override public Coord getLocation() 			{return new Coord(position.location());}
	@Override public long[] getDistribution() 		{return Array.copy(dist);}

	@Override public boolean isPieceActive() {
		return (hasStarted && !isGameOver && !isClearingLines && !isPaused && isActive);
	}

	private boolean handleRotation(Move direction) {
		if (!isActive) {
			return false;
		}

		if (rotate(direction)) {
			// If next gravity tick will plop the piece, maybe rotation should delay that
			// a little to give the user time to make final adjustments.
			// This is an anti-frustration technique.
			if (!canPieceMove(Move.DOWN) && numTimerPushbacks < 4) {
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

	@Override public synchronized boolean rotateClockwise() {
		return handleRotation(Move.CLOCKWISE);
	}

	@Override public synchronized boolean rotateCounterClockwise() {
		return handleRotation(Move.COUNTERCLOCKWISE);
	}

	@Override public synchronized boolean shift(int rowOffset, int colOffset) {
		Move move = new Move(new Coord(rowOffset, colOffset), 0);
		if (shiftPiece(move)) {
			if (!canPieceMove(Move.DOWN) && numTimerPushbacks < 4) {
				numTimerPushbacks++;
			}

			throwEvent(TetrisEvent.PIECE_SHIFT);
			return true;
		}

		return false;
	}

	////////////////////////
	//Event related methods
	////////////////////////

	@Override
	public boolean registerEventListener(TetrisEvent event, Consumer<Event> listener) {
		if (eventBus == null) {
			return false;
		}

		return eventBus.registerEventListener(event.name(), listener);
	}

	@Override
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

		eventBus.throwEvent(new Event(event.name()));
	}

	@Override public void shutdown() {
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
	 * Determines whether there is a path of legal moves between the
	 * current piece position and the given goal position.
	 * Uses a breadth-first search of possible moves that result in the goal position,
	 * optimized to prefer movements closer to the goal.
	 *
	 * @param goalPosition Goal position to check whether there is a path to.
	 * @return True if a path exists; otherwise false.
	 */
	protected boolean doesPathExist(Position goalPosition) {
		// Fail early if either:
		// - board contains blocks at the piece's location
		// - board contains blocks at the goal location
		// - goal is positioned somewhere above the current piece (moving UP is illegal)
		final int goalPositionRow = goalPosition.location().row();
		Coord[] toPositionBlocks = populateBlockPositions(Coord.copyFrom(blockLocations), goalPosition);
		if (
			intersects(blockLocations) ||
			intersects(toPositionBlocks) ||
			goalPositionRow < position.location().row()
		) {
			return false;
		}

		// Determines whether a given entry should be accepted into the queue.
		Function<PQEntry<Position>,Boolean> acceptanceCriteria = (offered) -> (
			offered.data().location().row() <= goalPositionRow && canPieceMove(offered.data())
		);
		PrioritySearchQueue<PQEntry<Position>> searchQueue = new PrioritySearchQueue<>(acceptanceCriteria);
		searchQueue.offer(positionSearchEntry(position, goalPosition));

		while (!searchQueue.isEmpty()) {
			Position currentPosition = searchQueue.poll().data();

			if (currentPosition.equals(goalPosition)) {
				return true;
			}

			for (Move move : ATOMIC_MOVES) {
				Position nextPosition = new Position(currentPosition).add(move);
				searchQueue.offer(positionSearchEntry(nextPosition, goalPosition));
			}
		}

		return false;
	}

	/**
	 * Resets this piece to the specified shape, location, and rotation.
	 *
	 * @param newShape - The new shape this should take.
	 * @param newLocation - The location to reset to.
	 * @param rotationIndex - The rotation of this piece.
	 */
	protected void nextPiece() {
		shape = nextShapes.poll();
		position = new Position(
			new Coord(1, calcEntryColumn(cols)),
			0,
			shape.getNumRotations()
		);
		isActive = true;
		populateBlockPositions(blockLocations, position);
		throwEvent(TetrisEvent.PIECE_CREATE);
	}
}
