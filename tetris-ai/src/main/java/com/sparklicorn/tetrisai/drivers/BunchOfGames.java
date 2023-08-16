package com.sparklicorn.tetrisai.drivers;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JFrame;

import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.tetrisai.genetic.RankerPopulation;
import com.sparklicorn.tetrisai.ranking.GenericRanker;
import com.sparklicorn.bucket.util.ThreadPool;

public class BunchOfGames {
	private static int rows = 1;
	private static int cols = 1;
	private static int popSize = rows * cols;
	private static int numGenerations = 100;
	private static int blockSize = 24;

	/**
	 * Runs the Tetris training algorithm.
	 */
	public static void trainAndShow() {
		// GenericRanker.setNumFitnessRuns(8);

		//Setup frame
		JFrame frame = new JFrame() {
			@Override public void dispose() {
				super.dispose();
				ThreadPool.shutdownNow();
			}
		};

		frame.setLayout(new GridLayout(rows, cols, 2, 2));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setTitle("Tetris AI Evolver");
		frame.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					frame.dispose();
				}
			}
		});

		//Setup panels vector. Runs parallel with pop.members
		Vector<TetrisBoardPanel> panels = new Vector<>();

		//Setup ranker population
		RankerPopulation<GenericRanker> pop = new RankerPopulation<GenericRanker>() {
			@Override
			public void updateFitnesses() {
				final int SIZE = size();
				System.out.printf("Recalculating fitness values for %d members.\n", SIZE);

				// SYNCHRONOUS
				// for (int i = 0; i < SIZE; i++) {
				// 	GenericRanker ranker = members.get(i);
				// 	TetrisBoardPanel panel = panels.get(i);

				// 	ranker.updateSyncAndShow(panel);
				// }

				// WITH STREAMS (BUT SEEMS EFFECTIVELY SYNCHRONOUS)
				// Shuffler.range(size()).stream().map((index) -> {
				// 	GenericRanker ranker = members.get(index);
				// 	TetrisBoardPanel panel = panels.get(index);

				// 	return ThreadPool.submit(() -> {
				// 		ranker.updateSyncAndShow(panel);
				// 	});
				// }).forEach((future) -> {
				// 	try {
				// 		future.get(1L, TimeUnit.MINUTES);
				// 	} catch (InterruptedException | ExecutionException | TimeoutException e) {
				// 		e.printStackTrace();
				// 	}
				// });

				// ASYNCHRONOUS
				List<Future<?>> futures = new ArrayList<>();

				for (int i = 0; i < SIZE; i++) {
					GenericRanker ranker = members.get(i);
					TetrisBoardPanel panel = panels.get(i);

					futures.add(ThreadPool.submit(() -> {
						ranker.updateSyncAndShow(panel);
					}));
				}

				futures.forEach((future) -> {
					try {
						future.get(1L, TimeUnit.MINUTES);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						e.printStackTrace();
					}
				});

				//ok all members have finished recalculating fitnesses.
				//check each to see if it is better than the current best.
				for (GenericRanker m : members) {
					checkIfNewBest(m);
				}
			}

			@Override public void select() {
				sortDesc();

				//remove the lesser beings.
				//or keep the best performing members.
				//it's a matter of perspective.
				for (int i = size() - 1, stop = size() / 2; i >= stop; i--) {
					members.remove(i);
					// size--;
				}
			}

			@Override public void cross() {
				System.out.println("Crossing...");
				ThreadLocalRandom rand = ThreadLocalRandom.current();
				String endl = System.lineSeparator();

				//these will crossover with other random members
				Vector<GenericRanker> sus = getSus();

				//perform old cross() with sus
				//add result to population

				int size = sus.size();
				for (int p1i = 0; p1i < size; p1i++) {
					//Decide if member p1i should cross
					if (rand.nextDouble() < crossoverRate) {
						GenericRanker p1 = sus.get(p1i);
						int p2i = rand.nextInt(size);
						GenericRanker p2 = sus.get(p2i);

						if (!p1.equals(p2)) {
							//generate children
							GenericRanker[] r = p1.cross(p2);
							System.out.println(
								"Crossing "
								+ p1
								+ endl
								+ "With     "
								+ p2
								+ endl
								+ "Children "
								+ r[0]
								+ endl
								+ "         "
								+ r[1]
							);
							sus.set(p1i, r[0].copy());
							sus.set(p2i, r[1].copy());
						}
					}
				}

				for (GenericRanker r : sus) {
					add(r);
				}
			}
		};

		for (int i = 0; i < popSize; i++) {
			pop.add(new GenericRanker(GenericRanker.getRandomHeuristicWeights(-10f, 10f)));
			TetrisBoardPanel p = new TetrisBoardPanel(null, blockSize, false);
			panels.add(p);
			frame.add(p);
		}

		frame.pack();
		frame.setVisible(true);

		// ThreadPool.submit(() -> {
			System.out.println("Ranker population initialized.");
			System.out.println("Calculating initial fitness values...");
			pop.updateFitnesses();
			System.out.println("Fitness values finished calculating.");
			System.out.println("Here's the initial population:");
			System.out.println(pop.json());

			// pop.select(); //cuts the population in half
			//because the first step in nextGeneration() is crossover, which doubles population.
			for (int gen = 1; gen <= numGenerations; gen++) {
				if (!frame.isVisible()) {
					break;
				}

				System.out.println("Generation " + gen);
				pop.nextGeneration();
				pop.updateFitnesses();
				System.out.println(pop.json());
			}

			System.out.println("Finished");
			// GenericRanker.shutdown();
			// ThreadPool.shutdown();
		// });
	}
}
