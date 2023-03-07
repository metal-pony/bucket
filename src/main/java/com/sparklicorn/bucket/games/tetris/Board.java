package com.sparklicorn.bucket.games.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.sparklicorn.bucket.games.tetris.util.structs.*;
import com.sparklicorn.bucket.util.PriorityQueue;

public class Board {

	protected int blocks[], rows, cols;

	protected Tetromino piece;

	/**
	 * Creates a new Board with the specified number of rows and columns.
	 * @param rows - Number of rows on the board.
	 * @param cols - Number of columns on the board.
	 */
	public Board(int rows, int cols) {
		this(rows, cols, null);
	}

	/**
	 * Creates a new Board as a copy of another.
	 * @param other - Board to copy from.
	*/
	public Board(Board other) {
		this(other.rows, other.cols, other.blocks());
	}

	/**
	 * Creates a Board with the specified state.
	 * @param state - <code>int[]</code> representing the blocks
	 * placed on the board.
	 * @param rows - Number of rows on the board.
	 * @param cols - Number of columns on the board.
	 */
	public Board(int rows, int cols, int[] state) {
		if (rows < 4 || cols < 4) {
			throw new IllegalArgumentException("rows and cols must both be 4 or greater.");
		}

		this.rows = rows;
		this.cols = cols;
		this.blocks = new int[rows * cols];

		if (state != null) {
			if (state.length != this.blocks.length) {
				throw new IllegalArgumentException("Given state's length must be product of rows and cols.");
			}

			System.arraycopy(state, 0, this.blocks, 0, this.blocks.length);
		}
	}

	/**
	 * Sets the current player piece.
	 * @param piece - piece to set as the player's current piece.
	*/
	public void setPiece(Tetromino piece) {
		this.piece = piece;
	}

	/**
	 * A Board is equal to another if they share the same number rows and
	 * columns and have the same block layout.
	 */
	@Override public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof Board) {
			Board _o = (Board) obj;
			result = (rows == _o.rows) &&
				(cols == _o.cols) &&
				Arrays.equals(blocks, _o.blocks);
		}
		return result;
	}

	@Override public int hashCode() {
		return Arrays.hashCode(blocks);
	}

	public int rows() {
		return rows;
	}

	public int cols() {
		return cols;
	}

	/**
	 * Gets the blocks currently on the board (excluding the active Tetromino).
	 * Each value in the array is associated with a shape (See {@link Shape}) or 0 for empty.
	 * @return An <code>int[]</code> containing the data of the board's blocks.
	 */
	public int[] blocks() {
		return blocks(new int[blocks.length]);
	}

	public int[] blocks(int[] arr) {
		if (arr == null) {
			throw new NullPointerException("Given array should not be null.");
		}

		if (arr.length != blocks.length) {
			throw new IllegalArgumentException("Given array's length should be board rows * cols.");
		}

		System.arraycopy(blocks, 0, arr, 0, blocks.length);

		return arr;
	}

	/**
	 * Determines whether the current piece can rotate clockwise.
	 * @return An integer representing how many blocks to shift
	 * the piece to be able to rotate, or null if the piece cannot rotate
	 * at all.
	 */
	public Integer canPieceRotateClockwise() {
		return canRotate(-1);
	}

	/**
	 * Determines whether the current piece can rotate counter-clockwise.
	 * @return An integer representing how many blocks to shift
	 * the piece to be able to rotate, or null if the piece cannot rotate
	 * at all.
	 */
	//todo RENAME METHOD - The answer to the question "can...something something"
	//should never be "7".
	//How about "whatOffsetCanPieceRotateCounterClockwiseWith()"
	public Integer canPieceRotateCounterClockwise() {
		return canRotate(1);
	}

	private Integer canRotate(int direction) {
		Integer result = null;
		int colOffset = 0;

		//this will attempt to kick off an edge or block
		while (result == null && colOffset < 3) {
			if (canPieceMove(new Coord(0, colOffset), direction)) {
				result = colOffset;
			} else if (canPieceMove(new Coord(0, -colOffset), direction)) {
				result = -colOffset;
			}
			colOffset++;
		}

		return result;
	}

	/**
	 * Determines if the piece can move with the given offset and rotation.
	 * @param offset - Offset coordinates relative to the current piece.
	 * @param rotationOffset - Offset applied to the current piece's rotation
	 * index. <code>0</code> for no rotation.
	 * @return True if the piece can move to the specified position and
	 * rotation; otherwise false.
	 */
	public boolean canPieceMove(Coord offset, int rotationOffset) {
		boolean result = false;

		if (piece != null && piece.isActive() && offset.row() >= 0) {
			result = true;
			Coord pieceBlockLocs[] = piece.getNewPositions(offset, rotationOffset);
			int s = pieceBlockLocs[0].col(); //column of last checked position

			for (Coord c : pieceBlockLocs) {
				if (
					c.col() < 0 ||
					c.col() >= cols ||
					c.row() < 0 ||
					c.row() >= rows ||
					isBlockAtCoord(c) ||
					(Math.abs(s - c.col())) > 4
				) {
					result = false;
					break;
				}
				s = c.col();
			}
		}

		return result;
	}

	/**
	 * Determines if the piece can move in the given direction.
	 * @param d - Direction representing a specific movement.
	 * @return True if the piece can move in the specified direction and
	 * rotation; otherwise false.
	 */
	public boolean canPieceMove(Direction d) {
		return canPieceMove(d.coordValue, d.rotation);
	}

	/**
	 * Attempts to rotate the current piece clockwise.
	 * @return True if the piece was successfully rotated;
	 * otherwise false.
	 */
	public boolean rotatePieceClockwise() {
		boolean result = false;
		Integer offset = canPieceRotateClockwise();

		//If piece is not set, then canPieceRotateClockwise() will return null.
		if (offset != null) {
			result = true;
			if (offset != 0) {
				piece.shift(new Coord(0, offset));
			}
			piece.rotateClockwise();
		}

		return result;
	}

	/**
	 * Attempts to rotate the current piece counter-clockwise.
	 * @return True if the piece was successfully rotated;
	 * otherwise false.
	 */
	public boolean rotatePieceCounterClockwise() {
		boolean result = false;
		Integer offset = canPieceRotateCounterClockwise();

		if (offset != null) {
			result = true;
			if (offset != 0) {
				piece.shift(new Coord(0, offset));
			}
			piece.rotateCounterClockwise();
		}

		return result;
	}

	/**
	 * Attempts to shift the current piece with the given offset.
	 * @param offset - Offset coordinates relative to the current piece.
	 * @return True if the piece was successfully shifted;
	 * otherwise false.
	 */
	public boolean shiftPiece(Coord offset) {
		boolean result = false;

		if (piece != null && piece.isActive()) {
			if (canPieceMove(offset, 0)) {
				result = true;
				piece.shift(offset);
			}
		}

		return result;
	}

	/**
	 * Returns whether the piece is at the bottom of the board.
	 * @return True if the piece is touching the bottom of the board;
	 * otherwise false.
	 * @throws NullPointerException if the player piece has not been set.
	 */
	public boolean isPieceAtBottom() {
		boolean result = false;
		Coord pieceBlockLocs[] = piece.getNewPositions(Direction.DOWN.coordValue, 0);

		for (Coord coord : pieceBlockLocs) {
			if (
				coord.col() < 0 ||
				coord.col() >= cols ||
				coord.row() < 0 ||
				coord.row() >= rows ||
				isBlockAtCoord(coord)
			) {
				result = true;
				break;
			}
		}

		return result;
	}

	/**
	 * Transfers the piece's blocks to the Board then kills it.
	 */
	public void plotPiece() {
		Shape shape = piece.getShape();

		for (Coord c : piece.getBlockLocations()) {
			blocks[c.row() * cols + c.col()] = shape.value;
		}

		piece.kill();
	}

	/**
	 * Returns true if there is a block at the given coordinates; otherwise
	 * returns false.
	 * @param c - Row,Column coordinates on the board.
	 * @return True if there is a block at the given coordinates; otherwise
	 * false. Does not include blocks belonging to the current piece.
	 * @throws ArrayIndexOutOfBoundsException if the coordinates are out of
	 * bounds for the game board.
	 */
	public boolean isBlockAtCoord(Coord c) {
		return (blocks[c.row() * cols + c.col()] != 0);
	}

	/**
	 * Returns true if there is a block at the given coordinate; otherwise
	 * returns false.
	 * @param row - Row on the board.
	 * @param col - Column on the board.
	 * @return True if there is a block at the given coordinates; otherwise
	 * false. Does not include blocks belonging to the current piece.
	 * @throws ArrayIndexOutOfBoundsException if the coordinates are out of
	 * bounds for the game board.
	 */
	public boolean isBlockAtCoord(int row, int col) {
		return (blocks[row * cols + col] != 0);
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
						blocks[j] = blocks[k];
						blocks[k++] = 0;
					}
				}
			}
		}

		return (fullRows.isEmpty() ? null : fullRows);
	}

	private static boolean isRowFull(int[] blocks, int row) {
		return false; // TODO implement and use below
	}

	/**
	 * Returns a List of rows that are completely full of blocks.
	 * The list will sorted in ascending order.
	 * @return A List of row indices for rows that are full.
	 */
	//todo Should only need to test rows that have changed since the last check.
	private List<Integer> getFullRows() {
		List<Integer> lines = new ArrayList<>();

		for (int row = 0; row < rows; row++) {
			boolean rowIsFull = true;
			for (int col = 0; col < cols; col++)
				if (!isBlockAtCoord(row, col)) {
					rowIsFull = false;
					break;
				}
			if (rowIsFull)
				lines.add(row);
		}

		return lines;
	}

	/**
	 * Clears the blocks and player piece from the board.
	 * The number of rows and columns are preserved.
	 */
	public void reset() {
		reset(rows, cols);
	}

	/**
	 * Clears the blocks and player piece from the board and resizes the board
	 * to the given dimensions.
	 * @param newNumRows - New number of rows.
	 * @param newNumCols - New number of columns.
	 */
	public void reset(int newNumRows, int newNumCols) {
		this.rows = newNumRows;
		this.cols = newNumCols;
		this.blocks = new int[this.cols * this.rows];
		this.piece = null;
	}

	/**
	 * Determines if the piece is sharing the same cell as any other block.
	 * @return True if the piece is overlapping with other blocks.
	 * @throws ArrayIndexOutOfBoundsException if the coordinates are out of
	 * bounds for the game board.
	 */
	public boolean intersects() {
		boolean result = false;

		for (Coord c : piece.getBlockLocations()) {
			if (isBlockAtCoord(c)) {
				result = true;
				break;
			}
		}

		return result;
	}

	//Describes the position of a tetromino.
	protected static class Position implements Comparable<Position> {
		final Coord coord; //location on game board
		final int rotation; //rotation
		final int numR;
		Position(Coord c, int r, Shape s) {
			this.coord = c;
			this.numR = s.getNumRotations();
			this.rotation = ((r % numR) + numR) % numR;
		}
		Position(Position p, Direction offset) {
			this.coord = new Coord(p.coord).add(offset.coordValue);
			this.numR = p.numR;
			this.rotation = ((p.rotation + offset.rotation % numR) + numR) % numR;
		}
		Position(Tetromino t) {
			this.coord = t.getLocation();
			this.numR = t.getShape().getNumRotations();
			this.rotation = ((t.getRotationIndex() % numR) + numR) % numR;
		}
		@Override public boolean equals(Object obj) {
			boolean result = false;
			if (obj instanceof Position) {
				Position o = (Position) obj;
				result = (o.coord.equals(coord) && o.rotation == rotation
						&& ((rotation + numR) % numR) == ((o.rotation + o.numR) % o.numR));
			}
			return result;
		}
		@Override public int hashCode() {
			return coord.hashCode() + rotation * 97;
		}
		double sqrdist(Position p) {
			return (coord.row() - p.coord.row())*(coord.row() - p.coord.row())
					+ (coord.col() - p.coord.col())*(coord.col() - p.coord.col());
		}
		@Override public int compareTo(Position o) {
			return (int) Math.round(sqrdist(o));
		}
		@Override
		public String toString() {
			return String.format("{%s, rotation: %d}", coord.toString(), rotation);
		}
	}

	protected static class PQPositionEntry implements Comparable<PQPositionEntry>{
		Position p;
		int priority;
		PQPositionEntry(Position p, int priority) {
			this.p = p;
			this.priority = priority;
		}
		@Override public int compareTo(PQPositionEntry o) {
			return (priority - o.priority);
		}
		@Override public int hashCode() {
			return p.hashCode();
		}
		@Override public boolean equals(Object obj) {
			return p.equals(obj);
		}
	}

	public boolean doesPathExist(Coord location, int rotation) {
		boolean pathFound = false;

		if (piece.isActive()) {
			//Check if the piece or it's goal position has blocks in it.
			boolean isBlocked = intersects();
			for (Coord c : piece.getBlockLocations()) {
				if (isBlockAtCoord(c)) {
					isBlocked = true;
					break;
				}
			}

			if (isBlocked) {
				//Piece or goal positions have blocks already.
				pathFound = false;
			} else {
				Board b = new Board(this);
				Tetromino t = new Tetromino(piece);
				b.setPiece(t);

				Position curPosition = new Position(t);
				Position goalPosition = new Position(location, rotation, t.getShape());

				PriorityQueue<PQPositionEntry> frontier = new PriorityQueue<>();
				HashSet<Position> visited = new HashSet<>();

				frontier.offer(new PQPositionEntry(curPosition, (int) curPosition.sqrdist(goalPosition)));
				visited.add(curPosition);

				while (!frontier.isEmpty() && !pathFound) {
					curPosition = frontier.poll().p;
					if (curPosition.equals(goalPosition)) {
						pathFound = true;
					} else {
						t.setLocation(curPosition.coord);
						t.setRotation(curPosition.rotation);
						for (Direction d : Direction.values()) {
							if (b.canPieceMove(d)) { //can move down
								Position nextPosition = new Position(curPosition, d);
								if (!visited.contains(nextPosition)) {
									frontier.offer(
										new PQPositionEntry(nextPosition, (int) nextPosition.sqrdist(goalPosition))
									);
									visited.add(nextPosition);
								}
							}
						}
					}
				}
			}
		}
		return pathFound;
	}
}
