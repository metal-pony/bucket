package com.sparklicorn.bucket.games.tetris.util.structs;

/**
 * Represents the different shapes of pieces in Tetris. Each Shape is
 * associated with an integer value which can be retrieved with
 * <code>Shape.getShape(int)</code> or <code>someShape.value</code>.
 * <br><br>Also provides rotational information in the form of offset
 * coordinates.
 * These offsets describe the coordinates of the blocks that form the shape
 * relative to the pivotal location of the piece.
 *
 * Each array in the offsets relates to an orientation of the
 * shape.  These orientations are ordered such that the application of
 * successive indices causes the shape to successively rotate counter-clockwise.
 * Likewise, subtracting indices causes the shape to rotate clockwise.
 */
public enum Shape {
	O(1, new Coord[][]{	{new FinalCoord(0,-1),	new FinalCoord(0,0), 	new FinalCoord(1,-1),	new FinalCoord(1,0)},	}),
	I(2, new Coord[][]{	{new FinalCoord(0,0),	new FinalCoord(0,-1), 	new FinalCoord(0,-2),	new FinalCoord(0,1)},		// I
						{new FinalCoord(0,0),	new FinalCoord(-1,0), 	new FinalCoord(1,0),	new FinalCoord(2,0)}	}),
	S(3, new Coord[][]{	{new FinalCoord(0,0),	new FinalCoord(0,1), 	new FinalCoord(1,-1), 	new FinalCoord(1,0)},		// S
						{new FinalCoord(0,0), 	new FinalCoord(-1,0), 	new FinalCoord(0,1), 	new FinalCoord(1,1)}	}),
	Z(4, new Coord[][]{	{new FinalCoord(0,0), 	new FinalCoord(0,-1), 	new FinalCoord(1,0), 	new FinalCoord(1,1)},		// Z
						{new FinalCoord(0,0), 	new FinalCoord(0,1), 	new FinalCoord(1,0), 	new FinalCoord(-1,1)}	}),
	L(5, new Coord[][]{	{new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(0,1), 	new FinalCoord(1,-1)},	// L
						{new FinalCoord(-1,0), 	new FinalCoord(0,0), 	new FinalCoord(1,0), 	new FinalCoord(1,1)},
						{new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(0,1), 	new FinalCoord(-1,1)},
						{new FinalCoord(-1,-1), new FinalCoord(-1,0), 	new FinalCoord(0,0), 	new FinalCoord(1,0)}	}),
	J(6, new Coord[][]{	{new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(0,1), 	new FinalCoord(1,1)},		// J
						{new FinalCoord(-1,0), 	new FinalCoord(-1,1), 	new FinalCoord(0,0), 	new FinalCoord(1,0)},
						{new FinalCoord(-1,-1), new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(0,1)},
						{new FinalCoord(-1,0),	new FinalCoord(0,0),	new FinalCoord(1,-1), 	new FinalCoord(1,0)}	}),
	T(7, new Coord[][]{	{new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(0,1), 	new FinalCoord(1,0)},		// T
						{new FinalCoord(-1,0), 	new FinalCoord(0,0), 	new FinalCoord(0,1), 	new FinalCoord(1,0)},
						{new FinalCoord(-1,0), 	new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(0,1)},
						{new FinalCoord(-1,0), 	new FinalCoord(0,-1), 	new FinalCoord(0,0), 	new FinalCoord(1,0)}	});

	public final int value;
	public final Coord[][] rotationOffsets;

	private Shape(int v, Coord[][] offsets) {
		this.value = v;
		this.rotationOffsets = offsets;
	}

	/**
	 * Returns the number of rotations for this shape.
	 * @return Number of rotations.
	 */
	public int getNumRotations() {
		return rotationOffsets.length;
	}

	/**
	 * Returns the block offsets associated with the shape's rotation.
	 * @param rotationIndex - An index that describes the rotation of the
	 * shape. Increase to retrieve counter-clockwise offsets; decrease to
	 * retrieve clockwise offsets.
	 * @return The offset coordinates for the given rotation.
	 */
	public Coord[] getRotation(int rotationIndex) {
		int len = rotationOffsets.length;
		return rotationOffsets[((rotationIndex % len) + len) % len];
	}

	public static final int NUM_SHAPES = Shape.values().length;
	private static final Shape[] shapeMap;
	static {
		shapeMap = new Shape[NUM_SHAPES + 1];
		for (Shape s : Shape.values()) {
			shapeMap[s.value] = s;
		}
	}

	/**
	 * Gets the shape associated with the given index.
	 * @param index - The shape's index.
	 * @return The shape associated with the given index.
	 */
	public static Shape getShape(int index) {
		return shapeMap[index];
	}

	/**
	 * Returns the number of rotations for the given shape.
	 * @param s - The shape.
	 * @return The number of rotations for the given shape.
	 */
	public static int getNumRotations(Shape s) {
		return s.rotationOffsets.length;
	}
}
