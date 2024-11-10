package com.metal_pony.bucket.sudoku;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class Adhoc {
    // public static void main(String[] args) {
    //     findInitialBands();
    // }

    static void benchSolving_Sudoku_vs_Sudoku2() {
        // Quick dirty benchmark between Sudoku and Sudoku2
        // searchForSolutions3, searchForSolutionsBranched

        HashMap<String,Integer> puzzlesForBench = new HashMap<>() {{
            put("...45.7...5........4......3.8...3.1.9..241..85.69...3.2..3...7.3...7..........3..", 1463);
            put("....5..89..8...16......1..2..76.3..............1..5..45...6..73.......4..74..89.1", 2361);
            put("..3.5.7.9..7..8.4...8............8.6.8...54.2...8..........932.3.42..6......3.1..", 25339);
            put("12..5..8..7.3.9........7..6...56..9.....4.8......92..1....2...8.6.1.......8...6.5", 996);
            put("1..45...96...1......7...1..3......5.9....531.......6...9.16.......3.4.6.2...7...1", 5076);
            put(".2..56...8..3..56........3..1.2...........64.....9..239.........81.2....26..314..", 3171);
            put(".2.4.6..99...........79213.........1..9...3.........5.3.8....72...5......65.29..4", 4004);
            put("....5..89....3.......2...........9..2......75..9.8.6.2.51...8.6....9...1.92..1.57", 7535);
            put("....5.7..56...8.4...9.7..61...6.....65...94.8..4....2.4.....836.3...7............", 1509);
            put("....56...76....52..95.2...3.......7.2.78...455...9.1...3.....5...8...3.......5...", 2132);
            put("..3.....9.7....65...9.71.345.1..78..9.43.2......54.......9..3............4.1.....", 322);
            put("...4..7......1...6.........3.....8..7.584......8.3..6.5...7....43...51.897.1...3.", 27462);
            put("1.......9.......4...4...2....2.....8..92..4.14.8....9..365...1.8.....5.6..56.8...", 5338);
            put("...45.................8..1..1...4...63......8..8...195...7..8.1.5..9.3.48.16...5.", 1589);
            put(".....6..9........2.84.97....1...23...9..85..62...61.4.3...2........38..4.....4...", 8244);
            put("..3..6........8..69.67..1..5.....96.8.9.....7.67....1....8.....4.8...6......94..3", 25661);
            put("..3.5.7.....2......4891..6.812.3.........5....9..8...........252.5.....1.795.....", 448);
            put("1..4..7.9......3...75.8.6143........8.43...6......4...2..1....6..8.........9.5..2", 3383);
            put("...4..7.9...7.8....681...4.....1.9....6......931.4.....8.2.4...2...6..7....3...9.", 7506);
            put(".2.......9.6.175..........34.....961.....5....7.9.4.......42...237.8...5....3..2.", 243);
        }};

        final int N = 100;
        System.out.printf("     Sudoku    Sudoku2\n");
        for (int n = 0; n < N; n++) {
            Long start1 = System.currentTimeMillis();
            for (Entry<String,Integer> benchCase : puzzlesForBench.entrySet()) {
                Sudoku p = new Sudoku(benchCase.getKey());
                int expectedSolutions = benchCase.getValue();
                AtomicInteger solutionCount = new AtomicInteger();
                p.searchForSolutions3(s -> {
                    solutionCount.incrementAndGet();
                    return true;
                });
                if (expectedSolutions != solutionCount.get()) {
                    throw new RuntimeException("Failed to generate all solutions");
                }
            }
            Long end1 = System.currentTimeMillis();
            System.out.printf(" %10d", end1 - start1);

            Long start2 = System.currentTimeMillis();
            for (Entry<String,Integer> benchCase : puzzlesForBench.entrySet()) {
                Sudoku2 p = new Sudoku2(benchCase.getKey());
                int expectedSolutions = benchCase.getValue();
                AtomicInteger solutionCount = new AtomicInteger();
                p.searchForSolutions3(s -> {
                    solutionCount.incrementAndGet();
                    return true;
                });
                if (expectedSolutions != solutionCount.get()) {
                    throw new RuntimeException("Failed to generate all solutions");
                }
            }
            Long end2 = System.currentTimeMillis();
            System.out.printf(" %10d\n", end2 - start2);
        }

        System.out.println("Done.");
    }

    static class Node2 {
        Sudoku2 sudoku;
        int index = -1;
        int values = -1;
        public Node2(Sudoku2 sudoku) {
            this.sudoku = sudoku;
            if (sudoku.reduce()) {
                // debug("reduced > %s\n", sudoku.toString());
            }
            index = sudoku.pickEmptyCell(0, 27);
            if (index != -1) {
                values = sudoku.candidates[index];
            }
        }
        public Node2 next() {
            if (values <= 0) {
                return null;
            }
            Sudoku2 s = new Sudoku2(sudoku);
            int d = Sudoku2.CANDIDATES_ARR[values][0];
            s.setDigit(index, d);
            values &= ~(Sudoku2.ENCODER[d]);
            return new Node2(s);
        }
    }

    static void findInitialBands() {
        Sudoku2 root = new Sudoku2("123456789--------");
        root.resetEmptyCells();
        Stack<Node2> q = new Stack<>();
        q.push(new Node2(root));
        HashSet<String> fullBandSet = new HashSet<>();

        final int N = Sudoku.DIGITS * 3;
        while (!q.isEmpty()) {
            Node2 top = q.peek();
            Node2 next = top.next();
            if (next == null) {
                String bandStr = top.sudoku.toString().substring(0, N);
                if (fullBandSet.add(bandStr)) {
                    System.out.println(bandStr);
                }
                q.pop();
            } else {
                q.push(next);
            }
        }

        System.out.printf(" -- found %d initial bands --\n", fullBandSet.size());
        System.out.println("Done.");
    }
}
