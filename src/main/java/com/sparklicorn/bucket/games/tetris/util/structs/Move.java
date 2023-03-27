package com.sparklicorn.bucket.games.tetris.util.structs;

import com.sparklicorn.bucket.games.tetris.util.structs.Coord.FinalCoord;

/**
 * Simple data class that represents game piece movement, i.e.
 * row, column, rotation changes.
 */
public class Move {
	/**
	 * A constant Move. Offset and rotation values cannot be changed.
	 */
	public static final class FinalMove extends Move {
		static final String NO_MODIFY = "Cannot modify final move";

		private FinalMove(int row, int col, int rotation) {
			offset = new FinalCoord(row, col);
			this.rotation = rotation;
		}

		@Override
		public Coord offset() {
			return new Coord(super.offset());
		}

		@Override public Move rotate(Move direction) 			{ throw new UnsupportedOperationException(NO_MODIFY); }
		@Override public Move add(Move other) 					{ throw new UnsupportedOperationException(NO_MODIFY); }
		@Override public Move add(Coord offset, int rotation) 	{ throw new UnsupportedOperationException(NO_MODIFY); }
		@Override public Move rotateClockwise() 				{ throw new UnsupportedOperationException(NO_MODIFY); }
		@Override public Move rotateCounterClockwise() 			{ throw new UnsupportedOperationException(NO_MODIFY); }
	}

	public static final Move STAND = new FinalMove(0, 0, 0);
	public static final Move UP = new FinalMove(-1, 0, 0);
	public static final Move DOWN = new FinalMove(1, 0, 0);
	public static final Move LEFT = new FinalMove(0, -1, 0);
	public static final Move RIGHT = new FinalMove(0, 1, 0);
	public static final Move CLOCKWISE = new FinalMove(0, 0, -1);
	public static final Move COUNTERCLOCKWISE = new FinalMove(0, 0, 1);

	protected Coord offset;
	protected int rotation;

	public Move() {
		this.offset = new Coord();
		this.rotation = 0;
	}

	public Move(Coord offset, int rotation) {
		this.offset = offset;
		this.rotation = rotation;
	}

	public Move(Move other) {
		this.offset = new Coord(other.offset);
		this.rotation = other.rotation;
	}

	public Coord offset() {
		return this.offset;
	}

	public int rowOffset() {
		return this.offset.row();
	}

	public int colOffset() {
		return this.offset.col();
	}

	public int rotation() {
		return this.rotation;
	}

	public Move add(Move other) {
		add(other.offset, other.rotation);
		return this;
	}

	public Move add(Coord offset, int rotation) {
		this.offset.add(offset);
		this.rotation += rotation;
		return this;
	}

	public Move rotateClockwise() {
		rotation--;
		return this;
	}

	public Move rotateCounterClockwise() {
		rotation++;
		return this;
	}

	public Move rotate(Move direction) {
		if (direction != Move.CLOCKWISE && direction != Move.COUNTERCLOCKWISE) {
			throw new IllegalArgumentException(
				"Direction must be CLOCKWISE or COUNTERCLOCKWISE"
			);
		}

		this.rotation += direction.rotation;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;

		if (other instanceof Move) {
			Move _other = (Move) other;
			return (
				this.offset.equals(_other.offset) &&
				this.rotation == _other.rotation
			);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return offset.hashCode() * 31 + rotation;
	}

	@Override
	public String toString() {
		return String.format(
			"Move{offset: %s, rotation: %d}",
			offset.toString(),
			rotation
		);
	}
}
