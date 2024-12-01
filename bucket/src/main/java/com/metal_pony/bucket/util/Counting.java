package com.metal_pony.bucket.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Contains utility functions pertaining to counting, including
 * functions for calculating permutation and combination sets from
 * given collections of objects.
 */
public class Counting {
	private static List<BigInteger> factMap = new ArrayList<>(Arrays.asList(BigInteger.ONE));

	/**
	 * Computes the factorial of the given number.
	 * Just a reminder because many forget: <code>0! = 1</code>.
	 *
	 * @param n - Number to compute the factorial of.
	 * @return <code>n!</code> as a BigInteger.
	 */
	public static BigInteger factorial(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

		if (n < factMap.size()) {
			return factMap.get(n);
		}

		int i = factMap.size() - 1;
		BigInteger result = factMap.get(i);

		for (i++; i <= n; i++) {
			result = result.multiply(BigInteger.valueOf(i));
			factMap.add(result);
		}

		return result;
	}

	/**
	 * Generates a random BigInteger in the interval [0, bound) with the given random generator.
	 */
	public static BigInteger random(BigInteger bound, Random rand) {
		if (bound.compareTo(BigInteger.ZERO) <= 0) {
			throw new IllegalArgumentException("bound must be positive");
		}

		BigInteger result = new BigInteger(bound.bitLength(), rand);
		while (result.compareTo(bound) >= 0) {
			result = new BigInteger(bound.bitLength(), rand);
		}
		return result;
	}

	/**
	 * Computes all permutations of the given list.
	 *
	 * @param <T> Type of object that the list contains.
	 * @param list - Contains items to compute permutations of.
	 * @return Set containing all permutations of the given list of items.
	 */
	public static <T> Set<List<T>> allPermutations(List<T> list) {
		HashSet<List<T>> resultSet = new HashSet<>();
		if (list.size() == 1) {
			resultSet.add(list);
		} else if (list.size() > 1) {
			for (T e : list) {
				List<T> sublist = new ArrayList<>(list);
				sublist.remove(e);
				Set<List<T>> r = allPermutations(sublist);
				for (List<T> x : r) {
					x.add(e);
					resultSet.add(x);
				}
			}
		}
		return resultSet;
	}

	/**
	 * Computes n choose k.
	 */
	public static BigInteger nChooseK(int n, int k) {
		if (n < 0 || k < 0 || n < k) {
			throw new IllegalArgumentException("n and k must both be >= 0 and n must be >= k.");
		}

		if (k == 0 || n == k) {
			return BigInteger.ONE;
		}

		return factorial(n).divide(factorial(k)).divide(factorial(n - k));
	}

	/**
	 * Generates a random combination, choosing k numbers randomly from the interval [0,n).
	 * The following must be true: <code>n >= k >= 0</code>.
	 */
	public static int[] randomCombo(int n, int k) {
		if (n < 0 || k < 0 || n < k) {
			throw new IllegalArgumentException("n and k must both be >= 0 and n must be >= k.");
		}

		if (k == 0) {
			return new int[0];
		}

		int[] items = Shuffler.rangeArr(n);
		Shuffler.shuffle(items);
		return Arrays.copyOfRange(items, 0, k);

		// This is an alternate approach in which a combination is generated via an index r.
		// BigInteger r = random(factorial(n), ThreadLocalRandom.current());
		// return combo(n, k, r);
	}

	/**
	 * Generates a subset of k numbers from the interval [0,n), given a number r from [0, n choose k).
	 */
	public static int[] combo(int n, int k, BigInteger r) {
		// validate r
		if (r.compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("r must be nonnegative");
		}
		BigInteger nChooseK = nChooseK(n, k);
		if (r.compareTo(nChooseK) >= 0) {
			throw new IllegalArgumentException(
				String.format("r must be in interval [0, n choose k (%s))", nChooseK.toString())
			);
		}

		int[] result = new int[k];

		// Anything choose 0 is 1. There's only one way to choose nothing, i.e. the empty set.
		if (k == 0) {
			return result;
		}

		int _n = n - 1;
		int _k = k - 1;
		BigInteger _r = new BigInteger(r.toString());

		int index = 0;
		for (int i = 0; i < n; i++) {
			BigInteger _nChoose_k = nChooseK(_n, _k);
			if (_r.compareTo(_nChoose_k) < 0) {
				result[index++] = i;
				_k--;

				if (index == k) {
					break;
				}
			} else {
				_r = _r.subtract(_nChoose_k);
			}
			_n--;

		}

		return result;
	}

	/**
	 * Generates a bitstring (length n) with k bits set.
	 * If there are (n choose k) possible bitstrings, this generates the r-th.
	 * Represented as an array of unsigned bytes.
	 */
	public static byte[] bitCombo(int n, int k, BigInteger r) {
		if (r.compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("r must be nonnegative");
		}

		// Throws if n < 0 or k < 0 or n < k
		BigInteger nck = nChooseK(n, k);

		if (r.compareTo(nck) >= 0) {
			throw new IllegalArgumentException("r must be in interval [0, (n choose k))");
		}

		BigInteger _r = new BigInteger(r.toByteArray());
		int nBytes = n / Byte.SIZE;
		int remBits = n % Byte.SIZE;
		if (remBits > 0) {
			nBytes++;
		}
		byte[] _result = new byte[nBytes];

		for (int _n = n - 1, _k = k - 1; _n >= 0 && _k >= 0; _n--) {
			BigInteger _nck = nChooseK(_n, _k);
			if (_r.compareTo(_nck) < 0) {
				int arrIndex = nBytes - 1 - (_n / Byte.SIZE);
				int bIndex = _n % Byte.SIZE;
				_result[arrIndex] |= (1 << bIndex);
				_k--;
			} else {
				_r = _r.subtract(_nck);
			}
		}

		return _result;
	}

	/**
	 * Generates a random permutation of the numbers 0 to n.
	 */
	public static int[] randomPermutation(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

		return Shuffler.shuffle(Shuffler.rangeArr(n));

		// This is an alternate approach in which a permutation is generated via an index r.
		// BigInteger r = random(factorial(n), ThreadLocalRandom.current());
		// return permutation(n, r);
	}

	/**
	 * Generates a permutation of the numbers [0,n), given a number r from [0, n!).
	 */
	public static int[] permutation(int n, BigInteger r) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

		int[] result = new int[n];
		int[] items = Shuffler.rangeArr(n);

		for (int i = 0; i < n; i++) {
			r = r.mod(factorial(n - i));
			BigInteger dividend = factorial(n - i - 1);
			int q = r.divide(dividend).intValue();
			result[i] = items[q];

			// Shift items whose index > q to the left one place
			for (int j = q + 1; j < n - i; j++) {
				items[j - 1] = items[j];
			}
		}

		return result;
	}
}
