package com.metal_pony.bucket.sudoku.drivers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.metal_pony.bucket.sudoku.GeneratedPuzzles;
import com.metal_pony.bucket.sudoku.Sudoku;
import com.metal_pony.bucket.sudoku.SudokuSieve.SudokuSearch;
import com.metal_pony.bucket.sudoku.drivers.gui.SudokuGuiDemo;

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
 * `solve --puzzle 1.3.456.2...(etc)`
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
    "benchmark", Main::benchmark,
    "generateBands", Main::generateInitialBands,
    "adhoc", Main::doThing, // TODO Temporary - for testing / experimentation
    "search", Main::search
  ));

  static boolean verbose;

  private static void play(ArgsMap args) {
    defaultInMap(args, "clues", "27");
    int clues = inBounds(Integer.parseInt(args.get("clues")), 19, 81);
    Sudoku puzzle = Sudoku.generatePuzzle(Sudoku.configSeed(), clues);
    SudokuGuiDemo.show(puzzle);
  }

  private static void generateConfigs(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    final int numConfigs = Math.max(Integer.parseInt(args.get("amount")), 1);
    final boolean normalize = args.containsKey("normalize");

    for (int n = 0; n < numConfigs; n++) {
      // Board config = Generator.generateConfig();
      Sudoku seed = Sudoku.configSeed();
      seed.searchForSolutions3((solution) -> {
        if (normalize) {
          solution.normalize();
        }

        System.out.println(solution.toString());

        return false;
      });
    }
  }

  private static void generatePuzzles(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    defaultInMap(args, "clues", "27");
    defaultInMap(args, "threads", "1");

    final int amount = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);
    final int clues = inBounds(Integer.parseInt(args.get("clues")), 19, Sudoku.SPACES);
    final int threads = inBounds(Integer.parseInt(args.get("threads")), 1, 4);

    // TODO Implement analog to GeneratePuzzles.generatePuzzles(amount, clues, threads);
  }

  private static void solve(ArgsMap args) {
    final int MIN_CLUES = 16;
    final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    // defaultInMap(args, "threads", Integer.toString(MAX_THREADS));

    // TODO Finish implementing timeout - decide on limits
    final long DEFAULT_TIMEOUT = 60L;
    final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
    defaultInMap(args, "timeout", Long.toString(DEFAULT_TIMEOUT));

    String boardStr = args.get("puzzle");
    boolean usingThreads = args.containsKey("threads");
    String threadsStr = usingThreads ? args.get("threads") : "1";
    final int threads = (usingThreads && threadsStr == null) ?
      MAX_THREADS :
      inBounds(Integer.parseInt(threadsStr), 1, MAX_THREADS);

    // final int timeout = inBounds(Integer.parseInt(args.get("timeout")), 1, MAX_THREADS);

    if (boardStr == null) {
      System.out.println("Usage: solve --puzzle ...234...657...198(etc)");
      return;
    }

    Sudoku board = new Sudoku(boardStr);

    if (board.numClues() < MIN_CLUES) {
      System.out.printf(
        "Puzzle has too few clues (%d); Minimum is %d\n",
        board.numClues(),
        MIN_CLUES
      );
      return;
    }

    Sudoku puzzle = new Sudoku(boardStr);

    System.out.println("Searching for solutions...");
    System.out.println(puzzle.toString());
    System.out.println("=".repeat(Sudoku.SPACES));

    final String newline = System.lineSeparator();

    if (usingThreads) {
      // List<PrintWriter> writers = new ArrayList<>();
      ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
      try {
        System.out.println("🧵".repeat(threads));
        // pool.prestartAllCoreThreads();
        Object lock = new Object();
        // List<Consumer<List<Sudoku>>> threadWriters = new ArrayList<>();
        // for (int t = 0; t < threads; t++) {
        //   PrintWriter pw = new PrintWriter(String.format("adhoc-%d.txt", t));
        //   // writers.add(pw);
        //   threadWriters.add((solutions) -> {
        //     StringBuilder strb = new StringBuilder();
        //     solutions.forEach(solution -> {
        //       strb.append(solution.toString());
        //       strb.append(newline);
        //     });
        //     // System.out.println(strb.toString());
        //     pw.write(strb.toString());
        //   });
        // }

        AtomicInteger t = new AtomicInteger();
        AtomicLong count = new AtomicLong();
        AtomicLong billions = new AtomicLong();
        Consumer<List<Sudoku>> solutionsCallback = (solutions) -> {
          // TODO FIX -- threads will block while waiting for this lock
          synchronized (lock) {
            // StringBuilder strb = new StringBuilder();
            // solutions.forEach(solution -> {
            //   strb.append(solution.toString());
            //   strb.append(newline);
            // });
            // System.out.println(strb.toString());
            // writers.get(t.getAndIncrement() % threads).write(strb.toString());
            long c = count.addAndGet(solutions.size());
            // long b = c / 10_000_000L;
            // if (b > billions.get()) {
              System.out.printf("Found %d so far...\n", c);
            //   billions.incrementAndGet();
            // }
          }
        };
        // puzzle.searchForSolutionsAsync(pool, solutionsCallback, 1<<20);

        puzzle.searchForSolutionsAsync(pool, count);
        pool.shutdown();
        pool.awaitTermination(1L, TimeUnit.DAYS);
        System.out.printf(" -- Found %d solutions -- \n", count.get());
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      } finally {
        pool.shutdownNow();
        // writers.forEach(writer -> {
        //   writer.close();
        // });
      }
    } else {
      puzzle.searchForSolutions3(solution -> {
        System.out.println(solution.toString());
        return true;
      });
    }

    // List<Future<List<Sudoku>>> results = puzzle.searchForSolutions4();

    // while (!results.isEmpty()) {
    //   Future<List<Sudoku>> nestedResult = results.get(0);
    //   try {
    //     List<Sudoku> solutionsBatch = nestedResult.get(5L, TimeUnit.MINUTES);
    //     solutionsBatch.forEach(solution -> {
    //       System.out.println(solution.toString());
    //     });
    //     results.remove(0);
    //   } catch (Exception ex) {
    //     ex.printStackTrace();
    //   }
    // }


    // // FORMER SOLVE DRIVER
    // long startTime = System.currentTimeMillis();
    // List<Board> solutions = new ArrayList<>(Solver.getAllSolutions(board));
    // long endTime = System.currentTimeMillis();
    // System.out.printf(
    //   "Found %d solution(s) in %s.\n",
    //   solutions.size(),
    //   formatDuration(endTime - startTime)
    // );

    // for (Board solution : solutions) {
    //   System.out.println(solution.getSimplifiedString());
    // }

    // // Pretty output when there's a single solution.
    // if (solutions.size() == 1) {
    //   System.out.println(solutions.get(0));
    // }
  }

  private static void generate(ArgsMap args) {
    defaultInMap(args, "amount", "1");

    final int numConfigs = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);

    Sudoku[] configs = new Sudoku[numConfigs];
    int interval = numConfigs / 100;
    for (int i = 0; i < numConfigs; i++) {
      if (i > 100 && i % interval == 0) {
        System.out.print('.');
      }
      configs[i] = Sudoku.configSeed().firstSolution();
    }
    System.out.println('#');

    try {
      ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      long start = bean.getCurrentThreadCpuTime();
      File stringsFile = new File("test-strings.txt");
      PrintWriter pw = new PrintWriter(stringsFile);
      for (Sudoku b : configs) {
        pw.println(b.toString());
      }
      pw.close();
      long end = bean.getCurrentThreadCpuTime();
      System.out.printf("Wrote configs in %d ms.%n", TimeUnit.NANOSECONDS.toMillis(end - start));

      start = bean.getCurrentThreadCpuTime();
      File serialFile = new File("test-serial.txt");
      FileOutputStream f = new FileOutputStream(serialFile);
      ObjectOutputStream o = new ObjectOutputStream(f);
      for (Sudoku b : configs) {
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
        configs[index % configs.length] = new Sudoku(line);
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
          configs[count++ % configs.length] = (Sudoku) obj;
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

    verbose = argMap.containsKey("v");

    COMMANDS.get(command).accept(argMap);
  }

  // TODO #67 Create general REPL tool
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
    List<Sudoku> boards = GeneratedPuzzles.convertStringsToBoards(GeneratedPuzzles.PUZZLES_24_1000);

    System.out.printf("%d boards loaded.%n", boards.size());

    CountDownLatch countDownLatch = new CountDownLatch(boards.size());
    List<Runnable> timedBoardSolvers = new ArrayList<>();
    List<Long> solveTimes = Collections.synchronizedList(new ArrayList<>());
    for (Sudoku b : boards) {
      timedBoardSolvers.add(() -> {
        long cpuTime = timeCpuExecution(() -> {
          Sudoku solution = b.firstSolution();
          if (verbose) {
            System.out.printf("%s  =>  %s%n", b.toString(), solution.toString());
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
    for (Sudoku b : boards) {
      cpuTimeSingleThreaded += timeCpuExecution(() -> {
        b.firstSolution();
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

  static class Node2 {
    Sudoku sudoku;
    int index = -1;
    int values = -1;
    public Node2(Sudoku sudoku) {
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
      Sudoku s = new Sudoku(sudoku);
      int d = Sudoku.CANDIDATES_ARR[values][0];
      s.setDigit(index, d);
      values &= ~(Sudoku.ENCODER[d]);
      return new Node2(s);
    }
  }

  public static void generateInitialBands(ArgsMap args) {
    HashSet<String> fullBandSet = new HashSet<>();
    final int N = Sudoku.DIGITS * 3;
    long time = timeCpuExecution(() -> {
      Sudoku root = new Sudoku("123456789--------");
      root.resetEmptyCells();
      Stack<Node2> q = new Stack<>();
      q.push(new Node2(root));

      while (!q.isEmpty()) {
        Node2 top = q.peek();
        Node2 next = top.next();
        if (next == null) {
          boolean hasEmptyInBand = top.sudoku.pickEmptyCell(0, N) >= 0;
          if (!hasEmptyInBand) {
            String bandStr = top.sudoku.toString().substring(0, N);
            if (fullBandSet.add(bandStr)) {
              if (verbose) {
                System.out.println(bandStr);
              }
            }
          }
          q.pop();
        } else {
          q.push(next);
        }
      }
    });

    if (verbose) {
      System.out.printf(
        " -- found %d initial bands in %s ms --\n",
        fullBandSet.size(),
        TimeUnit.NANOSECONDS.toMillis(time)
      );

      System.out.println("Reducing bands...");
    }

    long startTime = System.currentTimeMillis();
    Set<String> reducedBandSet = reduceFullBandSet(fullBandSet);
    long endTime = System.currentTimeMillis();
    if (verbose) {
      System.out.printf(" -- reduced bands to %d in %d ms --\n", reducedBandSet.size(), (endTime - startTime));
    }
    reducedBandSet.forEach(bandStr -> {
      System.out.println(bandStr);
    });
  }

  public static Set<String> reduceFullBandSet(Set<String> fullBandSet) {
    // TODO Reduce fullBandSet by discovering and removing transforms
    // For each BAND:
    //  new queue, new hashset<string> to track seen elements, add BAND
    //  while queue not empty:
    //    b = poll
    //    // always normalize after transform, before adding to queue
    //    add unseen block permutations to queue,
    //    add unseen row permutations to queue,
    //    add unseen column permutations to queue,
    //    band -> config -> search for UAs(level 2? 3?) -> when found, if (bandMask & ua) == ua -> if unseen, add to queue

    List<String> allBands = new ArrayList<>(fullBandSet);
    HashSet<String> reducedBands = new HashSet<>();
    final int N = Sudoku.DIGITS * 3;

    while (!allBands.isEmpty()) {
      String band = allBands.remove(allBands.size() - 1);
      // String bandPuzzleStr = band + "0".repeat(Sudoku.SPACES - band.length());
      // Sudoku bandPuzzle = new Sudoku(bandPuzzleStr);
      HashSet<String> seen = new HashSet<>();
      Queue<String> q = new LinkedList<>();
      seen.add(band);
      q.offer(band);
      reducedBands.add(band);

      // TODO NOT FEASIBLE TO COUNT SOLUTIONS
      // int rootCount = countSolutions(new Sudoku(band + "0".repeat(Sudoku.SPACES - band.length())));
      // if (verbose) {
      //   System.out.printf("Transforming band %s, all transforms should have %d solutions:\n", band, rootCount);
      // }

      while (!q.isEmpty()) {
        String bStr = q.poll() + "0".repeat(Sudoku.SPACES - band.length());

        // Transforms
        Sudoku[] transforms = new Sudoku[] {
          new Sudoku(bStr).swapStacks(1, 2),
          new Sudoku(bStr).swapStacks(0, 1),
          new Sudoku(bStr).swapStacks(0, 1).swapStacks(1, 2),
          new Sudoku(bStr).swapStacks(0, 2).swapStacks(1, 2),
          new Sudoku(bStr).swapStacks(0, 2),

          new Sudoku(bStr).swapBandRows(0, 1, 2),
          new Sudoku(bStr).swapBandRows(0, 0, 1),
          new Sudoku(bStr).swapBandRows(0, 0, 1).swapBandRows(0, 1, 2),
          new Sudoku(bStr).swapBandRows(0, 0, 2).swapBandRows(0, 1, 2),
          new Sudoku(bStr).swapBandRows(0, 0, 2),

          new Sudoku(bStr).swapStackCols(0, 1, 2),
          new Sudoku(bStr).swapStackCols(0, 0, 1),
          new Sudoku(bStr).swapStackCols(0, 0, 1).swapStackCols(0, 1, 2),
          new Sudoku(bStr).swapStackCols(0, 0, 2).swapStackCols(0, 1, 2),
          new Sudoku(bStr).swapStackCols(0, 0, 2),

          new Sudoku(bStr).swapStackCols(1, 1, 2),
          new Sudoku(bStr).swapStackCols(1, 0, 1),
          new Sudoku(bStr).swapStackCols(1, 0, 1).swapStackCols(1, 1, 2),
          new Sudoku(bStr).swapStackCols(1, 0, 2).swapStackCols(1, 1, 2),
          new Sudoku(bStr).swapStackCols(1, 0, 2),

          new Sudoku(bStr).swapStackCols(2, 1, 2),
          new Sudoku(bStr).swapStackCols(2, 0, 1),
          new Sudoku(bStr).swapStackCols(2, 0, 1).swapStackCols(2, 1, 2),
          new Sudoku(bStr).swapStackCols(2, 0, 2).swapStackCols(2, 1, 2),
          new Sudoku(bStr).swapStackCols(2, 0, 2)
        };

        for (Sudoku t : transforms) {
          String tStr = t.normalize().toString().substring(0, N);
          if (!seen.contains(tStr)) {
            seen.add(tStr);
            q.offer(tStr);

            // TODO NOT FEASIBLE TO COUNT SOLUTIONS
            // int count = countSolutions(new Sudoku(tStr));
            // if (verbose) {
            //   System.out.printf("%s [%d] %s\n", (rootCount == count) ? "  " : "🚨", count, tStr);
            // }
          }
        }

        // TODO Additional symmetries can be found by locating UAs within the band

        // AtomicReference<Sudoku> atomicConfig = new AtomicReference<>();
        // bandPuzzle.searchForSolutions3(solution -> {
        //   atomicConfig.set(solution);
        //   return false;
        // });
        // Sudoku c = atomicConfig.get();
        // SudokuSieve sieve = new SudokuSieve(c.getBoard());
        // BigInteger bandMask = new BigInteger("1".repeat(N) + "0".repeat(Sudoku.SPACES - N), 2);
        // for (int r = Sudoku.DIGIT_COMBOS_MAP[2].length - 1; r >= 0; r--) {
        //   BigInteger pMask = c.maskForDigits(Sudoku.DIGIT_COMBOS_MAP[2][r]);
        //   sieve.addFromFilter(pMask, (solution) -> {
        //     // TODO item may need to be inverted
        //     BigInteger item = c.diff2(solution);
        //     if (item.equals(item.and(bandMask))) {
        //       String tStr = solution.normalize().toString().substring(0, N);
        //       if (!seen.contains(tStr)) {
        //         seen.add(tStr);
        //         q.offer(tStr);
        //       }
        //     }
        //   });
        // }
      }

      int sizeBefore = allBands.size();
      allBands.removeAll(seen);
      int sizeAfter = allBands.size();
      if (verbose) {
        System.out.printf("Removed %d permuted bands, (%d remaining).\n", sizeBefore - sizeAfter, allBands.size());
      }
    }

    if (verbose) {
      System.out.printf(
        "Done.\nRemoved %d permuted bands in total.\nReduced band set size: %d.\n",
        fullBandSet.size() - reducedBands.size(),
        reducedBands.size()
      );
    }

    return reducedBands;
  }

  public static List<String> getBandPermutations(String band) {
    String bStr = band + "0".repeat(54);

    // Transforms
    Sudoku[] transforms = new Sudoku[] {
      new Sudoku(bStr).swapStacks(1, 2),
      new Sudoku(bStr).swapStacks(0, 1),
      new Sudoku(bStr).swapStacks(0, 1).swapStacks(1, 2),
      new Sudoku(bStr).swapStacks(0, 2).swapStacks(1, 2),
      new Sudoku(bStr).swapStacks(0, 2),

      new Sudoku(bStr).swapBandRows(0, 1, 2),
      new Sudoku(bStr).swapBandRows(0, 0, 1),
      new Sudoku(bStr).swapBandRows(0, 0, 1).swapBandRows(0, 1, 2),
      new Sudoku(bStr).swapBandRows(0, 0, 2).swapBandRows(0, 1, 2),
      new Sudoku(bStr).swapBandRows(0, 0, 2),

      new Sudoku(bStr).swapStackCols(0, 1, 2),
      new Sudoku(bStr).swapStackCols(0, 0, 1),
      new Sudoku(bStr).swapStackCols(0, 0, 1).swapStackCols(0, 1, 2),
      new Sudoku(bStr).swapStackCols(0, 0, 2).swapStackCols(0, 1, 2),
      new Sudoku(bStr).swapStackCols(0, 0, 2),

      new Sudoku(bStr).swapStackCols(1, 1, 2),
      new Sudoku(bStr).swapStackCols(1, 0, 1),
      new Sudoku(bStr).swapStackCols(1, 0, 1).swapStackCols(1, 1, 2),
      new Sudoku(bStr).swapStackCols(1, 0, 2).swapStackCols(1, 1, 2),
      new Sudoku(bStr).swapStackCols(1, 0, 2),

      new Sudoku(bStr).swapStackCols(2, 1, 2),
      new Sudoku(bStr).swapStackCols(2, 0, 1),
      new Sudoku(bStr).swapStackCols(2, 0, 1).swapStackCols(2, 1, 2),
      new Sudoku(bStr).swapStackCols(2, 0, 2).swapStackCols(2, 1, 2),
      new Sudoku(bStr).swapStackCols(2, 0, 2)
    };

    ArrayList<String> result = new ArrayList<>();
    for (Sudoku t : transforms) {
      String tStr = t.normalize().toString().substring(0, 27);
      result.add(tStr);
    }
    return result;
  }

  public static int countSolutions(Sudoku p) {
    AtomicInteger count = new AtomicInteger();
    p.searchForSolutions3(s -> {
      count.incrementAndGet();
      return true;
    });

    return count.get();
  }

  public static void doThing(ArgsMap args) {
    // Sudoku p = new Sudoku("123456789478932615659817243......................................................");
    // p.searchForSolutions3(s->{
    //     System.out.println(s.toString());
    //     return true;
    // });
    // Sudoku.main2(null);
  }

  public static void search(ArgsMap args) {
    defaultInMap(args, "numClues", Integer.toString(Sudoku.MIN_CLUES));

    String gridStr = args.get("grid");
    int numClues = inBounds(Integer.parseInt(args.get("numClues")), Sudoku.MIN_CLUES, 24);

    Sudoku grid = new Sudoku(gridStr);

    System.out.printf("Confirm grid to search for %d-clue puzzles for:\n", numClues);
    System.out.println(grid.toFullString());
    System.out.println("\n ➡️ Press ENTER to continue...\n");
    try (Scanner scanner = new Scanner(System.in)) {
      scanner.nextLine();
      // Sudoku.completeSearch(grid, 17);
    } catch (Exception ex) {
      ex.printStackTrace(System.out);
      return;
    }

    int threads = Runtime.getRuntime().availableProcessors() / 2;
    ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    // temp
    pool.shutdown();

    // List<String> results = Collections.synchronizedList(new ArrayList<>());
    Object sysoutLock = new Object();
    AtomicLong counter = new AtomicLong();
    Set<BigInteger> results = Collections.synchronizedSet(new HashSet<BigInteger>());

    // TODO Adapt this to use DFS instead of BFS

    Queue<BigInteger> q = new LinkedList<>();
    SudokuSearch _search = new SudokuSearch(grid);
    HashSet<BigInteger> seen = new HashSet<>();
    _search.search(3, (maskToCheck) -> {
      int bitCount = maskToCheck.bitCount();
      if (bitCount > numClues) {
        Sudoku puzzle = grid.filter(maskToCheck);
        int flag = puzzle.solutionsFlag();
        if (flag == 1) {
          results.add(maskToCheck);
          System.out.printf("⭐️ * [%2d] %s\n", bitCount, puzzle.toString());
        }
        return;
      }

      // If necessary, extrapolate mask to all 17 clues
      q.offer(maskToCheck);
      HashSet<BigInteger> masks = new HashSet<>();
      List<Future<Boolean>> batchFutures = new ArrayList<>();
      while (!q.isEmpty()) {
        BigInteger m = q.poll();

        if (seen.contains(m)) {
          continue;
        }

        if (m.bitCount() < numClues) {
          for (BigInteger _m : extrapolatePuzzleMask(m)) {
            if (!results.contains(_m)) {
              q.offer(_m);
              // System.out.printf("++ [%2d] %s\n", _m.bitCount(), grid.filter(_m).toString());
            }
          }
          continue;
        }

        masks.add(m);

        // counter.incrementAndGet();
        // batchFutures.add(pool.submit(() -> {
        //   Sudoku puzzle = grid.filter(m);
        //   // synchronized (sysoutLock) {
        //   //   System.out.printf("[%d] %s \n", m.bitCount(), puzzle.toString());
        //   // }
        //   if (puzzle.solutionsFlag() == 1) {
        //     results.add(m);
        //     synchronized (sysoutLock) {
        //       System.out.printf("⭐️ * %s\n", puzzle.toString());
        //     }
        //     return true;
        //   }
        //   return false;
        // }));
      }

      for (BigInteger m : masks) {
        seen.add(m);
        counter.incrementAndGet();
        batchFutures.add(pool.submit(() -> {
          Sudoku puzzle = grid.filter(m);
          // synchronized (sysoutLock) {
          //   System.out.printf("[%d] %s \n", m.bitCount(), puzzle.toString());
          // }
          if (puzzle.solutionsFlag() == 1) {
            results.add(m);
            synchronized (sysoutLock) {
              System.out.printf("⭐️ * %s\n", puzzle.toString());
            }
            return true;
          }
          return false;
        }));
      }
      masks.clear();
      masks = null;

      // Wait for batch work to complete before creating another batch
      while (!batchFutures.isEmpty()) {
        try {
          batchFutures.get(0).get(10L, TimeUnit.SECONDS);
          batchFutures.remove(0);
        } catch (
          TimeoutException |
          InterruptedException |
          CancellationException |
          ExecutionException ex
        ) {
          ex.printStackTrace();
        }
      }
      batchFutures.clear();
      batchFutures = null;
    }, 17);
  }

  private static List<BigInteger> extrapolatePuzzleMask(BigInteger mask) {
    List<BigInteger> list = new ArrayList<>();
    for (int ci = 0; ci < Sudoku.SPACES; ci++) {
      if (!mask.testBit(Sudoku.SPACES - 1 - ci)) {
        list.add(mask.setBit(Sudoku.SPACES - 1 - ci));
      }
    }
    return list;
  }
}
