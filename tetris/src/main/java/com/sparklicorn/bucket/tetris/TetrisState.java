package com.sparklicorn.bucket.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.tetris.util.structs.Move;
import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.tetris.util.structs.ShapeQueue;
import com.sparklicorn.bucket.util.Array;

/**
 * Represents the state of a Tetris game.
 */
public class TetrisState {
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
	 * Tracks whether the game piece is currently active.
	 */
	public boolean isActive;

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
	 * Tracks how many times each shape has appeared.
	 */
	public long[] dist;

	/**
	 * The active piece shape.
	 */
	public Shape shape;

	/**
	 * TODO #92 Hide shapeQueue so it can't be manipulated.
	 * A queue of next shapes.
	 */
	public ShapeQueue nextShapes;

	/**
	 * Position of the active piece.
	 */
	public Position position;

	/**
	 * The locations of the blocks in the active piece.
	 */
	public Coord[] blockLocations;

	/**
	 * Creates a new TetrisState with default rows and columns.
	 */
	public TetrisState() {
		this(TetrisGame.DEFAULT_NUM_ROWS, TetrisGame.DEFAULT_NUM_COLS);
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
		shape = null;
		nextShapes = new ShapeQueue();
		position = null;
		blockLocations = Array.fillWithFunc(new Coord[4], (i) -> new Coord(TetrisGame.DEFAULT_ENTRY_COORD));
		isActive = false;
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
		this.dist = Array.copy(other.dist);
		this.shape = other.shape;
		this.nextShapes = new ShapeQueue(other.nextShapes);
		this.position = (other.position != null) ? new Position(other.position) : null;
		this.blockLocations = Coord.copyFrom(other.blockLocations);
		this.isActive = other.isActive;
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
	 * Checks whether the specified cell is empty.
	 */
	public boolean isCellEmpty(int row, int col) {
		return board[row * cols + col] == 0;
	}

	/**
	 * Checks whether the specified cell is empty.
	 */
	public boolean isCellEmpty(Coord location) {
		return board[location.row() * cols + location.col()] == 0;
	}

	/**
	 * Checks whether the given coordinates are within the bounds of the board.
	 *
	 * @return True if the coordinates are valid; false otherwise.
	 */
	public boolean validateCoord(Coord coord) {
		return (
			coord.row() >= 0 &&
			coord.row() < rows &&
			coord.col() >= 0 &&
			coord.col() < cols
		);
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
		int minCol = cols - 1;
		int maxCol = 0;

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
		if (
			isGameOver ||
			!isActive ||
			isPaused ||
			move.rowOffset() < 0
		) {
			return false;
		}

		return isPositionValid(new Position(position).add(move));
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
	 * Updates the block coordinates for the active shape at the current position.
	 * Does not check if the active shape exists or is in valid position.
	 */
	public void updateBlockPositions() {
		int rotationIndex = shape.rotationIndex(position.rotation());
		for (int i = 0; i < blockLocations.length; i++) {
			blockLocations[i].set(position.offset());
			blockLocations[i].add(shape.rotationOffsets[rotationIndex][i]);
		}
	}

	/**
	 * Calculates the block coordinates for the active shape at the given position.
	 *
	 * @param pos Position of the shape to calculate the block coordinates for.
	 * @return An array of coordinates for the blocks of the shape at the given position.
	 */
	public Coord[] getShapeCoordsAtPosition(Position pos) {
		Coord[] coords = Coord.copyFrom(blockLocations);
		int rotationIndex = shape.rotationIndex(pos.rotation());
		for (int i = 0; i < blockLocations.length; i++) {
			coords[i].set(pos.offset());
			coords[i].add(shape.rotationOffsets[rotationIndex][i]);
		}

		return coords;
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
	 * @return True if the piece is overlapping with other blocks.
	 */
	protected boolean intersects(Coord[] locations) {
		for (Coord location : locations) {
			if (!isCellEmpty(location)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Moves the piece position by the specified amount and updates its block coordinates.
	 * Legality of the move is not checked.
	 *
	 * @param move The move to apply to the piece position.
	 */
	public void movePiece(Move move) {
		position.add(move);
		updateBlockPositions();
	}

	/**
	 * Places the piece on the board at its current position.
	 */
	public void placePiece() {
		updateBlockPositions();
		for (Coord c : blockLocations) {
			int index = c.row() * cols + c.col();
			board[index] = shape.value;
		}
	}
}
