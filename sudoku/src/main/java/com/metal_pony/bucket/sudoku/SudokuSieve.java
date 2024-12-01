package com.metal_pony.bucket.sudoku;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.metal_pony.bucket.util.Shuffler;

public class SudokuSieve {
    public static String maskString(BigInteger mask) {
        String bits = mask.toString(2);
        return "0".repeat(Sudoku.SPACES - bits.length()) + bits;
    }

    private static class ItemGroup {
        final int order;
        final TreeSet<BigInteger> items;
        ItemGroup(int order) {
            this.order = order;
            this.items = new TreeSet<>();
        }
    }

    // private final String _configStr;
    private final Sudoku _config;
    private int size;
    private final ArrayList<ItemGroup> _itemGroupsByBitCount;
    private HashSet<BigInteger> _cache;
    private HashMap<String,Integer> _validationCache;
    private int[] reductionMatrix;

    public SudokuSieve(Sudoku config) {
        if (!config.isSolved()) {
            throw new IllegalArgumentException("could not create sieve for malformed grid");
        }

        // this._configStr = config.toString();
        this._config = new Sudoku(config.getBoard());
        this._itemGroupsByBitCount = new ArrayList<>(Sudoku.SPACES + 1);
        for (int n = 0; n <= Sudoku.SPACES; n++) {
            this._itemGroupsByBitCount.add(n, new ItemGroup(n));
        }
        this._cache = new HashSet<>();
        this._validationCache = new HashMap<>();
        this.reductionMatrix = new int[Sudoku.SPACES];
    }

    public SudokuSieve(int[] configBoard) {
        this(new Sudoku(configBoard));
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Sudoku config() {
        return new Sudoku(_config);
    }

    public List<BigInteger> items() {
        return items(new ArrayList<>(size));
    }

    public List<BigInteger> items(List<BigInteger> list) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            list.addAll(group.items);
        }
        return list;
    }

    public BigInteger[] items(BigInteger[] arr) {
        int i = 0;
        for (ItemGroup group : _itemGroupsByBitCount) {
            for (BigInteger item : group.items) {
                arr[i++] = item;
            }
        }
        return arr;
    }

    /**
     * Maps sudoku cell indices to the number of times the cell appears among sieve items.
     */
    public int[] reductionMatrix() {
        return reductionMatrix(new int[Sudoku.SPACES]);
    }

    /**
     * Maps sudoku cell indices to the number of times the cell appears among sieve items.
     */
    public int[] reductionMatrix(int[] arr) {
        System.arraycopy(reductionMatrix, 0, arr, 0, Sudoku.SPACES);
        return arr;
    }

    private List<Integer> maximums = new ArrayList<>();
    private Random rand = new Random();
    public int firstImpact() {
        int max = 0;
        maximums.clear();
        for (int i = 0; i < reductionMatrix.length; i++) {
            int r = reductionMatrix[i];
            if (r > max) {
                max = r;
                maximums.clear();
                maximums.add(i);
            } else if (r == max) {
                maximums.add(i);
            }
        }
        return maximums.get(rand.nextInt(maximums.size()));
    }

    public BigInteger first() {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                return group.items.first();
            }
        }

        return BigInteger.ZERO;
    }

    ItemGroup groupForBitCount(int bitCount) {
        return _itemGroupsByBitCount.get(bitCount);
    }

    public List<BigInteger> getItemByNumClues(int numClues) {
        if (numClues < 0 || numClues > Sudoku.SPACES) {
            throw new IllegalArgumentException("Invalid number of clues");
        }
        return new ArrayList<>(groupForBitCount(numClues).items);
    }

    boolean validate(BigInteger mask) {
        BigInteger maskInverted = mask.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
        // Sudoku _c = new Sudoku(_config.getBoard());
        // Sudoku p = _config.filter(maskInverted);
        Sudoku p2 = _config.filter(maskInverted);

        if (p2.reduce()) {
            return false;
        }

        if (p2.solutionsFlag() != 2) {
            return false;
        }

        for (Sudoku anti : p2.antiDerivatives()) {
            String key = anti.toString();
            Integer cacheFlag = _validationCache.get(key);
            int flag;
            if (cacheFlag != null) {
                flag = cacheFlag.intValue();
            } else {
                flag = anti.solutionsFlag();
                _validationCache.put(key, flag);
            }

            if (flag != 1) {
                return false;
            }
        }

        return true;
    }

    public boolean isDerivative(BigInteger mask) {
        if (BigInteger.ZERO.equals(mask)) {
            return true;
        }

        // int bitCount = mask.bitCount();
        // for (int m = 0; m <= bitCount; m++) {
        //     for (BigInteger item : groupForBitCount(m).items) {
        //         if (item.and(mask).equals(item)) {
        //             return true;
        //         }
        //     }
        // }

        int len = this._itemGroupsByBitCount.size();
        for (int m = 0; m < len; m++) {
            ItemGroup group = this._itemGroupsByBitCount.get(m);
            for (BigInteger item : group.items) {
                if (item.equals(item.and(mask))) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addToReductionMatrix(BigInteger item) {
        for (int i = 0; i < Sudoku.SPACES; i++) {
            if (item.testBit(Sudoku.SPACES - 1 - i)) {
                reductionMatrix[i]++;
            }
        }
    }

    public void subtractFromReductionMatrix(BigInteger item) {
        for (int i = 0; i < Sudoku.SPACES; i++) {
            if (item.testBit(Sudoku.SPACES - 1 - i)) {
                reductionMatrix[i]--;
            }
        }
    }

    public void rawAdd(BigInteger item) {
        groupForBitCount(item.bitCount()).items.add(item);
        size++;
        addToReductionMatrix(item);
    }

    public boolean add(BigInteger item) {
        if (!_cache.contains(item)) {
            if (isDerivative(item) || !validate(item)) {
                return false;
            } else {
                _cache.add(item);
            }
        } else {
            // System.out.println("> SudokuSieve: Item found in cache. Adding to collection.");
        }
        if (groupForBitCount(item.bitCount()).items.add(item)) {
            size++;
            addToReductionMatrix(item);
            return true;
        }
        return false;
    }

    public boolean add(BigInteger item, boolean useCache) {
        if (!_cache.contains(item)) {
            if (isDerivative(item) || !validate(item)) {
                return false;
            } else {
                _cache.add(item);
            }
        } else {
            // System.out.println("> SudokuSieve: Item found in cache. Adding to collection.");
        }
        groupForBitCount(item.bitCount()).items.add(item);
        size++;
        addToReductionMatrix(item);
        return true;
    }

    public int addFromFilter(BigInteger mask) {
        return addFromFilter(mask, null);
    }

    public int addFromFilter(BigInteger mask, Consumer<String> callback) {
        int initialLength = size;
        BigInteger maskInverted = mask.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
        Sudoku puzzle = _config.filter(maskInverted);
        puzzle.searchForSolutions3(solution -> {
            BigInteger diff = _config.diff2(solution);
            if (!BigInteger.ZERO.equals(diff)) {
                if (add(diff) && callback != null) {
                    callback.accept(solution.toString());
                }
            }
            return true;
        });
        return size - initialLength;
    }

    // Removes and returns all items that have bits overlapping with the given mask.
    // Usually used for puzzle generation.
    // removeOverlapping(BigInteger mask)

    public void remove(BigInteger item) {
        groupForBitCount(item.bitCount()).items.remove(item);
        size--;
        subtractFromReductionMatrix(item);
    }

    public List<BigInteger> removeOverlapping(int cellIndex) {
        ArrayList<BigInteger> result = new ArrayList<>();

        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.removeIf((i) -> {
                boolean shouldRemove = i.testBit(Sudoku.SPACES - 1 - cellIndex);
                if (shouldRemove) {
                    result.add(i);
                    size--;
                    subtractFromReductionMatrix(i);
                }
                return shouldRemove;
            });
        }

        return result;
    }

    public List<BigInteger> removeOverlapping(int cellIndex, List<BigInteger> removedList) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.removeIf((i) -> {
                boolean shouldRemove = i.testBit(Sudoku.SPACES - 1 - cellIndex);
                if (shouldRemove) {
                    removedList.add(i);
                    size--;
                    subtractFromReductionMatrix(i);
                }
                return shouldRemove;
            });
        }
        return removedList;
    }

    public boolean doesMaskSatisfy(BigInteger mask) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            for (BigInteger item : group.items) {
                if (item.and(mask).equals(BigInteger.ZERO)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("{\n");

        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                strb.append(String.format("  [%d]: [\n", group.order));
                for (BigInteger item : group.items) {
                    // BigInteger itemInverted = item.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
                    strb.append(String.format("    %s\n", _config.filter(item).toString()));
                }
                strb.append("  ],\n");
            }
        }

        strb.append("}");
        return strb.toString();
    }

    public Map<Integer, List<BigInteger>> toMap() {
        Map<Integer, List<BigInteger>> map = new HashMap<>();
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                map.put(group.order, new ArrayList<>(group.items));
            }
        }
        return map;
    }

    public List<BigInteger> getCache() {
        ArrayList<BigInteger> result = new ArrayList<>();
        result.addAll(this._cache);
        return result;
    }

    public int[] orderCellsByNumOccurrences() {
        Map<Integer,Integer> r = new HashMap<>();

        for (int i = 0; i < Sudoku.SPACES; i++) {
            r.put(i, 0);
        }

        List<BigInteger> items = items(new ArrayList<>());
        for (BigInteger item : items) {
            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                int bit = Sudoku.SPACES - 1 - ci;
                if (item.testBit(bit)) {
                    // Possible NullPointerException, but all indices should be seeded 0 above.
                    int count = r.get(ci);
                    r.put(ci, count+1);
                }
            }
        }

        ArrayList<Entry<Integer,Integer>> list = new ArrayList<>();
        list.addAll(r.entrySet());
        list.sort((a, b) -> (b.getValue() - a.getValue()));
        int[] indicesSorted = new int[list.size()];
        for (int i = 0; i < indicesSorted.length; i++) {
            indicesSorted[i] = list.get(i).getKey();
        }

        return indicesSorted;
    }

    public static class SudokuSearch {
        public class Node {
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

                    // TODO ! FIX BEFORE USING CLASS AGAIN
                    // System.out.println(sieve.toString());
                }

                int j = 0;
                for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                    if (sieveItem.testBit(Sudoku.SPACES - 1 - ci)) {
                        this.choices[j++] = ci;
                    }
                }
                Shuffler.shuffle(this.choices);

                this.cellIndex = this.choices[this.index];
                this.digit = digits[this.cellIndex];
                chooseCell(this.cellIndex);
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
                removeCell(cellIndex);
            }

            boolean sub() {
                // System.out.println("> Node: sub()");
                putStuffBack();

                if (index <= 0) {
                    return false;
                }

                index--;

                cellIndex = choices[index];
                digit = digits[cellIndex];
                sieve.removeOverlapping(cellIndex, overlapping);
                chooseCell(this.cellIndex);
                return true;
            }

            String jsonStr() {
                StringBuilder overlappingStr = new StringBuilder("[\n");
                for (BigInteger item : overlapping) {
                    overlappingStr.append(String.format("  %s\n", grid.filter(item).toString()));
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
        // public class Node2 {
        //     int index;
        //     int digit;
        //     List<BigInteger> overlapping;

        //     Node2(int index) {
        //         this.index = index;
        //         this.digit = digits[this.index];
        //         chooseCell(this.index);
        //         this.overlapping = sieve.removeOverlapping(this.index, new ArrayList<>());
        //     }

        //     void putStuffBack() {
        //         int sieveSizeBefore = sieve.size();
        //         int overlappingSizeBefore = overlapping.size();
        //         overlapping.forEach(item -> sieve.add(item));
        //         overlapping.clear();
        //         if (sieve.size() != sieveSizeBefore + overlappingSizeBefore) {
        //             throw new RuntimeException("Something went wrong adding sieve items back.");
        //         }
        //         removeCell(index);
        //     }

        //     boolean sub() {
        //         putStuffBack();

        //         if (index <= 0) {
        //             return false;
        //         }

        //         index--;

        //         cellIndex = choices[index];
        //         digit = digits[cellIndex];
        //         sieve.removeOverlapping(cellIndex, overlapping);
        //         chooseCell(this.cellIndex);
        //         return true;
        //     }

        //     String jsonStr() {
        //         StringBuilder overlappingStr = new StringBuilder("[\n");
        //         for (BigInteger item : overlapping) {
        //             overlappingStr.append(String.format("  %s\n", grid.filter(item).toString()));
        //         }
        //         overlappingStr.append("]");

        //         return String.format(
        //             "{\n  choices: %s,\n  index: %d,\n  cellIndex: %d,\n  digit: %d,\n  overlapping: %s\n}",
        //             Arrays.toString(choices),
        //             index, cellIndex, digit,
        //             overlappingStr
        //         );
        //     }
        // }

        Sudoku grid;
        int[] digits;
        SudokuSieve sieve;
        List<BigInteger> sieveItems;

        BigInteger mask;
        BigInteger digitsMask = BigInteger.ZERO;
        int[] digitsUsed = new int[Sudoku.DIGITS + 1];

        public SudokuSearch(Sudoku grid) {
            this.grid = grid;
            this.digits = grid.getBoard();
        }

        public void init() {
            sieve = new SudokuSieve(grid.getBoard());
            // TODO export below to SudokuSieve
            for (int combo : Sudoku.DIGIT_COMBOS_MAP[4]) {
                Sudoku g = new Sudoku(grid);
                g.removeRows(combo);
                BigInteger m = g.getMask();
                System.out.println(g.toString());
                sieve.addFromFilter(Sudoku.invertMask(m), s -> { System.out.println(s.toString()); });

                g = new Sudoku(grid);
                g.removeCols(combo);
                System.out.println(g.toString());
                m = g.getMask();
                sieve.addFromFilter(Sudoku.invertMask(m), s -> { System.out.println(s.toString()); });

                g = new Sudoku(grid);
                g.removeRegions(combo);
                System.out.println(g.toString());
                m = g.getMask();
                sieve.addFromFilter(Sudoku.invertMask(m), s -> { System.out.println(s.toString()); });

                BigInteger f = grid.maskForDigits(combo);
                System.out.println(grid.filter(Sudoku.invertMask(f)).toString());
                sieve.addFromFilter(f, s -> { System.out.println(s.toString()); });
            }
            sieveItems = sieve.items(new ArrayList<>());

            mask = BigInteger.ZERO;
            digitsMask = BigInteger.ZERO;
            digitsUsed = new int[Sudoku.DIGITS + 1];
        }

        void chooseCell(int cellIndex) {
            // Is the cell already chosen? (error)
            // if (puzzleMask.testBit(SPACES - 1 - cellIndex)) {
            //     throw new RuntimeException("Sieve search tried to duplicate cell choice.");
            // }
            mask = mask.setBit(Sudoku.SPACES - 1 - cellIndex);
            digitsMask = digitsMask.setBit(digits[cellIndex]);
            digitsUsed[digits[cellIndex]]++;
        }

        void removeCell(int cellIndex) {
            mask = mask.clearBit(Sudoku.SPACES - 1 - cellIndex);
            int digit = digits[cellIndex];
            digitsUsed[digit] = Math.max(0, digitsUsed[digit] - 1);
            if (digitsUsed[digit] == 0) {
                digitsMask = digitsMask.clearBit(digit);
            }
        }

        int digitsUsed() {
            return digitsMask.bitCount();
        }

        int numClues() {
            return mask.bitCount();
        }

        boolean satisfiesAll() {
            for (BigInteger item : sieveItems) {
                if (item.and(mask).equals(BigInteger.ZERO)) {
                    return false;
                }
            }
            return true;
        }

        boolean isValidSudoku() {
            return grid.filter(mask).solutionsFlag() == 1;
        }

        List<Integer> cellsChosen(List<Integer> list) {
            for (int i = 0; i < Sudoku.SPACES; i++) {
                if (mask.testBit(Sudoku.SPACES - 1 - i)) {
                    list.add(i);
                }
            }
            return list;
        }

        @Override
        public String toString() {
            return grid.filter(mask).toString();
        }

        AtomicInteger count;
        PrintWriter pw;
        public void search(int level, Consumer<BigInteger> resultsCallback, int numClues) {
            init();

            HashSet<BigInteger> seen = new HashSet<>();
            Stack<Node> stack = new Stack<>();
            Node rootNode = new Node(sieve.first());
            stack.push(rootNode);
            count = new AtomicInteger();
            try {
                pw = new PrintWriter("17-search.txt");
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
                Sudoku.pressEnterToContinue();
            }

            while (!stack.isEmpty()) {
                // System.out.println(" ".repeat(12 + numClues()) + toString());
                if (sieve.isEmpty()) {
                    if (!seen.contains(mask)) {
                        // System.out.printf("[%d] %s\n", numClues(), toString());
                        // resultsCallback.accept(mask);
                        searchDown(seen, resultsCallback);
                    }

                    seen.add(mask);

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

                while (stack.size() >= Sudoku.MIN_CLUES && !stack.peek().sub()) {
                    stack.pop();
                }

                stack.push(new Node(sieve.first()));
                // if (!sieve.isEmpty()) {
                // } else {
                //     // TODO Remove -- This doesn't/can't ever trigger
                //     System.out.println("Could not push to stack from empty sieve");
                // }
            }

            System.out.println(count.get());
            pw.close();
        }

        void searchDown(HashSet<BigInteger> seen, Consumer<BigInteger> resultsCallback) {
            // if (mask.numClues() < MIN_CLUES) {
            //     return;
            // }

            // System.out.printf("%s v %s\n", " ".repeat(9 + numClues()), toString());

            // For each cell that's been chosen, see if removing it still satisfies the sieve
            // mask.cellsChosen(list);
            boolean hasReduction = false;
            for (int cellIndex : cellsChosen(new ArrayList<>())) {
                removeCell(cellIndex);
                if (
                    !seen.contains(mask) &&
                    satisfiesAll() // &&
                    // mask.digitsUsed() >= 8 &&
                    // (_valid = mask.isValidSudoku())
                ) {
                    hasReduction = true;
                    // System.out.printf(
                    //     "[%2d] %s %s\n",
                    //     numClues(),
                    //     (numClues() >= Sudoku.MIN_CLUES && isValidSudoku()) ? "* " : "  ",
                    //     toString()
                    // );
                    // System.out.printf("%s >> %s\n", " ".repeat(8 + numClues()), toString());
                    // System.out.println(toString());
                    // resultsCallback.accept(new BigInteger(mask.toByteArray()));
                    seen.add(mask);
                    searchDown(seen, resultsCallback);
                }
                chooseCell(cellIndex);
            }

            if (!hasReduction) {
                // resultsCallback.accept(new BigInteger(mask.toByteArray()));
                count.incrementAndGet();
                int numClues = numClues();
                String msg = String.format("[%d] %s", numClues(), toString());
                if (numClues == Sudoku.MIN_CLUES) {
                    pw.println(msg);
                    System.out.printf("⭐️ %s\n", msg);
                } else {
                    System.out.println(msg);
                }
            }
        }

        // TODO Search where choices are based on reductionMatrix
        // public void search2(int level, Consumer<BigInteger> resultsCallback) {
        //     init();

        //     // List<BigInteger> items = sieve.items(new ArrayList<>());
        //     HashSet<BigInteger> seen = new HashSet<>();

        //     Stack<Node2> stack = new Stack<>();
        //     Node2 rootNode = new Node2(sieve.firstImpact());
        //     stack.push(rootNode);

        //     // For search down
        //     // List<Integer> _list = new ArrayList<>();

        //     while (!stack.isEmpty()) {
        //         // int choiceCount = stack.size();
        //         // Node top = stack.peek();

        //         // System.out.println(" ".repeat(12 + numClues()) + toString());

        //         // If puzzleMask bits == target, run the checks and callback if puzzle found, then pop (or swap with alt) and continue
        //         // { checks: (1) digitsMask bits >= 8, (2) puzzleMask satisfies sieve, (3) puzzle is valid }
        //         if (
        //             // choiceCount == numClues ||
        //             sieve.isEmpty()
        //         ) {
        //             // boolean _seen = false, _satisfies = false, _valid = false;
        //             if (
        //                 !seen.contains(mask) // &&
        //                 // digitsUsed() >= 8
        //                 // (_satisfies = mask.satisfiesSieve()) &&
        //                 // (_valid = mask.isValidSudoku())
        //             ) {
        //                 // System.out.printf("@@@ > sieveSearch: ⭐️ [%24s] %s\n", mask.puzzleMask.toString(), mask.toString());
        //                 // System.out.printf(
        //                 //     "[%2d] %s %s\n",
        //                 //     numClues(),
        //                 //     (numClues() >= Sudoku.MIN_CLUES && isValidSudoku()) ? "* " : "  ",
        //                 //     mask.toString()
        //                 // );
        //                 // resultsCallback.accept(mask);

        //                 searchDown(seen, resultsCallback);
        //             } else {
        //                 // System.out.printf(
        //                 //     "> sieveSearch: ❌ [%20s] _seen=%b; _satisfies=%b; _valid=%b; %s\n",
        //                 //     mask.toString(),
        //                 //     _seen, _satisfies, _valid,
        //                 //     toString()
        //                 // );
        //             }

        //             seen.add(mask);

        //             while (!stack.peek().sub()) {
        //                 stack.pop();
        //             }

        //             continue;
        //         }

        //         // TODO Unused. Not sure if it will have an effect or just add overhead
        //         // Bits less than target
        //         // Can short-circuit early and pop if there's not enough choices left to satisfy digits constraint
        //         // (DIGITS - #digits used - 1) > (numClues - puzzleMask bits)
        //         // if ((DIGITS - mask.digitsUsed() - 1) > (numClues - choiceCount)) {
        //         //     int lastCellChoice = top.cellIndex;
        //         //     int lastDigitChoice = top.digit;
        //         //     System.out.println("> sieveSearch: Not enough choices left to satisfy digits constraint");
        //         //     top.sub();
        //         //     stack.pop();
        //         //     continue;
        //         // }

        //         // also if there's not enough choices left to satisfy disjoint UAs left in the sieve, but this may take compute time.
        //         // i.e., need to check how many non-overlapping sieve items are remaining
        //         // #non-overlapping sieve items > (numClues - choiceCount)

        //         // Otherwise... make another cell choice
        //         if (!sieve.isEmpty()) {
        //             stack.push(new Node2(sieve.firstImpact()));
        //         } else {
        //             System.out.println("Could not push to stack from empty sieve");
        //         }
        //     }
        // }
    }
}
