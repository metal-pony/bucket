package com.metal_pony.bucket.sudoku.drivers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.metal_pony.bucket.sudoku.Sudoku;
import com.metal_pony.bucket.sudoku.SudokuSieve;
import com.metal_pony.bucket.sudoku.Sudoku.SolutionCountResult;
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
    put("generateBands", Main::generateInitialBands);
    // --level %d --grid %s --threads %d
    put("sieve", Main::createSieve);
    // --level %d --grid %s --threads %d
    put("fingerprint", Main::fingerprint);
    // put("adhoc", Main::doThing); // For testing / experimentation
  }};

  static boolean verbose;

  private static void play(ArgsMap args) {
    defaultInMap(args, "clues", "27");
    int clues = inBounds(Integer.parseInt(args.get("clues")), 17, 81);
    Sudoku puzzle = Sudoku.generatePuzzle(clues);
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

  // TODO Adapt for multiple threads
  // TODO Output as json
  // TODO Difficulty option
  private static void generatePuzzles(ArgsMap args) {
    defaultInMap(args, "amount", "1");
    defaultInMap(args, "clues", "27");
    defaultInMap(args, "threads", "1");

    String gridStr = args.get("grid");
    boolean useSameSolution = (gridStr == null);
    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);

    final int amount = inBounds(Integer.parseInt(args.get("amount")), 1, 1_000_000);
    final int clues = inBounds(Integer.parseInt(args.get("clues")), 19, Sudoku.SPACES);
    final int threads = inBounds(Integer.parseInt(args.get("threads")), 1, 4);

    List<SudokuMask> sieve = new ArrayList<>();

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

  /**
   * level [2,4] (default: 2)
   * grid? [str] (default: randomly generated)
   * threads? [1, #cores - 2] (default: 1 if omitted; MAX if only "--threads" given)
   */
  public static void createSieve(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;

    defaultInMap(args, "level", "2");

    String gridStr = args.get("grid");
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);

    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);
    System.out.println(grid.toString());
    SudokuSieve sieve = new SudokuSieve(grid);

    sieve.seed(level);

    System.out.println(sieve.toString());
    System.out.printf("Added %d items to sieve.\n", sieve.size());
  }

  public static void fingerprint(ArgsMap args) {
    final int MIN_LEVEL = 2;
    final int MAX_LEVEL = 4;

    defaultInMap(args, "level", "2");

    String gridStr = args.get("grid");
    int level = inBounds(Integer.parseInt(args.get("level")), MIN_LEVEL, MAX_LEVEL);

    Sudoku grid = (gridStr == null) ? Sudoku.configSeed().firstSolution() : new Sudoku(gridStr);
    System.out.println(grid.toString());
    SudokuSieve sieve = new SudokuSieve(grid);

    sieve.seed(level);
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
}
