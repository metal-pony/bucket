package com.sparklicorn.tetrisai.drivers;

import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.util.ThreadPool;
import com.sparklicorn.tetrisai.AiTetris;
import com.sparklicorn.tetrisai.genetic.RankerPopulation;
import com.sparklicorn.tetrisai.ranking.GenericRanker;

import java.awt.GridLayout;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

public class GenericRankerEvolver {
	private static final long sleepTime = 0L;

	// Number of rows of tetris game panels to display.
	private static int rows = 4;
	private static int cols = 8;
	private static int numPanels = rows * cols;

	private static int blockSize = 8;

	// Lower and upper bounds used when generating random weights
	// for the population of rankers.
	private static float lower = -4.0f;
	private static float upper = 4.0f;

	private static int popSize = 25;
	private static int numGenerations = 25;

	private static int NUM_THREADS = Runtime.getRuntime().availableProcessors();

	/**
	 * Runs the Tetris training algorithm.
	 */
	public static void trainAndShow() {
		JFrame frame = new JFrame() {
			@Override
			public void dispose() {
				super.dispose();
				ThreadPool.shutdownNow();
			}
		};
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setTitle("Generic Ranker Evolver");
		frame.setLayout(new GridLayout(rows, cols, 2, 2));
		frame.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					frame.dispose();
				}
			}
		});

		List<TetrisBoardPanel> panels = new ArrayList<>();
		for (int n = 0; n < numPanels; n++) {
			TetrisBoardPanel panel = new TetrisBoardPanel(new AiTetris(), blockSize, false);
			frame.add(panel);
			panels.add(panel);
		}

		frame.pack();
		frame.setVisible(true);

		System.out.println("Generic Ranker Evolver:");
		System.out.println("Using threads: " + NUM_THREADS);
		System.out.print("Creating new population of random rankers ");

		// Create new population. Seed with random weights.
		RankerPopulation<GenericRanker> pop = new RankerPopulation<>();
		for (int n = 0; n < popSize; n++) {
			pop.add(new GenericRanker(GenericRanker.getRandomHeuristicWeights(lower, upper)));
			System.out.print('.');
		}

		System.out.println(" Done!\nHere we go...");
		for (int g = 0; g < numGenerations; g++) {
			System.out.println("\nGeneration: " + g);
			pop.nextGeneration();
			pop.updateFitnesses(panels);
			int mean = (int) pop.mean();
			System.out.printf(
				"Avg Score: %d, Best Score: %d, CoV: %d, Mutation Rate: %.2f\n",
				mean,
				(int) pop.getBestFitness(),
				(int) (100.0 * pop.stddev() / mean),
				pop.getMutRate()
			);
		}

		System.out.println("Finished.\nEnding population:\n" + pop.json());

		panels.forEach((panel) -> {
			ThreadPool.submit(() -> {
				AiTetris.runWithPanel(
					new TetrisState(),
					new GenericRanker(pop.fittestMember()),
					sleepTime,
					0,
					panel
				);
			});
		});
	}
}
