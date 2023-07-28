package com.sparklicorn.tetrisai.game;

// import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.tetris.util.structs.Move;
import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.tetris.util.structs.ShapeQueue;
import com.sparklicorn.bucket.util.SearchQueue;
// import com.sparklicorn.bucket.util.event.Event;
// import com.sparklicorn.bucket.util.event.EventBus;
import com.sparklicorn.tetrisai.structs.GameConfig;
import com.sparklicorn.tetrisai.structs.GameStats;
import com.sparklicorn.tetrisai.structs.PlacementRank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
// import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of Tetris that uses the 7-bag random piece generator.
 * Users call <code>gameloop()</code> directly.  There is no internal timer
 * associated with the gameloop in this version, as it is intended to be
 * played by a computer to play as quickly as possible.
 * Provides methods for automated gameplay.
 */
public class AiTetris extends TetrisGame {
	/* ****************
	* GAME COMPONENTS
	******************/
	protected ITetrisStateRanker ranker;

	protected double[] rank;

	/**
	 * Creates a new Tetris game with a default configuration.
	 */
	public AiTetris() {
		this(new GameConfig());
	}

	/**
	 * Creates a new Tetris game with the provided configuration.
	 *
	 * @param config Configuration used to set up the game.
	 */
	public AiTetris(GameConfig config) {
		this(
			config.rows(),
			config.cols(),
			config.useEvents(),
			config.stateRanker(),
			null
		);

		if (config.eventListeners() != null) {
			config.eventListeners().forEach((event, consumers) -> {
				consumers.forEach((consumer) -> {
					registerEventListener(event, consumer);
				});
			});
		}
	}

	/**
	 * Creates a new Tetris game with the given parameters.
	 */
	public AiTetris(
		int numRows,
		int numCols,
		boolean useEvents,
		ITetrisStateRanker ranker,
		ShapeQueue shapeQueue
	) {
		super(numRows, numCols, false);

		if (!useEvents) {
			this.eventBus.dispose(true);
			this.eventBus = null;
		}

		this.nextShapes = (shapeQueue == null) ? new ShapeQueue(256) : shapeQueue;
		this.ranker = ranker;
		this.rank = new double[rows * cols];
		resetRank();
	}

	public AiTetris(AiTetris other) {
		super(other);
		this.ranker = other.ranker;
		this.rank = new double[rows * cols];
		resetRank();
	}

	public double[] getRanks(double[] arr) {
		if (arr == null) {
			arr = new double[rows * cols];
		}
		System.arraycopy(rank, 0, arr, 0, rank.length);
		return arr;
	}

	private void resetRank() {
		Arrays.fill(rank, Double.MIN_VALUE);
	}

	// TODO sparklicorn/bucket#49 make shape queue more flexible
	// It is especially useful for testing to be able to change the active shape ad hoc.
	public void setShape(Shape shape) {
		// Fast-forward so that the given shape is next in the queue.
		while (nextShapes.peek() != shape) {
			nextShapes.poll();
		}
		// Set next shape and set block locations.
		this.shape = nextShapes.poll();
		position = new Position(
			new Coord(1, calcEntryColumn(cols)),
			0,
			this.shape.getNumRotations()
		);
		populateBlockPositions(blockLocations, position);
	}

	/**
	 * Resets the game using the given configuration.
	 *
	 * @param newConfig Configuration used to set the game properties.
	 */
	// public void reset(GameConfig newConfig) {
	// 	if (newConfig.rows() != rows || newConfig.cols() != cols) {
	// 		rows = newConfig.rows();
	// 		cols = newConfig.cols();

	// 		if (rows < MINIMUM_ROWS || rows > MAXIMUM_ROWS) {
	// 			rows = DEFAULT_NUM_ROWS;
	// 		}

	// 		if (cols < MINIMUM_COLS || cols > MAXIMUM_COLS) {
	// 			cols = DEFAULT_NUM_COLS;
	// 		}
	// 	}

	// 	if (newConfig.useEvents()) {
	// 		if (eventBus == null) {
	// 			eventBus = new EventBus();
	// 		}

	// 		eventBus.unregisterAll();

	// 		for (TetrisEvent e : newConfig.eventListeners().keySet()) {
	// 			List<Consumer<Event>> list = newConfig.eventListeners().get(e);
	// 			for (Consumer<Event> listener : list) {
	// 				registerEventListener(e, listener);
	// 			}
	// 		}
	// 	} else {
	// 		if (eventBus != null) {
	// 			eventBus.dispose(false);
	// 			eventBus = null;
	// 		}
	// 	}

	// 	// ranker = newConfig.stateRanker();
	// 	resetRank();
	// }

	// Exposes gameloop() publicly.
	@Override
	public synchronized void gameloop() {
		super.gameloop();
	}

	////////////////////////
	// AI playstyle  methods
	////////////////////////

	@Override
	protected void nextPiece() {
		super.nextPiece();

		// Calculates and stores terminal position ranks
		// resetRank();
		getTopPlacements(this, new ArrayList<>(), 1f);
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
	 * Plays a game of Tetris using the given configuration.
	 *
	 * @param config - The configuration used to set up the game.
	 * @return When the game is finished, this returns a container
	 * object holding statistics about the game.
	 */
	public static GameStats run(GameConfig config) {
		return run(0L, config);
	}

	/**
	 * Starts a new AITetris game with the given game configuration, using the given time
	 * between game loop ticks.
	 * NOTE: This call blocks until the game finishes, which may take some time.
	 *
	 * @param sleepTime Time (in ms) between game loop ticks.
	 * @param config Game configuration.
	 * @return Stats about the finished game.
	 */
	public static GameStats run(long sleepTime, GameConfig config) {
		AiTetris t = new AiTetris(config);
		LookAheadOptions options = new LookAheadOptions(config.useLookAhead() ? 3 : 0, 0.25f);
		t.run(sleepTime, options);
		t.shutdown();
		return new GameStats(t);
	}

	public static GameStats runWithPanel(long sleepTime, GameConfig config, TetrisBoardPanel panel) {
		AiTetris t = new AiTetris(config);
		LookAheadOptions options = new LookAheadOptions(config.useLookAhead() ? 3 : 0, 0.25f);
		panel.setGame(t);
		t.run(sleepTime, options);
		t.shutdown();
		return new GameStats(t);
	}

	public GameStats run(long sleepTime, boolean useLookAhead) {
		return run(sleepTime, new LookAheadOptions(useLookAhead ? 3 : 0, 0.25f));
	}

	/**
	 * Plays the game using the preconfigured ranker to drive piece placements.
	 * NOTE: This call blocks until the game finishes, which may take some time.
	 *
	 * @param sleepTime - The amount of time (in ms) to pause between gameloops.
	 * This is useful if there is a GUI displaying the game so that the game does not
	 * flash by too quickly.
	 */
	public GameStats run(long sleepTime, LookAheadOptions options) {
		System.out.println("Game stopping");
		stop();
		System.out.println("Configuring new game");
		newGame();
		System.out.println("Starting new game");
		start(0L);

		System.out.println("Running game using ranker: " + ((GenericRanker) ranker).addr() + "\n" + ranker.toString());

		// TODO #13 Remove this and the print-outs after debugging.
		// This could be a stat that TetrisGame tracks, but it's unclear what it could be useful for.
		// Turn it into a stat if a use case is found.
		// int ticks = 0;

		try {
			while (!isGameOver) {
				if (isActive) {
					position = findBestPlacement(options);
					populateBlockPositions(blockLocations, position);
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
						// fail silently
						System.err.println("Game run thread sleep interrupted?");
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		System.out.println("Game run finished.");
		return new GameStats(this);
	}

	/**
	 * Plays an automated round of Tetris using the game's ranker to guide
	 * piece placements. The game will try to play as quickly as possible.
	 * Blocks until the game is finished.
	 */
	public GameStats run(boolean useLookAhead) {
		return run(0, new LookAheadOptions(useLookAhead ? 3 : 0, 0.25f));
	}

	/**
	 * Attempts to place the current piece in whichever position the ranker calculates is best.
	 * Has no effect if the game is over or if the piece is inactive.
	 */
	public void placeBest(boolean useLookAhead) {
		if (!isGameOver && isActive) {
			LookAheadOptions options = new LookAheadOptions(useLookAhead ? 3 : 0, 0.25f);
			position = findBestPlacement(options);
			populateBlockPositions(blockLocations, position);
		}
	}

	@Override
	protected void plotPiece() {
		populateBlockPositions(blockLocations, position);
		super.plotPiece();
	}

	protected void unplotPiece() {
		populateBlockPositions(blockLocations, position);
		for (Coord c : blockLocations) {
			int index = c.row() * cols + c.col();
			board[index] = 0;
		}
		// TODO #13 Does this need to throw events (possibly new event)?
		// throwEvent(TetrisEvent.PIECE_PLACED);
		// throwEvent(TetrisEvent.BLOCKS);
	}

	public double getPlacementRank(Position placement) {
		Position originalPosition = position;
		position = placement;
		plotPiece();
		double rank = ranker.rank(this);
		unplotPiece();
		position = originalPosition;
		return rank;
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

	public static List<PlacementRank> getTopPlacements(
		AiTetris game,
		List<Position> prevPositions,
		float percentage
	) {
		// System.out.println("getTopPlacements:");
		List<PlacementRank> placements = new ArrayList<>();
		Set<Position> possiblePlacements = game.getPossiblePlacements();
		game.resetRank();
		for (Position placement : possiblePlacements) {
			// System.out.printf(
			// 	"Looking at %s @ %s\n",
			// 	game.shape.name(),
			// 	placement.toString()
			// );

			AiTetris copy = new AiTetris(game);
			copy.position = placement;
			copy.plotPiece();
			// copy.attemptClearLines(); // TODO #13 Not sure it is correct to clear lines?
			// copy.nextPiece();

			List<Position> prevPositionsCopy = new ArrayList<>(prevPositions);
			prevPositionsCopy.add(placement);

			// double rank = copy.getPlacementRank(placement);
			double rank = game.ranker.rank(copy);
			// System.out.println("Rank: " + rank);

			// calculate cell index from placement row and column
			// record rank at that index if it is more than the current rank
			int index = placement.rowOffset() * copy.cols + placement.colOffset();
			game.rank[index] = Math.max(game.rank[index], rank);

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
	 * @param lookAhead - Whether to consider the next shape when calculating.
	 */
	public Position findBestPlacement(LookAheadOptions options) {
		final float percentageToKeep = options.percentage();
		List<PlacementRank> topPlacements = getTopPlacements(
			this,
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
	 * @param b Board to calculate piece positions on.
	 * @param t Tetromino piece to calculate placements for.
	 * @return
	 */
	public Set<Position> getPossiblePlacements() {
		System.out.print("getPossiblePlacements()");
		HashSet<Position> placements = new HashSet<>();
		Function<Position,Boolean> acceptance = (position) -> isPositionValid(position);
		SearchQueue<Position> q = new SearchQueue<>(acceptance);

		int topMostBlockRow = getTopmostBlocksRow() - 3;
		Position startPosition = new Position(position);
		if (position.location().row() < topMostBlockRow) {
			startPosition.location().set(topMostBlockRow, position.colOffset());
			// startPosition.add(new Coord(position.location().row(), 0), 0);
		}
		q.offer(startPosition);

		while (!q.isEmpty()) {
			Position currentPosition = q.poll();
			// System.out.println("Looking at " + currentPosition.toString());

			// Is the move legal?
			// I don't think this this condition will ever be true, given the acceptCriteria on the queue.
			if (!isPositionValid(currentPosition)) {
				// System.out.println("Invalid, skipping...");
				continue;
			}

			// Is the new position terminal / i.e. can we move down?
			if (!isPositionValid(new Position(currentPosition).add(Move.DOWN))) {
				// System.out.println("Terminal position! Adding to result set.");
				placements.add(currentPosition);
				System.out.print(".");
			}

			for (Move move : ATOMIC_MOVES) {
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

		System.out.println();
		return placements;
	}

	private int getTopmostBlocksRow() {
		for (int i = 0; i < rows * cols; i++) {
			if (board[i] > 0) {
				return i / cols;
			}
		}
		return rows - 1;
	}
}
