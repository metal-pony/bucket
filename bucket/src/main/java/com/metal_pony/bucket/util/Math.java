package com.metal_pony.bucket.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Assorted math functions.
 */
public class Math {

    private static final String MISSING_ARRAY_ERR = "Missing array";
    private static final String EMPTY_ARRAY_ERR = "Array empty";
    private static void validateArrayNotNullOrEmpty(int[] arr) {
        if (arr == null) throw new NullPointerException(MISSING_ARRAY_ERR);
        if (arr.length == 0) throw new IllegalArgumentException(EMPTY_ARRAY_ERR);
    }
    private static void validateArrayNotNullOrEmpty(long[] arr) {
        if (arr == null) throw new NullPointerException(MISSING_ARRAY_ERR);
        if (arr.length == 0) throw new IllegalArgumentException(EMPTY_ARRAY_ERR);
    }
    private static void validateArrayNotNullOrEmpty(float[] arr) {
        if (arr == null) throw new NullPointerException(MISSING_ARRAY_ERR);
        if (arr.length == 0) throw new IllegalArgumentException(EMPTY_ARRAY_ERR);
    }
    private static void validateArrayNotNullOrEmpty(double[] arr) {
        if (arr == null) throw new NullPointerException(MISSING_ARRAY_ERR);
        if (arr.length == 0) throw new IllegalArgumentException(EMPTY_ARRAY_ERR);
    }

    /**
     * Finds the maximum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static int max(int... n) {
        validateArrayNotNullOrEmpty(n);
        int max = n[0];
        for (int m : n) if (m > max) max = m;
        return max;
    }

    /**
     * Finds the maximum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static long max(long... n) {
        validateArrayNotNullOrEmpty(n);
        long max = n[0];
        for (long m : n) if (m > max) max = m;
        return max;
    }

    /**
     * Finds the maximum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static float max(float... n) {
        validateArrayNotNullOrEmpty(n);
        float max = n[0];
        for (float m : n) if (m > max) max = m;
        return max;
    }

    /**
     * Finds the maximum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static double max(double... n) {
        validateArrayNotNullOrEmpty(n);
        double max = n[0];
        for (double m : n) if (m > max) max = m;
        return max;
    }

    /**
     * Finds the minimum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static int min(int ... n) {
        validateArrayNotNullOrEmpty(n);
        int min = n[0];
        for (int m : n) if (m < min) min = m;
        return min;
    }

    /**
     * Finds the minimum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static long min(long ... n) {
        validateArrayNotNullOrEmpty(n);
        long min = n[0];
        for (long m : n) if (m < min) min = m;
        return min;
    }

    /**
     * Finds the minimum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static float min(float ... n) {
        validateArrayNotNullOrEmpty(n);
        float min = n[0];
        for (float m : n) if (m < min) min = m;
        return min;
    }

    /**
     * Finds the minimum value among given numbers.
     * @param n - list of one or more numbers
     * @return the maximum value
     */
    public static double min(double ... n) {
        validateArrayNotNullOrEmpty(n);
        double min = n[0];
        for (double m : n) if (m < min) min = m;
        return min;
    }

    /**
     * Calculates the distance between two two-dimensional points, given as
     * A (ax, ay), and B (bx, by).
     * @param ax
     * @param ay
     * @param bx
     * @param by
     * @return
     */
    public static double dist(double ax, double ay, double bx, double by) {
        return java.lang.Math.sqrt((ax-bx)*(ax-bx) + (ay-by)*(ay-by));
    }

    /**
     * Calculates the greatest common divisor between a and b, i.e. the largest
     * integer that divides both a and b evenly.
     * @param a
     * @param b
     * @return
     */
    public static int gcd(int a, int b) {
        return 0;
    }

    /**
     * Calculates whether the given number is prime.
     * @param n
     * @return True if the number is prime; otherwise false.
     */
    public static boolean isPrime(long n) {
        return (n >= 2L && getLowestPrimeFactor(n) == n);
    }

    /**
     * Calculates all prime factors of the given number.
     * @param n
     * @return A List containing prime factors of <code>n</code>.
     */
    public static List<Long> getPrimeFactors(long n) {
        List<Long> factors = new ArrayList<>();

        long lowestFactor;
        while ((lowestFactor = getLowestPrimeFactor(n)) > 1L) {
            factors.add(lowestFactor);
            n /= lowestFactor;
        }

        return factors;
    }

    /**
     * Attempts to get the lowest prime factor of <code>n</code>.
     * Note: if <code>abs(n) == 0 or 1</code>, then 0 or 1 will be returned.
     * 0 and 1 are not prime numbers, so in this case, there are no prime factors of n.
     * @param n
     * @return the lowest prime factor of <code>n</code>.
     */
    public static long getLowestPrimeFactor(long n) {
        if (n < 0L) {
            n = -n;
        }

        // Can return 0, 1, 2, & 3 early
        if (n < 4L) {
            return n;
        }

        if (n % 2L == 0) {
            return 2L;
        }

        long sqrtCeil = (long)(java.lang.Math.ceil(java.lang.Math.sqrt(n))) + 1L;
        for (long x = 3L; x < sqrtCeil; x+=2L) {
            if (n % x == 0) {
                return x;
            }
        }

        return n;
    }

    /**
     * Calculates the shortest pair of rectangular, integer dimensions for the given area.
     * i.e., the shortest-perimeter problem.
     * @param area
     * @return Integer factors of the given area, a and b, such that a*b=area,
     * and a+b < all other area factor combinations.
     */
    public static int[] shortestDimensionsRect(int area) {
        if (area <= 0) {
            throw new IllegalArgumentException("Area must be positive");
        }

        int a = 1;
        int b = area;
        int nextFactor = a;

        while (a < b) {
            while (area % ++nextFactor != 0 && nextFactor <= b);
            if (nextFactor <= b) {
                a = nextFactor;
                b = area / a;
            }
        }

        return new int[]{ b, a };
    }
}
