package com.metal_pony.bucket.sudoku;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TestSudokuUtility {
    @Test
    void isSquare() {
        Set<Integer> squares100 = new HashSet<>();
        for (int n = 0; n < 100; n++) {
            squares100.add(n*n);
        }

        Random rand = new Random();
        for (int t = 0; t < 1000; t++) {
            // Negatives always return false
            assertFalse(SudokuUtility.isSquare(0 - rand.nextInt(1<<20)));
            // t^2 returns true
            assertTrue(SudokuUtility.isSquare(t*t));
            // Check if t is in precalculated squares set
            assertEquals(squares100.contains(t), SudokuUtility.isSquare(t));
        }
    }

    @Test
    void swap() {
        // When array is null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            SudokuUtility.swap(null, 0, 0);
        });

        int[] arr = new int[] {1, 2, 3};

        // When i or j is out of bounds, throws ArrayIndexOutOfBoundsException
        int[][] iAndJs = new int[][]{
            new int[]{-(arr.length+1), 0},
            new int[]{0, -(arr.length+1)},
            new int[]{-arr.length, 0},
            new int[]{0, -arr.length},
            new int[]{arr.length, 0},
            new int[]{0, arr.length},
            new int[]{arr.length+1, 0},
            new int[]{0, arr.length+1}
        };
        for (int[] iAndJ : iAndJs) {
            final int[] _arr = arr;
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                SudokuUtility.swap(_arr, iAndJ[0], iAndJ[1]);
            });
        }

        // When i and j are equal, does not modify the array
        for (int i = 0; i < 3; i++) {
            arr = new int[] {1, 2, 3};
            int[] actual = SudokuUtility.swap(arr, i, i);
            assertTrue(arr == actual);
            assertTrue(Arrays.equals(actual, new int[] {1, 2, 3}));
        }

        // Normal case works as expected
        arr = new int[] {1, 2, 3};
        int[] actual = SudokuUtility.swap(arr, 0, 2);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(actual, new int[] {3, 2, 1}));
        actual = SudokuUtility.swap(arr, 2, 1);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(actual, new int[] {3, 1, 2}));
        actual = SudokuUtility.swap(arr, 2, 0);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(actual, new int[] {2, 1, 3}));
        actual = SudokuUtility.swap(arr, 1, 2);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(actual, new int[] {2, 3, 1}));
    }

    @Test
    void rotate90() {
        // When arr is null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            SudokuUtility.rotate90(null);
        });

        // When arr is empty, does nothing
        int[] arr = new int[0];
        int[] actual = SudokuUtility.rotate90(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, actual));

        // When arr is not square, throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.rotate90(new int[2]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.rotate90(new int[3]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.rotate90(new int[5]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.rotate90(new int[99]);
        });

        // Otherwise, rotates array as expected
        arr = new int[]{1, 2, 3, 4};
        actual = SudokuUtility.rotate90(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{3, 1, 4, 2}));

        arr = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        actual = SudokuUtility.rotate90(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{7, 4, 1, 8, 5, 2, 9, 6, 3}));
    }

    @Test
    void reflectOverHorizontal() {
        // When arr is null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            SudokuUtility.reflectOverHorizontal(null, 0);
        });

        // When rows is 0 or negative, throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverHorizontal(new int[1], 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverHorizontal(new int[1], -1);
        });

        // When arr is not divisible by rows, throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverHorizontal(new int[3], 2);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverHorizontal(new int[4], 3);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverHorizontal(new int[9], 4);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverHorizontal(new int[99], 10);
        });


        // When rows < 2, does nothing
        int[] arr = new int[]{1,2,3,4,5};
        int[] actual = SudokuUtility.reflectOverHorizontal(arr, 1);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(new int[]{1,2,3,4,5}, actual));

        // Otherwise, reflects array as expected
        arr = new int[]{
            1, 2,
            3, 4
        };
        actual = SudokuUtility.reflectOverHorizontal(arr, 2);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            3, 4,
            1, 2
        }));

        arr = new int[]{
            1, 2,
            3, 4,
            5, 6,
            7, 8,
            9, 10
        };
        actual = SudokuUtility.reflectOverHorizontal(arr, 5);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            9, 10,
            7, 8,
            5, 6,
            3, 4,
            1, 2
        }));
    }

    @Test
    void reflectOverVertical() {
        // When arr is null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            SudokuUtility.reflectOverVertical(null, 0);
        });

        // When rows is 0, throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverVertical(new int[1], 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverVertical(new int[1], -1);
        });

        // When arr is not divisible by rows, throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverVertical(new int[3], 2);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverVertical(new int[4], 3);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverVertical(new int[9], 4);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverVertical(new int[99], 10);
        });

        // When cols (arr.length / rows) < 2, does nothing
        int[] arr = new int[]{1,2,3,4,5};
        int[] actual = SudokuUtility.reflectOverVertical(arr, 5);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(new int[]{1,2,3,4,5}, actual));

        // Otherwise, reflects array as expected
        arr = new int[]{
            1, 2,
            3, 4
        };
        actual = SudokuUtility.reflectOverVertical(arr, 2);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            2, 1,
            4, 3
        }));

        arr = new int[]{
            1, 2,
            3, 4,
            5, 6,
            7, 8,
            9, 10
        };
        actual = SudokuUtility.reflectOverVertical(arr, 5);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            2, 1,
            4, 3,
            6, 5,
            8, 7,
            10, 9
        }));
    }

    @Test
    void reflectOverDiagonal() {
        // When arr is null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            SudokuUtility.reflectOverDiagonal(null);
        });

        // When arr is empty, does nothing
        int[] arr = new int[0];
        int[] actual = SudokuUtility.reflectOverDiagonal(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, actual));

        // When arr is not square, throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverDiagonal(new int[2]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverDiagonal(new int[3]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverDiagonal(new int[5]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverDiagonal(new int[99]);
        });

        // Otherwise, rotates array as expected
        arr = new int[]{
            1, 2,
            3, 4
        };
        actual = SudokuUtility.reflectOverDiagonal(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            4, 2,
            3, 1
        }));

        arr = new int[]{
            1,  2,  3,  4,
            5,  6,  7,  8,
            9, 10, 11, 12,
            13, 14, 15, 16
        };
        actual = SudokuUtility.reflectOverDiagonal(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            16, 12,  8,  4,
            15, 11,  7,  3,
            14, 10,  6,  2,
            13,  9,  5,  1
        }));
    }

    @Test
    void reflectOverAntiDiagonal() {
        // When arr is null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            SudokuUtility.reflectOverAntiDiagonal(null);
        });

        // When arr is empty, does nothing
        int[] arr = new int[0];
        int[] actual = SudokuUtility.reflectOverAntiDiagonal(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, actual));

        // When arr is not square, throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverAntiDiagonal(new int[2]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverAntiDiagonal(new int[3]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverAntiDiagonal(new int[5]);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SudokuUtility.reflectOverAntiDiagonal(new int[99]);
        });

        // Otherwise, rotates array as expected
        arr = new int[]{
            1, 2,
            3, 4
        };
        actual = SudokuUtility.reflectOverAntiDiagonal(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            1, 3,
            2, 4
        }));

        arr = new int[]{
            1,  2,  3,  4,
            5,  6,  7,  8,
            9, 10, 11, 12,
            13, 14, 15, 16
        };
        actual = SudokuUtility.reflectOverAntiDiagonal(arr);
        assertTrue(arr == actual);
        assertTrue(Arrays.equals(arr, new int[]{
            1,  5,  9, 13,
            2,  6, 10, 14,
            3,  7, 11, 15,
            4,  8, 12, 16
        }));
    }
}
