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

import com.google.gson.Gson;

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
    @DisplayName("combination")
    class Combination {
        @Test
        @DisplayName("nChooseK throws exception on negative input")
        void nChooseK_negativeInputs() {
            assertThrows(ArithmeticException.class, () -> Shuffler.nChooseK(-1, 0));
            assertThrows(ArithmeticException.class, () -> Shuffler.nChooseK(-10, 0));
            assertThrows(ArithmeticException.class, () -> Shuffler.nChooseK(0, -1));
            assertThrows(ArithmeticException.class, () -> Shuffler.nChooseK(0, -10));
            assertThrows(ArithmeticException.class, () -> Shuffler.nChooseK(-10, -10));
        }

        @Test
        @DisplayName("nChooseK returns 0 whenever k is 0")
        void nChooseK_whenKIsZero_returnsOne() {
            for (long n = 0L; n < 100L; n++) {
                assertEquals(BigInteger.ONE, Shuffler.nChooseK(n, 0L));
            }
        }

        @ParameterizedTest(name = "n = {0}")
        @CsvFileSource(resources = "/NChooseK.csv", numLinesToSkip = 1, delimiter = '|', maxCharsPerColumn = 1<<24)
        void nChooseK(long n, String nChooseKArr) {
            Gson gson = new Gson();
            String[] expectedStrs = gson.fromJson(nChooseKArr, String[].class);

            for (int k = 0; k < expectedStrs.length; k++) {
                assertEquals(new BigInteger(expectedStrs[k]), Shuffler.nChooseK(n, k));
            }
        }

        @Test
        void combo_whenRIsNegative_throwsException() {
            assertThrows(IllegalArgumentException.class,
                () -> Shuffler.combo(0, 0, BigInteger.valueOf(-1)));
            assertThrows(IllegalArgumentException.class,
                () -> Shuffler.combo(0, 0, BigInteger.valueOf(-10)));
            assertThrows(IllegalArgumentException.class,
                () -> Shuffler.combo(0, 0, BigInteger.valueOf(-100)));
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
            assertThrows(IllegalArgumentException.class, () -> Shuffler.combo(n, k, new BigInteger(r)));
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
                ArithmeticException.class,
                () -> Shuffler.combo(n, k, BigInteger.ZERO)
            );
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

        @ParameterizedTest(name = "n = {0}")
        @CsvFileSource(resources = "/Perms.csv", numLinesToSkip = 1, delimiter = ';', maxCharsPerColumn = 1<<24)
        void permutationsFromFile(int n, String permsArrStr) {
            Gson gson = new Gson();
            int[][] expectedPerms = gson.fromJson(permsArrStr, int[][].class);

            for (int r = 0; r < expectedPerms.length; r++) {
                int[] expected = expectedPerms[r];
                int[] actual = Shuffler.permutation(n, BigInteger.valueOf(r));
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
