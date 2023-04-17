package com.sparklicorn.bucket.games.tetris.util.structs;

import com.sparklicorn.bucket.games.tetris.util.structs.Coord.FinalCoord;

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
	O(1,
		0,-1, 0,0, 1,-1, 1,0
	),
	I(2,
		0,0, 0,-1, 0,-2, 0,1,
		0,0, -1,0, 1,0, 2,0
	),
	S(3,
		0,0, 0,1, 1,-1, 1,0,
		0,0, -1,0, 0,1, 1,1
	),
	Z(4,
		0,0, 0,-1, 1,0, 1,1,
		0,0, 0,1, 1,0, -1,1
	),
	L(5,
		0,-1, 0,0, 0,1, 1,-1,
		-1,0, 0,0, 1,0, 1,1,
		0,-1, 0,0, 0,1, -1,1,
		-1,-1, -1,0, 0,0, 1,0
	),
	J(6,
		0,-1, 0,0, 0,1, 1,1,
		-1,0, -1,1, 0,0, 1,0,
		-1,-1, 0,-1, 0,0, 0,1,
		-1,0, 0,0, 1,-1, 1,0
	),
	T(7,
		0,-1, 0,0, 0,1, 1,0,
		-1,0, 0,0, 0,1, 1,0,
		-1,0, 0,-1, 0,0, 0,1,
		-1,0, 0,-1, 0,0, 1,0
	);

	private FinalCoord[][] buildOffsets(int... vals) {
		int numRotations = vals.length / 8;
		FinalCoord[][] result = new FinalCoord[numRotations][];
		for (int rotationIndex = 0; rotationIndex < numRotations; rotationIndex++) {
			result[rotationIndex] = new FinalCoord[4];
			for (int blockIndex = 0; blockIndex < 4; blockIndex++) {
				result[rotationIndex][blockIndex] = new FinalCoord(
					vals[rotationIndex * 8 + blockIndex * 2],
					vals[rotationIndex * 8 + blockIndex * 2 + 1]
				);
			}
		}

		return result;
	}

	public static final int NUM_SHAPES = Shape.values().length;

	private static final Shape[] shapeMap = new Shape[NUM_SHAPES + 1];
	static {
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

	public final int value;
	public final FinalCoord[][] rotationOffsets;

	private Shape(int value, int... blockOffsetsPerRotation) {
		this.value = value;
		this.rotationOffsets = buildOffsets(blockOffsetsPerRotation);
	}

	/**
	 * Returns the number of rotations for this shape.
	 * @return Number of rotations.
	 */
	public int getNumRotations() {
		return rotationOffsets.length;
	}

	public int rotationIndex(int rotations) {
		int numRotations = getNumRotations();
		return ((rotations % numRotations) + numRotations) % numRotations;
	}

	/**
	 * Returns the block offsets associated with the shape's rotation.
	 *
	 * @param rotationIndex - An index of rotation of the shape.
	 * Positive indices represent a counter-clockwise rotation, and negative are clockwise.
	 * @return The block offsets for the given shape's rotation.
	 */
	public Coord[] getRotation(int rotationIndex) {
		return rotationOffsets[rotationIndex(rotationIndex)];
	}
}
