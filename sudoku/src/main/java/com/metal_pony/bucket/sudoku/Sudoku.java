package com.metal_pony.bucket.sudoku;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.metal_pony.bucket.sudoku.game.SudokuUtility;
import com.metal_pony.bucket.util.Array;
import com.metal_pony.bucket.util.Counting;
import com.metal_pony.bucket.util.Shuffler;

public class Sudoku {
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

    // public static void main(String[] args) {
    //     // Sudoku seed = Sudoku.configSeed();
    //     Sudoku seed = new Sudoku("1.......945...71..9.7..23...3.2.9....9....57..8.......3.......1..26.5....79......");
    //     debug(seed.toString());
    //     debug(seed.toFullString());

    //     ArrayList<Sudoku> solutions = new ArrayList<>();
    //     HashSet<String> solutionSet = new HashSet<>();
    //     seed.searchForSolutionsBranched((solution) -> {
    //         solutions.add(solution);
    //         String str = solution.toString();
    //         if (solutionSet.add(str)) {
    //             debug(str);
    //         } else {
    //             debug("🚨 DUPLICATE SOLUTION: %s\n", str);
    //         }

    //         return true;
    //     }, 100);

    //     debug("Hello Sudoku");
    //     debug("Found %d solutions!\n", solutionSet.size());
    // }

    public static final int DIGITS_SQRT = 3;
    public static final int DIGITS = 9;
    public static final int SPACES = 81;
    public static final int ALL = 511;
    public static final int MIN_CLUES = 17;

    static final int ROW_MASK = ALL << (DIGITS * 2);
    static final int COL_MASK = ALL << DIGITS;
    static final int REGION_MASK = ALL;

    public static int[] getEmptyBoard() {
        return new int[SPACES];
    }

    static final int[] ENCODER = new int[] { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256 };
    static final int[] DECODER = new int[1<<DIGITS];
    static {
        for (int digit = 1; digit <= DIGITS; digit++) {
            DECODER[1 << (digit - 1)] = digit;
        }
    }

    /**
     * Maps candidates mask to the array of digits it represents.
     */
    static final int[][] CANDIDATES_ARR = new int[1<<DIGITS][];

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
                    CANDIDATES[val][j++] = encode(digit);
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

    public static int encode(int digit) {
        return ENCODER[digit];
    }

    public static int decode(int encoded) {
        return DECODER[encoded];
    }

    public static boolean isDigit(int encoded) {
        return DECODER[encoded] > 0;
    }

    public static class BitSet {
        static final int[] BITS_SET_IN_BYTE = new int[1 << Byte.SIZE];
        static {
            for (int i = 0; i < BITS_SET_IN_BYTE.length; i++) {
                BITS_SET_IN_BYTE[i] = Integer.bitCount(i);
            }
        }

        int bits;
        int numBytes;
        byte[] mag;
        int bitsSet;

        public BitSet(int bits) {
            this.bits = bits;
            this.numBytes = bits / Byte.SIZE;
            if ((bits % Byte.SIZE) > 0) {
                this.numBytes++;
            }
            this.mag = new byte[this.numBytes];
            this.bitsSet = 0;
        }

        public BitSet(int bits, byte[] values) {
            this.bits = bits;
            this.numBytes = bits / Byte.SIZE;
            this.mag = new byte[this.numBytes];
            if ((bits % Byte.SIZE) > 0) {
                this.numBytes++;
            }
            System.arraycopy(values, 0, this.mag, 0, this.numBytes);
            this.bitsSet = 0;
            for (byte b : this.mag) {
                this.bitsSet += BITS_SET_IN_BYTE[b];
            }
        }

        public BitSet(BitSet other) {
            this(other.bits, other.mag);
        }

        public boolean isSet(int i) {
            int magIndex = i / Byte.SIZE;
            return (this.mag[magIndex] & (1 << (i % Byte.SIZE))) == 1;
        }

        public int bits() {
            return bits;
        }

        public int bitsSet() {
            return bitsSet;
        }

        public void set(int i) {
            int magIndex = i / Byte.SIZE;
            int mask = 1 << (i % Byte.SIZE);

            if ((mag[magIndex] & mask) == 0) {
                mag[magIndex] |= mask;
                bitsSet++;
            }
        }

        public void unset(int i) {
            int magIndex = i / Byte.SIZE;
            int mask = 1 << (i % Byte.SIZE);

            if ((mag[magIndex] & mask) == 1) {
                mag[magIndex] &= ~mask;
                bitsSet--;
            }
        }

        public void flip(int i) {
            int magIndex = i / Byte.SIZE;
            int mask = 1 << (i % Byte.SIZE);

            if ((mag[magIndex] & mask) == 0) {
                mag[magIndex] |= mask;
                bitsSet++;
            } else {
                mag[magIndex] &= ~mask;
                bitsSet--;
            }
        }

        public BitSet negate() {
            // for (int i = 0; i < numBytes; i++) {
            //     mag[i] = (byte)((~mag[i]) & 0xff);
            // }
            // // the last byte may need to be truncated if there are extra bits
            // if ((bits % Byte.SIZE) > 0) {

            // }
            for (int i = 0; i < bits; i++) {
                flip(i);
            }
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (this == obj) return true;
            if (obj instanceof BitSet) {
                BitSet other = (BitSet) obj;
                return (
                    bits == other.bits &&
                    numBytes == other.numBytes &&
                    bitsSet == other.bitsSet &&
                    Arrays.equals(mag, other.mag)
                );
            }
            return false;
        }

        // TODO This hashcode doesn't necessarily uphold the equals contract
        @Override
        public int hashCode() {
            return Arrays.hashCode(mag);
        }

        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder();
            for (byte b : mag) {
                String bs = Integer.toString(Byte.toUnsignedInt(b), 2);
                if (bs.length() < Byte.SIZE) {
                    strb.append("0".repeat(Byte.SIZE - bs.length()));
                }
                strb.append(bs);
            }
            return strb.toString();
        }


        public String toSudokuString() {
            StringBuilder strb = new StringBuilder();
            for (byte b : mag) {
                String bs = Integer.toString(Byte.toUnsignedInt(b), 2);
                // if (bs.length() < Byte.SIZE) {
                //     strb.append("0".repeat(Byte.SIZE - bs.length()));
                // }
                String segment = String.format(
                    "%s%s",
                    "0".repeat(Byte.SIZE - bs.length()),
                    Integer.toString(Byte.toUnsignedInt(b), 2)
                );

                strb.append(Array.reverse(segment));
            }
            return strb.substring(0, bits).replaceAll("0", ".");
        }

        public BigInteger toBigInt() {
            return new BigInteger(toString(), 2);
        }
    }

    private class Area {
        final int index;
        int constraints = 0;
        // int[] candidateCount;
        Cell[] cells = new Cell[DIGITS];

        Area(int index) {
            this.index = index;
        }
    }

    private class Cell {
        final int index;
        final int rowIndex;
        final int colIndex;
        final int regionIndex;

        // TODO Not yet used
        boolean isClue = false;

        int digit = 0;
        int candidates = 0;
        Area row;
        Area col;
        Area region;
        Cell[] neighbors = new Cell[20];
        Cell[] rowNeighbors;
        Cell[] colNeighbors;
        Cell[] regionNeighbors;

        Cell(int index) {
            this.index = index;
            this.rowIndex = index / DIGITS;
            this.colIndex = index % DIGITS;
            this.regionIndex = (index / 27) * 3 + ((index % 9) / 3);
        }

        int constraints() {
            return row.constraints | col.constraints | region.constraints;
        }

        int getUniqueCandidate() {
            int[] candidatesArr = CANDIDATES[this.candidates];

            for (int candidateIndex = candidatesArr.length - 1; candidateIndex >= 0; candidateIndex--) {
                int candidate = candidatesArr[candidateIndex];

                boolean unique = true;
                for (int i = rowNeighbors.length - 1; i >= 0; i--) {
                    if ((rowNeighbors[i].candidates & candidate) > 0) {
                        unique = false;
                        break;
                    }
                }
                if (unique) {
                    return candidate;
                }

                unique = true;
                for (int i = colNeighbors.length - 1; i >= 0; i--) {
                    if ((colNeighbors[i].candidates & candidate) > 0) {
                        unique = false;
                        break;
                    }
                }
                if (unique) {
                    return candidate;
                }

                unique = true;
                for (int i = regionNeighbors.length - 1; i >= 0; i--) {
                    if ((regionNeighbors[i].candidates & candidate) > 0) {
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

        boolean reduce() {
            int candidatesBefore = candidates;

            if (digit > 0 || candidates == 0) {
                return false;
            }

            // ? If candidate constraints reduces to 0, then the board is likely invalid.
            int reducedCandidates = (candidates & ~constraints());
            if (reducedCandidates <= 0) {
                setDigit(0);
                return false;
            }

            // If by applying the constraints, the number of candidates is reduced to 1,
            // then the cell is solved.
            if (isDigit(reducedCandidates)) {
                setDigit(decode(reducedCandidates));
            } else {
                int uniqueCandidate = getUniqueCandidate();
                if (uniqueCandidate > 0) {
                    setDigit(decode(uniqueCandidate));
                    reducedCandidates = uniqueCandidate;
                } else {
                    candidates = reducedCandidates;
                }
            }

            if (reducedCandidates < candidates) {
                reduceNeighbors();
            }

            // Return whether candidates have changed.
            return candidatesBefore != candidates;
        }

        void reduceNeighbors() {
            for (Cell n : neighbors) {
                n.reduce();
            }
        }

        void setDigit(int newDigit) {
            int prevVal = candidates;
            int newVal = encode(newDigit);
            boolean wasDigit = this.digit > 0;
            this.digit = newDigit;
            this.candidates = newVal;
            boolean _isDigit = this.digit > 0;

            if (wasDigit && !_isDigit) {
                numEmptyCells++;
                removeConstraint(decode(prevVal));
            } else if (!wasDigit && _isDigit) {
                numEmptyCells--;
                addConstraint(newDigit);
            } else if (wasDigit && _isDigit) {
                // If both the previous and new values are digits, then the
                // constraint for the previous value is removed and the
                // constraint for the new value is added.
                removeConstraint(decode(prevVal));
                addConstraint(newDigit);
            }
        }

        private void addConstraint(int digit) {
            row.constraints |= (1 << (digit - 1));
            col.constraints |= (1 << (digit - 1));
            region.constraints |= (1 << (digit - 1));
        }

        private void removeConstraint(int digit) {
            row.constraints &= ~(1 << (digit - 1));
            col.constraints &= ~(1 << (digit - 1));
            region.constraints &= ~(1 << (digit - 1));
        }
    }

    Cell[] cells;
    Area[] rows;
    Area[] cols;
    Area[] regions;
    int numEmptyCells = SPACES;

    public Sudoku() {
        this.cells = new Cell[SPACES];
        for (int ci = 0; ci < SPACES; ci++) {
            this.cells[ci] = new Cell(ci);
        }

        this.rows = new Area[DIGITS];
        this.cols = new Area[DIGITS];
        this.regions = new Area[DIGITS];
        for (int i = 0; i < DIGITS; i++) {
            Area row = new Area(i);
            this.rows[i] = row;
            Area col = new Area(i);
            this.cols[i] = col;
            Area region = new Area(i);
            this.regions[i] = region;

            // Connect areas to cells
            int rowI = 0;
            int colI = 0;
            int regionI = 0;
            for (int ci = 0; ci < SPACES; ci++) {
                Cell c = this.cells[ci];
                if (c.rowIndex == i) {
                    row.cells[rowI++] = c;
                }
                if (c.colIndex == i) {
                    col.cells[colI++] = c;
                }
                if (c.regionIndex == i) {
                    region.cells[regionI++] = c;
                }
            }
        }

        // Connect cells to areas
        for (int ci = 0; ci < SPACES; ci++) {
            final int _ci = ci;
            Cell c = this.cells[ci];
            c.row = this.rows[c.rowIndex];
            c.col = this.cols[c.colIndex];
            c.region = this.regions[c.regionIndex];

            // Connect cells to neighbors
            c.rowNeighbors = Arrays.stream(c.row.cells).filter(_c -> (_c.index != _ci)).toArray(Cell[]::new);
            c.colNeighbors = Arrays.stream(c.col.cells).filter(_c -> (_c.index != _ci)).toArray(Cell[]::new);
            c.regionNeighbors = Arrays.stream(c.region.cells).filter(_c -> (_c.index != _ci)).toArray(Cell[]::new);
            c.neighbors = Stream.of(this.cells).filter(other -> (
                other.index != _ci && (
                    other.rowIndex == c.rowIndex ||
                    other.colIndex == c.colIndex ||
                    other.regionIndex == c.regionIndex
                )
            )).distinct().toArray(Cell[]::new);
        }
    }

    public Sudoku(Sudoku other) {
        this();

        this.numEmptyCells = other.numEmptyCells;

        // Copy cell data
        for (int ci = 0; ci < SPACES; ci++) {
            Cell cell = this.cells[ci];
            Cell otherCell = other.cells[ci];
            // if (otherCell.digit > 0) {
            //     this.setDigit(ci, otherCell.digit);
            //     if (otherCell.isClue) {
            //         this.cells[ci].isClue = true;
            //     }
            // }

            cell.isClue = otherCell.isClue;
            cell.digit = otherCell.digit;
            cell.candidates = otherCell.candidates;
        }

        // Copy Area data
        for (int i = 0; i < DIGITS; i++) {
            this.rows[i].constraints = other.rows[i].constraints;
            this.cols[i].constraints = other.cols[i].constraints;
            this.regions[i].constraints = other.regions[i].constraints;
        }
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

    public Sudoku(int[] board) {
        this();

        if (board.length != SPACES) {
            throw new IllegalArgumentException("board array is invalid length");
        }

        for (int i = 0; i < SPACES; i++) {
            int digit = board[i];
            if (digit > 0) {
                setDigit(i, digit);
            }
        }
    }

    public void setDigit(int cellIndex, int digit) {
        this.cells[cellIndex].setDigit(digit);
    }

    private int[] _digitsArr = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    public void fillRegion(int regionIndex) {
        Shuffler.shuffle(_digitsArr);
        Cell[] regionCells = this.regions[regionIndex].cells;
        for (int i = 0; i < DIGITS; i++) {
            this.setDigit(regionCells[i].index, _digitsArr[i]);
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
    public static Sudoku configSeed() {
        Sudoku seed = new Sudoku();
        int mask = regionsMasks[ThreadLocalRandom.current().nextInt(regionsMasks.length)];
        seed.fillRegions(mask);
        return seed;
    }

    public int[] getBoard() {
        int[] board = new int[SPACES];
        for (int i = 0; i < SPACES; i++) {
            board[i] = this.cells[i].digit;
        }
        return board;
    }

    public int[] getBoard2() {
        int[] board = new int[SPACES];
        for (int i = 0; i < SPACES; i++) {
            board[i] = this.cells[i].candidates;
        }
        return board;
    }

    public void resetEmptyCells() {
        for (Cell cell : this.cells) {
            if (cell.digit == 0) {
                cell.candidates = ALL;
            }
        }
    }

    public boolean isFull() {
        return this.numEmptyCells == 0;
    }

    public boolean isSolved() {
        boolean result = true;
        for (int i = 0; i < DIGITS; i++) {
            if (
                this.rows[i].constraints != ALL ||
                this.cols[i].constraints != ALL ||
                this.regions[i].constraints != ALL
            ) {
                result = false;
                break;
            }
        }
        return result;
    }

    public boolean reduce() {
        boolean boardSolutionChanged = false;
        boolean hadReduction = false;

        do {
            hadReduction = false;
            for (int i = 0; i < SPACES; i++) {
                hadReduction = hadReduction || this.cells[i].reduce();
                if (hadReduction) {
                    // console.log(`reduced> ${boardSolution.board.map(decode).join('').replace(/0/g, '.')}`);
                }
                boardSolutionChanged = boardSolutionChanged || hadReduction;
            }
        } while (hadReduction);

        return boardSolutionChanged;
    }

    public void searchForSolutions3() {
        searchForSolutions3(_s->true);
    }

    private static class SudokuNode {
        Sudoku sudoku;
        List<SudokuNode> nexts = null;
        SudokuNode(Sudoku sudoku) {
            this.sudoku = sudoku;
        }
    }

    public void searchForSolutions3(Function<Sudoku,Boolean> solutionFoundCallback) {
        Sudoku root = new Sudoku(this);
        root.resetEmptyCells();
        // debug("          %s\n", root.toString());

        // ArrayList<Sudoku> results = new ArrayList<>();
        Stack<SudokuNode> stack = new Stack<>();
        stack.push(new SudokuNode(root));

        boolean keepGoing = true;
        while (!stack.isEmpty() && keepGoing) {
            SudokuNode top = stack.peek();
            Sudoku sudoku = top.sudoku;
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
            } else {
                stack.pop();
                // top.nexts = null;
                sudoku.cells = null;
                sudoku.rows = null;
                sudoku.cols = null;
                sudoku.regions = null;
            }
        }

        // return results;
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
                    sudoku.cells = null;
                    sudoku.rows = null;
                    sudoku.cols = null;
                    sudoku.regions = null;
                }
            }
        }

        // return results;
    }

    public Sudoku normalize() {
        // TODO Error if top row isn't full

        for (int boardIndex = 1; boardIndex <= DIGITS; boardIndex++) {
			int digit = this.cells[boardIndex - 1].digit;
			if (digit != boardIndex) {
				swapDigits(digit, boardIndex);
			}
		}

        return this;
    }

    public void swapDigits(int a, int b) {
		if (a == b) {
            return;
        }

        for (Cell cell : cells) {
            if (cell.digit == a) {
                cell.setDigit(b);
            } else if (cell.digit == b) {
                cell.setDigit(a);
            }
        }
	}

    /**
     * Generates a copy of this sudoku with the specified digits removed.
     * @param digitsMask A 9-bit mask representing the combination of which digits to remove, where
     * the least significant bit represents the digit '1'.
     */
    public void removeDigits(int digitsMask) {
        for (Cell cell : cells) {
            if (((1 << (cell.digit - 1)) & digitsMask) > 0) {
                cell.setDigit(0);
            }
        }
    }

    public BigInteger maskForDigits(int digitsMask) {
        BigInteger result = BigInteger.ZERO;
        for (Cell cell : cells) {
            if (((1 << (cell.digit - 1)) & digitsMask) > 0) {
                result = result.setBit(SPACES - 1 - cell.index);
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

        // HashSet<BigInteger> uas = new HashSet<>();
        SudokuSieve sieve = new SudokuSieve(getBoard());

        for (int r = DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            // Sudoku copy = new Sudoku(this);
            // copy.removeDigits(DIGIT_COMBOS_MAP[level][r]);
            // BigInteger puzzleMask = diff2(copy);
            BigInteger pMask = maskForDigits(DIGIT_COMBOS_MAP[level][r]);
            // System.out.printf("Searching for solutions:\n%s\n\n", filter(pMask).toString());
            int numAdded = sieve.addFromFilter(pMask);

            // int sizeBefore = uas.size();
            // copy.searchForSolutions3((solution) -> {
            //     BigInteger diff = diff2(solution);
            //     if (diff.compareTo(BigInteger.ZERO) != 0) {
            //         if (uas.add(diff)) {
            //             System.out.printf("solution  ");
            //             System.out.println(solution.toString());
            //             System.out.printf(
            //                 "%s%s\n",
            //                 "0".repeat(SPACES - diff.toString(2).length()),
            //                 diff.toString(2)
            //             );
            //             // diff = diff.setBit(SPACES);
            //             // diff = diff.not().abs();
            //             // System.out.printf(
            //             //     "%s%s\n",
            //             //     " ".repeat(Math.max(0, SPACES - diff.toString(2).length())),
            //             //     diff.toString(2)
            //             // );
            //             diff = diff.xor(BigInteger.ONE.shiftLeft(SPACES).subtract(BigInteger.ONE));
            //             // diff = diff.not(SPACES);
            //             System.out.printf(
            //                 "%s%s\n",
            //                 " ".repeat(Math.max(0, SPACES - diff.toString(2).length())),
            //                 diff.toString(2)
            //             );
            //             Sudoku filtered = filter(diff);
            //             System.out.printf("%s\n", filtered.toString());
            //             System.out.println(filtered.toFullString());
            //             System.out.println();
            //         }
            //     }
            //     return true;
            // });

            // System.out.printf("-- %d solutions found for --\n%s\n\n", numAdded, filter(pMask).toString());
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
        // TODO
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

    public Sudoku filter(BitSet mask) {
        // Throw if this is not full grid
        Sudoku copy = new Sudoku();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.isSet(ci)) {
                copy.setDigit(ci, this.cells[ci].digit);
            }
        }
        return copy;
    }

    public Sudoku filter(BigInteger mask) {
        // Throw if this is not full grid
        Sudoku copy = new Sudoku();
        for (int ci = 0; ci < SPACES; ci++) {
            if (mask.testBit(SPACES - 1 - ci)) {
                copy.setDigit(ci, this.cells[ci].digit);
            }
        }
        return copy;
    }

    // Returns a mask representing the difference in cells between this Sudoku and the one given.
    public BitSet diff(Sudoku other) {
        BitSet result = new BitSet(SPACES);
        // byte[] result = new byte[(Sudoku.SPACES / Byte.SIZE) + 1];
        for (int ci = 0; ci < SPACES; ci++) {
            if (this.cells[ci].digit != other.cells[ci].digit) {
                result.set(ci);
                // result[ci / Byte.SIZE] |= 1 << (ci % Byte.SIZE);
            }
        }
        return result;
    }

    public BigInteger diff2(Sudoku other) {
        // BitSet result = new BitSet(SPACES);
        BigInteger result = BigInteger.ZERO;
        // byte[] result = new byte[(Sudoku.SPACES / Byte.SIZE) + 1];
        for (int ci = 0; ci < SPACES; ci++) {
            if (this.cells[ci].digit != other.cells[ci].digit) {
                // result = result.setBit(ci);
                result = result.setBit(SPACES - 1 - ci);
                // result[ci / Byte.SIZE] |= 1 << (ci % Byte.SIZE);
            }
        }
        return result;
    }

    public int solutionsFlag() {
        if (numEmptyCells > SPACES - MIN_CLUES) {
            return 2;
        }

        AtomicInteger count = new AtomicInteger();
        // TODO Do this with single thread, multiple DFS search branches
        searchForSolutions3(_s -> (count.incrementAndGet() < 2));
        return count.get();
    }

    void getNextsAdditive(Consumer<Sudoku> callback) {
        Cell emptyCell = this.pickEmptyCell();
        if (emptyCell != null) {
            for (int candidateDigit : CANDIDATES_ARR[emptyCell.candidates]) {
                Sudoku next = new Sudoku(this);
                next.setDigit(emptyCell.index, candidateDigit);
                callback.accept(next);
            }
        }
    }

    public List<Sudoku> antiDerivatives() {
        ArrayList<Sudoku> result = new ArrayList<>();
        Sudoku p = new Sudoku(this);
        p.resetEmptyCells();
        p.reduce();
        for (Cell cell : p.cells) {
            if (cell.digit == 0) {
                for (int candidateDigit : CANDIDATES_ARR[cell.candidates]) {
                    Sudoku next = new Sudoku(p);
                    next.setDigit(cell.index, candidateDigit);
                    result.add(next);
                }
            }
        }
        return result;
    }

    Cell pickEmptyCell() {
        int min = DIGITS + 1;
        List<Cell> minimums = new ArrayList<>();
        for (int ci = 0; ci < SPACES; ci++) {
            Cell cell = this.cells[ci];
            int numCandidates = BIT_COUNT_MAP[cell.candidates];
            if (numCandidates > 1 && numCandidates < min) {
                min = numCandidates;
                minimums.clear();
                minimums.add(cell);
            } else if (numCandidates == min) {
                minimums.add(cell);
            }
        }

        // If min still === 10, then there are no empty cells.
        return (min == (DIGITS + 1)) ? null : minimums.get(ThreadLocalRandom.current().nextInt(minimums.size()));
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        for (Cell c : this.cells) {
            strb.append((c.digit > 0) ? Integer.toString(c.digit) : ".");
        }
        return strb.toString();
    }

    public String toFullString() {
        StringBuilder strb = new StringBuilder("  ");
        for (int i = 0; i < SPACES; i++) {
            if (this.cells[i].digit > 0) {
                strb.append(this.cells[i].digit);
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
}
