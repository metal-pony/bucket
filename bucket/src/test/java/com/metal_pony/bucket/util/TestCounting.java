package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.Gson;
import com.metal_pony.bucket.util.Counting;

public class TestCounting {

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
            assertEquals(expected, Counting.factorial(n).intValue());
        }
    }

    @Nested
    @DisplayName("combination")
    class Combination {
        @Test
        @DisplayName("nChooseK throws exception on negative input")
        void nChooseK_negativeInputs() {
            assertThrows(IllegalArgumentException.class, () -> Counting.nChooseK(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> Counting.nChooseK(-10, 0));
            assertThrows(IllegalArgumentException.class, () -> Counting.nChooseK(0, -1));
            assertThrows(IllegalArgumentException.class, () -> Counting.nChooseK(0, -10));
            assertThrows(IllegalArgumentException.class, () -> Counting.nChooseK(-10, -10));
        }

        @Test
        @DisplayName("nChooseK returns 0 whenever k is 0")
        void nChooseK_whenKIsZero_returnsOne() {
            for (int n = 0; n < 100; n++) {
                assertEquals(BigInteger.ONE, Counting.nChooseK(n, 0));
            }
        }

        @ParameterizedTest(name = "n = {0}")
        @CsvFileSource(resources = "/NChooseK.csv", numLinesToSkip = 1, delimiter = '|', maxCharsPerColumn = 1<<24)
        void nChooseK(int n, String nChooseKArr) {
            Gson gson = new Gson();
            String[] expectedStrs = gson.fromJson(nChooseKArr, String[].class);

            for (int k = 0; k < expectedStrs.length; k++) {
                assertEquals(new BigInteger(expectedStrs[k]), Counting.nChooseK(n, k));
            }
        }

        @Test
        void combo_whenRIsNegative_throwsException() {
            assertThrows(IllegalArgumentException.class,
                () -> Counting.combo(0, 0, BigInteger.valueOf(-1)));
            assertThrows(IllegalArgumentException.class,
                () -> Counting.combo(0, 0, BigInteger.valueOf(-10)));
            assertThrows(IllegalArgumentException.class,
                () -> Counting.combo(0, 0, BigInteger.valueOf(-100)));
        }

        @ParameterizedTest
        @CsvSource(textBlock = """
            1,1,2
            2,1,3
            3,2,19
            7,5,22
            10,5,253

        """)
        void combo_whenRIsTooLarge_throwsException(int n, int k, String r) {
            assertThrows(IllegalArgumentException.class, () -> Counting.combo(n, k, new BigInteger(r)));
        }

        @ParameterizedTest(name = "({0} choose {1}) = {2}")
        @CsvSource(textBlock = """
            -10,0
            -1,0
            0,-1
            0,-10
            -1,-2
            -87456,-2384
        """)
        void combo_negativeInputs(int n, int k) {
            assertThrows(
                IllegalArgumentException.class,
                () -> Counting.combo(n, k, BigInteger.ZERO)
            );
        }
    }

    @Nested
    @DisplayName("permutation")
    class Permutation {
        @ParameterizedTest(name = "n = {0}, throws exception")
        @ValueSource(ints = { -1, -2, -3, -10, -100 })
        void testNegativeN(int n) {
            assertThrows(IllegalArgumentException.class, () -> Counting.randomPermutation(n));
        }

        @Test
        @DisplayName("n = 0")
        void testInputZero() {
            assertArrayEquals(
                new int[]{},
                Counting.randomPermutation(0)
            );
        }

        @ParameterizedTest(name = "n = {0}")
        @CsvFileSource(resources = "/Perms.csv", numLinesToSkip = 1, delimiter = ';', maxCharsPerColumn = 1<<24)
        void permutationsFromFile(int n, String permsArrStr) {
            Gson gson = new Gson();
            int[][] expectedPerms = gson.fromJson(permsArrStr, int[][].class);

            for (int r = 0; r < expectedPerms.length; r++) {
                int[] expected = expectedPerms[r];
                int[] actual = Counting.permutation(n, BigInteger.valueOf(r));
                assertArrayEquals(expected, actual);
            }
        }

        @RepeatedTest(value = 8, name = "total perms when n = {currentRepetition}")
        void testTotalPerms(RepetitionInfo repetitionInfo) {
            int n = repetitionInfo.getCurrentRepetition();
            long expectedPerms = factorial(n);

            Set<List<Integer>> perms = new HashSet<>();

            for (long r = 0L; r < expectedPerms; r++) {
                perms.add(
                    Arrays.stream(Counting.permutation(n, BigInteger.valueOf(r)))
                        .boxed()
                        .toList()
                );
            }

            assertEquals(expectedPerms, perms.size());
        }

        @Test
        public void testPermsList_whenListIsNull_throwsException() {
            assertThrows(NullPointerException.class, () -> {
                Counting.allPermutations(null);
            });
        }

        @Test
        public void testPermsList() {
            List<String> list = new ArrayList<>();
            List<String> itemsToAdd = Arrays.asList(
                // Don't add too many items or factorial(n) will break below.
                "meow", "pow", "how", "now", "brown", "cow"
            );
            List<String> expectedItems = new ArrayList<>();

            for (int i = 0; i < itemsToAdd.size(); i++) {
                String item = itemsToAdd.get(i);
                list.add(item);
                expectedItems.add(item);

                Set<List<String>> perms = Counting.allPermutations(list);
                assertEquals(factorial(i + 1), perms.size());
                for (List<String> perm : perms) {
                    assertEquals(expectedItems.size(), perm.size());
                    assertTrue(perm.containsAll(expectedItems));
                }
            }
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
