package com.metal_pony.bucket.sudoku.game;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.metal_pony.bucket.sudoku.game.solvers.Solver;
import com.metal_pony.bucket.util.LoadingMember;

/**
 * The standard 9x9 Sudoku board.
 *
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
	 * *****************************************************/

	public static final int ROOT = 3;
	/** Number of symbols used on Sudoku board. */
	public static final int DIGITS = ROOT * ROOT;
	/** Number of cells on a Sudoku board. */
	public static final int CELLS = DIGITS * DIGITS;
	/** Represents the combination of all candidate values.*/
	public static final int ALL = 0x1FF;

	public static final int[][] ROW_INDICES = new int[DIGITS][];
	public static final int[][] COL_INDICES = new int[DIGITS][];
	public static final int[][] REGION_INDICES = new int[DIGITS][];
	static {
		for (int i = 0; i < DIGITS; i++) {
			ROW_INDICES[i] = getRowIndices(i);
			COL_INDICES[i] = getColIndices(i);
			REGION_INDICES[i] = getRegionIndices(i);
		}
	}

	public static void validateCellIndex(int index) {
		if (index < 0 || index >= CELLS) {
			throw new IllegalArgumentException(
				String.format("Cell index %d is out of bounds.", index)
			);
		}
	}

	public static void validateDigit(int digit) {
		if (digit < 0 || digit > DIGITS) {
			throw new IllegalArgumentException(
				String.format("%d is not a valid sudoku digit.", digit)
			);
		}
	}

	public static void validateCellCandidates(int value) {
		if (value < 0 || value > ALL) {
			throw new IllegalArgumentException(
				String.format("Cell candidate value %d is invalid. Min: %d, Max: %d.", value, 0, ALL)
			);
		}
	}

	public static void validateBoardSize(int[] board) {
		if (board.length != CELLS) {
			throw new IllegalArgumentException(
				String.format("Given board length %d is invalid", board.length)
			);
		}
	}

	public static int getCellRowIndex(int i) {
		return i / DIGITS;
	}

	public static int getCellColIndex(int i) {
		return i % DIGITS;
	}

	public static int getCellRegionIndex(int i) {
		int r = i / DIGITS;
		int c = i % DIGITS;
		return (r / ROOT) * ROOT + c/ROOT;
	}

	public static int getIndexInRegion(int i) {
		int r = i / DIGITS;
		int c = i % DIGITS;
		return (r % ROOT) * ROOT + (c % ROOT);
	}

	/**
	 * Looks up the real Sudoku board value from the given bitstring version.
	 * If the bitstring does not represent a single value, then 0 is returned.
	 * @param mask - bitstring board value.
	 * @return A digit from 0 to DIGITS. A return of zero means the value is empty.
	 */
	public static int decode(int mask) {
		return MASK_TO_DIGIT[mask];
	}

	public static int encode(int digit) {
		validateDigit(digit);
		return (digit > 0 && digit <= DIGITS) ? 1 << (digit - 1) : 0;
	}

	private static final int[] MASK_TO_DIGIT = new int[1 << DIGITS];
	static {
		for (int digit = 1; digit <= DIGITS; digit++) {
			int mask = (1 << (digit - 1));
			MASK_TO_DIGIT[mask] = digit;
		}
	}

	private static final int[] DIGIT_TO_MASK = new int[DIGITS + 1];
	static {
		for (int digit = 1; digit <= DIGITS; digit++) {
			DIGIT_TO_MASK[digit] = 1 << (digit - 1);
		}
	}

	/**
	 * Determines whether the given bitstring board value represents a
	 * real Sudoku board value (and not a combination of multiple values).
	 * @param mask - bitstring board value.
	 * @return True if the given bitstring represents an actual Sudoku board
	 * value; otherwise false.
	 */
	public static boolean isDigit(int mask) {
		// return MASK_TO_DIGIT[mask] > 0;
		return (mask & (mask - 1)) == 0 && mask > 0 && mask < ALL;
	}

	private static final int[] INVERTED_MASK_TO_DIGIT = new int[1 << DIGITS];
	private static final int[] DIGIT_TO_INVERTED_MASK = new int[DIGITS + 1];
	static {
		for (int digit = 1; digit <= DIGITS; digit++) {
			int digitMask = (1 << (digit - 1));
			INVERTED_MASK_TO_DIGIT[digitMask] = digit;
			DIGIT_TO_INVERTED_MASK[digit] = ALL ^ digitMask;
		}
	}

	/**
	 * Removes a digit from the given candidate mask.
	 *
	 * @param mask
	 * @param digit
	 * @return
	 */
	public static int removeCandidate(int mask, int digit) {
		// mask: 110110101 (candidates: 135689)
		// digit: 5
		// result: 110100101 (candidates: 13689)

		if (digit < 1 || digit > DIGITS) {
			throw new IllegalArgumentException(
				String.format("digit (%d) out of bounds [1,%d]", digit, DIGITS)
			);
		}

		return mask & DIGIT_TO_INVERTED_MASK[digit];
	}

	public static int[] parseBoardString(String values) {
		if (values == null) {
			throw new NullPointerException("Given Board string is null.");
		}

		//Empty row shorthand.
		values = values.replaceAll("-", "000000000");

		if (values.length() > CELLS) {
			values = values.substring(0, CELLS);
		}

		while (values.length() < CELLS) {
			values += "0";
		}

		//Non-conforming characters to ZERO.
		values = values.replaceAll("[^1-9]", "0");

		int[] board = new int[CELLS];
		for (int i = 0; i < CELLS; i++) {
			int v = values.charAt(i) - '0';
			board[i] = (v > 0) ? (1 << (v - 1)) : 0;
		}

		return board;
	}

	public static int[][] getRows(int[] board) {
		int[][] rows = new int[DIGITS][];
		for (int rowIndex = 0; rowIndex < DIGITS; rowIndex++) {
			rows[rowIndex] = getRow(board, rowIndex);
		}
		return rows;
	}

	public static int[] getRow(int[] board, int rowIndex) {
		int[] row = new int[DIGITS];
		int index = 0;
		for (int boardIndex : ROW_INDICES[rowIndex]) {
			row[index++] = board[boardIndex];
		}
		return row;
	}

	public static int[][] getColumns(int[] board) {
		int[][] columns = new int[DIGITS][];
		for (int colIndex = 0; colIndex < DIGITS; colIndex++) {
			columns[colIndex] = getColumn(board, colIndex);
		}
		return columns;
	}

	public static int[] getColumn(int[] board, int colIndex) {
		int[] column = new int[DIGITS];
		int index = 0;
		for (int boardIndex : COL_INDICES[colIndex]) {
			column[index++] = board[boardIndex];
		}
		return column;
	}

	public static int[][] getRegions(int[] board) {
		int[][] regions = new int[DIGITS][];
		for (int regionIndex = 0; regionIndex < DIGITS; regionIndex++) {
			regions[regionIndex] = getRegion(board, regionIndex);
		}
		return regions;
	}

	public static int[] getRegion(int[] board, int regionIndex) {
		int[] region = new int[DIGITS];
		int index = 0;
		for (int boardIndex : REGION_INDICES[regionIndex]) {
			region[index++] = board[boardIndex];
		}
		return region;
	}

	public static Board fromCandidates(int[] candidates) {
		Board board = new Board();
		System.arraycopy(candidates, 0, board.board, 0, CELLS);
		board.numClues = board.countClues();
		return board;
	}

	/**
	 * Represents the values of the Sudoku board.
	 */
	protected int[] board;

	protected transient int numClues;

	protected transient LoadingMember<Set<Board>> solutions;

	public boolean hasUniqueSolution() {
		return this.solutions.get().size() == 1;
	}

	/** Creates a Board that is empty.*/
	public Board() {
		board = new int[CELLS];
		Arrays.fill(board, ALL);
		numClues = 0;

		solutions = new LoadingMember<Set<Board>>(() -> {
			return Solver.getAllSolutions(this);
		});
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

		if (values.length() > CELLS) {
			values = values.substring(0, CELLS);
		}

		while (values.length() < CELLS) {
			values += "0";
		}

		//Non-conforming characters to ZERO.
		values = values.replaceAll("[^1-9]", "0");

		board = new int[CELLS];
		for (int i = 0; i < CELLS; i++) {
			int v = values.charAt(i) - '0';
			// TODO #14 Should this be initializing non-digits to ALL?? I would expect 0 candidates instead.
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
		if (values.length != CELLS) {
			throw new IllegalArgumentException("Invalid array length");
		}

		board = new int[CELLS];

		for (int i = 0; i < CELLS; i++) {
			int v = values[i];
			if (v > 0 && v <= DIGITS) {
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
		board = new int[CELLS];
		System.arraycopy(other.board, 0, board, 0, CELLS);
		numClues = other.numClues;
	}

	/** Clears all values on the board.*/
	public void clear() {
		board = new int[CELLS];
		Arrays.fill(board, ALL);
		numClues = 0;
	}

	public int getNumClues() {
		return numClues;
	}

	/** Returns the number of empty spaces on the board.*/
	public int getNumEmptySpaces() {
		return CELLS - countClues();
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
		for (int i = 0; i < CELLS; i++) {
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
		if (board.length < CELLS) {
			board = new int[CELLS];
		}
		System.arraycopy(this.board, 0, board, 0, CELLS);
		return board;
	}

	@Override
	public int getValueAt(int index) {
		return decode(board[index]);
	}

	@Override
	public void setValueAt(int index, int digit) {
		validateCellIndex(index);
		validateDigit(digit);

		int prevValue = decode(board[index]);
		if (digit > 0) {
			board[index] = 1 << (digit - 1);
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

	public Set<Board> getSolutions() {
		return getSolutions(new HashSet<>());
	}

	public Set<Board> getSolutions(final Set<Board> solutionSet) {
		this.solutions.get().forEach((solution) -> {
			solutionSet.add(solution);
		});
		return solutionSet;
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
		if (getNumEmptySpaces() == CELLS) {
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

	// TODO #14 Build Board from compressed string

	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder("\n  ");

	    for (int i = 0; i < CELLS; i++) {
	    	if (isDigit(board[i])) {
	    		strb.append(decode(board[i]));
	    	} else {
	    		strb.append('.');
	    	}

	        if (((((i+1) % DIGITS) % ROOT) == 0) && (((i+1) % DIGITS) != 0)) {
	            strb.append(" | ");
	        } else {
	        	strb.append("   ");
	        }

	        if (((i+1) % DIGITS) == 0) {
	        	strb.append(System.lineSeparator());

	        	if (i == CELLS - 1) {
	        		break;
	        	}

	            if (((Math.floor((i+1) / DIGITS) % ROOT) == 0) && ((Math.floor(i/DIGITS) % (DIGITS-1)) != 0)) {
	            	strb.append(" -----------+-----------+------------");
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

	public static String toString(int[] board) {
		StringBuilder strb = new StringBuilder("  ");

		for (int i = 0; i < CELLS; i++) {
			if (isDigit(board[i])) {
				strb.append(decode(board[i]));
			} else {
				strb.append('.');
			}

			if (((((i+1) % DIGITS) % ROOT) == 0) && (((i+1) % DIGITS) != 0)) {
					strb.append(" | ");
			} else {
				strb.append("   ");
			}

			if (((i+1) % DIGITS) == 0) {
				strb.append(System.lineSeparator());

				if (i == CELLS - 1) {
					break;
				}

				if (((Math.floor((i+1) / DIGITS) % ROOT) == 0) && ((Math.floor(i/DIGITS) % (DIGITS-1)) != 0)) {
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
			for (int shift = 0; shift < DIGITS; shift++) {
				if ((value & (1 << shift)) > 0) {
					list.add(shift + 1);
				}
			}
		}
		return list;
	}

	public int[] getCellCandidates(int index, int[] candidates) {
		int value = board[index];
		int decoded = decode(value);
		int numCandidates = 0;

		if (decoded > 0) {
			// candidates.add(decoded);
			candidates[numCandidates++] = decoded;
		} else {
			for (int shift = 0; shift < DIGITS; shift++) {
				if ((value & (1 << shift)) > 0) {
					// candidates.add(shift + 1);
					candidates[numCandidates++] = shift + 1;
				}
			}
		}

		return candidates;
	}

	/**
	 * Populates the given list with copies of this board, one for each candidate at the given cell.
	 * If the cell is already resolved to a value, this returns the list with a copy of this board.
	 *
	 * @param cellIndex Index of the cell.
	 * @param candidateBoards List of boards to populate.
	 * @return The given list, returned for convenience.
	 */
	public List<Board> getCandidateBoards(int cellIndex, List<Board> candidateBoards) {
		forEachCandidateInCell(cellIndex, (digit) -> {
			Board copy = this.copy();
			this.setValueAt(cellIndex, digit);
			candidateBoards.add(copy);
		});

		return candidateBoards;
	}

	/**
	 * Performs the given callback function for all candidates of the given cell.
	 * The candidate digit is passed to the callback.
	 *
	 * @param cellIndex
	 * @param callback
	 */
	private void forEachCandidateInCell(int cellIndex, Consumer<Integer> callback) {
		int value = board[cellIndex];

		for (int shift = 0; shift < DIGITS; shift++) {
			if ((value & (1 << shift)) > 0) {
				callback.accept(shift + 1);
			}
		}
	}

	/**
	 * Iterates through each candidate for a given cell on the board. A callback is invoked
	 * with each candidate digit.
	 *
	 * @param board
	 * @param cellIndex
	 * @param func
	 */
	public static void forEachCellCandidate(int[] board, int cellIndex, Consumer<Integer> func) {
		if (func == null) {
			throw new NullPointerException("Given func cannot be null");
		}

		validateBoardSize(board);
		validateCellIndex(cellIndex);

		int candidates = board[cellIndex];

		for (int shift = 0; shift < DIGITS; shift++) {
			if ((candidates & (1 << shift)) > 0) {
				func.accept(shift + 1);
			}
		}
	}

	public static void forEachCellCandidate(int[] board, int cellIndex, Function<Integer,Boolean> func) {
		if (func == null) {
			throw new NullPointerException("Given func cannot be null");
		}

		validateBoardSize(board);
		validateCellIndex(cellIndex);

		int candidates = board[cellIndex];

		for (int shift = 0; shift < DIGITS; shift++) {
			if ((candidates & (1 << shift)) > 0) {
				boolean result = func.apply(shift + 1);
				if (!result) {
					break;
				}
			}
		}
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

	public static boolean isValid(int[] board) {
		validateBoardSize(board);

		for (int i = 0; i < DIGITS; i++) {
			if (!isRowValid(board, i)) {
				return false;
			}

			if (!isColValid(board, i)) {
				return false;
			}

			if (!isRegionValid(board, i)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Determines whether the given Sudoku board is valid.
	 * <br/>A board is valid if every row, column, and 3x3 region is valid,
	 * meaning none contain duplicate digits.
	 * <br/>Validity of the board only means that the configuration is
	 * acceptable.  It does not mean the configuration is correct.
	 * <br/>The board does not need to be complete to be valid. A blank board
	 * is valid by the above definition.
	 *
	 * @return True if the board is valid; otherwise false.
	 */
	public boolean isValid() {

		//Check for positions with no candidates.
		/*for (int i = 0; i < CELLS; i++) {
			int v = board[i];
			if (v == 0)
				return false;
		}*/

		for (int x = 0; x < DIGITS; x++) {
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
		int[] result = new int[DIGITS];
		for (int i = 0; i < DIGITS; i++) {
			result[i] = row * DIGITS + i;
		}
		return result;
	}

	public static int[] getColIndices(int col) {
		int[] result = new int[DIGITS];
		for (int i = 0; i < DIGITS; i++) {
			result[i] = col + i * DIGITS;
		}
		return result;
	}

	public static int[] getRegionIndices(int region) {
		int[] result = new int[DIGITS];
		int gr = region / ROOT;
		int gc = region % ROOT;
		for (int i = 0; i < DIGITS; i++) {
			result[i] = gr*DIGITS*ROOT + gc*ROOT +
				(i/ROOT)*DIGITS + (i%ROOT);
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
			if (digit > 0 && digit <= DIGITS) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	public static boolean isRowValid(int[] board, int rowIndex) {
		validateBoardSize(board);
		validateRowIndex(rowIndex);
		int c = 0;
		for (int cellIndex : ROW_INDICES[rowIndex]) {
			int digit = decode(board[cellIndex]);
			if (digit > 0 && digit <= DIGITS) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	private static void validateRowIndex(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= DIGITS) {
			throw new IllegalArgumentException(
				String.format("Given row index %d is invalid", rowIndex)
			);
		}
	}

	private static void validateColumnIndex(int columnIndex) {
		if (columnIndex < 0 || columnIndex >= DIGITS) {
			throw new IllegalArgumentException(
				String.format("Given column index %d is invalid", columnIndex)
			);
		}
	}

	private static void validateRegionIndex(int regionIndex) {
		if (regionIndex < 0 || regionIndex >= DIGITS) {
			throw new IllegalArgumentException(
				String.format("Given region index %d is invalid", regionIndex)
			);
		}
	}

	public boolean isRowFull(int row) {
		for (int i = row * DIGITS, nextRow = (row + 1) * DIGITS; i < nextRow; i++) {
			if (getValueAt(i) == 0) {
				return false;
			}
		}
		return true;
	}

	public static boolean isRowFull(int[] board, int rowIndex) {
		validateBoardSize(board);
		validateRowIndex(rowIndex);

		for (
			int cellIndex = rowIndex * DIGITS, nextRow = (rowIndex + 1) * DIGITS;
			cellIndex < nextRow;
			cellIndex++
		) {
			if (decode(board[cellIndex]) == 0) {
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
			if (digit > 0 && digit <= DIGITS) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	public static boolean isColValid(int[] board, int columnIndex) {
		validateBoardSize(board);
		validateColumnIndex(columnIndex);

		int c = 0;
		for (int cellIndex : COL_INDICES[columnIndex]) {
			int digit = decode(board[cellIndex]);
			if (digit > 0 && digit <= DIGITS) {
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

	public static boolean isColFull(int[] board, int columnIndex) {
		validateBoardSize(board);
		validateColumnIndex(columnIndex);

		for (int cellIndex : COL_INDICES[columnIndex]) {
			if (decode(board[cellIndex]) == 0) {
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
			if (digit > 0 && digit <= DIGITS) {
				int mask = 1 << (digit - 1);
				if ((c & mask) != 0) {
					return false;
				}
				c |= mask;
			}
		}
		return true;
	}

	public static boolean isRegionValid(int[] board, int regionIndex) {
		validateBoardSize(board);
		validateRegionIndex(regionIndex);

		int c = 0;
		for (int cellIndex : REGION_INDICES[regionIndex]) {
			int digit = decode(board[cellIndex]);
			if (digit > 0 && digit <= DIGITS) {
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

	public static boolean isRegionFull(int[] board, int regionIndex) {
		validateBoardSize(board);
		validateRegionIndex(regionIndex);

		for (int cellIndex : REGION_INDICES[regionIndex]) {
			if (decode(board[cellIndex]) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines whether the given Sudoku board is full. A board is full if every
	 * cell contains a digit from 1 to 9.
	 *
	 * @return True if the board is full; otherwise false.
	 */
	public boolean isFull() {
		return numClues == CELLS;
	}

	public static boolean isFull(int[] board) {
		validateBoardSize(board);

		for (int cellIndex = 0; cellIndex < CELLS; cellIndex++) {
			if (decode(board[cellIndex]) == 0) {
				return false;
			}
		}
		return true;
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

	public static boolean isSolved(int[] board) {
		validateBoardSize(board);

		return isFull(board) && isValid(board);
	}

	public void kill() {
		this.board = null;
	}

	public int[] toArray() {
		return toArray(SudokuUtility.emptyBoard());
	}

	public int[] toArray(int[] arr) {
		if (arr.length < CELLS) {
			throw new IllegalArgumentException(String.format(
				"Given array does not have enough space (size=%d)",
				arr.length
			));
		}

		System.arraycopy(board, 0, arr, 0, CELLS);

		return arr;
	}
}
