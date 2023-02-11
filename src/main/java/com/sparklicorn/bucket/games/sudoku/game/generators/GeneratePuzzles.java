package com.sparklicorn.bucket.games.sudoku.game.generators;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sparklicorn.bucket.games.sudoku.game.Board;

public class GeneratePuzzles {
  public static class GenerationOptions {
    public final static int DEFAULT_NUM_PUZZLES = 1;
    public final static int DEFAULT_NUM_CLUES = 32;
    public final static int DEFAULT_NUM_THREADS = Runtime.getRuntime().availableProcessors();

    public final int numClues;
    public final int numPuzzles;
    public final int numThreads;

    public GenerationOptions(int numClues, int numPuzzles, int numThreads) {
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
    generatePuzzles(options);
  }

  public static void generatePuzzles(GenerationOptions options) {
    generatePuzzles(
      options.numPuzzles,
      options.numClues,
      options.numThreads
    );
  }

  // TODO Move this to Generator, then delete this whole class.
  public static void generatePuzzles(int amount, int clues, int threads) {
    if (threads == 1) {
      for (int n = 0; n < amount; n++) {
        System.out.println(
          SudokuGeneratorService.generatePuzzle(clues).getSimplifiedString()
        );
      }

      return;
    }

    AtomicInteger latch = new AtomicInteger(amount);
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(amount);
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
      threads, // pool size
      threads, // max pool size
      1L,
      TimeUnit.SECONDS,
      workQueue
    );
    pool.prestartAllCoreThreads();

    for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
      pool.submit(() -> {
        while(latch.get() > 0) {
          printPuzzle(SudokuGeneratorService.generatePuzzle(clues), latch);
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
