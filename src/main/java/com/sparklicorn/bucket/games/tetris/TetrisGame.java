package com.sparklicorn.bucket.games.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sparklicorn.bucket.games.tetris.util.Timer;
import com.sparklicorn.bucket.util.PriorityQueue;
import com.sparklicorn.bucket.util.event.*;
import com.sparklicorn.bucket.games.tetris.util.structs.*;
import com.sparklicorn.bucket.games.tetris.util.structs.Coord.FinalCoord;

// TODO Feature: Multiplayer co-op
// Side-by-side boards, with light visual border between.
// Standard tetris rules apply.
// Players can place pieces anywhere.
// How to handle collisions?

// TODO Feature: Dynamic entry point option
// Default entry point for first piece.
// Each subsequent piece will enter row 1, at column of last placed piece.
// May be useful for multiplayer

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
		Arrays.asList(new Long[]{ 0L, 40L, 100L, 300L, 1200L })
	);

	public static final long MIN_LEVEL = 0L;
	public static final long MAX_LEVEL = 255L;
	public static final int LINES_PER_LEVEL = 10;

	/* ****************
	 * STATE AND STATS
	 ******************/
	protected int[] board;
	protected boolean isGameOver, isPaused, isClearingLines, hasStarted;
	protected long level, score, linesCleared, numPiecesDropped;
	protected int rows, cols, linesUntilNextLevel;
	protected Coord entryPoint;
	protected long[] dist;

	protected int numTimerPushbacks;

	protected ShapeQueue nextShapes;

	/* ****************
	 * GAME COMPONENTS
	 ******************/
	// protected Board board;
	// protected Tetromino piece;
	protected EventBus eventBus;
	protected Timer gameTimer;

	// TODO incorporate piece state from Tetromino
	private Shape shape;
	private Coord location, blockLocations[];
	private int rotationIndex;
	private boolean isActive;

	/**
	 * Returns points rewarded for clearing lines at a given level.
	 *
	 * @param lines Number of lines cleared.
	 * @param level Current level.
	 * @return Points to reward.
	 */
	private long calcPointsForClearing(int lines) {
		return POINTS_BY_LINES_CLEARED.get(lines) * (level + 1L);
	}

	private static int calcEntryColumn(int cols) {
		return (cols / 2) - ((cols % 2 == 0) ? 1 : 0);
	}

	/**
	 * Determines whether the current piece can rotate clockwise.
	 *
	 * @return An integer representing how many blocks to shift the piece to be able to rotate,
	 * or null if the piece cannot rotate at all.
	 */
	public Integer canPieceRotateClockwise() {
		return canRotate(Move.CLOCKWISE);
	}

	/**
	 * Determines whether the current piece can rotate counter-clockwise.
	 *
	 * @return An integer representing how many blocks to shift the piece to be able to rotate,
	 * or null if the piece cannot rotate at all.
	 */
	//todo RENAME METHOD - The answer to the question "can...something something"
	// Should never be "7".
	// How about "whatOffsetCanPieceRotateCounterClockwiseWith()"
	public Integer canPieceRotateCounterClockwise() {
		return canRotate(Move.COUNTERCLOCKWISE);
	}

	private Integer canRotate(Move move) {
		if (move != Move.CLOCKWISE && move != Move.COUNTERCLOCKWISE) {
			return null;
		}

		if (canPieceMove(move)) {
			return 0;
		}

		// Attempt to "kick off" an edge or block if rotating too close.
		Move kickLeft = new Move(move);
		Move kickRight = new Move(move);
		for (int colOffset = 1; colOffset < 3; colOffset++) {
			kickLeft.offset().add(Move.LEFT.offset());
			kickRight.offset().add(Move.RIGHT.offset());

			if (canPieceMove(kickLeft)) {
				return colOffset;
			} else if (canPieceMove(kickRight)) {
				return -colOffset;
			}
		}

		return null;
	}

	private boolean validateBlockPosition(Coord coords) {
		return (
			coords.row() >= 0 && coords.row() < rows &&
			coords.col() >= 0 && coords.col() < cols
		);
	}

	/**
	 * Determines if the piece can move with the given offset and rotation.
	 *
	 * @param offset - Offset coordinates relative to the current piece.
	 * @param rotationOffset - Offset applied to the current piece's rotation index.
	 * <code>0</code> for no rotation.
	 * @return True if the piece can move to the specified position; otherwise false.
	 */
	public boolean canPieceMove(Move move) {
		if (!isActive || move.rowOffset() < 0) {
			return false;
		}

		Coord newBlockCoords[] = getNewPositions(move);
		// int s = newBlockCoords[0].col(); //column of last checked position

		int minCol = cols - 1;
		int maxCol = 0;

		for (Coord c : newBlockCoords) {
			minCol = Math.min(minCol, c.col());
			maxCol = Math.max(maxCol, c.col());

			if (
				!validateBlockPosition(c) ||
				!isCellEmpty(c) ||
				(maxCol - minCol) > 4
			) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Attempts to rotate the current piece clockwise.
	 *
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	public boolean rotatePieceClockwise() {
		boolean result = false;
		Integer offset = canPieceRotateClockwise();

		//If piece is not set, then canPieceRotateClockwise() will return null.
		if (offset != null) {
			result = true;
			if (offset != 0) {
				shift(new Coord(0, offset));
			}
			pieceRotateClockwise();
		}

		return result;
	}

	/**
	 * Attempts to rotate the current piece counter-clockwise.
	 *
	 * @return True if the piece was successfully rotated; otherwise false.
	 */
	public boolean rotatePieceCounterClockwise() {
		boolean result = false;
		Integer offset = canPieceRotateCounterClockwise();

		if (offset != null) {
			result = true;
			if (offset != 0) {
				shift(new Coord(0, offset));
			}
			pieceRotateCounterClockwise();
		}

		return result;
	}

	/**
	 * Attempts to shift the current piece with the given offset.
	 *
	 * @param offset - Offset coordinates relative to the current piece.
	 * @return True if the piece was successfully shifted; otherwise false.
	 */
	//! NOTE: move.rotation is ignored
	public boolean shiftPiece(Move move) {
		if (!isActive) {
			return false;
		}

		if (canPieceMove(move)) {
			location.add(move.offset());
			for (Coord b : blockLocations) {
				b.add(move.offset());
			}

			return true;
		}

		return false;
	}

	/**
	 * Returns whether the piece is at the bottom of the board.
	 *
	 * @return True if the piece is touching the bottom of the board; otherwise false.
	 * @throws NullPointerException if the player piece has not been set.
	 */
	public boolean isPieceAtBottom() {
		for (Coord coord : getNewPositions(Move.DOWN)) {
			if (!validateBlockPosition(coord) || !isCellEmpty(coord)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Transfers the piece's blocks to the Board then kills it.
	 */
	public void plotPiece() {
		Shape shape = getShape();

		for (Coord c : getBlockLocations()) {
			board[c.row() * cols + c.col()] = shape.value;
		}

		kill();
	}

	/**
	 * Gets whether the cell is empty at the given coordinates.
	 */
	public boolean isCellEmpty(Coord coords) {
		return isCellEmpty(coords.row(), coords.col());
	}

	/**
	 * Gets whether the cell is empty at the given coordinates.
	 */
	public boolean isCellEmpty(int row, int col) {
		return (board[row * cols + col] == 0);
	}

	private static void removeRows(int[] blocks, int[] rows) {
		// TODO implement and use below
	}

	/**
	 * Clears full lines and shift remaining blocks down.
	 *
	 * @return List of cleared rows, or null if no rows were cleared.
	 */
	public List<Integer> clearLines() {
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

	private boolean isRowFull(int row) {
		for (int col = 0; col < cols; col++) {
			if (isCellEmpty(row, col)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns a List of rows that are completely full of blocks.
	 * The list will sorted in ascending order.
	 *
	 * @return A List of row indices for rows that are full.
	 */
	//todo Should only need to test rows that have changed since the last check.
	private List<Integer> getFullRows() {
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
	 * @throws ArrayIndexOutOfBoundsException if the coordinates are out of
	 * bounds for the game board.
	 */
	public boolean intersects() {
		for (Coord c : getBlockLocations()) {
			if (!isCellEmpty(c)) {
				return true;
			}
		}

		return false;
	}

	//! End of copied Board content

	/**
	 * Creates a new Tetris game with standard number of rows and columns.
	 */
	public TetrisGame() {
		this(DEFAULT_NUM_ROWS, DEFAULT_NUM_COLS);
	}

	/**
	 * Creates a new Tetris game with the given number of rows and columns.
	 * The piece entry point will be automatically calculated.
	 */
	public TetrisGame(int numRows, int numCols) {
		this(numRows, numCols, true);
	}

	/**
	 * Creates a new Tetris game object, including a game board with the given
	 * number of rows and columns, and the entry point for pieces.
	 *
	 * @param numRows - Number of rows for the game board.
	 * @param numCols - Number of columns for the game board.
	 * @param entryColumn - The coordinates at which the pieces start.
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

		//? Should this be row 0 or 1? Seeing inconsistency
		entryPoint = new Coord(1, calcEntryColumn(numCols));

		nextShapes = new ShapeQueue();
		eventBus = new EventBus();
		if (useGameloopTimer) {
			gameTimer = new Timer(this::gameloop, 1L, TimeUnit.SECONDS, true);
		}
		numTimerPushbacks = 0;

		blockLocations = new Coord[4];
		for (int i = 0; i < 4; i++) {
			blockLocations[i] = new Coord(entryPoint);
		}
	}

	public TetrisGame(TetrisGame other) {
		rows = other.rows;
		cols = other.cols;
		entryPoint = new FinalCoord(other.entryPoint);
		board = new int[rows * cols];
		System.arraycopy(other.board, 0, board, 0, rows * cols);
		level = other.level;
		linesCleared = other.linesCleared;
		linesUntilNextLevel = other.linesUntilNextLevel;
		score = other.score;
		dist = new long[NUM_SHAPES];
		System.arraycopy(other.dist, 0, dist, 0, dist.length);
		hasStarted = other.hasStarted;
		isGameOver = other.isGameOver;
		isClearingLines = other.isClearingLines;
		isPaused = other.isPaused;
		numPiecesDropped = other.numPiecesDropped;

		shape = other.shape;
		isActive = other.isActive;
		location = new Coord(other.location);
		rotationIndex = other.rotationIndex;
		blockLocations = new Coord[4];
		for (int i = 0; i < 4; i++) {
			blockLocations[i] = new Coord(location);
		}
		setBlockLocations();
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

		if (isActive && isPieceAtBottom()) {
			//*kerplunk*
			//next loop should attempt to clear lines
			plotPiece(); //kills 'piece'
			numPiecesDropped++;
			throwEvent(TetrisEvent.PIECE_PLACED);
			throwEvent(TetrisEvent.BLOCKS);

		} else if (!isActive) {	//the loop after piece kerplunks
			if (!attemptClearLines()) {
				//create next piece
				nextPiece();
				throwEvent(TetrisEvent.PIECE_CREATE);

				//Check for lose condition.
				//(the now reset piece intersects with a block on board)
				if (intersects()) {
					gameOver();
					return;
				}
			}

		} else {	//piece alive && not at bottom
			shiftPiece(Move.DOWN);
			throwEvent(TetrisEvent.PIECE_SHIFT);
		}

		throwEvent(TetrisEvent.GAMELOOP);
	}

	@Override public long getNumPiecesDropped() {
		return numPiecesDropped;
	}

	@Override public synchronized void newGame() {
		reset();
		throwEvent(TetrisEvent.NEW_GAME);
	}

	private void increaseLevel() {
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
	private long getTimerDelay() {
		return Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0);
	}

	private void reset() {
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
		if (gameTimer != null) { gameTimer.stop(); }
		throwEvent(TetrisEvent.STOP);
	}

	@Override public synchronized void pause() {
		if (hasStarted && !isGameOver) {
			isPaused = true;
			if (gameTimer != null) { gameTimer.stop(); }
			throwEvent(TetrisEvent.PAUSE);
		}
	}

	private void gameOver() {
		isGameOver = true;
		isPaused = false;
		isClearingLines = false;
		if (gameTimer != null) { gameTimer.stop(); }
		throwEvent(TetrisEvent.GAME_OVER);
	}

	@Override public synchronized void resume() {
		if (hasStarted && !isGameOver) {
			isPaused = false;
			if (gameTimer != null) { gameTimer.start(); }
			throwEvent(TetrisEvent.RESUME);
		}
	}

	// A bunch of Getters

	@Override public int[] getBlocksOnBoard() {
		int[] result = new int[board.length];
		System.arraycopy(board, 0, result, 0, board.length);
		return result;
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
	@Override public Coord[] getPieceBlocks() 		{return getBlockLocations();}
	@Override public Shape getNextShape() 			{return nextShapes.peek();}
	@Override public Shape getCurrentShape() 		{return getShape();}
	@Override public Coord getLocation() 			{return new Coord(location);}
	@Override public long[] getDistribution() {
		long[] result = new long[dist.length];
		System.arraycopy(dist, 0, result, 0, dist.length);
		return result;
	}

	@Override public boolean isPieceActive() {
		boolean result = false;
		if (hasStarted && !isGameOver && !isClearingLines && !isPaused) {
			result = isActive;
		}
		return result;
	}

	@Override public synchronized boolean rotateClockwise() {
		boolean result = rotatePieceClockwise();

		if (result && isPieceAtBottom() && numTimerPushbacks < 4) {
			// if (gameTimer != null) {
			// 	gameTimer.resetTickDelay();
			// 	System.out.println(gameTimer.resetTickDelay());
			// }
			numTimerPushbacks++;
		}

		if (result) {
			throwEvent(TetrisEvent.PIECE_ROTATE);
		}
		return result;
	}

	@Override public synchronized boolean rotateCounterClockwise() {
		boolean result = rotatePieceCounterClockwise();

		if (result && isPieceAtBottom() && numTimerPushbacks < 4) {
			// if (gameTimer != null) {
			// 	gameTimer.resetTickDelay();
			// 	System.out.println(gameTimer.resetTickDelay());
			// }
			numTimerPushbacks++;
		}

		if (result) {
			throwEvent(TetrisEvent.PIECE_ROTATE);
		}
		return result;
	}

	@Override public synchronized boolean shift(int rowOffset, int colOffset) {
		Move move = new Move(new Coord(rowOffset, colOffset), 0);
		boolean result = shiftPiece(move);

		if (result && isPieceAtBottom() && numTimerPushbacks < 4) {
			numTimerPushbacks++;
		}

		if (result) {
			throwEvent(TetrisEvent.PIECE_SHIFT);
		}
		return result;
	}

	////////////////////////
	//Event related methods
	////////////////////////

	@Override
	public boolean registerEventListener(TetrisEvent event, Consumer<Event> listener) {
		return eventBus.registerEventListener(event.name(), listener);
	}

	@Override
	public boolean unregisterEventListener(TetrisEvent event, Consumer<Event> listener) {
		return eventBus.unregisterEventListener(event.name(), listener);
	}

	private void throwEvent(TetrisEvent event) {
		eventBus.throwEvent(new Event(event.name()));
	}

	@Override public void shutdown() {
		stop();
		eventBus.dispose(false);
		if (gameTimer != null) { gameTimer.shutdown(); }
	}

	protected static class Position implements Comparable<Position> {
		final Coord location;
		final int rotation;
		final int numR;
		Position(Coord location, int r, Shape s) {
			this.location = location;
			this.numR = s.getNumRotations();
			this.rotation = ((r % numR) + numR) % numR;
		}
		Position(Position other) {
			this.location = new Coord(other.location);
			this.numR = other.numR;
			this.rotation = other.rotation;
		}
		Position(Position p, Move offset) {
			this.location = new Coord(p.location);
			this.location.add(offset.offset());
			this.numR = p.numR;
			this.rotation = ((p.rotation + offset.rotation() % numR) + numR) % numR;
		}
		@Override public boolean equals(Object obj) {
			boolean result = false;
			if (obj instanceof Position) {
				Position o = (Position) obj;
				result = (o.location.equals(location) && o.rotation == rotation
					&& ((rotation + numR) % numR) == ((o.rotation + o.numR) % o.numR));
			}
			return result;
		}
		@Override public int hashCode() {
			return location.hashCode() + rotation * 31;
		}
		int sqrdist(Position p) {
			int rowDiff = location.row() - p.location.row();
			int colDiff = location.col() - p.location.col();
			return rowDiff*rowDiff + colDiff*colDiff;
		}
		@Override public int compareTo(Position o) {
			return sqrdist(o);
		}
		@Override public String toString() {
			return String.format("{%s, rotation: %d}", location.toString(), rotation);
		}
	}

	protected static class PQPositionEntry implements Comparable<PQPositionEntry>{
		Position position;
		int priority;
		PQPositionEntry(Position p, int priority) {
			this.position = p;
			this.priority = priority;
		}
		@Override public int compareTo(PQPositionEntry o) {
			return (priority - o.priority);
		}
		@Override public int hashCode() {
			return position.hashCode();
		}
		@Override public boolean equals(Object obj) {
			return position.equals(obj);
		}
	}

	private static final Move[] POSSIBLE_MOVES = new Move[] {
		Move.UP, Move.DOWN,
		Move.LEFT, Move.RIGHT,
		Move.CLOCKWISE, Move.COUNTERCLOCKWISE
	};

	public boolean doesPathExist(Coord location, int rotation) {
		// Check if the piece or it's goal position has blocks in it.
		for (Coord c : getBlockLocations()) {
			if (!isCellEmpty(c)) {
				return false;
			}
		}

		// Tetromino originalPieceCopy = new Tetromino(piece);
		Position originalPosition = new Position(location, rotationIndex, shape);
		Position curPosition = new Position(originalPosition);
		Position goalPosition = new Position(location, rotation, getShape());

		PriorityQueue<PQPositionEntry> frontier = new PriorityQueue<>();
		HashSet<Position> visited = new HashSet<>();

		frontier.offer(new PQPositionEntry(curPosition, curPosition.sqrdist(goalPosition)));
		visited.add(curPosition);

		while (!frontier.isEmpty()) {
			curPosition = frontier.poll().position;
			setLocation(curPosition.location);
			setRotation(curPosition.rotation);

			for (Move move : POSSIBLE_MOVES) {
				if (canPieceMove(move)) {
					Position nextPosition = new Position(curPosition, move);

					if (nextPosition.equals(goalPosition)) {
						location = originalPosition.location;
						rotationIndex = originalPosition.rotation;
						return true;
					}

					if (!visited.contains(nextPosition)) {
						frontier.offer(
							new PQPositionEntry(nextPosition, nextPosition.sqrdist(goalPosition))
						);
						visited.add(nextPosition);
					}
				}
			}
		}

		location = originalPosition.location;
		rotationIndex = originalPosition.rotation;
		return false;
	}

	/**
	 * Resets this piece to the specified shape, location, and rotation.
	 *
	 * @param newShape - The new shape this should take.
	 * @param newLocation - The location to reset to.
	 * @param rotationIndex - The rotation of this piece.
	 */
	public void nextPiece() {
		shape = nextShapes.poll();
		location = new Coord(entryPoint);
		rotationIndex = 0;
		isActive = true;
		setBlockLocations();
	}

	/**
	 * Returns the current positions of the blocks that make up this piece.
	 *
	 * @return Coordinates of the blocks that make up this piece.
	 */
	public Coord[] getBlockLocations() {
		Coord blocks[] = new Coord[4];

		for (int i = 0; i < 4; i++) {
			blocks[i] = new Coord(blockLocations[i]);
		}

		return blocks;
	}

	/**
	 * Returns the current rotation of this piece.
	 *
	 * @return The rotation of this piece.
	 */
	public int getRotationIndex() {
		return rotationIndex;
	}

	/**
	 * Returns the current shape of this piece.
	 *
	 * @return The shape of this piece.
	 */
	public Shape getShape() {
		return shape;
	}

	/**
	 * Returns whether this piece is currently active.
	 * A piece that is active is movable.  A piece that is not active
	 * can not be moved until calling <code>reset</code>.
	 *
	 * @return True if this piece is flagged as active; otherwise false.
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Renders this piece unable to move until it is reset.
	 */
	public void kill() {
		isActive = false;
	}

	/**
	 * Shifts this piece with the specified offset coordinates.
	 *
	 * @param offsetCoords - Offset coordinates.
	 * @return True if the shift was successful; otherwise false.
	 */
	public boolean shift(Coord offsetCoords) {
		boolean result = false;

		if (isActive) {
			location.add(offsetCoords);

			for (Coord b : blockLocations) {
				b.add(offsetCoords);
			}
		}

		return result;
	}

	/**
	 * Attempts to rotate this piece clockwise.
	 *
	 * @return True if the rotation was successful; otherwise false.
	 */
	public boolean pieceRotateClockwise() {
		boolean result = false;

		if (isActive) {
			rotationIndex--;
			setBlockLocations();
			result = true;
		}

		return result;
	}

	/**
	 * Attempts to rotate this piece counter-clockwise.
	 *
	 * @return True if the rotation was successful; otherwise false.
	 */
	public boolean pieceRotateCounterClockwise() {
		boolean result = false;

		if (isActive) {
			rotationIndex++;
			setBlockLocations();
			result = true;
		}

		return result;
	}

	//Calculates the locations of the blocks which make up this piece.
	private void setBlockLocations() {
		Coord[] offsets = shape.getRotation(rotationIndex);

		for (int i = 0; i < 4; i++) {
			blockLocations[i].set(location);
			blockLocations[i].add(offsets[i]);
		}
	}

	/**
	 * Returns the block coordinates of where the piece would be if it were
	 * shifted and/or rotated by the specified amounts.
	 * <br>This does not change the piece's location or orientation.
	 *
	 * @param offset - Row/Column to offset by.
	 * @param rotationOffset - Number of times to rotate. Negative for
	 * Clockwise, Positive for CCW.
	 * @return Coordinates of the would-be piece if it were shifted and/or
	 * rotated.
	 */
	public Coord[] getNewPositions(Move move) {
		Coord[] result = new Coord[4];
		Coord[] blockOffsets = shape.getRotation(rotationIndex + move.rotation());

		for (int i = 0; i < 4; i++) {
			result[i] = new Coord(location);
			result[i].add(blockOffsets[i], move.offset());
		}

		return result;
	}

	// @Override public String toString() {
	// 	return String.format(
	// 		"{Shape: %s, Rotation: %d, Coords: %s}",
	// 		shape.toString(), rotationIndex, Arrays.toString(blockLocations)
	// 	);
	// }

	/**
	 * A Tetromino is equal to another if they share the same block locations.
	 * The locations must also be in the same order in the underlying array
	 * (but the user should not need to worry about that.)
	 */
	// @Override public boolean equals(Object obj) {
	// 	boolean result = false;
	// 	if (obj instanceof Tetromino) {
	// 		Tetromino o = (Tetromino) obj;
	// 		if (blockLocations[0].equals(o.blockLocations[0])
	// 				&& blockLocations[1].equals(o.blockLocations[1])
	// 				&& blockLocations[2].equals(o.blockLocations[2])
	// 				&& blockLocations[3].equals(o.blockLocations[3])) {
	// 			result = true;
	// 			//if block locations are the same, then they must also be the same
	// 			// shape and have the same rotation index.
	// 		}
	// 	}
	// 	return result;
	// }

	// @Override public int hashCode() {
	// 	return location.hashCode() + (31 * rotationIndex) + (73 * shape.value);
	// }

	/**
	 * Sets the location of this piece.
	 * If the piece is flagged as inactive (<code>isActive()</code> returns
	 * <code>false</code>), then this operation will fail.
	 *
	 * @param newLocation - The new coordinates.
	 * @return True if the piece was moved; otherwise false.
	 */
	public boolean setLocation(Coord newLocation) {
		if (!isActive) {
			return false;
		}

		this.location.set(newLocation);
		setBlockLocations();

		return true;
	}

	/**
	 * Sets the rotation of this piece.
	 * If the piece is flagged as inactive (<code>isActive()</code> returns
	 * <code>false</code>), then this operation will fail.
	 *
	 * @param newRotation - The new rotation.
	 * @return True if the piece was moved; otherwise false.
	 */
	public boolean setRotation(int newRotation) {
		if (!isActive) {
			return false;
		}

		this.rotationIndex = newRotation;
		setBlockLocations();

		return true;
	}
}
