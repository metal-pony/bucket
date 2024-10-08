package com.metal_pony.bucket.tetris;

import com.metal_pony.bucket.tetris.util.structs.Piece;
import com.metal_pony.bucket.util.Array;
import com.metal_pony.bucket.util.event.Event;

public enum TetrisEvent {
	/**
	 * Called when a new game is initialized (not yet started).
	 * Attaches the state property.
	 */
	NEW_GAME,

	/**
	 * Called when the game is started.
	 * Attaches the state property.
	 */
	START,

	/**
	 * Called when the game is stopped.
	 * Attaches the state property.
	 */
	STOP,

	/**
	 * Called when the game is paused.
	 */
	PAUSE,

	/**
	 * Called when the game is resumed.
	 */
	RESUME,

	/**
	 * Called when the game is reset to its initial state.
	 * Attaches the state property.
	 */
	RESET,

	/**
	 * Called when the game is over.
	 * Attaches the state property.
	 */
	GAME_OVER,

	/**
	 * Called when the gravity effect is turned on.
	 */
	GRAVITY_ENABLED,

	/**
	 * Called when the gravity effect is turned off.
	 */
	GRAVITY_DISABLED,

	/**
	 * Called every game loop tick, which normally coincides with the gravity effect, if enabled.
	 * Attaches the state property.
	 */
	GAMELOOP,

	/**
	 * Called when the score is updated.
	 * Attaches the score property.
	 */
	SCORE_UPDATE,

	/**
	 * Called when the level is updated.
	 * Attaches the level property.
	 */
	LEVEL_CHANGE,

	/**
	 * Called when the lines are updated.
	 * Attaches an array of the row numbers that were cleared and the stats linesCleared and linesUntilNextLevel.
	 */
	LINE_CLEAR,

	/**
	 * Called when the board blocks are updated.
	 * Attaches an int[] representing the blocks on the board.
	 */
	BLOCKS,

	/**
	 * Called when the piece is shifted.
	 * Attaches the piece state.
	 */
	PIECE_SHIFT,

	/**
	 * Called when the piece is rotated.
	 * Attaches the piece state.
	 */
	PIECE_ROTATE,

	/**
	 * Called when a new piece is created.
	 * Attaches the piece state and a copy of the nextShapes queue.
	 */
	PIECE_CREATE,

	/**
	 * Called when the piece is placed as blocks on the board.
	 * Attaches the piece state and numPiecesDropped stat.
	 */
	PIECE_PLACED;

	/**
	 * Creates an Event with the piece data from the given game.
	 *
	 * @param game The game to get the piece data from.
	 * @return An Event with the piece data attached.
	 */
	public Event withPieceData(TetrisGame game) {
		Event event = new Event(this.name());
		event.addProperty("piece", new Piece(game.state.piece));
		return event;
	}

	/**
	 * Creates an Event with the block data from the given game.
	 *
	 * @param game The game to get the block data from.
	 * @return An Event with the block data attached.
	 */
	public Event withBlockData(TetrisGame game) {
		Event event = new Event(this.name());
		event.addProperty("blocks", Array.copy(game.state.board));
		return event;
	}

	/**
	 * Creates an Event with the state data from the given game.
	 *
	 * @param game The game to get the state data from.
	 * @return An Event with the state data attached.
	 */
	public Event withState(TetrisGame game) {
		Event event = new Event(this.name());
		event.addProperty("state", game.getState());
		return event;
	}

	/**
	 * Creates an Event with the given data attached.
	 *
	 * @param name The name of the property to attach.
	 * @param data The data to attach.
	 * @return An Event with the given data attached.
	 */
	public Event withData(String name, Object data) {
		Event event = new Event(this.name());
		event.addProperty(name, data);
		return event;
	}
}
