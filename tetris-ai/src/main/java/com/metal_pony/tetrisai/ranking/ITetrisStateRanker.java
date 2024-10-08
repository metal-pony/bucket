package com.metal_pony.tetrisai.ranking;

import com.metal_pony.bucket.tetris.TetrisState;

@FunctionalInterface
public interface ITetrisStateRanker {
	/**
	 * Ranks a game state (the ordering of blocks) according to how
	 * desirable it is. A higher value indicates a higher desirability
	 * for the given state.
	 *
	 * @param state The game state.
	 * @return A value representing the desirability of the given game state.
	 */
	public abstract double rank(TetrisState state);
}
