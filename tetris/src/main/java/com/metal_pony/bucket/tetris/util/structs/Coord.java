package com.metal_pony.bucket.tetris.util.structs;

/**
 * Contains row, column coordinates.
 */
public class Coord {
	/**
	 * A Coord whose values cannot be changed.
	 */
	public static final class FinalCoord extends Coord {
		public static final String UNMODIFIABLE_ERR = "Cannot modify FinalCoord";

		public FinalCoord(int row, int col) { super(row, col); }
		public FinalCoord(Coord otherCoord) { super(otherCoord); }

		@Override public Coord add(int r, int c) 		{ throw new UnsupportedOperationException(UNMODIFIABLE_ERR); }
		@Override public Coord add(Coord...coords) 		{ throw new UnsupportedOperationException(UNMODIFIABLE_ERR); }
		@Override public Coord set(int row, int col) 	{ throw new UnsupportedOperationException(UNMODIFIABLE_ERR); }
		@Override public Coord set(Coord other) 		{ throw new UnsupportedOperationException(UNMODIFIABLE_ERR); }
	}

	/**
	 * Creates a deep copy of the given array.
	 */
	public static Coord[] copyFrom(Coord[] source) {
		Coord[] result = new Coord[source.length];
		for (int i = 0; i < source.length; i++) {
			result[i] = new Coord(source[i]);
		}
		return result;
	}

	/**
	 * Sets all Coords in the given array to the given value.
	 */
	public static void setAll(Coord[] arr, Coord value) {
		for (Coord coord : arr) {
			coord.set(value);
		}
	}

	/**
	 * Adds the given offset to all Coords in the array.
	 */
	public static void addToAll(Coord[] arr, Coord offset) {
		for (Coord coord : arr) {
			coord.add(offset);
		}
	}

	private int row;
	private int col;

	/**
	 * Creates a new Coord, (0, 0).
	 */
	public Coord() {
		this(0, 0);
	}

	/**
	 * Creates a new Coord with the given row and column coordinates.
	 */
	public Coord(int row, int col) {
		this.row = row;
		this.col = col;
	}

	/**
	 * Creates a new Coord with coordinates copied from the one given.
	 *
	 * @param other - Another Coord to copy from.
	 */
	public Coord(Coord other) {
		this.row = other.row;
		this.col = other.col;
	}

	/**
	 * Gets the row coordinate.
	 */
	public int row() {
		return this.row;
	}

	/**
	 * Gets the column coordinate.
	 */
	public int col() {
		return this.col;
	}

	/**
	 * Sets the coordinates to the ones specified.
	 */
	public Coord set(int row, int col) {
		this.row = row;
		this.col = col;
		return this;
	}

	/**
	 * Copies the given coordinates.
	 */
	public Coord set(Coord other) {
		this.row = other.row;
		this.col = other.col;
		return this;
	}

	/**
	 * Adds an arbitrary number of coordinates to this one.
	 *
	 * @param coords - Other coords whose positions should
	 * be added to this one.
	 */
	public Coord add(Coord... coords) {
		for (Coord c : coords){
			row += c.row;
			col += c.col;
		}
		return this;
	}

	/**
	 * Add the given coordinates to this one.
	 *
	 * @param toRow - Number of rows to add.
	 * @param toCol - Number of columns to add.
	 */
	public Coord add(int toRow, int toCol) {
		row += toRow;
		col += toCol;
		return this;
	}

	/**
	 * Gets the square distance between these coordinates and another.
	 */
	public int sqrDist(Coord other) {
		int rowDiff = row - other.row;
		int colDiff = col - other.col;
		return rowDiff*rowDiff + colDiff*colDiff;
	}

	@Override public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;

		if (obj instanceof Coord) {
			Coord _obj = (Coord) obj;
			return (this.row == _obj.row && this.col == _obj.col);
		}

		return false;
	}

	@Override public int hashCode() {
		return 31 * row + col;
	}

	@Override public String toString() {
		return String.format("(%d,%d)", row, col);
	}
}
