package com.sparklicorn.bucket.games.tetris;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sparklicorn.bucket.games.tetris.util.Timer;
import com.sparklicorn.bucket.util.event.*;
import com.sparklicorn.bucket.games.tetris.util.structs.*;

//Implementation of Tetris that uses the 7-bag random piece generator.
//The number of rows and columns can be specified by the user.
public class TetrisGame implements ITetrisGame {

	/*****************
	 * CONSTANTS
	 ******************/
	public static final int DEFAULT_NUM_ROWS = 20;
	public static final int DEFAULT_NUM_COLS = 10;

	public static final int MINIMUM_ROWS = 4;
	public static final int MAXIMUM_ROWS = 200;

	public static final int MINIMUM_COLS = 4;
	public static final int MAXIMUM_COLS = 200;

	public static final Coord DEFAULT_ENTRY_POINT = new FinalCoord(0,4);

	private static final long POINTS_BY_LINES_CLEARED[] = {0, 40, 100, 300, 1200};

	private static final long MIN_LEVEL = 0;
	private static final long MAX_LEVEL = 255;
	private static final Coord GRAVITY_OFFSET = Direction.DOWN.coordValue;
	private static final int LINES_PER_LEVEL = 10;

	/* ****************
	 * STATES AND STATS
	 ******************/
	private boolean isGameOver, isPaused, isClearingLines, hasStarted;
	private long level, score, linesCleared, numPiecesDropped;
	private int numRows, numCols, linesUntilNextLevel;
	private Coord entryPoint;
	private long[] dist;

	private int numTimerPushbacks;

	/* ****************
	 * STRUCTURES
	 ******************/
	private ShapeQueue nextShapes;

	/* ****************
	 * GAME COMPONENTS
	 ******************/
	private Board board;
	private Tetromino piece;
	private EventBus eventBus;

	protected Timer gameTimer;

	//Returns the number of points earned for clearing a given number of
	//	lines at a given level.
	private static long getPointsForClearing(int size, long level) {
		return POINTS_BY_LINES_CLEARED[size] * (level + 1);
	}

	/**
	 * Creates a new Tetris game object, including a game board with the
	 * default number of rows and columns and default entry point.
	 */
	public TetrisGame() {
		this(DEFAULT_NUM_ROWS, DEFAULT_NUM_COLS, DEFAULT_ENTRY_POINT);
	}

	/**
	 * Creates a new Tetris game object, including a game board with the given
	 * number of rows and columns. The entry point will be automatically
	 * calculated.
	 * @param numRows - Number of rows for the game board.
	 * @param numCols - Number of columns for the game board.
	 */
	public TetrisGame(int numRows, int numCols) {
		this(numRows, numCols, new Coord(1, (numCols / 2) - ((numCols % 2 == 0) ? 1 : 0)));
	}

	/**
	 * Creates a new Tetris game object, including a game board with the given
	 * number of rows and columns, and the entry point for pieces.
	 * @param numRows - Number of rows for the game board.
	 * @param numCols - Number of columns for the game board.
	 * @param entryPoint - The coordinates at which the pieces start.
	 */
	public TetrisGame(int numRows, int numCols, Coord entryPoint) {
		if (numRows < MINIMUM_ROWS || numRows > MAXIMUM_ROWS) {
			numRows = DEFAULT_NUM_ROWS;
		}

		if (numCols < MINIMUM_COLS || numCols > MAXIMUM_COLS) {
			numCols = DEFAULT_NUM_COLS;
		}

		this.numRows = numRows;
		this.numCols = numCols;
		this.entryPoint = entryPoint;//todo make sure this is valid
		this.nextShapes = new ShapeQueue();
		this.eventBus = new EventBus();
		this.gameTimer = new Timer(
				() -> {gameloop();},
				1L, TimeUnit.SECONDS, true);
		this.numTimerPushbacks = 0;
	}

	protected synchronized void gameloop() {
		if (isGameOver || isPaused) {
			return;
		}

		numTimerPushbacks = 0;

		if (piece.isActive() && board.isPieceAtBottom()) {
			//*kerplunk*
			//next loop should attempt to clear lines
			board.plotPiece(); //kills 'piece'
			numPiecesDropped++;
			throwEvent(TetrisEvent.PIECE_PLACED);
			throwEvent(TetrisEvent.BLOCKS);

		} else if (!piece.isActive()) {	//the loop after piece kerplunks
			List<Integer> lines = board.clearLines();
			if (lines != null) {	//lines were cleared!

				linesCleared += lines.size();
				score += getPointsForClearing(lines.size(), level);
				linesUntilNextLevel -= lines.size();

				throwEvent(TetrisEvent.LINE_CLEAR);
				throwEvent(TetrisEvent.SCORE_UPDATE);
				throwEvent(TetrisEvent.BLOCKS);

				//check if we need to update level
				if (linesUntilNextLevel <= 0) { //level up!!
					increaseLevel();
				}

			} else {	//piece is inactive, no line clears on this loop tick
				//create next piece
				piece.reset(nextShapes.poll(), entryPoint);
				throwEvent(TetrisEvent.PIECE_CREATE);

				//Check for lose condition.
				//(the now reset piece intersects with a block on board)
				if (board.intersects()) {
					gameOver();
					return;
				}
			}

		} else {	//piece alive && not at bottom
			board.shiftPiece(GRAVITY_OFFSET);
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
		gameTimer.setDelay(getTimerDelay(), TimeUnit.MILLISECONDS);
		//loopTickNanos = TimeUnit.MILLISECONDS.toNanos(
		//		Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0));
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

		board = new Board(numRows, numCols);
		piece = new Tetromino(Shape.O, entryPoint);
		setupNextPiece();
		board.setPiece(piece);
	}

	//Draws the next shape from the shapeQueue and resets the player piece.
	private void setupNextPiece() {
		Shape s = nextShapes.poll();
		piece.reset(s, entryPoint);
		dist[s.value - 1]++;
	}

	@Override public synchronized void start(long level) {
		if (!hasStarted) {
			reset();
			this.level = (level >= 0) ? level : MIN_LEVEL;
			this.level = (level <= MAX_LEVEL) ? level : MAX_LEVEL;
			hasStarted = true;
			gameTimer.setDelay(
					Math.round((Math.pow(0.8 - (level) * 0.007, level)) * 1000.0),
					TimeUnit.MILLISECONDS);
			gameTimer.start();
			throwEvent(TetrisEvent.START);
		}
	}

	@Override public synchronized void stop() {
		isPaused = false;
		isGameOver = true;
		isClearingLines = false;
		gameTimer.stop();
		throwEvent(TetrisEvent.STOP);
	}

	@Override public synchronized void pause() {
		if (hasStarted && !isGameOver) {
			isPaused = true;
			gameTimer.stop();
			throwEvent(TetrisEvent.PAUSE);
		}
	}

	private void gameOver() {
		isGameOver = true;
		isPaused = false;
		isClearingLines = false;
		gameTimer.stop();
		throwEvent(TetrisEvent.GAME_OVER);
	}

	@Override public synchronized void resume() {
		if (hasStarted && !isGameOver) {
			isPaused = false;
			gameTimer.start();
			throwEvent(TetrisEvent.RESUME);
		}
	}

	// A bunch of Getters

	@Override public boolean isGameOver() 			{return isGameOver;}
	@Override public boolean hasStarted() 			{return hasStarted;}
	@Override public boolean isPaused() 			{return isPaused;}
	@Override public boolean isClearingLines() 		{return isClearingLines;}
	@Override public int[] getBlocksOnBoard() 		{return board.blocks();}
	@Override public int[] getBlocksOnBoard(int[] blocks) {
		if (blocks == null || blocks.length != numRows * numCols) {
			return getBlocksOnBoard();
		}
		return board.blocks(blocks);
	}
	@Override public long getLevel() 				{return level;}
	@Override public long getScore() 				{return score;}
	@Override public long getLinesCleared() 		{return linesCleared;}
	@Override public int getLinesUntilNextLevel() 	{return linesUntilNextLevel;}
	@Override public int getNumRows() 				{return numRows;}
	@Override public int getNumCols() 				{return numCols;}
	@Override public Coord[] getPieceBlocks() 		{return piece.getBlockLocations();}
	@Override public Shape getNextShape() 			{return nextShapes.peek();}
	@Override public Shape getCurrentShape() 		{return piece.getShape();}
	@Override public Coord getLocation() 			{return piece.getLocation();}

	@Override public long[] getDistribution() {
		long[] result = new long[dist.length];
		System.arraycopy(dist, 0, result, 0, dist.length);
		return result;
	}

	@Override public boolean isPieceActive() {
		boolean result = false;
		if (hasStarted && !isGameOver && !isClearingLines && !isPaused) {
			result = piece.isActive();
		}
		return result;
	}

	@Override public synchronized boolean rotateClockwise() {
		boolean result = board.rotatePieceClockwise();

		if (result && board.isPieceAtBottom() && numTimerPushbacks < 4) {
			//gameTimer.resetTickDelay();
			//System.out.println(gameTimer.resetTickDelay());
			numTimerPushbacks++;
		}

		if (result) {
			throwEvent(TetrisEvent.PIECE_ROTATE);
		}
		return result;
	}

	@Override public synchronized boolean rotateCounterClockwise() {
		boolean result = board.rotatePieceCounterClockwise();

		if (result && board.isPieceAtBottom() && numTimerPushbacks < 4) {
			//gameTimer.resetTickDelay();
			//System.out.println(gameTimer.resetTickDelay());
			numTimerPushbacks++;
		}

		if (result) {
			throwEvent(TetrisEvent.PIECE_ROTATE);
		}
		return result;
	}

	@Override public synchronized boolean shift(int rowOffset, int colOffset) {
		boolean result = board.shiftPiece(new Coord(rowOffset, colOffset));

		if (result && board.isPieceAtBottom() && numTimerPushbacks < 4) {
			//System.out.println(gameTimer.resetTickDelay());
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
		return eventBus.registerEventListener(event.name, listener);
	}

	@Override
	public boolean unregisterEventListener(TetrisEvent event, Consumer<Event> listener) {
		return eventBus.unregisterEventListener(event.name, listener);
	}

	private void throwEvent(TetrisEvent event) {
		eventBus.throwEvent(new Event(event.name));
	}

	//Clean-up.  Free any used resources.
	@Override public void shutdown() {
		stop();
		eventBus.dispose(false);
		gameTimer.shutdown();
	}
}
