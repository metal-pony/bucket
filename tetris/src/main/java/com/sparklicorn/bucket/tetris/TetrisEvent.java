package com.sparklicorn.bucket.tetris;

public enum TetrisEvent {
	// TODO add descriptions
	NEW_GAME,
	START,
	STOP,
	PAUSE,
	RESUME,
	GAMELOOP,

	GAME_OVER,
	SCORE_UPDATE,
	LEVEL_CHANGE,
	LINE_CLEAR,

	BLOCKS, // TODO what is this used for?
	PIECE_SHIFT,
	PIECE_ROTATE,
	PIECE_CREATE,
	PIECE_PLACED;

	// public final String name;

	// private TetrisEvent() {
	// 	this.name = name();
	// }
}
