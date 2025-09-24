package com.metal_pony.bucket.sudoku;

public class SudokuUtility {
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

	public static enum Area {
		ROW(0), COL(1), REGION(2);
		private final int index;
		private Area(int index) {
			this.index = index;
		}
	};
	static final int[][][] INDICES = new int[Sudoku.RANK][Sudoku.DIGITS][Sudoku.DIGITS];
	static {
		for (int areaIndex = 0; areaIndex < Sudoku.DIGITS; areaIndex++) {
			for (int areaCellIndex = 0; areaCellIndex < Sudoku.DIGITS; areaCellIndex++) {
				INDICES[Area.ROW.index][areaIndex][areaCellIndex] = cellRow(areaCellIndex);
				INDICES[Area.COL.index][areaIndex][areaCellIndex] = cellCol(areaCellIndex);
				INDICES[Area.REGION.index][areaIndex][areaCellIndex] = cellRegion(areaCellIndex);
			}
		}
	}
	public static int[] indicesFor(Area area, int areaIndex) {
		return indicesFor(area.index, areaIndex);
	}
	public static int[] indicesFor(int area, int areaIndex) {
		int[] result = new int[Sudoku.DIGITS];
		System.arraycopy(INDICES[area][areaIndex], 0, result, 0, Sudoku.DIGITS);
		return result;
	}

	public static int cellRow(int ci) { return ci / Sudoku.DIGITS; }
	public static int cellCol(int ci) { return ci % Sudoku.DIGITS; }
	public static int cellRegion(int ci) {
		int regionRow = ci / (Sudoku.RANK * Sudoku.DIGITS);
		int regionCol = (ci % Sudoku.DIGITS) / Sudoku.RANK;
		return (regionRow * Sudoku.RANK) + regionCol;
	}

	private static void checkBoardArrayLength(int[] board) {
		if (board.length < Sudoku.SPACES) {
			throw new IllegalArgumentException("incorrect sudoku board array length");
		}
	}

	private static boolean isAreaValid(int[] board, int[] areaIndices) {
		int digitsSeen = 0;
		for (int i = 0; i < Sudoku.DIGITS; i++) {
			int digit = board[areaIndices[i]];
			if (digit > 0) {
				int digitMask = 1 << (digit - 1);
				if ((digitMask & digitsSeen) > 0) {
					return false;
				}
				digitsSeen |= digitMask;
			}
		}
		return true;
	}

	private static boolean isAreaFull(int[] board, int[] areaIndices) {
		for (int i = 0; i < Sudoku.DIGITS; i++) {
			if (board[areaIndices[i]] == 0) {
				return false;
			}
		}
		return true;
	}

	public static boolean isRowValid(int[] board, int rowIndex) {
		return isAreaValid(board, INDICES[Area.ROW.index][rowIndex]);
	}

	public static boolean isColValid(int[] board, int colIndex) {
		return isAreaValid(board, INDICES[Area.COL.index][colIndex]);
	}

	public static boolean isRegionValid(int[] board, int regionIndex) {
		return isAreaValid(board, INDICES[Area.REGION.index][regionIndex]);
	}

	public static boolean isRowFull(int[] board, int rowIndex) {
		return isAreaFull(board, INDICES[Area.ROW.index][rowIndex]);
	}

	public static boolean isColFull(int[] board, int colIndex) {
		return isAreaFull(board, INDICES[Area.COL.index][colIndex]);
	}

	public static boolean isRegionFull(int[] board, int regionIndex) {
		return isAreaFull(board, INDICES[Area.REGION.index][regionIndex]);
	}

	public static boolean isValid(int[] board) {
		checkBoardArrayLength(board);
		int[][] validity = new int[Sudoku.RANK][Sudoku.DIGITS];

		for (int ci = 0; ci < Sudoku.SPACES; ci++) {
			int digit = board[ci];

			if (digit == 0) {
				continue;
			}

			int row = cellRow(ci);
			int col = cellCol(ci);
			int region = cellRegion(ci);
			int digitMask = 1 << (digit - 1);
			if (
			(digitMask & validity[Area.ROW.index][row]) > 0 ||
			(digitMask & validity[Area.COL.index][col]) > 0 ||
			(digitMask & validity[Area.REGION.index][region]) > 0
			) {
				return false;
			}
			validity[Area.ROW.index][row] |= digitMask;
			validity[Area.COL.index][col] |= digitMask;
			validity[Area.REGION.index][region] |= digitMask;
		}

		return true;
	}

	public static boolean isFull(int[] board) {
		checkBoardArrayLength(board);
		for (int ci = 0; ci < Sudoku.DIGITS; ci++) {
			if (board[ci] == 0) {
				return false;
			}
		}
		return true;
	}

	public static boolean isSolved(int[] board) {
		checkBoardArrayLength(board);
		int[][] validity = new int[Sudoku.RANK][Sudoku.DIGITS];

		for (int ci = 0; ci < Sudoku.SPACES; ci++) {
			int digit = board[ci];

			if (digit == 0) {
				return false;
			}

			int row = cellRow(ci);
			int col = cellCol(ci);
			int region = cellRegion(ci);
			int digitMask = 1 << (digit - 1);
			if (
			(digitMask & validity[Area.ROW.index][row]) > 0 ||
			(digitMask & validity[Area.COL.index][col]) > 0 ||
			(digitMask & validity[Area.REGION.index][region]) > 0
			) {
				return false;
			}
			validity[Area.ROW.index][row] |= digitMask;
			validity[Area.COL.index][col] |= digitMask;
			validity[Area.REGION.index][region] |= digitMask;
		}

		return true;
	}

	/**
	* Normalizes the given board such that the top row reads "123456789".
	*/
	public static int[] normalize(int[] board) {
		for (int d = 1; d <= DIGITS; d++) {
			int current = board[d - 1];
			if (current != d) {
				swapAll(board, current, d);
			}
		}
		return board;
	}

	/**
	* Swaps all occurrences of 'a' and 'b' in the given list.
	* Returns the list for convenience.
	*/
	public static int[] swapAll(int[] list, int a, int b) {
		if (a != b) {
			for (int listIndex = 0; listIndex < list.length; listIndex++) {
				if (list[listIndex] == a) {
					list[listIndex] = b;
				} else if (list[listIndex] == b) {
					list[listIndex] = a;
				}
			}
		}
		return list;
	}

	public static boolean isSquare(int n) {
		if (n < 0)
			return false;
		int sqrt = (int)Math.sqrt(n);
		return sqrt*sqrt == n;
	}

	public static int[] swap(int[] arr, int i, int j) {
		int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
		return arr;
	  }

	/**
	* Rotates the given matrix array 90 degrees clockwise.
	* If `arr` is not a square matrix, an error will be thrown.
	* @param arr The matrix to rotate.
	*/
	public static int[] rotate90(int[] arr) {
		if (!isSquare(arr.length))
			throw new IllegalArgumentException("array length must be square");

		int n = (int)Math.sqrt(arr.length);
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

	public static int[] rotate180(int[] arr) {
		return rotate90(rotate90(arr));
	}

	public static int[] rotate270(int[] arr) {
		return rotate90(rotate90(rotate90(arr)));
	}

	/**
	* Reflects the board values over the horizontal axis (line from bottom to top).
	* If the `arr.length / rows` is not a whole number, an error will be thrown.
	* @param arr The matrix to reflect.
	* @param rows The number of rows in the matrix.
	*/
	public static int[] reflectOverHorizontal(int[] arr, int rows) {
		if (arr == null)
			throw new NullPointerException();
		if (rows <= 0)
			throw new IllegalArgumentException("rows must be positive");
		if ((arr.length % rows) != 0)
			throw new IllegalArgumentException("array length must be evenly divisible by given number of rows");
		if (rows < 2)
			return arr;

		int cols = arr.length / rows;
		for (int r = 0; r < (rows / 2); r++) {
			for (int c = 0; c < cols; c++) {
				swap(
					arr,
					r * cols + c,
					(rows - r - 1) * cols + c
				);
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
		if (arr == null)
			throw new NullPointerException();
		if (rows <= 0)
			throw new IllegalArgumentException("rows must be positive");
		if ((arr.length % rows) != 0)
			throw new IllegalArgumentException("array length must be evenly divisible by given number of rows");
		if (arr.length < 2 || rows == arr.length)
			return arr;

		int cols = arr.length / rows;
		for (int c = 0; c < (cols / 2); c++) {
			for (int r = 0; r < rows; r++) {
				swap(
					arr,
					r * cols + c,
					r * cols + (cols - c - 1)
				);
			}
		}
		return arr;
	}

	/**
	* Reflects the board values over the diagonal axis (line from bottomleft to topright).
	* If `arr` is not a square matrix, an error will be thrown.
	* @param arr
	*/
	public static int[] reflectOverDiagonal(int[] arr) {
		if (!isSquare(arr.length))
			throw new IllegalArgumentException("array length must be square");
		if (arr.length < 4)
			return arr;

		reflectOverVertical(arr, (int)Math.sqrt(arr.length));
		rotate90(arr);
		return arr;
	}

	/**
	* Reflects the board values over the antidiagonal axis (line from bottomright to topleft).
	* If `arr` is not a square matrix, an error will be thrown.
	* @param arr
	*/
	public static int[] reflectOverAntiDiagonal(int[] arr) {
		if (!isSquare(arr.length))
			throw new IllegalArgumentException("array length must be square");
		if (arr.length < 4)
			return arr;

		rotate90(arr);
		reflectOverVertical(arr, (int)Math.sqrt(arr.length));
		return arr;
	}

	public static final int RANK = 3;
	public static final int DIGITS = RANK*RANK;
	public static final int SPACES = DIGITS*DIGITS;

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

	public static int[] swapBandRows(int[] arr, int bi, int ri1, int ri2) {
		if (arr.length != 81)
			throw new IllegalArgumentException("arr length must be 81");
        if (ri1 == ri2)
			return arr;
        if (bi < 0 || bi > 2 || ri1 < 0 || ri2 < 0 || ri1 > 2 || ri2 > 2)
            throw new IllegalArgumentException("swapBandRows error, specified band or row(s) out of bounds");
        for (int i = 0; i < DIGITS; i++)
            swap(arr, BAND_ROW_INDICES[bi][ri1][i], BAND_ROW_INDICES[bi][ri2][i]);
        return arr;
    }

    public static int[] swapStackCols(int[] arr, int si, int ci1, int ci2) {
		if (arr.length != 81)
			throw new IllegalArgumentException("arr length must be 81");
        if (ci1 == ci2)
			return arr;
        if (si < 0 || ci1 < 0 || ci2 < 0 || si > 2 || ci1 > 2 || ci2 > 2)
            throw new IllegalArgumentException("swapStackCols error, specified stack or col(s) out of bounds");
        for (int i = 0; i < DIGITS; i++)
			swap(arr, STACK_COL_INDICES[si][ci1][i], STACK_COL_INDICES[si][ci2][i]);
        return arr;
    }

    public static int[] swapBands(int[] arr, int b1, int b2) {
		if (arr.length != 81)
			throw new IllegalArgumentException("arr length must be 81");
        if (b1 == b2)
			return arr;
        if (b1 < 0 || b2 < 0 || b1 > 2 || b2 > 2)
            throw new IllegalArgumentException("swapBands error, specified band(s) out of bounds");
        for (int i = 0; i < 27; i++)
            swap(arr, BAND_INDICES[b1][i], BAND_INDICES[b2][i]);
        return arr;
    }

    public static int[] swapStacks(int[] arr, int s1, int s2) {
		if (arr.length != 81)
			throw new IllegalArgumentException("arr length must be 81");
        if (s1 == s2)
			return arr;
        if (s1 < 0 || s2 < 0 || s1 > 2 || s2 > 2)
            throw new IllegalArgumentException("swapStacks error, specified stack(s) out of bounds");
        for (int i = 0; i < 27; i++)
            swap(arr, STACK_INDICES[s1][i], STACK_INDICES[s2][i]);
        return arr;
    }
}
