package com.metal_pony.bucket.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * A Shuffling utility that implements a version of the Fisher-Yates shuffling algorithm.
 */
public class Shuffler {
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

	/**
	 * Shuffles the given list in-place.
	 * @param list
	 * @return the same List, for convenience
	 */
	public static <T> List<T> shuffle(List<T> list) {
		if (list.isEmpty()) return list;
		ThreadLocalRandom rand = ThreadLocalRandom.current();

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
	public static <T> T[] shuffle(T[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

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
	public static int[] shuffle(int[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

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
	public static long[] shuffle(long[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

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
	public static float[] shuffle(float[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

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
	public static double[] shuffle(double[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

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
	public static char[] shuffle(char[] arr) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for(int i = arr.length - 1; i > 0; i--) {
			swap(arr, i, rand.nextInt(i + 1));
		}
		return arr;
	}

	public static <T> void swapList(List<T> list, int a, int b) {
		T temp = list.get(a);
		list.set(a, list.get(b));
		list.set(b, temp);
	}

	public static <T> void swap(T[] arr, int a, int b) {
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
