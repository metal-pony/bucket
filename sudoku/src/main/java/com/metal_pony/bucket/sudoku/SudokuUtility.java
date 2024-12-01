package com.metal_pony.bucket.sudoku;

public class SudokuUtility {
	public static enum Area {
		ROW(0), COL(1), REGION(2);
		private final int index;
		private Area(int index) {
			this.index = index;
		}
	};
	static final int[][][] INDICES = new int[Sudoku.DIGITS_SQRT][Sudoku.DIGITS][Sudoku.DIGITS];
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
		int regionRow = ci / (Sudoku.DIGITS_SQRT * Sudoku.DIGITS);
		int regionCol = (ci % Sudoku.DIGITS) / Sudoku.DIGITS_SQRT;
		return (regionRow * Sudoku.DIGITS_SQRT) + regionCol;
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
		int[][] validity = new int[Sudoku.DIGITS_SQRT][Sudoku.DIGITS];

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
		int[][] validity = new int[Sudoku.DIGITS_SQRT][Sudoku.DIGITS];

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
		for (int boardIndex = 1; boardIndex <= 9; boardIndex++) {
			int digit = board[boardIndex - 1];
			if (digit != boardIndex) {
				swapAll(board, digit, boardIndex);
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
}
