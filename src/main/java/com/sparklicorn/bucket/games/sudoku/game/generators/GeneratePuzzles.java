package com.sparklicorn.bucket.games.sudoku.game.generators;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sparklicorn.bucket.games.sudoku.game.Board;

public class GeneratePuzzles {

    private static class GenerationOptions {
        public final static int DEFAULT_NUM_PUZZLES = 1;
        public final static int DEFAULT_NUM_CLUES = 32;
        public final static int DEFAULT_NUM_THREADS = Runtime.getRuntime().availableProcessors();

        public final int numClues;
        public final int numPuzzles;
        public final int numThreads;

        GenerationOptions(int numClues, int numPuzzles, int numThreads) {
            this.numClues = numClues;
            this.numPuzzles = numPuzzles;
            this.numThreads = numThreads;
        }

        static GenerationOptions parseFromArgs(String[] args) {
            int numPuzzles = DEFAULT_NUM_PUZZLES;
            int numClues = DEFAULT_NUM_CLUES;
            int numThreads = DEFAULT_NUM_THREADS;
            if (args != null) {
                if (args.length >= 1) {
                    numPuzzles = Integer.parseInt(args[0]);
                }
                if (args.length >= 2) {
                    numClues = Integer.parseInt(args[1]);
                }
                if (args.length >= 3) {
                    numThreads = Integer.parseInt(args[2]);
                }
            }
            return new GenerationOptions(
                numClues,
                numPuzzles,
                numThreads
            );
        }
    }

    public static void main(String[] args) {
        GenerationOptions options = GenerationOptions.parseFromArgs(args);

        if (options.numThreads == 1) {
            for (int n = 0; n < options.numPuzzles; n++) {
                System.out.println(
                    Generator.generatePuzzle(options.numClues).getSimplifiedString()
                );
            }
            return;
        }

        AtomicInteger latch = new AtomicInteger(options.numPuzzles);
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(options.numPuzzles);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(options.numThreads, options.numThreads, 1L, TimeUnit.SECONDS, workQueue);
        pool.prestartAllCoreThreads();

        for (int threadIndex = 0; threadIndex < options.numThreads; threadIndex++) {
            pool.submit(() -> {
                while(latch.get() > 0) {
                    printPuzzle(Generator.generatePuzzle(options.numClues), latch);
                }
            });
        }

        pool.shutdown();
    }

    public static synchronized void printPuzzle(Board puzzle, AtomicInteger latch) {
        if (latch.get() > 0) {
            System.out.println(puzzle.getSimplifiedString());
            latch.decrementAndGet();
        }
    }
}
