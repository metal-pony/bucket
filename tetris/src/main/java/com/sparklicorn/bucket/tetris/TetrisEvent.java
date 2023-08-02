package com.sparklicorn.bucket.tetris;

// TODO #70 Document event meanings
public enum TetrisEvent {
	NEW_GAME,
	START,
	STOP,
	PAUSE,
	RESUME,

	/**
	 * Called when the game is reset to its initial state.
	 */
	RESET,
	GAMELOOP,

	GAME_OVER,
	SCORE_UPDATE,
	LEVEL_CHANGE,
	LINE_CLEAR,

	BLOCKS,
	PIECE_SHIFT,
	PIECE_ROTATE,
	PIECE_CREATE,
	PIECE_PLACED;

	// public final String name;

	// private TetrisEvent() {
	// 	this.name = name();
	// }
}
