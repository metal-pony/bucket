package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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

public class TestCounting {
    @Nested
    @DisplayName("factorial")
    class Factorial {
        @ParameterizedTest(name = "n = {0}")
        @CsvFileSource(resources = "/Factorials.csv", numLinesToSkip = 1, delimiter = ';', maxCharsPerColumn = 1<<24)
        void testFact(int n, String nFact) {
            assertEquals(new BigInteger(nFact), Counting.factorial(n));
        }

        // Arbitrary visual tests
        // @Test
        void adhoc() {
            System.out.println("Random big int, bit length:");
            for (int i = 0; i < 32; i++) {
                BigInteger r = Counting.random(
                    BigInteger.ONE.shiftLeft(100),
                    ThreadLocalRandom.current()
                );
                int bitLength = r.bitLength();
                System.out.printf(
                    "%s, %d\n",
                    r.toString(),
                    bitLength
                );
            }

            System.out.println("\n(N choose K):");
            int n = 9;
            for (int k = 0; k < n; k++) {
                System.out.printf("(%2d c %2d) = %s\n", n, k, Counting.nChooseK(n, k));
            }

            int k = 5;
            BigInteger nck = Counting.nChooseK(n, k);
            System.out.printf("\n(%d choose %d) {%s} combos:\n", n, k, nck.toString());
            for (BigInteger r = BigInteger.ZERO; r.compareTo(nck) < 0; r = r.add(BigInteger.ONE)) {
                System.out.printf("{%s} %s\n", r.toString(), Arrays.toString(Counting.combo(n, k, r)));
            }

            n = 22;
            k = 9;
            BigInteger r = new BigInteger("273864");
            int[] combo = Counting.combo(n, k, r);
            System.out.printf("\nChecking arbitrary combo(%d, %d, %s): %s\n", n, k, r.toString(), Arrays.toString(combo));
        }
    }

    @Nested
    @DisplayName("combination")
    class Combination {
        @Test
        void bitCombo() {
            // Test expected bitcombo results:  expectedCombos[n][k][r] = expected int
            int[][][] expectedCombos = new int[][][] {
                new int[0][0],
                new int[][] {
                    new int[] { 0 },
                    new int[] { 1 },
                },
                new int[][] {
                    new int[] { 0 },
                    new int[] { 0b10, 0b01 },
                    new int[] { 0b11 },
                },
                new int[][] {
                    new int[] { 0 },
                    new int[] { 0b100, 0b010, 0b001 },
                    new int[] { 0b110, 0b101, 0b011 },
                    new int[] { 0b111 },
                },
                new int[][] {
                    new int[] { 0 },
                    new int[] { 0b1000, 0b0100, 0b0010, 0b0001 },
                    new int[] { 0b1100, 0b1010, 0b1001, 0b0110, 0b0101, 0b0011 },
                    new int[] { 0b1110, 0b1101, 0b1011, 0b0111 },
                    new int[] { 0b1111 },
                },
                new int[][] {
                    new int[] { 0 },
                    new int[] { 0b10000, 0b01000, 0b00100, 0b00010, 0b00001 },
                    new int[] { 0b11000, 0b10100, 0b10010, 0b10001, 0b01100, 0b01010, 0b01001, 0b00110, 0b00101, 0b00011 },
                    new int[] { 0b11100, 0b11010, 0b11001, 0b10110, 0b10101, 0b10011, 0b01110, 0b01101, 0b01011, 0b00111 },
                    new int[] { 0b11110, 0b11101, 0b11011, 0b10111, 0b01111 },
                    new int[] { 0b11111 },
                },
            };

            for (int n = 0; n < expectedCombos.length; n++) {
                for (int k = 0; k < expectedCombos[n].length; k++) {
                    for (int r = 0; r < expectedCombos[n][k].length; r++) {
                        int expected = expectedCombos[n][k][r];
                        byte[] expectedArr = intToByteArray(expected);
                        byte[] bc = Counting.bitCombo(n, k, new BigInteger(Integer.toString(r)));
                        assertArrayEquals(expectedArr, bc);
                    }
                }
            }
        }

        @Test
        @DisplayName("bitCombo throws with bad (negative) input")
        void bitCombo_withBadInput_throws() {
            BigInteger NEG_ONE = BigInteger.ZERO.subtract(BigInteger.ONE);
            assertThrows(IllegalArgumentException.class, () -> Counting.bitCombo(-1, -1, BigInteger.ZERO));
            assertThrows(IllegalArgumentException.class, () -> Counting.bitCombo(-1, 0, BigInteger.ZERO));
            assertThrows(IllegalArgumentException.class, () -> Counting.bitCombo(1, -1, NEG_ONE));
            assertThrows(IllegalArgumentException.class, () -> Counting.bitCombo(1, 0, NEG_ONE));
            assertThrows(IllegalArgumentException.class, () -> Counting.bitCombo(1, 2, BigInteger.ZERO));
        }

        @Test
        @DisplayName("nChooseK throws with bad (negative) input")
        void nChooseK_withBadInput_throws() {
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
        @CsvFileSource(resources = "/NChooseK.csv", delimiter = '|', maxCharsPerColumn = 1<<24)
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
    }

    /** Converts an integer to a byte array.*/
    private static byte[] intToByteArray(int n) {
        if (n < 0) {
            n = -n;
        }

        int numBytes = 0;
        int _n = n;
        do {
            numBytes++;
            _n >>= Byte.SIZE;
        } while (_n > 0);

        byte[] result = new byte[numBytes];
        _n = n;
        int i = 0;
        while (_n > 0) {
            result[i++] = (byte)(_n & 0xff);
            _n >>= Byte.SIZE;
        }

        return result;
    }

    /** Calculates n! as a long with no overflow protection, so use with caution.*/
    private static long factorial(int n) {
        long result = (n < 1) ? 1L : (long) n;

        for (long r = n - 1; r > 1; r--) {
            result *= r;
        }

        return result;
    }
}
