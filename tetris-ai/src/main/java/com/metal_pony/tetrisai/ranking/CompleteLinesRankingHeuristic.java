package com.metal_pony.tetrisai.ranking;

import com.metal_pony.bucket.tetris.TetrisState;

public class CompleteLinesRankingHeuristic extends RankingHeuristic {
	static final String NAME = "Complete Lines";

	public CompleteLinesRankingHeuristic() {
		super(NAME);
	}

	@Override
	protected float quantifyImpl(TetrisState state) {
		int completeLines = 0;

		for (int r = 0; r < state.rows; r++) {
			boolean line = true;
			for (int c = 0; c < state.cols; c++) {
				if (state.board[r * state.cols + c] <= 0) {
					line = false;
					break;
				}
			}
			if (line) {
				completeLines++;
			}
		}

		return completeLines;
	}
}
