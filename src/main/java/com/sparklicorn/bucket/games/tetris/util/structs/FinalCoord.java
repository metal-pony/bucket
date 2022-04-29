package com.sparklicorn.bucket.games.tetris.util.structs;

/** A Coord object whose coordinates cannot be changed.*/
public final class FinalCoord extends Coord {
	public FinalCoord(int r, int c) { super(r, c); }
	public FinalCoord(Coord otherCoord) { super(otherCoord); }

	@Override public Coord add(int r, int c) { throw new UnsupportedOperationException(); }
	@Override public Coord add(Coord...coords) { throw new UnsupportedOperationException(); }
	@Override public Coord set(int row, int col) { throw new UnsupportedOperationException(); }
	@Override public Coord set(Coord other) { throw new UnsupportedOperationException(); }
}
