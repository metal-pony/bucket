package com.sparklicorn.bucket.games.sudoku.game;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The standard 9x9 Sudoku board.
 * <br/><br/>
 * This SudokuBoard can also keep track of candidate values in each cell
 * through somewhat cumbersome bitmasking.
 * <br/>Values in the backing array are bitmasks in the interval [0x0, 0x1FF].
 * Each bit represents a candidate digit.
 * <br/>For example,
 * <ul>
 * <li><code>000000001</code> represents the candidate value of 1.</li>
 * <li><code>000000010</code> represents the candidate value of 2.</li>
 * <li><code>100000000</code> represents the candidate value of 9.</li>
 * Multiple candidate values are represented by a combination of set bits.
 * <li><code>000000011</code> represents the candidate values 1 and 2.</li>
 * <li><code>000100011</code> represents the candidate values 1, 2, 6.</li>
 * <li><code>101010101</code> represents the candidate values 1, 3, 5, 7, 9.</li>
 * And lastly,
 * <li><code>000000000</code> represents no possible candidate values.</li>
 * </ul>
 */
public class Board implements ISudokuBoard, Serializable {

	private static final long serialVersionUID = -6346231818157404798L;

	/* ****************************************************
	 * 			NOTES
	 *
	 * Board values are represented as powers of 2, allowing for multiple
	 * values per cell (i.e. candidate values) in the form of a bitstring.
	 *
	 * Examples:
	 * board[x] = 1
	 * 1 => (0 0000 0001)
	 * (0 0000 0001) has only the first bit from the left set.
	 * board[x] real value is 1.
	 *
	 * board[y] = 64
	 * 64 => (0 0100 0000)
	 * (0 0100 0000) has only the 7th bit from the left set.
	 * board[y] real value is 7.
	 *
	 * Example: board[z] = 482
	 * 482 => (1 1110 0010)
	 * (1 1110 0010) has bits 2, 6, 7, 8, 9 set.
	 * board[z] real value may be 2, 6, 7, 8, 9, to be resolved by user later.
	 ******************************************************/

	public static final int NUM_DIGITS = 9;
	public static final int NUM_ROWS_IN_REGION = 3;
	public static final int NUM_COLS_IN_REGION = 3;

	/** Number of cells in a standard Sudoku board.*/
	public static final int NUM_CELLS = NUM_DIGITS * NUM_DIGITS;

	/** Represents the combination of all candidate values.*/
	public static final int ALL = 0x1ff;

	public static final int[][] ROW_INDICES = new int[NUM_DIGITS][];
	public static final int[][] COL_INDICES = new int[NUM_DIGITS][];
	public static final int[][] REGION_INDICES = new int[NUM_DIGITS][];
	static {
		for (int i = 0; i < NUM_DIGITS; i++) {
			ROW_INDICES[i] = getRowIndices(i);
			COL_INDICES[i] = getColIndices(i);
			REGION_INDICES[i] = getRegionIndices(i);
		}
	}

	public static int getRowForIndex(int i) {
		return i / 9;
	}

	public static int getColForIndex(int i) {
		return i % 9;
	}

	public static int getRegionForIndex(int i) {
		int r = i / 9;
		int c = i % 9;
		return (r / 3) * 3 + c/3;
	}

	public static int getIndexInRegion(int i) {
		int r = i / 9;
		int c = i % 9;
		return (r % 3) * 3 + (c % 3);
	}

	/**
	 * Looks up the real Sudoku board value from the given bitstring version.
	 * If the bitstring does not represent a single value, then 0 is returned.
	 * @param bits - bitstring board value.
	 * @return A digit from 0 to 9. A return of zero means the value is empty.
	 */
	public static int decode(int bits) {
		return DECODER2[bits];
	}

	private static final int[] DECODER2 = new int[512];
	static {
		for (int i = 1; i <= 9; i++) {
			int index = (1 << (i - 1));
			DECODER2[index] = i;
		}
	}

	/**
	 * Determines whether the given bitstring board value represents a
	 * real Sudoku board value (and not a combination of multiple values).
	 * @param bits - bitstring board value.
	 * @return True if the given bitstring represents an actual Sudoku board
	 * value; otherwise false.
	 */
	public static boolean isSingleDigit(int bits) {
		return DECODER2[bits] > 0;
	}

	/**
	 * Represents the values of the Sudoku board.
	 */
	protected int[] board;

	protected int numClues;

	/** Creates a Board that is empty.*/
	public Board() {
		board = new int[NUM_CELLS];
		Arrays.fill(board, ALL);
		numClues = 0;
	}

	/**
	 * Creates a Board from a string of values.
	 * Each block of 9 characters will be associated with a row on the board.
	 * The string length must exactly match the number of board spaces.
	 * Characters that are not 1 through 9 in the given string will be
	 * considered empty spaces.
	 * @param values - String of values that will be used to populate the board.
	 */
	public Board(String values) {
		if (values == null)
			throw new NullPointerException("Given Board string is null.");

		//Empty row shorthand.
		values = values.replaceAll("-", "000000000");

		if (values.length() > NUM_CELLS) {
			values = values.substring(0, NUM_CELLS);
		}

		while (values.length() < NUM_CELLS) {
			values += "0";
		}

		//Non-conforming characters to ZERO.
		values = values.replaceAll("[^1-9]", "0");

		board = new int[NUM_CELLS];
		for (int i = 0; i < NUM_CELLS; i++) {
			int v = values.charAt(i) - '0';
			board[i] = (v > 0) ? (1 << (v - 1)) : ALL;
		}

		numClues = countClues();
	}

	/**
	 * Creates a Board with the values provided by the given array.
	 * Each block of 9 values will be associated with a row on the board.
	 * The length of the array must exactly match the number of board spaces.
	 * Values that are out of the range [0, 9] will be consider empty spaces.
	 * @param values - Array of Sudoku values that will be used to populate
	 * the board.
	 */
	public Board(int[] values) {
		if (values.length != NUM_CELLS) {
			throw new IllegalArgumentException("Number of values was not appropriate.");
		}
		board = new int[NUM_CELLS];

		for (int i = 0; i < NUM_CELLS; i++) {
			int v = values[i];
			if (v > 0 && v <= 9) {
				board[i] = (1 << (v - 1));
			} else {
				board[i] = ALL;
			}
		}

		numClues = countClues();
	}

	/**
	 * Creates a Board that is the copy of the one given.
	 * @param other - Board object to copy values from.
	 */
	public Board(Board other) {
		board = new int[NUM_CELLS];
		System.arraycopy(other.board, 0, board, 0, NUM_CELLS);
		numClues = other.numClues;
	}

	/** Clears all values on the board.*/
	public void clear() {
		board = new int[NUM_CELLS];
		Arrays.fill(board, ALL);
		numClues = 0;
	}

	public int getNumClues() {
		return numClues;
	}

	/** Returns the number of empty spaces on the board.*/
	public int getNumEmptySpaces() {
		return NUM_CELLS - countClues();
	}

	public int countClues() {
		int result = 0;
		for (int v : this) {
			if (decode(v) > 0) {
				result++;
			}
		}
		return result;
	}

	@Override
	public int[] getValues(int[] board) {
		for (int i = 0; i < NUM_CELLS; i++) {
			board[i] = decode(this.board[i]);
		}
		return board;
	}

	/**
	 * Retrieves the board as an array of masks.
	 * @param board - The array to populate with the board values.
	 * @return The board populated array, or if the array was too small,
	 * a newly allocated array with the populated values.
	 */
	public int[] getMasks(int[] board) {
		if (board.length < NUM_CELLS) {
			board = new int[NUM_CELLS];
		}
		System.arraycopy(this.board, 0, board, 0, NUM_CELLS);
		return board;
	}

	@Override
	public int getValueAt(int index) {
		return decode(board[index]);
	}

	@Override
	public void setValueAt(int index, int value) {
		if (value < 0 || value > 9) {
			throw new IllegalArgumentException("Value is out of bounds.");
		}

		int prevValue = decode(board[index]);
		if (value > 0) {
			board[index] = 1 << (value - 1);
			if (prevValue == 0) {
				numClues++;
			}

		} else {
			board[index] = 0;
			if (prevValue > 0) {
				numClues--;
			}
		}
	}

	/**
	 * Returns the mask value on the board at the given position.
	 * This bitmask represents the candidates values for that position.
	 * <br/>See {@link Board} for information about how the bitmask is used.
	 * @param index - The position on the board [0, 80], where 0 represents
	 * the top-left position, and 80 the bottom-right.
	 */
	public int getMaskAt(int index) {
		return board[index];
	}

	/**
	 * Sets the mask value on the board at the given position.
	 * This bitmask represents the candidates values for that position.
	 * <br/>See {@link Board} for information about how the bitmask is used.
	 * @param index - The position on the board [0, 80], where 0 represents
	 * the top-left position, and 80 the bottom-right.
	 * @param value - The bitmask to set at this position.
	 */
	public void setMaskAt(int index, int value) {
		if (value < 0 || value > ALL) {
			throw new IllegalArgumentException("Value is out of bounds.");
		}

		int prevValue = decode(board[index]);
		int newValue = decode(value);
		if (newValue > 0 && prevValue == 0) {
			numClues++;
		} else if (newValue == 0 && prevValue > 0) {
			numClues--;
		}

		board[index] = value;
	}

	/**
	 * <em>Two boards are equal if they contain the same configuration of values.</em>
	 * <br/><br/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Board) {
			return Arrays.equals(board, ((Board) obj).board);
		}
		return false;
	}

	/** Returns a string representing the Sudoku board in a condensed form.*/
	public String getSimplifiedString() {
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

	public static final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public String getCompressedString() {
		if (getNumEmptySpaces() == NUM_CELLS) {
			return "-";
		}

		String str = getSimplifiedString();
		for (int numEmpty = alphabet.length() + 1; numEmpty > 1; numEmpty--) {
			str = str.replaceAll(
				"\\.".repeat(numEmpty),
				Character.toString(alphabet.charAt(numEmpty - 2))
			);
		}
		return str;
	}

	// TODO - Build Board from compressed string

	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder("  ");

	    for (int i = 0; i < NUM_CELLS; i++) {
	    	if (isSingleDigit(board[i])) {
	    		strb.append(decode(board[i]));
	    	} else {
	    		strb.append('.');
	    	}

	        if (((((i+1) % 9) % 3) == 0) && (((i+1) % 9) != 0)) {
	            strb.append(" | ");
	        } else {
	        	strb.append("   ");
	        }

	        if (((i+1) % 9) == 0) {
	        	strb.append(System.lineSeparator());

	        	if (i == NUM_CELLS - 1) {
	        		break;
	        	}

	            if (((Math.floor((i+1) / 9) % 3) == 0) && ((Math.floor(i/9) % 8) != 0)) {
	            	strb.append(" -----------|-----------|------------");
	            	strb.append(System.lineSeparator());
	            } else {
	                strb.append("            |           |            ");
	            	strb.append(System.lineSeparator());
	            }
	            strb.append("  ");
	        }
	    }

		return strb.toString();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(board);
	}

	@Override
	public List<Integer> getCandidates(int index, List<Integer> list) {
		int value = board[index];
		int decoded = decode(value);
		if (decoded > 0) {
			list.add(decoded);
		} else {
			for (int shift = 0; shift < 9; shift++) {
				if ((value & (1 << shift)) > 0) {
					list.add(shift + 1);
				}
			}
		}
		return list;
	}

	@Override
	public Iterator<Integer> iterator() {
		return Arrays.stream(board).iterator();
	}

	/**
	 * Returns a deep copy of this board.
	 */
	public Board copy() {
		return new Board(this);
	}

	/**
	 * Determines whether the given Sudoku board is valid.
	 * <br/>A board is valid if every row, column, and 3x3 region is valid,
	 * meaning none contain duplicate digits.
	 * <br/>Validity of the board only means that the configuration is
	 * acceptable.  It does not mean the configuration is correct.
	 * <br/>The board does not need to be complete to be valid. A blank board
	 * is valid by the above definition.
	 * @return True if the board is valid; otherwise false.
	 */
	public boolean isValid() {

		//Check for positions with no candidates.
		/*for (int i = 0; i < NUM_CELLS; i++) {
			int v = board[i];
			if (v == 0)
				return false;
		}*/

		for (int x = 0; x < 9; x++) {
			if (!isRowValid(x)) {
				return false;
			}

			if (!isColValid(x)) {
				return false;
			}

			if (!isRegionValid(x)) {
				return false;
			}
		}
		return true;
	}

	public static int[] getRowIndices(int row) {
		int[] result = new int[NUM_DIGITS];
		for (int i = 0; i < NUM_DIGITS; i++) {
			result[i] = row * NUM_DIGITS + i;
		}
		return result;
	}

	public static int[] getColIndices(int col) {
		int[] result = new int[NUM_DIGITS];
		for (int i = 0; i < NUM_DIGITS; i++) {
			result[i] = col + i * NUM_DIGITS;
		}
		return result;
	}

	public static int[] getRegionIndices(int region) {
		int[] result = new int[NUM_DIGITS];
		int gr = region / NUM_ROWS_IN_REGION;
		int gc = region % NUM_COLS_IN_REGION;
		for (int i = 0; i < NUM_DIGITS; i++) {
			result[i] = gr*NUM_DIGITS*NUM_ROWS_IN_REGION + gc*NUM_COLS_IN_REGION +
				(i/NUM_ROWS_IN_REGION)*NUM_DIGITS + (i%NUM_COLS_IN_REGION);
		}
		return result;
	}

	/**
	 * Determines whether the given row on the given board is valid.
	 * <br/>A row is considered valid if it contains no duplicate digits
	 * in any of the cells.
	 * <br/>The row does not need to be complete to be valid.
	 * @param row - the row of the board to evaluate.
	 * @return True if the row is valid; otherwise false.
	 */
	public boolean isRowValid(int row) {
		int c = 0;
		for (int i : ROW_INDICES[row]) {
			int digit = getValueAt(i);
			if (digit > 0 && digit <= 9) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	public boolean isRowFull(int row) {
		for (int i = row * 9, nextRow = (row + 1) * 9; i < nextRow; i++) {
			if (getValueAt(i) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines whether the given column on the given board is valid.
	 * <br/>A column is considered valid if it contains no duplicate digits
	 * in any of the cells.
	 * <br/>The column does not need to be complete to be valid.
	 * @param column - the column of the board to evaluate.
	 * @return True if the column is valid; otherwise false.
	 */
	public boolean isColValid(int column) {
		int c = 0;
		for (int i : COL_INDICES[column]) {
			int digit = getValueAt(i);
			if (digit > 0 && digit <= 9) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	public boolean isColFull(int column) {
		for (int i : COL_INDICES[column]) {
			if (getValueAt(i) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines whether the given region on the given board is valid.
	 * <br/>A region is considered valid if it contains no duplicate digits
	 * in any of the cells.
	 * <br/>The region does not need to be complete to be valid.
	 * @param region - the region of the board to evaluate.
	 * @return True if the region is valid; otherwise false.
	 */
	public boolean isRegionValid(int region) {
		int c = 0;
		for (int i : REGION_INDICES[region]) {
			int digit = getValueAt(i);
			if (digit > 0 && digit <= 9) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	public boolean isRegionFull(int region) {
		for (int i : REGION_INDICES[region]) {
			if (getValueAt(i) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines whether the given Sudoku board is full.
	 * <br/>A board is full if every cell contains a digit from 1 to 9.
	 * <br/>The configuration of the digits does not need to be valid
	 * for the board to be considered full.
	 * @return True if the board is full; otherwise false.
	 */
	public boolean isFull() {
		return numClues == NUM_CELLS;
		//for (int b : board) {
		//	if (DECODER2[b] == 0) {
		//		return false;
		//	}
		//}
		//return true;
	}

	/**
	 * Determines whether the given Sudoku board is solved.
	 * <br/>A board is solved if it is completely full of numbers and is
	 * valid.
	 * @return True if the board is solved; otherwise false.
	 */
	public boolean isSolved() {
		return isFull() && isValid();
	}

	public void kill() {
		this.board = null;
	}
}
