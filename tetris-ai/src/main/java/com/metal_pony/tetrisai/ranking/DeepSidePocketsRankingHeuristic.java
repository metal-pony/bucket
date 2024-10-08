package com.metal_pony.tetrisai.ranking;

import com.metal_pony.bucket.tetris.TetrisState;
import com.metal_pony.bucket.util.Array;

public class DeepSidePocketsRankingHeuristic extends RankingHeuristic {
	static final String NAME = "Deep Side Pockets";

	public DeepSidePocketsRankingHeuristic() {
		super(NAME);
	}

	@Override protected float quantifyImpl(TetrisState state) {
		int[] board = Array.copy(state.board);
		for (int c = 0; c < state.cols; c++) {
			for (int r = 0; r < state.rows; r++) {
				if (board[r * state.cols + c] == 0) {
					board[r * state.cols + c] = -1;
				} else {
					break;
				}
			}
		}

		int deepHolesOnSides = 0;  //number of > 2 block gaps on LEFT or RIGHT sides only

		int[] topBlocks = new int[state.cols]; //height in each column

		for (int r = 0; r < state.rows; r++) {
			for (int c = 0; c < state.cols; c++) {
				if (board[r * state.cols + c] > 0) {
					if (topBlocks[c] == 0) {
						topBlocks[c] = state.rows - r;
					}
				}
			}
		}

		if (topBlocks[1] - topBlocks[0] > 2) {
			deepHolesOnSides++;
		}
		if (topBlocks[state.cols - 2] - topBlocks[state.cols - 1] > 2) {
			deepHolesOnSides++;
		}


		return deepHolesOnSides;
	}
}
