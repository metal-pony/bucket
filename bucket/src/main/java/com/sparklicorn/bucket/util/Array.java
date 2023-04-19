package com.sparklicorn.bucket.util;

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
}
