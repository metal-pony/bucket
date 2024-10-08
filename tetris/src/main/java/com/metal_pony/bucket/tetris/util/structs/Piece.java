package com.metal_pony.bucket.tetris.util.structs;

import java.util.function.Consumer;

import com.metal_pony.bucket.tetris.TetrisState;
import com.metal_pony.bucket.util.Array;

/**
 * Represents a piece on the Tetris board.
 */
public class Piece {
    protected Position position;
    protected Coord[] blockCoords;
    private Shape shape;
    private boolean isActive;

    /**
     * Creates a new piece with the given location and shape.
     *
     * @param location The location of the piece.
     * @param shape The shape of the piece.
     */
    public Piece(Coord location, Shape shape) {
        reset(location, shape);
    }

    /**
     * Creates a new piece with the given position and shape.
     *
     * @param position The position of the piece.
     * @param shape The shape of the piece.
     */
    public Piece(Position position, Shape shape) {
        this.shape = shape;
        this.isActive = true;
        position(position);
        updateBlockCoords();
    }

    /**
     * Creates a new piece that is a copy of the given piece.
     *
     * @param other The piece to copy.
     */
    public Piece(Piece other) {
        this.position = new Position(other.position);
        this.shape = other.shape;
        this.isActive = other.isActive;
        this.blockCoords = Coord.copyFrom(other.blockCoords);
    }

    /**
     * Returns the shape of the piece.
     */
    public Shape shape() {
        return shape;
    }

    /**
     * Sets the shape of the piece and updates the block coordinates.
     *
     * @param shape The new shape of the piece.
     * @return The new shape of the piece.
     */
    public Shape shape(Shape shape) {
        this.shape = shape;
        updateBlockCoords();
        return this.shape;
    }

    /**
     * Returns the position of the piece.
     */
    public Position position() {
        return new Position(position);
    }

    /**
     * Sets the position of the piece and updates the block coordinates.
     *
     * @param position The new position of the piece.
     * @return The new position of the piece.
     */
    public Position position(Position position) {
        this.position = new Position(position);
        updateBlockCoords();
        return this.position;
    }

    /**
     * Resets the piece to the given location and shape.
     *
     * @param location The location to reset the piece to.
     * @param shape The shape to reset the piece to.
     */
    public void reset(Coord location, Shape shape) {
        this.shape = shape;
        position(new Position(location, 0, shape.getNumRotations()));
        updateBlockCoords();
        enable();
    }

    public void shapeShift(Shape shape) {
        this.shape = shape;
        position(new Position(position.offset(), 0, shape.getNumRotations()));
        updateBlockCoords();
    }

    /**
     * Returns whether or not the piece is active.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets the piece as inactive.
     */
    public void disable() {
        isActive = false;
    }

    /**
     * Sets the piece as active.
     */
    public void enable() {
        isActive = true;
    }

    /**
     * Returns a copy of the block coordinates of the piece.
     */
    public Coord[] blockCoords() {
        return Coord.copyFrom(blockCoords);
    }

    /**
	 * Moves the piece by the given offset and rotation, then updates the block coordinates.
	 * Legality of the move is not checked.
	 *
	 * @param move The move to apply to the piece position.
	 */
    public void move(Move move) {
        position.add(move);
        updateBlockCoords();
    }

    /**
     * Returns whether or not the piece intersects with any blocks on the given state.
     *
     * @param state The state to check for intersections.
     * @return Whether or not the piece intersects with any blocks on the given state.
     */
    public boolean intersects(TetrisState state) {
        for (Coord coord : blockCoords) {
            if (!state.isCellEmpty(coord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes the given consumer for each block (row, column) coordinates of the piece.
     *
     * @param consumer The consumer to execute for each block (row, column) coordinates of the piece.
     */
    public void forEachCell(Consumer<Coord> consumer) {
        for (Coord coord : blockCoords) {
            consumer.accept(new Coord(coord));
        }
    }

    /**
     * Updates the block coordinates based on the current position and shape.
     * This method is called automatically when the position or shape is changed.
     */
    private void updateBlockCoords() {
        if (blockCoords == null) {
            blockCoords = Array.fillWithFunc(new Coord[4], (i) -> new Coord());
        }

        int rotationIndex = shape.rotationIndex(position.rotation());
        for (int i = 0; i < blockCoords.length; i++) {
            blockCoords[i].set(position.offset());
            blockCoords[i].add(shape.rotationOffsets[rotationIndex][i]);
        }
    }
}
