package com.sparklicorn.bucket.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import com.google.gson.Gson;

/**
 * A Shuffle utility that implements a version of the Fisher-Yates shuffling algorithm.
 */
public final class Shuffler {

	static int[][] allPermutations(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

        int nFact = Counting.factorial(n);
		int[][] result = new int[nFact][n];
        for (int i = 0; i < nFact; i++) {
            result[i] = permutation(n, BigInteger.valueOf(i));
        }

		return result;
	}

	static void main(String[] args) {
		Gson gson = new Gson();

		// Generate table for nChooseK tests
		for (int n = 0; n < 200; n++) {
			String[] nChooseK = new String[n + 1];

			for (int k = 0; k <= n; k++) {
				nChooseK[k] = nChooseK(n, k).toString();
			}

			System.out.printf("%d|%s\n", n, gson.toJson(nChooseK));
		}

		// Generate table for permutations test
		// System.out.println("n,array_of_expected_perms_json");
		// for (int n = 1; n < 10; n++) {
		// 	int[][] perms = allPermutations(n);
		// 	System.out.println(n + ";" + gson.toJson(perms));
		// }
	}

	public static BigInteger factorial(long n) {
		BigInteger result = (n < 1L) ? BigInteger.valueOf(1L) : BigInteger.valueOf(n);
		for (long i = n - 1L; i > 1L; i--) {
			result = result.multiply(BigInteger.valueOf(i));
		}
		return result;
	}

	public static BigInteger nChooseK(long n, long k) {
		if (n < 0L || k < 0L) {
			throw new ArithmeticException("n Choose k: both n and k must be nonnegative");
		}

		if (k == 0L || n == k) {
			return BigInteger.valueOf(1L);
		}

		return factorial(n).divide(factorial(k)).divide(factorial(n - k));
	}

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

	public static int[] randomCombo(int n, int k) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

		BigInteger r = random(factorial(n), ThreadLocalRandom.current());

		return combo(n, k, r);
	}

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

	public static int[] randomPermutation(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

		BigInteger r = random(factorial(n), ThreadLocalRandom.current());

		return permutation(n, r);
	}

	public static int[] permutation(int n, BigInteger r) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be nonnegative");
		}

		int[] result = new int[n];
		int[] items = rangeArr(n);

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

	public static int[] rangeArr(int n) {
		int[] result = new int[n];
		for (int i = 0; i < n; i++) {
			result[i] = i;
		}
		return result;
	}

	/**
	 * Returns a list of ints in the range [0, to (exclusive)).
	 */
	public static List<Integer> range(int to) {
		return rangeFiltered(0, to, (n) -> true);
	}

	/**
	 * Returns a list of ints in the range [from, to (exclusive)).
	 */
	public static List<Integer> range(int from, int to) {
		return range(from, to, 1);
	}

	/**
	 * Returns a list of ints in the range [from, to (exclusive)).
	 */
	public static List<Integer> range(int from, int to, int increment) {
		List<Integer> result = new ArrayList<>();
		for (int n = from; n < to; n+=increment) {
			result.add(n);
		}
		return result;
	}

	/**
	 * Returns a list of ints in the range [0, to) that are accepted by the filter function.
	 */
	public static List<Integer> rangeFiltered(int from, int to, Function<Integer,Boolean> filter) {
		List<Integer> result = new ArrayList<>();
		for (int n = from; n < to; n++) {
			if (filter.apply(n)) {
				result.add(n);
			}
		}
		return result;
	}

	/**
	 * Returns a Set of random ints with the given size.
	 */
	public static Set<Integer> randomSet(int size) {
		return randomSet(new HashSet<>(), size, Integer.MIN_VALUE, Integer.MAX_VALUE, (n) -> true);
	}

	/*
	 * Returns a Set with the given size, containing random ints from interval [origin, bound).
	 */
	public static Set<Integer> randomSet(int size, int origin, int bound) {
		return randomSet(new HashSet<>(), size, origin, bound, (n) -> true);
	}

	/*
	 * Returns a Set with the given size, containing random ints from interval [origin, bound)
	 * that are accepted by the filter function.
	 */
	public static Set<Integer> randomSet(int size, int origin, int bound, Function<Integer, Boolean> filter) {
		return randomSet(new HashSet<>(), size, origin, bound, filter);
	}

	/**
	 * Clears the given Set, then repopulates it with random ints from the interval [origin, bound)
	 * that are accepted by the filter function.
	 *
	 * @return The given set.
	 */
	public static Set<Integer> randomSet(Set<Integer> set, int size, int origin, int bound, Function<Integer, Boolean> filter) {
		if ((long)size > (long)bound - (long)origin) {
			throw new IllegalArgumentException("Requested size exceeds number of possibilities.");
		}

		ThreadLocalRandom generator = ThreadLocalRandom.current();
		set.clear();
		while (set.size() < size) {
			int rand = generator.nextInt(origin, bound);
			if (filter.apply(rand)) {
				set.add(rand);
			}
		}
		return set;
	}

	public static int[] random(int[] arr) {
		return random(arr, 0, Integer.MAX_VALUE);
	}

	public static int[] random(int[] arr, int bound) {
		return random(arr, 0, bound);
	}

	public static int[] random(int[] arr, int origin, int bound) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		for (int i = 0; i < arr.length; i++) {
			arr[i] = rand.nextInt(origin, bound);
		}
		return arr;
	}

	public static <T> List<T> shuffleList(final List<T> list) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for(int i = list.size() - 1; i > 0; i--) {
			swapList(list, i, rand.nextInt(i + 1));
		}

		return list;
	}

	public static int[] shuffleInts(final int[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}

		return arr;
	}

	private final RandomGenerator rand;

	/**
	 * Creates a new Shuffler with ThreadLocalRandom as random class.
	 */
	public Shuffler() {
		this(ThreadLocalRandom.current());
	}

	/**
	 * Creates a new Shuffles with the given RandomGenerator.
	 * @param rand
	 */
	public Shuffler(RandomGenerator rand) {
		this.rand = rand;
	}

	/**
	 * Shuffles the given list in-place.
	 * @param list
	 * @return the same List, for convenience
	 */
	public <T> List<T> shuffle(final List<T> list) {
		for(int i = list.size() - 1; i > 0; i--) {
			swapList(list, i, rand.nextInt(i + 1));
		}
		return list;
	}

	/**
	 * Shuffles the given array in-place.
	 * @param arr
	 * @return the same array, for convenience
	 */
	public <T> T[] shuffle(final T[] arr) {
		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	/**
	 * Shuffles the given array in-place.
	 * @param arr
	 * @return the same array, for convenience
	 */
	public int[] shuffle(final int[] arr) {
		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	/**
	 * Shuffles the given array in-place.
	 * @param arr
	 * @return the same array, for convenience
	 */
	public long[] shuffle(final long[] arr) {
		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	/**
	 * Shuffles the given array in-place.
	 * @param arr
	 * @return the same array, for convenience
	 */
	public float[] shuffle(final float[] arr) {
		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	/**
	 * Shuffles the given array in-place.
	 * @param arr
	 * @return the same array, for convenience
	 */
	public double[] shuffle(final double[] arr) {
		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	/**
	 * Shuffles the given array in-place.
	 * @param arr
	 * @return the same array, for convenience
	 */
	public char[] shuffle(final char[] arr) {
		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	public static <T> void swapList(final List<T> list, int a, int b) {
		final T temp = list.get(a);
		list.set(a, list.get(b));
		list.set(b, temp);
	}

	public static <T> void swap(final T[] arr, int a, int b) {
		T temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	public static void swap(char[] arr, int a, int b) {
		char temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	public static void swap(int[] arr, int a, int b) {
		int temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	public static void swap(long[] arr, int a, int b) {
		long temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	public static void swap(float[] arr, int a, int b) {
		float temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	public static void swap(double[] arr, int a, int b) {
		double temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}
}
