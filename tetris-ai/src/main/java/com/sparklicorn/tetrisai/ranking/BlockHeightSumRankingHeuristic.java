package com.sparklicorn.tetrisai.ranking;

import com.sparklicorn.bucket.tetris.TetrisState;

public class BlockHeightSumRankingHeuristic extends RankingHeuristic {
	static final String NAME = "Block Height Sum";

	public BlockHeightSumRankingHeuristic() {
		super(NAME);
	}

	@Override
	protected float quantifyImpl(TetrisState state) {
		int sumBlockHeights = 0; //sum of block heights

		for (int r = 0; r < state.rows; r++) {
			for (int c = 0; c < state.cols; c++) {
				if (state.board[r * state.cols + c] > 0) {
					sumBlockHeights += state.rows - r;
				}
			}
		}

		return sumBlockHeights;
	}
}
