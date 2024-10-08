package com.metal_pony.tetrisai.genetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.metal_pony.bucket.tetris.gui.components.TetrisBoardPanel;
import com.metal_pony.bucket.util.ThreadPool;
import com.metal_pony.bucket.util.genetic.AbstractPopulation;

public class RankerPopulation<G extends AbstractRankerGene<G>> extends AbstractPopulation<G> {
	protected double mutationRate = 0.05;
	protected double crossoverRate = 0.05;

	//Holds a copy of the best member.
	protected G bestSeen = null;

	/**
	 * Creates a new RankerPopulation with default mutation and crossover rates.
	 */
	public RankerPopulation() {
		mutationRate = 0.05;
		crossoverRate = 0.05;
	}

	public double getMutRate() {
		return mutationRate;
	}

	@Override public boolean add(G newMember) {
		return super.add(newMember);
		//fitness needs to be calculated
		//checkIfNewBest(members.get(members.size() - 1));
	}

	/**
	 * Adds the given ranker to the population with a preset fitness value.
	 */
	public void add(G newMember, double fitness) {
		//super.add(newMember) calls new PopMember<>(newMember) which causes
		// updateFitness() to calculate the fitness.
		//If the fitness is known already, then the extra processing of
		// updateFitness() should be avoided.
		members.add(newMember);//, fitness); // TODO #13 wut do here? (I'm not sure why I originally tagged this as todo).
		// size++;
		//checkIfNewBest(members.get(members.size() - 1));
	}

	/**
	 * Adds all provided members to this population.
	 * @param newMembers New population members to add.
	 */
	public void addAll(G[] newMembers) {
		for (G newMember : newMembers) {
			add(newMember);
		}
	}

	/**
	 * Checks if the given PopMember has a greater fitness value than the
	 * current best.  If it does, then <code>bestSeen</code> will be set to
	 * a copy of it.
	 */
	protected void checkIfNewBest(G m) {
		if (bestSeen == null || m.getFitness() > bestSeen.getFitness()) {
			bestSeen = m.copy();
		}
	}

	protected void checkForNewBest() {
		System.out.println("Checking for best pop member.");
		for (G m : members) {
			checkIfNewBest(m);
		}
	}


	@Override public G fittestMember() {
		return bestSeen.copy();
	}

	/**
	 * Returns the highest fitness value recorded thus far.
	 */
	public double getBestFitness() {
		return bestSeen.getFitness();
	}

	//Returns a stochastic universal sample of pop members
	protected Vector<G> getSus() {
		Vector<G> chosen = new Vector<>(members);

		double fitSum = fitnessSum();
		if (fitSum <= 0.0 || size() == 0) {
			return chosen;
		}

		//distance between pointers (also avg fitness)
		double p = fitSum / size();

		//random starting point between 0 and dist between pointers
		double start = ThreadLocalRandom.current().nextDouble() * p;

		chosen.clear();
		int index = 0;
		int numChosen = 0;
		double sum = members.get(index).getFitness();

		while (numChosen < size()) {
			//Pointer
			double ptr = start + numChosen * p;

			if (ptr <= sum) {
				//sum += p; //Why did I comment this again? Is it wrong?
				numChosen++;
				G m = members.get(index);
				chosen.add(m.copy());
			} else {
				index++;
				sum += members.get(index).getFitness();
			}
		}

		return chosen;
	}

	//Stochastic Universal Sampling
	private void sus() {
		double f = fitnessSum();

		if (f <= 0.0 || size() == 0) {
			return;
		}

		//distance between pointers (also avg fitness)
		double p = f / size();

		//random starting point between 0 and dist between pointers
		double start = ThreadLocalRandom.current().nextDouble() * p;

		//Next generation of PopMembers.
		Vector<G> nextGen = new Vector<>();

		int index = 0;
		int numChosen = 0;
		double sum = members.get(index).getFitness();

		while (numChosen < size()) {
			//Pointer
			double ptr = start + numChosen * p;

			if (ptr <= sum) {
				//sum += p; //Why did I comment this again? Is it wrong?
				numChosen++;
				G m = members.get(index);
				nextGen.add(m.copy());
			} else {
				index++;
				sum += members.get(index).getFitness();
			}
		}

		//do the members swap.
		members = nextGen;
	}

	@Override public void select() {
		sus();
	}

	/**
	 * Calculates the standard deviation of the population's fitness.
	 */
	public double stddev() {
		double result = 0.0;

		if (size() > 0) {
			double mu = fitnessSum() / size();
			double varSum = 0.0;
			for (G m : members) {
				double d = m.getFitness() - mu;
				varSum += d * d;
			}
			result = Math.sqrt(varSum / size());
		}

		return result;
	}

	//Adjusts the mutation rate to make it more or less disruptive.
	//As the spread of the populations' fitnesses decreases, selection
	// becomes more and more useless.  To counter, the disruptiveness
	// of the mutation can be increased inversely proportional to the
	// spread in attempt to escape a local optima, instead of relying
	// upon a constant and possibly mutation rate.
	//Returns the new mutation rate.
	private double adjustMutationRate() {
		double stddev = stddev(); //0.0 if pop_size == 0
		double newMutateRate = 1.0;
		if (stddev > 0.0) {
			//mutation = e ^ (-3 * coeff of variation)
			//This gives a heavily skewed distribution for a much greater
			// mutation rate when the stddev is very low.

			//mean fitness
			double mu = fitnessSum() / size();

			//coeff of variation
			double cv = (stddev / mu);

			double a = -12.0;

			newMutateRate = Math.max(Math.pow(Math.E, a * cv), 0.05);
		}

		mutationRate = newMutateRate;
		return mutationRate;
	}

	/**
	 * Returns the sum of the member's fitnesses.
	 */
	public double fitnessSum() {
		return members.stream().mapToDouble((m) -> m.getFitness()).sum();
	}

	/**
	 * Returns the mean fitness of the population members.
	 */
	public double mean() {
		double result = 0.0;
		if (size() > 0) {
			result = fitnessSum() / size();
		}
		return result;
	}

	//fitness proportionate w/ elitism
	@SuppressWarnings("unused")
	private void fp_elitism() {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		double fitnessSum = fitnessSum();
		double[] runningTotal = new double[size()];

		double rt = 0.0; //holds running total while iterating over members
		for (int i = 0; i < size(); i++) {
			rt += members.get(i).getFitness() / fitnessSum;
			runningTotal[i] = rt;
		}

		Vector<G> survivingMembers = new Vector<>();

		for (int spin = 0; spin < size(); spin++) {
			double r = rand.nextDouble();
			for (int i = 0; i < size(); i++) {
				if (r < runningTotal[i]) {
					G m = members.get(i);
					survivingMembers.add(m.copy());
					break;
				}
			}
		}

		// Replace weakest surviving member with the best
		// survivingMembers.set(
		//   findWeakestMember(survivingMembers),
		//   new PopMember<>(bestSeen.getMember(), bestSeen.getFitness())
		// );

		members = survivingMembers;
	}

	@Override public void nextGeneration() {
		super.nextGeneration();
		checkForNewBest();
		adjustMutationRate();
	}

	@Override public void mutate() {
		System.out.println("Mutating...");
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		for (G m : members) {
			if (rand.nextDouble() < mutationRate) {
				String s = m.toString();
				m.mutate();
				System.out.println(s + " => " + m.toString());
			}
		}
	}

	/**
	 * Updates the fitness values for each member.
	 */
	public void updateFitnesses() {
		for (G member : members) {
			member.updateFitness();
		}
	}

	public void updateFitnesses(List<TetrisBoardPanel> panels) {
		List<Future<?>> results = new ArrayList<>();

		for (int i = 0; i < size(); i++) {
			G ranker = members.get(i);
			if (i < panels.size()) {
				final int index = i;
				results.add(ThreadPool.submit(() -> ranker.updateSyncAndShow(panels.get(index))));
			} else {
				ranker.updateFitness();
			}
		}

		results.forEach((future) -> {
			try {
				future.get(1L, TimeUnit.MINUTES);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		});
	}

	//fitness proportionate uniform crossover
	//choose 2 members (say m1 fitness > m2 fitness)
	//c1 := copy of m1
	//for each element in c1,
	//  with probability m2.fitness / (m1.fitness + m2.fitness)
	//  replace element with that from same position in m2
	//do this again (c2), swapping m1 & m2
	//replace parents with c1, c2
	@Override public void cross() {
		System.out.println("Crossing...");
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for (int parent1Index = 0; parent1Index < size(); parent1Index++) {
			// Decide if member p1i should cross
			if (rand.nextDouble() < crossoverRate) {
				G p1 = members.get(parent1Index);
				final int _p1i = parent1Index;
				final int p2i = rand.nextInt(size());
				G p2 = members.get(p2i);

				if (!p1.equals(p2)) {
					//generate children
					G[] r = (G[]) p1.cross(p2);
					System.out.printf(
						"Crossing %s%nWith     %s%nChildren %s%n         %s%n",
						p1, p2, r[0], r[1]
					);
					members.set(_p1i, r[0].copy());
					members.set(p2i, r[1].copy());
				}
			}
		}

	}

	/**
	 * Returns a json string representation of this population.
	 */
	public String json() {
		String lineBreak = System.lineSeparator();
		StringBuilder strb = new StringBuilder(
		String.format("{\n\"mean_fitness\": %f,\n\"members\": [\n", mean()));

		sortAsc();

		for (int i = 0; i < size(); i++) {
			G m = members.get(i);

			strb.append(
				String.format("{\"fitness\": %f, \"weights\": %s}", m.getFitness(), m.json())
			);

			if (i < size() - 1) {
				strb.append(',');
			}

			strb.append(lineBreak);
		}

		strb.append(']');
		strb.append(lineBreak);
		strb.append('}');

		return strb.toString();
	}
}
