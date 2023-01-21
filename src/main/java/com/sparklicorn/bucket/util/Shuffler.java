package com.sparklicorn.bucket.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * A Shuffle utility that implements a version of the Fisher-Yates shuffling algorithm.
 */
public final class Shuffler {

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
