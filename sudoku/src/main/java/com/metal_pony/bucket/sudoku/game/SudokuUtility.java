package com.metal_pony.bucket.sudoku.game;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import com.metal_pony.bucket.util.Shuffler;

import static com.metal_pony.bucket.sudoku.game.Board.*;

import com.metal_pony.bucket.sudoku.Sudoku;

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
	* Returns a copy of the given board, normalized such that the top row reads "123456789".
	*/
	public static Board normalize(Board board) {
		return new Board(normalize(board.getValues(new int[Board.CELLS])));
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

	/**
	* Determines if the given boards have the same configuration, but with
	* the digits swapped.
	*/
	public static boolean isPermutation(Board a, Board b) {
		return normalize(a).equals(normalize(b));
	}

	// RANDOM UTILITY

	/**
	 * Attempts to locate a random non-zero element in the given array.
	 *
	 * @param board
	 * @return Index of a non-zero element, or -1 if no such element exists.
	 */
	public static int getRandomNonZeroElementIndex(int[] board) {
		return findRandom(board, (element) -> element != 0);
	}

	/**
	 * Attempts to locate a random element in the given array that satisfies some condition.
	 *
	 * @param arr
	 * @return Index of a random element, or -1 if no element matching the predicate is found.
	 */
	public static int findRandom(int[] arr, Function<Integer,Boolean> predicate) {
		int index = ThreadLocalRandom.current().nextInt(CELLS);
		while (!predicate.apply(arr[(++index) % arr.length]) && index < arr.length * 2);
		return (index < arr.length * 2) ? index % arr.length : -1;
	}

	/**
	 * Among elements that satisfy the given condition, swaps two at random.
	 *
	 * @param board
	 * @throws NoSuchElementException If there are less than two elements in the array that
	 * satisfy the condition.
	 */
	public static void swapRandom(int[] arr, Function<Integer,Boolean> predicate) {
		int swappableElements = 0;
		for (int index = 0; index < arr.length && swappableElements < 2; index++) {
			if (predicate.apply(arr[index])) {
				swappableElements++;
			}
		}

		if (swappableElements < 2) {
			throw new NoSuchElementException("Not enough elements to swap.");
		}

		int a = findRandom(arr, predicate);
		int b;
		while (a == (b = findRandom(arr, predicate)));
		Shuffler.swap(arr, a, b);
	}

	/**
	 * Inserts a random digit into a random potition on the given board.
	 * @param board
	 */
	public static void randomlyMutate(int[] board) {
		randomlyMutate(board, 1);
	}

	/**
	 * Inserts a random digit in random positions on the given board.
	 * Repeated a given number of times.
	 *
	 * @param board
	 * @param times
	 */
	public static void randomlyMutate(int[] board, int times) {
		if (times < 1 || times > 1000) {
			times = 1;
		}

		ThreadLocalRandom rand = ThreadLocalRandom.current();
		for (int n = 0; n < times; n++) {
			board[rand.nextInt(CELLS)] = encode(rand.nextInt(DIGITS + 1));
		}
	}

	public static int[] copyArr(int[] arr) {
		int[] copy = new int[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}

	public static boolean[][] validityMatrix() {
		boolean[][] validityMatrix = new boolean[3][DIGITS];
		for (boolean[] dimension : validityMatrix) {
			Arrays.fill(dimension, false);
		}
		return validityMatrix;
	}

	public static boolean validAreas(int[] board, boolean[][] validityMatrix) {
		boolean result = false;

		for (int areaIndex = 0; areaIndex < DIGITS; areaIndex++) {
			boolean isRowValid = validityMatrix[0][areaIndex] = isRowValid(board, areaIndex);
			boolean isColValid = validityMatrix[1][areaIndex] = isColValid(board, areaIndex);
			boolean isRegionValid = validityMatrix[2][areaIndex] = isRegionValid(board, areaIndex);

			result = result || isRowValid || isColValid || isRegionValid;
		}

		return result;
	}

	public static boolean hasValidArea(int[] board) {
		for (int areaIndex = 0; areaIndex < DIGITS; areaIndex++) {
			if (
			isRowValid(board, areaIndex) ||
			isColValid(board, areaIndex) ||
			isRegionValid(board, areaIndex)
			) {
				return true;
			}
		}
		return false;
	}

	public static boolean cellAreaIsValid(int[] board, int cellIndex) {
		return isRowValid(board, getCellRowIndex(cellIndex)) ||
		isColValid(board, getCellColIndex(cellIndex)) ||
		isRegionValid(board, getCellRegionIndex(cellIndex));
	}

	public static void makeIncomplete(int[] board) {
		// Erasing these cells yields a board where every row, column, and region are incomplete.
		Arrays.asList(0, 13, 26, 28, 41, 51, 56, 70, 75)
		.stream()
		.forEach(index -> board[index] = 0);
	}

	public static void digitsToMasks(int [] digits) {
		for (int i = 0; i < digits.length; i++) {
			digits[i] = encode(digits[i]);
		}
	}

	public static void blankUneededCells(int[] board) {
		int[] cellIndices = new int[CELLS];
		for (int i = 0; i < cellIndices.length; i++) {
			cellIndices[i] = i;
		}
		Shuffler.shuffle(cellIndices);

		for (int index : cellIndices) {
			int cellValue = board[index];
			board[index] = 0;
			board[index] = cellAreaIsValid(board, index) ? cellValue : 0;
		}
	}

	public static int[] getRandomDigits(int[] digits) {
		return Shuffler.random(digits, 1, DIGITS + 1);
	}

	/**
	 * Returns a newly allocated sudoku board array.
	 *
	 * @return
	 */
	public static int[] emptyBoard() {
		return new int[CELLS];
	}

	public static void validateValues(int[] values) {
		if (values == null || values.length != DIGITS) {
			throw new IllegalArgumentException(String.format(
			"Failed to set board values. values[%s] = %s",
			(values == null) ? "-" : Integer.toString(values.length),
			(values == null) ? "null" : Arrays.toString(values)
			));
		}
	}

	public static void setRowValues(int[] board, int rowIndex, int[] values) {
		validateValues(values);
		System.arraycopy(values, 0, board, rowIndex * DIGITS, DIGITS);
	}

	public static void setColumnValues(int[] board, int columnIndex, int[] values) {
		validateValues(values);
		for (int valuesIndex = 0; valuesIndex < values.length; valuesIndex++) {
			board[COL_INDICES[columnIndex][valuesIndex]] = values[valuesIndex];
		}
	}

	public static void setRegionValues(int[] board, int regionIndex, int[] values) {
		validateValues(values);
		for (int valuesIndex = 0; valuesIndex < values.length; valuesIndex++) {
			board[REGION_INDICES[regionIndex][valuesIndex]] = values[valuesIndex];
		}
	}

	public static String boardRowStr(int[] board, int rowIndex) {
		return Arrays.toString(
		Arrays.stream(getRow(board, rowIndex))
		.map(Board::decode)
		.toArray()
		);
	}

	public static String boardColStr(int[] board, int colIndex) {
		return Arrays.toString(
		Arrays.stream(getColumn(board, colIndex))
		.map(Board::decode)
		.toArray()
		);
	}

	public static String boardRegionStr(int[] board, int regionIndex) {
		return Arrays.toString(
		Arrays.stream(getRegion(board, regionIndex))
		.map(Board::decode)
		.toArray()
		);
	}

	public static int[] generateRandomInvalidCompleteConfig() {
		int[] board = Shuffler.random(emptyBoard(), 1, DIGITS + 1);
		digitsToMasks(board);

		int[] randomAreaValues = new int[DIGITS];
		boolean[][] validityMatrix = validityMatrix();
		while (validAreas(board, validityMatrix)) {
			for (int areaIndex = 0; areaIndex < DIGITS; areaIndex++) {
				if (validityMatrix[0][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRowValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[1][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setColumnValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[2][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRegionValues(board, areaIndex, randomAreaValues);
				}
			}
		}

		return board;
	}

	public static int[] generateRandomInvalidIncompleteConfig() {
		int[] board = Shuffler.random(emptyBoard(), 1, DIGITS + 1);
		digitsToMasks(board);
		makeIncomplete(board);

		int[] randomAreaValues = new int[DIGITS];
		boolean[][] validityMatrix = validityMatrix();
		while (validAreas(board, validityMatrix)) {
			for (int areaIndex = 0; areaIndex < DIGITS; areaIndex++) {
				if (validityMatrix[0][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRowValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[1][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setColumnValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[2][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRegionValues(board, areaIndex, randomAreaValues);
				}
			}
			makeIncomplete(board);
		}

		blankUneededCells(board);

		return board;
	}

	public static String getSimplifiedString(int[] board) {
		StringBuilder strb = new StringBuilder();
		for (int i : board) {
			int v = decode(i);
			if (v > 0) {
				strb.append(v);
			} else {
				strb.append('.');
			}
		}
		return strb.toString();
	}

	public static String getString(int[] board) {
		final String SOLID_ROW_BORDER = "-----------|-----------|-----------";
		final String EMPTY_ROW_BORDER = "           |           |           ";
		final String SOLID_COL_BORDER = " | ";
		final String EMPTY_COL_BORDER = "   ";

		StringBuilder strb = new StringBuilder(" ");
		for (int i = 0; i < CELLS; i++) {
			int cellDigit = decode(board[i]);
			char digitChar = cellDigit > 0 ? (char)((char)cellDigit + '0') : '.';
			boolean isLastCell = i == (CELLS - 1);

			int nextIndex = i + 1;
			boolean isNextCellInAnotherRegion = getCellRegionIndex(i) != getCellRegionIndex(nextIndex);
			boolean isNextCellInAnotherRow = getCellRowIndex(i) != getCellRowIndex(nextIndex);
			boolean isNextCellInAnotherRowOfRegions = (nextIndex % (DIGITS * ROOT) == 0);

			strb.append(digitChar);

			if (isNextCellInAnotherRow) {
				strb.append(' '); // Cap off end of digit row with an empty buffer space
				strb.append(System.lineSeparator());

				if (!isLastCell) {
					strb.append(isNextCellInAnotherRowOfRegions ? SOLID_ROW_BORDER : EMPTY_ROW_BORDER);
					strb.append(System.lineSeparator());
					strb.append(' '); // Start new digit row with an empty buffer space
				}
			} else {
				strb.append(isNextCellInAnotherRegion ? SOLID_COL_BORDER : EMPTY_COL_BORDER);
			}
		}

		return strb.toString();
	}
}
