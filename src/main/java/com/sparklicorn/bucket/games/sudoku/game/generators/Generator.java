package com.sparklicorn.bucket.games.sudoku.game.generators;

import java.util.Stack;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.ArrayDeque;

import java.util.concurrent.ThreadLocalRandom;

import com.sparklicorn.bucket.games.sudoku.game.*;
import com.sparklicorn.bucket.games.sudoku.game.solvers.*;

public class Generator {

    private static class Node {
        Board b;
        private Node[] nexts;
        Node prev;
        boolean visited;

        Node(Board b, Node prev) {
            this.b = b;
            this.prev = prev;
            visited = false;
        }

        void visit() {
            visited = true;
        }

        void kill() { //release memory
            b = null;
            nexts = null;
        }

        private void findNexts() {
            if (nexts == null) {
                nexts = new Node[b.getNumClues()];
                int index = 0;
                for (int i = 0; i < Board.NUM_CELLS; i++) {
                    if (b.getValueAt(i) > 0) {
                        Board bCopy = new Board(b);
                        bCopy.setValueAt(i, 0);
                        nexts[index++] = new Node(bCopy, this);
                    }
                }
            }
        }

        Node[] getNeighbors() {
            if (nexts == null)
                findNexts();
            return nexts;
        }

        Node getNextUnvisited() {
            if (nexts == null)
                findNexts();
            ArrayList<Node> bag = new ArrayList<>();
            for (Node n : nexts) {
                if (n != null && !n.visited) {
                    bag.add(n);
                }
            }
            if (bag.isEmpty())
                return null;
            return bag.get(ThreadLocalRandom.current().nextInt(bag.size()));
        }

        @Override
        public int hashCode() {
            return b.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this)
                return true;
            if (other instanceof Node) {
                Node _other = (Node) other;
                return b.equals(_other.b);
            }
            return false;
        }

    }

    public static List<Board> generatePuzzles(int numClues) {
        return generatePuzzles(numClues, Integer.MAX_VALUE);
    }

    public static Board generatePuzzle(int numClues) {
        List<Board> boards = generatePuzzles(numClues);
        return boards.get(boards.size() - 1);
    }

    //Uses stochastic BFS
    public static List<Board> generatePuzzles2(int numClues, double prob) {
        Queue<Node> q = new ArrayDeque<>();
        Queue<Node> u = new ArrayDeque<>();

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
                //System.out.print('.');
                if (pollCounter > 0 && pollCounter % (80 * dot_increments) == 0) {
                    //System.out.println();
                }
            }

            if (Solver.solvesUniquely(n.b, config)) {
                if (n.b.getNumClues() <= numClues) {
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
                n.kill(); //releases some memory
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
                stack.push(n.b);
                n = n.prev;
            }

            while (!stack.isEmpty()) {
                result.add(stack.pop());
            }
        }

        return result;
    }

    //Uses DFS to locate valid sudoku puzzle.
    public static List<Board> generatePuzzles(int numClues, int maxPops) {
        Stack<Node> stack = new Stack<>();
        //HashSet<Node> visited = new HashSet<>();
        Board config = generateConfig();
        Node root = new Node(config, null);
        stack.push(root);
        //visited.add(root);

        int tp = 0;
        int pops = 0;

        // System.out.println("Starting with " + config.getSimplifiedString());

        while (!stack.isEmpty() && pops < maxPops) {
            Node n = stack.peek();
            n.visit();
            //visited.add(n);

            // System.out.println("Peek > (" + n.b.getNumClues() + ") " + n.b.getSimplifiedString());

            //if more than one solution...
            if (!Solver.solvesUniquely(n.b, config)) {
                // System.out.println("Doesn't solve uniquely.");
                stack.pop();
                n.kill();
                tp++;
                if (++pops == 100) {
                    // System.out.printf("Too many pops (%d)... trying again.%n", tp);
                    if (tp > 0 && tp % 8000 == 0)
                        System.out.println();
                    stack.clear();
                    stack.push(root);
                    pops = 0;
                }
                continue;
            } else if (n.b.getNumClues() <= numClues) { //if target found
                // System.out.println("Target found!");
                break;
            }

            //target number of clues not reached, but current node puzzle has only single solution

            Node next = n.getNextUnvisited();

            if (next != null) {
                stack.push(next);
                //visited.add(next);
            } else {
                // System.out.println("Out of neighbors.");
                stack.pop();

                tp++;
                if (++pops == 100) {
                    // System.out.printf("Too many pops (%d)... trying again.%n", tp);
                    if (tp > 0 && tp % 8000 == 0)
                        System.out.println();
                    stack.clear();
                    stack.push(root);
                    pops = 0;
                }
            }
        }

        // System.out.println();
        // System.out.println("Pops: " + tp);

        List<Board> result = new ArrayList<>();
        if (!stack.isEmpty()) {
            for (Node n : stack)
                result.add(n.b);
        }

        return result;
    }

    //Random permutation of the digits 1 - n, stored in list.
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

    private static void shuffle(int[] array, int numSwaps) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int n = 0; n < numSwaps; n++) {
            int i = rand.nextInt(array.length), j = rand.nextInt(array.length);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

	private static void fillSections(Board board, int mask) {
        int[] list = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
		for (int m = 0; m < 9; m++) {
			if ((mask & (1 << (8 - m))) > 0) {
				shuffle(list, 18);
				int gr = m/3;
			    int gc = m%3;
			    for (int i = 0; i < 9; i++) {
			    	int bi = gr*27 + gc*3 + (i/3)*9 + (i%3);
			    	board.setValueAt(bi, list[i]);
			    }
			}
		}
    }

    public static Set<Board> generateConfigs() {
		//Start with a blank board and generate some of the regions.
		Set<Board> configs = null;

		while (configs == null || configs.isEmpty()) {
            Board b = new Board();

            do {
                b.clear();
                Generator.fillSections(b, 0b101010001);
            } while (!b.isValid());

            //System.out.println("Trying partial config: ");
            //System.out.println(b);
            //System.out.println("Trying partial config: " + b.getSimplifiedString());

			//Attempt to solve the partially-filled board.
			configs = Solver.getAllSolutions(b);

            //System.out.println("Found " + set.size() + " solutions.");
		}

        //System.out.println("Generated config -> " + b.getSimplifiedString());
		return configs;
    }

    public static Board generateConfig() {
        Set<Board> configSet = generateConfigs();
        int size = configSet.size();
        return configSet.toArray(new Board[size])[ThreadLocalRandom.current().nextInt(size)];
    }
}
