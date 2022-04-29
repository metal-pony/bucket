package com.sparklicorn.bucket.games.sudoku.game.solvers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.sparklicorn.bucket.games.sudoku.game.Board.*;

import com.sparklicorn.bucket.games.sudoku.game.*;
import com.sparklicorn.bucket.util.PQEntry;
import com.sparklicorn.bucket.util.PriorityQueue;

public class Solver {

	public static interface SolutionFoundCallback {
		/**
		 * Performs the specified callback with a given Board object.
		 * @return Whether the solution search algorithm should continue;
		 */
		public boolean call(Board b);
	}

	/**
	 * Attempts to solve the given Sudoku board, returning the first
	 * solution found, or null if no solution was found.
	 * <br/>
	 * This may take a long time, and possibly never return, depending on
	 * how much of the board is already solved.  Generally, boards with
	 * around 20 clues should have little problem solving on modern processors.
	 * @param board - the Sudoku board to work on.
	 * @return A new ISudokuBoard representing the puzzle solution.
	 */
	public static Board solve(Board board) {
		Board[] result = new Board[1];
		searchForSolution(board, (b) -> {
			result[0] = b;
			return false;
		});
		return result[0];
	}

	/**
	 * Attempts to solve the given Sudoku board.
	 * <br/>This may take a long time, and possibly never return, depending on
	 * how much of the board is already solved.  Generally, boards with
	 * around 20 clues should have little problem solving on modern processors.
	 * @param board - the Sudoku board to work on.
	 * @return A set containing all the solutions for the given Sudoku board.
	 */
	public static Set<Board> getAllSolutions(Board board) {
		HashSet<Board> result = new HashSet<>();
		searchForSolution(board, (b) -> {
			//System.out.println("found solution: " + b.getSimplifiedString());
			result.add(b);
			return true;
		});
		return result;
	}

	/**
	 * Attempts to solve the given Sudoku board.
	 * <br/>This may take a long time, and possibly never return, depending on
	 * how much of the board is already solved.  Generally, boards with
	 * around 20 clues should have little problem solving on modern processors.
	 * @param board - the Sudoku board to work on.
	 * @return A set containing all the solutions for the given Sudoku board.
	 */
	public static void findAllSolutions(Board board, Consumer<Board> callback) {
		searchForSolution3(board, (b) -> {
			if (callback != null) {
				callback.accept(b);
			}
			return true;
		});
	}

	/**
	 * Determines if the given board solves uniquely to the provided solution.
	 * <br/>This may take a long time, and possibly never return, depending on
	 * how much of the board is already solved.  Generally, boards with
	 * around 20 clues should have little problem solving on modern processors.
	 * @param board - the Sudoku board to solve.
	 * @param solution - the solution that the board should solve to.
	 * @return True if the board has one unique solution equivalent to the
	 * one provided; otherwise false.
	 */
	public static boolean solvesUniquely(Board board, Board solution) {
		//p.execute(b) => false if b is alternate solution.
		//When p.execute == false, search stops and returns false.
		//When p.execute == true, search continues.
		//Search returns true when the search is exhausted.
		AtomicBoolean result = new AtomicBoolean(false);
		searchForSolution(board, (b) -> {
			//System.out.println(b.getSimplifiedString());
			result.set(b.equals(solution));
			return result.get();
		});
		return result.get();
	}

	private static boolean searchForSolution(Board board, SolutionFoundCallback p) {
		//This will be reused and repopulated by board.getCandidates(list) to reduce overhead.
		List<Integer> candidates = new ArrayList<>(9);
		Queue<Board> q = new ArrayDeque<>();
		q.offer(new Board(board));

		while (!q.isEmpty()) {
			Board b = q.poll();

			if (!b.isFull()) {
				reduce(b);
			}

			if (b.isFull()) {
				if (!p.call(b)) {
					return false;
				}
			} else {
				int index = pickEmptyCell(b);
				if (index >= 0) {
					for (Board c : getCandidateBoards(b, index, candidates)) {
						q.offer(c);
					}
					//putCellCandidatesInQueue(b, index, q, candidates);
				}
			}
		}

		return true;
	}

	private static class Node<T> {
		T data;
		List<Node<T>> nextsUnivisited;
		boolean visited;
		boolean gen;
		Node(T data) {
			this.data = data;
			this.visited = false;
			this.nextsUnivisited = new ArrayList<>();
			gen = false;
		}
		T getData() {
			return data;
		}
		int getSize() {
			return nextsUnivisited.size();
		}
		Node<T> getNextUnvisited() {
			if (nextsUnivisited.isEmpty()) {
				return null;
			}
			return nextsUnivisited.remove(nextsUnivisited.size() - 1);
		}
		void visit() {
			this.visited = true;
		}
		boolean addUnvisited(Node<T> neighbor) {
			gen = true;
			return nextsUnivisited.add(neighbor);
		}
		void kill() {
			nextsUnivisited = null;
			data = null;
		}
	}

	private static boolean searchForSolution2(Board board, SolutionFoundCallback p) {

		List<Integer> candidates = new ArrayList<>(9);
		Stack<Node<Board>> stack = new Stack<>();

		HashSet<Board> solutions = new HashSet<>();

		Board _board = board.copy();
		reduce(_board);
		Node<Board> startNode = new Node<>(_board);
		startNode.visit();

		stack.push(startNode);

		int[] masks = new int[Board.NUM_CELLS];

		search: while (!stack.empty()) {
			Node<Board> node = stack.peek();
			Board b = node.getData();

			System.out.println("Checking " + b.getSimplifiedString());

			if (b.isFull()) {
				if (!solutions.contains(b)) {
					if (!p.call(b)) {
						return false;
					}
				}
				solutions.add(b);

				stack.pop();
				node.kill();
				b.kill();
			} else {

				if (node.gen) {
					if (node.getSize() > 0) {
						Node<Board> next = node.getNextUnvisited();
						next.visit();
						stack.push(next);
					} else {
						stack.pop();
						node.kill();
						b.kill();
					}

				} else {
					//int count = 0;
					for (int i = 0; i < Board.NUM_CELLS; i++) {
						if (b.getValueAt(i) == 0) {
							candidates.clear();
							for (Board c : getCandidateBoards(b, i, candidates)) {
								reduce(c);
								boolean hasZero = false;
								for (int rawValue : c.getMasks(masks)) {
									if (rawValue == 0) {
										hasZero = true;
										//System.out.println("Board has zero.");
										break;
									}
								}
								if (!hasZero && c.isValid()) {
									node.addUnvisited(new Node<>(c));
									//count++;
								} else {
									stack.pop();
									node.kill();
									b.kill();
									continue search;
								}
							}
						}
					}
					node.gen = true;
					//System.out.println("Generated " + count + " neighbors.");

					if (node.getSize() > 0) {
						Node<Board> next = node.getNextUnvisited();
						next.visit();
						stack.push(next);
					} else {
						stack.pop();
						node.kill();
						b.kill();
					}
				}
			}
		}

		return true;
	}

	private static <T> boolean isheap(List<T> list, Comparator<T> comparator) {
		for (int i = 1; i < list.size(); i++) {
			if (comparator.compare(list.get(i), list.get((i - 1) / 2)) < 0) {
				return false;
			}
		}
		return true;
	}

	//use priorityQueue, where less empty spaces = higher priority
	private static boolean searchForSolution3(Board board, SolutionFoundCallback p) {
		//This will be reused and repopulated by board.getCandidates(list)
		//	to reduce overhead.
		List<Integer> candidates = new ArrayList<>(9);
		Comparator<Board> boardComparator = (Board b1, Board b2) -> {
			return b2.getNumClues() - b1.getNumClues();
		};

		PriorityQueue<PQEntry<Board>> q = new PriorityQueue<>();

		HashSet<Board> solutions = new HashSet<>();
		//int[] masks = new int[Board.NUM_CELLS];
		q.offer(new PQEntry<>(board.copy(), boardComparator));

		while (!q.isEmpty()) {
			Board b = q.poll().data();

			//System.out.print(q.size() + " Checking " + b.getSimplifiedString());

			//ArrayList<Board> heap = new ArrayList<>(q);
			//if (!isheap(heap, comparator)) {
			//	System.out.println();
			//	System.out.println("HEAP BROKEN");
			//	return false;
			//}

			if (!b.isValid()) {
				//System.out.println(" [INVALID]");
				continue;
			}

			if (b.isFull()) {
				if (solutions.add(b)) {
					//System.out.println(" [SOLUTION FOUND]");
					if (!p.call(b)) {
						return false;
					}
				}

			} else {
				for (int i = 0; i < Board.NUM_CELLS; i++) {
					if (b.getValueAt(i) == 0) {
						for (Board c : getCandidateBoards(b, i, candidates)) {
							reduce(c);
							q.offer(new PQEntry<>(c, boardComparator));
						}
					}
				}
			}
		}

		return true;
	}

	//Use arraylist for queue, sort after inserting
	private static boolean searchForSolution4(Board board, SolutionFoundCallback p) {
		//This will be reused and repopulated by board.getCandidates(list)
		//	to reduce overhead.
		//List<Integer> candidates = new ArrayList<>(9);
		Comparator<Board> boardComparator = (Board b1, Board b2) -> {
			return b2.getNumClues() - b1.getNumClues();
		};
		ArrayList<Board> q = new ArrayList<>();
		//HashSet<Board> solutions = new HashSet<>();
		//int[] masks = new int[Board.NUM_CELLS];
		q.add(board.copy());

		while (!q.isEmpty()) {
			Board b = q.remove(0);

			System.out.print(q.size() + " Checking " + b.getSimplifiedString());

			//ArrayList<Board> heap = new ArrayList<>(q);
			//if (!isheap(heap, comparator)) {
			//	System.out.println();
			//	System.out.println("HEAP BROKEN");
			//	return false;
			//}

			if (!b.isValid()) {
				System.out.println(" [INVALID]");
				continue;
			}

			if (b.isFull()) {
				//if (!solutions.contains(b)) {
					System.out.println(" [SOLUTION FOUND]");
					if (!p.call(b)) {
						return false;
					}
					//solutions.add(b);
				//}

			} else {
				int count = 0;
				for (int i = 0; i < Board.NUM_CELLS; i++) {
					if (b.getValueAt(i) == 0) {
						for (int x = 1; x < 10; x++) {
							Board c = b.copy();
							c.setValueAt(i, x);
							q.add(c);
							count++;
						}
					}
				}
				if (count > 0) {
					q.sort(boardComparator);
				}
				System.out.println(" [" + count + "]");
			}
		}

		return true;
	}

	/**
	 * Searches for the index of the empty cell in board which has the fewest
	 * number of candidates. Starts at the top of the board.
	 */
	private static int pickEmptyCell(Board b) {
		int index = -1;
		int numDigits = 10; //number of digit options for cell at index
		for (int i = 0; i < NUM_CELLS; i++) {
			int numOpts = Integer.bitCount(b.getMaskAt(i));
			if (numOpts > 1 && numOpts < numDigits) {
				index = i;
				numDigits = numOpts;
				if (numDigits == 2) //stop early. Won't find a cell with fewer candidates.
					break;
			}
		}
		return index;
	}

	/**
	 * Picks the cell from board with the fewest number of candidates,
	 * then fills in the candidates for that cell in copies of board.  Places
	 * the copies into the queue.
	 * <br/>Since this method is called frequently while searching for Sudoku
	 * solutions, and Board.getCandidates() takes and returns a List, the caller may
	 * pass in a list object to be used instead of this method allocating a new one
	 * (and creating more garbage).
	 * @param board - the Sudoku board trying to be solved.
	 * @param queue - the queue of boards used to search for the solution(s).
	 * @param candidates - a list of Integers used internally.
	 * The list will be cleared and repopulated with the candidates for the
	 * cell described above.
	 */
	private static void putCellCandidatesInQueue(Board board, int cellIndex,
	Queue<Board> queue, List<Integer> candidates)
	{
		if (candidates == null)
			candidates = new ArrayList<Integer>(9);

		candidates.clear();
		board.getCandidates(cellIndex, candidates);
		for (int option : candidates) {
			Board bCopy = new Board(board);
			bCopy.setValueAt(cellIndex, option);
			queue.offer(bCopy);
		}
	}

	private static List<Board> getCandidateBoards(Board board, int cellIndex, List<Integer> candidates) {
		if (candidates == null)
			candidates = new ArrayList<Integer>(9);

		candidates.clear();
		board.getCandidates(cellIndex, candidates);
		List<Board> result = new ArrayList<>();
		for (int option : candidates) {
			Board bCopy = new Board(board);
			bCopy.setValueAt(cellIndex, option);
			result.add(bCopy);
		}
		return result;
	}

	/**
	 * Attempts to fill in the most obvious cells on the board.
	 * <br/>It is possible that this method solves the puzzle.
	 * @param board - the Sudoku board to work on.
	 * @return True if the board was changed as a result of this call;
	 * otherwise false.
	 */
	protected static boolean reduce(Board board) {
		if (board.isFull()) {
			return false;
		}

		//reduce until we can't reduce no more
	    boolean overallChange = false;
		boolean changed = false;

	    int[] masks = board.getMasks(new int[NUM_CELLS]);

	    //Track positions that are not already reduced.
	    ArrayList<Integer> indices = new ArrayList<>();
	    for (int i = 0; i < NUM_CELLS; i++) {
	    	if (board.getValueAt(i) == 0) {
	    		indices.add(i);
	    		masks[i] = ALL;
	    	}
	    }

		do {
			changed = false;
	        for (int i = indices.size() - 1; i >= 0; i--) {
	        	int j = indices.get(i);
	        	if (reduce(masks, j)) {
	                changed = true;
	                overallChange = true;
	                board.setMaskAt(j, masks[j]);
	            }
	        	if (decode(masks[j]) > 0) {
		    		indices.remove(i);
		    	}
	        }
		} while (changed);

	    return overallChange;
	}

	private static boolean reduce(int[] masks, int index) {
	    int initial = masks[index];

	    //check if this cell already contains a single digit
	    if (decode(initial) > 0)
	        return false;

	    int candidates = ALL;
	    candidates = reduceRow(masks, index, candidates);
	    candidates = reduceCol(masks, index, candidates);
	    candidates = reduceRegion(masks, index, candidates);
	    masks[index] = candidates;
	    return (initial - candidates) > 0;
	}

	private static int reduceRegion(int[] masks, int index, int candidates) {
	    for (int i : REGION_INDICES[Board.getRegionForIndex(index)]) {
	        if (i == index)
				continue;

	    	int v = masks[i];
	        if (decode(v) > 0) {
	            if ((candidates ^ v) < candidates) {
	                candidates ^= v;
	            }
	        }
	    }
		return candidates;
	}

	private static int reduceCol(int[] masks, int index, int candidates) {
		int c = index % 9;
	    for (int i : COL_INDICES[c]) {
	        if (i == index)
				continue;

	        int v = masks[i];
	        if (decode(v) > 0) {
	            if ((candidates ^ v) < candidates) {
	                candidates ^= v;
	            }
	        }
	    }
		return candidates;
	}

	private static int reduceRow(int[] masks, int index, int candidates) {
		int r = index / 9;
		for (int i : ROW_INDICES[r]) {
	        if (i == index)
	            continue;

	        int v = masks[i];
	        if (decode(v) > 0) {
	        	if ((candidates ^ v) < candidates) {
	                candidates ^= v;
	            }
	        }
	    }
		return candidates;
	}
}
