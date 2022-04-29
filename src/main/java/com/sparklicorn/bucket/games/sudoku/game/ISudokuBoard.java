package com.sparklicorn.bucket.games.sudoku.game;

import java.util.List;

/**
 * Defines basic functionality of a Sudoku board.
 *
 * This interface extends Iterable<Integer>. Specifically, the iterator
 * produced should step through the values of the board, from what would be
 * considered the top-left value to the bottom-right (wrapping around to the
 * first column when the last column is reached).
 */
public interface ISudokuBoard extends Iterable<Integer> {

	/**
	 * Populates the given array with the values of the Sudoku board.
	 * @param board - the array to fill with Sudoku board values.
	 */
	public int[] getValues(int[] board);

	/**
	 * Gets the value on the Sudoku board at the given index.
	 * @param index - the position of the value on the board. 0 for the
	 * topleft corner, to 80 for the bottomright.
	 * @return The value on the board at the given position.
	 */
	public int getValueAt(int index);

	/**
	 * Sets the value on the Sudoku board to the one given at the specified
	 * position.
	 * @param index - the position on the board.
	 * @param value - the value to set in the given position.
	 */
	public void setValueAt(int index, int value);

	/**
	 * Populates a list of candidate values that the Sudoku cell at the given
	 * index may be.  This list may vary among implementations, as some
	 * may use more complex techniques for narrowing the list.
	 * <br/><br/>
	 * In general, the implemention should at least narrow the
	 * candidates list by eliminating existing values in the same row,
	 * col, and region of the provided index.
	 * @param index - the position of the space on the board.
	 * @param list - a list object to populate.  This will be returned for
	 * convenience.
	 * @return A list containing candidate digits for the given space on the
	 * Sudoku board.
	 */
	public List<Integer> getCandidates(int index, List<Integer> list);

}
