package com.metal_pony.bucket.sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.metal_pony.bucket.sudoku.util.SudokuMask;
import com.metal_pony.bucket.util.Counting;
import com.metal_pony.bucket.util.Shuffler;
import com.metal_pony.bucket.util.ThreadPool;

public class Sudoku {
    static final char EMPTY_CHAR = '.';

    public static final int RANK = 3;
    public static final int DIGITS = 9; // rank^2
    public static final int SPACES = 81; // rank^2^2
    /** Value representing all candidates a cell may be.*/
    static final int ALL = 511; // 2^rank^2 - 1
    public static final int MIN_CLUES = 17; // rank^2 * 2 - 1

    static final int ROW_MASK = ALL << (DIGITS * 2);
    static final int COL_MASK = ALL << DIGITS;
    static final int REGION_MASK = ALL;
    static final int FULL_CONSTRAINTS = ROW_MASK | COL_MASK | REGION_MASK;

    public static final int[] ENCODER = new int[] { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256 };
    static final int[] DECODER = new int[1<<DIGITS];
    static {
        for (int digit = 1; digit <= DIGITS; digit++) {
            DECODER[1 << (digit - 1)] = digit;
        }
    }

    /**
     * Maps candidates mask to the array of digits it represents.
     */
    public static final int[][] CANDIDATES_ARR = new int[1<<DIGITS][];

    /**
     * Maps candidates masks to the array of digits (encoded) it represents.
     */
    static final int[][] CANDIDATES = new int[CANDIDATES_ARR.length][];
    static {
        for (int val = 0; val < CANDIDATES_ARR.length; val++) {
            CANDIDATES_ARR[val] = new int[Integer.bitCount(val)];
            CANDIDATES[val] = new int[Integer.bitCount(val)];
            int _val = val;
            int i = 0;
            int j = 0;
            int digit = 1;
            while (_val > 0) {
                if ((_val & 1) > 0) {
                    CANDIDATES_ARR[val][i++] = digit;
                    CANDIDATES[val][j++] = ENCODER[digit];
                }
                _val >>= 1;
                digit++;
            }
        }
    }

    /**
     * Maps indices [0, 511] to its bit count.
     */
    static final int[] BIT_COUNT_MAP = new int[1<<DIGITS];

    /**
     * Digit combinations indexed by bit count (aka digit count).
     */
    public static final int[][] DIGIT_COMBOS_MAP = new int[DIGITS + 1][];
    static {
        for (int nDigits = 0; nDigits < DIGIT_COMBOS_MAP.length; nDigits++) {
            DIGIT_COMBOS_MAP[nDigits] = new int[Counting.nChooseK(DIGITS, nDigits).intValueExact()];
        }
        int[] combosCount = new int[DIGITS + 1];
        for (int i = 0; i < BIT_COUNT_MAP.length; i++) {
            int bits = Integer.bitCount(i);
            BIT_COUNT_MAP[i] = bits;
            DIGIT_COMBOS_MAP[bits][combosCount[bits]++] = i;
        }
    }

    static int encode(int digit) {
        return ENCODER[digit];
    }

    public static int decode(int encoded) {
        return DECODER[encoded];
    }

    public static boolean isDigit(int encoded) {
        return DECODER[encoded] > 0;
    }

    public static int cellRow(int ci) { return ci / DIGITS; }
	public static int cellCol(int ci) { return ci % DIGITS; }
	public static int cellRegion(int ci) {
		int regionRow = ci / (RANK * DIGITS);
		int regionCol = (ci % DIGITS) / RANK;
		return (regionRow * RANK) + regionCol;
	}

    public static int[] CELL_ROWS = new int[SPACES];
    public static int[] CELL_COLS = new int[SPACES];
    public static int[] CELL_REGIONS = new int[SPACES];
    public static int[][] ROW_INDICES = new int[DIGITS][DIGITS];
    public static int[][] COL_INDICES = new int[DIGITS][DIGITS];
    public static int[][] REGION_INDICES = new int[DIGITS][DIGITS];
    public static int[][] BAND_INDICES = new int[3][3*DIGITS];
    public static int[][] STACK_INDICES = new int[3][3*DIGITS];
    public static int[][][] BAND_ROW_INDICES = new int[3][3][DIGITS];
    public static int[][][] STACK_COL_INDICES = new int[3][3][DIGITS];
    static {
        int[] rowi = new int[DIGITS];
        int[] coli = new int[DIGITS];
        int[] regi = new int[DIGITS];
        for (int i = 0; i < SPACES; i++) {
            int row = cellRow(i);
            int col = cellCol(i);
            int region = cellRegion(i);
            CELL_ROWS[i] = row;
            CELL_COLS[i] = col;
            CELL_REGIONS[i] = region;

            ROW_INDICES[row][rowi[row]++] = i;
            COL_INDICES[col][coli[col]++] = i;
            REGION_INDICES[region][regi[region]++] = i;

            int band = row / RANK;
            int rowInBand = row % RANK;
		    int stack = col / RANK;
            int colInStack = col % RANK;
            int indexInBand = i % (DIGITS * RANK);
            int indexInStack = (row * RANK) + colInStack;
            BAND_INDICES[band][indexInBand] = i;
            STACK_INDICES[stack][indexInStack] = i;
            BAND_ROW_INDICES[band][rowInBand][col] = i;
            STACK_COL_INDICES[stack][colInStack][row] = i;
        }
    }
    public static int[][] ROW_NEIGHBORS = new int[SPACES][DIGITS - 1];
    public static int[][] COL_NEIGHBORS = new int[SPACES][DIGITS - 1];
    public static int[][] REGION_NEIGHBORS = new int[SPACES][DIGITS - 1];
    public static int[][] CELL_NEIGHBORS = new int[SPACES][3*(DIGITS-1) - (DIGITS-1)/2]; // Not checked if true for other ranks
    static {
        for (int ci = 0; ci < SPACES; ci++) {
            int row = cellRow(ci);
            int col = cellCol(ci);
            int region = cellRegion(ci);

            int ri = 0;
            int coli = 0;
            int regi = 0;
            int ni = 0;

            for (int cj = 0; cj < SPACES; cj++) {
                if (ci == cj) continue;
                int jrow = cellRow(cj);
                int jcol = cellCol(cj);
                int jregion = cellRegion(cj);

                if (jrow == row) {
                    ROW_NEIGHBORS[ci][ri++] = cj;
                }
                if (jcol == col) {
                    COL_NEIGHBORS[ci][coli++] = cj;
                }
                if (jregion == region) {
                    REGION_NEIGHBORS[ci][regi++] = cj;
                }
                if (jrow == row || jcol == col || jregion == region) {
                    CELL_NEIGHBORS[ci][ni++] = cj;
                }
            }
        }
    }

    private static boolean isAreaValid(int[] digits, int[] areaIndices) {
        if (digits.length != SPACES) return false;
		int digitsSeen = 0;
		for (int i = 0; i < DIGITS; i++) {
			int digit = digits[areaIndices[i]];
            if (digit < 0 || digit > DIGITS) return false;
			if (digit > 0) {
				int digitMask = 1 << (digit - 1);
				if ((digitMask & digitsSeen) > 0) return false;
				digitsSeen |= digitMask;
			}
		}
		return true;
	}

	private static boolean isAreaFull(int[] digits, int[] areaIndices) {
        if (digits.length != SPACES) return false;
		for (int i = 0; i < DIGITS; i++) {
            int digit = digits[areaIndices[i]];
			if (digit <= 0 || digit > DIGITS) return false;
		}
		return true;
	}

    public static boolean isRowValid(int[] digits, int rowIndex) {
		return isAreaValid(digits, ROW_INDICES[rowIndex]);
	}

	public static boolean isColValid(int[] digits, int colIndex) {
		return isAreaValid(digits, COL_INDICES[colIndex]);
	}

	public static boolean isRegionValid(int[] digits, int regionIndex) {
		return isAreaValid(digits, REGION_INDICES[regionIndex]);
	}

	public static boolean isRowFull(int[] digits, int rowIndex) {
		return isAreaFull(digits, ROW_INDICES[rowIndex]);
	}

	public static boolean isColFull(int[] digits, int colIndex) {
		return isAreaFull(digits, COL_INDICES[colIndex]);
	}

	public static boolean isRegionFull(int[] digits, int regionIndex) {
		return isAreaFull(digits, REGION_INDICES[regionIndex]);
	}

    public static boolean isValid(int[] digits) {
		if (digits.length != SPACES) return false;

		int[] rowValidity = new int[DIGITS];
		int[] colValidity = new int[DIGITS];
		int[] regionValidity = new int[DIGITS];

		for (int ci = 0; ci < SPACES; ci++) {
			int digit = digits[ci];
            if (digit < 0 || digit > DIGITS) return false;
			if (digit == 0) continue;

			int row = CELL_ROWS[ci];
			int col = CELL_COLS[ci];
			int region = CELL_REGIONS[ci];
			int digitMask = 1 << (digit - 1);
			if (
                (digitMask & rowValidity[row]) > 0 ||
                (digitMask & colValidity[col]) > 0 ||
                (digitMask & regionValidity[region]) > 0
			) {
				return false;
			}
			rowValidity[row] |= digitMask;
			colValidity[col] |= digitMask;
			regionValidity[region] |= digitMask;
		}

		return true;
	}

	public static boolean isFull(int[] digits) {
		if (digits.length != SPACES) return false;
		for (int ci = 0; ci < DIGITS; ci++) {
			if (digits[ci] <= 0 || digits[ci] > DIGITS) return false;
		}
		return true;
	}

	public static boolean isSolved(int[] digits) {
		if (digits.length != SPACES) return false;

        int[] rowValidity = new int[DIGITS];
		int[] colValidity = new int[DIGITS];
		int[] regionValidity = new int[DIGITS];

		for (int ci = 0; ci < SPACES; ci++) {
			int digit = digits[ci];
            if (digit <= 0 || digit > DIGITS) return false;

			int row = CELL_ROWS[ci];
			int col = CELL_COLS[ci];
			int region = CELL_REGIONS[ci];
			int digitMask = 1 << (digit - 1);
			if (
                (digitMask & rowValidity[row]) > 0 ||
                (digitMask & colValidity[col]) > 0 ||
                (digitMask & regionValidity[region]) > 0
			) {
				return false;
			}
			rowValidity[row] |= digitMask;
			colValidity[col] |= digitMask;
			regionValidity[region] |= digitMask;
		}

		return true;
	}

    /**
	 * Rotates the given matrix array 90 degrees clockwise.
	 * @param arr The matrix to rotate.
     * @param n Length of one of the sides.
	 */
	public static int[] rotate90(int[] arr, int n) {
        if (arr == null) throw new NullPointerException();
        if (n < 0) throw new IllegalArgumentException("n must be nonnegative");
        if (arr.length != n * n) throw new IllegalArgumentException("arr length not n square");
		for (int layer = 0; layer < n / 2; layer++) {
			int first = layer;
			int last = n - 1 - layer;
			for (int i = first; i < last; i++) {
				int offset = i - first;
				int top = arr[first * n + i];
				arr[first * n + i] = arr[(last - offset) * n + first];
				arr[(last - offset) * n + first] = arr[last * n + (last - offset)];
				arr[last * n + (last - offset)] = arr[i * n + last];
				arr[i * n + last] = top;
			}
		}
		return arr;
	}

    /**
	 * Reflects the board values over the horizontal axis (line from bottom to top).
	 * If the `arr.length / rows` is not a whole number, an error will be thrown.
	 * @param arr The matrix to reflect.
	 * @param rows The number of rows in the matrix.
	 */
	public static int[] reflectOverHorizontal(int[] arr, int rows) {
        if (arr == null) throw new NullPointerException();
        if (rows <= 0) throw new IllegalArgumentException("rows must be positive");
        if (arr.length % rows != 0) throw new IllegalArgumentException("array length must be divisible by number of rows");
		int cols = arr.length / rows;
		for (int r = 0; r < (rows / 2); r++) {
			for (int c = 0; c < cols; c++) {
                int ai = r * cols + c;
				int bi = (rows - r - 1) * cols + c;
				arr[ai] ^= arr[bi];
				arr[bi] ^= arr[ai];
				arr[ai] ^= arr[bi];
			}
		}
		return arr;
	}

	/**
	 * Reflects the board values over the vertical axis (line from left to right).
	 * If the `arr.length / rows` is not a whole number, an error will be thrown.
	 * @param arr The matrix to reflect.
	 * @param rows The number of rows in the matrix.
	 */
	public static int[] reflectOverVertical(int[] arr, int rows) {
        if (arr == null) throw new NullPointerException();
        if (rows <= 0) throw new IllegalArgumentException("rows must be positive");
        if (arr.length % rows != 0) throw new IllegalArgumentException("array length must be divisible by number of rows");
		int cols = arr.length / rows;
		for (int c = 0; c < (cols / 2); c++) {
			for (int r = 0; r < rows; r++) {
				int ai = r * cols + c;
				int bi = r * cols + (cols - c - 1);
				arr[ai] ^= arr[bi];
				arr[bi] ^= arr[ai];
				arr[ai] ^= arr[bi];
			}
		}
		return arr;
	}

    public static int[] reflectOverDiagonal(int[] arr, int rows) {
        reflectOverVertical(arr, rows);
		rotate90(arr, rows);
        return arr;
    }

    public static int[] reflectOverAntiDiagonal(int[] arr, int rows) {
		rotate90(arr, rows);
        reflectOverVertical(arr, rows);
        return arr;
    }

    /**
     * Swaps the given bands. Bands are groups of 3 regions, horizontally.
     * @param b1 Band index (0, 1, or 2)
     * @param b2 Band index (0, 1, or 2) Different than b1
     * @return This sudoku instance for convenience.
     */
    public static int[] swapBands(int[] arr, int b1, int b2) {
        if (b1 == b2) return arr;
        if (b1 < 0 || b2 < 0 || b1 > 2 || b2 > 2)
            throw new IllegalArgumentException("swapBands error, specified band(s) out of bounds");
        for (int i = 0; i < 27; i++) {
            int ai = BAND_INDICES[b1][i];
            int bi = BAND_INDICES[b2][i];

            arr[ai] ^= arr[bi];
            arr[bi] ^= arr[ai];
            arr[ai] ^= arr[bi];
        }
        return arr;
    }

    /**
     * Swaps the given rows within a band.
     * @param b1 Band index (0, 1, or 2)
     * @param ri1 Row index (0, 1, or 2)
     * @param ri2 Row index (0, 1, or 2) Different than ri1
     * @return This sudoku instance for convenience.
     */
    public static int[] swapBandRows(int[] arr, int bi, int ri1, int ri2) {
        if (ri1 == ri2) return arr;
        if (bi < 0 || bi > 2 || ri1 < 0 || ri2 < 0 || ri1 > 2 || ri2 > 2)
            throw new IllegalArgumentException("swapBandRows error, specified band or row(s) out of bounds");
        for (int i = 0; i < DIGITS; i++) {
            int ii = BAND_ROW_INDICES[bi][ri1][i];
            int jj = BAND_ROW_INDICES[bi][ri2][i];

            arr[ii] ^= arr[jj];
            arr[jj] ^= arr[ii];
            arr[ii] ^= arr[jj];
        }
        return arr;
    }

    /**
     * Swaps the given stacks. Stacks are groups of 3 regions, vertically.
     * @param s1 Stack index (0, 1, or 2)
     * @param s2 Stack index (0, 1, or 2) Different than s1
     * @return This sudoku instance for convenience.
     */
    public static int[] swapStacks(int[] arr, int s1, int s2) {
        if (s1 == s2) return arr;
        if (s1 < 0 || s2 < 0 || s1 > 2 || s2 > 2)
            throw new IllegalArgumentException("swapStacks error, specified stack(s) out of bounds");
        for (int i = 0; i < 27; i++) {
            int ai = STACK_INDICES[s1][i];
            int bi = STACK_INDICES[s2][i];

            arr[ai] ^= arr[bi];
            arr[bi] ^= arr[ai];
            arr[ai] ^= arr[bi];
        }
        return arr;
    }

    /**
     * Swaps the given columns within a stack.
     * @param b1 Stack index (0, 1, or 2)
     * @param ri1 Column index (0, 1, or 2)
     * @param ri2 Column index (0, 1, or 2) Different than si1
     * @return This sudoku instance for convenience.
     */
    public static int[] swapStackCols(int[] arr, int si, int ci1, int ci2) {
        if (ci1 == ci2) return arr;
        if (si < 0 || ci1 < 0 || ci2 < 0 || si > 2 || ci1 > 2 || ci2 > 2)
            throw new IllegalArgumentException("swapStackCols error, specified stack or col(s) out of bounds");
        for (int i = 0; i < Sudoku.DIGITS; i++) {
            int ii = STACK_COL_INDICES[si][ci1][i];
            int jj = STACK_COL_INDICES[si][ci2][i];

            arr[ii] ^= arr[jj];
            arr[jj] ^= arr[ii];
            arr[ii] ^= arr[jj];
        }
        return arr;
    }

    public static int[] normalize(int[] digits) {
        if (digits.length != SPACES) {
            throw new IllegalArgumentException("digits length must be 81");
        }
        // Top row must be filled in and valid.
        if (!isRowFull(digits, 0) || !isRowValid(digits, 0)) {
            throw new IllegalArgumentException("top row must be full and valid");
        }
        // All in array must be proper digits
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] < 0 || digits[ci] > DIGITS) {
                throw new IllegalArgumentException("bad value in sudoku matrix");
            }
        }

        for (int tarDigit = 1; tarDigit <= DIGITS; tarDigit++) {
			int curDigit = digits[tarDigit - 1];
			if (curDigit != tarDigit) {
                for (int ci = 0; ci < SPACES; ci++) {
                    if (digits[ci] == curDigit) {
                        digits[ci] = tarDigit;
                    } else if (digits[ci] == tarDigit) {
                        digits[ci] = curDigit;
                    }
                }
			}
		}

        return digits;
    }

    public static int[] CANDIDATE_PAIRS = new int[36];
    // There are (9 choose 2 = 36) pairs of digits.
    static {
        int i = 0;
        for (int n = 0b11; n < ALL; n++) {
            if (Integer.bitCount(n) == 2) {
                CANDIDATE_PAIRS[i++] = n;
            }
        }
    }
    public static boolean isCandidatePair(int n) {
        return (n > 0) && (n < ALL) && (Integer.bitCount(n) == 2);
    }

    public static final SudokuMask[] BAND_FILTERS;
    public static final SudokuMask[] STACK_FILTERS;
    static {
        BAND_FILTERS = new SudokuMask[BAND_INDICES.length];
        for (int bandIndex = 0; bandIndex < BAND_INDICES.length; bandIndex++) {
            SudokuMask bf = new SudokuMask();
            for (int i : BAND_INDICES[bandIndex]) {
                bf = bf.setBit(i);
            }
            BAND_FILTERS[bandIndex] = bf;
            // System.out.printf("BAND_FILTERS[%d] = %s\n", bandIndex, BAND_FILTERS[bandIndex].toString(2));
        }

        STACK_FILTERS = new SudokuMask[STACK_INDICES.length];
        for (int stackIndex = 0; stackIndex < STACK_INDICES.length; stackIndex++) {
            SudokuMask sf = new SudokuMask();
            for (int i : STACK_INDICES[stackIndex]) {
                sf = sf.setBit(SPACES - 1 - i);
            }
            STACK_FILTERS[stackIndex] = sf;
            // System.out.printf("STACK_FILTERS[%d] = %s\n", stackIndex, STACK_FILTERS[stackIndex].toString(2));
        }
    }

    /**
     * Checks that the given string is valid to be used to initialize a Sudoku instance.
     * (i.e. is proper length and contains digits, '.', or '-' chars).
     *
     * NOTE: This does NOT check if the grid is a valid sudoku.
     * For that, check <code>sudoku.solutionsFlag() == 1</code>.
     * @param gridStr
     * @return True if the string can be used to instantiate a Sudoku instance; otherwise false.
     */
    public static boolean isValidStr(String gridStr) {
        return conformGridStr(gridStr) != null;
    }

    private static String conformGridStr(String gridStr) {
        // Check for NULL and fail fast if length is bad.
        if (gridStr == null || gridStr.length() > SPACES) return null;
        // Expand '-' with 9 '0', and replace nonzero chars with '0'
        gridStr = gridStr.replaceAll("-", "0".repeat(DIGITS)).replaceAll("[^1-9]", "0");
        // Check for proper length
        return (gridStr.length() == SPACES) ? gridStr : null;
    }

    public static String toFullString(int[] digits) {
        StringBuilder strb = new StringBuilder("  ");
		String lineSep = System.lineSeparator();
        for (int i = 0; i < SPACES; i++) {
            if (digits[i] > 0) {
                strb.append(digits[i]);
            } else {
                strb.append('.');
            }

            // Print pipe between region columns
            if ((((i+1)%3) == 0) && (((i+1)%9) != 0)) {
                strb.append(" | ");
            } else {
                strb.append("   ");
            }

            if (((i+1)%9) == 0) {
                strb.append(lineSep);

                if (i < 80) {
                    // Border between region rows
                    if (((((i+1)/9)%3) == 0) && (((i/9)%8) != 0)) {
                        strb.append(" -----------+-----------+------------");
                    } else {
                        strb.append("            |           |            ");
                    }
                    strb.append(lineSep);
                    strb.append("  ");
                }
            }
        }

        return strb.toString();
    }

	public static String toMedString(int[] digits) {
        StringBuilder strb = new StringBuilder();
		String lineSep = System.lineSeparator();
        for (int i = 0; i < SPACES; i++) {
            if (digits[i] > 0) {
                strb.append(digits[i]);
            } else {
                strb.append('.');
            }

            // Print pipe between region columns
            if ((((i+1)%3) == 0) && (((i+1)%9) != 0)) {
                strb.append(" | ");
            } else {
                strb.append(' ');
			}

            if (((i+1)%9) == 0) {
				strb.append(lineSep);
                if (i < 80) {
                    // Border between region rows
                    if (((((i+1)/9)%3) == 0) && (((i/9)%8) != 0)) {
                        strb.append("------+-------+------");
						strb.append(lineSep);
                    }
                }
            }
        }

        return strb.toString();
    }

    /** Cell digits, as one would see on a sudoku board.*/
    int[] digits;

    /**
     * Tracks a cell's possible digits, encoded in 9-bit values (one bit per possible digit).
     *
     * e.g., If `candidates[55] = 0b110110001`, then the cell at index `55` has the possible
     * digits `9, 8, 6, 5, and 1`.
     *
     * `0b111111111` indicates that the cell can be any digit, and `0b000000000` indicates
     * that the cell cannot be any digit (and the puzzle is likely invalid).
     */
    int[] candidates;

    /**
     * Tracks the digits that have been used for each row, column, and region - combined into encoded 27-bit values.
     *
     * The encoding works as follows:
     * [9 bits for the row digits][9 bits for the column][9 bits for the region]
     *
     * Some bit manipulation is required to access the values for any given area.
     *
     * e.g., Get the digits used by row 7: `(constraints[7] >> 18) & ALL`.
     * This yields a 9-bit value which is an encoded form identical to `candidates`.
     */
    int[] constraints;

    int numEmptyCells = SPACES;

    boolean isValid = true;

    // TODO Implement isSolved cache
    // This should be cached true when isSolved is called, and invalidated whenever a value is changed
    boolean isSolved = false;

    public Sudoku() {
        this.digits = new int[SPACES];
        this.candidates = new int[SPACES];
        Arrays.fill(this.candidates, ALL);
        this.constraints = new int[DIGITS];
    }

    public Sudoku(Sudoku other) {
        this();
        this.numEmptyCells = other.numEmptyCells;
        this.isValid = other.isValid;
        System.arraycopy(other.digits, 0, this.digits, 0, SPACES);
        System.arraycopy(other.candidates, 0, this.candidates, 0, SPACES);
        System.arraycopy(other.constraints, 0, this.constraints, 0, DIGITS);
    }

    public Sudoku(String gridStr) {
        this();

        gridStr = conformGridStr(gridStr);
        if (gridStr == null) {
            throw new IllegalArgumentException("Malformed sudoku grid string");
        }

		for (int i = 0; i < SPACES; i++) {
            int digit = gridStr.charAt(i) - '0';
            if (digit > 0) {
                setDigit(i, digit);
            }
		}
    }

    public Sudoku(int[] digits) {
        this();

        if (digits.length != SPACES) {
            throw new IllegalArgumentException("sudoku initialization failed: insufficient board values");
        }

        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] > 0 || digits[ci] <= DIGITS) {
                setDigit(ci, digits[ci]);
            }
        }
    }

    private static int[] fromBytes(byte[] bytes) {
        if (bytes.length != 41) throw new IllegalArgumentException("bytes length must be 41");
        int[] _digits = new int[SPACES];
        for (int bi = 0; bi < 40; bi++) {
            _digits[2*bi] = (int)( (bytes[bi] >>> 4) & 0xf );
            _digits[2*bi + 1] = (int)( bytes[bi] & 0xf );
        }
        _digits[80] = (int)( (bytes[40] >>> 4) & 0xf );
        return _digits;
    }

    public Sudoku(byte[] bytes) {
        this(fromBytes(bytes));
    }

    public int getDigit(int ci) {
        return digits[ci];
    }

    public int getCandidate(int ci) {
        return candidates[ci];
    }

    public void setDigit(int ci, int digit) {
        int prevDigit = this.digits[ci];
        if (prevDigit == digit) return;

        digits[ci] = digit;
        candidates[ci] = ENCODER[digit];

        // Digit removed (or replaced)
        if (prevDigit > 0) {
            numEmptyCells++;
            removeConstraint(ci, prevDigit);
        }
        // Digit added (or replaced)
        if (digit > 0) {
            numEmptyCells--;
            addConstraint(ci, digit);
        }
    }

    void addConstraint(int ci, int digit) {
        int dMask = ENCODER[digit];
        constraints[CELL_ROWS[ci]] |= dMask << (DIGITS*2);
        constraints[CELL_COLS[ci]] |= dMask << DIGITS;
        constraints[CELL_REGIONS[ci]] |= dMask;
    }

    void removeConstraint(int ci, int digit) {
        int dMask = ENCODER[digit];
        constraints[CELL_ROWS[ci]] &= ~(dMask << (DIGITS*2));
        constraints[CELL_COLS[ci]] &= ~(dMask << DIGITS);
        constraints[CELL_REGIONS[ci]] &= ~dMask;
    }

    int cellConstraints(int ci) {
        return (
            (constraints[CELL_ROWS[ci]] >> (DIGITS*2)) |
            (constraints[CELL_COLS[ci]] >> (DIGITS)) |
            constraints[CELL_REGIONS[ci]]
        ) & ALL;
    }

    public static Sudoku configSeed() {
        Sudoku seed = new Sudoku();
        int[] _digitsArr = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        Shuffler.shuffle(_digitsArr);
        for (int i = 0; i < DIGITS; i++) seed.setDigit(REGION_INDICES[0][i], _digitsArr[i]);
        Shuffler.shuffle(_digitsArr);
        for (int i = 0; i < DIGITS; i++) seed.setDigit(REGION_INDICES[4][i], _digitsArr[i]);
        Shuffler.shuffle(_digitsArr);
        for (int i = 0; i < DIGITS; i++) seed.setDigit(REGION_INDICES[8][i], _digitsArr[i]);
        return seed;
    }

    public int[] getBoard() {
        int[] board = new int[SPACES];
        System.arraycopy(digits, 0, board, 0, SPACES);
        return board;
    }

    public int[] getCandidates() {
        int[] board = new int[SPACES];
        System.arraycopy(candidates, 0, board, 0, SPACES);
        return board;
    }

    //////////////////////////////////////
    ////// . . . . . . . . . . . . . . .//
    //// . . .  RESTORE POINTS . . . /////
    // . . . . . . . . . . . . . . .//////
    //////////////////////////////////////

    private static class Snapshot {
        int[] digits = new int[SPACES];
        int[] candidates = new int[SPACES];
        int[] constraints = new int[DIGITS];
        int numEmptyCells = SPACES;
        boolean isValid = true;

        Snapshot() {}
        Snapshot(Sudoku sudoku) { set(sudoku); }

        void set(Sudoku sudoku) {
            for (int i = 0; i < SPACES; i++) {
                this.digits[i] = sudoku.digits[i];
                this.candidates[i] = sudoku.candidates[i];
            }
            for (int i = 0; i < DIGITS; i++) this.constraints[i] = sudoku.constraints[i];
            this.numEmptyCells = sudoku.numEmptyCells;
            this.isValid = sudoku.isValid;
        }
    }

    /**
     * Creates a Snapshot of the current state which can be used as a restore point.
     */
    public Snapshot snapshot() {
        return new Snapshot(this);
    }

    /**
     * Copies the snapshot data into this Sudoku instance.
     * <br></br>
     * ⚠️ instance state will be overwritten.
     * @param data Snapshot to copy data from.
     */
    public void loadFromSnapshot(Snapshot data) {
        for (int i = 0; i < SPACES; i++) {
            this.digits[i] = data.digits[i];
            this.candidates[i] = data.candidates[i];
        }
        for (int i = 0; i < DIGITS; i++) this.constraints[i] = data.constraints[i];
        this.numEmptyCells = data.numEmptyCells;
        this.isValid = data.isValid;
    }

    //////////////////////////////////////
    //////////////////////////////////////

    /**
     * Resets isValid, rebuilds the constraints and candidates.
     */
    void resetCandidatesAndValidity() {
        isValid = true;
        Arrays.fill(constraints, 0);
        for (int ci = 0; ci < SPACES; ci++) {
            candidates[ci] = ALL;
            if (digits[ci] > 0) {
                candidates[ci] = ENCODER[digits[ci]];
                if ((cellConstraints(ci) & candidates[ci]) > 0) {
                    isValid = false;
                }
                addConstraint(ci, digits[ci]);
            }
        }
    }

    public boolean isFull() {
        return this.numEmptyCells == 0;
    }

    public boolean isEmpty() {
        return numEmptyCells == SPACES;
    }

    public int numEmptyCells() {
        return numEmptyCells;
    }

    public int numClues() {
        return SPACES - numEmptyCells;
    }

    public boolean isSolved() {
        for (int c : constraints) {
            if (c != FULL_CONSTRAINTS) {
                return false;
            }
        }
        return true;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void reduce() {
        for (int i = 0; i < SPACES; i++) reduceCell(i);
    }

    void reduceCell(int ci) {
        if (digits[ci] > 0) return;

        int originalCandidates = candidates[ci];
        // If candidate reduces to 0, then the board is invalid.
        candidates[ci] &= ~cellConstraints(ci);

        if (candidates[ci] <= 0) {
            isValid = false;
            setDigit(ci, 0);
            return;
        }

        // If by applying the constraints, the number of candidates is reduced to 1,
        // then the cell is solved.
        if (isDigit(candidates[ci])) {
            setDigit(ci, DECODER[candidates[ci]]);
        } else {
            int uniqueCandidate = getUniqueCandidate(ci);
            if (uniqueCandidate > 0) {
                setDigit(ci, DECODER[uniqueCandidate]);
            } else {
                // If cell[ci] is not a double, then this next part can be skipped.
                // if (isCandidatePair(reducedCandidates)) {
                //     // For each area,
                //     //    Look for candidate pairs
                //     //    If found,
                //     //      Remove the pair of digits from candidates in the area (except the pair of cells)
                //     int ciRow = CELL_ROWS[ci];
                //     int ciCol = CELL_COLS[ci];
                //     int ciRegion = CELL_REGIONS[ci];
                //     for (int col = 0; col < DIGITS; col++) {
                //         // Look at row neighbors for a potential pair with ci
                //         if (col == ciCol) continue; // Skip ci
                //         int rowNi = DIGITS*ciRow + col;
                //         if (decode(candidates[rowNi]) > 0) continue;
                //         if (candidates[rowNi] == candidates[ci]) {
                //             // Found pair (ci, gi)
                //             // TODO Maintain a collection of 'seen pairs' and if we've seen this pair, skip processing.
                //             // console.log(`Found pairs within row, value [ ${reducedCandidates.toString(2)} ] at (${ci}, ${rowNi})`);

                //             for (int ei = 0; ei < DIGITS; ei++) {
                //                 int ki = DIGITS*ciRow + ei;
                //                 if (ci == ki || rowNi == ki || digits[ki] > 0) continue;
                //                 int _before = candidates[ki];
                //                 int _after = (_before & ~reducedCandidates);
                //                 if (isDigit(_after)) {
                //                     setDigit(ki, decode(_after));
                //                     // console.log(`DIGIT resolved after reducing PAIR [${ki}] ${_before.toString(2)} -> ${decode(_after)}`);
                //                 } else {
                //                     candidates[ki] = _after;
                //                 }
                //                 if (_after < _before) {
                //                     for (int ni : CELL_NEIGHBORS[ki]) {
                //                         if (DECODER[candidates[ni]] == 0) reduceCell(ni);
                //                     }
                //                 }
                //             }
                //         }
                //     }
                // }
            }
        }

        if (candidates[ci] < originalCandidates) {
            for (int n : CELL_NEIGHBORS[ci]) {
                if (digits[n] == 0) {
                    reduceCell(n);
                }
            }
        }
    }

    private void constraintProp() {
        for (int i = 0; i < SPACES; i++) {
            _constraintProp(i);
        }
    }

    private void _constraintProp(int ci) {
        if (digits[ci] > 0) {
            return;
        }

        int originalCandidates = candidates[ci];
        // If candidate constraints reduces to 0, then the board is likely invalid.
        candidates[ci] &= ~cellConstraints(ci);
        if (candidates[ci] <= 0) {
            isValid = false;
            setDigit(ci, 0);
            return;
        }

        // If by applying the constraints, the number of candidates is reduced to 1,
        // then the cell is solved.
        if (isDigit(candidates[ci])) {
            setDigit(ci, DECODER[candidates[ci]]);
        }

        if (candidates[ci] < originalCandidates) {
            for (int n : CELL_NEIGHBORS[ci]) {
                if (digits[n] == 0) {
                    _constraintProp(n);
                }
            }
        }
    }

    int getUniqueCandidate(int ci) {
        for (int candidate : CANDIDATES[candidates[ci]]) {
            boolean unique = true;
            for (int ni : ROW_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) return candidate;

            unique = true;
            for (int ni : COL_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) return candidate;

            unique = true;
            for (int ni : REGION_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) return candidate;
        }

        return 0;
    }

    /**
     * Generates a puzzle with the given number of clues.
     * If numClues is less than the minimum 17, returns null.
     * Generally not recommended to attempt puzzle generation with less than 20 clues.
     * @return A new Sudoku instance (the puzzle).
     */
    public static Sudoku generatePuzzle(int numClues) {
        if (numClues < MIN_CLUES) return null;
        Sudoku grid = configSeed().firstSolution();
        if (numClues >= SPACES) return grid;
        return generatePuzzle(grid, numClues, null, 0, 0L, true);
    }

    /**
     * Generates a puzzle.
     * If numClues is less than the minimum 17, returns null.
     * @param grid (Optional) The solution. If provided, must be full and valid.
     * @param numClues Number of clues.
     * @param sieve A list of SudokuMask to use as a sieve of unavoidable sets.
     * @param difficulty From 0 to 4.
     * @param timeoutMs Amount of system time(ms) to spend generating. 0 for no limit.
     * @param useSieve Whether a sieve may be seeded progressively at certain points.
     * @return A new Sudoku instance (the puzzle); or null if the time limit is exceeded.
     * @throws IllegalArgumentException If a populated sieve is given without a grid;
     * if a grid is given but is invalid or not full;
     * if difficulty is out of range.
     */
    public static Sudoku generatePuzzle(
        Sudoku grid,
        int numClues,
        List<SudokuMask> sieve,
        int difficulty,
        long timeoutMs,
        boolean useSieve
    ) {
        if (numClues < MIN_CLUES)
            return null;
        if (sieve != null && !sieve.isEmpty() && grid == null)
            throw new IllegalArgumentException("Sieve provided without grid");
        if (grid == null)
            grid = configSeed().firstSolution();
        if (!grid.isSolved())
            throw new IllegalArgumentException("Solution grid is invalid");
        if (numClues >= SPACES)
            return grid;
        if (difficulty < 0 || difficulty > 4)
            throw new IllegalArgumentException(String.format("Invalid difficulty (%d); expected 0 <= difficulty <= 4", difficulty));
        if (sieve == null)
            sieve = new ArrayList<>();

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        long start = System.currentTimeMillis();
        // const FULLMASK = (1n << BigInt(SPACES)) - 1n;
        // SudokuMask FULLMASK = SudokuMask.full();
        int maskFails = 0;
        int puzzleCheckFails = 0;
        int putBacks = 0;
        SudokuMask mask = SudokuMask.full();
        List<Integer> remaining = Shuffler.range(SPACES);
        ArrayList<Integer> removed = new ArrayList<>();

        while (remaining.size() > numClues) {
            int startChoices = remaining.size();
            Shuffler.shuffle(remaining);
            for (int i = 0; i < remaining.size() && remaining.size() > numClues; i++) {
                int choice = remaining.get(i);
                // mask &= ~cellMask(choice);
                mask.unsetBit(choice);

                // Check if mask satisfies sieve
                boolean satisfies = true;
                for (SudokuMask item : sieve) {
                    if (!mask.intersects(item)) {
                        satisfies = false;
                        break;
                    }
                }

                // If not, or if there are multiple solutions,
                // put the cell back and try the next
                if (!satisfies) {
                    maskFails++;
                    // mask |= cellMask(choice);
                    mask.setBit(choice);

                    // Once in awhile, check the time
                    if (timeoutMs > 0L && (maskFails % 100) == 0) {
                        if ((System.currentTimeMillis() - start) > timeoutMs) {
                            return null;
                        }
                    }

                    continue;
                }

                if (grid.filter(mask).solutionsFlag() != 1) {
                    puzzleCheckFails++;
                    if (useSieve && puzzleCheckFails == 100 && sieve.size() < 36) {
                        SudokuSieve.seedSieve(grid, sieve, 2);
                    } else if (useSieve && puzzleCheckFails == 2500 && sieve.size() < 200) {
                        SudokuSieve.seedSieve(grid, sieve, 3);
                    } else if (useSieve && puzzleCheckFails > 10000 && sieve.size() < 1000) {
                        SudokuSieve.searchForItemsFromMask(grid, sieve, mask, false);
                    } else if (useSieve && puzzleCheckFails > 25000) {
                        // SudokuSieve.searchForItemsFromMask(grid, sieve, mask, false);
                    }

                    mask.setBit(choice);
                    continue;
                }

                removed.add(choice);
                remaining.remove(i);
                i--;
            }

            // If no cells were chosen
            // - Put some cells back and try again
            if (
            (
                remaining.size() == numClues &&
                difficulty > 0 &&
                grid.filter(mask).solutionsFlag() == 1 //&&
                // grid.filter(mask).difficulty() != difficulty
            ) || remaining.size() == startChoices
            ) {
                int numToPutBack = 1 + (putBacks % 4) + rand.nextInt(1 + (putBacks % 8));
                Shuffler.shuffle(removed);
                for (int i = 0; i < numToPutBack; i++) {
                    int cell = removed.remove(removed.size() - 1);
                    remaining.add(cell);
                    mask.setBit(cell);
                    if (removed.size() == 0)
                        break;
                }
                putBacks++;
            }
        }

        return grid.filter(mask);
    }

    static class SudokuNode {
        // int[] digits;
        // int[] candidates;
        // int[] constraints;
        Sudoku sudoku;
        int index = -1;
        int values = -1;
        ArrayList<SudokuNode> nexts;
        SudokuNode(Sudoku sudoku) {
            this.sudoku = sudoku;
            sudoku.reduce();
            index = sudoku.pickEmptyCell();
            if (index != -1) {
                values = sudoku.candidates[index];
            }
        }
        // void generateNextsIfNull() {
        //     if (nexts == null) {
        //         nexts = new ArrayList<>();
        //         sudoku.getNextsAdditive(n -> nexts.add(new SudokuNode(n)));
        //         Shuffler.shuffle(nexts);
        //     }
        // }
        SudokuNode next() {
            // If this node's sudoku had no emptycells, then `index` and `values` would have never been set, and both would still be -1
            if (values <= 0 || !sudoku.isValid) {
                return null;
            }

            Sudoku s = new Sudoku(sudoku);
            int[] candidateDigits = CANDIDATES_ARR[values];
            int randomCandidateDigit = candidateDigits[ThreadLocalRandom.current().nextInt(candidateDigits.length)];
            s.setDigit(index, randomCandidateDigit);
            values &= ~(ENCODER[randomCandidateDigit]);
            return new SudokuNode(s);
            // generateNextsIfNull();
            // return (nexts.isEmpty()) ? null : nexts.remove(nexts.size() - 1);
        }
        boolean hasNext() {
            return (values > 0 && sudoku.isValid) ? true : false;
        }
    }

    public void searchForSolutions3(Function<Sudoku,Boolean> solutionFoundCallback) {
        Sudoku root = new Sudoku(this);
        root.resetEmptyCells();
        root.resetConstraints();

        if (!root.isValid) return;

        Stack<SudokuNode> stack = new Stack<>();
        stack.push(new SudokuNode(root));

        while (!stack.isEmpty()) {
            SudokuNode top = stack.peek();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                stack.pop();
                if (solutionFoundCallback.apply(sudoku)) {
                    continue;
                } else {
                    break;
                }
            }

            SudokuNode next = top.next();

            if (next == null) {
                stack.pop();
            } else {
                stack.push(next);
            }
        }
    }

    /**
     * Counts the puzzle's solution. (Synchronous DFS.)
     * This may take a very long time if the puzzle is sparse.
     * @return Number of solutions.
     */
    public long countSolutions() {
        Sudoku root = new Sudoku(this);
        root.resetCandidatesAndValidity();

        if (!root.isValid) return 0;

        long count = 0L;
        Stack<SudokuNode> stack = new Stack<>();
        stack.push(new SudokuNode(root));

        while (!stack.isEmpty()) {
            SudokuNode top = stack.peek();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                stack.pop();
                count++;
            } else if (top.hasNext()) {
                stack.push(top.next());
            } else {
                stack.pop();
            }
        }

        return count;
    }

    public List<Future<List<Sudoku>>> searchForSolutions4() {
        List<Future<List<Sudoku>>> allResults = new ArrayList<>();

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetCandidatesAndValidity();

        Queue<SudokuNode> q = new LinkedList<>();
        q.offer(new SudokuNode(root));

        List<Sudoku> preSolved = new ArrayList<>();
        // int maxSplitSize = 16;
        int maxSplitSize = 1 << 16;
        while (!q.isEmpty() && q.size() < maxSplitSize) {
            SudokuNode top = q.peek();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                q.poll();
                preSolved.add(sudoku);
                continue;
            }

            SudokuNode next = top.next();

            if (next == null) {
                q.poll();
            } else {
                q.offer(next);
            }
        }

        if (!preSolved.isEmpty()) {
            allResults.add(ThreadPool.submit(() -> preSolved));
        }

        for (SudokuNode node : q) {
            List<Sudoku> results = new ArrayList<>();
            allResults.add(ThreadPool.submit(() -> {
                node.sudoku.searchForSolutions3(solution -> {
                    results.add(solution);
                    return true;
                });
                return results;
            }));
        }

        return allResults;
    }

    /**
     * Finds all solutions to this sudoku, using the given number of threads. This method blocks
     * until all solutions are found, or until the specified amount of time has elapsed,
     * at which point the TimeOutException will be thrown. Timeout will default to 1 hour if not positive.
     *
     * The given callback will be invoked periodically by each thread with a list of
     * at most <code>batchSize</code> solutions as they are accumulated.
     * @param solutionBatchCallback
     * @param batchSize
     * @param numThreads
     * @param timeout
     * @param timeoutUnit
     * @return True if all solutions were found; otherwise false (due to timeout or interruption).
     */
    public boolean searchForSolutionsAsync(
        Consumer<List<Sudoku>> solutionBatchCallback,
        int batchSize,
        int numThreads,
        long timeout,
        TimeUnit timeoutUnit
    ) {
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be positive");
        if (numThreads < 1) throw new IllegalArgumentException("numThreads must be positive");
        if (timeout < 0L) timeout = 1L;

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetCandidatesAndValidity();

        Queue<SudokuNode> q = new LinkedList<>();
        q.offer(new SudokuNode(root));

        List<Sudoku> solvedBeforeSplit = new ArrayList<>();
        final int MAX_QUEUE_SIZE = (1 << 10);
        while (!q.isEmpty() && q.size() < MAX_QUEUE_SIZE) {
            SudokuNode top = q.poll();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                solvedBeforeSplit.add(sudoku);
                if (solvedBeforeSplit.size() == batchSize) {
                    solutionBatchCallback.accept(new ArrayList<>(solvedBeforeSplit));
                    solvedBeforeSplit.clear();
                }
                continue;
            }

            SudokuNode next;
            while ((next = top.next()) != null) {
                q.offer(next);
            }
        }

        if (!solvedBeforeSplit.isEmpty()) {
            solutionBatchCallback.accept(solvedBeforeSplit);
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            numThreads,
            numThreads,
            1L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );

        final int BATCH_SIZE = batchSize;
        for (SudokuNode node : q) {
            pool.submit(() -> {
                List<Sudoku> resultBatch = new ArrayList<>();
                node.sudoku.searchForSolutions3(solution -> {
                    resultBatch.add(solution);
                    if (resultBatch.size() == BATCH_SIZE) {
                        solutionBatchCallback.accept(new ArrayList<>(resultBatch));
                        resultBatch.clear();
                    }
                    return true;
                });
                solutionBatchCallback.accept(resultBatch);
            });
        }

        pool.shutdown();
        try {
            boolean success = (timeout > 0L) ?
                pool.awaitTermination(timeout, timeoutUnit) :
                pool.awaitTermination(1L, TimeUnit.HOURS);
            if (!success) {
                pool.shutdownNow();
            }
            return success;
		} catch (InterruptedException e) {
			e.printStackTrace();
            pool.shutdownNow();
            return false;
		}
    }

    public static class SolutionCountResult {
        private ThreadPoolExecutor pool;
        private long timeout;
        private TimeUnit timeoutUnit;
        private List<Future<Boolean>> tasks;

        private AtomicLong longCount;
        private SolutionCountResult(long timeout, TimeUnit timeoutUnit) {
            this.longCount = new AtomicLong();
            this.timeoutUnit = (timeout <= 0L) ? TimeUnit.HOURS : timeoutUnit;
            this.timeout = (timeout <= 0L) ? 1L : timeout;
            this.tasks = new ArrayList<>();
        }

        /** Gets the current count.*/
        public long get() {
            return longCount.get();
        }

        /**
         * Attempts to get the count as an integer.
         * @return Solution count as an integer; or -1 if count is larger than <code>Integer.MAX_VALUE</code>.
         */
        public int getInt() {
            if (get() <= (long)Integer.MAX_VALUE) {
                return (int)get();
            } else {
                return -1;
            }
        }

        /** Gets whether the count has completed.*/
        public boolean isDone() {
            return pool == null || pool.isTerminated();
        }

        /** Gets whether the count was successful, i.e. it didn't time out or get interrupted.*/
        public boolean wasSuccessful() {
            return isDone() && removeCompletedTasks();
        }

        public void interrupt() {
            if (pool != null) pool.shutdownNow();
        }

        public boolean await() throws InterruptedException {
            if (pool == null) return true;
            if (isDone()) return removeCompletedTasks();
            return pool.awaitTermination(timeout, timeoutUnit);
        }

        private void submitAndShutdown(Collection<SudokuNode> nodes, int numThreads) {
            if (pool != null) return;

            this.pool = new ThreadPoolExecutor(
                numThreads,
                numThreads,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
            );

            for (SudokuNode n : nodes) {
                tasks.add(pool.submit(() -> {
                    n.sudoku.searchForSolutions3(solution -> {
                        longCount.incrementAndGet();
                        return true;
                    });
                    return true;
                }));
            }

            pool.shutdown();
        }

        private boolean removeCompletedTasks() {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                Future<Boolean> task = tasks.get(i);
                if (task.isDone()) {
                    try {
						if (task.get()) {
						    tasks.remove(i);
						}
					} catch (InterruptedException | ExecutionException e) {
                        return false;
					}
                }
            }
            return true;
        }
    }

    static ThreadPoolExecutor pool = new ThreadPoolExecutor(
        1, 8,
        10L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>()
    );

    public void searchForSolutionsAsync(
        Consumer<Sudoku> solutionsCallbackAsync,
        int maxThreads
    ) {
        Sudoku root = new Sudoku(this);
        if (root.isSolved()) {
            solutionsCallbackAsync.accept(root);
            return;
        }
        // Ensure candidates and constraints are in good order for the search
        root.resetCandidatesAndValidity();
        root.reduce();
        if (root.isSolved()) {
            solutionsCallbackAsync.accept(root);
            return;
        }

        // ThreadPoolExecutor pool = new ThreadPoolExecutor(
        //     1, maxThreads,
        //     10L, TimeUnit.MILLISECONDS,
        //     new LinkedBlockingQueue<>()
        // );

        AtomicInteger threadsActive = new AtomicInteger(1);
        pool.submit(() -> {
            searchForSolutionsAsyncWorker(new SudokuNode(root), solutionsCallbackAsync, threadsActive);
        });

        while (threadsActive.get() > 0) {
            try {
                // System.out.println("waiting on " + threadsActive.get() + " threads");
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(1L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void searchForSolutionsAsyncWorker(
        SudokuNode node,
        Consumer<Sudoku> solutionsCallbackAsync,
        AtomicInteger threadsActive
    ) {
        long startTime = System.currentTimeMillis();

        ArrayList<SudokuNode> stack = new ArrayList<>();
        stack.add(node);
        while (!stack.isEmpty()) {
            long curTime = System.currentTimeMillis();
            long timeSinceStart = curTime - startTime;
            if (!pool.isShutdown() && timeSinceStart > 25L && stack.size() > 3) {
                startTime = curTime;

                // Fast-forward in case all the nexts have been used on the lower end of the stack
                SudokuNode firstNode; do {
                    firstNode = stack.remove(0);
                } while(!firstNode.hasNext() && stack.size() > 3);

                if (firstNode.hasNext()) {
                    while (firstNode.hasNext()) {
                        SudokuNode next = firstNode.next();
                        threadsActive.incrementAndGet();
                        pool.submit(() -> {
                            searchForSolutionsAsyncWorker(next, solutionsCallbackAsync, threadsActive);
                        });
                    }
                }
                // At this point, there is at least 3 items left in the stack,
                // therefore safe to stack.peek() below.
            }

            SudokuNode top = stack.get(stack.size() - 1); // top = peek
            if (top.sudoku.isSolved()) {
                solutionsCallbackAsync.accept(top.sudoku);
            }

            // If necessary, rewind top until a node with nexts is found
            while (!stack.isEmpty() && !(top = stack.get(stack.size() - 1)).hasNext()) {
                stack.remove(stack.size() - 1); // pop
            }

            if (top.hasNext()) {
                stack.add(top.next());
            } // else stack is empty and the while loop will end
        }

        threadsActive.decrementAndGet();
    }



    /**
     * Counts the number of solutions to this sudoku with the given number of threads,
     * up to the given amount of time. Timeout will default to 1 hour if not positive.
     * @param numThreads
     * @param timeout
     * @param timeoutUnit
     * @return
     */
    public SolutionCountResult countSolutionsAsync(int numThreads, long timeout, TimeUnit timeoutUnit) {
        SolutionCountResult result = new SolutionCountResult(timeout, timeoutUnit);

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();

        Queue<SudokuNode> q = new LinkedList<>();
        q.offer(new SudokuNode(root));

        int maxSplitSize = (1 << 10);
        while (!q.isEmpty() && q.size() < maxSplitSize) {
            SudokuNode top = q.poll();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                result.longCount.incrementAndGet();
                continue;
            }

            SudokuNode next;
            while ((next = top.next()) != null) {
                q.offer(next);
            }
        }

        if (!q.isEmpty()) {
            result.submitAndShutdown(q, numThreads);
        }

        return result;
    }

    /**
     * Counts the number of solutions to this sudoku with the given number of threads.
     * Even with multiple threads, a very sparse puzzle may take a long time.
     */
    public long countSolutionsAsync(int numThreads) {
        AtomicLong count = new AtomicLong();

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetCandidatesAndValidity();

        int maxSplitSize = (1 << 12);
        Queue<SudokuNode> queue = new LinkedList<>();
        queue.offer(new SudokuNode(root));
        while (!queue.isEmpty() && queue.size() < maxSplitSize) {
            SudokuNode top = queue.poll();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                count.incrementAndGet();
            } else {
                while (top.hasNext()) {
                    queue.offer(top.next());
                }
            }
        }

        if (queue.isEmpty()) {
            return count.get();
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            numThreads, numThreads,
            1L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );

        while (!queue.isEmpty()) {
            SudokuNode node = queue.poll();
            pool.submit(() -> {
                long localCount = node.sudoku.countSolutions();
                count.addAndGet(localCount);
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(1L, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return count.get();
    }


    /**
     * Searches for and returns the first solution.
     * @return A new Sudoku instance (the solution).
     */
    public Sudoku firstSolution() {
        AtomicReference<Sudoku> result = new AtomicReference<>();
        searchForSolutions3(solution -> {
            result.set(solution);
            return false;
        });
        return result.get();
    }

    /**
     * Checks whether all branches of this puzzle solve uniquely.
     * Branches are created by filling in each cell with each of its possible candidates.
     * TODO Not 100% confident this is working as intended.
     * @return True if all branches of this puzzle are solvable with a unique solution.
     */
    public boolean allBranchesSolveUniquely() {
        for (int ci = 0; ci < SPACES; ci++) {
            int originalVal = candidates[ci];
            if (originalVal == 0) return false;
            if (digits[ci] == 0) {
                for (int candidateDigit : CANDIDATES_ARR[originalVal]) {
                    setDigit(ci, candidateDigit); // mutates constraints
                    int flag = solutionsFlag();
                    setDigit(ci, 0); // undo the constraints mutation
                    candidates[ci] = originalVal;
                    if (flag != 1) return false;
                }
            }
        }
        return true;
    }

    /**
     * Generates a copy of this sudoku with the specified digits removed.
     * @param digitsMask A 9-bit mask representing the combination of which digits to remove, where
     * the least significant bit represents the digit '1'.
     */
    public void removeDigits(int digitsMask) {
        for (int ci = 0; ci < SPACES; ci++) {
            if ((candidates[ci] & digitsMask) > 0) {
                setDigit(ci, 0);
            }
        }
    }

    /**
     * @return A SudokuMask where bits are set for each digit on the board.
     */
    public SudokuMask getMask() {
        if (isFull()) return SudokuMask.full();
        if (isEmpty()) return new SudokuMask();

        SudokuMask result = new SudokuMask();
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] > 0) {
                result.setBit(ci);
            }
        }
        return result;
    }

    /**
     * Gets a SudokuMask where bits are set for each digit in digitsMask on the board.
     * @param digitsMask Bitmask representing multiple digits. Same as the candidate values.
     * @return A new SudokuMask with bits set for select digits on the board.
     */
    public SudokuMask maskForDigits(int digitsMask) {
        SudokuMask result = new SudokuMask();
        for (int ci = 0; ci < SPACES; ci++) {
            if ((candidates[ci] & digitsMask) > 0) {
                result = result.setBit(ci);
            }
        }
        return result;
    }

    /**
     * Generates a code for the solved sudoku.
     * The code will be the same regardless of how the board is manipulated by
     * symmetry-preserving transforms (rotating, mirroring, digit-swapping).
     * This Sudoku must be solved before generating, or an exception will be thrown.
     * The level indicates granularity. Higher levels involve exponentially more calculation.
     * @param level From 2-4, Not recommended higher than 4.
     * @return A 'fingerprint' code.
     */
    public String fingerprint(int level) {
        if (level < 2 || level > 8) {
            throw new IllegalArgumentException("sudoku fingerprint level (f) must be 2 <= f <= 8");
        }

        if (!SudokuUtility.isSolved(getBoard())) {
            throw new IllegalArgumentException("cannot compute fingerprint: sudoku grid must be full");
        }

        SudokuSieve sieve = new SudokuSieve(getBoard());
        sieve.seed(level);

        // Track the maximum number of cells used by any unavoidable set
        // int minNumCells = SPACES;
        int maxNumCells = 0;
        int[] itemCountByNumCells = new int[SPACES];
        for (SudokuMask ua : sieve.items(new ArrayList<>())) {
            int numCells = ua.bitCount();
            itemCountByNumCells[numCells]++;
            // if (numCells < minNumCells) minNumCells = numCells;
            if (numCells > maxNumCells) maxNumCells = numCells;
        }

        ArrayList<String> itemsList = new ArrayList<>();
        // An item (unavoidable set) includes a minimum of 4 cells
        for (int numCells = 4; numCells <= maxNumCells; numCells++) {
            // In level 2, there can be no UAs using an odd number of cells,
            // because each cell must have at least one complement.
            // Skipping odd numbers avoids "::", keeping the fingerprint short.
            if (level == 2 && (numCells % 2) > 0) {
                continue;
            }

            int count = itemCountByNumCells[numCells];
            itemsList.add((count > 0) ? Integer.toString(count, 16) : "");
        }

        return String.join(":", itemsList);
    }

    /***********************************************
     *
     * Transformations
     *
     * The following transformations are symmetry-preserving,
     * i.e. they do not change the number of solutions.
     * Transformed grids will retain the same fingerprint.
     *
     * After any transformation, 'constraints' may be out of sync
     * and require reseting via 'resetConstraints()'.
     ***********************************************/

    /**
     * Swaps all of digit 'a' on the board with digit 'b'.
     * Does nothing if digits are the same, either are negative,
     * or either are a number greater than 9.
     * @param a 1 through 9
     * @param b 1 through 9
     */
    public void swapDigits(int a, int b) {
		if (a == b) return;
        if (a <= 0 || a > 9 || b <= 0 || b > 9) return;
        for (int i = 0; i < SPACES; i++) {
            int d = digits[i];
            if (d == a) {
                setDigit(i, b);
            } else if (d == b) {
                setDigit(i, a);
            }
        }
	}

    /**
     * Rearranges the board digits so the top row is sequential.
     * Empty top row cells are skipped.
     * @return This sudoku instance for convenience.
     */
    public Sudoku normalize() {
        for (int d = 1; d <= DIGITS; d++) {
			int cellDigit = digits[d - 1];
			if (cellDigit > 0 && cellDigit != d) {
				swapDigits(cellDigit, d);
			}
		}
        return this;
    }

    /**
     * Rotates the board clockwise the given number of turns, up to 3.
     * @param turns Number of times to rotate the board.
     * @return This sudoku instance for convenience.
     */
    public Sudoku rotate(int turns) {
        for (int t = 0; t < turns; t++) {
            rotate90(candidates, DIGITS);
            rotate90(digits, DIGITS);
        }
        return this;
    }

    /**
     * Reflects the board values over the horizontal.
     * @return This sudoku instance for convenience.
     */
    public Sudoku reflectHorizontal() {
        reflectOverHorizontal(candidates, DIGITS);
        reflectOverHorizontal(digits, DIGITS);
        return this;
    }

    /**
     * Reflects the board values over the vertical.
     * @return This sudoku instance for convenience.
     */
    public Sudoku reflectVertical() {
        reflectOverVertical(candidates, DIGITS);
        reflectOverVertical(digits, DIGITS);
        return this;
    }

    /**
     * Reflects the board values over the diagonal.
     * @return This sudoku instance for convenience.
     */
    public Sudoku reflectDiagonal() {
        reflectOverDiagonal(candidates, DIGITS);
        reflectOverDiagonal(digits, DIGITS);
        return this;
    }

    /**
     * Reflects the board values over the antidiagonal.
     * @return This sudoku instance for convenience.
     */
    public Sudoku reflectAntiDiagonal() {
		reflectOverAntiDiagonal(candidates, DIGITS);
        reflectOverAntiDiagonal(digits, DIGITS);
        return this;
    }

    /**
     * Swaps the given bands. Bands are groups of 3 regions, horizontally.
     * @param b1 Band index (0, 1, or 2)
     * @param b2 Band index (0, 1, or 2) Different than b1
     * @return This sudoku instance for convenience.
     */
    public Sudoku swapBands(int b1, int b2) {
        swapBands(candidates, b1, b2);
        swapBands(digits, b1, b2);
        return this;
    }

    /**
     * Swaps the given rows within a band.
     * @param b1 Band index (0, 1, or 2)
     * @param ri1 Row index (0, 1, or 2)
     * @param ri2 Row index (0, 1, or 2) Different than ri1
     * @return This sudoku instance for convenience.
     */
    public Sudoku swapBandRows(int bi, int ri1, int ri2) {
        swapBandRows(candidates, bi, ri1, ri2);
        swapBandRows(digits, bi, ri1, ri2);
        return this;
    }

    /**
     * Swaps the given stacks. Stacks are groups of 3 regions, vertically.
     * @param s1 Stack index (0, 1, or 2)
     * @param s2 Stack index (0, 1, or 2) Different than s1
     * @return This sudoku instance for convenience.
     */
    public Sudoku swapStacks(int s1, int s2) {
        swapStacks(candidates, s1, s2);
        swapStacks(digits, s1, s2);
        return this;
    }

    /**
     * Swaps the given columns within a stack.
     * @param b1 Stack index (0, 1, or 2)
     * @param ri1 Column index (0, 1, or 2)
     * @param ri2 Column index (0, 1, or 2) Different than si1
     * @return This sudoku instance for convenience.
     */
    public Sudoku swapStackCols(int si, int ci1, int ci2) {
        swapStackCols(candidates, si, ci1, ci2);
        swapStackCols(digits, si, ci1, ci2);
        return this;
    }

    public Sudoku scramble() {
        rotate(ThreadLocalRandom.current().nextInt(4));

        int[] digits = new int[9];
        for (int d = 1; d <= 9; d++)
            digits[d - 1] = d;
        Shuffler.shuffle(digits);
        for (int d = 1; d <= 9; d++)
            swapDigits(d, digits[d - 1]);

        List<Runnable> transforms = new ArrayList<>(){{
            add(() -> swapBands(0, 1));
            add(() -> swapBands(0, 2));
            add(() -> swapBands(1, 2));
            add(() -> swapBandRows(0, 0, 1));
            add(() -> swapBandRows(0, 0, 2));
            add(() -> swapBandRows(0, 1, 2));
            add(() -> swapBandRows(1, 0, 1));
            add(() -> swapBandRows(1, 0, 2));
            add(() -> swapBandRows(1, 1, 2));
            add(() -> swapBandRows(2, 0, 1));
            add(() -> swapBandRows(2, 0, 2));
            add(() -> swapBandRows(2, 1, 2));
            add(() -> swapStacks(0, 1));
            add(() -> swapStacks(0, 2));
            add(() -> swapStacks(1, 2));
            add(() -> swapStackCols(0, 0, 1));
            add(() -> swapStackCols(0, 0, 2));
            add(() -> swapStackCols(0, 1, 2));
            add(() -> swapStackCols(1, 0, 1));
            add(() -> swapStackCols(1, 0, 2));
            add(() -> swapStackCols(1, 1, 2));
            add(() -> swapStackCols(2, 0, 1));
            add(() -> swapStackCols(2, 0, 2));
            add(() -> swapStackCols(2, 1, 2));
            add(() -> reflectHorizontal());
            add(() -> reflectVertical());
            add(() -> reflectDiagonal());
            add(() -> reflectAntiDiagonal());
        }};
        for (int t = 0; t < 137; t++)
            transforms.get(ThreadLocalRandom.current().nextInt(28)).run();

        return this;
    }

    // End transformations

    /**
     * Filters this sudoku grid with the given mask.
     * @param mask A mask indicating which digits to keep in the result.
     * @return A new Sudoku instance with filtered board values.
     */
    public Sudoku filter(SudokuMask mask) {
        // Throw if this is not full grid
        Sudoku result = new Sudoku();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(ci)) {
                result.setDigit(ci, digits[ci]);
            }
        }
        return result;
    }

    /**
     * Gets a string representation of this board filtered through the given mask.
     * @param mask A mask indicating which digits to keep in the result.
     * @return A new Sudoku instance with filtered board values.
     */
    public String filterStr(SudokuMask mask) {
        StringBuilder strb = new StringBuilder();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(ci)) {
                strb.append(digits[ci]);
            } else {
                strb.append('.');
            }
        }
        return strb.toString();
    }

    /**
     * Gets a mask indicating differences between this sudoku board and the one given.
     * @param other Another sudoku board to compare to.
     * @return A new SudokuMask where 1s indicate a difference between boards.
     */
    public SudokuMask diff2(Sudoku other) {
        return diff2(other.digits);
    }

    /**
     * Gets a mask indicating differences between this sudoku board and the one given.
     * @param otherBoardDigits Another sudoku board to compare to.
     * @return A new SudokuMask where 1s indicate a difference between boards.
     */
    public SudokuMask diff2(int[] otherBoardDigits) {
        SudokuMask result = new SudokuMask();
        for (int i = 0; i < SPACES; i++) {
            if (digits[i] != otherBoardDigits[i]) {
               result.setBit(i);
            }
        }
        return result;
    }

    /**
     * Gets a flag indicating information about the sudoku's number of solutions.
     * <ul>
     * <li>0 -> No solutions</li>
     * <li>1 -> Single solution</li>
     * <li>2 -> Multiple solutions</li>
     * </ul>
     */
    public int solutionsFlag() {
        if (!isValid) return 0;
        if (numEmptyCells > SPACES - MIN_CLUES) return 2;

        AtomicInteger count = new AtomicInteger();
        searchForSolutions3(_s -> (count.incrementAndGet() < 2));
        return count.get();
    }

    /**
     * Finds and returns the index of an empty cell, or -1 if no empty cells exist.
     * Prioritizes empty cells with the fewest number of candidates. If multiple cells
     * have the fewest number of candidates, chooses one of them at random.
     * @return Index of an empty cell, or -1 if no empty cells exist.
     */
    int pickEmptyCell() {
        return pickEmptyCell(0, SPACES);
    }

    /**
     * Finds and returns the index of an empty cell within a given cell range,
     * or -1 if no empty cells exist.
     * Prioritizes empty cells with the fewest number of candidates. If multiple cells
     * have the fewest number of candidates, chooses one of them at random.
     * @param startIndex Starting cell index of the range to check (inclusive).
     * @param endIndex Ending cell index of the range to check (exclusive).
     * @return Index of an empty cell, or -1 if no empty cells exist.
     */
    // Hoisting this list up here actually runs slightly slower...
    // private List<Integer> _minimums = new ArrayList<>();
    public int pickEmptyCell(int startIndex, int endIndex) {
        if (numEmptyCells == 0) {
            return  -1;
        }

        int min = DIGITS + 1;
        List<Integer> _minimums = new ArrayList<>();
        for (int ci = startIndex; ci < endIndex; ci++) {
            if (digits[ci] == 0) {
                int numCandidates = BIT_COUNT_MAP[candidates[ci]];
                // This actually seems to run slightly slower...
                // if (numCandidates == 2) {
                //     return ci;
                // }
                if (numCandidates < min) {
                    min = numCandidates;
                    _minimums.clear();
                    _minimums.add(ci);
                } else if (numCandidates == min) {
                    _minimums.add(ci);
                }
            }
        }

        return (!_minimums.isEmpty()) ? _minimums.get(ThreadLocalRandom.current().nextInt(_minimums.size())) : -1;
        // return _minimums.get(RandomGenerator.getDefault().nextInt(_minimums.size()));
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        for (int d : this.digits) {
            strb.append((d > 0) ? (char)('0' + d) : '.');
        }
        return strb.toString();
    }

    /**
     * @return A multi-line string representation of the puzzle.
     */
    public String toFullString() {
        return toFullString(digits);
    }

    /**
     * @return A multi-line string representation of the puzzle.
     */
    public String toMedString() {
        return toMedString(digits);
    }

    /**
     * Export this sudoku digits as 41 bytes.
     * For use with <code>new Sudoku(bytesArr)</code>.
     * @return A byte array containing this sudoku's digit information.
     */
    public byte[] toBytes() {
        if (numEmptyCells == SPACES) return new byte[41];

        int len = 41;
        byte[] result = new byte[len];
        for (int i = 0; i < len - 1; i++) {
            result[i] = (byte)( ((digits[i*2] & 0xf) << 4) + (digits[i*2 + 1] & 0xf) );
        }
        result[40] = (byte)( ((digits[80] & 0xf) << 4) + 0xf );

        return result;
    }
}
