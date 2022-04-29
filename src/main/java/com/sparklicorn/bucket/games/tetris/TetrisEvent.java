package com.sparklicorn.bucket.games.tetris;

public enum TetrisEvent {

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

	BLOCKS,
	PIECE_SHIFT,
	PIECE_ROTATE,
	PIECE_CREATE,
	PIECE_PLACED;

	public final String name;

	private TetrisEvent() {
		this.name = name();
	}
}
