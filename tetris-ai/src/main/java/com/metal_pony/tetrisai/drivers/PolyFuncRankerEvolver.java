package com.metal_pony.tetrisai.drivers;

import com.metal_pony.tetrisai.genetic.RankerPopulation;
import com.metal_pony.tetrisai.ranking.PolyFuncRanker;

public class PolyFuncRankerEvolver {
	static int popSize = 100;
	static int numGenerations = 100;

	/**
	 * Runs Tetris training algorithm.
	 */
	public static void trainNewPolyFuncRankers() {
		System.out.println("Creating new population of random rankers... ");
		//create new population, random weights
		RankerPopulation<PolyFuncRanker> pop = new RankerPopulation<>();
		PolyFuncRanker[] rankers = new PolyFuncRanker[popSize];
		for (int n = 0; n < popSize; n++) {
			rankers[n] = new PolyFuncRanker();
			//System.out.print('.');
		}
		pop.addAll(rankers);

		System.out.println("Done!");
		System.out.println();
		System.out.println("Here we go...");

		//run next generation repeatedly
		for (int g = 0; g < numGenerations; g++) {
			System.out.println();
			System.out.println("Gen " + g);
			pop.nextGeneration();
			int mean = (int) pop.mean();
			System.out.print("Avg Score: " + mean + ", Best Score: " + (int) pop.getBestFitness());
			System.out.println(
				", CoV: "
				+ (int) (100 * pop.stddev() / mean)
				+ ", Mutation Rate: "
				+ pop.getMutRate()
			);
		}

		System.out.println("Finished.");
	}
}
