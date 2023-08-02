package com.sparklicorn.tetrisai.ranking;

import java.util.LinkedList;
import java.util.Queue;

import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.util.Array;

public class BlockedSpacesRankingHeuristic extends RankingHeuristic {
	static final String NAME = "Blocked Spaces";

	public BlockedSpacesRankingHeuristic() {
		super(NAME);
	}

	private static final Coord LEFT = new Coord(0, -1);
	private static final Coord RIGHT = new Coord(0, 1);
	private static final Coord DOWN = new Coord(1, 0);

	@Override
	protected float quantifyImpl(TetrisState state) {
		int[] board = Array.copy(state.board);

		int entryCol = TetrisState.calcEntryColumn(state.cols);

		Queue<Coord> queue = new LinkedList<>();
		queue.add(new Coord(0, entryCol));

		while (!queue.isEmpty()) {
			Coord cellCoord = queue.poll();
			int cellIndex = cellCoord.row() * state.cols + cellCoord.col();

 			if (
				!state.validateCoord(cellCoord) ||
				!state.isCellEmpty(cellCoord) ||
				board[cellIndex] == -1
			) {
				continue;
			}

			board[cellIndex] = -1;
			// add adjacent cells to queue (probably don't need to check UP)
			queue.offer(new Coord(cellCoord).add(LEFT));
			queue.offer(new Coord(cellCoord).add(RIGHT));
			queue.offer(new Coord(cellCoord).add(DOWN));
		}

		int sumBlockedSpaces = 0; //number of empty cells without LOS to ceiling
		for (int val : board) {
			if (val == 0) {
				sumBlockedSpaces++;
			}
		}

		return sumBlockedSpaces;
	}
}
