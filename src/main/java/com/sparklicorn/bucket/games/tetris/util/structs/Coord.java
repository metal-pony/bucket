package com.sparklicorn.bucket.games.tetris.util.structs;

/** A Coord represents (row, column) coordinates.*/
public class Coord {

	private int row, col;

	/**
	 * Creates a new Coord with the given row and column coordinates.
	 * @param r - Row coordinate.
	 * @param c - Column coordinate.
	 */
	public Coord(int r, int c) {
		this.row = r;
		this.col = c;
	}

	/**
	 * Creates a new Coord with coordinates copied from the one given.
	 * @param otherCoord - Another Coord to copy from.
	 */
	public Coord(Coord otherCoord) {
		this.row = otherCoord.row;
		this.col = otherCoord.col;
	}

	/**
	 * Returns the row coordinate.
	 * @return The row.
	 */
	public int row() {
		return this.row;
	}

	/**
	 * Returns the column coordinate.
	 * @return The column.
	 */
	public int col() {
		return this.col;
	}

	/**
	 * Sets the coordinates to the ones specified.
	 * @param row - The row.
	 * @param col - The column.
	 * @return Returns itself for convenience.
	 */
	public Coord set(int row, int col) {
		this.row = row;
		this.col = col;

		return this;
	}

	/**
	 * Sets the coordinate to the ones specified.
	 * @param other - Another coordinate to copy from.
	 * @return Returns itself for convenience.
	 */
	public Coord set(Coord other) {
		this.row = other.row;
		this.col = other.col;

		return this;
	}

	/**
	 * Adds an arbitrary number of coordinates to this one.
	 * @param coords - Other coords whose positions should
	 * be added to this one.
	 * @return Returns itself for convenience.
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
	 * @param r - Number of rows to add.
	 * @param c - Number of columns to add.
	 * @return Returns itself for convenience.
	 */
	public Coord add(int r, int c) {
		this.row += r;
		this.col += c;

		return this;
	}

	@Override public boolean equals(Object obj) {
		boolean result = false;

		if (obj != null) {
			if (obj instanceof Coord) {
				Coord o = (Coord) obj;
				if (row == o.row && col == o.col) {
					result = true;
				}
			}
		}

		return result;
	}

	@Override public int hashCode() {
		return row * 37 + col;
	}

	@Override public String toString() {
		return String.format("(%d,%d)", row, col);
	}
}
