package com.metal_pony.bucket.sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.metal_pony.bucket.sudoku.util.SudokuMask;

public class SudokuSieve {
    /**
     * Filters the grid with the given mask, adding the solution diffs to the sieve
     * if they validate as unavoidable sets.
     * @param grid Sudoku grid associated with the sieve.
     * @param sieve List of unavoidable sets.
     * @param mask Applied to grid to create a puzzle.
     * @param announce Whether to print out when items are added.
     */
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
                if (mask.hasBitsSet(item))
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
            if (!p.allBranchesSolveUniquely())
                return true;

            // We've made it this far, so this diff is an Unavoidable Set ('UA' or 'sieve item')
            sieve.add(diff.flip());
            if (announce)
                System.out.println("+ " + grid.filter(diff).toString());

            return true;
        });
    }

    /**
     * Populates a sieve for a given grid.
     * @param grid Full and valid sudoku grid.
     * @param sieve (Optional) List of existing sieve items.
     * @param level 2 through 5. Higher numbers will search for more unavoidable sets, and will
     * take more time. Not recommended beyond 4.
     * @return The given sieve list of unavoidable sets, if provided, or a newly allocated list.
     */
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

            searchForItemsFromMask(grid, sieve, grid.maskForDigits(Sudoku.DIGIT_COMBOS_MAP[level][r]).flip(), false);
            searchForItemsFromMask(grid, sieve, rowMask, false);
            searchForItemsFromMask(grid, sieve, colMask, false);
            searchForItemsFromMask(grid, sieve, regionMask, false);
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

    /**
     * Checks whether the given list contains unavoidable sets.
     * TODO NYI
     * @param grid
     * @param sieve
     * @return True if the list contains all unavoidable sets; otherwise false.
     */
    public static boolean isSieveValid(Sudoku grid, List<SudokuMask> sieve) {
        // TODO
        return false;
    }

    private static class ItemGroup {
        final int order;
        final ArrayList<SudokuMask> items;
        ItemGroup(int order) {
            this.order = order;
            this.items = new ArrayList<>();
        }
    }

    private final Sudoku _config;
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

        this._config = new Sudoku(config.getBoard());
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
    public List<SudokuMask> items() {
        return items(new ArrayList<>(size));
    }

    /**
     * Populates a List with copies of this sieve's items.
     * @param list A List to copy items into.
     * @return The given list, for convenience.
     */
    public List<SudokuMask> items(List<SudokuMask> list) {
        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.sort(SudokuMask::compareTo);
            for (SudokuMask item : group.items) {
                list.add(new SudokuMask(item.toString()));
            }
        }
        return list;
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

    /**
     * @return The first item in the sieve; null if the sieve is empty.
     */
    public SudokuMask first() {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                return group.items.get(0);
            }
        }

        return null;
    }

    /**
     * Searches for and returns the first item in the sieve that satifies the given predicate.
     * @param predicate Takes a SudokuMask and returns a boolean.
     * @return The found item; null if no items satisfy the predicate function.
     */
    public SudokuMask find(Function<SudokuMask,Boolean> predicate) {
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
        for (SudokuMask item : groupForBitCount(numClues).items) {
            results.add(new SudokuMask(item.toString()));
        }
        return results;
    }

    /**
     * Populates the sieve at the given level.
     * @param level 2 through 5. Higher numbers will search for more unavoidable sets, and will
     * take more time. Not recommended beyond 4.
     */
    public void seed(int level) {
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
            addFromFilter(rowMask);
            addFromFilter(colMask);
            addFromFilter(regionMask);
        }
    }

    /**
     * Checks whether the given SudokuMask is an unavoidable set.
     * @param mask Mask representing an unavoidable set.
     * @return True if the mask is an unavoidable set; otherwise false.
     */
    boolean validate(SudokuMask mask) {
        SudokuMask maskInverted = new SudokuMask(mask.toString()).flip();
        Sudoku p2 = _config.filter(maskInverted);

        int pEmptyCells = p2.numEmptyCells;
        p2.reduce();
        if (p2.numEmptyCells != pEmptyCells) return false;
        if (p2.solutionsFlag() != 2) return false;
        return p2.allBranchesSolveUniquely();
    }

    /**
     * Checks whether the given SudokuMask is derivative of an existing unavoidable set
     * already in this sieve.
     * @param mask
     * @return True if the mask is covered by an unavoidable set mask in this sieve; otherwise false.
     * Empty masks (0 bitCount are always TRUE).
     */
    synchronized boolean isDerivative(SudokuMask mask) {
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
    synchronized boolean rawAdd(SudokuMask item) {
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
    public void addFromFilter(SudokuMask mask) {
        _config.filter(mask.flip()).searchForSolutions3(solution -> {
            SudokuMask diff = _config.diff2(solution);
            if (
                diff.bitCount() > 0 &&
                !isDerivative(diff) &&
                validate(diff)
            ) {
                rawAdd(diff);
            }
            return true;
        });
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
    public List<SudokuMask> removeOverlapping(int cellIndex) {
        ArrayList<SudokuMask> list = new ArrayList<>();
        return removeOverlapping(cellIndex, list);
    }

    /**
     * Removes and returns all items that include the given cell index.
     * Items removed are automatically deducted from the reduction matrix.
     * @param cellIndex
     * @param removedList A list to add the removed items to.
     * @return The given list for convenience.
     */
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

    /**
     * Checks whether the given mask intersects with all sieve items.
     * @param mask
     * @return True if the mask contains at least one bit intersecting with each sieve item.
     */
    public boolean doesMaskSatisfy(SudokuMask puzzleMask) {
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
    public String toString() {
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
