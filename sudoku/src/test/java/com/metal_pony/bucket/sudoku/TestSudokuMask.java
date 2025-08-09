package com.metal_pony.bucket.sudoku;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.metal_pony.bucket.sudoku.util.SudokuMask;
import com.metal_pony.bucket.sudoku.util.SudokuMask.LengthException;
import com.metal_pony.bucket.sudoku.util.SudokuMask.RangeException;

public class TestSudokuMask {
    SudokuMask mask;
    Random rand = new Random();

    // bit string -> bits count
    HashMap<String,Integer> cases = new HashMap<>() {{
        put("0".repeat(81), 0);
        put("011011011100110011001111111110000000101110101111010100010110100100011111111101111", 49);
        put("111111011011111111101111111111110111111010111111111010111011000100000111001111011", 61);
        put("111111111111111110111111111111111111011111110111111111101101111111111111111111011", 75);
        put("011011111111111111111101111111101111111111111110111111111111111011111101110111110", 72);
        put("110111100111111001011000111111011011101111101101111011100111110111101010111001101", 57);
        put("000000000000000000000000100000010000000000000000000000100000001000000000000000000", 4);
        put("000111000000000111001000000001001100010000000101101110010101100011001000100001110", 28);
        put("011110111111110101111111011111111111011111110111111111011111111111111111011111111", 72);
        put("010111100110011111111111111100011111011111111100100011001011100000111011111011001", 54);
        put("000000000000000100000100010001010001000100010000010100000100001001010000000000001", 15);
        put("001111001111011111101111010111101111110111110001100110111110101111011111111110011", 60);
        put("010000111110100001011100100000011101001101101000100010000010010101110111010111111", 39);
        put("110110111111110101001000011001011101000100110011110111111111111111100101010111111", 54);
        put("111110111111111111111110111110111110111111111111111111111111111111111110111111111", 76);
        put("000011010000001010011100010111100001011101100100110100101110110110010001010101000", 36);
        put("000000000000000010100000000000000000000000000001000000000000000000000000000000010", 4);
        put("010000110100110011010000100000000101001010000010011000001100000000001001011010010", 25);
        put("111011111111110011100110001001100111111101111111110101111111111101100110111110111", 61);
        put("1".repeat(81), 81);
    }};

    @BeforeEach
    void before() {
        mask = new SudokuMask();
    }

    @Nested
    class Static {
        @Test
        void full_returnsFullMask() {
            mask = SudokuMask.full();
            assertEquals(81, mask.bitCount());
            assertEquals("1".repeat(81), mask.toString());
            assertEquals(SudokuMask.full(), mask); // via Object.equals(obj)
            assertEquals(SudokuMask.full().hashCode(), mask.hashCode());
        }

        @Test
        void random_whenBitCountOutOfRange_throws() {
            // Negatives
            for (int bitCount = -1; bitCount > -100; bitCount--) {
                final int _bitCount = bitCount;
                assertThrows(RangeException.class, () -> {
                    SudokuMask.random(_bitCount);
                });
            }
            // Too large
            for (int bitCount = 82; bitCount > 200; bitCount++) {
                final int _bitCount = bitCount;
                assertThrows(RangeException.class, () -> {
                    SudokuMask.random(_bitCount);
                });
            }
        }

        @Test
        void random_returnsMaskWithNumBitsSet() {
            assertEquals(new SudokuMask(), SudokuMask.random(0));
            assertEquals(SudokuMask.full(), SudokuMask.random(81));

            for (int numBits = 0; numBits <= 81; numBits++) {
                for (int t = 0; t < 10; t++) {
                    mask = SudokuMask.random(numBits);
                    assertEquals(numBits, mask.bitCount());

                    // For redundancy, count the 1 chars in the string
                    String maskStr = mask.toString();
                    int count = 0;
                    for (char c : maskStr.toCharArray()) {
                        if (c == '1') {
                            count++;
                        }
                    }
                    assertEquals(numBits, count);
                }
            }
        }
    }

    @Test
    void default_constructor() {
        assertEquals(0, mask.bitCount());
        assertEquals("0".repeat(81), mask.toString());
    }

    @Test
    void chars_constructor() {
        assertThrows(LengthException.class, () -> { new SudokuMask("".toCharArray()); });
        assertThrows(LengthException.class, () -> { new SudokuMask("1".repeat(80).toCharArray()); });
        assertThrows(LengthException.class, () -> { new SudokuMask("1".repeat(82).toCharArray()); });

        // Check that non-zero digit characters set bits
        char[] input = new char[81];
        Arrays.fill(input, '0');
        int expectedBitCount = 0;
        mask = new SudokuMask(input);
        for (int i = 0; i < 81; i++) {
            input[i] = '1';
            expectedBitCount++;
            mask = new SudokuMask(input);
            assertEquals(expectedBitCount, mask.bitCount());
            assertEquals(new String(input), mask.toString());
            assertTrue(mask.testBit(i));
        }
    }

    @Test
    void string_constructor() {
        assertThrows(LengthException.class, () -> { new SudokuMask(""); });
        assertThrows(LengthException.class, () -> { new SudokuMask("1".repeat(80)); });
        assertThrows(LengthException.class, () -> { new SudokuMask("1".repeat(82)); });

        // Check that non-zero digit characters set bits
        char[] input = "0".repeat(81).toCharArray();
        int expectedBitCount = 0;
        mask = new SudokuMask(new String(input));
        for (int i = 0; i < 81; i++) {
            input[i] = '1';
            expectedBitCount++;
            mask = new SudokuMask(new String(input));
            assertEquals(expectedBitCount, mask.bitCount());
            assertEquals(new String(input), mask.toString());
            assertTrue(mask.testBit(i));
        }
    }

    @Test
    void test_equals_and_hashCode() {
        assertFalse(mask.equals(null));
        assertFalse(mask.equals(new Object()));
        assertTrue(mask.equals(mask));
        assertTrue(mask.hashCode() == mask.hashCode());

        SudokuMask equalMask = new SudokuMask(mask.toString());
        // Equality both ways
        assertTrue(mask.equals(equalMask));
        assertTrue(equalMask.equals(mask));
        assertEquals(mask.hashCode(), equalMask.hashCode());

        // Set / Unset bits at random to both instances and check that equality is maintained
        for (int n = 0; n < 100; n++) {
            int bit = rand.nextInt(81);
            mask.setBit(bit);
            equalMask.setBit(bit);
            assertTrue(mask.equals(equalMask));
            assertTrue(equalMask.equals(mask));
            assertEquals(mask.hashCode(), equalMask.hashCode());
        }
        // Masks are equal at this point

        // Flip each bit equalMask and check that equality is broken
        for (int i = 0; i < 81; i++) {
            equalMask.flipBit(i);
            assertFalse(mask.equals(equalMask));
            assertFalse(equalMask.equals(mask));
        }

        // Sync the bits again and test one more time
        equalMask.flip();
        assertTrue(mask.equals(equalMask));
        assertTrue(equalMask.equals(mask));
        assertEquals(mask.hashCode(), equalMask.hashCode());
    }

    @Test
    void test_toString() {
        assertEquals("0".repeat(81), mask.toString());
        assertEquals("1".repeat(81), SudokuMask.full().toString());

        char[] input = "0".repeat(81).toCharArray();
        for (int i = 0; i < 81; i++) {
            mask.setBit(i);
            input[i] = '1';
            assertEquals(new String(input), mask.toString());
        }
    }

    @Test
    void toHexString() {
        assertEquals("0", new SudokuMask().toHexString());
        assertEquals("1ffffffffffffffffffff", SudokuMask.full().toHexString());

        String[][] cases = new String[][]{
            new String[]{"1".repeat(17)+"0".repeat(64), "1ffff0000000000000000"},
            new String[]{"0".repeat(17)+"1".repeat(64), "ffffffffffffffff"},
            new String[]{"1".repeat(17)+"101".repeat(21)+"1", "1ffffb6db6db6db6db6db"},
        };

        for (String[] c : cases) {
            String maskStr = c[0];
            String expectedHexStr = c[1];
            assertEquals(expectedHexStr, new SudokuMask(maskStr).toHexString());
        }
    }

    @Test
    void bitCount() {
        for (Entry<String,Integer> c : cases.entrySet()) {
            mask = new SudokuMask(c.getKey());
            assertEquals(c.getValue(), mask.bitCount());
        }
    }

    @Test
    void testBit() {
        assertThrows(RangeException.class, () -> { mask.testBit(-2); });
        assertThrows(RangeException.class, () -> { mask.testBit(-1); });
        assertThrows(RangeException.class, () -> { mask.testBit(82); });
        assertThrows(RangeException.class, () -> { mask.testBit(83); });

        for (Entry<String,Integer> c : cases.entrySet()) {
            char[] chars = c.getKey().toCharArray();
            mask = new SudokuMask(chars);
            for (int i = 0; i < 81; i++) {
                assertEquals(chars[i] == '1', mask.testBit(i));
            }
        }
    }

    @Test
    void add() {
        String originalMask = "000011010000001010011100010111100001011101100100110100101110110110010001010101000";
        String otherMask    = "000000000000000010100000000000000000000000000001000000000000000000000000000000010";
        String expectedMask = "000011010000001010111100010111100001011101100101110100101110110110010001010101010";

        mask = new SudokuMask(originalMask);
        int originalMaskBitCount = mask.bitCount();
        SudokuMask other = new SudokuMask(otherMask);
        SudokuMask result;

        // mask.add returns itself, nothing should change
        result = mask.add(mask);
        assertTrue(mask == result);
        assertEquals(originalMaskBitCount, mask.bitCount());
        assertEquals(originalMask, result.toString());

        // mask.add(new SudokuMask()) returns itself, nothing should change
        result = mask.add(new SudokuMask());
        assertTrue(mask == result);
        assertEquals(originalMaskBitCount, mask.bitCount());
        assertEquals(originalMask, result.toString());

        // mask.add(SudokuMask.full()) returns itself, all bits are set
        result = mask.add(SudokuMask.full());
        assertTrue(mask == result);
        assertEquals(81, mask.bitCount());
        assertEquals(SudokuMask.full().toString(), result.toString());

        // mask.add(other) returns itself, some bits were set (+3)
        // reset mask
        mask = new SudokuMask(originalMask);
        result = mask.add(other);
        assertTrue(mask == result);
        assertEquals(originalMaskBitCount + 3, mask.bitCount());
        assertEquals(expectedMask, result.toString());
    }

    @Test
    void setBit() {
        assertThrows(RangeException.class, () -> { mask.setBit(-2); });
        assertThrows(RangeException.class, () -> { mask.setBit(-1); });
        assertThrows(RangeException.class, () -> { mask.setBit(82); });
        assertThrows(RangeException.class, () -> { mask.setBit(83); });

        char[] expectedVals = new char[81];
        Arrays.fill(expectedVals, '0');
        for (int i = 0; i < 81; i++) {
            expectedVals[i] = '1';

            assertTrue(mask == mask.setBit(i));
            assertEquals(new String(expectedVals), mask.toString());

            // Setting again should have no further effect
            assertTrue(mask == mask.setBit(i));
            assertEquals(new String(expectedVals), mask.toString());
        }
    }

    @Test
    void unsetBit() {
        assertThrows(RangeException.class, () -> { mask.unsetBit(-2); });
        assertThrows(RangeException.class, () -> { mask.unsetBit(-1); });
        assertThrows(RangeException.class, () -> { mask.unsetBit(82); });
        assertThrows(RangeException.class, () -> { mask.unsetBit(83); });

        char[] expectedVals = new char[81];
        Arrays.fill(expectedVals, '1');
        mask = SudokuMask.full();
        for (int i = 0; i < 81; i++) {
            expectedVals[i] = '0';

            assertTrue(mask == mask.unsetBit(i));
            assertEquals(new String(expectedVals), mask.toString());

            // Unsetting again should have no further effect
            assertTrue(mask == mask.unsetBit(i));
            assertEquals(new String(expectedVals), mask.toString());
        }
    }

    @Test
    void flipBit() {
        assertThrows(RangeException.class, () -> { mask.flipBit(-2); });
        assertThrows(RangeException.class, () -> { mask.flipBit(-1); });
        assertThrows(RangeException.class, () -> { mask.flipBit(82); });
        assertThrows(RangeException.class, () -> { mask.flipBit(83); });

        char[] expectedVals = new char[81];
        Arrays.fill(expectedVals, '0');
        for (int i = 0; i < 81; i++) {
            expectedVals[i] = '1';

            assertTrue(mask == mask.flipBit(i));
            assertEquals(new String(expectedVals), mask.toString());

            // Now flip it back
            expectedVals[i] = '0';
            assertTrue(mask == mask.flipBit(i));
            assertEquals(new String(expectedVals), mask.toString());
        }
    }

    @Test
    void flip() {
        for (Entry<String,Integer> c : cases.entrySet()) {
            char[] chars = c.getKey().toCharArray();
            mask = new SudokuMask(chars);
            for (int i = 0; i < 81; i++) {
                chars[i] = chars[i] == '0' ? '1' : '0';
            }
            assertTrue(mask == mask.flip());
            assertEquals(new String(chars), mask.toString());
        }
    }

    @Test
    void intersects() {
        assertFalse(mask.intersects(null));
        // Empty never intersects
        assertFalse(mask.intersects(new SudokuMask()));
        assertFalse(mask.intersects(SudokuMask.full()));

        // None of these intersect with one another
        String[] nonOverlapping = new String[] {
            "010100000000000000010001010000000000000001100000000000000000000001010000010100000",
            "000000000010000000000100000101100100100000000100000000000001000000000000000000000",
            "000000000000000000001000001000000000000000000000000011000010010000000000000001000",
            "100000000000000000000000000000000000000010000010001000000000000000000100000000000",
            "000010000100010010000000000000000000000100010000100000000000000010000001001000011",
            "001001000001000001100000100010000001001000000001000000011000000100000000000010000",
            "000000001000000000000010000000000010010000001000000100000000001000101010100000000",
            "000000010000001000000000000000010000000000000000010000100100000000000000000000100",
            "000000100000100100000000000000001000000000000000000000000000100000000000000000000"
        };

        for (int i = 0; i < nonOverlapping.length - 1; i++) {
            SudokuMask a = new SudokuMask(nonOverlapping[i]);
            assertTrue(a.intersects(a));
            for (int j = i + 1; j < nonOverlapping.length; j++) {
                SudokuMask b = new SudokuMask(nonOverlapping[j]);
                assertTrue(b.intersects(b));
                assertFalse(a.intersects(b));
                assertFalse(b.intersects(a));
            }
        }

        // All of these intersect with one another
        String[] allOverlapping = new String[] {
            "010100000000000000010001011000000000000001100000000000000000000001010000010100000",
            "000000000010000000000100001101100100100000000100000000000001000000000000000000000",
            "000000000000000000001000001000000000000000000000000011000010010000000000000001000",
            "100000000000000000000000001000000000000010000010001000000000000000000100000000000",
            "000010000100010010000000001000000000000100010000100000000000000010000001001000011",
            "001001000001000001100000101010000001001000000001000000011000000100000000000010000",
            "000000001000000000000010001000000010010000001000000100000000001000101010100000000",
            "000000010000001000000000001000010000000000000000010000100100000000000000000000100",
            "000000100000100100000000001000001000000000000000000000000000100000000000000000000"
        };

        for (int i = 0; i < allOverlapping.length - 1; i++) {
            SudokuMask a = new SudokuMask(allOverlapping[i]);
            assertTrue(a.intersects(a));
            for (int j = i + 1; j < allOverlapping.length; j++) {
                SudokuMask b = new SudokuMask(allOverlapping[j]);
                assertTrue(b.intersects(b));
                assertTrue(a.intersects(b));
                assertTrue(b.intersects(a));
            }
        }
    }

    @Test
    void hasBitsSet() {
        // No longer supported
        // assertFalse(mask.hasBitsSet(null));

        // Empty never has bits set
        assertFalse(mask.hasBitsSet(mask));
        assertFalse(mask.hasBitsSet(new int[0]));
        assertFalse(mask.hasBitsSet(new SudokuMask()));
        assertFalse(mask.hasBitsSet(SudokuMask.full()));
        assertFalse(mask.hasBitsSet(SudokuMask.full().toIndices()));
        assertFalse(SudokuMask.full().hasBitsSet(mask));
        assertFalse(SudokuMask.full().hasBitsSet(new int[0]));

        String[] falseCases = new String[] {
            "010100000000000000010001010000000000000001100000000000000000000001010000010100000",
            "000000000010000000000100000101100100100000000100000000000001000000000000000000000",
            "000000000000000000001000001000000000000000000000000011000010010000000000000001000",
            "100000000000000000000000000000000000000010000010001000000000000000000100000000000",
            "000010000100010010000000000000000000000100010000100000000000000010000001001000011",
            "001001000001000001100000100010000001001000000001000000011000000100000000000010000",
            "000000001000000000000010000000000010010000001000000100000000001000101010100000000",
            "000000010000001000000000000000010000000000000000010000100100000000000000000000100",
            "000000100000100100000000000000001000000000000000000000000000100000000000000000000",
        };

        for (int i = 0; i < falseCases.length - 1; i++) {
            SudokuMask a = new SudokuMask(falseCases[i]);
            assertTrue(a.hasBitsSet(a));
            assertTrue(a.hasBitsSet(a.toIndices()));
            for (int j = i + 1; j < falseCases.length; j++) {
                SudokuMask b = new SudokuMask(falseCases[j]);
                assertTrue(b.hasBitsSet(b));
                assertTrue(b.hasBitsSet(b.toIndices()));
                assertFalse(a.hasBitsSet(b));
                assertFalse(a.hasBitsSet(b.toIndices()));
                assertFalse(b.hasBitsSet(a));
                assertFalse(b.hasBitsSet(a.toIndices()));
            }
        }

        mask.setBit(19);
        for (int t = 0; t < 10; t++) {
            SudokuMask other = new SudokuMask(mask.toString());
            for (int n = 0; n < 50; n++) {
                other.setBit(rand.nextInt(81));
                assertTrue(other.hasBitsSet(mask));
                assertTrue(other.hasBitsSet(mask.toIndices()));
            }
            assertFalse(mask.hasBitsSet(other));
            assertFalse(mask.hasBitsSet(other.toIndices()));
        }
    }

    @Test
    void testEquals_and_hash() {
        assertTrue(new SudokuMask().equals(new SudokuMask()));
        assertTrue(SudokuMask.full().equals(SudokuMask.full()));
        assertEquals(new SudokuMask().hashCode(), new SudokuMask().hashCode());
        assertEquals(SudokuMask.full().hashCode(), SudokuMask.full().hashCode());

        assertTrue(mask.equals(new SudokuMask()));
        assertTrue(new SudokuMask().equals(mask));
        assertEquals(mask.hashCode(), new SudokuMask().hashCode());

        assertFalse(mask.equals(SudokuMask.full()));
        assertFalse(SudokuMask.full().equals(mask));
        assertNotEquals(SudokuMask.full().hashCode(), mask.hashCode());

        for (String stra : cases.keySet()) {
            SudokuMask a = new SudokuMask(stra);

            for (String strb : cases.keySet()) {
                SudokuMask b = new SudokuMask(strb);

                if (stra.equals(strb)) {
                    assertTrue(a.equals(b));
                    assertTrue(b.equals(a));
                    assertEquals(a.hashCode(), b.hashCode());
                } else {
                    assertFalse(a.equals(b));
                    assertFalse(b.equals(a));
                    assertNotEquals(a.hashCode(), b.hashCode());
                }
            }
        }
    }

    // integration test
    void usage() {

    }
}
