package com.metal_pony.bucket.util.genetic;

/**
 * Defines operations used in the genetic algorithm.
 *
 * @param <T> Some type that maintains data representing genetic material.
 */
public interface IGenes<T> {
	/**
	 * Returns the fitness value of this object, i.e. a rate of how well it performs.
	 * A higher value should translate into a more "fit" organism.
	 * @return Fitness value
	 */
	public abstract double getFitness();

	/**
	 * Crosses genetic material with one other, returning an arbitrary number
	 * or offspring. Offspring's genetic data should occur in order relative to its parents,
	 * i.e. data should not be positionally scrambled or randomized.
	 * @param other Set of genes representing another organism to cross with this one.
	 * @return One or more offspring as the result of the crossover.
	 */
	public abstract T[] cross(T other);

	/**
	 * Crosses genetic material with one or more other, returning an arbitrary number
	 * of offspring. Offspring's genetic data should occur in order relative to its parents,
	 * i.e. data should not be positionally scrambled or randomized.
	 *
	 * @param others One or more set of genes to cross with this one.
	 * @return One or more offspring as the result of the crossover.
	 */
	public abstract T[] cross(T[] others);

	/**
	 * Mutates the data representing genetic material in some random way.
	 */
	public abstract void mutate();
}
