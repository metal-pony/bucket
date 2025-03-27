package com.metal_pony.bucket.sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.metal_pony.bucket.sudoku.util.SudokuMask;

public class SudokuSieve {
    public static void searchForItemsFromMask(
        Sudoku grid,
        List<SudokuMask> sieve,
        SudokuMask mask,
        boolean announce
    ) {
        grid.filter(mask).searchForSolutions3((solution) -> {
            SudokuMask diff = grid.diff2(solution);

            // Filter out solutions that are the original grid
            if (diff.bitCount() == 0)
                return true;
            // Filter out solutions already covered by an existing sieve item
            for (SudokuMask item : sieve) {
                if (mask.overlapsAllOf(item))
                    return true;
            }
            // Now, for a diff to be considered a sieve item...
            // (1) it must not be reducible
            Sudoku p = grid.filter(diff.flip());
            int pEmptyCells = p.numEmptyCells();
            p.reduce();
            if (p.numEmptyCells() != pEmptyCells)
                return true;
            // (2) it must have multiple solutions
            if (p.solutionsFlag() != 2)
                return true;
            // (3) for each empty cell, filling it with one of its remaining candidates and solving yields a solution
            if (!p.allAntiesSolve())
                return true;

            // We've made it this far, so this diff is an Unavoidable Set ('UA' or 'sieve item')
            sieve.add(diff.flip());
            if (announce)
                System.out.println("+ " + grid.filter(diff).toString());

            return true;
        });
    }

    public static List<SudokuMask> seedSieve(Sudoku grid, List<SudokuMask> sieve, int level) {
        if (grid == null)
            throw new IllegalArgumentException("Null grid");
        if (!grid.isSolved())
            throw new IllegalArgumentException("Invalid grid");
        if (level < 2 || level > 5)
            throw new IllegalArgumentException("Invalid level. Expected 2 <= level <= 5; got " + level);
        if (sieve == null)
            sieve = new ArrayList<>();

        for (int r = Sudoku.DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            int dCombo = Sudoku.DIGIT_COMBOS_MAP[level][r];

            SudokuMask rowMask = SudokuMask.full();
            SudokuMask colMask = SudokuMask.full();
            SudokuMask regionMask = SudokuMask.full();

            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                if ((dCombo & (1 << Sudoku.CELL_ROWS[ci])) > 0)
                    rowMask.unsetBit(ci);
                if ((dCombo & (1 << Sudoku.CELL_COLS[ci])) > 0)
                    colMask.unsetBit(ci);
                if ((dCombo & (1 << Sudoku.CELL_REGIONS[ci])) > 0)
                    regionMask.unsetBit(ci);
            }

            searchForItemsFromMask(grid, sieve, grid.maskForDigits(Sudoku.DIGIT_COMBOS_MAP[level][r]).flip(), true);
            searchForItemsFromMask(grid, sieve, rowMask, true);
            searchForItemsFromMask(grid, sieve, colMask, true);
            searchForItemsFromMask(grid, sieve, regionMask, true);
        }

        sieve.sort((a, b) -> {
            int aBits = a.bitCount();
            int bBits = b.bitCount();
            if (aBits > bBits)
                return 1;
            if (bBits > aBits)
                return -1;
            return a.compareTo(b);
        });

        return sieve;
    }

    public static boolean isSieveValid(Sudoku grid, List<SudokuMask> sieve) {

        return false;
    }

    // public static String maskString(BigInteger mask) {
    //     String bits = mask.toString(2);
    //     return ("0".repeat(Sudoku.SPACES - bits.length()) + bits).replaceAll("[0]", ".");
    // }

    private static class ItemGroup {
        final int order;
        final ArrayList<SudokuMask> items;
        ItemGroup(int order) {
            this.order = order;
            this.items = new ArrayList<>();
        }
    }

    // private final String _configStr;
    private final Sudoku _config;
    private int size;
    private final ArrayList<ItemGroup> _itemGroupsByBitCount;
    // private Set<BigInteger> _cache;
    // private Map<String,Integer> _validationCache;
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
        // this._cache = Collections.synchronizedSet(new HashSet<>());
        // this._validationCache = Collections.synchronizedMap(new HashMap<>());
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

    public List<SudokuMask> items() {
        return items(new ArrayList<>(size));
    }

    public List<SudokuMask> items(List<SudokuMask> list) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.sort(SudokuMask::compareTo);
            list.addAll(group.items);
        }
        return list;
    }

    public SudokuMask[] items(SudokuMask[] arr) {
        int i = 0;
        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.sort(SudokuMask::compareTo);
            for (SudokuMask item : group.items) {
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

    public SudokuMask first() {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                // return group.items.first();
                return group.items.get(0);
            }
        }

        return new SudokuMask();
    }

    public SudokuMask firstNonOverlapping(SudokuMask mask) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                for (SudokuMask item : group.items) {
                    // if (BigInteger.ZERO.equals(item.and(mask))) {
                    if (mask.overlapsAllOf(item)) {
                        return item;
                    }
                }
            }
        }

        return new SudokuMask();
    }

    ItemGroup groupForBitCount(int bitCount) {
        return _itemGroupsByBitCount.get(bitCount);
    }

    public List<SudokuMask> getItemByNumClues(int numClues) {
        if (numClues < 0 || numClues > Sudoku.SPACES) {
            throw new IllegalArgumentException("Invalid number of clues");
        }
        return new ArrayList<>(groupForBitCount(numClues).items);
    }

    public void seed(int level) {
        seed(level, null);
    }

    public Set<SudokuMask> genSeedMasks(int level) {
        Set<SudokuMask> masks = new HashSet<>();
        int[] board = _config.getBoard();
        for (int combo : Sudoku.DIGIT_COMBOS_MAP[level]) {
            SudokuMask digMask = new SudokuMask();
            SudokuMask rowMask = new SudokuMask();
            SudokuMask colMask = new SudokuMask();
            SudokuMask regionMask = new SudokuMask();

            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                if ((combo & (1 << (board[ci]) - 1)) > 0) {
                    digMask.setBit(ci);
                }
                if ((combo & (1 << Sudoku.CELL_ROWS[ci])) > 0) {
                    rowMask.setBit(ci);
                }
                if ((combo & (1 << Sudoku.CELL_COLS[ci])) > 0) {
                    colMask.setBit(ci);
                }
                if ((combo & (1 << Sudoku.CELL_REGIONS[ci])) > 0) {
                    regionMask.setBit(ci);
                }
            }

            digMask.flip();
            rowMask.flip();
            colMask.flip();
            regionMask.flip();

            // System.out.println(maskString(digMask));
            // System.out.println(maskString(rowMask));
            // System.out.println(maskString(colMask));
            // System.out.println(maskString(regionMask));

            masks.add(digMask);
            masks.add(rowMask);
            masks.add(colMask);
            masks.add(regionMask);
        }
        return masks;
    }

    public void seed(int level, ThreadPoolExecutor pool) {
        int[] board = _config.getBoard();
        for (int combo : Sudoku.DIGIT_COMBOS_MAP[level]) {
            SudokuMask digMask = new SudokuMask();
            SudokuMask rowMask = new SudokuMask();
            SudokuMask colMask = new SudokuMask();
            SudokuMask regionMask = new SudokuMask();

            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                if ((combo & (1 << (board[ci]) - 1)) > 0) {
                    digMask.setBit(ci);
                }
                if ((combo & (1 << Sudoku.CELL_ROWS[ci])) > 0) {
                    rowMask.setBit(ci);
                }
                if ((combo & (1 << Sudoku.CELL_COLS[ci])) > 0) {
                    colMask.setBit(ci);
                }
                if ((combo & (1 << Sudoku.CELL_REGIONS[ci])) > 0) {
                    regionMask.setBit(ci);
                }
            }

            addFromFilter(digMask);
            // System.out.println(maskString(digMask));
            addFromFilter(rowMask);
            // System.out.println(maskString(rowMask));
            addFromFilter(colMask);
            // System.out.println(maskString(colMask));
            addFromFilter(regionMask);
            // System.out.println(maskString(regionMask));
        }
    }

    public List<SudokuMask> solveMasksForDiffs(Set<SudokuMask> masks) {
        int numThreads = Runtime.getRuntime().availableProcessors() / 2;
        // int numThreads = 1;
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        List<SudokuMask> diffs = new ArrayList<>();

        List<Set<SudokuMask>> diffCatalog = new ArrayList<>();
        for (int i = 0; i < Sudoku.SPACES; i++) {
            diffCatalog.add(Collections.synchronizedSet(new HashSet<>()));
        }

        pool.prestartAllCoreThreads();
        Object printLock = new Object();
        for (SudokuMask mask : masks) {
            pool.submit(() -> {
                Sudoku puzzle = _config.filter(mask);
                AtomicInteger solutionCount = new AtomicInteger();
                puzzle.searchForSolutions3(solution -> {
                    solutionCount.incrementAndGet();
                    SudokuMask diff = _config.diff2(solution);
                    // if (BigInteger.ZERO.compareTo(diff) < 0) {
                    if (diff.bitCount() == 0) {
                        diffCatalog.get(diff.bitCount()).add(diff);
                    }
                    // diffs.add(_config.diff2(solution));
                    return true;
                });
                synchronized (printLock) {
                    System.out.printf("[%10d] %s\n", solutionCount.get(), puzzle.toString());
                }
            });
        }
        pool.shutdown();

        try {
            pool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Set<SudokuMask> subset : diffCatalog) {
            diffs.addAll(subset);
        }
        return diffs;
    }

    public void processDiffs(List<SudokuMask> diffs) {
        System.out.printf("Processing %d diffs...\n", diffs.size());

        System.out.println("Sorting diffs by bit count...");
        long start = System.currentTimeMillis();
        diffs.sort((a, b) -> (b.bitCount() - a.bitCount()));
        System.out.printf("Done (%d ms)\n", System.currentTimeMillis() - start);

        // This is too slow
        // int diffsSize = diffs.size();
        // diffs.removeIf(diff -> !validate(diff));
        // System.out.printf("Removed %d invalid items\n", diffsSize - diffs.size());

        int invalidCount = 0;
        int addFailures = 0;
        AtomicInteger derivativeCount = new AtomicInteger();
        long lastGc = System.currentTimeMillis();
        long gcInterval = 60000L;

        while (!diffs.isEmpty()) {
            if (System.currentTimeMillis() - lastGc > gcInterval) {
                System.gc();
                lastGc = System.currentTimeMillis();
            }

            SudokuMask diff = diffs.remove(diffs.size() - 1);
            if (!validate(diff)) {
                invalidCount++;
                System.out.print('.');
                continue;
            }

            if (rawAdd(diff)) {
                // System.out.println(_config.filter(diff).toString());
                System.out.print('+');
                // System.out.printf("Added: %s\n", maskString(diff));
                // diffsSize = diffs.size();
                AtomicInteger removeCount = new AtomicInteger();
                diffs.removeIf(d -> {
                    // boolean remove = diff.and(d).equals(diff);
                    boolean remove = diff.overlapsAllOf(d);
                    if (remove) {
                        removeCount.incrementAndGet();
                        derivativeCount.incrementAndGet();
                        // System.out.print('-');
                    }
                    return remove;
                });
                if (removeCount.get() > 0) {
                    System.out.printf("-[%d]", removeCount.get());
                }
                // System.out.printf("Removed %d items\n", diffsSize - diffs.size());
            } else {
                // System.out.println("⚠️ rawAdd failed?");
                System.out.print('!');
                addFailures++;
            }
        }

        System.out.printf("\nInvalid items: %d\n", invalidCount);
        System.out.printf("Derivatives removed: %d\n", derivativeCount.get());
        System.out.printf("Add failures: %d\n", addFailures);
    }

    // int numThreads = Runtime.getRuntime().availableProcessors() / 2;
    // ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    boolean validate(SudokuMask mask) {
        // BigInteger maskInverted = mask.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
        SudokuMask maskInverted = new SudokuMask(mask.toString()).flip();
        // Sudoku _c = new Sudoku(_config.getBoard());
        // Sudoku p = _config.filter(maskInverted);
        Sudoku p2 = _config.filter(maskInverted);

        int pEmptyCells = p2.numEmptyCells;
        p2.reduce();
        if (p2.numEmptyCells != pEmptyCells) {
            return false;
        }

        if (p2.solutionsFlag() != 2) {
            return false;
        }

        for (Sudoku anti : p2.antiDerivatives()) {
            String key = anti.toString();
            // Integer cacheFlag = _validationCache.get(key);
            int flag;
            // if (cacheFlag != null) {
            //     flag = cacheFlag.intValue();
            // } else {
                flag = anti.solutionsFlag();
            //     _validationCache.put(key, flag);
            // }

            if (flag != 1) {
                return false;
            }
        }

        return true;
    }

    public synchronized boolean isDerivative(SudokuMask mask) {
        if (mask.bitCount() == 0) {
            return true;
        }

        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                for (SudokuMask item : group.items) {
                    if (mask.overlapsAllOf(item)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public synchronized void addToReductionMatrix(SudokuMask item) {
        for (int i = 0; i < Sudoku.SPACES; i++) {
            if (item.testBit(i)) {
                reductionMatrix[i]++;
            }
        }
    }

    public synchronized void subtractFromReductionMatrix(SudokuMask item) {
        for (int i = 0; i < Sudoku.SPACES; i++) {
            if (item.testBit(i)) {
                reductionMatrix[i]--;
            }
        }
    }

    public synchronized boolean rawAdd(SudokuMask item) {
        if (!groupForBitCount(item.bitCount()).items.contains(item)) {
            groupForBitCount(item.bitCount()).items.add(item);
            size++;
            addToReductionMatrix(item);
            return true;
        }
        return false;
    }

    public boolean add(SudokuMask item) {
        // if (!_cache.contains(item)) {
            // // if (isDerivative(item) || !validate(item)) {
            if (!validate(item)) {
                return false;
            } else {
                // _cache.add(item);
            }
        // }

        return rawAdd(item);
    }

    public boolean add(SudokuMask item, boolean useCache) {
        // if (!_cache.contains(item)) {
            // // if (isDerivative(item) || !validate(item)) {
            if (!validate(item)) {
                return false;
            } else {
                // _cache.add(item);
            }
        // }

        return rawAdd(item);
    }

    public void addFromFilter(SudokuMask mask) {
        addFromFilter(mask, null, null);
    }

    public void addFromFilter(SudokuMask mask, ThreadPoolExecutor pool) {
        addFromFilter(mask, null, pool);
    }

    public void addFromFilter(SudokuMask mask, Consumer<String> callback, ThreadPoolExecutor pool) {
        // int initialLength = size;
        SudokuMask maskInverted = new SudokuMask(mask.toString()).flip();
        Sudoku puzzle = _config.filter(maskInverted);
        // List<Future<Boolean>> resultsProcessing = new LinkedList<>();
        ArrayList<SudokuMask> diffs = new ArrayList<>();
        puzzle.searchForSolutions3(solution -> {
            diffs.add(_config.diff2(solution));
            // if (_cache.contains(diff)) {
                // rawAdd(diff);
            // } else

            //  if (!BigInteger.ZERO.equals(diff) && !isDerivative(diff)) {
            //     Runnable adder = () -> {
            //         if (!validate(diff)) {
            //             return;
            //         }
            //         // _cache.add(diff);
            //         if (rawAdd(diff)) {
            //             if (callback != null) {
            //                 callback.accept(solution.toString());
            //             }
            //             // return true;
            //         }
            //         // return false;
            //     };

            //     if (pool == null) {
            //         adder.run();
            //     } else {
            //         pool.submit(adder);
            //     }
            // }

            return true;
        });

        diffs.removeIf(diff -> isDerivative(diff));
        diffs.sort((a, b) -> (b.bitCount() - a.bitCount()));
        while (!diffs.isEmpty()) {
            SudokuMask diff = diffs.remove(diffs.size() - 1);
            if (!validate(diff)) {
                // invalidCount++;
                // System.out.print('.');
                continue;
            }

            if (rawAdd(diff)) {
                // System.out.print('+');
                // System.out.printf("Added: %s\n", maskString(diff));
                // diffsSize = diffs.size();
                diffs.removeIf(d -> {
                    // boolean remove = diff.and(d).equals(diff);
                    boolean remove = diff.overlapsAllOf(d);
                    // if (remove) {
                    //     derivativeCount.incrementAndGet();
                    //     System.out.print('-');
                    // }
                    return remove;
                });
                // System.out.printf("Removed %d items\n", diffsSize - diffs.size());
            } else {
                // System.out.println("⚠️ rawAdd failed?");
                // System.out.print('!');
                // addFailures++;
            }
        }

        // return resultsProcessing;
    }

    public boolean remove(SudokuMask item) {
        if (groupForBitCount(item.bitCount()).items.remove(item)) {
            size--;
            subtractFromReductionMatrix(item);
            return true;
        }
        return false;
    }

    /**
     * Removes and returns all items that include the given cell index.
     */
    public List<SudokuMask> removeOverlapping(int cellIndex) {
        ArrayList<SudokuMask> result = new ArrayList<>();

        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.removeIf((i) -> {
                // boolean shouldRemove = i.testBit(Sudoku.SPACES - 1 - cellIndex);
                boolean shouldRemove = i.testBit(cellIndex);
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

    public List<SudokuMask> removeOverlapping(int cellIndex, List<SudokuMask> removedList) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.removeIf((i) -> {
                // boolean shouldRemove = i.testBit(Sudoku.SPACES - 1 - cellIndex);
                boolean shouldRemove = i.testBit(cellIndex);
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

    public boolean doesMaskSatisfy(SudokuMask mask) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            for (SudokuMask item : group.items) {
                // if (item.and(mask).equals(BigInteger.ZERO)) {
                if (item.overlapsAllOf(mask)) {
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
                for (SudokuMask item : group.items) {
                    // BigInteger itemInverted = item.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
                    strb.append(String.format("    %s\n", _config.filter(item).toString()));
                }
                strb.append("  ],\n");
            }
        }

        strb.append("}");
        return strb.toString();
    }

    public String toHexString() {
        StringBuilder strb = new StringBuilder();
        strb.append("{\n");

        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                strb.append(String.format("  [%d]: [\n", group.order));
                for (SudokuMask item : group.items) {
                    // BigInteger itemInverted = item.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
                    strb.append(String.format("    %s\n", _config.filter(item).toString()));
                }
                strb.append("  ],\n");
            }
        }

        strb.append("}");
        return strb.toString();
    }

    public Map<Integer, List<SudokuMask>> toMap() {
        Map<Integer, List<SudokuMask>> map = new HashMap<>();
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                map.put(group.order, new ArrayList<>(group.items));
            }
        }
        return map;
    }

    public int[] orderCellsByNumOccurrences() {
        Map<Integer,Integer> r = new HashMap<>();

        for (int i = 0; i < Sudoku.SPACES; i++) {
            r.put(i, 0);
        }

        List<SudokuMask> items = items(new ArrayList<>());
        for (SudokuMask item : items) {
            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                // int bit = Sudoku.SPACES - 1 - ci;
                // if (item.testBit(bit)) {
                if (item.testBit(ci)) {
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
}
