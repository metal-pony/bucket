package com.sparklicorn.tetrisai.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.tetrisai.genetic.AbstractRankerGene;

public class GenericRanker extends AbstractRankerGene<GenericRanker> {
	public static class HeuristicWeight {
		private float weight;
		private RankingHeuristic heuristic;

		HeuristicWeight(RankingHeuristic heuristic, float weight) {
			this.heuristic = heuristic;
			this.weight = weight;
		}

		public float weight() {
			return weight;
		}

		public void setWeight(float newWeight) {
			this.weight = newWeight;
		}

		public void adjust(float amount) {
			this.weight += amount;
		}

		public RankingHeuristic heuristic() {
			return heuristic;
		}

		public String name() {
			return heuristic.name;
		}

		public double quantify(TetrisState state) {
			return (double)heuristic.quantify(state) * (double)weight;
		}

		HeuristicWeight copy() {
			return new HeuristicWeight(heuristic, weight);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || !(obj instanceof HeuristicWeight))
				return false;

			HeuristicWeight _obj = (HeuristicWeight) obj;
			return name().equals(_obj.name()) && weight == _obj.weight;
		}

		@Override
		public int hashCode() {
			return Float.hashCode(weight) + heuristic.name.hashCode();
		}

		@Override
		public String toString() {
			return String.format("{ \"%s\": %.2f }", name(), weight());
		}
	}

	private static final HeuristicWeight[] DEFAULT_RANKERS = new HeuristicWeight[] {
		new HeuristicWeight(new CompleteLinesRankingHeuristic(), 3.56f),
		new HeuristicWeight(new BlockHeightSumRankingHeuristic(), -1.37f),
		new HeuristicWeight(new BlockedSpacesRankingHeuristic(), -17.40f),
		new HeuristicWeight(new DeepPocketsRankingHeuristic(), -12.83f),
		new HeuristicWeight(new DeepSidePocketsRankingHeuristic(), -10.74f)
	};

	public static List<HeuristicWeight> getDefaultHeuristicWeights() {
		ArrayList<HeuristicWeight> list = new ArrayList<>();
		for (HeuristicWeight hw : DEFAULT_RANKERS) {
			list.add(hw.copy());
		}
		return list;
	}

	public static List<HeuristicWeight> getRandomHeuristicWeights(float origin, float bound) {
		List<HeuristicWeight> weights = getDefaultHeuristicWeights();
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for (HeuristicWeight hw : weights) {
			hw.setWeight(rand.nextFloat(origin, bound));
		}

		return weights;
	}

	/** Number of weights used by this ranker algorithm.*/
	public static final int NUM_WEIGHTS = 5;

	private static float mutationAmplifier = 10f;

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
	// private double[] weights;

	// TODO wire these up in constructor. Adds method to set/get weight by heuristic name.
	// TODO update rest of methods to use this instead of `weights` array.
	private List<HeuristicWeight> heuristicWeights;

	protected AtomicBoolean hasChanged;

	/**
	 * Creates a new GapFinder Ranker with a default reduction value.
	 */
	public GenericRanker() {
		this(getDefaultHeuristicWeights());
	}

	/**
	 * Creates a new GapFinder Ranker with the specified reduction value.
	 *
	 * @param weights - The number of points to reduce the rank per sealed gap in the state.
	 * @throws IllegalArgumentException if given weights array is null or
	 *     not of {@link GenericRanker#NUM_WEIGHTS} length.
	 */
	public GenericRanker(List<HeuristicWeight> weights) {
		if (weights == null) {
			throw new IllegalArgumentException();
		}

		this.heuristicWeights = weights;
		this.fitness = 0.0;
		this.hasChanged = new AtomicBoolean(true);
	}

	/**
	 * Copy constructor.
	 */
	public GenericRanker(GenericRanker other) {
		this.heuristicWeights = new ArrayList<>();
		for (HeuristicWeight otherHw : other.heuristicWeights) {
			this.heuristicWeights.add(otherHw.copy());
		}
		this.fitness = other.fitness;
		this.hasChanged = new AtomicBoolean(other.hasChanged());
	}

	public HeuristicWeight getWeightByName(String name) {
		for (HeuristicWeight hw : heuristicWeights) {
			if (hw.heuristic().name.equals(name)) {
				return hw;
			}
		}
		return null;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof GenericRanker))
			return false;

		GenericRanker _obj = (GenericRanker) obj;
		return heuristicWeights.equals(_obj.heuristicWeights);
	}

	@Override public int hashCode() {
		return heuristicWeights.hashCode();
	}

	@Override public void mutate() {
		while (!mut()) {
		}

		hasChanged.set(true);
	}

	private boolean mut() {
		AtomicBoolean anyChanged = new AtomicBoolean();
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		heuristicWeights.forEach((hw) -> {
			if (rand.nextDouble() < mutateRate) {
				hw.adjust((rand.nextFloat() * 2f - 1f) * mutationAmplifier);
				anyChanged.set(true);
			}
		});

		return anyChanged.get();
	}

	private boolean containsSameWeight(HeuristicWeight weight) {
		for (HeuristicWeight hw : heuristicWeights) {
			if (hw.equals(weight)) {
				return true;
			}
		}
		return false;
	}

	@Override public GenericRanker[] cross(GenericRanker other) {
		GenericRanker[] result = new GenericRanker[] {
			new GenericRanker(this),
			new GenericRanker(other)
		};

		if (equals(other)) {
			return result;
		}

		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for (HeuristicWeight r0Weight : result[0].heuristicWeights) {
			if (rand.nextDouble() >= crossRate)
				continue;

			HeuristicWeight r1Weight = result[1].getWeightByName(r0Weight.name());
			if (!result[1].containsSameWeight(r0Weight)) {
				float temp = r0Weight.weight();
				r0Weight.setWeight(r1Weight.weight());
				result[0].hasChanged.set(true);
				r1Weight.setWeight(temp);
				result[1].hasChanged.set(true);
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
		strb.append(System.lineSeparator());
		for (int i = 0; i < heuristicWeights.size(); i++) {
			strb.append("  ");
			strb.append(heuristicWeights.get(i).toString());
			if (i < heuristicWeights.size() - 1) {
				strb.append(", ");
			}
			strb.append(System.lineSeparator());
		}
		strb.append("]");

		return strb.toString();
	}

	@Override public String toString() {
		return json();
	}

	@Override
	public double rank(TetrisState state) {
		double rank = 0.0;
		// System.out.print("Rank = ");
		for (HeuristicWeight hw : heuristicWeights) {
			// double quantify = hw.quantify(game);
			double quantity = hw.heuristic.quantify(state);
			double weight = hw.weight;
			rank += quantity * weight;
			// System.out.printf("%.2f(%.2f) + ", weight, quantity);
		}
		// System.out.printf("=> %.2f\n", rank);
		return rank;
	}

	@Override
	public GenericRanker copy() {
		return new GenericRanker(this);
	}

    public List<HeuristicWeight> getHeuristicWeights() {
        return this.heuristicWeights;
    }

	public String addr() {
		return super.toString();
	}
}
