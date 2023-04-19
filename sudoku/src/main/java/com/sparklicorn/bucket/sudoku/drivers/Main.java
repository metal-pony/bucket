package com.sparklicorn.bucket.sudoku.drivers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sparklicorn.bucket.sudoku.game.Board;
import com.sparklicorn.bucket.sudoku.game.SudokuUtility;
import com.sparklicorn.bucket.sudoku.game.generators.*;
import com.sparklicorn.bucket.sudoku.game.solvers.Solver;
import com.sparklicorn.bucket.sudoku.puzzles.GeneratedPuzzles;
import com.sparklicorn.bucket.sudoku.drivers.gui.SudokuGuiDemo;

/**
 * Sudoku command-line interface. Commands:
 *
 * `play`
 * Open the Sudoku GUI with a random puzzle.
 * Optional args:
 *    `--clues XX` Number of clues for the puzzle. Default: 27.
 *
 * `generateConfigs`
 * Generate a number of sudoku configurations.
 * Optional args:
 *    `--amount XX` [Default: 1] Number of configurations to generate.
 *    `--normalize` [Default: omitted] Flag to "normalize" the output, swapping values
 *        around such that the first row reads the digits 1-9 consecutively.
 *
 * `generatePuzzles`
 * Generate a number of sudoku puzzles. Optionally multi-threaded.
 * Optional args:
 *    `--amount XX` [Default: 1] Number of puzzles to generate.
 *    `--clues XX` [Default: 27] Number of clues for the puzzles.
 *    `--threads XX` [Default: 1] Number of threads used for generation.
 *        More is not necessarily better.
 *
 * `solve --board 1.3.456.2...(etc)`
 * Search for and output solutions to the given sudoku board.
 *
 * `generate`
 * Can't remember what the flippity flop is this for... Seems to be benchmarking
 * configuration generation and serial/deserialization.
 * Optional args:
 *    `--amount XX` [Default: 1] Number of configurations to generate.
 *
 * `benchmark`
 * Runs multi-threaded puzzle solver benchmarking, using all available processors
 * to solve a preset of 1000 27-clue puzzles. It was meant to show off how fast
 * the algorithms perform, or something.
 * Optional args:
 *    `--verbose` Flag for additional output while the puzzles are being solved.
 *        Probably interferes with the benchmarking since console output
 *        is actually fairly expensive, so the results when using this flag are
 *        probably not super useful.
 */
public class Main {
  public static final String DEFAULT_COMMAND = "generateConfigs";

  private static final class ArgsMap extends HashMap<String,String> {}

  private static final Map<String, Consumer<ArgsMap>> COMMANDS = Collections.unmodifiableMap(Map.of(
    "play", Main::play,
    "generateConfigs", Main::generateConfigs,
    "generatePuzzles", Main::generatePuzzles,
    "solve", Main::solve,
    "generate", Main::generate,
    "benchmark", Main::benchmark
  ));

  private static void play(ArgsMap args) {
    defaultInMap(args, "clues", "27");
    int clues = inBounds(Integer.parseInt(args.get("clues")), 19, 81);
    Board puzzle = Generator.generatePuzzle(clues);
    SudokuGuiDemo.show(puzzle);
  }

  private static void generateConfigs(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    final int numConfigs = Math.max(Integer.parseInt(args.get("amount")), 1);
    final boolean normalize = args.containsKey("normalize");

    for (int n = 0; n < numConfigs; n++) {
      Board config = Generator.generateConfig();

      if (normalize) {
        config = SudokuUtility.normalize(config);
      }

      System.out.println(config.getSimplifiedString());
    }
  }

  private static void generatePuzzles(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    defaultInMap(args, "clues", "27");
    defaultInMap(args, "threads", "1");

    final int amount = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);
    final int clues = inBounds(Integer.parseInt(args.get("clues")), 19, Board.NUM_CELLS);
    final int threads = inBounds(Integer.parseInt(args.get("threads")), 1, 4);

    GeneratePuzzles.generatePuzzles(amount, clues, threads);
  }

  private static void solve(ArgsMap args) {
    final int MIN_CLUES = 16;

    String boardStr = args.get("board");

    if (boardStr == null) {
      System.out.println("Usage: solve --board ...234...657...198(etc)");
      return;
    }

    Board board = new Board(boardStr);

    if (board.getNumClues() < MIN_CLUES) {
      System.out.printf(
        "Puzzle has too few clues (%d); Minimum is %d\n",
        board.getNumClues(),
        MIN_CLUES
      );
      return;
    }

    System.out.println("Searching for solutions...");
    System.out.println(board);

    long startTime = System.currentTimeMillis();
    List<Board> solutions = new ArrayList<>(Solver.getAllSolutions(board));
    long endTime = System.currentTimeMillis();
    System.out.printf(
      "Found %d solution(s) in %s.\n",
      solutions.size(),
      formatDuration(endTime - startTime)
    );

    for (Board solution : solutions) {
      System.out.println(solution.getSimplifiedString());
    }

    // Pretty output when there's a single solution.
    if (solutions.size() == 1) {
      System.out.println(solutions.get(0));
    }
  }

  private static void generate(ArgsMap args) {
    defaultInMap(args, "amount", "1");

    final int numConfigs = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);

    Board[] configs = new Board[numConfigs];
    int interval = numConfigs / 100;
    for (int i = 0; i < numConfigs; i++) {
      if (i > 100 && i % interval == 0) {
        System.out.print('.');
      }
      configs[i] = Generator.generateConfig();
    }
    System.out.println('#');

    try {
      ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      long start = bean.getCurrentThreadCpuTime();
      File stringsFile = new File("test-strings.txt");
      PrintWriter pw = new PrintWriter(stringsFile);
      for (Board b : configs) {
        pw.println(b.getSimplifiedString());
      }
      pw.close();
      long end = bean.getCurrentThreadCpuTime();
      System.out.printf("Wrote configs in %d ms.%n", TimeUnit.NANOSECONDS.toMillis(end - start));

      start = bean.getCurrentThreadCpuTime();
      File serialFile = new File("test-serial.txt");
      FileOutputStream f = new FileOutputStream(serialFile);
      ObjectOutputStream o = new ObjectOutputStream(f);
      for (Board b : configs) {
        o.writeObject(b);
      }
      o.close();
      f.close();
      end = bean.getCurrentThreadCpuTime();
      System.out.printf("Serialized configs in %d ms.%n", TimeUnit.NANOSECONDS.toMillis(end - start));

      start = bean.getCurrentThreadCpuTime();
      Scanner scanner = new Scanner(stringsFile);
      int index = 0;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        configs[index % configs.length] = new Board(line);
      }
      end = bean.getCurrentThreadCpuTime();
      scanner.close();
      System.out.printf("Read configs in %d ms.%n", TimeUnit.NANOSECONDS.toMillis(end - start));

      start = bean.getCurrentThreadCpuTime();
      Object obj;
      int count = 0;
      try (
        FileInputStream fi = new FileInputStream(serialFile);
        ObjectInputStream oi = new ObjectInputStream(fi);
      ) {
        while ((obj = oi.readObject()) != null) {
          configs[count++ % configs.length] = (Board) obj;
        }
      } catch (Exception e) {
        // do nothing
      }
      end = bean.getCurrentThreadCpuTime();
      System.out.printf("Deserialized %d configs in %d ms.%n", count,
      TimeUnit.NANOSECONDS.toMillis(end - start));
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static void benchmark(ArgsMap args) {
    benchy(args.containsKey("verbose"));
  }

  private static int inBounds(int value, int min, int max) {
    return Math.max(
      min,
      Math.min(value, max)
    );
  }

  private static <K,V> void defaultInMap(Map<K,V> map, K key, V defaultValue) {
    V value = map.get(key);
    if (value == null) {
      map.put(key, defaultValue);
    }
  }

  /**
   * Attempts to parse command arguments from the given array.
   * The first element is ignored as it should be the command.
   * Commands should be invoked with the format:
   * <code>command --argName someValue --someOtherArgWithoutValue --example 69</code>
   *
   * @param args
   * @return
   */
  private static ArgsMap parseCommandLineArgs(String[] args) {
    ArgsMap mapped = new ArgsMap();

    if (args != null && args.length > 1) {
      String lastArgKey = null;
      for (int i = 1; i < args.length; i++) {
        String arg = args[i];

        // An arg can be either a key

        if (arg.startsWith("--")) {
          // This arg is a key. Add to the map with an empty value for now.

          lastArgKey = arg.substring(2);

          // Fail if the key contains non-alphabet chars.
          if (!lastArgKey.matches("[a-zA-Z]+")) {
            throw new IllegalArgumentException("Invalid argument format: " + String.join(" ", args));
          }

          mapped.put(lastArgKey, null);
        } else {
          // This arg is a value. Pair it to the last key seen.

          // Fail if there has not been a key yet.
          if (lastArgKey == null) {
            throw new IllegalArgumentException("Invalid argument format: " + String.join(" ", args));
          }

          // Fail if the last key already has a value.
          if (mapped.get(lastArgKey) != null) {
            throw new IllegalArgumentException("Invalid argument format: " + String.join(" ", args));
          }

          mapped.put(lastArgKey, arg);
        }
      }
    }

    return mapped;
  }

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    ArgsMap argMap = parseCommandLineArgs(args);

    String command = DEFAULT_COMMAND;
    if (args != null) {
      if (args.length >= 1) {
        command = args[0];
      }
    }

    if (!COMMANDS.containsKey(command)) {
      System.out.println("Sudoku: Command not recognized.");
      System.exit(1);
    }

    COMMANDS.get(command).accept(argMap);
  }

  // TODO generalize REPL
  // public static void repl() {
  //   Scanner scanner = new Scanner(System.in);
  //   System.out.println("Sudoku. \"help\" to list commands, \"exit\" or Ctrl+C to exit.");
  //   String line = scanner.nextLine().trim().toLowerCase();
  //   while (!line.equals("exit")) {
  //     switch (line) {
  //       case "help":
  //         System.out.println("""

  //         """);
  //         break;

  //       default:
  //         break;
  //     }
  //   }
  //   scanner.close();
  // }

  private static void benchy(boolean verbose) {
    List<Board> boards = GeneratedPuzzles.convertStringsToBoards(GeneratedPuzzles.PUZZLES_24_1000);

    System.out.printf("%d boards loaded.%n", boards.size());

    CountDownLatch countDownLatch = new CountDownLatch(boards.size());
    List<Runnable> timedBoardSolvers = new ArrayList<>();
    List<Long> solveTimes = Collections.synchronizedList(new ArrayList<>());
    for (Board b : boards) {
      timedBoardSolvers.add(() -> {
        long cpuTime = timeCpuExecution(() -> {
          Board solution = Solver.solve(b);
          if (verbose) {
            System.out.printf("%s  =>  %s%n", b.getSimplifiedString(), solution.getSimplifiedString());
          }
        });
        // System.out.printf("Puzzle solved in %d ms.%n",
        // TimeUnit.NANOSECONDS.toMillis(cpuTime));
        solveTimes.add(cpuTime);
        countDownLatch.countDown();
      });
    }

    final int numThreads = Runtime.getRuntime().availableProcessors();
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1000);
    ThreadPoolExecutor t = new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.SECONDS, workQueue);
    t.prestartAllCoreThreads();

    final long startRealTime = System.currentTimeMillis();
    for (Runnable solver : timedBoardSolvers) {
      t.submit(solver);
    }

    try {
      countDownLatch.await(1L, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    t.shutdownNow();

    if (countDownLatch.getCount() > 0) {
      System.out.println("TIMEOUT -- DID NOT SOLVE ALL PUZZLES IN TIME (1 MINUTE)");
      return;
    }

    long totalCpuTime = 0L;
    for (long time : solveTimes) {
      totalCpuTime += time;
    }
    System.out.printf(
    "%nReal time to solve all puzzles: %s.%n",
    formatDuration(System.currentTimeMillis() - startRealTime)
    );
    System.out.printf(
    "Total cpu time to solve all puzzles: %s [%s / thread].%n",
    formatDuration(TimeUnit.NANOSECONDS.toMillis(totalCpuTime)),
    formatDuration(TimeUnit.NANOSECONDS.toMillis(totalCpuTime / numThreads))
    );
    System.out.printf("Using %d threads.%n", numThreads);

    System.out.println();
    System.out.print("Solving all single-threaded... ");
    long cpuTimeSingleThreaded = 0L;
    for (Board b : boards) {
      cpuTimeSingleThreaded += timeCpuExecution(() -> {
        Solver.solve(b);
      });
    }
    System.out.println("Done.");
    System.out.printf(
    "Total cpu time to solve all puzzles with single-thread: %s.%n",
    formatDuration(TimeUnit.NANOSECONDS.toMillis(cpuTimeSingleThreaded))
    );
  }

  private static String formatDuration(long milli) {
    long mins = milli / 1000L / 60L;
    long secs = (milli / 1000L) % 60L;
    milli %= 1000L;

    String secString = String.format("%d.%d s", secs, milli);
    if (mins > 0L) {
      return String.format("%d m   %s", mins, secString);
    }

    return secString;
  }

  private static long timeCpuExecution(Runnable runnable) {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    long start = bean.getCurrentThreadCpuTime();
    runnable.run();
    long end = bean.getCurrentThreadCpuTime();
    return end - start;
  }
}
