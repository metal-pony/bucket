package com.sparklicorn.tetrisai.structs;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.sparklicorn.bucket.util.genetic.IGenes;

// PolyFunc that also implements mutate() and cross(PolyFunc).
public class MutatingPolyFunc extends PolyFunc implements IGenes<MutatingPolyFunc> {
	public enum Param {
		MIN_TERMS(1.0),
		MAX_TERMS(2.0),

		COEFF_MUTATE_DELTA(10.0),
		EXP_MUTATE_DELTA(10.0),

		MIN_COEFF(-100.0),
		MAX_COEFF(100.0),
		MIN_EXP(-10.0),
		MAX_EXP(10.0),

		ADD_TERM_PROB(0.1),
		REMOVE_TERM_PROB(0.1),
		ADJUST_TERM_PROB(0.8),

		COEFF_MUTATE_PROB(0.75),
		EXP_MUTATE_PROB(0.25),

		COEFF_CROSS_PROB(0.5),
		EXP_CROSS_PROB(0.5);

		private double value;
		private Param(double defaultValue) {
			this.value = defaultValue;
		}
	}

	protected HashMap<Param, Double> params;

	/**
	 * Creates a new mutating PolyFunc with an empty set of terms.
	 */
	public MutatingPolyFunc() {
		this(new PolyFuncTerm[0]);
	}

	/**
	 * Creates a new mutating PolyFunc with the given terms.
	 *
	 * @param funcTerms Terms to add to this one.
	 */
	public MutatingPolyFunc(PolyFuncTerm...funcTerms) {
		super(funcTerms);
		initParams();
		initTerms();
	}

	/**
	 * Creates a new mutating PolyFunc with identical terms as the one given.
	 *
	 * @param other Mutating PolyFunc to copy terms from.
	 */
	public MutatingPolyFunc(MutatingPolyFunc other) {
		super(other);
		initParams();
		initTerms();
	}

	private void initParams() {
		this.params = new HashMap<>();
		for (Param p : Param.values()) {
			params.put(p, p.value);
		}
	}

	private void initTerms() {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		while (terms.size() < Param.MIN_TERMS.value) {
			double coeff = rand.nextDouble(Param.MIN_COEFF.value, Param.MAX_COEFF.value);
			double exp = rand.nextDouble(Param.MIN_EXP.value, Param.MAX_EXP.value);

			PolyFuncTerm newTerm = new PolyFuncTerm(coeff, exp);
			terms.add(newTerm);
		}
	}

	/**
	 * Sets a parameter to a given value.
	 *
	 * @param param Parameter to set.
	 * @param value Value of parameter.
	 */
	public void setParam(Param param, double value) {
		if (params.containsKey(param)) {
			params.put(param, value);
		}
	}

	public double getParam(Param param) {
		return params.get(param);
	}

	private static final double EPSILON = 1.0e-10;

	@Override public MutatingPolyFunc[] cross(MutatingPolyFunc other) {
		MutatingPolyFunc[] result = new MutatingPolyFunc[] {
			new MutatingPolyFunc(this),
			new MutatingPolyFunc(other)
		};

		if (!this.equals(other)) {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			boolean hasCrossed = false;
			int attempts = 0;

			while (!hasCrossed && attempts < 100) {
				// Choose terms to cross
				int index1 = rand.nextInt(result[0].terms.size());
				PolyFuncTerm term1 = result[0].terms.get(index1);

				int index2 = rand.nextInt(result[1].terms.size());
				PolyFuncTerm term2 = result[1].terms.get(index2);

				attempts++;
				if (!term1.equals(term2)) {
					double roll = rand.nextDouble(
					Param.COEFF_CROSS_PROB.value + Param.EXP_CROSS_PROB.value
					);

					if (roll < Param.COEFF_CROSS_PROB.value) {
						//swap coeffs if they are different
						if (Math.abs(term1.coeff - term2.coeff) > EPSILON) {
							double temp = term1.coeff;
							term1.coeff = term2.coeff;
							term2.coeff = temp;
							hasCrossed = true;
						}
					} else {
						// Swap exps if they are different
						if (Math.abs(term1.exp - term2.exp) > EPSILON) {
							double temp = term1.exp;
							term1.exp = term2.exp;
							term2.exp = temp;
							hasCrossed = true;
						}
					}
				}
			}

		}

		return result;
	}

	@Override
	public MutatingPolyFunc[] cross(MutatingPolyFunc[] other) {
		// TODO #14 Refine genetic algorithm phase implementation
		return null;
	}

	@Override
	public void mutate() {
		boolean hasMutated = false;

		ThreadLocalRandom rand = ThreadLocalRandom.current();
		double rollCeil = Param.ADD_TERM_PROB.value +
			Param.REMOVE_TERM_PROB.value +
			Param.ADJUST_TERM_PROB.value;

		while (!hasMutated) {
			// Decide how to mutate
			double roll = rand.nextDouble(rollCeil);

			if (
				roll < Param.ADD_TERM_PROB.value &&
				terms.size() < Param.MAX_TERMS.value
			) {
				// Add another term
				// Choose a random coeff/exp with param constraints
				terms.add(new PolyFuncTerm(
						rand.nextDouble(Param.MIN_COEFF.value, Param.MAX_COEFF.value),
						rand.nextDouble(Param.MIN_EXP.value, Param.MAX_EXP.value)
				));
				hasMutated = true;

			} else if (
				roll < Param.REMOVE_TERM_PROB.value &&
				terms.size() > Param.MIN_TERMS.value
			) {
				// Remove a random term
				int index = rand.nextInt(terms.size());
				terms.remove(index);
				hasMutated = true;

			} else {
				// Adjust one of the terms
				// Choose a random term to adjust
				int index = rand.nextInt(terms.size());
				PolyFuncTerm term = terms.get(index);

				// Decide if adjusting coeff or exp
				roll = rand.nextDouble(Param.COEFF_MUTATE_PROB.value + Param.EXP_MUTATE_PROB.value);

				if (roll < Param.COEFF_MUTATE_PROB.value) {
					// Choose a delta to adjust by with param constraints
					roll = (rand.nextDouble(Param.COEFF_MUTATE_DELTA.value) * 2.0) - Param.COEFF_MUTATE_DELTA.value;
					// If param value + adjust value is within constraints, then do it
					if (
						term.coeff + roll <= Param.MAX_COEFF.value &&
						term.coeff + roll >= Param.MIN_COEFF.value
					) {
						term.coeff += roll;
						hasMutated = true;
					}

				} else {
					roll = (rand.nextDouble(Param.EXP_MUTATE_DELTA.value) * 2.0) - Param.EXP_MUTATE_DELTA.value;
					if (
						term.exp + roll <= Param.MAX_EXP.value &&
						term.exp + roll >= Param.MIN_EXP.value
					) {
						term.exp += roll;
						hasMutated = true;
					}
				}
			}
		}
	}

	@Override
	public double getFitness() {
		// TODO #14 Refine genetic algorithm phase implementation
		return 0;
	}
}
