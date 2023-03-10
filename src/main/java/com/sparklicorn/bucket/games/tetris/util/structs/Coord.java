package com.sparklicorn.bucket.games.tetris.util.structs;

/**
 * Contains row, column coordinates.
 */
public class Coord {
	public static final class FinalCoord extends Coord {
		public FinalCoord(int r, int c) { super(r, c); }
		public FinalCoord(Coord otherCoord) { super(otherCoord); }

		@Override public void add(int r, int c) { throw new UnsupportedOperationException(); }
		@Override public void add(Coord...coords) { throw new UnsupportedOperationException(); }
		@Override public void set(int row, int col) { throw new UnsupportedOperationException(); }
		@Override public void set(Coord other) { throw new UnsupportedOperationException(); }
	}

	private int row;
	private int col;

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
	public void set(int row, int col) {
		this.row = row;
		this.col = col;
	}

	/**
	 * Sets the coordinate to the ones specified.
	 *
	 * @param other - Another coordinate to copy from.
	 */
	public void set(Coord other) {
		this.row = other.row;
		this.col = other.col;
	}

	/**
	 * Adds an arbitrary number of coordinates to this one.
	 *
	 * @param coords - Other coords whose positions should
	 * be added to this one.
	 */
	public void add(Coord... coords) {
		for (Coord c : coords){
			row += c.row;
			col += c.col;
		}
	}

	/**
	 * Add the given coordinates to this one.
	 *
	 * @param toRow - Number of rows to add.
	 * @param toCol - Number of columns to add.
	 */
	public void add(int toRow, int toCol) {
		this.row += toRow;
		this.col += toCol;
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
