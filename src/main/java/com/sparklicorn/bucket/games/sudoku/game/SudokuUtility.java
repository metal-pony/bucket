package com.sparklicorn.bucket.games.sudoku.game;

public class SudokuUtility {

    /**
     * Returns a copy of the given board that has been normalized.
     * A board is "normalized" when the top row reads "123456789".
     * Yes, I just made that up.
     */
    public static Board normalize(Board board) {
        return new Board(normalize(board.getValues(new int[Board.NUM_CELLS])));
    }

    /**
     * Returns the board (int[]) that has been normalized.
     * A board is "normalized" when the top row reads "123456789".
     * Yes, I just made that up.
     */
    public static int[] normalize(int[] board) {
        for (int i = 1; i <= 9; i++) {
            int d = board[i - 1];
            if (d != i)
                swapAll(board, d, i);
        }
        return board;
    }

    /**
     * Swaps all occurrences of 'a' and 'b' in the given list.
     * Returns the list for convenience.
     */
    public static int[] swapAll(int[] list, int a, int b) {
        if (a != b) {
            for (int i = 0; i < list.length; i++) {
                if (list[i] == a)
                    list[i] = b;
                else if (list[i] == b)
                    list[i] = a;
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
}
