package com.metal_pony.bucket.sudoku.util;

import java.util.Arrays;

/**
 * Represents a sudoku mask containing 81 bits.
 *
 * The mask is intended to be applied to a sudoku grid to create a
 * partially-filled grid or puzzle, through <code>sudokuGrid.filter(mask)</code>.
 * The bits set in the mask are associated with cells to carry over from the grid
 * to the resulting puzzle.
 */
public class SudokuMask {
    static final int N = 81;
    static final String RANGE_EXCEPTION = "";
    static final SudokuMask[] CELL_MASKS = new SudokuMask[N];
    static {
        for (int ci = 0; ci < N; ci++) {
            CELL_MASKS[ci] = new SudokuMask();
            CELL_MASKS[ci].setBit(ci);
        }
    }

    public static final class LengthException extends RuntimeException {
        LengthException() {
            super("Invalid length");
        }
    }

    public static final class RangeException extends RuntimeException {
        RangeException() {
            super("Out of range");
        }
        RangeException(int val) {
            super(String.format("Out of range: %d", val));
        }
    }

    /**
     * Returns a new SudokuMask with all bits set.
     */
    public static SudokuMask full() {
        SudokuMask mask = new SudokuMask();
        mask.bitsSet = N;
        Arrays.fill(mask.vals, '1');
        mask.bits[1] = 0x1FFFFL;
        mask.bits[0] = 0xFFFFFFFFFFFFFFFFL;
        return mask;
    }

    char[] vals;
    long[] bits;
    int bitsSet;

    /**
     * Creates a new SudokuMask from the given sudoku string.
     * Non-digit and '0' characters translate to unset bits.
     * @param sudokuStr
     * @throws LengthException If the string length is not 81.
     */
    public SudokuMask(String sudokuStr) {
        this();
        if (sudokuStr == null || sudokuStr.length() != N) throw new LengthException();
        setFromCharArr(sudokuStr.toCharArray());
    }

    /**
     * Creates a new SudokuMask from the given values.
     * Non-digit and '0' characters translate to unset bits.
     * @param vals
     * @throws LengthException If the array length is not 81.
     */
    public SudokuMask(char[] vals) {
        this();
        if (vals == null || vals.length != N) throw new LengthException();
        setFromCharArr(vals);
    }

    /**
     * Creates a new SudokuMask where all bits are unset.
     */
    public SudokuMask() {
        this.vals = new char[N];
        Arrays.fill(this.vals, '0');
        this.bits = new long[]{0L, 0L};
        this.bitsSet = 0;
    }

    private void setFromCharArr(char[] arr) {
        for (int i = 0; i < N; i++) {
            if (arr[i] > '0' && arr[i] <= '9') {
                setBit(i);
            }
        }
    }

    /**
     * @return The number of bits set.
     */
    public int bitCount() {
        return bitsSet;
    }

    /**
     * Gets whether the given bit is set in the mask.
     * @param bit Index of the bit to check. Aka sudoku cell index.
     * @return True if the bit associated with the sudoku cell is set; otherwise false.
     */
    public boolean testBit(int bit) {
        if (bit < 0 || bit >= N) throw new RangeException(bit);
        return vals[bit] == '1';
    }

    /**
     * Sets the bit at the given index.
     * @param bit Index of the bit to set. Aka sudoku cell index.
     * @return This SudokuMask for convenience.
     */
    public SudokuMask setBit(int bit) {
        if (bit < 0 || bit >= N) throw new RangeException(bit);
        if (!testBit(bit)) {
            bitsSet++;
            vals[bit] = '1';
            int bsi = bit > (N - 1 - 64) ? 0 : 1;
            int bi = (N - 1 - bit) % 64;
            bits[bsi] |= 1L<<bi;
        }
        return this;
    }

    /**
     * Unsets the bit at the given index.
     * @param bit Index of the bit to unset. Aka sudoku cell index.
     * @return This SudokuMask for convenience.
     */
    public SudokuMask unsetBit(int bit) {
        if (bit < 0 || bit >= N) throw new RangeException(bit);
        if (testBit(bit)) {
            bitsSet--;
            vals[bit] = '0';
            int bsi = bit > (N - 1 - 64) ? 0 : 1;
            int bi = (N - 1 - bit) % 64;
            bits[bsi] ^= 1L<<bi;
        }
        return this;
    }

    /**
     * Flips the bit at the given index.
     * @param bit Index of the bit to flip. Aka sudoku cell index.
     * @return  This SudokuMask for convenience.
     */
    public SudokuMask flipBit(int bit) {
        if (bit < 0 || bit >= N) throw new RangeException(bit);
        if (testBit(bit)) {
            unsetBit(bit);
        } else {
            setBit(bit);
        }
        return this;
    }

    /**
     * Flips all bits.
     * @return This SudokuMask for convenience.
     */
    public SudokuMask flip() {
        for (int i = N - 1; i >= 0; i--) {
            flipBit(i);
        }
        return this;
    }

    // caveat: false if either are empty
    /**
     * Checks whether this mask and the given mask have any set bits in common.
     *
     * If either have no bits set, this returns false.
     * @param other The other SudokuMask to compare bits.
     * @return True if this and `other` have any set bits in common; otherwise false.
     */
    public boolean hasAnyOverlapWith(SudokuMask other) {
        if (other == null) return false;
        return ((bits[0] & other.bits[0]) | (bits[1] & other.bits[1])) != 0L;
    }

    // caveat: false if either are empty
    /**
     * Checks whether this mask has all the set bits of the given mask.
     *
     * If either have no bits set, this returns false.
     * @param other The other SudokuMask to compare bits.
     * @return True if this has all the set bits of `other`; otherwise false.
     */
    public boolean overlapsAllOf(SudokuMask other) {
        if (other == null) return false;
        if (bitsSet == 0 || other.bitsSet == 0) return false;
        return (
            (bits[0] | other.bits[0]) == bits[0] &&
            (bits[1] | other.bits[1]) == bits[1]
        );
    }

    @Override
    public String toString() {
        return new String(vals);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SudokuMask)) return false;
        if (this == obj) return true;
        SudokuMask _obj = (SudokuMask) obj;
        return (bits[0] == _obj.bits[0] && bits[1] == _obj.bits[1]);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
