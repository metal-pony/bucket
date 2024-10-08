package com.metal_pony.tetrisai.structs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PolyFunc {
	/**
	 * Represents a term in a polynomial function.
	 */
	public static class PolyFuncTerm {
		protected double coeff;
		protected double exp;

		/**
		 * Creates a new PolyFunc term given a coefficient and exponent.
		 *
		 * @param coeff Coefficient
		 * @param exp Exponent
		 */
		public PolyFuncTerm(double coeff, double exp) {
			this.coeff = coeff;
			this.exp = exp;
		}

		/**
		 * Creates a new PolyFunc term with the same coefficient and exponent as the term given.
		 *
		 * @param other Other PolyFunc term to copy.
		 */
		public PolyFuncTerm(PolyFuncTerm other) {
			this(other.coeff, other.exp);
		}

		/**
		 * Calculates the term's value given a value for the variable.
		 *
		 * @param x Value for the variable of the term.
		 */
		public double calc(double x) {
			return coeff * Math.pow(x, exp);
		}

		@Override public boolean equals(Object other) {
			if (this == other) {
				return true;
			}

			if (other instanceof PolyFuncTerm) {
				PolyFuncTerm otherTerm = (PolyFuncTerm) other;
				double epsilon = 1.0e-10;

				if (
					Math.abs(coeff - otherTerm.coeff) < epsilon &&
					Math.abs(exp - otherTerm.exp) < epsilon
				) {
					return true;
				}
			}

			return false;
		}

		@Override public int hashCode() {
			return Double.hashCode(coeff) + 31 * Double.hashCode(exp);
		}

		@Override public String toString() {
			return String.format("(%f * x^%f)", coeff, exp);
		}
	}

	//////////////////////////////

	protected List<PolyFuncTerm> terms;

	public PolyFunc() {
		this(new PolyFuncTerm[0]);
	}

	/**
	 * Creates a new PolyFunc with the given terms.
	 *
	 * @param funcTerms PolyFunc terms to add to this one.
	 */
	public PolyFunc(PolyFuncTerm...funcTerms) {
		this.terms = new LinkedList<>();
		if (funcTerms != null) {
			for (PolyFuncTerm term : funcTerms) {
				addTerm(term);
			}
		}
	}

	/**
	 * Creates a new PolyFunc with identical terms to the one given.
	 *
	 * @param other PolyFunc to copy terms from.
	 */
	public PolyFunc(PolyFunc other) {
		this();

		for (PolyFuncTerm otherTerm : other.terms) {
			terms.add(new PolyFuncTerm(otherTerm));
		}
	}

	public void addTerm(PolyFuncTerm term) {
		terms.add(term);
	}

	public void addTerm(double coeff, double exp) {
		terms.add(new PolyFuncTerm(coeff, exp));
	}

	public boolean removeTerm(PolyFuncTerm term) {
		return terms.remove(term);
	}

	public void clearTerms() {
		terms.clear();
	}

	/**
	 * Calculates the value of the function given a value for the variable.
	 *
	 * @param x Value of the variable.
	 */
	public double calc(double x) {
		if (terms.size() == 0) {
			throw new IllegalArgumentException("NOT TERMS IN FUNC");
		}

		double result = 0;

		for (PolyFuncTerm term : terms) {
			double r = term.calc(x);
			if (Double.isFinite(r)) {
				result += r;
			}
		}

		return result;
	}

	@Override public String toString() {
		StringBuilder strb = new StringBuilder();

		strb.append('[');
		Iterator<PolyFuncTerm> termsIter = terms.iterator();
		while (termsIter.hasNext()) {
			PolyFuncTerm t = termsIter.next();
			strb.append(t.toString());
			if (termsIter.hasNext()) {
				strb.append(" + ");
			}
		}
		strb.append(']');

		return strb.toString();
	}

	@Override public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof PolyFunc) {
			PolyFunc otherPolyFunc = (PolyFunc) other;
			if (terms.size() == otherPolyFunc.terms.size()) {
				for (PolyFuncTerm t : terms) {
					if (!otherPolyFunc.terms.contains(t)) {
						return false;
					}
				}
				for (PolyFuncTerm t : otherPolyFunc.terms) {
					if (!terms.contains(t)) {
						return false;
					}
				}
				//this.terms and _obj terms have equivalent elements
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		int result = 0;
		for (PolyFuncTerm t : terms) {
			result += t.hashCode();
		}
		return result;
	}
}
