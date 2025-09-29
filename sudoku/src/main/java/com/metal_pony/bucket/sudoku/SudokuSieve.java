package com.metal_pony.bucket.sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.metal_pony.bucket.sudoku.util.SudokuMask;

public class SudokuSieve {
    private static class ItemGroup {
        final int order;
        final TreeSet<SudokuMask> items;
        ItemGroup(int order) {
            this.order = order;
            this.items = new TreeSet<>();
        }
    }

    private final Sudoku _config;
    private final int[] board;
    private int size;
    private final ArrayList<ItemGroup> _itemGroupsByBitCount;
    private int[] reductionMatrix;

    /**
     * Creates a new Sieve for the given sudoku configuration.
     * @param config Full and valid sudoku.
     * @throws IllegalArgumentException If the given sudoku is not full and valid.
     */
    public SudokuSieve(Sudoku config) {
        if (!config.isSolved()) {
            throw new IllegalArgumentException("could not create sieve for malformed grid");
        }

        this.board = config.getBoard();
        this._config = new Sudoku(this.board);
        this._itemGroupsByBitCount = new ArrayList<>(Sudoku.SPACES + 1);
        for (int n = 0; n <= Sudoku.SPACES; n++) {
            this._itemGroupsByBitCount.add(n, new ItemGroup(n));
        }
        this.reductionMatrix = new int[Sudoku.SPACES];
    }

    /**
     * Creates a new Sieve for the given sudoku board array.
     * @param configBoard Full and valid sudoku board.
     * @throws IllegalArgumentException If the given sudoku board is not full and valid.
     */
    public SudokuSieve(int[] configBoard) {
        this(new Sudoku(configBoard));
    }

    /**
     * @return Number of items in the sieve.
     */
    public int size() {
        return size;
    }

    /**
     * @return Whether the sieve contains no items.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * @return A Sudoku instance copy of the grid associated with this sieve.
     */
    public Sudoku config() {
        return new Sudoku(_config);
    }

    /**
     * @return A new List containing copies of this sieve's items.
     */
    public Set<SudokuMask> items() {
        return items(new HashSet<>(size));
    }

    /**
     * Populates a List with copies of this sieve's items.
     * @param list A List to copy items into.
     * @return The given list, for convenience.
     */
    public synchronized Set<SudokuMask> items(Set<SudokuMask> list) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            for (SudokuMask item : group.items) {
                list.add(new SudokuMask(item));
            }
        }
        return list;
    }

    /**
     * Maps sudoku cell indices to the number of times the cell appears among sieve items.
     */
    public synchronized int[] reductionMatrix() {
        return reductionMatrix(new int[Sudoku.SPACES]);
    }

    /**
     * Maps sudoku cell indices to the number of times the cell appears among sieve items.
     */
    public synchronized int[] reductionMatrix(int[] arr) {
        System.arraycopy(reductionMatrix, 0, arr, 0, Sudoku.SPACES);
        return arr;
    }

    /**
     * @return The first item in the sieve; null if the sieve is empty.
     */
    public synchronized SudokuMask first() {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                return group.items.first();
            }
        }

        return null;
    }

    /**
     * Searches for and returns the first item in the sieve that satifies the given predicate.
     * @param predicate Takes a SudokuMask and returns a boolean.
     * @return The found item; null if no items satisfy the predicate function.
     */
    public synchronized SudokuMask find(Function<SudokuMask,Boolean> predicate) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.isEmpty()) continue;
            for (SudokuMask item : group.items) {
                SudokuMask _item = new SudokuMask(item.toString());
                if (predicate.apply(_item)) {
                    return _item;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the group associated with the given bitCount.
     */
    ItemGroup groupForBitCount(int bitCount) {
        return _itemGroupsByBitCount.get(bitCount);
    }

    /**
     * Gets a list of items associated with the given number of clues.
     * @param numClues
     * @return A new List containing copies of the sieve items associated with the number of clues.
     * @throws IllegalArgumentException If numClues is out of range.
     */
    public List<SudokuMask> getItemByNumClues(int numClues) {
        if (numClues < 0 || numClues > Sudoku.SPACES) {
            throw new IllegalArgumentException("Invalid number of clues");
        }
        List<SudokuMask> results = new ArrayList<>();
        synchronized (this) {
            for (SudokuMask item : groupForBitCount(numClues).items) {
                results.add(new SudokuMask(item.toString()));
            }
        }
        return results;
    }

    public void seed(Collection<SudokuMask> masks) {
        masks.forEach(mask -> addFromFilter(mask));
    }

    // TODO These can be precompiled on class load
    public List<SudokuMask> areaCombos(int level) {
        if (level < 2 || level > 4) throw new IllegalArgumentException("Invalid level");

        List<SudokuMask> combos = new ArrayList<>();
        for (int combo : Sudoku.DIGIT_COMBOS_MAP[level]) {
            SudokuMask rowMask = new SudokuMask();
            SudokuMask colMask = new SudokuMask();
            SudokuMask regionMask = new SudokuMask();

            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
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

            combos.add(rowMask);
            combos.add(colMask);
            combos.add(regionMask);
        }

        return combos;
    }

    public List<SudokuMask> digitCombos(int level) {
        if (level < 2 || level > 4) throw new IllegalArgumentException("Invalid level");

        List<SudokuMask> combos = new ArrayList<>();
        int[] board = _config.getBoard();
        for (int combo : Sudoku.DIGIT_COMBOS_MAP[level]) {
            SudokuMask digMask = new SudokuMask();

            for (int ci = 0; ci < Sudoku.SPACES; ci++) {
                if ((combo & (1 << (board[ci]) - 1)) > 0) {
                    digMask.setBit(ci);
                }
            }

            combos.add(digMask);
        }

        return combos;
    }

    public List<SudokuMask> fullPrintCombos(int level) {
        List<SudokuMask> combos = new ArrayList<>();
        combos.addAll(digitCombos(level));
        combos.addAll(areaCombos(level));
        return combos;
    }

    public void seedThreaded(Collection<SudokuMask> masks, int numThreads) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            numThreads, numThreads,
            1L, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>()
        );

        masks.forEach(mask -> pool.submit(() -> {
            addFromFilter(mask);
        }));

        pool.shutdown();
        try {
			pool.awaitTermination(1L, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    public void seedThreaded(Collection<SudokuMask> masks) {
        seedThreaded(masks, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Checks whether the given SudokuMask is an unavoidable set.
     * @param mask Mask representing an unavoidable set.
     * @return True if the mask is an unavoidable set; otherwise false.
     */
    public boolean validate(SudokuMask mask) {
        return _config.filter(
            new SudokuMask(mask.toString()).flip()
        ).allBranchesSolveUniquely();
    }

    /**
     * Checks whether the given SudokuMask is derivative of an existing unavoidable set
     * already in this sieve.
     * @param mask
     * @return True if the mask is covered by an unavoidable set mask in this sieve; otherwise false.
     * Empty masks (0 bitCount are always TRUE).
     */
    public synchronized boolean isDerivative(SudokuMask mask) {
        if (mask.bitCount() == 0) return true;

        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                for (SudokuMask item : group.items) {
                    if (mask.hasBitsSet(item)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Adds the given item to the reduction matrix.
     * @param item Mask of the item to add.
     */
    synchronized void addToReductionMatrix(SudokuMask item) {
        for (int i = 0; i < Sudoku.SPACES; i++) {
            if (item.testBit(i)) {
                reductionMatrix[i]++;
            }
        }
    }

    /**
     * Subtracts the given item from the reduction matrix.
     * @param item Mask of the item to subtract.
     */
    synchronized void subtractFromReductionMatrix(SudokuMask item) {
        for (int i = 0; i < Sudoku.SPACES; i++) {
            if (item.testBit(i)) {
                reductionMatrix[i]--;
            }
        }
    }

    /**
     * Adds an item directly into the sieve without validating.
     * @param item Item to add.
     * @return True if the item was added; otherwise false if the item already exists.
     */
    public synchronized boolean rawAdd(SudokuMask item) {
        if (!groupForBitCount(item.bitCount()).items.contains(item)) {
            groupForBitCount(item.bitCount()).items.add(item);
            size++;
            addToReductionMatrix(item);
            return true;
        }
        return false;
    }

    /**
     * Attempts to add the given item to this sieve.
     * @param item Item to add.
     * @return True if the item was added; otherwise false if the item has not bits set;
     * if the item is derivative of an existing item;
     * if the item is not an unavoidable set;
     * if the item was previously added.
     */
    public synchronized boolean add(SudokuMask item) {
        if (
            item.bitCount() > 0 &&
            !isDerivative(item) &&
            validate(item)
        ) {
            rawAdd(item);
            return true;
        }
        return false;
    }

    /**
     * Filters the sieve's grid with the given mask, and for each solution,
     * adds the diff as an item if it validates as an unavoidable set.
     * @param mask Used to filter the sudoku grid associated with this sieve.
     */
    public int addFromFilter(SudokuMask mask) {
        AtomicInteger numAdded = new AtomicInteger();
        _config.filter(mask.flip()).searchForSolutions3(solution -> {
            SudokuMask diff = _config.diff2(solution);
            if (
                diff.bitCount() > 0 &&
                !isDerivative(diff) &&
                validate(diff)
            ) {
                rawAdd(diff);
                numAdded.incrementAndGet();
            }
            return true;
        });
        return numAdded.get();
    }

    public int addFromPuzzleMask(SudokuMask mask) {
        AtomicInteger numAdded = new AtomicInteger();
        _config.filter(mask).searchForSolutions3(solution -> {
            SudokuMask diff = _config.diff2(solution);
            if (
                diff.bitCount() > 0 &&
                !isDerivative(diff) &&
                validate(diff)
            ) {
                rawAdd(diff);
                numAdded.incrementAndGet();
            }
            return true;
        });
        return numAdded.get();
    }

    /**
     * Removes the specific item if it exists in the sieve.
     * @param item Item to remove.
     * @return True if the item was found and removed; otherwise false.
     */
    public synchronized boolean remove(SudokuMask item) {
        if (groupForBitCount(item.bitCount()).items.remove(item)) {
            size--;
            subtractFromReductionMatrix(item);
            return true;
        }
        return false;
    }

    /**
     * Removes and returns all items that include the given cell index.
     * Items removed are automatically deducted from the reduction matrix.
     * @param cellIndex
     * @return A list containing all items that were removed.
     */
    public synchronized List<SudokuMask> removeOverlapping(int cellIndex) {
        return removeOverlapping(cellIndex, new ArrayList<>());
    }

    /**
     * Removes and returns all items that include the given cell index.
     * Items removed are automatically deducted from the reduction matrix.
     * @param cellIndex
     * @param removedList A list to add the removed items to.
     * @return The given list for convenience.
     */
    public synchronized List<SudokuMask> removeOverlapping(int cellIndex, List<SudokuMask> removedList) {
        SudokuMask mask = new SudokuMask();
        mask.setBit(cellIndex);
        return removeOverlapping(mask, removedList);
    }

    public synchronized List<SudokuMask> removeOverlapping(SudokuMask mask, List<SudokuMask> removedList) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.removeIf((i) -> {
                // boolean shouldRemove = i.testBit(Sudoku.SPACES - 1 - cellIndex);
                boolean shouldRemove = i.intersects(mask);
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

    /**
     * Checks whether the given mask intersects with all sieve items.
     * @param puzzleMask
     * @return True if the mask contains at least one bit intersecting with each sieve item.
     */
    public synchronized boolean doesMaskSatisfy(SudokuMask puzzleMask) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            for (SudokuMask item : group.items) {
                // TODO There's no way this is correct, right?
                if (!item.intersects(puzzleMask)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public synchronized String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("{\n");

        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                strb.append(String.format("  [%d]: [\n", group.order));
                for (SudokuMask item : group.items) {
                    strb.append(String.format("    %s\n", _config.filter(item).toString()));
                }
                strb.append("  ],\n");
            }
        }

        strb.append("}");
        return strb.toString();
    }
}
