package com.sparklicorn.tetrisai;

import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.tetris.util.structs.Move;
import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.bucket.util.SearchQueue;
import com.sparklicorn.tetrisai.ranking.ITetrisStateRanker;
import com.sparklicorn.tetrisai.structs.GameStats;
import com.sparklicorn.tetrisai.structs.PlacementRank;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Implementation of Tetris that provides methods that help automate gameplay.
 */
public class AiTetris extends TetrisGame {
	/**
	 * Runs a game of Tetris using the given ranker to drive piece placement.
	 * NOTE: This call blocks until the game finishes, which may take some time.
	 *
	 * @param ranker The ranker used to determine the best placement for each piece.
	 * @return Stats about the finished game.
	 */
	public static GameStats run(ITetrisStateRanker ranker) {
		return run(new TetrisState(), ranker);
	}

	/**
	 * Runs a game of Tetris using the given state and ranker to drive piece placement.
	 * NOTE: This call blocks until the game finishes, which may take some time.
	 *
	 * @param state The initial state of the game.
	 * @param ranker The ranker used to determine the best placement for each piece.
	 * @return Stats about the finished game.
	 */
	public static GameStats run(TetrisState state, ITetrisStateRanker ranker) {
		return run(state, ranker, 0L, 0);
	}

	/**
	 * Runs a game of Tetris using the given state and ranker to drive piece placement,
	 * where sleepTime is the time (in ms) between game loop ticks, and
	 * numLookAhead is the number of pieces to look ahead when ranking placements.
	 * NOTE: This call blocks until the game finishes, which may take some time.
	 *
	 * @param state The initial state of the game.
	 * @param sleepTime Time (in ms) between game loop ticks.
	 * @param numLookAhead Number of pieces to look ahead when ranking placements.
	 * @return Stats about the finished game.
	 */
	public static GameStats run(TetrisState state, ITetrisStateRanker ranker, long sleepTime, int numLookAhead) {
		return runWithPanel(state, ranker, sleepTime, numLookAhead, null);
	}

	/**
	 * Runs a game of Tetris using the given state and ranker to drive piece placement,
	 * where sleepTime is the time (in ms) between game loop ticks, and
	 * numLookAhead is the number of pieces to look ahead when ranking placements.
	 * The game will connect to the given panel, which is updated as the game progresses.
	 * NOTE: This call blocks until the game finishes, which may take some time.
	 *
	 * @param state The initial state of the game.
	 * @param sleepTime Time (in ms) between game loop ticks.
	 * @param numLookAhead Number of pieces to look ahead when ranking placements.
	 * @param panel The panel to connect the game to. Can be null to not connect to any gui.
	 * @return Stats about the finished game.
	 */
	public static GameStats runWithPanel(TetrisState state, ITetrisStateRanker ranker, long sleepTime, int numLookAhead, TetrisBoardPanel panel) {
		AiTetris t = new AiTetris(state);
		t.setRanker(ranker);
		LookAheadOptions options = new LookAheadOptions(numLookAhead > 0 ? 1 : 0, 0.25f);
		if (panel != null) {
			panel.connectGame(t);
		}
		t.run(sleepTime, options, panel);
		t.shutdown();
		return new GameStats(t.getState());
	}

	/* ****************
	* GAME COMPONENTS
	******************/
	protected ITetrisStateRanker ranker;

	/**
	 * Creates a new Tetris game with a default configuration.
	 */
	public AiTetris() {
		super();
	}

	/**
	 * Creates a new Tetris game with the provided configuration.
	 *
	 * @param config Configuration used to set up the game.
	 */
	public AiTetris(int rows, int cols) {
		this(rows, cols, null);
	}

	/**
	 * Creates a new Tetris game with the given rows, columns, and ranker.
	 */
	public AiTetris(int rows, int cols, ITetrisStateRanker ranker) {
		super(rows, cols);
		this.ranker = ranker;
	}

	/**
	 * Creates a new Tetris game with a copy of the given state.
	 *
	 * @param other The state to copy.
	 */
	public AiTetris(TetrisState state) {
		super(state);
	}

	/**
	 * Creates a new Tetris game that is a copy of the given.
	 *
	 * @param other The game to copy.
	 */
	public AiTetris(AiTetris other) {
		super(other);
		this.ranker = other.ranker;
	}

	/**
	 * Sets the game state ranker to the one given. The ranker is used to automate gameplay.
	 *
	 * @param newRanker New Tetris state ranker to use for calculating piece placement choices.
	 */
	public void setRanker(ITetrisStateRanker newRanker) {
		this.ranker = newRanker;
	}

	/**
	 * Returns this game's state ranker. The ranker is used to automate gameplay.
	 */
	public ITetrisStateRanker getRanker() {
		return ranker;
	}

	/**
	 * Runs the game. This method blocks until the game is finished.
	 *
	 * @return Stats about the finished game.
	 */
	public GameStats run() {
		return run(0L, 0);
	}

	/**
	 * Runs the game with the given time between game loop ticks and number of pieces to look ahead.
	 * This method blocks until the game is finished.
	 *
	 * @return Stats about the finished game.
	 */
	public GameStats run(long sleepTime, int numLookAhead) {
		return run(sleepTime, new LookAheadOptions(numLookAhead > 0 ? 1 : 0, 0.25f), null);
	}

	/**
	 * Runs the game using the preconfigured ranker to drive piece placements.
	 * This method blocks until the game is finished.
	 *
	 * @param sleepTime Time (in ms) between game loop ticks.
	 * @param options Options for how to look ahead when ranking placements.
	 * @return Stats about the finished game.
	 */
	public GameStats run(long sleepTime, LookAheadOptions options, TetrisBoardPanel panel) {
		if (panel != null) {
			panel.connectGame(this);
		}

		stop();
		newGame();
		start(0L, false);

		// TODO #13 Remove this and the print-outs after debugging.
		// This could be a stat that TetrisGame tracks, but it's unclear what it could be useful for.
		// Turn it into a stat if a use case is found.
		// int ticks = 0;

		try {
			while (!state.isGameOver) {
				if (state.piece.isActive()) {
					Position bestPlacement = findBestPlacement(getState(), ranker, options);
					if (bestPlacement != null) {
						state.piece.position(bestPlacement);
					}
				}

				// System.out.print('.');
				// if (ticks > 80) {
				// 	System.out.println();
				// 	ticks = 0;
				// }
				gameloop();

				if (sleepTime > 0L) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						System.err.println("Game run thread sleep interrupted?");
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new GameStats(getState());
	}

	/**
	 * Attempts to place the current piece in whichever position the ranker calculates is best.
	 * Has no effect if the game is over or if the piece is inactive.
	 */
	public void placeBest(boolean useLookAhead) {
		if (!state.isGameOver && state.piece.isActive()) {
			LookAheadOptions options = new LookAheadOptions(useLookAhead ? 3 : 0, 0.25f);
			Position bestPlacement = findBestPlacement(getState(), ranker, options);
			if (bestPlacement != null) {
				state.piece.position(bestPlacement);
			}
		}
	}

	public static record LookAheadOptions(
		int lookAhead,	// how many pieces to look ahead (0 - 5)
		float percentage // percentage of top-ranks to perform for (0.0 - 1.0)
	) {
		public LookAheadOptions(int lookAhead, float percentage) {
			this.lookAhead = Math.max(0, Math.min(lookAhead, 5));
			this.percentage = Math.max(0f, Math.min(percentage, 1f));
		}
	}

	/**
	 * Finds the best placements for the current piece using the given ranker.
	 *
	 * @param state The current game state.
	 * @param ranker The ranker to use to rank placements.
	 * @param prevPositions The previous positions of the current piece.
	 * @param percentage The percentage of possible placements to keep.
	 * @return A list of the best placements for the current piece.
	 */
	public static List<PlacementRank> getTopPlacements(
		TetrisState state,
		ITetrisStateRanker ranker,
		List<Position> prevPositions,
		float percentage
	) {
		List<PlacementRank> placements = new ArrayList<>();
		Set<Position> possiblePlacements = getPossiblePlacements(state);
		for (Position placement : possiblePlacements) {
			// System.out.printf(
			// 	"Looking at %s @ %s\n",
			// 	game.shape.name(),
			// 	placement.toString()
			// );

			TetrisState stateCopy = new TetrisState(state);
			AiTetris copy = new AiTetris(stateCopy);
			copy.state.piece.position(placement);
			copy.plotPiece();
			// copy.attemptClearLines(); // TODO #13 Not sure it is correct to clear lines?
			// copy.nextPiece();

			List<Position> prevPositionsCopy = new ArrayList<>(prevPositions);
			prevPositionsCopy.add(placement);

			double rank = ranker.rank(copy.getState());
			// System.out.println("Rank: " + rank);

			placements.add(new PlacementRank(
				copy,
				prevPositionsCopy,
				rank
			));
		}
		placements.sort((a, b) -> Double.compare(b.rank(), a.rank()));
		int numToKeep = Math.round(placements.size() * percentage);

		if (placements.size() > 0 && numToKeep == 0) {
			numToKeep = 1;
		}

		List<PlacementRank> sublist = placements.subList(0, numToKeep);
		// System.out.printf(
		// 	"TopPlacements ([%d], [%d] possible, [%.2f] keepPercentage): %s\n",
		// 	sublist.size(),
		// 	possiblePlacements.size(),
		// 	percentage,
		// 	sublist
		// );
		return sublist;
	}

	/**
	 * Calculates the best placement for the current piece using the preconfigured state ranker.
	 *
	 * @param state The current game state.
	 * @param ranker The ranker to use to rank placements.
	 * @param options The look-ahead options to use.
	 * @return The best placement for the current piece.
	 */
	public static Position findBestPlacement(
		TetrisState state,
		ITetrisStateRanker ranker,
		LookAheadOptions options
	) {
		final float percentageToKeep = options.percentage();
		List<PlacementRank> topPlacements = getTopPlacements(
			state,
			ranker,
			new ArrayList<Position>(),
			percentageToKeep
		);

		// TODO temp turn off
		// for (int n = 0; n < options.lookAhead(); n++) {
		// 	topPlacements.stream().flatMap((p) -> getTopPlacements(
		// 		p.game(),
		// 		p.placements(),
		// 		percentageToKeep
		// 	).stream());
		// }

		if (topPlacements.size() == 0) {
			return null;
		}

		topPlacements.sort((a, b) -> Double.compare(a.rank(), b.rank()));
		return topPlacements.get(0).placements().get(0);
	}

	/**
	 * Calculates the set of Tetrominos containing all terminal states for the given
	 * Tetromino on the given Board.
	 *
	 * @param state The current game state.
	 * @return The set of all possible placements for the current piece.
	 */
	public static Set<Position> getPossiblePlacements(TetrisState state) {
		// System.out.print("getPossiblePlacements()");
		HashSet<Position> placements = new HashSet<>();
		Function<Position,Boolean> acceptance = (position) -> state.isPositionValid(position);
		SearchQueue<Position> q = new SearchQueue<>(acceptance);

		int topMostBlockRow = getTopmostBlocksRow(state) - 3;
		Position startPosition = state.piece.position();
		if (startPosition.rowOffset() < topMostBlockRow) {
			startPosition.location().set(topMostBlockRow, startPosition.colOffset());
			// startPosition.add(new Coord(position.location().row(), 0), 0);
		}
		q.offer(startPosition);

		while (!q.isEmpty()) {
			Position currentPosition = q.poll();
			// System.out.println("Looking at " + currentPosition.toString());

			// Is the move legal?
			// I don't think this this condition will ever be true, given the acceptCriteria on the queue.
			if (!state.isPositionValid(currentPosition)) {
				// System.out.println("Invalid, skipping...");
				continue;
			}

			// Is the new position terminal / i.e. can we move down?
			if (!state.isPositionValid(new Position(currentPosition).add(Move.DOWN))) {
				// System.out.println("Terminal position! Adding to result set.");
				placements.add(currentPosition);
				// System.out.print(".");
			}

			for (Move move : Move.ATOMIC_MOVES) {
				Position nextPosition = new Position(currentPosition).add(move);
				if (q.offer(nextPosition)) {
					// System.out.printf("Added %s to the queue.\n", nextPosition.toString());
				} else {
					// System.out.printf(
					// 	"Tried to add %s to the queue, but failed. [seen: %b; passedCriteria: %b]\n",
					// 	nextPosition.toString(),
					// 	q.hasSeen(nextPosition),
					// 	acceptance.apply(nextPosition)
					// );
				}
			}
		}

		// System.out.println();
		return placements;
	}

	/**
	 * Calculates the row of the topmost blocks on the board.
	 *
	 * @param state The current game state.
	 * @return The row of the topmost blocks on the board.
	 */
	private static int getTopmostBlocksRow(TetrisState state) {
		for (int i = 0; i < state.rows * state.cols; i++) {
			if (state.board[i] > 0) {
				return i / state.cols;
			}
		}
		return state.rows - 1;
	}
}
