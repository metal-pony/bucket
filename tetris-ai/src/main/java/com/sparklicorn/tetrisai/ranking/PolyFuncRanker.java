package com.sparklicorn.tetrisai.ranking;

import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.tetrisai.genetic.AbstractRankerGene;
import com.sparklicorn.tetrisai.structs.MutatingPolyFunc;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class PolyFuncRanker extends AbstractRankerGene<PolyFuncRanker> {
	/**
	 * Number of functions composing a ranker.
	 */
	private static final int NUM_FUNCS = 5;

	/**
	 * The rate at which functions will cross.
	 */
	protected double crossRate = 0.25;

	/**
	 * The rate at which a function will mutate.
	 */
	protected double mutateRate = 0.25;

	public void setCrossRate(double newCrossRate) {
		crossRate = newCrossRate;
	}

	public void setMutateRate(double newMutateRate) {
		mutateRate = newMutateRate;
	}

	private MutatingPolyFunc[] funcs;
	protected AtomicBoolean hasChanged;

	/**
	 * Creates a new GapFinder Ranker with a default reduction value.
	 */
	public PolyFuncRanker() {
		this(new MutatingPolyFunc[] {
			new MutatingPolyFunc(),
			new MutatingPolyFunc(),
			new MutatingPolyFunc(),
			new MutatingPolyFunc(),
			new MutatingPolyFunc()
		});
	}

	/**
	 * Creates a new ranker with the specified functions.
	 *
	 * @param funcs - Functions that compose the ranker.
	 * @throws IllegalArgumentException if given functions is null or
	 *     not of {@link PolyFuncRanker#NUM_FUNCS} length.
	 */
	public PolyFuncRanker(MutatingPolyFunc[] funcs) {
		if (funcs == null || funcs.length != NUM_FUNCS) {
			throw new IllegalArgumentException();
		}
		this.funcs = new MutatingPolyFunc[NUM_FUNCS];
		for (int i = 0; i < NUM_FUNCS; i++) {
			this.funcs[i] = new MutatingPolyFunc(funcs[i]);
		}
		fitness = 0.0;
		hasChanged = new AtomicBoolean(true);
	}

	/** Copy constructor.*/
	public PolyFuncRanker(PolyFuncRanker r) {
		this(r.funcs);
		fitness = r.fitness;
		hasChanged.set(r.hasChanged());
	}

	public void setFunc(int index, MutatingPolyFunc func) {
		this.funcs[index] = func;
		hasChanged.set(true);
	}

	/**
	 * Sets the functions that compose this ranker.
	 *
	 * @param newFuncs New functions to set.
	 */
	public void setFuncs(MutatingPolyFunc[] newFuncs) {
		for (int i = 0; i < NUM_FUNCS; i++) {
			funcs[i] = newFuncs[i];
		}

		hasChanged.set(true);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof PolyFuncRanker) {
			PolyFuncRanker ranker = (PolyFuncRanker) obj;

			for (int i = 0; i < funcs.length; i++) {
				if (!funcs[i].equals(ranker.funcs[i])) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int result = 0;
		result += funcs[0].hashCode();
		result += 31 * funcs[1].hashCode();
		result += 47 * funcs[2].hashCode();
		result += 59 * funcs[3].hashCode();
		result += 71 * funcs[4].hashCode();
		return result;
	}

	@Override
	public void mutate() {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		//guarantee that something will change
		boolean somethingChanged = false;
		while (!somethingChanged) {
			for (int i = 0; i < NUM_FUNCS; i++) {
				double roll = rand.nextDouble();
				if (roll < mutateRate) {
					funcs[i].mutate();
					somethingChanged = true;
					hasChanged.set(true);
				}
			}
		}
	}

	@Override
	public PolyFuncRanker[] cross(PolyFuncRanker other) {
		PolyFuncRanker[] result = new PolyFuncRanker[] {
			new PolyFuncRanker(this),
			new PolyFuncRanker(other)
		};

		ThreadLocalRandom rand = ThreadLocalRandom.current();

		//guarantee that something will cross
		boolean crossed = false;
		while (!crossed) {
			for (int i = 0; i < NUM_FUNCS; i++) {
				double roll = rand.nextDouble();
				if (roll < crossRate) {

					boolean parentsFound = false;
					int attempts = 0;
					MutatingPolyFunc p1 = funcs[i];
					int p2Index = 0;
					MutatingPolyFunc p2 = null;
					while (!parentsFound && attempts < 100) {
						attempts++;
						p2Index = rand.nextInt(NUM_FUNCS);
						if (!funcs[p2Index].equals(p1)) {
							p2 = funcs[p2Index];
							parentsFound = true;
						}
					}

					MutatingPolyFunc[] successors = p1.cross(p2);
					result[0].setFunc(i, successors[0]);
					result[1].setFunc(p2Index, successors[1]);

					result[0].hasChanged.set(true);
					result[1].hasChanged.set(true);

					crossed = true;
				}
			}
		}

		return result;
	}

	@Override
	public PolyFuncRanker[] cross(PolyFuncRanker[] others) {
		PolyFuncRanker[] result = (PolyFuncRanker[]) new Object[others.length * 2];

		for (int i = 0; i < others.length; i++) {
			System.arraycopy(this.cross(others[i]), 0, result, i * 2, 2);
		}

		return result;
	}

	public void updateFitness() {
		super.updateFitness();
		hasChanged.set(false);
	}

	public boolean hasChanged() {
		return hasChanged.get();
	}

	@Override
	public String toString() {
		return json();
	}

	@Override
	public String json() {
		StringBuilder strb = new StringBuilder();

		strb.append("[");
		for (int i = 0; i < NUM_FUNCS; i++) {
			strb.append(funcs[i]);
			if (i < NUM_FUNCS - 1) {
				strb.append(", ");
			}
		}
		strb.append("]");

		return strb.toString();
	}

	@Override
	public PolyFuncRanker copy() {
		return new PolyFuncRanker(this);
	}

	@Override
	public double rank(TetrisState state) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'rank'");
	}
}
