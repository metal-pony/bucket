package com.metal_pony.bucket.util;

import java.util.function.Function;

/**
 * Provides utilities for working with arrays.
 */
public class Array {
    /**
     * Returns a copy of the given array.
     */
    public static int[] copy(int[] source) {
        int[] result = new int[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Returns a copy of the given array.
     */
    public static long[] copy(long[] source) {
        long[] result = new long[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Returns a copy of the given array.
     */
    public static boolean[] copy(boolean[] source) {
        boolean[] result = new boolean[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Returns a copy of the given array.
     */
    public static float[] copy(float[] source) {
        float[] result = new float[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Returns a copy of the given array.
     */
    public static double[] copy(double[] source) {
        double[] result = new double[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Returns a copy of the given array.
     */
    public static <T> T[] copy(T[] source) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /**
     * Attempts to fill the given array with the results of the given function.
     * The function takes an array index as input.
     */
    public static <T> T[] fillWithFunc(T[] array, Function<Integer,T> func) {
        for (int i = 0; i < array.length; i++) {
            array[i] = func.apply(i);
        }
        return array;
    }
}
