package com.sparklicorn.bucket.games.tetris;

import java.util.Arrays;

import com.sparklicorn.bucket.games.tetris.util.structs.Coord;
import com.sparklicorn.bucket.games.tetris.util.structs.Shape;

public class Tetromino {

	/** Tetrominoes have 4 blocks.*/
	public static final int NUM_BLOCKS = 4;

	private Shape shape;
	private int rotationIndex;
	private boolean isActive;
	private Coord location, blockLocations[];

	/**
	 * Create a new Tetromino with the given shape, location, and
	 * default rotation.
	 * @param shape - The shape this Tetromino should assume.
	 * @param location - The location of this Tetromino.
	 */
	public Tetromino(Shape shape, Coord location) {
		this(shape, location, 0);
	}

	/**
	 * Creates a new Tetromino with the given shape, location, and rotation.
	 * @param shape - The shape this Tetromino should assume.
	 * @param location - The location of this Tetromino.
	 * @param rotationIndex - The rotation of this Tetromino.
	 */
	public Tetromino(Shape shape, Coord location, int rotationIndex) {
		reset(shape, location, rotationIndex);
	}

	/**
	 * Creates a new Tetromino that is a copy of the one given.
	 * @param other - Another Tetromino that this should copy.
	 */
	public Tetromino(Tetromino other) {
		this(other.shape, other.location, other.rotationIndex);
	}

	/**
	 * Resets this piece to the specified shape, location, and rotation.
	 * @param newShape - The new shape this should take.
	 * @param newLocation - The location to reset to.
	 * @param rotationIndex - The rotation of this piece.
	 * @return Itself for convenience.
	 */
	public Tetromino reset(Shape newShape, Coord newLocation, int rotationIndex) {
		this.shape = newShape;
		this.rotationIndex = rotationIndex;
		this.isActive = true;
		this.location = new Coord(newLocation);

		if (this.blockLocations == null) {
			this.blockLocations = new Coord[NUM_BLOCKS];

			for (int i = 0; i < NUM_BLOCKS; i++) {
				this.blockLocations[i] = new Coord(location);
			}
		}

		setBlockLocations();
		return this;
	}

	/**
	 * Resets this piece to the specified shape, location, and
	 * default rotation.
	 * @param newShape - The new shape this should take.
	 * @param newLocation - The location to reset to.
	 * @return Itself for convenience.
	 */
	public Tetromino reset(Shape newShape, Coord newLocation) {
		return reset(newShape, newLocation, 0);
	}

	/**
	 * Returns this piece's location.
	 * @return Row,Column coordinates.
	 */
	public Coord getLocation() {
		return new Coord(location);
	}

	/**
	 * Returns the current positions of the blocks that make up this piece.
	 * @return Coordinates of the blocks that make up this piece.
	 */
	public Coord[] getBlockLocations() {
		Coord blocks[] = new Coord[NUM_BLOCKS];

		for (int i = 0; i < NUM_BLOCKS; i++) {
			blocks[i] = new Coord(blockLocations[i]);
		}

		return blocks;
	}

	/**
	 * Returns the current rotation of this piece.
	 * @return The rotation of this piece.
	 */
	public int getRotationIndex() {
		return rotationIndex;
	}

	/**
	 * Returns the current shape of this piece.
	 * @return The shape of this piece.
	 */
	public Shape getShape() {
		return shape;
	}

	/**
	 * Returns whether this piece is currently active.
	 * A piece that is active is movable.  A piece that is not active
	 * can not be moved until calling <code>reset</code>.
	 * @return True if this piece is flagged as active; otherwise false.
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Renders this piece unable to move until it is reset.
	 * @return Itself for convenience.
	 */
	public Tetromino kill() {
		isActive = false;
		return this;
	}

	/**
	 * Shifts this piece with the specified offset coordinates.
	 * @param offsetCoords - Offset coordinates.
	 * @return True if the shift was successful; otherwise false.
	 */
	public boolean shift(Coord offsetCoords) {
		boolean result = false;

		if (isActive) {
			location.add(offsetCoords);

			for (Coord b : blockLocations) {
				b.add(offsetCoords);
			}
		}

		return result;
	}

	/**
	 * Attempts to rotate this piece clockwise.
	 * @return True if the rotation was successful; otherwise false.
	 */
	public boolean rotateClockwise() {
		boolean result = false;

		if (isActive) {
			rotationIndex--;
			setBlockLocations();
			result = true;
		}

		return result;
	}

	/**
	 * Attempts to rotate this piece counter-clockwise.
	 * @return True if the rotation was successful; otherwise false.
	 */
	public boolean rotateCounterClockwise() {
		boolean result = false;

		if (isActive) {
			rotationIndex++;
			setBlockLocations();
			result = true;
		}

		return result;
	}

	//Calculates the locations of the blocks which make up this piece.
	private void setBlockLocations() {
		Coord[] offsets = shape.getRotation(rotationIndex);

		for (int i = 0; i < NUM_BLOCKS; i++) {
			blockLocations[i].set(location).add(offsets[i]);
		}
	}

	/**
	 * Returns the block coordinates of where the piece would be if it were
	 * shifted and/or rotated by the specified amounts.
	 * <br>This does not change the piece's location or orientation.
	 * @param offset - Row/Column to offset by.
	 * @param rotationOffset - Number of times to rotate. Negative for
	 * Clockwise, Positive for CCW.
	 * @return Coordinates of the would-be piece if it were shifted and/or
	 * rotated.
	 */
	public Coord[] getNewPositions(Coord offset, int rotationOffset) {
		Coord[] coords = new Coord[NUM_BLOCKS];

		int rotation = rotationIndex + rotationOffset;
		Coord[] blockOffsets = shape.getRotation(rotation);

		for (int i = 0; i < NUM_BLOCKS; i++) {
			coords[i] = new Coord(location).add(blockOffsets[i], offset);
		}

		return coords;
	}

	@Override public String toString() {
		return String.format(
			"{Shape: %s, Rotation: %d, Coords: %s}",
			shape.toString(), rotationIndex, Arrays.toString(blockLocations)
		);
	}

	/**
	 * A Tetromino is equal to another if they share the same block locations.
	 * The locations must also be in the same order in the underlying array
	 * (but the user should not need to worry about that.)
	 */
	@Override public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof Tetromino) {
			Tetromino o = (Tetromino) obj;
			if (blockLocations[0].equals(o.blockLocations[0])
					&& blockLocations[1].equals(o.blockLocations[1])
					&& blockLocations[2].equals(o.blockLocations[2])
					&& blockLocations[3].equals(o.blockLocations[3])) {
				result = true;
				//if block locations are the same, then they must also be the same
				// shape and have the same rotation index.
			}
		}
		return result;
	}

	@Override public int hashCode() {
		int result = location.hashCode();
		result += 59 * rotationIndex;
		result += 73 * shape.value;
		return result;
	}

	/**
	 * Sets the location of this piece.
	 * If the piece is flagged as inactive (<code>isActive()</code> returns
	 * <code>false</code>), then this operation will fail.
	 * @param newLocation - The new coordinates.
	 * @return True if the piece was moved; otherwise false.
	 */
	public boolean setLocation(Coord newLocation) {
		boolean result = false;

		if (isActive) {
			this.location.set(newLocation);
			setBlockLocations();
			result = true;
		}

		return result;
	}

	/**
	 * Sets the rotation of this piece.
	 * If the piece is flagged as inactive (<code>isActive()</code> returns
	 * <code>false</code>), then this operation will fail.
	 * @param newRotation - The new rotation.
	 * @return True if the piece was moved; otherwise false.
	 */
	public boolean setRotation(int newRotation) {
		boolean result = false;

		if (isActive) {
			this.rotationIndex = newRotation;
			setBlockLocations();
			result = true;
		}

		return result;
	}
}
