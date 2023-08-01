package com.sparklicorn.tetrisai.structs;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.Array;

/**
 * Container for Tetris game statistics.
 */
public record GameStats(
	long score,
	long numLinesCleared,
	long numPiecesPlaced,
	long level,
	long[] distribution
) {
	public static final GameStats EMPTY_STATS = new GameStats(0L, 0L, 0L, 0L, new long[Shape.NUM_SHAPES]);

	/**
	 * Creates new GameStats given a game instance to derive stats from.
	 */
	public GameStats(TetrisState state) {
		this(
			state.score,
			state.linesCleared,
			state.numPiecesDropped,
			state.level,
			Array.copy(state.dist)
		);
	}

	private static String mapToString(Map<String,? extends Object> map, int spacesIndent) {
        StringBuilder strb = new StringBuilder();
        String indent = " ".repeat(spacesIndent);
        int i = 0;
        for (Entry<String,? extends Object> entry : map.entrySet()) {
            strb.append(indent);
            strb.append(String.format("  %s: %s", entry.getKey(), entry.getValue()));
            if (i++ < map.size() - 1) {
                strb.append(",\n");
            }
        }

        return String.format(
            """
            %s{
            %s  %s
            %s}
            """,
            indent,
            indent, strb.toString(),
            indent
        );
    }

	@Override
	public String toString() {
		// StringBuilder distributionStr = new StringBuilder();

		Map<String,Long> distMap = new HashMap<>();

		for (Shape s : Shape.values()) {
			// distributionStr.append("  " + s.name() + ": " + distribution[s.value - 1]);
			// distributionStr.append(System.lineSeparator());
			distMap.put(s.name(), distribution[s.value - 1]);
		}


		return String.format(
			"""
			{
			  Score: %d,
			  Level: %d,
			  Lines cleared: %d,
			  Pieces dropped: %d,
			  Piece distribution: %s
			}
			""",
			score,
			level,
			numLinesCleared,
			numPiecesPlaced,
			mapToString(distMap, 2)
			// distributionStr.toString()
		);
	}
}
