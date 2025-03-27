package com.metal_pony.bucket.sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.Gson;
import com.metal_pony.bucket.sudoku.drivers.Main;
import com.metal_pony.bucket.sudoku.util.SudokuMask;
import com.metal_pony.bucket.util.Counting;
import com.metal_pony.bucket.util.Shuffler;
import com.metal_pony.bucket.util.ThreadPool;

public class Sudoku {
    static final String PUZZLES_17_RESOURCE = "17-puzzle-records.json";

    public static List<Sudoku> readSudokusFromFile(String filename, boolean log) throws FileNotFoundException {
        if (log) {
            System.out.print("Reading sudoku file...");
        }

        List<Sudoku> list = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            list.add(new Sudoku(line));
        }
        scanner.close();

        if (log) {
            System.out.println("Done.");
            System.out.printf("Read %d sudokus.\n", list.size());
        }

        return list;
    }

    public static void batchAndWait(int threads, long waitTime, TimeUnit waitTimeUnit, List<Runnable> work, boolean shouldPrint) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        work.forEach(w -> pool.submit(w));

        if (shouldPrint) {
            long workSize = (long)work.size();
            long completed = 0L;
            int numDots = 64;
            int dotsPrinted = 0;
            // int workPerDot = work.size() / numDots;
            long _waitTime = waitTimeUnit.toMillis(waitTime);
            boolean aborted = false;
            System.out.printf("[ %s ]\n", "=".repeat(numDots));
            System.out.print("[ ");
            long startTime = System.currentTimeMillis();
            while ((completed = pool.getCompletedTaskCount()) < workSize) {
                while (dotsPrinted < (completed*numDots)/workSize) {
                    System.out.print('.');
                    dotsPrinted++;
                }

                if (System.currentTimeMillis() - startTime > _waitTime) {
                    aborted = true;
                    break;
                }

                // Thread.onSpinWait();

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (aborted) {
                System.out.println(" ]\n⚠️ Took too long. Aborted. ⚠️");
            } else {
                while (dotsPrinted < numDots) {
                    System.out.print('.');
                    dotsPrinted++;
                    try {
                        Thread.sleep(25L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println(" ]\nDone.");
            }
        }

        pool.shutdown();
        if (!shouldPrint) {
            try {
                pool.awaitTermination(waitTime, waitTimeUnit);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PuzzleEntry {
        String puzzle;
        String solution;
        String fingerprint2;
        String fingerprint3;
        String fingerprint4;

        public PuzzleEntry(
            String puzzle,
            String solution,
            String fingerprint2,
            String fingerprint3,
            String fingerprint4
        ) {
            this.puzzle = puzzle;
            this.solution = solution;
            this.fingerprint2 = fingerprint2;
            this.fingerprint3 = fingerprint3;
            this.fingerprint4 = fingerprint4;
        }

        public Sudoku puzzle() {
            return new Sudoku(puzzle);
        }

        public void clear() {
            solution = null;
            fingerprint2 = null;
            fingerprint3 = null;
            fingerprint4 = null;
        }

        public String solution() {
            if (solution == null) {
                solution = new Sudoku(puzzle).firstSolution().toString();
            }
            return solution;
        }

        public String fingerprint2() {
            if (fingerprint2 == null) {
                fingerprint2 = new Sudoku(solution()).fingerprint(2);
            }
            return fingerprint2;
        }

        public String fingerprint3() {
            if (fingerprint3 == null) {
                fingerprint3 = new Sudoku(solution()).fingerprint(3);
            }
            return fingerprint3;
        }

        public String fingerprint4() {
            if (fingerprint4 == null) {
                fingerprint4 = new Sudoku(solution()).fingerprint(4);
            }
            return fingerprint4;
        }

        @Override
        public String toString() {
            return String.format(
                """
                {
                  "puzzle":       "%s",
                  "solution":     "%s",
                  "fingerprint2": "%s",
                  "fingerprint3": "%s",
                  "fingerprint4": "%s"
                }""",
                puzzle, solution(), fingerprint2(), fingerprint3(), fingerprint4()
            );
        }

        public static PuzzleEntry[] readFromJsonInStream(InputStream inStream) {
            try (Reader reader = new InputStreamReader(inStream)) {
                Gson gson = new Gson();
                return gson.fromJson(reader, PuzzleEntry[].class);
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
                return new PuzzleEntry[0];
            }
        }

        public static PuzzleEntry[] all17() {
            return PuzzleEntry.readFromJsonInStream(Main.resourceStream(PUZZLES_17_RESOURCE));
        }
    }

    /* ************************************
     *           !! IGNORE !!
     * Used for adhoc testing.
     * ************************************/
    public static void main2(String[] args) {
        // Read puzzle entries from JSON file and recalculate solutions and fingerprints.
        PuzzleEntry[] records = PuzzleEntry.readFromJsonInStream(Main.resourceStream(PUZZLES_17_RESOURCE));
        int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors() - 4);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        Object sysoutLock = new Object();
        for (PuzzleEntry record : records) {
            pool.submit(() -> {
                record.clear();
                String recordStr = record.toString();
                synchronized (sysoutLock) {
                    System.out.printf("%s,\n", recordStr);
                }
            });
        }
        pool.shutdown();

        // 218574639573896124469123578721459386354681792986237415147962853695318247832745961

        // // Load from file
        // String bandsFileName = "initial-bands.txt";
        // File bandsFile = new File(bandsFileName);
        // try (Scanner scanner = new Scanner(bandsFile)) {
		// 	while(scanner.hasNextLine()) {
		// 		String line = scanner.nextLine().trim();
		// 		if (line.isEmpty()) {
		// 			continue;
		// 		}
        //         // allBands.add(new BigInteger(line));
        //         fullBandSet.add(line);
		// 	}
        // } catch (IOException ex) {
		// 	ex.printStackTrace();
		// }
        // System.out.printf("Loaded %d bands from %s\n", fullBandSet.size(), bandsFileName);

        // // Run through reduction func
        // Set<String> reduced = Main.reduceFullBandSet(fullBandSet);
    }

    static final boolean isDebugging = true;
    static void debug(String formatMsg, Object... args) {
        if (isDebugging) {
            System.out.printf(formatMsg, args);
        }
    }
    static void debug(String msg) {
        if (isDebugging) {
            System.out.println(msg);
        }
    }

    static final char EMPTY_CHAR = '.';

    // TODO rename "RANK"
    public static final int DIGITS_SQRT = 3;
    public static final int DIGITS = 9; // rank^2
    public static final int SPACES = 81; // rank^2^2
    static final int ALL = 511; // 2^rank^2 - 1
    public static final int MIN_CLUES = 17; // rank^2 * 2 - 1

    static final int ROW_MASK = ALL << (DIGITS * 2);
    static final int COL_MASK = ALL << DIGITS;
    static final int REGION_MASK = ALL;
    static final int FULL_CONSTRAINTS = ROW_MASK | COL_MASK | REGION_MASK;

    public static final int[] ENCODER = new int[] { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256 };
    static final int[] DECODER = new int[1<<DIGITS];
    static {
        for (int digit = 1; digit <= DIGITS; digit++) {
            DECODER[1 << (digit - 1)] = digit;
        }
    }

    /**
     * Maps candidates mask to the array of digits it represents.
     */
    public static final int[][] CANDIDATES_ARR = new int[1<<DIGITS][];

    /**
     * Maps candidates masks to the array of digits (encoded) it represents.
     */
    static final int[][] CANDIDATES = new int[CANDIDATES_ARR.length][];
    static {
        for (int val = 0; val < CANDIDATES_ARR.length; val++) {
            CANDIDATES_ARR[val] = new int[Integer.bitCount(val)];
            CANDIDATES[val] = new int[Integer.bitCount(val)];
            int _val = val;
            int i = 0;
            int j = 0;
            int digit = 1;
            while (_val > 0) {
                if ((_val & 1) > 0) {
                    CANDIDATES_ARR[val][i++] = digit;
                    CANDIDATES[val][j++] = ENCODER[digit];
                }
                _val >>= 1;
                digit++;
            }
        }
    }

    /**
     * Maps indices [0, 511] to its bit count.
     */
    static final int[] BIT_COUNT_MAP = new int[1<<DIGITS];

    /**
     * Digit combinations indexed by bit count (aka digit count).
     */
    public static final int[][] DIGIT_COMBOS_MAP = new int[DIGITS + 1][];
    static {
        for (int nDigits = 0; nDigits < DIGIT_COMBOS_MAP.length; nDigits++) {
            DIGIT_COMBOS_MAP[nDigits] = new int[Counting.nChooseK(DIGITS, nDigits).intValueExact()];
        }
        int[] combosCount = new int[DIGITS + 1];
        for (int i = 0; i < BIT_COUNT_MAP.length; i++) {
            int bits = Integer.bitCount(i);
            BIT_COUNT_MAP[i] = bits;
            DIGIT_COMBOS_MAP[bits][combosCount[bits]++] = i;
        }
    }

    static int encode(int digit) {
        return ENCODER[digit];
    }

    public static int decode(int encoded) {
        return DECODER[encoded];
    }

    public static boolean isDigit(int encoded) {
        return DECODER[encoded] > 0;
    }

    public static int[] CELL_ROWS = new int[SPACES];
    public static int[] CELL_COLS = new int[SPACES];
    public static int[] CELL_REGIONS = new int[SPACES];
    public static int[][] ROW_INDICES = new int[DIGITS][DIGITS];
    public static int[][] COL_INDICES = new int[DIGITS][DIGITS];
    public static int[][] REGION_INDICES = new int[DIGITS][DIGITS];
    public static int[][] BAND_INDICES = new int[3][3*DIGITS];
    public static int[][] STACK_INDICES = new int[3][3*DIGITS];
    public static int[][][] BAND_ROW_INDICES = new int[3][3][DIGITS];
    public static int[][][] STACK_COL_INDICES = new int[3][3][DIGITS];
    static {
        int[] rowi = new int[DIGITS];
        int[] coli = new int[DIGITS];
        int[] regi = new int[DIGITS];
        for (int i = 0; i < SPACES; i++) {
            int row = SudokuUtility.cellRow(i);
            int col = SudokuUtility.cellCol(i);
            int region = SudokuUtility.cellRegion(i);
            CELL_ROWS[i] = row;
            CELL_COLS[i] = col;
            CELL_REGIONS[i] = region;

            ROW_INDICES[row][rowi[row]++] = i;
            COL_INDICES[col][coli[col]++] = i;
            REGION_INDICES[region][regi[region]++] = i;

            int band = row / DIGITS_SQRT;
            int rowInBand = row % DIGITS_SQRT;
		    int stack = col / DIGITS_SQRT;
            int colInStack = col % DIGITS_SQRT;
            int indexInBand = i % (DIGITS * DIGITS_SQRT);
            int indexInStack = (row * DIGITS_SQRT) + colInStack;
            BAND_INDICES[band][indexInBand] = i;
            STACK_INDICES[stack][indexInStack] = i;
            BAND_ROW_INDICES[band][rowInBand][col] = i;
            STACK_COL_INDICES[stack][colInStack][row] = i;
        }
    }
    public static int[][] ROW_NEIGHBORS = new int[SPACES][DIGITS - 1];
    public static int[][] COL_NEIGHBORS = new int[SPACES][DIGITS - 1];
    public static int[][] REGION_NEIGHBORS = new int[SPACES][DIGITS - 1];
    public static int[][] CELL_NEIGHBORS = new int[SPACES][3*(DIGITS-1) - (DIGITS-1)/2]; // Not checked if true for other ranks
    static {
        for (int ci = 0; ci < SPACES; ci++) {
            int row = SudokuUtility.cellRow(ci);
            int col = SudokuUtility.cellCol(ci);
            int region = SudokuUtility.cellRegion(ci);

            int ri = 0;
            int coli = 0;
            int regi = 0;
            int ni = 0;

            for (int cj = 0; cj < SPACES; cj++) {
                if (ci == cj) continue;
                int jrow = SudokuUtility.cellRow(cj);
                int jcol = SudokuUtility.cellCol(cj);
                int jregion = SudokuUtility.cellRegion(cj);

                if (jrow == row) {
                    ROW_NEIGHBORS[ci][ri++] = cj;
                }
                if (jcol == col) {
                    COL_NEIGHBORS[ci][coli++] = cj;
                }
                if (jregion == region) {
                    REGION_NEIGHBORS[ci][regi++] = cj;
                }
                if (jrow == row || jcol == col || jregion == region) {
                    CELL_NEIGHBORS[ci][ni++] = cj;
                }
            }
        }
    }

    public static final SudokuMask[] BAND_FILTERS;
    public static final SudokuMask[] STACK_FILTERS;
    static {
        BAND_FILTERS = new SudokuMask[BAND_INDICES.length];
        for (int bandIndex = 0; bandIndex < BAND_INDICES.length; bandIndex++) {
            SudokuMask bf = new SudokuMask();
            for (int i : BAND_INDICES[bandIndex]) {
                bf = bf.setBit(i);
            }
            BAND_FILTERS[bandIndex] = bf;
            // System.out.printf("BAND_FILTERS[%d] = %s\n", bandIndex, BAND_FILTERS[bandIndex].toString(2));
        }

        STACK_FILTERS = new SudokuMask[STACK_INDICES.length];
        for (int stackIndex = 0; stackIndex < STACK_INDICES.length; stackIndex++) {
            SudokuMask sf = new SudokuMask();
            for (int i : STACK_INDICES[stackIndex]) {
                sf = sf.setBit(SPACES - 1 - i);
            }
            STACK_FILTERS[stackIndex] = sf;
            // System.out.printf("STACK_FILTERS[%d] = %s\n", stackIndex, STACK_FILTERS[stackIndex].toString(2));
        }
    }

    /** Cell digits, as one would see on a sudoku board.*/
    int[] digits;

    /**
     * Tracks a cell's possible digits, encoded in 9-bit values (one bit per possible digit).
     *
     * e.g., If `candidates[55] = 0b110110001`, then the cell at index `55` has the possible
     * digits `9, 8, 6, 5, and 1`.
     *
     * `0b111111111` indicates that the cell can be any digit, and `0b000000000` indicates
     * that the cell cannot be any digit (and the puzzle is likely invalid).
     */
    public int[] candidates;

    /**
     * Tracks the digits that have been used for each row, column, and region - combined into encoded 27-bit values.
     *
     * The encoding works as follows:
     * [9 bits for the row digits][9 bits for the column][9 bits for the region]
     *
     * Some bit manipulation is required to access the values for any given area.
     *
     * e.g., Get the digits used by row 7: `(constraints[7] >> 18) & ALL`.
     * This yields a 9-bit value which is an encoded form identical to `candidates`.
     */
    int[] constraints;

    char[] str;

    int numEmptyCells = SPACES;

    boolean isValid = true;

    // TODO Implement isSolved cache
    // This should be cached true when isSolved is called, and invalidated whenever a value is changed
    private boolean _isSolved = false;

    public Sudoku() {
        this.digits = new int[SPACES];
        this.candidates = new int[SPACES];
        Arrays.fill(this.candidates, ALL);
        this.constraints = new int[DIGITS];
        this.str = new char[SPACES];
        Arrays.fill(this.str, EMPTY_CHAR);
    }

    public Sudoku(Sudoku other) {
        this();
        this.numEmptyCells = other.numEmptyCells;
        this.isValid = other.isValid;
        System.arraycopy(other.digits, 0, this.digits, 0, SPACES);
        System.arraycopy(other.candidates, 0, this.candidates, 0, SPACES);
        System.arraycopy(other.constraints, 0, this.constraints, 0, DIGITS);
        System.arraycopy(other.str, 0, this.str, 0, SPACES);
    }

    public Sudoku(String str) {
        this();

        // Replace '-' and '.' with '0's
        str = str.replaceAll("-", "0".repeat(DIGITS)).replaceAll("[^1-9]", "0");

        if (str.length() != SPACES) {
            throw new IllegalArgumentException("str is invalid as sudoku grid");
        }

		for (int i = 0; i < SPACES; i++) {
            int digit = str.charAt(i) - '0';
            if (digit > 0) {
                setDigit(i, digit);
            }
		}
    }

    public Sudoku(int[] digits) {
        this();

        if (digits.length != SPACES) {
            throw new IllegalArgumentException("sudoku initialization failed: insufficient board values");
        }

        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] > 0 || digits[ci] <= DIGITS) {
                setDigit(ci, digits[ci]);
            }
        }
    }

    public int getDigit(int ci) {
        return digits[ci];
    }

    public void setDigit(int ci, int digit) {
        int prevDigit = this.digits[ci];
        if (prevDigit == digit) return;

        digits[ci] = digit;
        candidates[ci] = ENCODER[digit];
        // str[ci] = (digit > 0) ? (char)(digit + '0') : EMPTY_CHAR;

        // Digit removed (or replaced)
        if (prevDigit > 0) {
            numEmptyCells++;
            removeConstraint(ci, prevDigit);
        }
        // Digit added (or replaced)
        if (digit > 0) {
            numEmptyCells--;
            addConstraint(ci, digit);
        }
    }

    void addConstraint(int ci, int digit) {
        int dMask = ENCODER[digit];
        constraints[CELL_ROWS[ci]] |= dMask << (DIGITS*2);
        constraints[CELL_COLS[ci]] |= dMask << DIGITS;
        constraints[CELL_REGIONS[ci]] |= dMask;
    }

    void removeConstraint(int ci, int digit) {
        int dMask = ENCODER[digit];
        constraints[CELL_ROWS[ci]] &= ~(dMask << (DIGITS*2));
        constraints[CELL_COLS[ci]] &= ~(dMask << DIGITS);
        constraints[CELL_REGIONS[ci]] &= ~dMask;
    }

    int cellConstraints(int ci) {
        return (
            (constraints[CELL_ROWS[ci]] >> (DIGITS*2)) |
            (constraints[CELL_COLS[ci]] >> (DIGITS)) |
            constraints[CELL_REGIONS[ci]]
        ) & ALL;
    }

    private int[] _digitsArr = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    public void fillRegions(int regionsMask) {
        for (int regIndex = 0; regIndex < DIGITS; regIndex++) {
            if ((regionsMask & (1<<(DIGITS - 1 - regIndex))) > 0) {
                Shuffler.shuffle(_digitsArr);
                for (int i = 0; i < DIGITS; i++) {
                    this.setDigit(REGION_INDICES[regIndex][i], _digitsArr[i]);
                }
            }
        }
    }

    private static final int[] regionsMasks = new int[] {
        0b100010001, 0b100001010, 0b010100001,
        0b010001100, 0b001100010, 0b001010100
    };
    public static Sudoku configSeed() {
        Sudoku seed = new Sudoku();
        seed.fillRegions(regionsMasks[0]);
        return seed;
    }

    public int[] getBoard() {
        int[] board = new int[SPACES];
        System.arraycopy(digits, 0, board, 0, SPACES);
        return board;
    }

    public int[] getCandidates() {
        int[] board = new int[SPACES];
        System.arraycopy(candidates, 0, board, 0, SPACES);
        return board;
    }

    public void resetEmptyCells() {
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] == 0) candidates[ci] = ALL;
        }
    }

    public void resetConstraints() {
        isValid = true;
        constraints = new int[DIGITS];
        for (int i = 0; i < SPACES; i++) {
            if (digits[i] > 0) {
                if ((cellConstraints(i) & encode(digits[i])) > 0) {
                    isValid = false;
                }
                addConstraint(i, digits[i]);
            }
        }
    }

    public boolean isFull() {
        return this.numEmptyCells == 0;
    }

    public boolean isEmpty() {
        return numEmptyCells == SPACES;
    }

    public int numEmptyCells() {
        return numEmptyCells;
    }

    public int numClues() {
        return SPACES - numEmptyCells;
    }

    public boolean isSolved() {
        for (int c : constraints) {
            if (c != FULL_CONSTRAINTS) {
                return false;
            }
        }
        return true;
    }

    public void reduce() {
        for (int i = 0; i < SPACES; i++) reduceCell(i);
    }

    void reduceCell(int ci) {
        if (digits[ci] > 0) return;
        if (candidates[ci] == 0) {
            isValid = false;
            return;
        }

        // ? If candidate constraints reduces to 0, then the board is likely invalid.
        int reducedCandidates = (candidates[ci] & ~cellConstraints(ci));
        if (reducedCandidates <= 0) {
            isValid = false;
            setDigit(ci, 0);
            return;
        }

        // If by applying the constraints, the number of candidates is reduced to 1,
        // then the cell is solved.
        if (isDigit(reducedCandidates)) {
            setDigit(ci, DECODER[reducedCandidates]);
        } else {
            int uniqueCandidate = getUniqueCandidate(ci);
            if (uniqueCandidate > 0) {
                setDigit(ci, DECODER[uniqueCandidate]);
                reducedCandidates = uniqueCandidate;
            } else {
                candidates[ci] = reducedCandidates;
            }
        }

        if (reducedCandidates < candidates[ci]) {
            for (int n : CELL_NEIGHBORS[ci]) reduceCell(n);
        }
    }

    int getUniqueCandidate(int ci) {
        for (int candidate : CANDIDATES[candidates[ci]]) {
            boolean unique = true;
            for (int ni : ROW_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) return candidate;

            unique = true;
            for (int ni : COL_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) return candidate;

            unique = true;
            for (int ni : REGION_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) return candidate;
        }

        return 0;
    }

    public static Sudoku generatePuzzle2(
        Sudoku grid,
        int numClues,
        List<SudokuMask> sieve,
        int difficulty,
        long timeoutMs,
        boolean useSieve
    ) {
        if (numClues < MIN_CLUES || numClues > SPACES)
            return null;
        if (grid == null)
            grid = configSeed().firstSolution();
        if (numClues == SPACES)
            return grid;
        if (!grid.isSolved())
            throw new IllegalArgumentException("Solution grid is invalid");
        if (difficulty < 0 || difficulty > 4)
            throw new IllegalArgumentException(String.format("Invalid difficulty (%d); expected 0 <= difficulty <= 4", difficulty));
        if (sieve == null)
            sieve = new ArrayList<>();

        long start = System.currentTimeMillis();
        // const FULLMASK = (1n << BigInt(SPACES)) - 1n;
        // SudokuMask FULLMASK = SudokuMask.full();
        int maskFails = 0;
        int puzzleCheckFails = 0;
        int putBacks = 0;
        SudokuMask mask = SudokuMask.full();
        List<Integer> remaining = Shuffler.range(SPACES);
        ArrayList<Integer> removed = new ArrayList<>();

        while (remaining.size() > numClues) {
            int startChoices = remaining.size();
            Shuffler.shuffle(remaining);
            for (int i = 0; i < remaining.size() && remaining.size() > numClues; i++) {
                int choice = remaining.get(i);
                // mask &= ~cellMask(choice);
                mask.unsetBit(choice);

                // Check if mask satisfies sieve
                boolean satisfies = true;
                for (SudokuMask item : sieve) {
                    if (!mask.hasAnyOverlapWith(item)) {
                        satisfies = false;
                        break;
                    }
                }

                // If not, or if there are multiple solutions,
                // put the cell back and try the next
                if (!satisfies) {
                    maskFails++;
                    // mask |= cellMask(choice);
                    mask.setBit(choice);

                    // Once in awhile, check the time
                    if (timeoutMs > 0L && (maskFails % 100) == 0) {
                        if ((System.currentTimeMillis() - start) > timeoutMs) {
                            return null;
                        }
                    }

                    continue;
                }

                if (grid.filter(mask).solutionsFlag() != 1) {
                    puzzleCheckFails++;
                    if (useSieve && puzzleCheckFails == 100 && sieve.size() < 36) {
                        SudokuSieve.seedSieve(grid, sieve, 2);
                    } else if (useSieve && puzzleCheckFails == 2500 && sieve.size() < 200) {
                        SudokuSieve.seedSieve(grid, sieve, 3);
                    } else if (useSieve && puzzleCheckFails > 10000 && sieve.size() < 1000) {
                        SudokuSieve.searchForItemsFromMask(grid, sieve, mask, false);
                    } else if (useSieve && puzzleCheckFails > 25000) {
                        // SudokuSieve.searchForItemsFromMask(grid, sieve, mask, false);
                    }

                    mask.setBit(choice);
                    continue;
                }

                removed.add(choice);
                remaining.remove(i);
                i--;
            }

            // If no cells were chosen
            // - Put some cells back and try again
            if (
            (
                remaining.size() == numClues &&
                difficulty > 0 &&
                grid.filter(mask).solutionsFlag() == 1 //&&
                // grid.filter(mask).difficulty() != difficulty
            ) || remaining.size() == startChoices
            ) {
                int numToPutBack = 5;//1 + (putBacks % 4) + (((putBacks % 8)*Math.random())|0);
                Shuffler.shuffle(removed);
                for (int i = 0; i < numToPutBack; i++) {
                    int cell = removed.remove(removed.size() - 1);
                    remaining.add(cell);
                    mask.setBit(cell);
                    if (removed.size() == 0)
                        break;
                }
                putBacks++;
            }
        }

        System.out.printf(
            "{maskFails: %d; puzzleCheckFails: %d; sieveSize: %d; timeMs: %d} %s%n",
            maskFails,
            puzzleCheckFails,
            sieve.size(),
            (System.currentTimeMillis() - start),
            grid.filter(mask).toString()
        );
        SudokuSieve _sieve = new SudokuSieve(grid.getBoard());
        for (SudokuMask m : sieve) {
            _sieve.add(m);
        }
        System.out.println(_sieve.toString());

        return grid.filter(mask);
    }

    static class SudokuNode {
        // int[] digits;
        // int[] candidates;
        // int[] constraints;
        Sudoku sudoku;
        int index = -1;
        int values = -1;
        ArrayList<SudokuNode> nexts;
        SudokuNode(Sudoku sudoku) {
            this.sudoku = sudoku;
            sudoku.reduce();
            index = sudoku.pickEmptyCell();
            if (index != -1) {
                values = sudoku.candidates[index];
            }
        }
        // void generateNextsIfNull() {
        //     if (nexts == null) {
        //         nexts = new ArrayList<>();
        //         sudoku.getNextsAdditive(n -> nexts.add(new SudokuNode(n)));
        //         Shuffler.shuffle(nexts);
        //     }
        // }
        SudokuNode next() {
            // If this node's sudoku had no emptycells, then `index` and `values` would have never been set, and both would still be -1
            if (values <= 0 || !sudoku.isValid) {
                return null;
            }

            Sudoku s = new Sudoku(sudoku);
            int[] candidateDigits = CANDIDATES_ARR[values];
            int randomCandidateDigit = candidateDigits[ThreadLocalRandom.current().nextInt(candidateDigits.length)];
            s.setDigit(index, randomCandidateDigit);
            values &= ~(ENCODER[randomCandidateDigit]);
            return new SudokuNode(s);
            // generateNextsIfNull();
            // return (nexts.isEmpty()) ? null : nexts.remove(nexts.size() - 1);
        }
        boolean hasNext() {
            return (values > 0 && sudoku.isValid) ? true : false;
        }
    }

    public void searchForSolutions3(Function<Sudoku,Boolean> solutionFoundCallback) {
        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();

        if (!root.isValid) {
            return;
        }

        // debug("          %s\n", root.toString());
        // if (root.reduce()) {
        //     debug("reduced > %s\n", root.toString());
        // }

        // ArrayList<Sudoku> results = new ArrayList<>();
        Stack<SudokuNode> stack = new Stack<>();
        stack.push(new SudokuNode(root));

        // boolean keepGoing = true;
        while (!stack.isEmpty()) { // && keepGoing) {
            SudokuNode top = stack.peek();
            Sudoku sudoku = top.sudoku;
            // String pred = " ".repeat(stack.size());
            // debug("%s        > %s\n", pred, sudoku.toString());

            // if (sudoku.reduce()) {
            //     debug("%sreduced > %s\n", pred, sudoku.toString());
            // }

            if (sudoku.isSolved()) {
                // results.add(new Sudoku(sudoku));
                // debug("%s **⭐️** > %s\n", pred, sudoku.toString());
                stack.pop();
                if (solutionFoundCallback.apply(sudoku)) {
                    continue;
                } else {
                    // keepGoing = false;
                    break;
                }
            }

            SudokuNode next = top.next();

            if (next == null) {
                stack.pop();
                // top.nexts = null;
            } else {
                stack.push(next);
            }
        }

        // return results;
    }

    public List<Future<List<Sudoku>>> searchForSolutions4() {
        List<Future<List<Sudoku>>> allResults = new ArrayList<>();

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();

        Queue<SudokuNode> q = new LinkedList<>();
        q.offer(new SudokuNode(root));

        List<Sudoku> preSolved = new ArrayList<>();
        // int maxSplitSize = 16;
        int maxSplitSize = 1 << 16;
        while (!q.isEmpty() && q.size() < maxSplitSize) {
            SudokuNode top = q.peek();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                q.poll();
                preSolved.add(sudoku);
                continue;
            }

            SudokuNode next = top.next();

            if (next == null) {
                q.poll();
            } else {
                q.offer(next);
            }
        }

        if (!preSolved.isEmpty()) {
            allResults.add(ThreadPool.submit(() -> preSolved));
        }

        for (SudokuNode node : q) {
            List<Sudoku> results = new ArrayList<>();
            allResults.add(ThreadPool.submit(() -> {
                node.sudoku.searchForSolutions3(solution -> {
                    results.add(solution);
                    return true;
                });
                return results;
            }));
        }

        return allResults;
    }

    /**
     * Finds all solutions to this sudoku, using the given number of threads. This method blocks
     * until all solutions are found, or until the specified amount of time has elapsed,
     * at which point the TimeOutException will be thrown. Timeout will default to 1 hour if not positive.
     *
     * The given callback will be invoked periodically by each thread with a list of
     * at most <code>batchSize</code> solutions as they are accumulated.
     * @param solutionBatchCallback
     * @param batchSize
     * @param numThreads
     * @param timeout
     * @param timeoutUnit
     * @return True if all solutions were found; otherwise false (due to timeout or interruption).
     */
    public boolean searchForSolutionsAsync(
        Consumer<List<Sudoku>> solutionBatchCallback,
        int batchSize,
        int numThreads,
        long timeout,
        TimeUnit timeoutUnit
    ) {
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be positive");
        if (numThreads < 1) throw new IllegalArgumentException("numThreads must be positive");
        if (timeout < 0L) timeout = 1L;

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();

        Queue<SudokuNode> q = new LinkedList<>();
        q.offer(new SudokuNode(root));

        List<Sudoku> solvedBeforeSplit = new ArrayList<>();
        final int MAX_QUEUE_SIZE = (1 << 10);
        while (!q.isEmpty() && q.size() < MAX_QUEUE_SIZE) {
            SudokuNode top = q.poll();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                solvedBeforeSplit.add(sudoku);
                if (solvedBeforeSplit.size() == batchSize) {
                    solutionBatchCallback.accept(new ArrayList<>(solvedBeforeSplit));
                    solvedBeforeSplit.clear();
                }
                continue;
            }

            SudokuNode next;
            while ((next = top.next()) != null) {
                q.offer(next);
            }
        }

        if (!solvedBeforeSplit.isEmpty()) {
            solutionBatchCallback.accept(solvedBeforeSplit);
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            numThreads,
            numThreads,
            1L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );

        final int BATCH_SIZE = batchSize;
        for (SudokuNode node : q) {
            pool.submit(() -> {
                List<Sudoku> resultBatch = new ArrayList<>();
                node.sudoku.searchForSolutions3(solution -> {
                    resultBatch.add(solution);
                    if (resultBatch.size() == BATCH_SIZE) {
                        solutionBatchCallback.accept(new ArrayList<>(resultBatch));
                        resultBatch.clear();
                    }
                    return true;
                });
                solutionBatchCallback.accept(resultBatch);
            });
        }

        pool.shutdown();
        try {
            boolean success = (timeout > 0L) ?
                pool.awaitTermination(timeout, timeoutUnit) :
                pool.awaitTermination(1L, TimeUnit.HOURS);
            if (!success) {
                pool.shutdownNow();
            }
            return success;
		} catch (InterruptedException e) {
			e.printStackTrace();
            pool.shutdownNow();
            return false;
		}
    }

    public static class SolutionCountResult {
        private ThreadPoolExecutor pool;
        private long timeout;
        private TimeUnit timeoutUnit;
        private List<Future<Boolean>> tasks;

        private AtomicLong longCount;
        private SolutionCountResult(long timeout, TimeUnit timeoutUnit) {
            this.longCount = new AtomicLong();
            this.timeoutUnit = (timeout <= 0L) ? TimeUnit.HOURS : timeoutUnit;
            this.timeout = (timeout <= 0L) ? 1L : timeout;
            this.tasks = new ArrayList<>();
        }

        /** Gets the current count.*/
        public long get() {
            return longCount.get();
        }

        /**
         * Attempts to get the count as an integer.
         * @return Solution count as an integer; or -1 if count is larger than <code>Integer.MAX_VALUE</code>.
         */
        public int getInt() {
            if (get() <= (long)Integer.MAX_VALUE) {
                return (int)get();
            } else {
                return -1;
            }
        }

        /** Gets whether the count has completed.*/
        public boolean isDone() {
            return pool == null || pool.isTerminated();
        }

        /** Gets whether the count was successful, i.e. it didn't time out or get interrupted.*/
        public boolean wasSuccessful() {
            return isDone() && removeCompletedTasks();
        }

        public void interrupt() {
            if (pool != null) pool.shutdownNow();
        }

        public boolean await() throws InterruptedException {
            if (pool == null) return true;
            if (isDone()) return removeCompletedTasks();
            return pool.awaitTermination(timeout, timeoutUnit);
        }

        private void submitAndShutdown(Collection<SudokuNode> nodes, int numThreads) {
            if (pool != null) return;

            this.pool = new ThreadPoolExecutor(
                numThreads,
                numThreads,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
            );

            for (SudokuNode n : nodes) {
                tasks.add(pool.submit(() -> {
                    n.sudoku.searchForSolutions3(solution -> {
                        longCount.incrementAndGet();
                        return true;
                    });
                    return true;
                }));
            }

            pool.shutdown();
        }

        private boolean removeCompletedTasks() {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                Future<Boolean> task = tasks.get(i);
                if (task.isDone()) {
                    try {
						if (task.get()) {
						    tasks.remove(i);
						}
					} catch (InterruptedException | ExecutionException e) {
                        return false;
					}
                }
            }
            return true;
        }
    }

    static ThreadPoolExecutor pool = new ThreadPoolExecutor(
        1, 8,
        10L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>()
    );

    public void searchForSolutionsAsync(
        Consumer<Sudoku> solutionsCallbackAsync,
        int maxThreads
    ) {
        Sudoku root = new Sudoku(this);
        if (root.isSolved()) {
            solutionsCallbackAsync.accept(root);
            return;
        }
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();
        root.reduce();
        if (root.isSolved()) {
            solutionsCallbackAsync.accept(root);
            return;
        }

        // ThreadPoolExecutor pool = new ThreadPoolExecutor(
        //     1, maxThreads,
        //     10L, TimeUnit.MILLISECONDS,
        //     new LinkedBlockingQueue<>()
        // );

        AtomicInteger threadsActive = new AtomicInteger(1);
        pool.submit(() -> {
            searchForSolutionsAsyncWorker(new SudokuNode(root), solutionsCallbackAsync, threadsActive);
        });

        while (threadsActive.get() > 0) {
            try {
                // System.out.println("waiting on " + threadsActive.get() + " threads");
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(1L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void searchForSolutionsAsyncWorker(
        SudokuNode node,
        Consumer<Sudoku> solutionsCallbackAsync,
        AtomicInteger threadsActive
    ) {
        long startTime = System.currentTimeMillis();

        ArrayList<SudokuNode> stack = new ArrayList<>();
        stack.add(node);
        while (!stack.isEmpty()) {
            long curTime = System.currentTimeMillis();
            long timeSinceStart = curTime - startTime;
            if (!pool.isShutdown() && timeSinceStart > 25L && stack.size() > 3) {
                startTime = curTime;

                // Fast-forward in case all the nexts have been used on the lower end of the stack
                SudokuNode firstNode; do {
                    firstNode = stack.remove(0);
                } while(!firstNode.hasNext() && stack.size() > 3);

                if (firstNode.hasNext()) {
                    while (firstNode.hasNext()) {
                        SudokuNode next = firstNode.next();
                        threadsActive.incrementAndGet();
                        pool.submit(() -> {
                            searchForSolutionsAsyncWorker(next, solutionsCallbackAsync, threadsActive);
                        });
                    }
                }
                // At this point, there is at least 3 items left in the stack,
                // therefore safe to stack.peek() below.
            }

            SudokuNode top = stack.get(stack.size() - 1); // top = peek
            if (top.sudoku.isSolved()) {
                solutionsCallbackAsync.accept(top.sudoku);
            }

            // If necessary, rewind top until a node with nexts is found
            while (!stack.isEmpty() && !(top = stack.get(stack.size() - 1)).hasNext()) {
                stack.remove(stack.size() - 1); // pop
            }

            if (top.hasNext()) {
                stack.add(top.next());
            } // else stack is empty and the while loop will end
        }

        threadsActive.decrementAndGet();
    }



    /**
     * Counts the number of solutions to this sudoku with the given number of threads,
     * up to the given amount of time. Timeout will default to 1 hour if not positive.
     * @param numThreads
     * @param timeout
     * @param timeoutUnit
     * @return
     */
    public SolutionCountResult countSolutionsAsync(int numThreads, long timeout, TimeUnit timeoutUnit) {
        SolutionCountResult result = new SolutionCountResult(timeout, timeoutUnit);

        Sudoku root = new Sudoku(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();

        Queue<SudokuNode> q = new LinkedList<>();
        q.offer(new SudokuNode(root));

        int maxSplitSize = (1 << 10);
        while (!q.isEmpty() && q.size() < maxSplitSize) {
            SudokuNode top = q.poll();
            Sudoku sudoku = top.sudoku;

            if (sudoku.isSolved()) {
                result.longCount.incrementAndGet();
                continue;
            }

            SudokuNode next;
            while ((next = top.next()) != null) {
                q.offer(next);
            }
        }

        if (!q.isEmpty()) {
            result.submitAndShutdown(q, numThreads);
        }

        return result;
    }

    public void searchForSolutionsBranched(Function<Sudoku,Boolean> solutionFoundCallback, int numBranches) {
        numBranches = Math.max(1, numBranches);

        Sudoku root = new Sudoku(this);
        root.resetEmptyCells();
        // debug("          %s\n", root.toString());

        // ArrayList<Sudoku> results = new ArrayList<>();
        List<Stack<SudokuNode>> stacks = new ArrayList<Stack<SudokuNode>>(numBranches);
        List<Stack<SudokuNode>> emptyStackPool = new ArrayList<Stack<SudokuNode>>();
        for (int n = 1; n < numBranches; n++) {
            emptyStackPool.add(new Stack<SudokuNode>());
        }
        Stack<SudokuNode> initialStack = new Stack<>();
        initialStack.push(new SudokuNode(root));
        stacks.add(initialStack);

        boolean keepGoing = true;
        while (!stacks.isEmpty() && keepGoing) {
            for (int si = stacks.size() - 1; si >= 0; si--) {
                Stack<SudokuNode> stack = stacks.get(si);
                if (stack.isEmpty()) {
                    emptyStackPool.add(stacks.remove(si));
                    continue;
                }

                SudokuNode top = stack.peek();
                Sudoku sudoku = top.sudoku;
                // String pred = " ".repeat(stack.size());
                // debug("%s        > %s\n", pred, sudoku.toString());

                sudoku.reduce();

                if (sudoku.isSolved()) {
                    // results.add(new Sudoku(sudoku));
                    // debug("%s **⭐️** > %s\n", pred, sudoku.toString());
                    stack.pop();
                    if (solutionFoundCallback.apply(sudoku)) {
                        continue;
                    } else {
                        keepGoing = false;
                        break;
                    }
                }

                if (top.nexts == null) {
                    top.nexts = new ArrayList<>();
                    sudoku.getNextsAdditive(n -> {
                        top.nexts.add(new SudokuNode(n));
                        // debug("%s      + > %s\n", pred, n.toString());
                    });
                    Shuffler.shuffle(top.nexts);
                }

                if (top.nexts.size() > 0) {
                    stack.push(top.nexts.remove(top.nexts.size() - 1));

                    // Split out the next nodes into separate stacks if there are any empty / ready to go.
                    // Mitigate thrashing between stacks and emptyStackPool by only allowing mostly empty
                    //      puzzles to be split.
                    while (!emptyStackPool.isEmpty() && !top.nexts.isEmpty() && sudoku.numEmptyCells > (SPACES / 2)) {
                        Stack<SudokuNode> s = emptyStackPool.remove(emptyStackPool.size() - 1);
                        s.push(top.nexts.remove(top.nexts.size() - 1));
                        stacks.add(s);
                    }
                } else {
                    stack.pop();
                    // top.nexts = null;
                }
            }
        }

        // return results;
    }

    public Sudoku firstSolution() {
        AtomicReference<Sudoku> result = new AtomicReference<>();
        searchForSolutions3(solution -> {
            result.set(solution);
            return false;
        });
        return result.get();
    }

    public Sudoku normalize() {
        // TODO Board must have top row full

        for (int d = 1; d <= DIGITS; d++) {
			int cellDigit = digits[d - 1];
			if (cellDigit != d) {
				swapDigits(cellDigit, d);
			}
		}

        return this;
    }

    public boolean allAntiesSolve() {
        for (int ci = 0; ci < SPACES; ci++) {
            int originalVal = candidates[ci];
            if (originalVal == 0)
                return false;

            if (digits[ci] == 0) {
                for (int candidateDigit : CANDIDATES_ARR[originalVal]) {
                    setDigit(ci, candidateDigit); // mutates constraints
                    int flag = solutionsFlag();
                    setDigit(ci, 0); // undo the constraints mutation
                    candidates[ci] = originalVal;
                    if (flag != 1)
                        return false;
                }
            }
        }

        return true;
      }

    public void swapDigits(int a, int b) {
		if (a == b) {
            return;
        }

        for (int ci = 0; ci < SPACES; ci++) {
            int d = digits[ci];
            if (d == a) {
                setDigit(ci, b);
            } else if (d == b) {
                setDigit(ci, a);
            }
        }
	}

    /**
     * Generates a copy of this sudoku with the specified digits removed.
     * @param digitsMask A 9-bit mask representing the combination of which digits to remove, where
     * the least significant bit represents the digit '1'.
     */
    public void removeDigits(int digitsMask) {
        for (int ci = 0; ci < SPACES; ci++) {
            if ((candidates[ci] & digitsMask) > 0) {
                setDigit(ci, 0);
            }
        }
    }

    public SudokuMask getMask() {
        if (isFull())
            return SudokuMask.full();
        if (isEmpty())
            return new SudokuMask();

        SudokuMask result = new SudokuMask();
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] > 0) {
                result.setBit(ci);
            }
        }
        return result;
    }

    public SudokuMask maskForDigits(int digitsMask) {
        SudokuMask result = new SudokuMask();
        for (int ci = 0; ci < SPACES; ci++) {
            if ((candidates[ci] & digitsMask) > 0) {
                result = result.setBit(ci);
            }
        }
        return result;
    }

    public String fingerprint(int level) {
        if (level < 2 || level > 8) {
            throw new IllegalArgumentException("sudoku fingerprint level (f) must be 2 <= f <= 8");
        }

        if (!SudokuUtility.isSolved(getBoard())) {
            throw new IllegalArgumentException("cannot compute fingerprint: sudoku grid must be full");
        }

        SudokuSieve sieve = new SudokuSieve(getBoard());
        sieve.seed(level);

        // Track the maximum number of cells used by any unavoidable set
        // int minNumCells = SPACES;
        int maxNumCells = 0;
        int[] itemCountByNumCells = new int[SPACES];
        for (SudokuMask ua : sieve.items(new ArrayList<>())) {
            int numCells = ua.bitCount();
            itemCountByNumCells[numCells]++;
            // if (numCells < minNumCells) minNumCells = numCells;
            if (numCells > maxNumCells) maxNumCells = numCells;
        }

        ArrayList<String> itemsList = new ArrayList<>();
        // An item (unavoidable set) includes a minimum of 4 cells
        for (int numCells = 4; numCells <= maxNumCells; numCells++) {
            // In level 2, there can be no UAs using an odd number of cells,
            // because each cell must have at least one complement.
            // Skipping odd numbers avoids "::", keeping the fingerprint short.
            if (level == 2 && (numCells % 2) > 0) {
                continue;
            }

            int count = itemCountByNumCells[numCells];
            itemsList.add((count > 0) ? Integer.toString(count, 16) : "");
        }

        return String.join(":", itemsList);
    }

    public void shuffleDigits() {
        // TODO prereq: puzzle must contain every distinct digit
        Shuffler.shuffle(_digitsArr);

        for (int i = 0; i < DIGITS; i++) {
            int a = i + 1;
            int b = _digitsArr[i];
            swapDigits(a, b);
        }
    }

    public void rotate(int n) {
        // TODO
        // Rotate (clockwise) n times
        // n = n % 4; and ensure positive
    }

    public void reflect(int orientation) {
        // TODO
        // preset orientations: HORIZONTAL, VERTICAL, DIAGONAL, ANTIDIAGONAL
    }

    public Sudoku swapBandRows(int bi, int ri1, int ri2) {
        if (ri1 == ri2) return this;
        if (bi < 0 || bi > 2 || ri1 < 0 || ri2 < 0 || ri1 > 2 || ri2 > 2) {
            throw new IllegalArgumentException("swapBandRows error, specified band or row(s) out of bounds");
        }

        int[] board = getBoard();

        for (int i = 0; i < DIGITS; i++) {
            setDigit(BAND_ROW_INDICES[bi][ri1][i], 0);
            setDigit(BAND_ROW_INDICES[bi][ri2][i], 0);
            Shuffler.swap(board, BAND_ROW_INDICES[bi][ri1][i], BAND_ROW_INDICES[bi][ri2][i]);
        }

        for (int i = 0; i < DIGITS; i++) {
            setDigit(BAND_ROW_INDICES[bi][ri1][i], board[BAND_ROW_INDICES[bi][ri1][i]]);
            setDigit(BAND_ROW_INDICES[bi][ri2][i], board[BAND_ROW_INDICES[bi][ri2][i]]);
        }

        return this;
    }

    public Sudoku swapStackCols(int si, int ci1, int ci2) {
        if (ci1 == ci2) return this;
        if (si < 0 || ci1 < 0 || ci2 < 0 || si > 2 || ci1 > 2 || ci2 > 2) {
            throw new IllegalArgumentException("swapStackCols error, specified stack or col(s) out of bounds");
        }

        int[] board = getBoard();

        for (int i = 0; i < DIGITS; i++) {
            setDigit(STACK_COL_INDICES[si][ci1][i], 0);
            setDigit(STACK_COL_INDICES[si][ci2][i], 0);
            Shuffler.swap(board,STACK_COL_INDICES[si][ci1][i], STACK_COL_INDICES[si][ci2][i]);
        }

        for (int i = 0; i < DIGITS; i++) {
            setDigit(STACK_COL_INDICES[si][ci1][i], board[STACK_COL_INDICES[si][ci1][i]]);
            setDigit(STACK_COL_INDICES[si][ci2][i], board[STACK_COL_INDICES[si][ci2][i]]);
        }

        return this;
    }

    public Sudoku swapBands(int b1, int b2) {
        if (b1 == b2) return this;
        if (b1 < 0 || b2 < 0 || b1 > 2 || b2 > 2) {
            throw new IllegalArgumentException("swapBands error, specified band(s) out of bounds");
        }

        int[] board = getBoard();

        for (int i = 0; i < 27; i++) {
            setDigit(BAND_INDICES[b1][i], 0);
            setDigit(BAND_INDICES[b2][i], 0);
            Shuffler.swap(board, BAND_INDICES[b1][i], BAND_INDICES[b2][i]);
        }

        for (int i = 0; i < 27; i++) {
            setDigit(BAND_INDICES[b1][i], board[BAND_INDICES[b1][i]]);
            setDigit(BAND_INDICES[b2][i], board[BAND_INDICES[b2][i]]);
        }

        return this;
    }

    public Sudoku swapStacks(int s1, int s2) {
        if (s1 == s2) return this;
        if (s1 < 0 || s2 < 0 || s1 > 2 || s2 > 2) {
            throw new IllegalArgumentException("swapStacks error, specified stack(s) out of bounds");
        }

        int[] board = getBoard();

        for (int i = 0; i < 27; i++) {
            setDigit(STACK_INDICES[s1][i], 0);
            setDigit(STACK_INDICES[s2][i], 0);
            Shuffler.swap(board, STACK_INDICES[s1][i], STACK_INDICES[s2][i]);
        }

        for (int i = 0; i < 27; i++) {
            setDigit(STACK_INDICES[s1][i], board[STACK_INDICES[s1][i]]);
            setDigit(STACK_INDICES[s2][i], board[STACK_INDICES[s2][i]]);
        }

        return this;
    }

    public Sudoku filter(BigInteger mask) {
        // Throw if this is not full grid
        Sudoku result = new Sudoku();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(SPACES - 1 - ci)) {
                result.setDigit(ci, digits[ci]);
            }
        }
        return result;
    }

    public Sudoku filter(SudokuMask mask) {
        // Throw if this is not full grid
        Sudoku result = new Sudoku();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(ci)) {
                result.setDigit(ci, digits[ci]);
            }
        }
        return result;
    }

    public String filterStr(SudokuMask mask) {
        StringBuilder strb = new StringBuilder();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(ci)) {
                strb.append(digits[ci]);
            } else {
                strb.append('.');
            }
        }
        return strb.toString();
    }

    // Returns a mask representing the difference in cells between this Sudoku and the one given.
    public SudokuMask diff2(Sudoku other) {
        SudokuMask result = new SudokuMask();
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] != other.digits[ci]) {
                result.setBit(ci);
            }
        }
        return result;
    }

    public SudokuMask diff2(int[] otherBoard) {
        SudokuMask result = new SudokuMask();
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] != otherBoard[ci]) {
               result.setBit(ci);
            }
        }
        return result;
    }

    // TODO This can be kept in sync as board changes
    public int digitsUsed() {
        int result = 0;
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] > 0) {
                result |= encode(digits[ci]);
            }
        }
        return Integer.bitCount(result);
    }

    public int solutionsFlag() {
        if (!isValid) {
            return 0;
        }

        if (numEmptyCells > SPACES - MIN_CLUES) {
            return 2;
        }

        AtomicInteger count = new AtomicInteger();
        searchForSolutions3(_s -> (count.incrementAndGet() < 2));
        return count.get();
    }

    void getNextsAdditive(Consumer<Sudoku> callback) {
        int emptyCell = pickEmptyCell();
        if (emptyCell < 0) return;
        for (int candidateDigit : CANDIDATES_ARR[candidates[emptyCell]]) {
            Sudoku next = new Sudoku(this);
            next.setDigit(emptyCell, candidateDigit);
            callback.accept(next);
        }
    }

    public List<Sudoku> antiDerivatives() {
        ArrayList<Sudoku> result = new ArrayList<>();
        Sudoku p = new Sudoku(this);
        p.resetEmptyCells();
        p.reduce();
        for (int ci = 0; ci < SPACES; ci++) {
            if (p.digits[ci] == 0) {
                for (int candidateDigit : CANDIDATES_ARR[p.candidates[ci]]) {
                    Sudoku next = new Sudoku(p);
                    next.setDigit(ci, candidateDigit);
                    result.add(next);
                }
            }
        }
        return result;
    }

    int pickEmptyCell() {
        return pickEmptyCell(0, SPACES);
    }

    /**
     * Finds and returns the index of an empty cell, or -1 if no empty cells exist.
     * Prioritizes empty cells with the fewest number of candidates. If multiple cells
     * have the fewest number of candidates, chooses one of them at random.
     */
    // Hoisting this list up here actually runs slightly slower...
    // private List<Integer> _minimums = new ArrayList<>();
    public int pickEmptyCell(int startIndex, int endIndex) {
        if (numEmptyCells == 0) {
            return  -1;
        }

        int min = DIGITS + 1;
        List<Integer> _minimums = new ArrayList<>();
        for (int ci = startIndex; ci < endIndex; ci++) {
            if (digits[ci] == 0) {
                int numCandidates = BIT_COUNT_MAP[candidates[ci]];
                // This actually runs slightly slower...
                // if (numCandidates == 2) {
                //     return ci;
                // }
                if (numCandidates < min) {
                    min = numCandidates;
                    _minimums.clear();
                    _minimums.add(ci);
                } else if (numCandidates == min) {
                    _minimums.add(ci);
                }
            }
        }

        return (!_minimums.isEmpty()) ? _minimums.get(ThreadLocalRandom.current().nextInt(_minimums.size())) : -1;
        // return _minimums.get(RandomGenerator.getDefault().nextInt(_minimums.size()));
    }

    @Override
    public String toString() {
        // return new String(str);

        StringBuilder strb = new StringBuilder();
        for (int d : this.digits) {
            strb.append((d > 0) ? Integer.toString(d) : ".");
        }
        return strb.toString();
    }

    public String toFullString() {
        StringBuilder strb = new StringBuilder("  ");
        for (int i = 0; i < SPACES; i++) {
            if (this.digits[i] > 0) {
                strb.append(this.digits[i]);
            } else {
                strb.append('.');
            }

            // Print pipe between region columns
            if ((((i+1)%3) == 0) && (((i+1)%9) != 0)) {
                strb.append(" | ");
            } else {
                strb.append("   ");
            }

            if (((i+1)%9) == 0) {
                strb.append(System.lineSeparator());

                if (i < 80) {
                    // Border between region rows
                    if (((((i+1)/9)%3) == 0) && (((i/9)%8) != 0)) {
                        strb.append(" -----------+-----------+------------");
                    } else {
                        strb.append("            |           |            ");
                    }
                    strb.append(System.lineSeparator());
                    strb.append("  ");
                }
            }
        }

        return strb.toString();
    }

    public static int[] bigToCells(BigInteger big) {
        // big = big.abs(); // Is this necessary?
        int length = big.bitCount();
        int[] result = new int[length];
        int i = 0;

        for (int ci = SPACES - 1; ci >= 0; ci--) {
            if (big.testBit(SPACES - 1 - ci)) {
                result[i++] = ci;
            }
        }

        // int shift = 0;
        // while (!big.equals(BigInteger.ZERO)) {
        //     if (big.testBit(0)) {
        //         result[i] = SPACES - 1 - shift;
        //         i++;
        //     }
        //     shift++;
        //     big = big.shiftRight(1);
        // }
        return result;
    }

    static int numPasses = 0;
    public static Sudoku testRandomPuzzleGen(int numClues) {
        debug("> testRandomPuzzleGen(numClues = %d)\n", numClues);
        debug("> testRandomPuzzleGen: Generating config...%s ", "");
        Sudoku config = configSeed().firstSolution();
        debug(config.toString());
        int level = (numClues >= 24) ? 2 : 3;
        debug("> testRandomPuzzleGen: Generating sieve (level %d)... ", level);
        SudokuSieve sieve = new SudokuSieve(config.getBoard());
        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            SudokuMask pMask = config.maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }
        debug("Done. Populated sieve with %d items.\n", sieve.size());
        debug(sieve.toString());
        debug("\n> testRandomPuzzleGen: Searching...");

        boolean puzzleMaskFound = false;
        HashSet<SudokuMask> seen = new HashSet<>();
        SudokuMask mask = null;
        numPasses = 0;
        // int foundCount = 0;
        do {
            while (seen.contains(mask = SudokuMask.random(numClues)));
            seen.add(mask);
            numPasses++;
            debug("> testRandomPuzzleGen: Checking %s ...\n", mask.toString());
            if (!sieve.doesMaskSatisfy(mask)) {
                debug("> testRandomPuzzleGen: ... rejected by sieve.");
            } else {
                debug("> testRandomPuzzleGen: ... PASSED sieve check.");
                // BigInteger maskInverted = mask.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
                Sudoku p = config.filter(mask);
                int flag = p.solutionsFlag();
                if (puzzleMaskFound = (flag == 1)) {
                    debug("> testRandomPuzzleGen: ... PASSED solutions check.");
                    // foundCount++;
                    // System.out.printf("[%8d] %s\n", numPasses, p.toString());
                    // numPasses = 0;
                } else {
                    debug("> testRandomPuzzleGen: ... rejected by solutions check.");
                    sieve.addFromFilter(mask);
                }
            }
        } while (!puzzleMaskFound);

        debug("> testRandomPuzzleGen: Done in %d passes.", numPasses);

        return config.filter(mask);
    }

    /**
     * ********************************************
     *     genPuzz2 (experimental sieve search)
     * ********************************************
     */

    // static Sudoku config = new Sudoku("237841569186795243594326718315674892469582137728139456642918375853467921971253684");
    // static int[] configDigits;
    // static SudokuSieve sieve;
    // static BigInteger[] sieveItems;

    static class PuzzleMask {
        Sudoku config;
        int[] configDigits = new int[SPACES];
        SudokuMask[] sieveItems;

        SudokuMask puzzleMask = new SudokuMask();
        SudokuMask digitsMask = new SudokuMask();
        int[] digitsUsed = new int[DIGITS + 1];

        PuzzleMask(SudokuSieve sieve) {
            this.config = sieve.config();
            this.configDigits = this.config.getBoard();
            this.sieveItems = sieve.items(new SudokuMask[sieve.size()]);
        }

        PuzzleMask(PuzzleMask other) {
            this.config = new Sudoku(other.config);
            System.arraycopy(other.configDigits, 0, this.configDigits, 0, SPACES);
            this.sieveItems = new SudokuMask[other.sieveItems.length];
            System.arraycopy(other.sieveItems, 0, this.sieveItems, 0, other.sieveItems.length);
            this.puzzleMask = new SudokuMask(other.digitsMask.toString());
            this.digitsMask = new SudokuMask(other.digitsMask.toString());
            System.arraycopy(other.digitsUsed, 0, this.digitsUsed, 0, DIGITS + 1);
        }

        void chooseCell(int cellIndex) {
            // Is the cell already chosen? (error)
            // if (puzzleMask.testBit(SPACES - 1 - cellIndex)) {
            //     throw new RuntimeException("Sieve search tried to duplicate cell choice.");
            // }
            puzzleMask = puzzleMask.setBit(cellIndex);
            digitsMask = digitsMask.setBit(configDigits[cellIndex]);
            digitsUsed[configDigits[cellIndex]]++;
        }

        void removeCell(int cellIndex) {
            puzzleMask = puzzleMask.unsetBit(cellIndex);
            int digit = configDigits[cellIndex];
            digitsUsed[digit] = Math.max(0, digitsUsed[digit] - 1);
            if (digitsUsed[digit] == 0) {
                digitsMask = digitsMask.unsetBit(digit);
            }
        }

        int digitsUsed() {
            return digitsMask.bitCount();
        }

        int numClues() {
            return puzzleMask.bitCount();
        }

        boolean satisfiesSieve() {
            for (SudokuMask b : sieveItems) {
                if (b.overlapsAllOf(puzzleMask)) {
                    return false;
                }
            }
            return true;
        }

        boolean isValidSudoku() {
            return config.filter(puzzleMask).solutionsFlag() == 1;
        }

        List<Integer> cellsChosen(List<Integer> list) {
            for (int i = 0; i < SPACES; i++) {
                if (puzzleMask.testBit(i)) {
                    list.add(i);
                }
            }
            return list;
        }

        @Override
        public String toString() {
            return config.filter(puzzleMask).toString();
        }
    }

    // static PuzzleMask mask;

    static class Node {
        int[] choices;
        int index;
        int cellIndex;
        int digit;
        List<SudokuMask> overlapping;
        // int digitConstraints;

        Node(SudokuMask sieveItem) {
            this.choices = new int[sieveItem.bitCount()];
            this.index = sieveItem.bitCount() - 1;

            if (this.index < 0) {
                System.out.printf("sieveItem: %s (bits=%d)\n", sieveItem.toString(), sieveItem.bitCount());
                System.out.println("index: " + this.index);

                // TODO ! FIX BEFORE USING CLASS AGAIN
                // System.out.println(sieve.toString());
            }

            int j = 0;
            for (int ci = 0; ci < SPACES; ci++) {
                if (sieveItem.testBit(ci)) {
                    this.choices[j++] = ci;
                }
            }
            Shuffler.shuffle(this.choices);

            this.cellIndex = this.choices[this.index];
            // TODO ! FIX BEFORE USING CLASS AGAIN
            // this.digit = configDigits[this.cellIndex];
            // mask.chooseCell(this.cellIndex);
            // this.overlapping = sieve.removeOverlapping(this.cellIndex, new ArrayList<>());
        }

        void putStuffBack() {
            // TODO ! FIX BEFORE USING CLASS AGAIN
            // int sieveSizeBefore = sieve.size();
            // int overlappingSizeBefore = overlapping.size();
            // overlapping.forEach(item -> sieve.add(item));
            overlapping.clear();
            // if (sieve.size() != sieveSizeBefore + overlappingSizeBefore) {
            //     throw new RuntimeException("Something went wrong adding sieve items back.");
            // }
            // mask.removeCell(cellIndex);
        }

        boolean sub() {
            // System.out.println("> Node: sub()");
            putStuffBack();

            if (index <= 0) {
                return false;
            }

            index--;

            cellIndex = choices[index];
            // TODO ! FIX BEFORE USING CLASS AGAIN
            // digit = configDigits[cellIndex];
            // sieve.removeOverlapping(cellIndex, overlapping);
            // mask.chooseCell(this.cellIndex);
            return true;
        }

        String jsonStr() {
            StringBuilder overlappingStr = new StringBuilder("[\n");
            // for (BigInteger item : overlapping) {
            //     // TODO ! FIX BEFORE USING CLASS AGAIN
            //     // overlappingStr.append(String.format("  %s\n", config.filter(item).toString()));
            // }
            overlappingStr.append("]");

            return String.format(
                "{\n  choices: %s,\n  index: %d,\n  cellIndex: %d,\n  digit: %d,\n  overlapping: %s\n}",
                Arrays.toString(choices),
                index, cellIndex, digit,
                overlappingStr
            );
        }
    }

    // priority DFS - where priority value is some aggregate of satisfied/unsatisfied constraints
    public static Sudoku sieveSearch(Sudoku grid) {
        debug("> sieveSearch(grid=%s)\n", grid.toString());
        // debug("> sieveSearch: Generating config...%s ", "");

        // config = configSeed().firstSolution();
        // config = grid;
        // configDigits = config.getBoard();
        // debug(grid.toString());

        int level = 3; // (numClues > 24) ? 2 : 3;
        debug("> sieveSearch: Generating sieve (level %d)...\n", level);
        SudokuSieve sieve = new SudokuSieve(grid.getBoard());
        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            SudokuMask pMask = grid.maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }
        // The main sieve will be manipulated by the DFS, so cache the items - they'll be needed later.
        // sieve.items(sieveItems = new BigInteger[sieve.size()]);

        debug("Done. Populated sieve with %d items.\n", sieve.size());
        debug(sieve.toString());
        debug("\n> sieveSearch: Searching...");
        PuzzleMask mask = new PuzzleMask(sieve);

        // Seen sets, indexed by #clues
        List<HashSet<SudokuMask>> seen = new ArrayList<>();
        for (int i = 0; i <= SPACES; i++) {
            seen.add(new HashSet<>());
        }

        Stack<Node> stack = new Stack<>();
        Node rootNode = new Node(sieve.first());
        stack.push(rootNode);

        while (!stack.isEmpty()) {
            // int choiceCount = stack.size();
            // Node top = stack.peek();

            // System.out.println("            " + " ".repeat(mask.numClues()) + mask.toString());

            // If puzzleMask bits == target, run the checks and callback if puzzle found, then pop (or swap with alt) and continue
            // { checks: (1) digitsMask bits >= 8, (2) puzzleMask satisfies sieve, (3) puzzle is valid }
            if (
                // choiceCount == numClues ||
                sieve.isEmpty()
            ) {
                // boolean _seen = false, _satisfies = false, _valid = false;
                if (
                    !seen.get(mask.numClues()).contains(mask.puzzleMask) // &&
                    // mask.digitsUsed() >= 8
                    // (_satisfies = mask.satisfiesSieve()) &&
                    // (_valid = mask.isValidSudoku())
                ) {
                    // System.out.printf("@@@ > sieveSearch: ⭐️ [%24s] %s\n", mask.puzzleMask.toString(), mask.toString());
                    System.out.printf(
                        "[%2d] %s %s\n",
                        mask.numClues(),
                        (mask.numClues() >= MIN_CLUES && mask.isValidSudoku()) ? "* " : "  ",
                        mask.toString()
                    );

                    // TODO Search DFS search to remove cells while sieve is satisfied
                    searchDown(mask, seen);
                } else {
                    // System.out.printf(
                    //     "> sieveSearch: ❌ [%20s] _seen=%b; _satisfies=%b; _valid=%b; %s\n",
                    //     mask.puzzleMask.toString(),
                    //     _seen, _satisfies, _valid,
                    //     mask.toString()
                    // );
                }

                seen.get(mask.numClues()).add(mask.puzzleMask);

                while (!stack.peek().sub()) {
                    stack.pop();
                }

                continue;
            }

            // TODO Unused. Not sure if it will have an effect or just add overhead
            // Bits less than target
            // Can short-circuit early and pop if there's not enough choices left to satisfy digits constraint
            // (DIGITS - #digits used - 1) > (numClues - puzzleMask bits)
            // if ((DIGITS - mask.digitsUsed() - 1) > (numClues - choiceCount)) {
            //     int lastCellChoice = top.cellIndex;
            //     int lastDigitChoice = top.digit;
            //     System.out.println("> sieveSearch: Not enough choices left to satisfy digits constraint");
            //     top.sub();
            //     stack.pop();
            //     continue;
            // }

            // also if there's not enough choices left to satisfy disjoint UAs left in the sieve, but this may take compute time.
            // i.e., need to check how many non-overlapping sieve items are remaining
            // #non-overlapping sieve items > (numClues - choiceCount)

            // Otherwise... make another cell choice
            if (!sieve.isEmpty()) {
                stack.push(new Node(sieve.first()));
            } else {
                System.out.println("Could not push to stack from empty sieve");
            }
        }

        return null;
    }

    static void searchDown(PuzzleMask mask, List<HashSet<SudokuMask>> seen) {
        List<Integer> _list = mask.cellsChosen(new ArrayList<>());
        // if (mask.numClues() < MIN_CLUES) {
        //     return;
        // }

        // For each cell that's been chosen, see if removing it still satisfies the sieve
        // mask.cellsChosen(_list);
        for (int cellIndex : _list) {
            mask.removeCell(cellIndex);
            if (
                !seen.get(mask.numClues()).contains(mask.puzzleMask) &&
                mask.satisfiesSieve() // &&
                // mask.digitsUsed() >= 8 &&
                // (_valid = mask.isValidSudoku())
            ) {
                System.out.printf(
                    "[%2d] %s %s\n",
                    mask.numClues(),
                    (mask.numClues() >= MIN_CLUES && mask.isValidSudoku()) ? "* " : "  ",
                    mask.toString()
                );
                seen.get(mask.numClues()).add(mask.puzzleMask);
                searchDown(mask, seen);
            }
            mask.chooseCell(cellIndex);
        }
    }

    public static void completeSearch(Sudoku grid, int numClues) {
        debug("> completeSearch(grid = %s, numClues = %d)\n", grid.toString(), numClues);

        if (numClues < MIN_CLUES) {
            throw new IllegalArgumentException("cannot generate puzzle with less than " + MIN_CLUES + " clues");
        } else if (numClues >= SPACES) {
            throw new IllegalArgumentException("cannot generate puzzle with " + SPACES + " or greater clues");
        }

        int[] board = grid.getBoard();
        int sievePopLevel = 3;
        debug("> completeSearch: Generating sieve (level %d)...\n", sievePopLevel);
        SudokuSieve sieve = new SudokuSieve(board);
        for (int r = DIGIT_COMBOS_MAP[sievePopLevel].length - 1; r >= 0; r--) {
            SudokuMask pMask = grid.maskForDigits(DIGIT_COMBOS_MAP[sievePopLevel][r]);
            sieve.addFromFilter(pMask);
        }
        debug("> completeSearch: Done. Sieve size: %d\n", sieve.size());
        debug(sieve.toString());

        debug("\n> completeSearch: Initiating search...");
        Sudoku puzzle = new Sudoku();
        PuzzleMask mask = new PuzzleMask(sieve);
        BiConsumer<Integer, Integer> updatePuzzleAndMask = (from, to) -> {
            mask.removeCell(from);
            puzzle.setDigit(from, 0);
            mask.chooseCell(to);
            puzzle.setDigit(to, board[to]);
        };
        ComboCounter counter = new ComboCounter(numClues, SPACES, updatePuzzleAndMask);
        long c = 0L;
        long testsPerDot = 10_000_000L;
        long dotPerLine = 100L;
        long startTime = System.currentTimeMillis();
        boolean estimated = false;

        while (!counter.atMax) {
            // debug(puzzle.toString());
            if ((c+1L) % testsPerDot == 0L) {
                System.out.print('.');
                if ((c+1L) % (testsPerDot * dotPerLine) == 0L) {
                    System.out.println();
                    if (!estimated) {
                        long now = System.currentTimeMillis();
                        long diffSecs = (now - startTime) / 1000L;

                        BigInteger secs = Counting.nChooseK(SPACES, numClues)
                            .divide(new BigInteger(Long.toString(testsPerDot * dotPerLine)))
                            .multiply(new BigInteger(Long.toString(diffSecs)));
                        BigInteger[] years = secs.divideAndRemainder(new BigInteger(Long.toString(365*24*60*60)));
                        BigInteger[] days = years[1].divideAndRemainder(new BigInteger(Long.toString(24*60*60)));
                        BigInteger hours = days[1].divide(new BigInteger(Long.toString(60*60)));

                        debug(
                            "Estimated: %s yrs, %s days, %s hrs\n",
                            years[0].toString(), days[0].toString(), hours.toString()
                        );
                        estimated = true;
                    }
                }
            }

            if (
                mask.digitsUsed() >= 8 &&
                mask.satisfiesSieve() &&
                mask.isValidSudoku()
            ) {
                debug("\n⭐️ %s\n", puzzle.toString());
            }

            counter.inc();
            c++;
        }

        debug("Done. Checked %d puzzles.\n", c);
    }

    static class ComboCounter {
        int n;
        int max;
        int[] vals;
        boolean atMax;
        BiConsumer<Integer, Integer> valSwitched;

        ComboCounter(int n, int max, BiConsumer<Integer, Integer> valSwitched) {
            this.n = n;
            this.max = max;
            this.vals = new int[n];
            this.valSwitched = valSwitched;
            reset();
        }

        void reset() {
            atMax = false;
            for (int i = n - 1; i >= 0; i--) {
                int from = vals[i];
                vals[i] = i;
                valSwitched.accept(from, i);
            }
        }

        int maxForIndex(int i) {
            return max - (n - i);
        }

        boolean canInc() {
            return atMax;
        }

        void inc() {
            if (atMax) {
                throw new RuntimeException("Could not increment - already at maximum");
            }

            int bi = n - 1;
            while (bi >= 0 && vals[bi] == maxForIndex(bi)) {
                bi--;
            }

            // At maximum when bi == 0
            if (bi == 0) {
                atMax = true;
            }

            int from = vals[bi];
            vals[bi]++;
            valSwitched.accept(from, vals[bi]);
            bi++;
            while (bi < n) {
                from = vals[bi];
                vals[bi] = vals[bi - 1] + 1;
                valSwitched.accept(from, vals[bi]);
                bi++;
            }
        }
    }

    public static Sudoku generatePuzzle(Sudoku grid, int numClues, int amount, int sieveLevel) {
        if (numClues < MIN_CLUES || numClues >= SPACES) {
            throw new IllegalArgumentException("Cannot generate puzzle with " + numClues + " clues.");
        }
        if (sieveLevel < 2 || sieveLevel > 4) {
            throw new IllegalArgumentException("Cannot generate sieve with level " + sieveLevel + ".");
        }

        debug(
            "> generatePuzzle(grid = %s, numClues = %d)\n",
            (grid == null) ? "null" : grid.toString(),
            numClues
        );

        if (grid == null) {
            debug("> generatePuzzle: Null grid received. Generating new grid...");
            grid = configSeed().firstSolution();
            debug("> generatePuzzle: Done.\n%s\n", grid.toString());
        }

        // int level = 4;
        debug("> generatePuzzle: Generating sieve (level %d)...", sieveLevel);
        SudokuSieve sieve = new SudokuSieve(grid);
        sieve.seed(sieveLevel);
        debug("Done.");
        debug(sieve.toString());
        debug("Populated sieve with %d items.\n", sieve.size());

        debug("> generatePuzzle: Initiating puzzle search...");

        if (numClues >= 28) {
            AtomicLong puzzlesCount = new AtomicLong();
            AtomicLong checked = new AtomicLong();
            AtomicLong satisfiesCount = new AtomicLong();
            while (true) {
                SudokuMask r = SudokuMask.random(numClues);
                checked.incrementAndGet();
                if (sieve.doesMaskSatisfy(r)) {
                    satisfiesCount.incrementAndGet();
                    Sudoku rPuzz = grid.filter(r);
                    int flag = rPuzz.solutionsFlag();
                    if (flag == 1) {
                        puzzlesCount.incrementAndGet();
                        // return rPuzz;
                        System.out.printf(
                            "%d {%d (%.2f%%) | %d (%.2f%%)} ⭐️ %s\n",
                            checked.get(),
                            satisfiesCount.get(), 100.0*satisfiesCount.doubleValue()/checked.doubleValue(),
                            puzzlesCount.get(), 100.0*puzzlesCount.doubleValue()/satisfiesCount.doubleValue(),
                            rPuzz.toString()
                        );
                    } else {
                        System.out.printf(
                            "❌ %d {%d (%.2f%%) | %d (%.2f%%)} %s\n",
                            checked.get(),
                            satisfiesCount.get(), 100.0*satisfiesCount.doubleValue()/checked.doubleValue(),
                            puzzlesCount.get(), 100.0*puzzlesCount.doubleValue()/satisfiesCount.doubleValue(),
                            rPuzz.toString()
                        );
                    }
                }
            }
        } else {
            // 1. Create jump off masks for the search by extrapolating sieve items
            // in a queue up to a predetermined size.
            ArrayList<SudokuMask> jumpOffs = new ArrayList<>();
            Queue<SudokuMask> q = new LinkedList<>();
            HashSet<SudokuMask> seen = new HashSet<>();
            final int MAX_QUEUE_SIZE = (1<<20);
            q.add(new SudokuMask());
            int[][] cellIndices = new int[SPACES + 1][];
            for (int i = 0; i < SPACES; i++) cellIndices[i] = new int[i];

            int lastBitCount = 0;
            while (!q.isEmpty() && (q.size() + jumpOffs.size()) < MAX_QUEUE_SIZE) {
                SudokuMask m = q.poll(); // get a (m)ask out of the (q)ueue
                int bitCount = m.bitCount();

                if (bitCount > lastBitCount) {
                    seen.clear();
                    debug("bitCount = %d; q.size() = %d; jumpOffs.size() = %d\n", bitCount, q.size(), jumpOffs.size());
                    lastBitCount = bitCount;
                }

                SudokuMask item = sieve.firstNonOverlapping(m);
                // If there's no such item, first is ZERO (the sieve is satisfied)
                if (item.bitCount() == 0) {
                    jumpOffs.add(m);
                    continue;
                }

                if (bitCount >= numClues)
                    continue;

                int[] cells = cellIndices[item.bitCount()];
                int j = 0;
                for (int ci = 0; ci < SPACES; ci++) {
                    if (item.testBit(ci)) {
                        cells[j++] = ci;
                        item = item.unsetBit(ci);
                    }
                    if (item.bitCount() == 0)
                        break;
                }

                // TODO Instead of random shuffle, sort by reduction matrix
                for (int nextCellIndex : Shuffler.shuffle(cells)) {
                    SudokuMask cellMask = new SudokuMask().setBit(nextCellIndex);
                    if (cellMask.overlapsAllOf(m)) {
                        // BigInteger next = m.or(BigInteger.ONE.shiftLeft(SPACES - 1 - nextCellIndex));
                        SudokuMask next = new SudokuMask(m.toString()).setBit(nextCellIndex);
                        if (!seen.contains(next)) {
                            seen.add(next);
                            q.offer(next);
                            // debug("%d | %s\n", bitCount+1, grid.filter(next).toString());
                        }
                    }
                }

                // debug("q.size() = %d; jumpOffs.size() = %d\n", q.size(), jumpOffs.size());
            }

            debug("Shuffling and re-sorting by bitCount...");
            jumpOffs.addAll(q);
            Shuffler.shuffle(jumpOffs);
            jumpOffs.sort((a, b) -> b.bitCount() - a.bitCount());

            // Return some memory
            debug("Reclaiming mem");
            q.clear();
            seen.clear();

            debug("Searching from jumpoff points...");
            AtomicLong puzzlesCount = new AtomicLong();
            AtomicLong checked = new AtomicLong();
            AtomicLong satisfiesCount = new AtomicLong();
            HashSet<SudokuMask> satisfiesCache = new HashSet<>();
            Random rand = new Random();
            // ! jumpOffs is sorted descending
            // while (!jumpOffs.isEmpty()) {
            for (int i = jumpOffs.size() - 1; i >= 0; i--) {
                // BigInteger n = jumpOffs.remove(jumpOffs.size() - 1);
                SudokuMask n = jumpOffs.get(i);
                // debug(
                //     "%d [%d] {%d (%.2f%%) | %d (%.2f%%)} %s\n",
                //     checked.get(),
                //     i,
                //     satisfiesCount.get(), 100.0*satisfiesCount.doubleValue()/checked.doubleValue(),
                //     puzzlesCount.get(), 100.0*puzzlesCount.doubleValue()/satisfiesCount.doubleValue(),
                //     SudokuSieve.maskString(n)
                // );

                // EXPERIMENT 1 - RANDOMLY FILLING IN REMAINING CELLS
                for (int trial = 1000; trial > 0; trial--) {
                    checked.incrementAndGet();
                    SudokuMask _n = new SudokuMask(n.toString());
                    while (_n.bitCount() < numClues) {
                        int randBit = rand.nextInt(SPACES);
                        if (!_n.testBit(randBit)) {
                            _n = _n.setBit(randBit);
                        }
                    }

                    // Caching everything clobbers mem
                    // if (seen.contains(_n)) {
                    //     saved.incrementAndGet();
                    //     continue;
                    // }
                    // seen.add(_n);

                    if (satisfiesCache.contains(_n))
                        continue;

                    boolean satisfies = sieve.doesMaskSatisfy(_n);
                    if (satisfies) {
                        satisfiesCache.add(_n);
                        satisfiesCount.incrementAndGet();
                        Sudoku nPuzz = grid.filter(_n);
                        // debug(nPuzz.toString());
                        int flag = nPuzz.solutionsFlag();
                        // System.out.printf("%s | sieve check: ✅; real sudoku: %s\n", nPuzz.toString(), (flag == 1) ? "⭐️" : "❌");
                        if (flag == 1) {
                            puzzlesCount.incrementAndGet();
                            System.out.printf(
                                "%d [%d] {%d (%.2f%%) | %d (%.2f%%)} ⭐️ %s\n",
                                checked.get(),
                                i,
                                satisfiesCount.get(), 100.0*satisfiesCount.doubleValue()/checked.doubleValue(),
                                puzzlesCount.get(), 100.0*puzzlesCount.doubleValue()/satisfiesCount.doubleValue(),
                                nPuzz.toString()
                            );
                            // return nPuzz;
                        } else {
                            // System.out.printf(
                            //     "%d [%d] {%d (%.2f%%) | %d (%.2f%%)} ✅ %s\n",
                            //     checked.get(),
                            //     i,
                            //     satisfiesCount.get(), 100.0*satisfiesCount.doubleValue()/checked.doubleValue(),
                            //     puzzlesCount.get(), 100.0*puzzlesCount.doubleValue()/satisfiesCount.doubleValue(),
                            //     nPuzz.toString()
                            // );
                        }
                    } else {
                        // System.out.printf("%s | sieve check: ❌\n", nPuzz.toString());
                    }
                }

            }

            // TODO EXPERIMENT 2 - FULL DFS FROM JUMP-OFF POINTS
            // for (int i = jumpOffs.size() - 1; i >= 0; i--) {
            //     BigInteger m = jumpOffs.get(i);

            //     Stack<BigInteger> stack = new Stack<>();
            //     BigInteger item = sieve.firstNonOverlapping(m);

            // }
        }

        return null;
    }

    public static List<BigInteger> bigShatter(BigInteger big, List<BigInteger> list) {
        for (int j = 0; j < SPACES; j++) {
            if (big.testBit(j)) {
                list.add(BigInteger.ONE.shiftLeft(j));
            }
        }
        return list;
    }
}
