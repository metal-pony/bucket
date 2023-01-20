package com.sparklicorn.bucket.games.sudoku.game;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import com.sparklicorn.bucket.util.Shuffler;

import static com.sparklicorn.bucket.games.sudoku.game.Board.*;

public class SudokuUtility {
	/**
	* Returns a copy of the given board, normalized such that the top row reads "123456789".
	*/
	public static Board normalize(Board board) {
		return new Board(normalize(board.getValues(new int[Board.NUM_CELLS])));
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
		int index = ThreadLocalRandom.current().nextInt(NUM_CELLS);
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
			board[rand.nextInt(NUM_CELLS)] = encode(rand.nextInt(NUM_DIGITS + 1));
		}
	}

	public static int[] copyArr(int[] arr) {
		int[] copy = new int[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}

	public static boolean[][] validityMatrix() {
		boolean[][] validityMatrix = new boolean[3][NUM_ROWS];
		for (boolean[] dimension : validityMatrix) {
			Arrays.fill(dimension, false);
		}
		return validityMatrix;
	}

	public static boolean validAreas(int[] board, boolean[][] validityMatrix) {
		boolean result = false;

		for (int areaIndex = 0; areaIndex < NUM_DIGITS; areaIndex++) {
			boolean isRowValid = validityMatrix[0][areaIndex] = isRowValid(board, areaIndex);
			boolean isColValid = validityMatrix[1][areaIndex] = isColValid(board, areaIndex);
			boolean isRegionValid = validityMatrix[2][areaIndex] = isRegionValid(board, areaIndex);

			result = result || isRowValid || isColValid || isRegionValid;
		}

		return result;
	}

	public static boolean hasValidArea(int[] board) {
		for (int areaIndex = 0; areaIndex < NUM_DIGITS; areaIndex++) {
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
		int[] cellIndices = new int[NUM_CELLS];
		for (int i = 0; i < cellIndices.length; i++) {
			cellIndices[i] = i;
		}
		Shuffler.shuffleInts(cellIndices);

		for (int index : cellIndices) {
			int cellValue = board[index];
			board[index] = 0;
			board[index] = cellAreaIsValid(board, index) ? cellValue : 0;
		}
	}

	public static int[] getRandomDigits(int[] digits) {
		return Shuffler.random(digits, 1, NUM_DIGITS + 1);
	}

	/**
	 * Returns a newly allocated sudoku board array.
	 *
	 * @return
	 */
	public static int[] emptyBoard() {
		return new int[NUM_CELLS];
	}

	public static void validateValues(int[] values) {
		if (values == null || values.length != NUM_DIGITS) {
			throw new IllegalArgumentException(String.format(
			"Failed to set board values. values[%s] = %s",
			(values == null) ? "-" : Integer.toString(values.length),
			(values == null) ? "null" : Arrays.toString(values)
			));
		}
	}

	public static void setRowValues(int[] board, int rowIndex, int[] values) {
		validateValues(values);
		System.arraycopy(values, 0, board, rowIndex * NUM_DIGITS, NUM_DIGITS);
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
		int[] board = Shuffler.random(emptyBoard(), 1, NUM_DIGITS + 1);
		digitsToMasks(board);

		int[] randomAreaValues = new int[NUM_DIGITS];
		boolean[][] validityMatrix = validityMatrix();
		while (validAreas(board, validityMatrix)) {
			for (int areaIndex = 0; areaIndex < NUM_ROWS; areaIndex++) {
				if (validityMatrix[0][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, NUM_DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRowValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[1][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, NUM_DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setColumnValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[2][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, NUM_DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRegionValues(board, areaIndex, randomAreaValues);
				}
			}
		}

		return board;
	}

	public static int[] generateRandomInvalidIncompleteConfig() {
		int[] board = Shuffler.random(emptyBoard(), 1, NUM_DIGITS + 1);
		digitsToMasks(board);
		makeIncomplete(board);

		int[] randomAreaValues = new int[NUM_DIGITS];
		boolean[][] validityMatrix = validityMatrix();
		while (validAreas(board, validityMatrix)) {
			for (int areaIndex = 0; areaIndex < NUM_ROWS; areaIndex++) {
				if (validityMatrix[0][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, NUM_DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setRowValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[1][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, NUM_DIGITS + 1);
					digitsToMasks(randomAreaValues);
					setColumnValues(board, areaIndex, randomAreaValues);
				}

				if (validityMatrix[2][areaIndex]) {
					Shuffler.random(randomAreaValues, 1, NUM_DIGITS + 1);
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
		for (int i = 0; i < NUM_CELLS; i++) {
			int cellDigit = decode(board[i]);
			char digitChar = cellDigit > 0 ? (char)((char)cellDigit + '0') : '.';
			boolean isLastCell = i == (NUM_CELLS - 1);

			int nextIndex = i + 1;
			boolean isNextCellInAnotherRegion = getCellRegionIndex(i) != getCellRegionIndex(nextIndex);
			boolean isNextCellInAnotherRow = getCellRowIndex(i) != getCellRowIndex(nextIndex);
			boolean isNextCellInAnotherRowOfRegions = (nextIndex % (NUM_COLUMNS * NUM_ROWS_IN_REGION) == 0);

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
