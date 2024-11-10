package com.metal_pony.bucket.sudoku;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.metal_pony.bucket.sudoku.game.SudokuUtility;
import com.metal_pony.bucket.util.Counting;
import com.metal_pony.bucket.util.Shuffler;

public class Sudoku2 {
    public static void main2(String[] args) {
        // for (int numClues = 45; numClues >= 20; numClues--) {
        //     for (int i = 0; i < 100; i++) {
        //         String puzzleStr = Sudoku2.testRandomPuzzleGen(numClues).toString();
        //         System.out.printf("[%d] [%8d] %s\n", numClues, numPasses, puzzleStr);
        //     }
        // }

        // genPuzz2(17);

        // Sudoku2 seed = configSeed();
        // System.out.println(seed.toString());
        // seed.searchForSolutions3(solution -> {
        //     System.out.println(solution.toString());
        //     return true;
        // });

        // Sudoku2 p = new Sudoku2("123456789478932615659817243......................................................");
        // p.searchForSolutions3(s->{
        //     System.out.println(s.toString());
        //     return true;
        // });

        // ArrayList<BigInteger> allBands = new ArrayList<>();
        // HashSet<String> fullBandSet = new HashSet<>();

        // String band = "123456789478932615659817243";
        // String pStr = band + "0".repeat(SPACES - band.length());
        // System.out.println(band);

        // System.out.println("Root:");
        // System.out.println(new Sudoku(pStr).toFullString());

        // List<String> transforms = Main.getBandPermutations(band);
        // System.out.println(transforms.size());
        // transforms.forEach(t -> {
        //     System.out.println(new Sudoku(t + "0".repeat(SPACES - t.length())).toFullString() + "\n");
        // });

        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStacks(1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStacks(1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStacks(0, 1)", "-".repeat(38), new Sudoku2(pStr).swapStacks(0, 1).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStacks(0, 1).swapStacks(1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStacks(0, 1).swapStacks(1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStacks(0, 2).swapStacks(1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStacks(0, 2).swapStacks(1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStacks(0, 2)", "-".repeat(38), new Sudoku2(pStr).swapStacks(0, 2).toFullString());

        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapBandRows(0, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapBandRows(0, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapBandRows(0, 0, 1)", "-".repeat(38), new Sudoku2(pStr).swapBandRows(0, 0, 1).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapBandRows(0, 0, 1).swapBandRows(0, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapBandRows(0, 0, 1).swapBandRows(0, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapBandRows(0, 0, 2).swapBandRows(0, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapBandRows(0, 0, 2).swapBandRows(0, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapBandRows(0, 0, 2)", "-".repeat(38), new Sudoku2(pStr).swapBandRows(0, 0, 2).toFullString());

        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(0, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(0, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(0, 0, 1)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(0, 0, 1).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(0, 0, 1).swapStackCols(0, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(0, 0, 1).swapStackCols(0, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(0, 0, 2).swapStackCols(0, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(0, 0, 2).swapStackCols(0, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(0, 0, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(0, 0, 2).toFullString());

        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(1, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(1, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(1, 0, 1)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(1, 0, 1).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(1, 0, 1).swapStackCols(1, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(1, 0, 1).swapStackCols(1, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(1, 0, 2).swapStackCols(1, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(1, 0, 2).swapStackCols(1, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(1, 0, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(1, 0, 2).toFullString());

        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(2, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(2, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(2, 0, 1)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(2, 0, 1).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(2, 0, 1).swapStackCols(2, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(2, 0, 1).swapStackCols(2, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(2, 0, 2).swapStackCols(2, 1, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(2, 0, 2).swapStackCols(2, 1, 2).toFullString());
        // System.out.printf("%s\n%s\n%s\n%s\n", "-".repeat(38), "swapStackCols(2, 0, 2)", "-".repeat(38), new Sudoku2(pStr).swapStackCols(2, 0, 2).toFullString());

        // Load from file
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

        // // // Run through reduction func
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

    static final int DIGITS_SQRT = 3;
    static final int DIGITS = 9;
    static final int SPACES = 81;
    static final int ALL = 511;
    static final int MIN_CLUES = 17;

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
    static final int[][] DIGIT_COMBOS_MAP = new int[DIGITS + 1][];
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

    static int[] CELL_ROWS = new int[SPACES];
    static int[] CELL_COLS = new int[SPACES];
    static int[] CELL_REGIONS = new int[SPACES];
    static int[][] ROW_INDICES = new int[DIGITS][DIGITS];
    static int[][] COL_INDICES = new int[DIGITS][DIGITS];
    static int[][] REGION_INDICES = new int[DIGITS][DIGITS];
    static int[][] BAND_INDICES = new int[3][3*DIGITS];
    static int[][] STACK_INDICES = new int[3][3*DIGITS];
    static int[][][] BAND_ROW_INDICES = new int[3][3][DIGITS];
    static int[][][] STACK_COL_INDICES = new int[3][3][DIGITS];
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
    static int[][] ROW_NEIGHBORS = new int[SPACES][DIGITS - 1];
    static int[][] COL_NEIGHBORS = new int[SPACES][DIGITS - 1];
    static int[][] REGION_NEIGHBORS = new int[SPACES][DIGITS - 1];
    static int[][] CELL_NEIGHBORS = new int[SPACES][3*(DIGITS-1) - (DIGITS-1)/2]; // Not checked if true for other ranks
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

    int[] digits;
    public int[] candidates;
    int[] constraints;
    int numEmptyCells = SPACES;

    public Sudoku2() {
        this.digits = new int[SPACES];
        this.candidates = new int[SPACES];
        this.constraints = new int[DIGITS];
    }

    public Sudoku2(Sudoku2 other) {
        this();
        this.numEmptyCells = other.numEmptyCells;
        System.arraycopy(other.digits, 0, this.digits, 0, SPACES);
        System.arraycopy(other.candidates, 0, this.candidates, 0, SPACES);
        System.arraycopy(other.constraints, 0, this.constraints, 0, DIGITS);
    }

    public Sudoku2(String str) {
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

    public Sudoku2(int[] digits) {
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
        );
    }

    private int[] _digitsArr = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    public void fillRegion(int regionIndex) {
        Shuffler.shuffle(_digitsArr);
        for (int i = 0; i < DIGITS; i++) {
            this.setDigit(REGION_INDICES[regionIndex][i], _digitsArr[i]);
        }
    }

    public void fillRegions(int regionsMask) {
        for (int regIndex = 0; regIndex < DIGITS; regIndex++) {
            if ((regionsMask & (1<<(DIGITS - 1 - regIndex))) > 0) {
                this.fillRegion(regIndex);
            }
        }
    }

    private static final int[] regionsMasks = new int[] {
        0b100010001, 0b100001010, 0b010100001,
        0b010001100, 0b001100010, 0b001010100
    };
    public static Sudoku2 configSeed() {
        Sudoku2 seed = new Sudoku2();
        int mask = regionsMasks[ThreadLocalRandom.current().nextInt(regionsMasks.length)];
        seed.fillRegions(mask);
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
        constraints = new int[DIGITS];
        for (int i = 0; i < SPACES; i++) {
            if (digits[i] > 0) {
                addConstraint(i, digits[i]);
            }
        }
    }

    public boolean isFull() {
        return this.numEmptyCells == 0;
    }

    public boolean isSolved() {
        for (int c : constraints) {
            if (c != FULL_CONSTRAINTS) {
                return false;
            }
        }
        return true;
    }

    public boolean reduce() {
        boolean boardSolutionChanged = false;
        boolean hadReduction = false;

        do {
            hadReduction = false;
            for (int i = 0; i < SPACES; i++) {
                hadReduction = hadReduction || reduceCell(i);
                if (hadReduction) {
                    // console.log(`reduced> ${boardSolution.board.map(decode).join('').replace(/0/g, '.')}`);
                }
                boardSolutionChanged = boardSolutionChanged || hadReduction;
            }
        } while (hadReduction);

        return boardSolutionChanged;
    }

    boolean reduceCell(int ci) {
        int candidatesBefore = candidates[ci];

        if (digits[ci] > 0 || candidates[ci] == 0) {
            return false;
        }

        // ? If candidate constraints reduces to 0, then the board is likely invalid.
        int reducedCandidates = (candidates[ci] & ~cellConstraints(ci));
        if (reducedCandidates <= 0) {
            setDigit(ci, 0);
            return false;
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
            reduceNeighbors(ci);
        }

        // Return whether candidates have changed.
        return candidatesBefore != candidates[ci];
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
            if (unique) {
                return candidate;
            }

            unique = true;
            for (int ni : COL_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                return candidate;
            }

            unique = true;
            for (int ni : REGION_NEIGHBORS[ci]) {
                if ((candidates[ni] & candidate) > 0) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                return candidate;
            }
        }

        return 0;
    }

    void reduceNeighbors(int ci) {
        for (int n : CELL_NEIGHBORS[ci]) {
            reduceCell(n);
        }
    }

    public void searchForSolutions3() {
        searchForSolutions3(_s->true);
    }

    static class SudokuNode {
        // int[] digits;
        // int[] candidates;
        // int[] constraints;
        Sudoku2 sudoku;
        int index = -1;
        int values = -1;
        ArrayList<SudokuNode> nexts;
        SudokuNode(Sudoku2 sudoku) {
            this.sudoku = sudoku;
            if (sudoku.reduce()) {
                // debug("reduced > %s\n", sudoku.toString());
            }
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
            if (values <= 0) {
                return null;
            }
            Sudoku2 s = new Sudoku2(sudoku);
            int[] candidateDigits = CANDIDATES_ARR[values];
            int d = candidateDigits[ThreadLocalRandom.current().nextInt(candidateDigits.length)];
            s.setDigit(index, d);
            values &= ~(ENCODER[d]);
            return new SudokuNode(s);
            // generateNextsIfNull();
            // return (nexts.isEmpty()) ? null : nexts.remove(nexts.size() - 1);
        }
    }

    public void searchForSolutions3(Function<Sudoku2,Boolean> solutionFoundCallback) {
        Sudoku2 root = new Sudoku2(this);
        // Ensure candidates and constraints are in good order for the search
        root.resetEmptyCells();
        root.resetConstraints();

        // debug("          %s\n", root.toString());
        // if (root.reduce()) {
        //     debug("reduced > %s\n", root.toString());
        // }

        // ArrayList<Sudoku> results = new ArrayList<>();
        Stack<SudokuNode> stack = new Stack<>();
        stack.push(new SudokuNode(root));

        boolean keepGoing = true;
        while (!stack.isEmpty() && keepGoing) {
            SudokuNode top = stack.peek();
            Sudoku2 sudoku = top.sudoku;
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
                    keepGoing = false;
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

    public void searchForSolutionsBranched(Function<Sudoku2,Boolean> solutionFoundCallback, int numBranches) {
        numBranches = Math.max(1, numBranches);

        Sudoku2 root = new Sudoku2(this);
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
                Sudoku2 sudoku = top.sudoku;
                // String pred = " ".repeat(stack.size());
                // debug("%s        > %s\n", pred, sudoku.toString());

                if (sudoku.reduce()) {
                    // debug("%sreduced > %s\n", pred, sudoku.toString());
                }

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
                    while (!emptyStackPool.isEmpty() && !top.nexts.isEmpty() && sudoku.numEmptyCells < (SPACES / 2)) {
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

    public Sudoku2 firstSolution() {
        AtomicReference<Sudoku2> result = new AtomicReference<>();
        configSeed().searchForSolutions3(solution -> {
            result.set(solution);
            return false;
        });
        return result.get();
    }

    public Sudoku2 normalize() {
        // TODO Board must contain at least one of each distinct digit, else error

        for (int d = 1; d <= DIGITS; d++) {
			int cellDigit = digits[d - 1];
			if (cellDigit != d) {
				swapDigits(cellDigit, d);
			}
		}

        return this;
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

    public BigInteger maskForDigits(int digitsMask) {
        BigInteger result = BigInteger.ZERO;
        for (int ci = 0; ci < SPACES; ci++) {
            if ((candidates[ci] & digitsMask) > 0) {
                result = result.setBit(SPACES - 1 - ci);
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

        // For each level-digits combo (9 choose level):
        //      copy the board with the digits removed,
        //      collect all the solutions
        SudokuSieve sieve = new SudokuSieve(getBoard());

        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            BigInteger pMask = maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }

        int _sum = 0;
        int minM = SPACES;
        int maxM = 0;
        int[] itemsByM = new int[SPACES];

        for (BigInteger ua : sieve.items(new ArrayList<>())) {
            int bits = ua.bitCount();
            itemsByM[bits]++;
            if (bits < minM) minM = bits;
            if (bits > maxM) maxM = bits;
            _sum += bits;
        }

        ArrayList<String> itemsList = new ArrayList<>();
        for (int m = 4; m <= maxM; m++) {
            if (level == 2 && (m % 2) > 0) {
                continue;
            }

            int n = itemsByM[m];
            itemsList.add((n > 0) ? Integer.toString(n, 16) : "");
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

    public Sudoku2 swapBandRows(int bi, int ri1, int ri2) {
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

    public Sudoku2 swapStackCols(int si, int ci1, int ci2) {
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

    public Sudoku2 swapBands(int b1, int b2) {
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

    public Sudoku2 swapStacks(int s1, int s2) {
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

    public Sudoku2 filter(BigInteger mask) {
        // Throw if this is not full grid
        Sudoku2 copy = new Sudoku2();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(SPACES - 1 - ci)) {
                copy.setDigit(ci, digits[ci]);
            }
        }
        return copy;
    }

    // Returns a mask representing the difference in cells between this Sudoku and the one given.
    public BigInteger diff2(Sudoku2 other) {
        BigInteger result = BigInteger.ZERO;
        for (int ci = 0; ci < SPACES; ci++) {
            if (digits[ci] != other.digits[ci]) {
                result = result.setBit(SPACES - 1 - ci);
            }
        }
        return result;
    }

    public int solutionsFlag() {
        if (numEmptyCells > SPACES - MIN_CLUES) {
            return 2;
        }

        AtomicInteger count = new AtomicInteger();
        // TODO Test the switch to searchForSolutionsBranched (9)
        searchForSolutionsBranched(_s -> {
            return count.incrementAndGet() < 2;
        }, 9);
        return count.get();
    }

    void getNextsAdditive(Consumer<Sudoku2> callback) {
        int emptyCell = pickEmptyCell();
        if (emptyCell != -1) {
            for (int candidateDigit : CANDIDATES_ARR[candidates[emptyCell]]) {
                Sudoku2 next = new Sudoku2(this);
                next.setDigit(emptyCell, candidateDigit);
                callback.accept(next);
            }
        }
    }

    public List<Sudoku2> antiDerivatives() {
        ArrayList<Sudoku2> result = new ArrayList<>();
        Sudoku2 p = new Sudoku2(this);
        p.resetEmptyCells();
        p.reduce();
        for (int ci = 0; ci < SPACES; ci++) {
            if (p.digits[ci] == 0) {
                for (int candidateDigit : CANDIDATES_ARR[p.candidates[ci]]) {
                    Sudoku2 next = new Sudoku2(p);
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
    public static Sudoku2 testRandomPuzzleGen(int numClues) {
        debug("> testRandomPuzzleGen(numClues = %d)\n", numClues);
        debug("> testRandomPuzzleGen: Generating config...%s ", "");
        Sudoku2 config = configSeed().firstSolution();
        debug(config.toString());
        int level = (numClues >= 24) ? 2 : 3;
        debug("> testRandomPuzzleGen: Generating sieve (level %d)... ", level);
        SudokuSieve sieve = new SudokuSieve(config.getBoard());
        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            BigInteger pMask = config.maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }
        debug("Done. Populated sieve with %d items.\n", sieve.size());
        debug("> testRandomPuzzleGen: Searching...");

        boolean puzzleMaskFound = false;
        HashSet<BigInteger> seen = new HashSet<>();
        BigInteger mask = null;
        numPasses = 0;
        // int foundCount = 0;
        do {
            while (seen.contains(mask = Counting.randomBitCombo(SPACES, numClues)));
            seen.add(mask);
            numPasses++;
            debug("> testRandomPuzzleGen: Checking %s ...\n", mask.toString(2));
            if (!sieve.doesMaskSatisfy(mask)) {
                debug("> testRandomPuzzleGen: ... rejected by sieve.");
            } else {
                debug("> testRandomPuzzleGen: ... PASSED sieve check.");
                // BigInteger maskInverted = mask.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
                Sudoku2 p = config.filter(mask);
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

    static Sudoku2 config = new Sudoku2("237841569186795243594326718315674892469582137728139456642918375853467921971253684");
    static int[] configDigits;
    static SudokuSieve sieve;
    static BigInteger[] sieveItems;

    static class PuzzleMask {
        BigInteger puzzleMask = BigInteger.ZERO;
        BigInteger digitsMask = BigInteger.ZERO;
        int[] digitsUsed = new int[DIGITS + 1];

        void chooseCell(int cellIndex) {
            // Is the cell already chosen? (error)
            if (puzzleMask.testBit(SPACES - 1 - cellIndex)) {
                throw new RuntimeException("Sieve search tried to duplicate cell choice.");
            }
            puzzleMask = puzzleMask.setBit(SPACES - 1 - cellIndex);
            digitsMask = digitsMask.setBit(configDigits[cellIndex]);
            digitsUsed[configDigits[cellIndex]]++;
        }

        void removeCell(int cellIndex) {
            puzzleMask = puzzleMask.clearBit(SPACES - 1 - cellIndex);
            int digit = configDigits[cellIndex];
            digitsUsed[digit]--;
            if (digitsUsed[digit] == 0) {
                digitsMask = digitsMask.clearBit(digit);
            }
        }

        int digitsUsed() {
            return digitsMask.bitCount();
        }

        int numClues() {
            return puzzleMask.bitCount();
        }

        boolean satisfiesSieve() {
            for (BigInteger b : sieveItems) {
                if (b.and(puzzleMask).equals(BigInteger.ZERO)) {
                    return false;
                }
            }
            return true;
        }
        boolean isValidSudoku() {
            return config.filter(puzzleMask).solutionsFlag() == 1;
        }
        @Override
        public String toString() {
            return config.filter(puzzleMask).toString();
        }
    }

    static PuzzleMask mask;

    static class Node {
        int[] choices;
        int index;
        int cellIndex;
        int digit;
        List<BigInteger> overlapping;
        // int digitConstraints;

        Node(BigInteger sieveItem) {
            this.choices = new int[sieveItem.bitCount()];
            this.index = sieveItem.bitCount() - 1;

            if (this.index < 0) {
                System.out.printf("sieveItem: [%s] %s (bits=%d)\n", sieveItem.toString(), sieveItem.toString(2), sieveItem.bitCount());
                System.out.println("index: " + this.index);
                System.out.println(sieve.toString());
            }

            int j = 0;
            for (int ci = 0; ci < SPACES; ci++) {
                if (sieveItem.testBit(SPACES - 1 - ci)) {
                    this.choices[j++] = ci;
                }
            }
            Shuffler.shuffle(this.choices);

            this.cellIndex = this.choices[this.index];
            this.digit = configDigits[this.cellIndex];
            mask.chooseCell(this.cellIndex);
            this.overlapping = sieve.removeOverlapping(this.cellIndex, new ArrayList<>());
        }

        void putStuffBack() {
            int sieveSizeBefore = sieve.size();
            int overlappingSizeBefore = overlapping.size();
            overlapping.forEach(item -> sieve.add(item));
            overlapping.clear();
            if (sieve.size() != sieveSizeBefore + overlappingSizeBefore) {
                throw new RuntimeException("Something went wrong adding sieve items back.");
            }
            mask.removeCell(cellIndex);
        }

        boolean sub() {
            // System.out.println("> Node: sub()");
            putStuffBack();

            if (index <= 0) {
                return false;
            }

            index--;

            cellIndex = choices[index];
            digit = configDigits[cellIndex];
            sieve.removeOverlapping(cellIndex, overlapping);
            mask.chooseCell(this.cellIndex);
            return true;
        }

        String jsonStr() {
            StringBuilder overlappingStr = new StringBuilder("[\n");
            for (BigInteger item : overlapping) {
                overlappingStr.append(String.format("  %s\n", config.filter(item).toString()));
            }
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
    public static Sudoku2 genPuzz2(Sudoku2 grid, int numClues) {
        debug("> genPuzz2(numClues = %d)\n", numClues);
        debug("> genPuzz2: Generating config...%s ", "");

        // static vars for debug
        // config = configSeed().firstSolution();
        config = grid;
        configDigits = config.getBoard();

        debug(config.toString());
        int level = (numClues > 24) ? 2 : 3;
        debug("> genPuzz2: Generating sieve (level %d)... ", level);

        // static var for debug
        sieve = new SudokuSieve(config.getBoard());

        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            BigInteger pMask = config.maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }
        // The main sieve will be manipulated by the DFS, so cache the items - they'll be needed later.
        sieve.items(sieveItems = new BigInteger[sieve.size()]);
        debug("Done. Populated sieve with %d items.\n", sieve.size());
        debug("> genPuzz2: Searching...");

        // static var for debug
        mask = new PuzzleMask();

        // Seen sets, indexed by number of clues (bits)
        List<HashSet<BigInteger>> seen = new ArrayList<>();
        for (int i = 0; i <= numClues; i++) {
            seen.add(new HashSet<>());
        }

        Stack<Node> stack = new Stack<>();
        Node rootNode = new Node(sieve.first());
        stack.push(rootNode);

        while (!stack.isEmpty()) {
            int choiceCount = stack.size();
            Node top = stack.peek();

            // System.out.println("            " + " ".repeat(mask.numClues()) + mask.toString());

            // If puzzleMask bits == target, run the checks and callback if puzzle found, then pop (or swap with alt) and continue
            // { checks: (1) digitsMask bits >= 8, (2) puzzleMask satisfies sieve, (3) puzzle is valid }
            if (choiceCount == numClues || sieve.isEmpty()) {
                boolean _seen = false, _satisfies = false, _valid = false;
                if (
                    !(_seen = seen.get(mask.numClues()).contains(mask.puzzleMask)) &&
                    (_satisfies = mask.satisfiesSieve()) &&
                    (_valid = mask.isValidSudoku())
                ) {
                    System.out.printf("@@@ > genPuzz2: ⭐️ [%24s] %s\n", mask.puzzleMask.toString(), mask.toString());
                } else {
                    // System.out.printf(
                    //     "> genPuzz2: ❌ [%20s] _seen=%b; _satisfies=%b; _valid=%b; %s\n",
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
            if ((DIGITS - mask.digitsUsed() - 1) > (numClues - choiceCount)) {
                int lastCellChoice = top.cellIndex;
                int lastDigitChoice = top.digit;
                System.out.println("> genPuzz2: Not enough choices left to satisfy digits constraint");
                top.sub();
                stack.pop();
                continue;
            }

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

    public static Sudoku2 generatePuzzle(int numClues) {
        if (numClues < MIN_CLUES) {
            throw new IllegalArgumentException("cannot generate puzzle with less than " + MIN_CLUES + " clues");
        } else if (numClues > SPACES) {
            throw new IllegalArgumentException("cannot generate puzzle with more than " + SPACES + " clues");
        } else if (numClues == SPACES) {
            return configSeed().firstSolution();
        }

        debug("> generatePuzzle(numClues = %d)", numClues);
        debug("> generatePuzzle: Generating config...");
        Sudoku2 config = configSeed().firstSolution();
        debug("> generatePuzzle: Done. config: %s", config.toString());
        int level = (numClues >= 24) ? 2 : 3;
        debug("> generatePuzzle: Generating sieve (level %d)...", level);
        SudokuSieve sieve = new SudokuSieve(config.getBoard());
        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            BigInteger pMask = config.maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }
        debug("> generatePuzzle: Done. Populated sieve with %d items.", sieve.size());
        debug("> generatePuzzle: Initiating puzzle search...");

        HashSet<BigInteger> seen = new HashSet<>();
        BigInteger mask = Counting.nChooseK(SPACES, numClues);




        return null;
    }
}
