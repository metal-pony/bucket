package com.sparklicorn.bucket.games.tetris.util.structs;

import com.sparklicorn.bucket.games.tetris.util.structs.Coord.FinalCoord;

public class Move {
	static final class FinalMove extends Move {
		static final String NO_MODIFY = "Cannot modify final move";

		FinalMove(FinalCoord offset, int rotation) {
			super(offset, rotation);
		}

		@Override
		public void rotate(Move direction) {
			throw new UnsupportedOperationException(NO_MODIFY);
		}

		@Override
		public void add(Move other) {
			throw new UnsupportedOperationException(NO_MODIFY);
		}
	}

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
		this.offset.add(other.offset);
		this.rotation += other.rotation;
	}

	public void rotate(Move direction) {
		if (direction != Move.CLOCKWISE && direction != Move.COUNTERCLOCKWISE) {
			throw new IllegalArgumentException(
				"Direction must be CLOCKWISE or COUNTERCLOCKWISE"
			);
		}

		this.rotation += direction.rotation;
	}
}
