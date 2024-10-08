package com.metal_pony.bucket.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.metal_pony.bucket.tetris.util.structs.Coord;
import com.metal_pony.bucket.tetris.util.structs.Coord.FinalCoord;
import com.metal_pony.bucket.tetris.util.structs.Move;
import com.metal_pony.bucket.tetris.util.structs.Piece;
import com.metal_pony.bucket.tetris.util.structs.Position;
import com.metal_pony.bucket.tetris.util.structs.Shape;
import com.metal_pony.bucket.tetris.util.structs.ShapeQueue;
import com.metal_pony.bucket.util.Array;

/**
 * Represents the state of a Tetris game.
 */
public class TetrisState {
	public static final int DEFAULT_NUM_ROWS = 20;
	public static final int DEFAULT_NUM_COLS = 10;
	public static final int DEFAULT_ENTRY_COLUMN = calcEntryColumn(DEFAULT_NUM_COLS);
	/**
	 * Calculates the column where pieces appear, which is the center column,
	 * or just left of center if there are an even number of columns.
	 *
	 * @param cols Number of columns on the board.
	 */
	public static int calcEntryColumn(int cols) {
		return (cols / 2) - ((cols % 2 == 0) ? 1 : 0);
	}
	public static final Coord DEFAULT_ENTRY_COORD = new FinalCoord(1, DEFAULT_ENTRY_COLUMN);

	/**
	 * Represents a Tetris board. Each element in the rows*cols array represents a single cell,
	 * where the value is the color of the block in that cell. A value of 0 means the cell is empty.
	 */
	public int[] board;

	// TODO #44 keep track of full rows as pieces are placed
	/**
	 * Tracks which rows are full.
	 */
	public boolean[] fullRows;

	/**
	 * Tracks whether the game is over.
	 */
	public boolean isGameOver;

	/**
	 * Tracks whether the game is paused.
	 */
	public boolean isPaused;

	/**
	 * Tracks whether the game is currently in the line-clearing process.
	 */
	public boolean isClearingLines;

	/**
	 * Tracks whether the game has started.
	 */
	public boolean hasStarted;

	/**
	 * Current game level.
	 */
	public long level;

	/**
	 * Current score.
	 */
	public long score;

	/**
	 * Tracks how many lines have been cleared.
	 */
	public long linesCleared;

	/**
	 * Tracks how many pieces have been placed.
	 */
	public long numPiecesDropped;

	/**
	 * Number of rows on the board.
	 */
	public int rows;

	/**
	 * Number of columns on the board.
	 */
	public int cols;

	/**
	 * Tracks how many lines must be cleared before the next level.
	 */
	public int linesUntilNextLevel;

	/**
	 * The coordinates where pieces appear on the board.
	 */
	public Coord entryCoord;

	/**
	 * Tracks how many times each shape has appeared.
	 */
	public long[] dist;

	/**
	 * A queue of next shapes.
	 */
	public ShapeQueue nextShapes;

	/**
	 * The current piece.
	 */
	public Piece piece;

	/**
	 * Creates a new TetrisState with default rows and columns.
	 */
	public TetrisState() {
		this(TetrisState.DEFAULT_NUM_ROWS, TetrisState.DEFAULT_NUM_COLS);
	}

	/**
	 * Creates a new TetrisState with the given number of rows and columns.
	 */
	public TetrisState(int rows, int cols) {
		TetrisGame.ROWS_VALIDATOR.validate(rows);
		TetrisGame.COLS_VALIDATOR.validate(cols);

		board = new int[rows * cols];
		Arrays.fill(board, 0);
		fullRows = new boolean[rows];
		Arrays.fill(fullRows, false);
		isGameOver = false;
		isPaused = false;
		isClearingLines = false;
		hasStarted = false;
		level = 0L;
		score = 0L;
		linesCleared = 0L;
		numPiecesDropped = 0L;
		this.rows = rows;
		this.cols = cols;
		linesUntilNextLevel = TetrisGame.LINES_PER_LEVEL;
		dist = new long[Shape.NUM_SHAPES];
		Arrays.fill(dist, 0L);
		nextShapes = new ShapeQueue();
		entryCoord = new Coord(1, calcEntryColumn(cols));
		piece = new Piece(entryCoord, nextShapes.poll());
	}

	/**
	 * Creates a new TetrisState with the same values as the given TetrisState.
	 */
	public TetrisState(TetrisState other) {
		this.board = Array.copy(other.board);
		this.fullRows = Array.copy(other.fullRows);
		this.isGameOver = other.isGameOver;
		this.isPaused = other.isPaused;
		this.isClearingLines = other.isClearingLines;
		this.hasStarted = other.hasStarted;
		this.level = other.level;
		this.score = other.score;
		this.linesCleared = other.linesCleared;
		this.numPiecesDropped = other.numPiecesDropped;
		this.rows = other.rows;
		this.cols = other.cols;
		this.linesUntilNextLevel = other.linesUntilNextLevel;
		this.entryCoord = new Coord(other.entryCoord);
		this.dist = Array.copy(other.dist);
		this.nextShapes = new ShapeQueue(other.nextShapes);
		this.piece = new Piece(other.piece);
	}

	/**
	 * Peeks the given number of shapes in the queue.
	 *
	 * @param numShapes Number of shapes to peek.
	 * @return An array of the next shapes in the queue.
	 */
	public Shape[] getNextShapes(int numShapes) {
		return nextShapes.peekNext(numShapes);
	}

	/**
	 * Sets the value of the cell at the given location.
	 *
	 * @param row The row of the cell to set.
	 * @param col The column of the cell to set.
	 * @param value The value to set the cell to.
	 */
	public void setCell(int row, int col, int value) {
		board[row * cols + col] = value;
	}

	/**
	 * Sets the value of the cell at the given location.
	 *
	 * @param location The location of the cell to set.
	 * @param value The value to set the cell to.
	 */
	public void setCell(Coord location, int value) {
		setCell(location.row(), location.col(), value);
	}

	/**
	 * Gets the value of the cell at the given location.
	 *
	 * @param row The row of the cell to get.
	 * @param col The column of the cell to get.
	 * @return The value of the cell at the given location.
	 */
	public int getCell(int row, int col) {
		return board[row * cols + col];
	}

	/**
	 * Gets the value of the cell at the given location.
	 *
	 * @param location The location of the cell to get.
	 * @return The value of the cell at the given location.
	 */
	public int getCell(Coord location) {
		return getCell(location.row(), location.col());
	}

	/**
	 * Checks whether the specified cell is empty.
	 */
	public boolean isCellEmpty(int row, int col) {
		return getCell(row, col) == 0;
	}

	/**
	 * Checks whether the specified cell is empty.
	 */
	public boolean isCellEmpty(Coord location) {
		return getCell(location.row(), location.col()) == 0;
	}

	/**
	 * Checks whether the given coordinates are within the bounds of the board.
	 *
	 * @param row The row to check.
	 * @param col The column to check.
	 * @return True if the coordinates are valid; false otherwise.
	 */
	public boolean validateCoords(int row, int col) {
		return (
			row >= 0 &&
			row < rows &&
			col >= 0 &&
			col < cols
		);
	}

	/**
	 * Checks whether the given coordinates are within the bounds of the board.
	 *
	 * @return True if the coordinates are valid; false otherwise.
	 */
	public boolean validateCoord(Coord coord) {
		return validateCoords(coord.row(), coord.col());
	}

	/**
	 * Checks whether the current piece is within the bounds of the board.
	 *
	 * @return True if the piece is in bounds; false otherwise.
	 */
	public boolean pieceInBounds() {
		return Arrays.stream(piece.blockCoords()).allMatch(this::validateCoord);
	}

	/**
	 * Checks whether the given position for the current shape is valid,
	 * i.e. the piece blocks are all within the bounds of the board, there
	 * are no blocks in the way, and the piece is not wrapping around the board.
	 *
	 * @param pos The position to check.
	 * @return True if the position is valid; false otherwise.
	 */
	public boolean isPositionValid(Position pos) {
		Coord[] newBlockCoords = getShapeCoordsAtPosition(pos);
		int minCol = newBlockCoords[0].col();
		int maxCol = newBlockCoords[0].col();



		for (Coord c : newBlockCoords) {
			minCol = Math.min(minCol, c.col());
			maxCol = Math.max(maxCol, c.col());

			if (
				!validateCoord(c) ||
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
	 * ! it's more like validation of the resulting location and does not imply that a path exists to it unless the
	 * ! magnitude of the move is (exclusively) one unit of rotation or of offset.
	 * ! however you wanna fit that into a name... is great
	 * Checks that the given move is in bounds of the board, that there are no blocks occupying
	 * the spaces, and that the resulting position does not end up higher on the board.
	 *
	 * @param move
	 * @return True if the active piece can move to the specified position; otherwise false.
	 */
	public boolean canPieceMove(Move move) {
		return (
			!isGameOver &&
			piece.isActive() &&
			!isPaused &&
			move.rowOffset() >= 0 &&
			isPositionValid(piece.position().add(move))
		);
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
	public Move validateRotation(Move move) {
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
	 * Calculates the block coordinates for the active shape at the given position.
	 *
	 * @param pos Position of the shape to calculate the block coordinates for.
	 * @return An array of coordinates for the blocks of the shape at the given position.
	 */
	public Coord[] getShapeCoordsAtPosition(Position pos) {
		return new Piece(pos, piece.shape()).blockCoords();
	}

	/**
	 * Determines whether the given row is full.
	 *
	 * @param row Index of the row to check.
	 * @return True if the row is full; false otherwise.
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
	 * Gets a list of indices for rows that are full, in ascending order.
	 */
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
	 * @return True if the piece is overlapping with other blocks; otherwise false.
	 */
	public boolean pieceOverlapsBlocks() {
		return piece.isActive() && piece.intersects(this);
	}

	/**
	 * Places the piece on the board at its current position.
	 */
	public void placePiece() {
		if (piece.isActive()) {
			piece.forEachCell((coord) -> setCell(coord, piece.shape().value));
			piece.disable();
			numPiecesDropped++;
		}
	}

	/**
	 * Resets the piece to the top of the board with the next shape from the queue.
	 */
	public void resetPiece() {
		piece.reset(entryCoord, nextShapes.poll());
	}

	/**
	 * Fast-forwards the shape queue so that the given shape is next in the queue.
	 *
	 * @param shape The Shape that will be next in the queue.
	 */
	public void setNextShape(Shape shape) {
		// Fast-forward so that the given shape is next in the queue.
		while (nextShapes.peek() != shape) {
			nextShapes.poll();
		}
	}

	/**
	 * Returns whether the game has started and is in progress.
	 */
	public boolean isRunning() {
		return hasStarted && !isGameOver;
	}
}
