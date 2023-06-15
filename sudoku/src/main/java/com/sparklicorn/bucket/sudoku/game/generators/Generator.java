package com.sparklicorn.bucket.sudoku.game.generators;

import java.util.Stack;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.ArrayDeque;

import java.util.concurrent.ThreadLocalRandom;

import com.sparklicorn.bucket.sudoku.game.*;
import com.sparklicorn.bucket.sudoku.game.solvers.*;
import com.sparklicorn.bucket.util.Shuffler;

/**
* Responsible for generating Sudoku configurations and puzzles.
*/
public class Generator {

  private static class Node {
    Board board;
    private Node[] nexts;
    Node prev;
    boolean visited;

    Node(Board board, Node prev) {
      this.board = board;
      this.prev = prev;
      visited = false;
    }

    Node(Board board) {
      this(board, null);
    }

    void visit() {
      visited = true;
    }

    void dispose() { //release memory
      board = null;
      nexts = null;
    }

    private void findNexts() {
      if (nexts == null) {
        nexts = new Node[board.getNumClues()];
        int index = 0;
        for (int i = 0; i < Board.NUM_CELLS; i++) {
          if (board.getValueAt(i) > 0) {
            Board bCopy = new Board(board);
            bCopy.setValueAt(i, 0);
            nexts[index++] = new Node(bCopy, this);
          }
        }
      }
    }

    Node[] getNeighbors() {
      if (nexts == null) {
        findNexts();
      }

      return nexts;
    }

    /**
     * Attempts to get a random, unvisited neighbor of this node.
     * Populates the list of neighbors for this node if it does not yet exist.
     *
     * @return A random unvisited neighbor node.
     */
    Node getNextUnvisited() {
      if (nexts == null) {
        findNexts();
      }

      ArrayList<Node> bag = new ArrayList<>();
      for (Node next : nexts) {
        if (next != null && !next.visited) {
          bag.add(next);
        }
      }
      if (bag.isEmpty()) {
        return null;
      }

      return bag.get(ThreadLocalRandom.current().nextInt(bag.size()));
    }

    @Override
    public int hashCode() {
      return board.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }

      if (other instanceof Node) {
        return board.equals(((Node) other).board);
      }

      return false;
    }
  }

  public static List<Board> generatePuzzles(int numClues) {
    return generatePuzzles(numClues, 1L << 34);
  }

  public static Board generatePuzzle(int numClues) {
    List<Board> boards = generatePuzzles(numClues);
    return boards.isEmpty() ? null : boards.get(boards.size() - 1);
  }

  //Uses stochastic BFS
  public static List<Board> generatePuzzles2(int numClues, double prob) {
    Queue<Node> q = new ArrayDeque<>();
    Queue<Node> u = new ArrayDeque<>();
    int[] _board = new int[Board.NUM_CELLS];

    Board config = generateConfig();

    Node root = new Node(config, null);
    q.offer(root);

    int pollCounter = 0;
    final int dot_increments = 10000;

    Node n = null;
    boolean found = false;

    while (!found && !q.isEmpty() || !u.isEmpty()) {
      if (q.isEmpty()) {
        Queue<Node> temp = q;
        q = u;
        u = temp;
      }

      n = q.poll();
      n.visit();

      if (++pollCounter % dot_increments == 0) {
        System.out.print('.');
        if (pollCounter > 0 && pollCounter % (80 * dot_increments) == 0) {
          System.out.println();
        }
      }

      n.board.getMasks(_board);
      if (Solver.solvesUniquely(_board)) {
        if (n.board.getNumClues() <= numClues) {
          //System.out.println("Target found!");
          found = true; //break out of loop

        } else {
          if (ThreadLocalRandom.current().nextDouble() < prob) {
            for (Node next : n.getNeighbors()) {
              if (!next.visited) {
                q.offer(next);
              }
            }
          } else {
            for (Node next : n.getNeighbors()) {
              if (!next.visited) {
                u.offer(next);
              }
            }
          }
        }
      } else {
        n.dispose(); //releases some memory
      }

    }

    //System.out.println();
    //System.out.println("Polls: " + pollCounter);

    //if search criteria found, then n should point to
    //node holding target board.

    List<Board> result = new ArrayList<>();

    if (found) {
      Stack<Board> stack = new Stack<>();
      while (n != null) {
        stack.push(n.board);
        n = n.prev;
      }

      while (!stack.isEmpty()) {
        result.add(stack.pop());
      }
    }

    return result;
  }

  // Uses DFS to locate valid sudoku puzzle.
  public static List<Board> generatePuzzles(int numClues, long maxPops) {
    Stack<Node> puzzleStack = new Stack<>();
    Board config = generateConfig();
    Node rootNode = new Node(config);
    int[] _board = new int[Board.NUM_CELLS];
    puzzleStack.push(rootNode);

    long numPops = 0L; // Number of pops. If the search resets, so does this.

    while (!puzzleStack.isEmpty() && numPops < maxPops) {
      Node puzzleNode = puzzleStack.peek();
      Board puzzle = puzzleNode.board;
      puzzleNode.visit();

      puzzle.getMasks(_board);
      if (!Solver.solvesUniquely(_board)) {
        puzzleStack.pop();
        puzzleNode.dispose();

        // TODO #24 explore whether it's possible to keep a history for each node,
        //  i.e. track which cells were attempted to be removed.
        //  Then, this won't need any sort of restart fail-safe.

        // After a certain number of pops, restart the search. This ensures that
        // that the algorithm won't continue to try to remove cells when there is
        // no path to a valid puzzle.
        if (++numPops == 100L) {
          puzzleStack.clear();
          puzzleStack.push(rootNode);
          numPops = 0L;
        }

        continue;
      }

      if (puzzle.getNumClues() <= numClues) {
        break;
      }

      Node next = puzzleNode.getNextUnvisited();
      if (next != null) {
        puzzleStack.push(next);
      } else {
        puzzleStack.pop();

        if (++numPops == 100L) {
          puzzleStack.clear();
          puzzleStack.push(rootNode);
          numPops = 0L;
        }
      }
    }

    List<Board> result = new ArrayList<>();
    if (!puzzleStack.isEmpty()) {
      for (Node n : puzzleStack) {
        result.add(n.board);
      }
    }

    return result;
  }

  /**
   * Populates the given list with a random permutation of numbers 1 - n.
   * TODO #68 move to some other utility
   *
   * @param n
   * @param list
   * @return The passed list, for convenience.
   */
  @SuppressWarnings("unused")
  private static List<Integer> randPerm(int n, List<Integer> list) {
    ThreadLocalRandom rand = ThreadLocalRandom.current();
    ArrayList<Integer> bag = new ArrayList<>(n);

    for (int i = 1; i <= n; i++) {
      bag.add(i);
    }

    while (!bag.isEmpty()) {
      list.add(bag.remove(rand.nextInt(bag.size())));
    }

    return list;
  }

  // TODO #68 move to some other utility
  // Utility to generate array containing range of integers [start, start + n].
  private static int[] getSeries(int n, int start) {
    int[] series = new int[n];

    for (int i = 0; i < n; i++) {
      series[i] = start + i;
    }

    return series;
  }

  private static void fillSections(Board board, int mask) {
    int[] list = getSeries(Board.NUM_DIGITS, 1);
    for (int m = 0; m < Board.NUM_DIGITS; m++) {
      if ((mask & (1 << (8 - m))) > 0) {
        Shuffler.shuffle(list);
        int gr = m/3;
        int gc = m%3;
        for (int i = 0; i < Board.NUM_DIGITS; i++) {
          int bi = gr*27 + gc*3 + (i/3)*9 + (i%3);
          board.setValueAt(bi, list[i]);
        }
      }
    }
  }

  public static Set<Board> generateConfigs() {
    Set<Board> configs = null;

    while (configs == null || configs.isEmpty()) {
      Board b = new Board();

      do {
        b.clear();
        fillSections(b, 0b101010001);
      } while (!b.isValid());

      configs = Solver.getAllSolutions(b);
    }

    return configs;
  }

  public static Board generateConfig() {
    Set<Board> configSet = generateConfigs();
    int size = configSet.size();
    return configSet.toArray(new Board[size])[ThreadLocalRandom.current().nextInt(size)];
  }
}
