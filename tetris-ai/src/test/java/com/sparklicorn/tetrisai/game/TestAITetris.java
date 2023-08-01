package com.sparklicorn.tetrisai.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.Shuffler;

public class TestAITetris {
	// TODO sparklicorn/bucket#9 move to utility class
	private static <T> void assertCollectionEquals(Collection<T> expected, Collection<T> actual) {
		assertEquals(expected.size(), actual.size());

		for (T e : expected) {
			assertTrue(
				actual.contains(e),
				String.format("Expected actual to contain %s", e.toString())
			);
		}

		for (T e : actual) {
			assertTrue(
				expected.contains(e),
				String.format("Actual contains unexpected element %s", e.toString())
			);
		}
	}

	// TODO sparklicorn/bucket#9 move to utility class
	// private static <T> String strAll(Collection<T> collection) {
	// 	StringBuilder strb = new StringBuilder();
	// 	collection.forEach((obj) -> {
	// 		strb.append(obj.toString());
	// 		strb.append(System.lineSeparator());
	// 	});
	// 	return strb.toString();
	// }

	@Test
	public void getTopPlacements_whenBoardIsEmpty() {
		ITetrisStateRanker ranker = (state) -> {
			double result = 0.0;
			for (int i = 0; i < state.board.length; i++) {
				if (state.board[i] > 0) {
					result += i;
				}
			}
			return result;
		};
		TetrisState state = new TetrisState();
		state.shape = Shape.I;
		state.position = new Position(1, 4, 0, state.shape.getNumRotations());

		List<Position> actual = AiTetris.getTopPlacements(state, ranker, new ArrayList<>(), 1f)
			.stream()
			.map((placementRank) -> placementRank.placements().get(0))
			.toList();

		Set<Position> expected = new HashSet<>();
		// 7 horizontal placements across the bottom
		Shuffler.range(2, 9).forEach((i) -> expected.add(new Position(19, i, 0, 2)));
		// 10 vertical placements
		Shuffler.range(0, 10).forEach((i) -> expected.add(new Position(17, i, 1, 2)));

		assertCollectionEquals(expected, actual);
	}

	// TODO #13 test more states - may want to build a tool or functions to assist in creation of test fixtures
	@Test
	public void getPossiblePlacements_whenBoardIsEmpty() {
		TetrisState state = new TetrisState();
		state.shape = Shape.I;
		state.position = new Position(1, 4, 0, state.shape.getNumRotations());

		Set<Position> actual = AiTetris.getPossiblePlacements(state);
		Set<Position> expected = new HashSet<>();
		// 7 horizontal placements across the bottom
		Shuffler.range(2, 9).forEach((i) -> expected.add(new Position(19, i, 0, 2)));
		// 10 vertical placements
		Shuffler.range(0, 10).forEach((i) -> expected.add(new Position(17, i, 1, 2)));

		assertCollectionEquals(expected, actual);
	}
}
