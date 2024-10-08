package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.junit.jupiter.api.*;

public class TestMath {

    private static final double EPSILON = 1e-10;

    @Test
    public void testMax_whenNoValuesAreProvided_throwsNullPointerException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> { Math.max(); },
            "Array missing elements."
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> { Math.max(new int[]{}); },
            "Array missing elements."
        );
    }

    @Test
    public void testMax_whenSingleValueProvided_returnsSingleValue() {
        assertEquals(0, Math.max(0));
        assertEquals(-88, Math.max(-88));
        assertEquals(Integer.MAX_VALUE, Math.max(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Math.max(Integer.MIN_VALUE));
    }

    @Test
    public void testDist() {
        assertEquals(5.0, Math.dist(0, 0, 3, 4), EPSILON);
        assertEquals(13.0, Math.dist(0, 0, 5, 12), EPSILON);
    }

    @Test
    public void testShortestDimensionsRect_whenAreaIsNotPositive_throwException() {
        int[] badAreas = new int[]{ -100, -3, -2, -1, 0 };
        for (int area : badAreas) {
            assertThrows(IllegalArgumentException.class, () -> Math.shortestDimensionsRect(area));
        }
    }

    @Test
    public void testShortestDimensionsRect() {
        HashMap<Integer, int[]> testData = new HashMap<>();

        testData.put(1, new int[]{ 1, 1 });
        testData.put(2, new int[]{ 1, 2 });
        testData.put(3, new int[]{ 1, 3 });
        testData.put(4, new int[]{ 2, 2 });
        testData.put(5, new int[]{ 1, 5 });
        testData.put(6, new int[]{ 2, 3 });
        testData.put(24, new int[]{ 4, 6 });
        testData.put(120, new int[]{ 10, 12 });
        testData.put(720, new int[]{ 24, 30 });
        testData.put(29*67, new int[]{ 29, 67 });

        for (int area : testData.keySet()) {
            int[] expected = testData.get(area);
            assertArrayEquals(expected, Math.shortestDimensionsRect(area));
        }
    }
}
