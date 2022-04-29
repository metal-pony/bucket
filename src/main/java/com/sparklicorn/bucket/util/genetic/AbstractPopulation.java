package com.sparklicorn.bucket.util.genetic;

import java.util.List;
import java.util.Vector;

/**
 * Represents a population that is manipulated under the genetic algorithm.
 *
 * @param <G> Some type that maintains data representing genetic material, and
 * implements operations necessary for the genetic algorithm.
 */
public abstract class AbstractPopulation<G extends IGenes<G>> {

	protected Vector<G> members;

	/**
	 * Creates a new empty population. So lonely.
	 */
	public AbstractPopulation() {
		members = new Vector<>();
	}

	/**
	 * Adds a member to the population.
	 * @param newMember - The member to add. Newbie.
	 * @return True if the member was successfully added to the population; otherwise false.
	 */
	public boolean add(G newMember) {
		return members.add(newMember);
	}

	/**
	 * Selects which population members will move on to the next generation.
	 * Implementation is typically based on member performance, to represent
	 * a form of "survival of the fittest".
	 */
	public abstract void select();

	/**
	 * Mutates some or all population members in some arbitrary way,
	 * representing genetic mutation.
	 */
	public abstract void mutate();

	/**
	 * Represents reproduction, such that one or more new members are created using
	 * the genetic data of two (or more) existing members.
	 */
	public abstract void cross();

	/**
	 * Generates the next generation of the population by applying the
	 * steps of the genetic algorithm: Crossover > Mutation > Selection.
	 * This method can be overridden to apply additional or alternative steps.
	 */
	public void nextGeneration() {
		cross();
		mutate();
		select();
	}

	/**
	 * Sorts the population collection by member fitness in ascending order.
	 */
	public void sortAsc() {
		members.sort((G g1, G g2) -> {
			return (g1.getFitness() < g2.getFitness()) ? -1 : 1;
		});
	}

	/**
	 * Sorts the population collection by member fitness in descending order.
	 */
	public void sortDesc() {
		members.sort((G g1, G g2) -> {
			return (g1.getFitness() < g2.getFitness()) ? 1 : -1;
		});
	}

	/**
	 * Returns a List containing the population members.
	 * @return a new List containing members.
	 */
	public List<G> getMembers() {
		return getMembers(new Vector<G>());
	}

	/**
	 * Adds each member of this population to the given List.
	 * @param list
	 * @return the given List, for convenience.
	 */
	public List<G> getMembers(List<G> list) {
		list.addAll(members);
		return list;
	}

	/**
	 * Returns the member with the highest fitness.
	 * If the population is empty, returns null.
	 */
	public G fittestMember() {
		G fittest = null;

		for (G member : members) {
			if (member.getFitness() > fittest.getFitness()) {
				fittest = member;
			}
		}

		return fittest;
	}

	/**
	 * Returns the size of the population.
	 */
	public int size() {
		return members.size();
	}
}
