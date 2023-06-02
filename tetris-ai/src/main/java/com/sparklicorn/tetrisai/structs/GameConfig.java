package com.sparklicorn.tetrisai.structs;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.util.event.Event;
import com.sparklicorn.tetrisai.game.AiTetris;
import com.sparklicorn.tetrisai.game.GenericRanker;
import com.sparklicorn.tetrisai.game.ITetrisStateRanker;

public record GameConfig(
	int rows,
	int cols,
	boolean useEvents,
	Map<TetrisEvent, List<Consumer<Event>>> eventListeners,
	ITetrisStateRanker stateRanker,
	int numGames,
	// TODO #13 Convert to int to specify how many pieces should be considered (up to a max)
	boolean useLookAhead
) {
	public static final boolean DEFAULT_USE_EVENTS = true;
	public static final int DEFAULT_NUM_GAMES = 1;
	public static final boolean DEFAULT_LOOK_AHEAD = false;

	/**
	 * Creates a new game configuration with defaults.
	 */
	public GameConfig() {
		this(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			DEFAULT_USE_EVENTS,
			null,
			new GenericRanker(),
			DEFAULT_NUM_GAMES,
			DEFAULT_LOOK_AHEAD
		);
	}

	/**
	 * Creates a new game configuration with the given options.
	 */
	public GameConfig(
		boolean useEvents,
		Map<TetrisEvent, List<Consumer<Event>>> eventListeners,
		ITetrisStateRanker ranker
	) {
		this(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			useEvents,
			eventListeners,
			ranker,
			DEFAULT_NUM_GAMES,
			DEFAULT_LOOK_AHEAD
		);
	}

	/**
	 * Creates a new game configuration with the given options.
	 */
	public GameConfig(
		boolean useEvents,
		Map<TetrisEvent, List<Consumer<Event>>> eventListeners,
		ITetrisStateRanker ranker,
		int numGames
	) {
		this(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			useEvents,
			eventListeners,
			ranker,
			numGames,
			DEFAULT_LOOK_AHEAD
		);
	}

	public GameConfig(ITetrisStateRanker ranker) {
		this(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			DEFAULT_USE_EVENTS,
			null,
			ranker,
			DEFAULT_NUM_GAMES,
			DEFAULT_LOOK_AHEAD
		);
	}
}
