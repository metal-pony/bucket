package com.sparklicorn.bucket.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains utility functions pertaining to counting, including
 * functions for calculating permutation and combination sets from
 * given collections of objects.
 */
public class Counting {
    // factorial(FACTORIAL_MAX) produces the largest value that will fit in 64-bits (signed).
    public static final long FACTORIAL_MAX_LONG;
    public static final int FACTORIAL_MAX;
    static {
        long n = Long.MAX_VALUE;
        long max = 1;
        while (n / max > 1) {
            n /= max++;
        }
        FACTORIAL_MAX_LONG = max - 1;

        int n2 = Integer.MAX_VALUE;
        int max2 = 1;
        while (n2 / max2 > 1) {
            n2 /= max2++;
        }
        FACTORIAL_MAX = max2 - 1;
    }

    /**
     * Computes the factorial of the given number.
     * Must be in the interval <code>[1, FACTORIAL_MAX_LONG]</code>.
     * @param n - Number to compute the factorial of.
     * @return <code>n!</code>
     */
    public static long factorial(long n) {
        if (n == 0L) {
            return 1L;
        }

        if (n < 0L || n > FACTORIAL_MAX_LONG) {
            throw new IllegalArgumentException(
                String.format("n must be in interval [1, %d]", FACTORIAL_MAX_LONG)
            );
        }

        long result = n;
        for (long i = n - 1L; i > 0L; i--) {
            result *= i;
        }
        return result;
    }

    /**
     * Computes the factorial of the given number.
     * Must be in the interval <code>[1, FACTORIAL_MAX]</code>.
     * @param n - Number to compute the factorial of.
     * @return <code>n!</code>
     */
    public static int factorial(int n) {
        if (n == 0) {
            return 1;
        }

        if (n < 1 || n > FACTORIAL_MAX) {
            throw new IllegalArgumentException(
                String.format("n must be in interval [1, %d]", FACTORIAL_MAX)
            );
        }

        int result = n;
        for (int i = n - 1; i > 0; i--) {
            result *= i;
        }
        return result;
    }

    /**
     * Computes all permutations of the given list.
     * @param <T> Type of object that the list contains.
     * @param list - Contains items to compute permutations of.
     * @return Set containing all permutations of the given list of items.
     */
    public static <T> Set<List<T>> perms(List<T> list) {
        HashSet<List<T>> resultSet = new HashSet<>();
        if (list.size() == 1) {
            resultSet.add(list);
        } else if (list.size() > 1) {
            for (T e : list) {
                List<T> sublist = new ArrayList<>(list);
                sublist.remove(e);
                Set<List<T>> r = perms(sublist);
                for (List<T> x : r) {
                    x.add(e);
                    resultSet.add(x);
                }
            }
        }
        return resultSet;
    }

    // Inserts <code>n</code> at <code>index</code> in the given array.
    // Shifts all elements at <code>index</code> and greater to the right.
    private static int[] insert(int[] arr, int index, int n) {
        int[] result = new int[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, index);
        result[index] = n;
        System.arraycopy(arr, index, result, index + 1, arr.length - index);
        return result;
    }

    /**
     * Computes all permutation of the numbers from 1 to n (inclusive).
     * @param n - Amount of numbers to compute permutations of.
     * @return Array containing all permutations of the numbers 1 through n.
     */
    public static int[][] perms(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater than 1");
        }

        int[][] perms = new int[][]{ new int[]{1} };

        for (int i = 2; i <= n; i++) {
            int[][] newPerms = new int[perms.length * i][];
            for (int j = 0; j < perms.length; j++) {
                for (int k = 0; k < i; k++) {
                    newPerms[(i*j) + k] = insert(perms[j], k, i);
                }
            }
            perms = newPerms;
        }

        return perms;
    }
}
