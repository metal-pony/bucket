package com.metal_pony.bucket.util;

import java.util.Collection;

/**
 * Contains statistics functions for calculating means, variances, and standard deviations.
 */
public class Stats {
    /**
     * Calculates the mean of the given List of numbers.
     * @param numbers - Contains numbers to calculate mean of.
     * @return mean
     */
    public static <N extends Number> double mean(Collection<N> numbers) {
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Numbers collection cannot be empty.");
        }
        double total = 0.0;
        for (Number x : numbers) {
            total += x.doubleValue();
        }
        return total / (double)numbers.size();
    }

    /**
     * Calculates the mean of the given array of ints.
     * @param arr
     * @return mean
     */
    public static double mean(int... arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("arr cannot be empty.");
        }
        double total = 0.0;
        for (long x : arr) {
            total += (double)x;
        }
        return total / (double)arr.length;
    }

    /**
     * Calculates the mean of the given array of ints.
     * @param arr
     * @return mean
     */
    public static double mean(long... arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("arr cannot be empty.");
        }
        double total = 0.0;
        for (long x : arr) {
            total += (double)x;
        }
        return total / (double)arr.length;
    }

    /**
     * Calculates the mean of the given array of ints.
     * @param arr
     * @return mean
     */
    public static double mean(float... arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("arr cannot be empty.");
        }
        double total = 0.0;
        for (float x : arr) {
            total += (double)x;
        }
        return total / (double)arr.length;
    }

    /**
     * Calculates the mean of the given array of ints.
     * @param arr
     * @return mean
     */
    public static double mean(double... arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("arr cannot be empty.");
        }
        double total = 0.0;
        for (double x : arr) {
            total += (double)x;
        }
        return total / (double)arr.length;
    }

    /**
     * Calculates the variance of the given List of integers.
     * @param list
     * @return variance
     */
    public static <N extends Number> double variance(Collection<N> list) {
        double mean = mean(list);
        double sumOfSquareDiffs = 0L;
        for (Number x : list) {
            double diff = x.doubleValue() - mean;
            sumOfSquareDiffs += (diff * diff);
        }
        return sumOfSquareDiffs / (double) list.size();
    }

    /**
     * Calculates the variance of the given numbers.
     * @param arr
     * @return variance
     */
    public static double variance(int[] arr) {
        double mean = mean(arr);
        double sumOfSquareDiffs = 0L;
        for (int x : arr) {
            double diff = (double)x - mean;
            sumOfSquareDiffs += (diff * diff);
        }
        return sumOfSquareDiffs / (double) arr.length;
    }

    /**
     * Calculates the variance of the given numbers.
     * @param arr
     * @return variance
     */
    public static double variance(float[] arr) {
        double mean = mean(arr);
        double sumOfSquareDiffs = 0L;
        for (float x : arr) {
            double diff = (double)x - mean;
            sumOfSquareDiffs += (diff * diff);
        }
        return sumOfSquareDiffs / (double) arr.length;
    }

    /**
     * Calculates the variance of the given numbers.
     * @param arr
     * @return variance
     */
    public static double variance(long[] arr) {
        double mean = mean(arr);
        double sumOfSquareDiffs = 0L;
        for (long x : arr) {
            double diff = (double)x - mean;
            sumOfSquareDiffs += (diff * diff);
        }
        return sumOfSquareDiffs / (double) arr.length;
    }

    /**
     * Calculates the variance of the given numbers.
     * @param arr
     * @return variance
     */
    public static double variance(double[] arr) {
        double mean = mean(arr);
        double sumOfSquareDiffs = 0L;
        for (double x : arr) {
            double diff = (double)x - mean;
            sumOfSquareDiffs += (diff * diff);
        }
        return sumOfSquareDiffs / (double) arr.length;
    }

    /**
     * Calculates the standard deviation of the given List of integers.
     * @param list
     * @return standard deviation
     */
    public static <N extends Number> double stddev(Collection<N> list) {
        return java.lang.Math.sqrt(variance(list));
    }

    /**
     * Calculates the standard deviation of the given numbers.
     * @param arr
     * @return standard deviation
     */
    public static double stddev(int[] arr) {
        return java.lang.Math.sqrt(variance(arr));
    }

    /**
     * Calculates the standard deviation of the given numbers.
     * @param arr
     * @return standard deviation
     */
    public static double stddev(float[] arr) {
        return java.lang.Math.sqrt(variance(arr));
    }

    /**
     * Calculates the standard deviation of the given numbers.
     * @param arr
     * @return standard deviation
     */
    public static double stddev(long[] arr) {
        return java.lang.Math.sqrt(variance(arr));
    }

    /**
     * Calculates the standard deviation of the given numbers.
     * @param arr
     * @return standard deviation
     */
    public static double stddev(double[] arr) {
        return java.lang.Math.sqrt(variance(arr));
    }
}
