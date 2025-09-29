package com.metal_pony.bucket.sudoku.drivers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.metal_pony.bucket.sudoku.PuzzleEntry;
import com.metal_pony.bucket.sudoku.Sudoku;
import com.metal_pony.bucket.sudoku.SudokuSieve;
import com.metal_pony.bucket.sudoku.drivers.gui.SudokuGuiDemo;
import com.metal_pony.bucket.sudoku.util.SudokuMask;

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
 */
public class Main {
  private static void sleep(long timeMs) {
    try {
      Thread.sleep(timeMs);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void debug(String msg) { debug(msg, (Object[]) null); }
  private static void debug(String msg, Object... args) {
    if (verbose) System.out.printf(msg, args);
  }

  private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

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
    put("benchConfigs", Main::benchConfigGeneration);
    // --amount %d --clues %d --threads %d
    put("generatePuzzles", Main::generatePuzzles);
    put("countSolutions", Main::countSolutions);
    // --puzzle %s --threads %d --timeout %d
    put("solve", Main::solve);
    put("generateBands", Main::generateInitialBands);
    // --level %d --grid %s --threads %d
    put("sieve", Main::createSieve);
    // --level %d --grid %s --threads %d
    put("fingerprint", Main::fingerprint);

    // For testing / experimentation
    put("adhoc", Main::adhoc);
    put("help", Main::help);
    put("check17", Main::check17);
    put("buildcsv", Main::buildcsv);
    put("process17", Main::process17s);
    put("dj", Main::createDisjointMaps);
    put("countCompare", Main::compareCountSolutions);

    put("buildSieveTest", Main::buildSieveTestCSV);
    put("buildjson17", Main::sudoku17ToJSON);
  }};

  static boolean verbose;

  // private static String padLeft(String str, int length, char fillChar) {
  //   return Character.toString(fillChar).repeat(length - str.length()) + str;
  // }

  private static void sudoku17ToJSON(ArgsMap args) {
    long start = System.currentTimeMillis();

    // Use maximum of 8 processors while keeping 2 available for the system to keep doing its thing.
    int numThreads = inBounds(Runtime.getRuntime().availableProcessors() - 2, 1, 8);
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
      numThreads, numThreads,
      1L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>()
    );

    System.out.println("[");
    PuzzleEntry.allSudoku17().forEach(entry -> {
      pool.submit(() -> {
        System.out.println(entry.toString() + ",");
      });
    });

    pool.shutdown();
    try {
      pool.awaitTermination(1L, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("]");

    long end = System.currentTimeMillis();
    long total = end - start;
    long mins = total / 60000L;
    long secs = (total - (60000L * mins)) / 1000L;
    String timeStr = (mins > 0) ? String.format("%d min, %d sec", mins, secs) : String.format("%d sec", secs);
    System.out.printf("Done (%s)\n", timeStr);
  }

  private static void process17s(ArgsMap args) {
    long start = System.currentTimeMillis();

    PuzzleEntry[] sudoku17 = PuzzleEntry.all17();

    // Use maximum of 8 processors while keeping 2 available for the system to keep doing its thing.
    int numThreads = inBounds(Runtime.getRuntime().availableProcessors() - 2, 1, 8);
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
      numThreads, numThreads,
      10L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>()
    );

    String debug_setName = "dc4";
    List<Future<String[]>> ftrs = new ArrayList<>();
    for (PuzzleEntry entry : sudoku17) {
      ftrs.add(pool.submit(() -> {
        Sudoku solution = new Sudoku(entry.solution());
        return new String[]{
          entry.solutionStr(),
          // solution.dc2(),
          // solution.dc3(),
          solution.dc4(),
          // solution.ac2(),
          // solution.ac3(),
          // solution.ac4(),
          // solution.fp2(),
          // solution.fp3(),
          // solution.fp4()
        };
      }));
    }

    pool.shutdown();

    // HashSet<String> dc2Set = new HashSet<>();
    // HashSet<String> dc3Set = new HashSet<>();
    HashSet<String> dc4Set = new HashSet<>();

    // HashSet<String> ac2Set = new HashSet<>();
    // HashSet<String> ac3Set = new HashSet<>();
    // HashSet<String> ac4Set = new HashSet<>();

    // HashSet<String> fp2Set = new HashSet<>();
    // HashSet<String> fp3Set = new HashSet<>();
    // HashSet<String> fp4Set = new HashSet<>();

    // Only used for counting;
    // int fi = 0;
    while (!ftrs.isEmpty()) {
      Future<String[]> ftr = ftrs.remove(0);
      try {
        String[] prints = ftr.get(1L, TimeUnit.MINUTES);
        // dc2Set.add(prints[1]);
        // dc3Set.add(prints[2]);
        dc4Set.add(prints[1]);

        // ac2Set.add(prints[4]);
        // ac3Set.add(prints[5]);
        // ac4Set.add(prints[6]);

        // fp2Set.add(prints[7]);
        // fp3Set.add(prints[1]);
        // fp4Set.add(prints[9]);
        System.out.println(String.join(",", prints));
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        e.printStackTrace();
      }
      // fi++;
    }

    long end = System.currentTimeMillis();
    long total = end - start;
    long mins = total / 60000L;
    long secs = (total - (60000L * mins)) / 1000L;
    String timeStr = (mins > 0) ? String.format("%d min, %d sec", mins, secs) : String.format("%d sec", secs);
    System.out.printf("Done (%s). FP counts:\n", timeStr);
    System.out.printf(
      // "dc4: %4d\n", dc4Set.size()
      "%s: %4d\n", debug_setName, dc4Set.size()

      // "dc2: %4d        ac2: %4d        fp2: %4d\ndc3: %4d        ac3: %4d        fp3: %4d\ndc4: %4d        ac4: %4d        fp4: %4d\n",
      //dc2Set.size(), ac2Set.size(), fp2Set.size(),
      // dc3Set.size(), ac3Set.size(), fp3Set.size(),
      // dc4Set.size(), ac4Set.size(), fp4Set.size()
    );
  }

  // TODO update help content
  private static void help(ArgsMap args) {
    System.out.println(
"""
Commands:
    play [--clues (27)]
        Generatees a puzzle with the given number of clues, then starts a GUI.
    generateConfigs [--amount (1), --normalize (false)]
        Generates a full sudoku grid. Optionally "normalize" the grid such that
        the top row reads 1 through 9, sequentially.
    generatePuzzles [--amount (1) --clues (27) --threads (1)]
        Generates a sudoku puzzle with the given number of clues.
    solve --grid [--timeoutMs (10_000) --threads (1)]
        Prints all solutions of the given sudoku grid.
    sieve [--grid (random) --level (2)]
        Gets unavoidable sets for the given grid.
        `level` supported from 2 through 4.
    fingerprint [--grid (random) --level (2)]
        Generates a fingerprint for the sudoku grid.
        `level` supported from 2 through 4.
        The result is independent how the grid may be changed by symmetry-preserving
        operations. Therefore, if any two seemingly different grids have the same
        fingerprints, it is very likely they are the same grid, just scrambled.
        Like different states of a Rubik's cube.
"""
    );
  }

  private static void play(ArgsMap args) {
    defaultInMap(args, "clues", "27");
    int clues = inBounds(Integer.parseInt(args.get("clues")), 17, 81);
    Sudoku puzzle = Sudoku.generatePuzzle(clues);
    SudokuGuiDemo.show(puzzle);
  }

  private static void generateConfigs(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    defaultInMap(args, "threads", "1");

    final int numConfigs = Math.max(Integer.parseInt(args.get("amount")), 1);
    final int numThreads = inBounds(Integer.parseInt(args.get("threads")), 1, 8);
    final boolean normalize = args.containsKey("normalize");

    List<Future<?>> ftrs = Collections.synchronizedList(new ArrayList<>(numConfigs));
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
      numThreads, numConfigs,
      1L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>()
    );
    pool.prestartAllCoreThreads();

    long start = System.currentTimeMillis();
    for (int n = 0; n < numConfigs; n++) {
      ftrs.add(
        pool.submit(() -> {
          Sudoku config = Sudoku.generateConfig();
          if (normalize) config.normalize();
          System.out.println(config.toString());
          return config.toString();
        })
      );
    }

    pool.shutdown();
    for (Future<?> f : ftrs) {
      try {
        f.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    debug(
      "Generated %d configs in %d ms.\n",
      // "Generated %d configs in %d ms. %d different fps.\n",
      numConfigs,
      System.currentTimeMillis() - start//,
      // fps.size()
    );
  }

  // TODO Adapt for multiple threads
  // TODO Output as json
  // TODO Difficulty option
  private static void generatePuzzles(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    defaultInMap(args, "clues", "27");
    defaultInMap(args, "threads", "1");

    String gridStr = args.get("grid");
    boolean useSameSolution = (gridStr == null);
    Sudoku grid = (gridStr == null) ? Sudoku.generateConfig() : new Sudoku(gridStr);

    final int amount = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);
    final int clues = inBounds(Integer.parseInt(args.get("clues")), 19, Sudoku.SPACES);
    // TODO Generate with multiple threads
    // final int threads = inBounds(Integer.parseInt(args.get("threads")), 1, MAX_THREADS);

    SudokuSieve sieve = new SudokuSieve(grid);

    for (int n = 0; n < amount; n++) {
      Sudoku puzzle = Sudoku.generatePuzzle(
        useSameSolution ? grid : null,
        clues,
        useSameSolution ? sieve : null,
        0,
        60*1000L,
        true
      );
      if (puzzle == null) {
        // Timed out
        return;
      } else {
        System.out.println(puzzle);
      }
    }
  }

  private static void countSolutions(ArgsMap args) {
    defaultInMap(args, "threads", "1");

    String gridStr = args.get("grid");
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().solution() : new Sudoku(gridStr);
    final int numThreads = inBounds(Integer.parseInt(args.get("threads")), 1, Runtime.getRuntime().availableProcessors());

    long start = System.currentTimeMillis();
    long numSolutions = 0L;
    if (numThreads == 1) {
      debug("countSolutions(\n  grid: %s\n  numThreads: %d\n):\n", grid.toString(), numThreads);
      numSolutions = grid.countSolutionsAsync(numThreads);
    } else {

    }
    long end = System.currentTimeMillis();
    debug("Total: %d\n", numSolutions);
    debug("(%d ms)\n", end - start);
    System.out.println(numSolutions);
  }

  private static void solve(ArgsMap args) {
    System.out.println(new Sudoku(args.get("grid")).solution().toString());
  }

  private static int inBounds(int value, int min, int max) {
    return Math.max(min, Math.min(value, max));
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
        values = sudoku.getCandidate(index);
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
    Set<String> fullBandSet = new HashSet<>();
    final int N = Sudoku.DIGITS * 3;
    long time = timeCpuExecution(() -> {
      Sudoku root = new Sudoku("123456789--------");

      // TODO Ensure that this script works properly without needing to call reset.
      // root.resetCandidatesAndValidity();

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
    reducedBandSet.forEach(System.out::println);
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

  /**
   * level [2,4] (default: 2)
   * grid? [str] (default: randomly generated)
   * threads? [1, #cores - 2] (default: 1 if omitted; MAX if only "--threads" given)
   */
  public static void createSieve(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;

    defaultInMap(args, "level", "2");
    defaultInMap(args, "threads", "1");

    String gridStr = args.get("grid");
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    int numThreads = inBounds(Integer.parseInt(args.get("threads")), 1, Runtime.getRuntime().availableProcessors());

    Sudoku grid = (gridStr == null) ? Sudoku.generateConfig() : new Sudoku(gridStr);
    System.out.println(grid.toString());
    SudokuSieve sieve = new SudokuSieve(grid);

    debug("Using " + numThreads + " threads.");
    long startTime = System.currentTimeMillis();
    sieve.seedThreaded(sieve.fullPrintCombos(level), numThreads);
    long endTime = System.currentTimeMillis();

    System.out.println(sieve.toString());
    System.out.printf("Added %d items to sieve (%d ms).\n", sieve.size(), endTime - startTime);
  }

  public static void fingerprint(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;
    final String ALL_LEVELS = "2,3,4";

    defaultInMap(args, "level", "2");
    defaultInMap(args, "threads", "1");

    String gridStr = args.get("grid");
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().solution() : new Sudoku(gridStr);

    String threadsStr = args.get("threads");
    if ("max".equalsIgnoreCase(threadsStr)) {
      threadsStr = Integer.toString(Runtime.getRuntime().availableProcessors());
    }
    int numThreads = inBounds(Integer.parseInt(threadsStr), 1, Runtime.getRuntime().availableProcessors());

    String levelArg = args.get("level");
    if ("all".equals(levelArg)) { levelArg = ALL_LEVELS; }

    if (verbose) {
      System.out.printf("Calculating fingerprint of grid:\n%s\n", grid.toString());
    }

    if (numThreads > 1) {
      System.out.printf("Using %d threads.\n", numThreads);
    }

    if (levelArg.contains(",")) {
      for (String lvlStr : levelArg.split(",")) {
        try {
          int level = inBounds(Integer.parseInt(lvlStr), MIN_LEVEL, MAX_LEVEL);
          long start = System.currentTimeMillis();
          String fp = grid.fp(level, numThreads);
          long sysTime = System.currentTimeMillis() - start;
          if (verbose) {
            System.out.printf("fingerprint (level %d): %s (%d ms)\n", level, fp, sysTime);
          } else {
            System.out.println(fp);
          }
        } catch (NumberFormatException numbrEx) {
          System.out.println("Error: unrecognized level");
        }
      }

    } else {
      try {
        int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
        long start = System.currentTimeMillis();
        String fp = grid.fp(level, numThreads);
        long sysTime = System.currentTimeMillis() - start;
        if (verbose) {
          System.out.printf("fingerprint (level %d): %s (%d ms)\n", level, fp, sysTime);
        } else {
          System.out.println(fp);
        }
      } catch (NumberFormatException numbrEx) {
        System.out.println("Error: unrecognized level");
      }
    }

    // String format = "{\"%s\", \"%s\"},\n";
    // System.out.printf(format, "dc2", grid.dc2());
    // System.out.printf(format, "dc3", grid.dc3());
    // System.out.printf(format, "dc4", grid.dc4());
    // System.out.printf(format, "ac2", grid.ac2());
    // System.out.printf(format, "ac3", grid.ac3());
    // System.out.printf(format, "ac4", grid.ac4());
    // System.out.printf(format, "fp2", grid.fp2());
    // System.out.printf(format, "fp3", grid.fp3());
    // System.out.printf(format, "fp4", grid.fp4());
  }

  // CURRENTLY: Sieve search
  public static void adhoc(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;

    // defaultInMap(args, "amount", "1");
    defaultInMap(args, "level", "2");
    defaultInMap(args, "clues", "81");
    // defaultInMap(args, "threads", "1");

    String gridStr = args.get("grid");
    System.out.println("Using sudoku grid:");
    Sudoku grid = (gridStr == null) ? Sudoku.generateConfig() : new Sudoku(gridStr);
    System.out.println(grid.toString());
    SudokuSieve sieve = new SudokuSieve(grid);
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    int clues = inBounds(Integer.parseInt(args.get("clues")), Sudoku.MIN_CLUES, Sudoku.SPACES);
    // final int amount = Math.max(Integer.parseInt(args.get("amount")), 1);
    // int numThreads = inBounds(Integer.parseInt(args.get("threads")), 1, Runtime.getRuntime().availableProcessors());

    // SIEVE SEARCHING
    int numThreads = MAX_THREADS;
    System.out.printf("Seeding sieve (level %d; threads %d)...", level, numThreads);
    long seedStartTime = System.currentTimeMillis();
    sieve.seedThreaded(sieve.fullPrintCombos(level), MAX_THREADS);
    long seedEndTime = System.currentTimeMillis();
    System.out.printf("Done in %d ms.\n", seedEndTime - seedStartTime);
    System.out.println(sieve.toString());
    System.out.printf("Sieve has %d items.\n", sieve.size());

    SieveSearcher searcher = new SieveSearcher(sieve);
    System.out.println("Starting in just a sec...");
    sleep(2000L);

    searcher.search(clues);
    System.out.println("Done searching.");
  }

  // private static int[] arrCopy(int[] arr) {
  //   int[] copy = new int[arr.length];
  //   System.arraycopy(arr, 0, copy, 0, arr.length);
  //   return copy;
  // }

  // private static String join(int[] arr) {
  //   StringBuilder strb = new StringBuilder();
  //   for (int n : arr)
  //     strb.append(n);
  //   return strb.toString();
  // }

  // private static int minOfMaxIndices(SudokuSieve sieve) {
  //   int min = 81;
  //   for (SudokuMask item : sieve.items()) {
  //     min = Math.min(maxIndex(item), min);
  //   }
  //   return min;
  // }

  // private static int maxIndex(SudokuMask mask) {
  //   return last(mask.toIndices());
  // }

  // private static int last(int[] arr) {
  //   if (arr.length == 0) throw new RuntimeException("arr is empty");
  //   return arr[arr.length - 1];
  // }

  private static void benchConfigGeneration(ArgsMap args) {
    final int MAX_AMOUNT = Integer.MAX_VALUE;
    defaultInMap(args, "amount", "1");
    int amount = inBounds(Integer.parseInt(args.get("amount")), 1, MAX_AMOUNT);

    // TODO Adapt for multithreading
    // defaultInMap(args, "threads", "1");
    // int numThreads = inBounds(Integer.parseInt(args.get("threads")), 1, Runtime.getRuntime().availableProcessors());

    List<Sudoku> configs = new ArrayList<>();
    long time = timeCpuExecution(() -> {
      for (int t = 0; t < amount; t++) {
        configs.add(Sudoku.generateConfig());
      }
    });

    System.out.printf(
      "benchConfigGeneration({ amount: %d }): %d ms\n",
      amount,
      TimeUnit.NANOSECONDS.toMillis(time)
    );
  }

  private static void check17(ArgsMap args) {
    long startTime = System.currentTimeMillis();
    System.out.print("Reading in 17-clue puzzles...");
    PuzzleEntry[] sudoku17 = PuzzleEntry.all17();
    System.out.println(" ✅.");

    // Use maximum of 8 processors while keeping 2 available for the system to keep doing its thing.
    int numThreads = Runtime.getRuntime().availableProcessors();
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
      numThreads, numThreads,
      1L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>()
    );
    pool.prestartAllCoreThreads();

    System.out.println("Solving all puzzles" + (numThreads > 1 ? " ["+numThreads+" threads]." : "."));
    Set<String> solutions = Collections.synchronizedSet(new HashSet<>());
    Set<String> fp2s = Collections.synchronizedSet(new HashSet<>());
    for (int i = 0; i < sudoku17.length; i++) {
      final int j = i;
      pool.submit(() -> {
        PuzzleEntry entry = sudoku17[j];
        solutions.add(entry.solutionStr());
        fp2s.add(entry.fp2());
      });
    }

    pool.shutdown();
    try {
      pool.awaitTermination(10L, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
      pool.shutdownNow();
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Finished in " + (endTime - startTime) + " ms.");
    System.out.printf("Found %d unique solutions.\n", solutions.size());
    System.out.printf("Found %d unique fp2s.\n", fp2s.size());
  }

  static void buildcsv(ArgsMap args) {
    PuzzleEntry.buildCSV("out/sudoku-17.csv", 8);
  }

  static void buildSieveTestCSV(ArgsMap args) {
    defaultInMap(args, "numEntries", "8");
    int numEntries = Integer.parseInt(args.get("numEntries"));

    for (int n = 0; n < numEntries; n++) {
      Sudoku grid = Sudoku.generateConfig();
      SudokuSieve sieve = new SudokuSieve(grid);
      sieve.seedThreaded(sieve.fullPrintCombos(2));
      List<String> itemStrsSorted = new ArrayList<>();
      sieve.items().forEach(item -> itemStrsSorted.add("\""+item.toString()+"\""));
      itemStrsSorted.sort(String::compareTo);
      System.out.printf("%s;2;%d;[%s]\n", grid.toString(), sieve.size(), String.join(",", itemStrsSorted));

      sieve = new SudokuSieve(grid);
      sieve.seedThreaded(sieve.fullPrintCombos(3));
      itemStrsSorted.clear();
      sieve.items().forEach(item -> itemStrsSorted.add("\""+item.toString()+"\""));
      itemStrsSorted.sort(String::compareTo);
      System.out.printf("%s;3;%d;[%s]\n", grid.toString(), sieve.size(), String.join(",", itemStrsSorted));
    }
  }

  public static void createDisjointMaps(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;
    defaultInMap(args, "level", "2");
    defaultInMap(args, "threads", "1");
    String gridStr = args.get("grid");
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().solution() : new Sudoku(gridStr);
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);
    int numThreads = inBounds(Integer.parseInt(args.get("threads")), 1, Runtime.getRuntime().availableProcessors());

    System.out.printf("createDisjointMaps({ grid: %s, level: %d, threads: %d })\n", grid.toString(), level, numThreads);

    System.out.printf("Creating and seeding sieve (level %d)... ", level);
    long start = System.currentTimeMillis();
    SudokuSieve sieve = new SudokuSieve(grid);
    sieve.seed(sieve.digitCombos(level));
    long end = System.currentTimeMillis();
    System.out.printf("done (%d ms).\n", end - start);
    System.out.printf("Sieve contains %d items.\n", sieve.size());
    System.out.println(sieve.toString());

    System.out.println("Creating an index of all disjoint sets...");
    start = System.currentTimeMillis();
    Set<SudokuMask> items = sieve.items();
    SetArray<SudokuMask> disjointSetsMap = new SetArray<>(81);
    Queue<DjNode> q = new LinkedList<>();
    for (SudokuMask m : items) {
      DjNode node = new DjNode(1, new SudokuMask(m));
      q.offer(node);
      disjointSetsMap.add(node.mask, node.n);
      System.out.printf("[%d] [%2d] %s\n", disjointSetsMap.size() - 1, node.n, grid.filterStr(node.mask));
    }

    while (!q.isEmpty()) {
      DjNode curNode = q.poll();

      curNode.nexts(items, nextNode -> {
        if (disjointSetsMap.add(nextNode.mask, nextNode.n)) {
          System.out.printf("[%d] [%2d] %s\n", disjointSetsMap.size() - 1, nextNode.n, grid.filterStr(nextNode.mask));
          q.offer(nextNode);
        }
      });
    }
    end = System.currentTimeMillis();
    System.out.printf("Finished (%d ms); found %d total disjointed sets.\n", end - start, disjointSetsMap.size());

    for (int n = 0; n < disjointSetsMap.length; n++) {
      Set<SudokuMask> set = disjointSetsMap.list.get(n);
      if (set.size() > 0) {
        System.out.printf("[%2d]: %d\n", n, set.size());
      }
    }

  }

  public static class DisjointSetsMap extends SetArray<SudokuMask> {
    Sudoku grid;

    public DisjointSetsMap(Sudoku grid) {
      super(Sudoku.SPACES);
      this.grid = grid;
    }

    public void dumpTo(DisjointSetsMap other) {
      for (int i = length - 1; i > 0; i--) {
        if (!list.get(i).isEmpty()) {
          for (SudokuMask item : list.get(i)) {
            other.add(item, i);
          }
          list.get(i).clear();
        }
      }
      size = 0;
    }

    public void splitOutOverlapping(SudokuMask mask, DisjointSetsMap other) {
      for (int i = length - 1; i > 0; i--) {
        final int _i = i;
        list.get(i).removeIf(item -> {
          boolean intersects = mask.intersects(item);
          if (intersects) {
            other.add(item, _i);
            size--;
          }
          return intersects;
        });
      }
    }

    public void build(int level) {
      SudokuSieve sieve = new SudokuSieve(grid);
      sieve.seedThreaded(sieve.digitCombos(level));
      Set<SudokuMask> items = sieve.items();

      Queue<DjNode> q = new LinkedList<>();
      for (SudokuMask m : items) {
        DjNode node = new DjNode(1, new SudokuMask(m));
        q.offer(node);
        add(node.mask, node.n);
        // System.out.printf("[%d] [%2d] %s\n", size() - 1, node.n, grid.filterStr(node.mask));
      }

      while (!q.isEmpty()) {
        DjNode curNode = q.poll();

        curNode.nexts(items, nextNode -> {
          if (add(nextNode.mask, nextNode.n)) {
            // System.out.printf("[%d] [%2d] %s\n", size() - 1, nextNode.n, grid.filterStr(nextNode.mask));
            q.offer(nextNode);
          }
        });
      }
    }

    public SudokuMask largestNonOverlapping(SudokuMask mask) {
      for (int i = length - 1; i > 0; i--) {
        Set<SudokuMask> set = list.get(i);
        if (!set.isEmpty()) {
          for (SudokuMask item : set) {
            if (!mask.intersects(item)) {
              return new SudokuMask(item);
            }
          }
        }
      }
      return null;
    }

    public int minToSatisfy(SudokuMask mask) {
      // int bc = mask.bitCount();
      for (int i = length - 1; i > 0; i--) {
        Set<SudokuMask> set = list.get(i);
        if (!set.isEmpty()) {
          for (SudokuMask item : set) {
            if (!mask.intersects(item)) {
              return i;
            }
          }
        }
      }
      return 0;
    }
  }

  static class SetArray<T> {
    List<Set<T>> list;
    int length;
    long size;

    SetArray(int length) {
      this.length = length;
      this.size = 0;
      this.list = new ArrayList<>();
      for (int i = 0; i < length; i++) {
        list.add(new HashSet<>());
      }
    }

    long size() {
      return size;
    }

    boolean contains(T data, int index) {
      validateIndex(index);
      return list.get(index).contains(data);
    }

    boolean add(T data, int index) {
      validateIndex(index);
      if (list.get(index).add(data)) {
        size++;
        return true;
      }
      return false;
    }

    private void validateIndex(int index) {
      if (index < 0 || index >= length) {
        throw new IllegalArgumentException("Bad index (" + index + "). Max " + length + ".");
      }
    }
  }

  static class DjNode {
    // The number of UAs this mask contains
    int n;
    SudokuMask mask;
    DjNode() {
      this(0, new SudokuMask());
    }
    DjNode(DjNode other) {
      this(other.n, new SudokuMask(other.mask));
    }
    DjNode(int n, SudokuMask mask) {
      this.n = n;
      this.mask = mask;
    }
    List<DjNode> nexts(List<SudokuMask> maskPool) {
      List<DjNode> nexts = new ArrayList<>();
      for (SudokuMask maybeNext : maskPool) {
        if (!mask.intersects(maybeNext)) {
          nexts.add(new DjNode(n + 1, new SudokuMask(mask).add(maybeNext)));
        }
      }
      return nexts;
    }
    void nexts(Collection<SudokuMask> maskPool, Consumer<DjNode> callback) {
      for (SudokuMask maybeNext : maskPool) {
        if (!mask.intersects(maybeNext)) {
          callback.accept(new DjNode(n + 1, new SudokuMask(mask).add(maybeNext)));
        }
      }
    }
  }

  // TODO do something with this -- maybe create a class that compares performance of various sudoku algorithms,
  // compiles the results, shows them formatted.
  private static void compareCountSolutions(ArgsMap args) {
    Map<String,Integer> puzzleStrs = new HashMap<>() {{
        put("...45.7...5........4......3.8...3.1.9..241.....69...3.2......7.3...7..........3..", 76293);
        put("....5..89......16......1..2...6.3..............1..5..45...6..73.......4..74..89.1", 50035);
        put("..3.5.7.9..7..8.4...8..............6.8...54.2...8..........932.3.42..6......3.1..", 25339);
        put("12..5..8..7.3.9........7..6...56........4.8......92..1....2...8.6.1.......8......", 300986);
        put("1..45...96...1......7...1..3........9....531.......6...9.16.......3.4.6.2...7...1", 33887);
        put(".2..56...8..3..56...........1.2...........64.....9..239.........81.2....26..314..", 7585);
        put(".2.4.6..99...........79..3.........1..9...3.........5.3.8....72...5......65.29..4", 21421);
        put("....5..89....3.......2...........9..2......75..9.8.6.2.51...8.6....9...1.92..1.57", 7535);
        put("....5.7..56...8.4...9.7.......6.....65...94.8..4....2.4.....836.3...7............", 15322);
        put("....56...76....52..95.2...3.......7.2.78...455...9.1...3.....5...8...3.......5...", 2132);
        put("..3.....9.7....65...9.71.....1..78..9....2......54.......9..3............4.1.....", 50920);
        put("...4..7......1...6.........3.....8..7...4......8.3..6.5...7....43...51.897.1...3.", 60419);
        put("1.......9.......4...4...2....2.....8......4.14.8....9..365...1.8.....5.6..56.8...", 39620);
        put("...45.................8..1..1...4...63.........8...195...7..8.1.5..9.3.48.16...5.", 6508);
        put(".....6..9........2.84.97....1...23...9......62...61.4.3...2........38..4.....4...", 64015);
        put("..3..6........8..69.67..1..5.....96.8.9.......67....1....8.....4.8...6......94..3", 68182);
        put("..3.5.7.....2......4891..6.....3.........5.......8...........252.5.....1.795.....", 240665);
        put("1..4..7.9......3...75.8.6...........8.43...6......4...2..1....6..8.........9.5..2", 39901);
        put("...4..7.9...7.8....681...4............6......931.4.....8.2.4...2...6..7....3...9.", 20840);
        put(".2.......9.6.175..........34.....961.....5....7.9.........42...........5....3..2.", 121787);
    }};

    // Total times accrued by each function
    // 0 - countSolutions
    // 1 - countSolutionsAsync (newer)
    // 2 - countSolutionsAsync (older)
    long[][] times = new long[puzzleStrs.size()][3];
    int i = 0;
    int numThreads = 8;

    System.out.println("⚠️ count2 algo (SolutionCountResult) has been deleted.\nThe performance between the two was comparable, but the implementation of count1 was much simpler, and so it stays.");
    System.out.printf("%9s%9s%9s %s\n%s\n", "count1", "count2", "count3", "puzzle", "-".repeat(81+28));
    for (Entry<String,Integer> entry : puzzleStrs.entrySet()) {
      String pStr = entry.getKey();
      int expectedCount = entry.getValue();
      long actualCount, start, end;

      start = System.currentTimeMillis();
      actualCount = new Sudoku(pStr).countSolutions();
      end = System.currentTimeMillis();
      if (actualCount == expectedCount) {
        times[i][0] = end - start;
        System.out.printf("%9d", times[i][0]);
      } else {
        System.out.printf("\n❌ countSolutions mismatch (expected %d, got %d)\n%s\n", expectedCount, actualCount, pStr);
      }

      start = System.currentTimeMillis();
      actualCount = new Sudoku(pStr).countSolutionsAsync(numThreads);
      end = System.currentTimeMillis();
      if (actualCount == expectedCount) {
        times[i][1] = end - start;
        System.out.printf("%9d", times[i][1]);
      } else {
        System.out.printf("\n❌ countSolutionsAsync1 mismatch (expected %d, got %d)\n%s\n", expectedCount, actualCount, pStr);
      }

      start = System.currentTimeMillis();
      // SolutionCountResult countResult = new Sudoku(pStr).countSolutionsAsync(numThreads, 1L, TimeUnit.MINUTES);
      // try {
      //   countResult.await();
      // } catch (InterruptedException e) {
      //   System.out.printf("\n❌ countSolutionsAsync2 failed\n%s\n", pStr);
      //   e.printStackTrace();
      // }
      // actualCount = countResult.get();
      end = System.currentTimeMillis();
      if (actualCount == expectedCount) {
        times[i][2] = end - start;
        System.out.printf("%9d", times[i][2]);
      } else {
        System.out.printf("\n❌ countSolutionsAsync2 mismatch (expected %d, got %d)\n%s\n", expectedCount, actualCount, pStr);
      }

      i++;
      System.out.println(" " + pStr);
    }
  }
}
