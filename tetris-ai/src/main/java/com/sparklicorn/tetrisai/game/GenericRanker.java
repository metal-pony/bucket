package com.sparklicorn.tetrisai.game;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;

public class GenericRanker extends AbstractRankerGene<GenericRanker> {
	/**
	 * A default GenericRanker with established ranking weights.
	 */
	public static GenericRanker DEFAULT_RANKER = new GenericRanker(
		new double[] { 3.589, -1.374, -17.409, -12.835, -10.748 }
	);

	/** Number of weights used by this ranker algorithm.*/
	public static final int NUM_WEIGHTS = 5;

	private static double mutationAmplifier = 10.0;

	/** The rate at which weights will cross.*/
	protected double crossRate = 0.25;

	/** The rate at which a weight will mutate.*/
	protected double mutateRate = 0.25;

	public void setCrossRate(double newCrossRate) {
		crossRate = newCrossRate;
	}

	public void setMutateRate(double newMutateRate) {
		mutateRate = newMutateRate;
	}

	// weights
	// index desc
	// 0 lines cleared
	// 1 sum of block heights
	// 2 number of blocked in spaces
	// 3 existence of a line block hole
	// 4 number of excess line block holes (holes > 3 blocks deep)
	private double[] weights;

	protected AtomicBoolean hasChanged;

	/**
	 * Creates a new GapFinder Ranker with a default reduction value.
	 */
	public GenericRanker() {
		this(DEFAULT_RANKER);
	}

	/**
	 * Creates a new GapFinder Ranker with the specified reduction value.
	 *
	 * @param weights - The number of points to reduce the rank per sealed gap in the state.
	 * @throws IllegalArgumentException if given weights array is null or
	 *     not of {@link GenericRanker#NUM_WEIGHTS} length.
	 */
	public GenericRanker(double[] weights) {
		if (weights == null || weights.length != NUM_WEIGHTS) {
			throw new IllegalArgumentException();
		}

		this.weights = new double[NUM_WEIGHTS];
		System.arraycopy(weights, 0, this.weights, 0, NUM_WEIGHTS);
		this.fitness = 0.0;
		this.hasChanged = new AtomicBoolean(true);
	}

	/**
	 * Copy constructor.
	 */
	public GenericRanker(GenericRanker r) {
		this.weights = new double[NUM_WEIGHTS];
		System.arraycopy(r.weights, 0, weights, 0, NUM_WEIGHTS);
		this.fitness = r.fitness;
		this.hasChanged = new AtomicBoolean(r.hasChanged());
	}

	/**
	 * Sets the weights of this ranker.
	 *
	 * @param newWeights New weights to set.
	 */
	public void setWeights(double[] newWeights) {
		for (int i = 0; i < NUM_WEIGHTS && i < weights.length; i++) {
			weights[i] = newWeights[i];
		}

		hasChanged.set(true);
	}

	@Override public boolean equals(Object obj) {
		boolean result = false;

		if (this == obj) {
			result = true;

		} else {
			if (obj instanceof GenericRanker) {
				GenericRanker ranker = (GenericRanker) obj;
				result = Arrays.equals(weights, ranker.weights);
			}
		}

		return result;
	}

	@Override public int hashCode() {
		return Arrays.hashCode(weights);
	}

	@Override public void mutate() {
		while (!mut()) {
		}

		hasChanged.set(true);
	}

	private boolean mut() {
		boolean result = false;
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for (int i = 0; i < weights.length; i++) {
			if (rand.nextDouble() < mutateRate) {
				weights[i] += (rand.nextDouble() * 2.0 - 1.0) * mutationAmplifier;
				result = true;
			}
		}

		return result;
	}

	@Override public GenericRanker[] cross(GenericRanker other) {
		if (!(other instanceof GenericRanker)) {
			throw new IllegalArgumentException();
		}

		GenericRanker otherCopy = (GenericRanker) other;

		GenericRanker[] result = new GenericRanker[] {
			new GenericRanker(this),
			new GenericRanker(otherCopy)
		};

		if (!this.equals(otherCopy)) {
			ThreadLocalRandom rand = ThreadLocalRandom.current();

			for (int i = 0; i < NUM_WEIGHTS; i++) {
				if (rand.nextDouble() < crossRate) {
					if (result[0].weights[i] != result[1].weights[i]) {
						double temp = result[0].weights[i];
						result[0].weights[i] = result[1].weights[i];
						result[1].weights[i] = temp;

						result[0].hasChanged.set(true);
						result[1].hasChanged.set(true);
					}
				}
			}
		}

		return result;
	}

	@Override
	public GenericRanker[] cross(GenericRanker[] others) {
		GenericRanker[] result = (GenericRanker[]) new Object[others.length * 2];

		for (int i = 0; i < others.length; i++) {
			System.arraycopy(this.cross(others[i]), 0, result, i * 2, 2);
		}

		return result;
	}

	@Override public void updateFitness() {
		super.updateFitness();
		hasChanged.set(false);
	}

	@Override
	public void updateSyncAndShow(TetrisBoardPanel panel) {
		super.updateSyncAndShow(panel);
		hasChanged.set(false);
	}

	public boolean hasChanged() {
		return hasChanged.get();
	}

	@Override public String json() {
		StringBuilder strb = new StringBuilder();

		strb.append("[");

		for (int i = 0; i < NUM_WEIGHTS; i++) {
			strb.append(String.format("%.3f", weights[i]));

			if (i < NUM_WEIGHTS - 1) {
				strb.append(", ");
			}
		}

		strb.append("]");

		return strb.toString();
	}

	@Override public String toString() {
		return json();
	}

	@Override protected double rankImpl(int[] features) {
		double result = 0.0;

		for (int i = 0; i < features.length; i++) {
			result += ((double) features[i]) * weights[i];
		}

		return result;
	}

	@Override public GenericRanker copy() {
		return new GenericRanker(this);
	}
}
