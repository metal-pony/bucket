package com.sparklicorn.bucket.games.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sparklicorn.bucket.games.tetris.util.Timer;
import com.sparklicorn.bucket.util.Array;
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
	private static int calcEntryColumn(int cols) {
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
	protected Coord entryPoint;
	protected long[] dist;

	protected int numTimerPushbacks;

	protected ShapeQueue nextShapes;

	/* ****************
	 * GAME COMPONENTS
	 ******************/
	protected EventBus eventBus;
	protected Timer gameTimer;

	protected Shape shape;

	// TODO location + rotationIndex can be combined into a Position type
	// protected Move position;
	protected Coord location;
	protected int rotationIndex;

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

	/**
	 * Creates a new Tetris game as a copy of another.
	 *
	 * NOTE: The new game will not have an event bus or gameloop timer.
	 */
	public TetrisGame(TetrisGame other) {
		rows = other.rows;
		cols = other.cols;
		entryPoint = new FinalCoord(other.entryPoint);
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
		// System.out.printf("validateRotation(%s)\n", move);

		if (!(move.equals(Move.CLOCKWISE) || move.equals(Move.COUNTERCLOCKWISE))) {
			// TODO If going to keep output, wrap into function with debug flag.
			// System.out.println("validateRotation return standstill");
			return new Move(Move.STAND);
		}

		if (canPieceMove(move)) {
			// System.out.println("validateRotation Move is valid! Returning");
			return new Move(move);
		}
		// System.out.println("validateRotation Move not valid, checking offsets...");

		// Attempt to "kick off" an edge or block if rotating too close.
		Move kickLeft = new Move(move);
		Move kickRight = new Move(move);
		for (int colOffset = 1; colOffset < 3; colOffset++) {
			kickLeft.add(Move.LEFT);
			if (canPieceMove(kickLeft)) {
				// System.out.printf("");
				return kickLeft;
			}

			kickRight.add(Move.RIGHT);
			if (canPieceMove(kickRight)) {
				return kickRight;
			}
		}

		// System.out.println("validateRotation No offsets valid... Returning standstill");
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
		// System.out.printf("rotate(%s)\n", move);

		Move _move = validateRotation(move);
		// System.out.println("Adjusted move: " + move);

		if (_move.equals(Move.STAND)) {
			// System.out.println("rotate return false (standstill - rotation invalid)");
			return false;
		}

		location.add(_move.offset());
		rotationIndex += _move.rotation();
		setBlockLocations();

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
			// String before = location.toString();
			location.add(move.offset());
			// System.out.printf("shiftPiece(%s): %s -> %s\n", move.toString(), before, location.toString());
			setBlockLocations();
			return true;
		}

		return false;
	}

	/**
	 * Plots the piece's block data to the board.
	 */
	protected void plotPiece() {
		for (Coord c : getBlockLocations()) {
			board[c.row() * cols + c.col()] = shape.value;
		}
	}

	/**
	 * Gets whether the cell is empty at the given coordinates.
	 */
	protected boolean isCellEmpty(Coord coords) {
		return isCellEmpty(coords.row(), coords.col());
	}

	/**
	 * Gets whether the cell is empty at the given coordinates.
	 */
	protected boolean isCellEmpty(int row, int col) {
		return board[row * cols + col] == 0;
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
	protected boolean intersects() {
		for (Coord c : getBlockLocations()) {
			if (!isCellEmpty(c)) {
				return true;
			}
		}

		return false;
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
			// System.out.println("GRAVITY");
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
	@Override public Coord[] getPieceBlocks() 		{return getBlockLocations();}
	@Override public Shape getNextShape() 			{return nextShapes.peek();}
	@Override public Shape getCurrentShape() 		{return shape;}
	@Override public Coord getLocation() 			{return new Coord(location);}
	@Override public long[] getDistribution() 		{return Array.copy(dist);}

	@Override public boolean isPieceActive() {
		return (hasStarted && !isGameOver && !isClearingLines && !isPaused && isActive);
	}

	private boolean handleRotation(Move direction) {
		// System.out.printf("handleRotation(%s)\n", direction);

		if (!isActive) {
			// System.out.printf("handleRotation return false (piece is inactive)\n", direction);
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

			// System.out.println("Rotation successful");
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
		return eventBus.registerEventListener(event.name(), listener);
	}

	@Override
	public boolean unregisterEventListener(TetrisEvent event, Consumer<Event> listener) {
		return eventBus.unregisterEventListener(event.name(), listener);
	}

	protected void throwEvent(TetrisEvent event) {
		eventBus.throwEvent(new Event(event.name()));
	}

	@Override public void shutdown() {
		stop();
		eventBus.dispose(false);
		if (gameTimer != null) {
			gameTimer.shutdown();
		}
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
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}

			if (obj instanceof Position) {
				Position o = (Position) obj;
				return (
					o.location.equals(location) &&
					o.rotation == rotation &&
					((rotation + numR) % numR) == ((o.rotation + o.numR) % o.numR));
			}

			return false;
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

	protected static final Move[] POSSIBLE_MOVES = new Move[] {
		Move.UP, Move.DOWN,
		Move.LEFT, Move.RIGHT,
		Move.CLOCKWISE, Move.COUNTERCLOCKWISE
	};

	protected boolean doesPathExist(Coord location, int rotation) {
		// Check if the piece or it's goal position has blocks in it.
		for (Coord c : getBlockLocations()) {
			if (!isCellEmpty(c)) {
				return false;
			}
		}

		// Tetromino originalPieceCopy = new Tetromino(piece);
		Position originalPosition = new Position(location, rotationIndex, shape);
		Position curPosition = new Position(originalPosition);
		Position goalPosition = new Position(location, rotation, shape);

		PriorityQueue<PQPositionEntry> frontier = new PriorityQueue<>();
		HashSet<Position> visited = new HashSet<>();

		frontier.offer(new PQPositionEntry(curPosition, curPosition.sqrdist(goalPosition)));
		visited.add(curPosition);

		while (!frontier.isEmpty()) {
			curPosition = frontier.poll().position;
			location.set(curPosition.location);
			rotationIndex = curPosition.rotation;
			setBlockLocations();

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
	protected void nextPiece() {
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
	protected Coord[] getBlockLocations() {
		return Coord.copyFrom(blockLocations);
	}

	//Calculates the locations of the blocks which make up this piece.
	protected void setBlockLocations() {
		shape.calcBlockPositions(blockLocations, new Move(location, rotationIndex));
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
	protected Coord[] getNewPositions(Move move) {
		Coord[] result = new Coord[4];
		Move newPosition = new Move(new Coord(location), rotationIndex);
		newPosition.add(move);
		shape.calcBlockPositions(result, newPosition);
		return result;
	}
}
