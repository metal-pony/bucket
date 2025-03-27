package com.metal_pony.bucket.sudoku.drivers;

import java.io.File;
import java.io.FileInputStream;
// import java.io.FileNotFoundException;
import java.io.FileOutputStream;
// import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.ExecutionException;
// import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
// import java.util.concurrent.TimeoutException;
// import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import com.google.gson.Gson;
// import com.google.gson.JsonIOException;
// import com.google.gson.JsonSyntaxException;
import com.metal_pony.bucket.sudoku.GeneratedPuzzles;
import com.metal_pony.bucket.sudoku.Sudoku;
import com.metal_pony.bucket.sudoku.SudokuSieve;
import com.metal_pony.bucket.sudoku.Sudoku.SolutionCountResult;
import com.metal_pony.bucket.sudoku.drivers.gui.SudokuGuiDemo;
// import com.metal_pony.bucket.sudoku.util.Sudoku17Database;
import com.metal_pony.bucket.sudoku.util.SudokuMask;
// import com.metal_pony.bucket.sudoku.util.Sudoku17Database.JsonPuzzleRecord2;
import com.metal_pony.bucket.util.Counting;
// import com.metal_pony.bucket.util.ThreadPool;

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
  static final String RESOURCES_DIR = "resources";
  public static InputStream resourceStream(String name) {
    return Main.class.getResourceAsStream(String.format("/%s/%s", RESOURCES_DIR, name));
  }

  public static List<String> readAllLines(InputStream inStream) {
    List<String> lines = new ArrayList<>();

    Scanner scanner = new Scanner(inStream);
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();
      if (!line.isEmpty()) {
        lines.add(line);
      }
    }
    scanner.close();

    return lines;
  }

  public static final String DEFAULT_COMMAND = "generateConfigs";

  private static final class ArgsMap extends HashMap<String,String> {}

  private static final Map<String, Consumer<ArgsMap>> COMMANDS = new HashMap<>() {{
    // --clues %d
    put("play", Main::play);
    // --amount %d --normalize
    put("generateConfigs", Main::generateConfigs);
    // --amount %d --clues %d --threads %d
    put("generatePuzzles", Main::generatePuzzles);
    // --puzzle %s --threads %d --timeout %d
    put("solve", Main::solve);
    // --amount %d
    put("generate", Main::generate);
    put("benchmark", Main::benchmark);
    put("generateBands", Main::generateInitialBands);
    put("adhoc", Main::doThing); // TODO Temporary - for testing / experimentation
    // --level %d --grid %s --threads %d
    put("sieve", Main::createSieve);
    // --level %d --grid %s --threads %d
    put("fingerprint", Main::fingerprint);
    put("stats", Main::stats);
    put("gen", Main::gen2);

  }};

  static boolean verbose;

  private static void play(ArgsMap args) {
    defaultInMap(args, "clues", "27");
    int clues = inBounds(Integer.parseInt(args.get("clues")), 17, 81);
    Sudoku puzzle = Sudoku.generatePuzzle(null, clues, 1, 3);
    SudokuGuiDemo.show(puzzle);
  }

  private static void generateConfigs(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    final int numConfigs = Math.max(Integer.parseInt(args.get("amount")), 1);
    final boolean normalize = args.containsKey("normalize");

    long startTime = System.currentTimeMillis();
    for (int n = 0; n < numConfigs; n++) {
      // Board config = Generator.generateConfig();
      Sudoku config = Sudoku.configSeed().firstSolution();
      if (normalize) {
        config.normalize();
      }
      System.out.println(config.toString());
    }
    long endTime = System.currentTimeMillis();
    System.out.printf("Generated %d configs in %d ms.\n", numConfigs, endTime - startTime);
  }

  private static void generatePuzzles(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    defaultInMap(args, "clues", "27");
    defaultInMap(args, "threads", "1");

    final int amount = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);
    final int clues = inBounds(Integer.parseInt(args.get("clues")), 19, Sudoku.SPACES);
    final int threads = inBounds(Integer.parseInt(args.get("threads")), 1, 4);

    // String gridStr = args.get("grid");
    // Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);

    for (int n = 0; n < amount; n++) {
      Sudoku puzzle = Sudoku.generatePuzzle2(null, clues, new ArrayList<SudokuMask>(), 0, 60*1000L, true);
      if (puzzle == null) {
        // Timed out
        return;
      } else {
        System.out.println(puzzle);
      }
    }
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
      // ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
      // try {
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

        // Consumer<List<Sudoku>> solutionsCallback = (solutions) -> {
        //   // TODO FIX -- threads will block while waiting for this lock
        //   synchronized (lock) {
        //     // StringBuilder strb = new StringBuilder();
        //     // solutions.forEach(solution -> {
        //     //   strb.append(solution.toString());
        //     //   strb.append(newline);
        //     // });
        //     // System.out.println(strb.toString());
        //     // writers.get(t.getAndIncrement() % threads).write(strb.toString());
        //     long c = count.addAndGet(solutions.size());
        //     // long b = c / 10_000_000L;
        //     // if (b > billions.get()) {
        //       System.out.printf("Found %d so far...\n", c);
        //     //   billions.incrementAndGet();
        //     // }
        //   }
        // };
        // puzzle.searchForSolutionsAsync(pool, solutionsCallback, 1<<20);

        SolutionCountResult count = puzzle.countSolutionsAsync(threads, 1L, TimeUnit.DAYS);
        try {
          boolean complete = count.await();
          if (complete) {
            System.out.printf(" -- Complete. Found %d solutions -- \n", count.get());
          } else {
            System.out.printf(" -- Count incomplete {%d} --\n", count.get());
          }
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          System.out.printf(" -- Count interrupted {%d} --\n", count.get());
        }
        // pool.shutdown();
        // pool.awaitTermination(1L, TimeUnit.DAYS);
      // } catch (InterruptedException ex) {
      //   ex.printStackTrace();
      // } finally {
        // pool.shutdownNow();
        // writers.forEach(writer -> {
        //   writer.close();
        // });
      // }
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
      sudoku.reduce();
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

  private static HashMap<String,Integer> PUZZLESTRS_TO_NUM_SOLUTIONS = new HashMap<>() {{
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

  static Map<String,String[]> parseData(List<String> lines) {
    // captures:
    // [1] solution
    // [2] fingerprint level 2
    // [3] fingerprint level 3
    // [4] fingerprint level 4
    String regex = "\\(.+\\)\\s+(\\d+)\\s+([\\w:]+)\\s+([\\w:]+)\\s+([\\w:]+)";

    // String testStr = "(fp2:   9ms, fp3:  120ms, fp4:   5497ms) 425368917896571342713942856239156784578294631164783295947635128682419573351827469     5:15:8:7:4:4::24               5::15::11:6:2c:d:31:12:29:8:f:6:5d 5::15::11:6:39:26:8b:6a:c1:4e:ad:74:d8:2e:2f:10:a:1";

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher("");

    Map<String,String[]> fpMap = new HashMap<>();

    lines.forEach(line -> {
      matcher.reset(line);
      if (matcher.matches()) {
        String solution = matcher.group(1);
        String sNormal = new Sudoku(solution).normalize().toString();
        String fp2 = "\""+matcher.group(2)+"\"";
        String fp3 = "\""+matcher.group(3)+"\"";
        String fp4 = "\""+matcher.group(4)+"\"";
        // System.out.printf("%s,%s,%s,%s%n", sNormal, fp2, fp3, fp4);
        if (fpMap.containsKey(sNormal)) {
          if (!Arrays.equals(fpMap.get(sNormal), new String[]{fp2, fp3, fp4})) {
            System.out.printf(
              "🚨 Duplicate solution with different fingerprints:\n  %s\n  > %s\n  > %s\n",
              sNormal,
              Arrays.toString(fpMap.get(sNormal)),
              Arrays.toString(new String[]{fp2, fp3, fp4})
            );
          }
        } else {
          fpMap.put(sNormal, new String[]{fp2, fp3, fp4});
        }
      } else {
        // System.out.println("🚨 Not a match:\n"+line);
      }
    });

    return fpMap;
  }

  public static void solveAndFingerprintAsync(List<String> pStrs, int numThreads) {
    ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 100L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    Object sysoutLock = new Object();
    pStrs.forEach(pStr -> {
      // Skip empty strings
      if (pStr.trim().isEmpty())
        return;

      pool.submit(() -> {
        Sudoku p = new Sudoku(pStr);
        Sudoku s = p.firstSolution();

        long start = System.currentTimeMillis();
        String fp2 = s.fingerprint(2);
        long fp2Time = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        String fp3 = s.fingerprint(3);
        long fp3Time = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        String fp4 = s.fingerprint(4);
        long fp4Time = System.currentTimeMillis() - start;

        String record = String.format(
          "(fp2: %3dms, fp3: %4dms, fp4: %6dms) %s %20s %48s %s",
          fp2Time, fp3Time, fp4Time, s.toString(),
          fp2, fp3, fp4
        );
        synchronized (sysoutLock) {
          System.out.println(record);
        }
      });
    });

    pool.shutdown();
    try {
      pool.awaitTermination(1L, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static Map<String,String[]> readFingerprintsFile(String filename) {
    System.out.println("Parsing fingerprint file...");
    Map<String,String[]> fpMap = null;
    long start = System.currentTimeMillis();
    try {
      fpMap = parseData(Files.readAllLines(new File("./adhoc-fp4.txt").toPath()));
      long parseTime = System.currentTimeMillis() - start;
      System.out.printf("Done. (%d ms)%n", parseTime);
    } catch (IOException e) {
      System.out.println("🚨 Error. Did not parse.");
      e.printStackTrace();
    }
    return fpMap;
  }

  public static void doThing(ArgsMap args) {
    // Sudoku.main2(null);

    // Read in sudoku-17 strings and print solution and fingerprints and time taken to generate, multithreaded
    // [Macbook Pro, M2 Max] fp2 only, ~1min; fp3 ~5min; fp4 ~6hr.
    // List<String> sudokuStrs = readAllLines(resourceStream("sudoku-17.txt"));
    // solveAndFingerprintAsync(sudokuStrs, Runtime.getRuntime().availableProcessors() - 1);

    // long start;

    // try {
    //   Gson gson = new Gson();
    //   System.out.println("Loading sudoku17 puzzle records json...");
    //   JsonPuzzleRecord2[] records = gson.fromJson(new FileReader("./sudoku17.json"), JsonPuzzleRecord2[].class);
    //   System.out.println("Done! Loaded " + records.length + " records.\nVerifying puzzle solutions and fingerprint(2)...");

    //   Set<String> puzzleSet = Collections.synchronizedSet(new HashSet<>());
    //   Set<String> solutionSet = Collections.synchronizedSet(new HashSet<>());
    //   Set<String> f2Set = Collections.synchronizedSet(new HashSet<>());
    //   Set<String> f3Set = Collections.synchronizedSet(new HashSet<>());
    //   Set<String> f4Set = Collections.synchronizedSet(new HashSet<>());
    //   int[] solutionPuzzlesCount = new int[81];

    //   ThreadPool.useMaxThreads();
    //   AtomicBoolean passing = new AtomicBoolean(true);
    //   Object sysoutLock = new Object();
    //   List<Future<Boolean>> ftrs = new ArrayList<>();

    //   for (JsonPuzzleRecord2 r : records) {
    //     solutionSet.add(r.solution);
    //     for (String p : r.puzzles) {
    //       if (puzzleSet.contains(p)) {
    //         synchronized (sysoutLock) {
    //           System.out.println("🚨 Duplicate puzzle: " + p);
    //         }
    //       }
    //       puzzleSet.add(p);
    //       ftrs.add(ThreadPool.submit(() -> {
    //         Sudoku puzzle = new Sudoku(p);
    //         int flag = puzzle.solutionsFlag();
    //         String pSolution = puzzle.firstSolution().toString();
    //         if (flag != 1 || !r.solution.equals(pSolution)) {
    //           passing.set(false);
    //           synchronized (sysoutLock) {
    //             System.out.printf(
    //               "\n🚨 BAD RECORD:\n                    %s\n                    flag == %s\nexpected solution   %s\n",
    //               p,
    //               flag,
    //               r.solution
    //             );
    //           }
    //           return false;
    //         }
    //         return true;
    //       }));
    //     }
    //     f2Set.add(r.fingerprints[0]);
    //     f3Set.add(r.fingerprints[1]);
    //     f4Set.add(r.fingerprints[2]);
    //     solutionPuzzlesCount[r.puzzles.length]++;

    //     // ftrs.add(ThreadPool.submit(() -> {
    //     //   Sudoku solution = new Sudoku(r.solution);
    //     //   String fp2 = solution.fingerprint(2);
    //     //   if (!r.fingerprints[0].equals(fp2)) {
    //     //     passing.set(false);
    //     //     synchronized (sysoutLock) {
    //     //       System.out.printf(
    //     //         "\n🚨 BAD RECORD:\n                    %s\n                fp2 %s\n       expected fp2 %s\n",
    //     //         r.solution,
    //     //         fp2,
    //     //         r.fingerprints[0]
    //     //       );
    //     //     }
    //     //     return false;
    //     //   }
    //     //   return true;
    //     // }));
    //   }

    //   ThreadPool.shutdown();
    //   System.out.print("[" + "=".repeat(100) + "]\n[");
    //   AtomicInteger failCounter = new AtomicInteger();
    //   int i = 0;
    //   int rPerDot = ftrs.size() / 100;
    //   for (Future<Boolean> f : ftrs) {
    //     if ((i++ % rPerDot) == rPerDot - 1) {
    //       System.out.print('.');
    //     }
    //     try {
    //       boolean success = f.get(1L, TimeUnit.MINUTES);
    //       if (!success) {
    //         failCounter.incrementAndGet();
    //         synchronized (sysoutLock) {
    //           // Redraw progress bar because why not?
    //           System.out.printf("\n[");
    //           int j = 0;
    //           while (j < i) {
    //             if ((j++ % rPerDot) == rPerDot - 1) {
    //               System.out.print('.');
    //             }
    //           }
    //         }
    //       }
    //     } catch (InterruptedException | ExecutionException | TimeoutException e) {
    //       synchronized (sysoutLock) {
    //         System.out.println("🚨 Something went wrong while processing the file.");
    //         e.printStackTrace();
    //       }
    //     }
    //   }

    //   System.out.println("]");
    //   if (passing.get()) {
    //     System.out.println("✅ All json puzzle records verified correct hydration!");
    //   }

    //   System.out.println("Finished.");
    //   System.out.println("Puzzles: " + puzzleSet.size());
    //   System.out.println("Solutions: " + solutionSet.size());
    //   System.out.println("Unique fp2: " + f2Set.size());
    //   System.out.println("Unique fp3: " + f3Set.size());
    //   System.out.println("Unique fp4: " + f4Set.size());
    //   System.out.println("Spread of [Num Puzzles]: Num Solutions");
    //   for (int countIndex = 0; countIndex < solutionPuzzlesCount.length; countIndex++) {
    //     if (solutionPuzzlesCount[countIndex] > 0) {
    //       System.out.printf("[%2d]: %d\n", countIndex, solutionPuzzlesCount[countIndex]);
    //     }
    //   }
    // } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
    //   System.out.println("🚨 Failed to parse file. Aborting...");
    //   e.printStackTrace();
    //   return;
    // }


    // Sudoku17Database.init();


    // Map<String,String[]> fpMap = readFingerprintsFile("./adhoc-fp4.txt");

    // System.out.print("Loading sudoku-17.txt... ");
    // start = System.currentTimeMillis();
    // List<String> sudokuStrs = readAllLines(resourceStream("sudoku-17.txt"));
    // long readTime = System.currentTimeMillis() - start;
    // System.out.printf("Done. (%d ms)%n", readTime);

    // System.out.println("Solving sudoku-17s...");
    // // sudoku17 -> solution
    // // Map<String,String> solutionMap = new HashMap<>();
    // start = System.currentTimeMillis();
    // for (String sudoku17 : sudokuStrs) {
    //   Sudoku p17 = new Sudoku(sudoku17);
    //   Sudoku p17Solution = p17.firstSolution().normalize();
    //   // p17.getMask()
    //   String solution = p17Solution.toString();
    //   String[] fpses = fpMap.get(solution);
    //   if (fpses == null) {
    //     System.out.println("🚨 Error. Solution not found in fingerprint map: " + solution);
    //     return;
    //   }
    //   if (fpses.length == 0) {
    //     System.out.println("🚨 Error. Solution fingerprints empty: " + solution);
    //     return;
    //   }
    //   // System.out.printf(
    //   //   "%s,%s,%s,%s,%s\n",
    //   //   new SudokuMask(sudoku17).toHexString(),
    //   //   solution.substring(9),
    //   //   fpses[0], fpses[1], fpses[2]
    //   // );
    //   System.out.printf(
    //     "{\"puzzle\":\"%s\",\"solution\":\"%s\",\"fingerprints\":[%s]},\n",
    //     new SudokuMask(sudoku17).toHexString(),
    //     solution,
    //     String.join(",", fpses)
    //   );
    //   // String prev = solutionMap.put(sudoku17, solution);
    //   // if (prev != null)
    //   //   System.out.println("🚨 SOLUTION OVERWRITTEN FOR " + sudoku17);
    // }
    // long solveTime = System.currentTimeMillis() - start;
    // System.out.printf("Done. (%d ms)%n", solveTime);

    // Connect and output record json
    // System.out.println("Matching sudoku-17s with solution fingerprints...");
    // final Map<String,String[]> _fpMap = fpMap;
    // start = System.currentTimeMillis();
    // solutionMap.forEach((p17,solution) -> {
    //   String[] fpses = _fpMap.get(solution);
    //   if (fpses == null) {
    //     System.out.println("🚨 Error. Solution not found in fingerprint map: " + solution);
    //     return;
    //   }
    //   if (fpses.length == 0) {
    //     System.out.println("🚨 Error. Solution fingerprints empty: " + solution);
    //     return;
    //   }

    //   System.out.printf("%s,%s,%s,%s,%s\n", new SudokuMask(p17).toHexString(), solution.substring(9), fpses[0], fpses[1], fpses[2]);
    // });
    // long outputTime = System.currentTimeMillis() - start;
    // System.out.printf("Done. (%d ms)%n", outputTime);


    // Sudoku p = new Sudoku("............2......4..1..6.812.3.........5....9..8...........252.5.....1.795.....");
    // Set<String> solutionSet = Collections.synchronizedSet(new HashSet<>());

    // final int tests = 10;
    // ArrayList<Long> ts = new ArrayList<>();
    // for (int t = 0 ; t < 10; t++) {
    //   long start = System.currentTimeMillis();
    //   p.searchForSolutions3(s -> {
    //     solutionSet.add(s.toString());
    //     return true;
    //   });
    //   long end = System.currentTimeMillis();
    //   ts.add(end - start);
    // }
    // long sum = 0L;
    // for (long t : ts) sum += t;
    // System.out.println(sum/10);

    // ts.clear();
    // for (int t = 0 ; t < 10; t++) {
    //   long start = System.currentTimeMillis();
    //   p.searchForSolutionsAsync(s -> {
    //     solutionSet.add(s.toString());
    //   }, 4);
    //   long end = System.currentTimeMillis();
    //   ts.add(end - start);
    // }
    // sum = 0L;
    // for (long t : ts) sum += t;
    // System.out.println(sum/10);

    // ThreadPool.useMaxThreads();

    // ts.clear();
    // for (int t = 0 ; t < 10; t++) {
    //   long start = System.currentTimeMillis();
    //   List<Future<List<Sudoku>>> futures = p.searchForSolutions4();
    //   futures.forEach((ftr) -> {
    //     try {
    //       for (Sudoku solution : ftr.get(10L, TimeUnit.SECONDS)) {
    //         solutionSet.add(solution.toString());
    //       }
    //     } catch (InterruptedException | ExecutionException | TimeoutException e) {
    //       // TODO Auto-generated catch block
    //       e.printStackTrace();
    //     }
    //   });
    //   long end = System.currentTimeMillis();
    //   // if (solutionSet.size() != n) {
    //   //   System.out.printf("❌ %d != %d%n", solutionSet.size(), n);
    //   // }
    //   ts.add(end - start);
    // }
    // sum = 0L;
    // for (long t : ts) sum += t;
    // System.out.println(sum/10);

    // ThreadPool.shutdown();





    // long[][] avgs = new long[9][];
    // System.out.print("🟣 ");
    // AtomicInteger i = new AtomicInteger();
    // avgs[0] = new long[PUZZLESTRS_TO_NUM_SOLUTIONS.size()];
    // // PUZZLESTRS_TO_NUM_SOLUTIONS.forEach((str, n) -> {
    // //   long[] times = new long[tests];
    // //   for (int t = 0; t < tests; t++) {
    // //     solutionSet.clear();
    // //     long start = System.currentTimeMillis();
    // //     new Sudoku(str).searchForSolutions3((solution) -> {
    // //       solutionSet.add(solution.toString());
    // //       return true;
    // //     });
    // //     long end = System.currentTimeMillis();
    // //     if (solutionSet.size() != n) {
    // //       System.out.printf("❌ %d != %d%n", solutionSet.size(), n);
    // //     }
    // //     times[t] = end - start;
    // //   }
    // //   // Print averages
    // //   long total = 0L;
    // //   for (long t : times)
    // //     total += t;
    // //   avgs[0][i.get()] = total/tests;
    // //   String avgStr = ""+avgs[0][i.get()];
    // //   System.out.printf("%s%s", " ".repeat(5-avgStr.length()), avgStr);
    // //   i.incrementAndGet();
    // // });
    // // System.out.println();

    // // for (int threads = 1; threads <= 8; threads++) {
    // //   avgs[threads] = new long[PUZZLESTRS_TO_NUM_SOLUTIONS.size()];
    // //   final int _threads = threads;
    // //   i.set(0);
    // //   System.out.print(_threads+"  ");
    // //   PUZZLESTRS_TO_NUM_SOLUTIONS.forEach((str, n) -> {
    // //     long[] times = new long[tests];
    // //     for (int t = 0; t < tests; t++) {
    // //       solutionSet.clear();
    // //       long start = System.currentTimeMillis();
    // //       new Sudoku(str).searchForSolutionsAsync((solution) -> {
    // //         solutionSet.add(solution.toString());
    // //       }, _threads);
    // //       long end = System.currentTimeMillis();
    // //       if (solutionSet.size() != n) {
    // //         System.out.printf("❌ %d != %d%n", solutionSet.size(), n);
    // //       }
    // //       times[t] = end - start;
    // //     }
    // //     // Print averages
    // //     long total = 0L;
    // //     for (long t : times)
    // //       total += t;
    // //     avgs[_threads][i.get()] = total/tests;
    // //     String avgStr = ""+avgs[_threads][i.get()];
    // //     System.out.printf("%s%s", " ".repeat(5-avgStr.length()), avgStr);
    // //     i.incrementAndGet();
    // //   });
    // //   System.out.println();
    // // }





    // Read all 17 clue puzzle entries from JSON file.
    // Track how many clues they have after a reduction.
    // Log and examine the spread.
    // PuzzleEntry[] entries = PuzzleEntry.all17();
    // int[] spread = new int[Sudoku.SPACES + 1];
    // for (PuzzleEntry puzzleEntry : entries) {
    //   Sudoku p = puzzleEntry.puzzle();
    //   p.resetEmptyCells();
    //   p.resetConstraints();
    //   p.reduce();

    //   spread[p.numClues()]++;
    //   System.out.printf("[%2d] %s\n", p.numClues(), p.toString());
    // }

    // System.out.println("Spread of reduced puzzles by numClues:");
    // for (int numClues = 0; numClues < spread.length; numClues++) {
    //   if (spread[numClues] == 0) continue;
    //   System.out.printf("  [%2d]: %6d  (%.4f%%)\n", numClues, spread[numClues], 100.0 * (double)spread[numClues]/(double)entries.length);
    // }
  }

  /**
   * level [2,4] (default: 2)
   * grid? [str] (default: randomly generated)
   * threads? [1, #cores - 2] (default: 1 if omitted; MAX if only "--threads" given)
   */
  public static void createSieve(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;
    final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    final long DEFAULT_TIMEOUT = 1L;
    final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    defaultInMap(args, "level", "2");
    // defaultInMap(args, "threads", Integer.toString(MAX_THREADS));

    String gridStr = args.get("grid");
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    boolean usingThreads = args.containsKey("threads");
    String threadsStr = usingThreads ? args.get("threads") : "1";
    int threads = (usingThreads && threadsStr == null) ? MAX_THREADS : inBounds(Integer.parseInt(threadsStr), 1, MAX_THREADS);

    ThreadPoolExecutor pool = null;
    if (usingThreads) {
      pool = new ThreadPoolExecutor(threads, threads, DEFAULT_TIMEOUT, TIMEOUT_UNIT, new LinkedBlockingQueue<>());
    }
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);
    System.out.println(grid.toString());
    SudokuSieve sieve = new SudokuSieve(grid);

    sieve.seed(level, pool);
    if (usingThreads) {
      try {
        pool.awaitTermination(5L, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println(sieve.toString());
    System.out.printf("Added %d items to sieve.\n", sieve.size());
  }

  public static void fingerprint(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;
    final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    final long DEFAULT_TIMEOUT = 1L;
    final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    defaultInMap(args, "level", "2");

    String gridStr = args.get("grid");
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    boolean usingThreads = args.containsKey("threads");
    String threadsStr = usingThreads ? args.get("threads") : "1";
    int threads = (usingThreads && threadsStr == null) ? MAX_THREADS : inBounds(Integer.parseInt(threadsStr), 1, MAX_THREADS);

    ThreadPoolExecutor pool = null;
    if (usingThreads) {
      System.out.println("Creating ThreadPool...");
      pool = new ThreadPoolExecutor(threads, threads, DEFAULT_TIMEOUT, TIMEOUT_UNIT, new LinkedBlockingQueue<>());
    }
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);
    System.out.println(grid.toString());
    SudokuSieve sieve = new SudokuSieve(grid);

    sieve.seed(level, pool);
    if (usingThreads) {
      pool.shutdown();
      try {
        System.out.println("Waiting on ThreadPool...");
        pool.awaitTermination(1L, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("Done!");

    // int minNumCells = Sudoku.SPACES;
    int maxNumCells = 0;
    int[] itemsByNumCells = new int[Sudoku.SPACES];

    for (SudokuMask ua : sieve.items(new ArrayList<>())) {
      int numCells = ua.bitCount();
      itemsByNumCells[numCells]++;
      // if (numCells < minNumCells) minNumCells = numCells;
      if (numCells > maxNumCells) maxNumCells = numCells;
    }

    ArrayList<String> itemsList = new ArrayList<>();
    // An unavoidable set includes a minimum of 4 cells
    for (int m = 4; m <= maxNumCells; m++) {
      // In level 2, there can be no UAs using an odd number of cells,
      // because each cell must have at least one complement.
      if (level == 2 && (m % 2) > 0) {
        continue;
      }

      int count = itemsByNumCells[m];
      itemsList.add((count > 0) ? Integer.toString(count, 16) : "");
    }

    System.out.println(String.join(":", itemsList));
  }

  public static void stats(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;
    final int MIN_K = 20;
    final int MIN_NUM_TESTS = 100;
    final int MAX_NUM_TESTS = 1_000_000;

    defaultInMap(args, "level", "3");
    defaultInMap(args, "n", "1000");

    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    int numTests = inBounds(Integer.parseInt(args.get("n")), MIN_NUM_TESTS, MAX_NUM_TESTS);
    String gridStr = args.get("grid");
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);
    System.out.println(grid.toString());

    SudokuSieve sieve = new SudokuSieve(grid);
    sieve.seed(level);
    System.out.println(sieve.toString());

    int[][] results = new int[Sudoku.SPACES][2];
    HashSet<SudokuMask> seenForK = new HashSet<>();
    for (int k = Sudoku.SPACES - 1; k >= MIN_K; k--) {
      seenForK.clear();

      BigInteger nck = Counting.nChooseK(Sudoku.SPACES, k);
      int n = numTests;
      if (nck.compareTo(BigInteger.valueOf((long) n)) < 0) {
        n = nck.intValue();
        if (n < 1) {
          throw new RuntimeException("Something dumb happened with numTests (it's negative? idk)");
        }
      }

      while (seenForK.size() < n) {
        SudokuMask r = SudokuMask.random(k);
        if (seenForK.contains(r)) {
          continue;
        }

        results[k][0]++;
        boolean satisfies = sieve.doesMaskSatisfy(r);
        Sudoku rPuzz = grid.filter(r);
        if (satisfies) {
          seenForK.add(r);
          int flag = rPuzz.solutionsFlag();
          if (flag == 1) {
            results[k][1]++;
          }
          System.out.printf("[%d] %s | sieve check: ✅; real sudoku: %s\n", k, rPuzz.toString(), (flag == 1) ? "⭐️" : "❌");
        } else {
          System.out.printf("[%d] %s | sieve check: ❌\n", k, rPuzz.toString());
        }
      }
    }

    for (int k = Sudoku.SPACES - 1; k >= MIN_K; k--) {
      int randomsChecked = results[k][0];
      // String realSudoku = (results[k][0] > 0) ?
      //   String.format("%d/%d (%.2f%%)", results[k][1], results[k][0], ((double)results[k][1]) / ((double)results[k][0]) * 100.0) :
      //   "---"
      // ;

      System.out.printf(
        "[%d] satisfied: %d/%d (%.2f%%); real sudoku: %d/%d (%.2f%%)\n",
        k,
        numTests, randomsChecked, ((double)numTests) / ((double)randomsChecked) * 100.0,
        results[k][1], numTests, ((double)results[k][1]) / ((double)numTests) * 100.0
      );
    }
  }

  public static void gen2(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;

    defaultInMap(args, "level", "3");
    defaultInMap(args, "numClues", "27");
    defaultInMap(args, "amount", "1");

    final int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    final int numClues = inBounds(Integer.parseInt(args.get("numClues")), Sudoku.MIN_CLUES, Sudoku.SPACES);
    final int amount = inBounds(Integer.parseInt(args.get("amount")), 1, 1000);

    String gridStr = args.get("grid");
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);

    Sudoku.generatePuzzle(grid, numClues, amount, level);
  }
}
