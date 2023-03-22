package com.sparklicorn.bucket.games.tetris.util.structs;

public class Position extends Move implements Comparable<Position> {
	public static int MAX_ROTATION = 8;

	/**
	 * Validates that the given maxRotation is within bounds.
	 * Throws and error if invalid.
	 */
	protected static void validateMaxRotation(int maxRotation) {
		if (maxRotation < 0) {
			throw new IllegalArgumentException("Max rotation must be non-negative");
		}
		if (maxRotation > MAX_ROTATION) {
			throw new IllegalArgumentException("Max rotation must be smaller than " + MAX_ROTATION);
		}
	}

	/**
	 * Maximum number of rotations. 0-based, so the max value `rotation` can be is
	 * this `maxRotation - 1`.
	 */
	protected int maxRotation;

	/**
	 * Creates a new Position with the given location and rotation.
	 */
	public Position(Coord location, int rotation) {
		this(location, rotation, 1);
	}

	/**
	 * Creates a new Position with the given location and rotation.
	 */
	public Position(Coord location, int rotation, int maxRotation) {
		super(location, rotation);
		this.maxRotation = maxRotation;
		validateMaxRotation(maxRotation);
		normalizeRotation();
	}

	/**
	 * Creates a new Position that is a copy of the given.
	 */
	public Position(Position other) {
		this.offset = new Coord(other.offset);
		this.rotation = other.rotation;
		this.maxRotation = other.maxRotation;
	}

	/**
	 * Gets the location coordinates.
	 */
	public Coord location() {
		return offset;
	}

	@Override
	public void rotateClockwise() {
		super.rotateClockwise();
		normalizeRotation();
	}

	@Override
	public void rotateCounterClockwise() {
		super.rotateCounterClockwise();
		normalizeRotation();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;

		if (other instanceof Position) {
			Position _other = (Position) other;
			return (
				offset.equals(_other.offset) &&
				_other.rotation == rotation &&
				maxRotation == _other.maxRotation
			);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + 37 * maxRotation;
	}

	/**
	 * Gets the square distance between this position and another.
	 */
	public int sqrdist(Position other) {
		return offset.sqrDist(other.offset);
	}

	@Override
	public int compareTo(Position other) {
		return sqrdist(other);
	}

	@Override
	public String toString() {
		return String.format(
			"Position{location: %s, rotation: %d, maxRotation: %d}",
			offset.toString(),
			rotation,
			maxRotation
		);
	}

	/**
	 * Constrains rotation between 0 and maxRotation if current value is out of bounds.
	 */
	private void normalizeRotation() {
		if (rotation < 0 || rotation > maxRotation) {
			rotation = ((rotation % maxRotation) + maxRotation) % maxRotation;
		}
	}
}
