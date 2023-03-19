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

		FinalMove(FinalCoord offset, int rotation) {
			super(offset, rotation);
		}

		@Override
		public Coord offset() {
			return new Coord(super.offset());
		}

		@Override
		public void rotate(Move direction) {
			throw new UnsupportedOperationException(NO_MODIFY);
		}

		@Override
		public void add(Move other) {
			throw new UnsupportedOperationException(NO_MODIFY);
		}

		@Override
		public void add(Coord offset, int rotation) {
			throw new UnsupportedOperationException(NO_MODIFY);
		}

		@Override
		public void rotateClockwise() {
			throw new UnsupportedOperationException(NO_MODIFY);
		}

		@Override
		public void rotateCounterClockwise() {
			throw new UnsupportedOperationException(NO_MODIFY);
		}
	}

	public static final Move STAND = new FinalMove(new FinalCoord(0, 0), 0);
	public static final Move UP = new FinalMove(new FinalCoord(-1, 0), 0);
	public static final Move DOWN = new FinalMove(new FinalCoord(1, 0), 0);
	public static final Move LEFT = new FinalMove(new FinalCoord(0, -1), 0);
	public static final Move RIGHT = new FinalMove(new FinalCoord(0, 1), 0);
	public static final Move CLOCKWISE = new FinalMove(new FinalCoord(0, 0), -1);
	public static final Move COUNTERCLOCKWISE = new FinalMove(new FinalCoord(0, 0), 1);

	private Coord offset;
	private int rotation;

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

	public void add(Move other) {
		add(other.offset, other.rotation);
	}

	public void add(Coord offset, int rotation) {
		this.offset.add(offset);
		this.rotation += rotation;
	}

	public void rotateClockwise() {
		rotation--;
	}

	public void rotateCounterClockwise() {
		rotation++;
	}

	public void rotate(Move direction) {
		if (direction != Move.CLOCKWISE && direction != Move.COUNTERCLOCKWISE) {
			throw new IllegalArgumentException(
				"Direction must be CLOCKWISE or COUNTERCLOCKWISE"
			);
		}

		this.rotation += direction.rotation;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;

		if (other instanceof Move) {
			Move _other = (Move) other;
			return (this.offset.equals(_other.offset) && this.rotation == _other.rotation);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return offset.hashCode() * 31 + rotation;
	}

	@Override
	public String toString() {
		return String.format("Move{offset: %s, rotation: %d}", offset.toString(), rotation);
	}
}
