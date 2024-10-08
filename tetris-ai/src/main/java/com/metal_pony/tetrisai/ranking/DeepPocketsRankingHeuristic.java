package com.metal_pony.tetrisai.ranking;

import com.metal_pony.bucket.tetris.TetrisState;

public class DeepPocketsRankingHeuristic extends RankingHeuristic {
	static final String NAME = "Deep Pockets";

	public DeepPocketsRankingHeuristic() {
		super(NAME);
	}

	@Override protected float quantifyImpl(TetrisState state) {
		int deepHoles = 0;   //number of gaps that are > 2 blocks deep AND NOT ON LEFT OR RIHT SIDES

		int[] topBlocks = new int[state.cols]; //height in each column

		for (int r = 0; r < state.rows; r++) {
			for (int c = 0; c < state.cols; c++) {
				if (state.board[r * state.cols + c] > 0) {
					if (topBlocks[c] == 0) {
						topBlocks[c] = state.rows - r;
					}
				}
			}
		}

		for (int c = 1; c < state.cols - 1; c++) {
			if ((topBlocks[c - 1] - topBlocks[c] > 2) && (topBlocks[c + 1] - topBlocks[c] > 2)) {
				deepHoles++;
			}
		}

		return deepHoles;
	}
}
