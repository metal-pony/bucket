package com.sparklicorn.bucket.games.tetris.util.structs;

public enum Direction {
	UP(new FinalCoord(-1, 0), 0),
	DOWN(new FinalCoord(1, 0), 0),
	LEFT(new FinalCoord(0, -1), 0),
	RIGHT(new FinalCoord(0, 1), 0),
	CLOCKWISE(new FinalCoord(0, 0), -1),
	COUNTERCLOCKWISE(new FinalCoord(0, 0), 1);

	public final Coord coordValue;
	public final int rotation;

	private Direction(Coord value, int r) {
		this.coordValue = value;
		this.rotation = r;
	}
}
