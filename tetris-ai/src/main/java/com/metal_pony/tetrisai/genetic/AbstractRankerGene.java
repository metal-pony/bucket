package com.metal_pony.tetrisai.genetic;

import com.metal_pony.bucket.tetris.TetrisState;
import com.metal_pony.bucket.tetris.gui.components.TetrisBoardPanel;
import com.metal_pony.bucket.util.ThreadPool;
import com.metal_pony.bucket.util.genetic.IGenes;
import com.metal_pony.tetrisai.AiTetris;
import com.metal_pony.tetrisai.ranking.ITetrisStateRanker;
import com.metal_pony.tetrisai.structs.GameStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractRankerGene<R extends AbstractRankerGene<R>>
	implements IGenes<R>, ITetrisStateRanker
{
	/**
	 * The number of tetris games to run when calculating {@link #getFitness()}.
	 */
	protected static int numFitnessRuns = 8;

	public static void setNumFitnessRuns(int numRuns) {
		numFitnessRuns = numRuns;
	}

	protected double fitness;

	@Override
	public double getFitness() {
		return fitness;
	}

	/**
	 * Updates the fitness value by running a number of games (synchronously) and averaging the scores.
	 * The game will be linked to the given Panel to allow viewing while the games are running.
	 * Note:
	 *
	 * @param panel A game panel to link the game runs to.
	 */
	public void updateSyncAndShow(TetrisBoardPanel panel) {
		List<GameStats> results = new ArrayList<>(numFitnessRuns);

		for (int i = 0; i < numFitnessRuns; i++) {
			results.add(
				AiTetris.runWithPanel(
					new TetrisState(),
					this,
					0L,
					0,
					panel
				)
			);
		}

		fitness = (double) results.stream()
			.mapToLong((stats) -> stats.score())
			.sum() / numFitnessRuns;
	}

	/**
	 * Recalculates fitness value by running a number of games and averaging the scores.
	 */
	public void updateFitness() {
		// throw new RuntimeException("AbstractRankerGene#updateFitness");
		double scoreSum = (double) asyncRunGames(numFitnessRuns).stream()
			.mapToLong((futureGameStats) -> this.handleWaitForGameStats(futureGameStats).score())
			.sum();

		fitness = scoreSum / numFitnessRuns;
	}

	protected GameStats handleWaitForGameStats(Future<GameStats> futureStats) {
		try {
			return futureStats.get(5L, TimeUnit.MINUTES);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			return GameStats.EMPTY_STATS;
		}
	}

	protected List<Future<GameStats>> asyncRunGames(int numGames) {
		List<Future<GameStats>> result = new ArrayList<>(numGames);
		for (int n = 0; n < numGames; n++) {
			result.add(asyncRunGame());
		}
		return result;
	}

	protected Future<GameStats> asyncRunGame() {
		return ThreadPool.submit(() -> {
			return AiTetris.run(this);
		});
	}

	/** Returns a JSON string representation of this ranker.*/
	public abstract String json();

	public abstract R copy();
}
