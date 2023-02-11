package com.sparklicorn.bucket.games.sudoku.game.solvers;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.sparklicorn.bucket.games.sudoku.game.Board.*;

import com.sparklicorn.bucket.games.sudoku.game.*;

public class Solver {
	static class Constraints {
		static final int ROW_MASK = ALL << (NUM_DIGITS * 2);
		static final int COLUMN_MASK = ALL << NUM_DIGITS;
		static final int REGION_MASK = ALL;

		/**
		 * Contains the contraints in the form
		 * <code>[... other bits][9 row bits][9 column bits][9 region bits]</code>.
		 * Use the provided masks to filter for row, column, or region constraints.
		 */
		int[] values;

		Constraints() {
			this.values = new int[NUM_DIGITS];
		}

		Constraints(Constraints source) {
			this();
			System.arraycopy(source.values, 0, this.values, 0, NUM_DIGITS);
		}

		Constraints(int[] board) {
			this();
			forEach(board, (cellIndex, candidates) -> {
				if (isDigit(candidates)) {
					add(cellIndex, decode(candidates));
				}
			});
		}

		int getForRow(int rowIndex) {
			return (values[rowIndex] & ROW_MASK) >> (NUM_DIGITS * 2);
		}

		int getForCol(int columnIndex) {
			return (values[columnIndex] & COLUMN_MASK) >> NUM_DIGITS;
		}

		int getForRegion(int regionIndex) {
			return values[regionIndex] & REGION_MASK;
		}

		int getForCell(int cellIndex) {
			return getForRow(getCellRowIndex(cellIndex)) |
				getForCol(getCellColIndex(cellIndex)) |
				getForRegion(getCellRegionIndex(cellIndex));

			// int row = values[getCellRowIndex(cellIndex)] & ROW_MASK;
			// int col = values[getCellColIndex(cellIndex)] & COLUMN_MASK;
			// int region = values[getCellRegionIndex(cellIndex)] & REGION_MASK;

			// return row | col | region;
		}

		void add(int cellIndex, int digit) {
			int rowIndex = getCellRowIndex(cellIndex);
			int columnIndex = getCellColIndex(cellIndex);
			int regionIndex = getCellRegionIndex(cellIndex);
			// System.out.printf("Adding constraint on %d (%d,%d) -> %d\n", cellIndex, rowIndex + 1, columnIndex + 1, digit);

			values[rowIndex] |= 1 << (digit - 1 + (NUM_DIGITS * 2));
			values[columnIndex] |= 1 << (digit - 1 + NUM_DIGITS);
			values[regionIndex] |= 1 << (digit - 1);
		}

		String[] inspect(int cellIndex) {
			return new String[] {
				Integer.toBinaryString(getForRow(getCellRowIndex(cellIndex))),
				Integer.toBinaryString(getForCol(getCellColIndex(cellIndex))),
				Integer.toBinaryString(getForRegion(getCellRegionIndex(cellIndex))),
				Integer.toBinaryString(getForCell(cellIndex))
			};
		}

		String toString(int cellIndex) {
			return String.format(
				"row: %s, col: %s, reg: %s, cel: %s",
				d(getForRow(getCellRowIndex(cellIndex))),
				d(getForCol(getCellColIndex(cellIndex))),
				d(getForRegion(getCellRegionIndex(cellIndex))),
				d(getForCell(cellIndex))
			);
		}

		String bin(int constraints) {
			String str = Integer.toBinaryString(constraints);
			String pad = "";

			if (str.length() < Board.NUM_DIGITS) {
				pad = "0".repeat(Board.NUM_DIGITS - str.length());
			}

			return pad + str;
		}
	}

	static <T> void forEach(T[] arr, Consumer<Integer> func) {
		for (int index = 0; index < arr.length; index++) {
			func.accept(index);
		}
	}
	static <T> void forEach(T[] arr, Function<Integer,Boolean> func) {
		for (int index = 0; index < arr.length; index++) {
			if (!func.apply(index)) {
				return;
			}
		}
	}

	static void forEach(int[] arr, Consumer<Integer> func) {
		for (int index = 0; index < arr.length; index++) {
			func.accept(index);
		}
	}
	static void forEach(int[] arr, Function<Integer,Boolean> func) {
		for (int index = 0; index < arr.length; index++) {
			if (!func.apply(index)) {
				return;
			}
		}
	}

	static <T> void forEach(T[] arr, BiConsumer<Integer, T> func) {
		for (int index = 0; index < arr.length; index++) {
			func.accept(index, arr[index]);
		}
	}
	static <T> void forEach(T[] arr, BiFunction<Integer, T, Boolean> func) {
		for (int index = 0; index < arr.length; index++) {
			if(!func.apply(index, arr[index])) {
				return;
			}
		}
	}

	static void forEach(int[] arr, BiConsumer<Integer, Integer> func) {
		for (int index = 0; index < arr.length; index++) {
			func.accept(index, arr[index]);
		}
	}
	static void forEach(int[] arr, BiFunction<Integer, Integer, Boolean> func) {
		for (int index = 0; index < arr.length; index++) {
			if (!func.apply(index, arr[index])) {
				return;
			}
		}
	}

	static void forEachCell(int[] board, int[] cellIndices, BiConsumer<Integer, Integer> func) {
		for (int cellIndex : cellIndices) {
			func.accept(cellIndex, board[cellIndex]);
		}
	}
	static void forEachCell(int[] board, int[] cellIndices, BiFunction<Integer, Integer, Boolean> func) {
		for (int cellIndex : cellIndices) {
			if (!func.apply(cellIndex, board[cellIndex])) {
				return;
			}
		}
	}

	static void forEachCellInRow(int[] board, int rowIndex, BiConsumer<Integer, Integer> func) {
		forEachCell(board, ROW_INDICES[rowIndex], func);
	}
	static void forEachCellInRow(int[] board, int rowIndex, BiFunction<Integer, Integer, Boolean> func) {
		forEachCell(board, ROW_INDICES[rowIndex], func);
	}

	static void forEachCellInColumn(int[] board, int columnIndex, BiConsumer<Integer, Integer> func) {
		forEachCell(board, COL_INDICES[columnIndex], func);
	}
	static void forEachCellInColumn(int[] board, int columnIndex, BiFunction<Integer, Integer, Boolean> func) {
		forEachCell(board, COL_INDICES[columnIndex], func);
	}

	static void forEachCellInRegion(int[] board, int regionIndex, BiConsumer<Integer, Integer> func) {
		forEachCell(board, REGION_INDICES[regionIndex], func);
	}
	static void forEachCellInRegion(int[] board, int regionIndex, BiFunction<Integer, Integer, Boolean> func) {
		forEachCell(board, REGION_INDICES[regionIndex], func);
	}

	static void forEachNeighborCell(int[] board, int cellIndex, BiConsumer<Integer, Integer> func) {
		forEachCellInRow(board, getCellRowIndex(cellIndex), func);
		forEachCellInColumn(board, getCellColIndex(cellIndex), func);
		forEachCellInRegion(board, getCellRegionIndex(cellIndex), func);
	}

	/**
	 * Resets empty cells to all candidates.
	 *
	 * @param board
	 */
	public static int[] resetEmptyCells(int[] board) {
		forEach(board, (cellIndex, candidates) -> {
			if (!isDigit(candidates)) {
				board[cellIndex] = ALL;
			}
		});
		return board;
	}

	static class IntArr {
		int[] arr;

		IntArr(int[] arr) {
			this.arr = new int[arr.length];
			System.arraycopy(arr, 0, this.arr, 0, arr.length);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof IntArr) {
				IntArr _obj = (IntArr) obj;
				return Arrays.equals(arr, _obj.arr);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(arr);
		}
	}

	/**
	 * Attempts to get all solutions for the given board.
	 *
	 * @param board
	 * @return
	 */
	public static List<int[]> getSolutions(int[] board) {
		BoardSolution _board = new BoardSolution(board);
		Set<IntArr> solutions = new HashSet<>();
		searchForSolutions(_board, (solution) -> {
			solutions.add(solution);
		});

		return solutions.stream()
			.map((intArr) -> intArr.arr)
            .toList();
	}

	/**
	 * Decodes Sudoku candidate bits into a representation easier consumed by humans.
	 * Eg, <code>d(0b011011001) => 14578</code>, and each digit in the result
	 * corresponds to a candidate for the associated cell.
	 * TODO Remove this after debugging
	 *
	 * @param encoded
	 * @return
	 */
	public static int d(int encoded) {
		if (Board.isDigit(encoded)) {
			return Board.decode(encoded);
		}

		int result = 0;
		for (int digit = 1; digit <= Board.NUM_DIGITS; digit++) {
			if ((encoded & (1 << (digit - 1))) > 0) {
				result = (result * 10) + digit;
			}
		}
		return result;
	}

	static int[] decodeToArr(int encoded) {
		int[] result = new int[Integer.bitCount(encoded)];

		int index = 0;
		for (int digit = 1; digit <= Board.NUM_DIGITS; digit++) {
			if ((encoded & (1 << (digit - 1))) > 0) {
				result[index] = digit;
				index++;
			}
		}

		return result;
	}

	static void postDigit(BoardSolution board, int cellIndex, int digit) {
		board.board[cellIndex] = Board.encode(digit);
		board.constraints.add(cellIndex, digit);
	}

	static void reduceNeighbors(BoardSolution board, int cellIndex) {
		// System.out.printf("Relaxing neighbors of cell %d ...\n", cellIndex);
		forEachNeighborCell(board.board, cellIndex, (neighborIndex, _neighborCandidates) -> {
			if (neighborIndex != cellIndex) {
				reduce2(board, neighborIndex);
			}
		});
		// System.out.printf("Done relaxing neighbors of cell %d ...\n", cellIndex);
	}

	static boolean isCandidateUniqueInArea(int[] board, int[] areaIndices, int cellIndex, int candidateMask) {
		for (int neighborIndex : areaIndices) {
			if (neighborIndex == cellIndex) {
				continue;
			}

			if ((board[neighborIndex] & candidateMask) > 0) {
				return false;
			}
		}

		return true;
	}

	static int searchForUniqueCandidate(int[] board, int cellIndex) {
		final int candidates = board[cellIndex];
		// // TODO for debug, delete later
		// System.out.printf(
		// 	"Unique check on %d for [%d]: ",
		// 	cellIndex,
		// 	d(candidates)
		// );
		final int row = Board.getCellRowIndex(cellIndex);
		final int col = Board.getCellColIndex(cellIndex);
		final int region = Board.getCellRegionIndex(cellIndex);

		// For each candidate of given cell
		for (int candidateMask = 1; candidateMask < (1<<Board.NUM_DIGITS); candidateMask <<= 1) {
			if ((candidates & candidateMask) > 0) {
				// System.out.printf("%d... ", Board.decode(candidateMask));

				// For each area the cell is a part of (row, column, region)
				// If candidate is unique among cells in any area
				if (
					isCandidateUniqueInArea(board, Board.ROW_INDICES[row], cellIndex, candidateMask) ||
					isCandidateUniqueInArea(board, Board.COL_INDICES[col], cellIndex, candidateMask) ||
					isCandidateUniqueInArea(board, Board.REGION_INDICES[region], cellIndex, candidateMask)
				) {
					// System.out.println("unique found!");
					// System.out.printf(
					// 	"Unique check on %d for [%d] found %d\n",
					// 	cellIndex,
					// 	d(candidates),
					// 	d(candidateMask)
					// );
					return candidateMask;
				}
			}
		}
		// System.out.println();

		// No unique candidate found.
		return 0;
	}

	// attempts to reduce the given cell's candidates by elimination
	// returns whether the cell changed as result of this call.
	// may mutate board.board and board.constraints
	/**
	 * Attempts to reduce the given cell's candidates.
	 *
	 * @param board
	 * @param cellIndex
	 * @param nextCells
	 * @return
	 */
	static boolean reduce2(BoardSolution board, int cellIndex) {
		Board.validateCellIndex(cellIndex);
		int[] _board = board.board;
		int candidates = _board[cellIndex];

		if (isDigit(candidates) || candidates <= 0) {
			// System.out.println(".  skipping...");
			return false;
		}

		// System.out.printf(
		// 	"reduce %d (%d,%d): [%d]",
		// 	cellIndex,
		// 	Board.getCellRowIndex(cellIndex) + 1,
		// 	Board.getCellColIndex(cellIndex) + 1,
		// 	d(board.board[cellIndex])
		// );

		// ? If candidate constraints reduces to 0, then the board is likely invalid.
		// TODO Reason out and test what happens when the board is invalid.
		int reducedCandidates = _board[cellIndex] &= ~board.constraints.getForCell(cellIndex);
		if (reducedCandidates <= 0) {
			// System.out.printf(
			// 	"reduce %d (%d,%d): [%d]",
			// 	cellIndex,
			// 	Board.getCellRowIndex(cellIndex) + 1,
			// 	Board.getCellColIndex(cellIndex) + 1,
			// 	d(candidates)
			// );
			// System.out.println(".  constraints reduced to 0... ERROR ERROR ERROR");
			return false;
		}

		// // TODO for debug, delete later
		// if (reducedCandidates < candidates) {
		// 	System.out.printf(
		// 		"reduce %d (%d,%d): [%d]",
		// 		cellIndex,
		// 		Board.getCellRowIndex(cellIndex) + 1,
		// 		Board.getCellColIndex(cellIndex) + 1,
		// 		d(candidates)
		// 	);
		// 	System.out.printf(" -> relaxes to [%d]\n", d(reducedCandidates));
		// }

		if (isDigit(reducedCandidates)) {
			postDigit(board, cellIndex, Board.decode(reducedCandidates));
		} else {
			int uniqueCandidate = searchForUniqueCandidate(_board, cellIndex);
			if (uniqueCandidate > 0) {
				postDigit(board, cellIndex, Board.decode(uniqueCandidate));
				reducedCandidates = uniqueCandidate;
			}
		}

		if (reducedCandidates < candidates) {
			reduceNeighbors(board, cellIndex);
		}

		// Whether candidates for the given cell have changed.
		return candidates != reducedCandidates;
	}

	static boolean reduce(int[] board) {
		return reduce(new BoardSolution(board));
	}

	static boolean reduce(BoardSolution boardSolution) {
		boolean boardSolutionChanged = false;
		boolean hadReduction;

		do {
			hadReduction = false;
			for (int i = 0; i < boardSolution.board.length; i++) {
				hadReduction |= reduce2(boardSolution, i);
				boardSolutionChanged |= hadReduction;
			}
		} while (hadReduction);

		return boardSolutionChanged;
	}

	/**
	 * Determines if the given board solves uniquely to the provided solution.
	 * <br/>This may take a long time, and possibly never return, depending on
	 * how much of the board is already solved.  Generally, boards with
	 * around 20 clues should have little problem solving on modern processors.
	 *
	 * @param board - the Sudoku board to solve.
	 * @return True if the board has one unique solution equivalent to the
	 * one provided; otherwise false.
	 */
	public static boolean solvesUniquely(int[] board) {
		//p.execute(b) => false if b is alternate solution.
		//When p.execute == false, search stops and returns false.
		//When p.execute == true, search continues.
		//Search returns true when the search is exhausted.
		AtomicInteger numSolves = new AtomicInteger(0);

		BoardSolution boardSolution = new BoardSolution(board);
		searchForSolutions(boardSolution, (b) -> {
			//System.out.println(b.getSimplifiedString());
			return numSolves.incrementAndGet() == 1;
		});

		return numSolves.get() == 1;
	}

	/**
	 * Attempts to solve the given Sudoku puzzle.
	 * This may take a long time, and possibly never return, depending on
	 * how much of the board is already solved.
	 *
	 * @param puzzle - The Sudoku board to solve.
	 * @return A set containing all the solutions for the given Sudoku board.
	 */
	public static Set<Board> getAllSolutions(Board puzzle) {
		HashSet<Board> result = new HashSet<>();

		searchForSolutions(new BoardSolution(puzzle), (solution) -> {
			//System.out.println("found solution: " + b.getSimplifiedString());
			result.add(Board.fromCandidates(solution.arr));
		});

		return result;
	}

	static int[] copyBoard(int[] board) {
		int[] copy = new int[NUM_CELLS];
		System.arraycopy(board, 0, copy, 0, NUM_CELLS);
		return copy;
	}

	static record BoardSolution(int[] board, Constraints constraints) {
		BoardSolution(int[] board) {
			this(copyBoard(board), new Constraints(board));
		}
		BoardSolution(Board board) {
			this(board.toArray(), new Constraints(board.toArray()));
		}
		BoardSolution(BoardSolution source) {
			this(copyBoard(source.board), new Constraints(source.constraints));
		}

		void solve() {

		}
	}

	public static void forAllSolutions(Board board, Consumer<Board> func) {
		searchForSolutions(
			new BoardSolution(board),
			(solution) -> {
				func.accept(Board.fromCandidates(solution.arr));
				return true;
			}
		);
	}

	public static Board solve(Board board) {
		int[] solution = SudokuUtility.emptyBoard();
		searchForSolutions(
			new BoardSolution(board),
			(_solution) -> {
				System.arraycopy(_solution.arr, 0, solution, 0, NUM_CELLS);
				return false;
			}
		);
		return Board.fromCandidates(solution);
	}

	/**
	 * Performs a breadth-first search for sudoku solution(s) of the given board.
	 * The given callback function is triggered when a solution is found. If the callback
	 * returns <code>false</code>, the search will stop;
	 * otherwise it will continue searching for solutions.
	 *
	 * @param boardSolution Contains the initial board and its constraints.
	 * @param solutionFoundCallback Called with a solution board when one is found.
	 * If this returns <code>true</code>, then the search will continue for more solutions;
	 * otherwise the search will stop.
	 * @return <code>true</code> if the search exhausted all possible solutions;
	 * otherwise <code>false</code>.
	 */
	static boolean searchForSolutions(
		BoardSolution boardSolution,
		Function<IntArr, Boolean> solutionFoundCallback
	) {
		Queue<BoardSolution> solutionQueue = new ArrayDeque<>();
		solutionQueue.offer(boardSolution);
		resetEmptyCells(boardSolution.board);

		while (!solutionQueue.isEmpty()) {
			BoardSolution possibleSolution = solutionQueue.poll();
			reduce(possibleSolution);

			if (
                isSolved(possibleSolution) &&
                !solutionFoundCallback.apply(new IntArr(possibleSolution.board))
            ) {
				return false;
			} else {
				expandQueueWithPossibleSolutions(solutionQueue, possibleSolution);
			}
		}

		return true;
	}

	static boolean searchForSolutions(BoardSolution boardSolution, Consumer<IntArr> callback) {
		return searchForSolutions(boardSolution, (solution) -> {
			callback.accept(solution);
			return true;
		});
	}

	private static void expandQueueWithPossibleSolutions(
		Queue<BoardSolution> queue,
		BoardSolution currentSolution
	) {
		int unsolvedIndex = pickEmptyCell(currentSolution.board);
		if (
			unsolvedIndex >= 0 // &&
			// empty cell actually has candidates (if cell is 0 -> board is invalid)
			// TODO This doesn't seem possible - may be able to remove
			// candidateBoard[emptyCellIndex] > 0
		) {
			forEachCellCandidate(currentSolution.board, unsolvedIndex, (candidateDigit) -> {
				BoardSolution possibleSolution = new BoardSolution(currentSolution);
				possibleSolution.board[unsolvedIndex] = encode(candidateDigit);
				possibleSolution.constraints.add(unsolvedIndex, candidateDigit);
				queue.offer(possibleSolution);
			});
		}
	}

	/**
	 * Returns the index of an empty cell in <code>board</code> which contains the
	 * fewest candidates. If the board has no empty cell, returns <code>-1</code>.
	 *
	 * @param board Sudoku board array.
	 * @return Index of an unsolved cell that contains the fewest number of candidates,
	 * or <code>-1</code> if the board has no empty cells.
	 */
	private static int pickEmptyCell(int[] board) {
		int minIndex = -1;
		int minCandidates = Board.NUM_DIGITS + 1;

		for (int i = 0; i < Board.NUM_CELLS; i++) {
			int numCandidates = Integer.bitCount(board[i]);

			if (numCandidates < 2) {
				continue;
			} else if (numCandidates == 2) {
				return i;
			}

			if (numCandidates < minCandidates) {
				minIndex = i;
				minCandidates = numCandidates;
			}
		}

		return minIndex;
	}

	static boolean isSolved(BoardSolution boardSolution) {
		return Board.isSolved(boardSolution.board);
	}
}
