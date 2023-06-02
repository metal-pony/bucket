package com.sparklicorn.bucket.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Shuffler")
public class TestShuffler {
    @Nested
    @DisplayName("factorial")
    class Factorial {
        @ParameterizedTest(name = "factorial({0}) = {1}")
        @CsvSource(textBlock = """
            0,1
            1,1
            2,2
            3,6
            4,24
            5,120
            6,720
            7,5040
        """)
        void testFactorial(int n, int expected) {
            assertEquals(expected, Shuffler.factorial(n).intValue());
        }
    }

    @Nested
    @DisplayName("permutation")
    class Permutation {
        @ParameterizedTest(name = "n = {0}, throws exception")
        @ValueSource(ints = { -1, -2, -3, -10, -100 })
        void testNegativeN(int n) {
            assertThrows(IllegalArgumentException.class, () -> Shuffler.randomPermutation(n));
        }

        @Test
        @DisplayName("n = 0")
        void testInputZero() {
            assertArrayEquals(
                new int[]{},
                Shuffler.randomPermutation(0)
            );
        }

        @Test
        @DisplayName("n = 1")
        void testInput1() {
            assertArrayEquals(
                new int[]{ 0 },
                Shuffler.randomPermutation(1)
            );
        }

        @ParameterizedTest(name = "n = 2")
        @CsvSource(textBlock = """
            0,  0,  1
            1,  1,  0
        """)
        void testN2(int r, int i0, int i1) {
            int[] result = Shuffler.permutation(2, BigInteger.valueOf(r));
            assertArrayEquals(
                new int[]{ i0, i1 },
                result
            );
        }

        @ParameterizedTest(name = "n = 3")
        @CsvSource(textBlock = """
            0, 0, 1, 2
            1, 0, 2, 1
            2, 1, 0, 2
            3, 1, 2, 0
            4, 2, 0, 1
            5, 2, 1, 0
        """)
        void testN3(int r, int i0, int i1, int i2) {
            int[] result = Shuffler.permutation(3, BigInteger.valueOf(r));
            assertArrayEquals(
                new int[] { i0, i1, i2 },
                result
            );
        }

        @ParameterizedTest(name = "n = 4; r = {0}")
        @CsvFileSource(resources = "/RandomPermsN4.csv", numLinesToSkip = 1)
        void testN4(int r, int i0, int i1, int i2, int i3) {
            int[] result = Shuffler.permutation(4, BigInteger.valueOf(r));
            assertArrayEquals(
                new int[] { i0, i1, i2, i3 },
                result
            );
        }

        @RepeatedTest(value = 8, name = "total perms when n = {currentRepetition}")
        void testTotalPerms(RepetitionInfo repetitionInfo) {
            int n = repetitionInfo.getCurrentRepetition();
            long expectedPerms = factorial(n);

            Set<List<Integer>> perms = new HashSet<>();

            for (long r = 0L; r < expectedPerms; r++) {
                perms.add(
                    Arrays.stream(Shuffler.permutation(n, BigInteger.valueOf(r)))
                        .boxed()
                        .toList()
                );
            }

            assertEquals(expectedPerms, perms.size());
        }

        private static long factorial(int n) {
            long result = (n < 1) ? 1L : (long) n;

            for (long r = n - 1; r > 1; r--) {
                result *= r;
            }

            return result;
        }
    }
}
