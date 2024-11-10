package com.metal_pony.bucket.sudoku;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

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
    private final Sudoku2 _config;
    private int size;
    private final ArrayList<ItemGroup> _itemGroupsByBitCount;
    private HashSet<BigInteger> _cache;

    public SudokuSieve(Sudoku config) {
        if (!config.isSolved()) {
            throw new IllegalArgumentException("could not create sieve for malformed grid");
        }

        // this._configStr = config.toString();
        this._config = new Sudoku2(config.getBoard());
        this._itemGroupsByBitCount = new ArrayList<>(Sudoku.SPACES + 1);
        for (int n = 0; n <= Sudoku.SPACES; n++) {
            this._itemGroupsByBitCount.add(n, new ItemGroup(n));
        }
        this._cache = new HashSet<>();
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

    public BigInteger first() {
        for (ItemGroup group : _itemGroupsByBitCount) {
            if (group.items.size() > 0) {
                return group.items.first();
            }
        }

        // for (int m = 0; m < _itemGroupsByBitCount.size(); m++) {
        //     ItemGroup group = groupForBitCount(m);
        //     if (group.items.size() > 0) {
        //         return group.items.first();
        //     }
        // }

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
        Sudoku2 _c = new Sudoku2(_config.getBoard());
        // Sudoku p = _config.filter(maskInverted);
        Sudoku2 p2 = _c.filter(maskInverted);

        return (
            !p2.reduce() &&
            p2.solutionsFlag() == 2 &&
            p2.antiDerivatives().stream().allMatch(ad -> (ad.solutionsFlag() == 1))
        );
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

    public void rawAdd(BigInteger item) {
        groupForBitCount(item.bitCount()).items.add(item);
        size++;
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
        return true;
    }

    public int addFromFilter(BigInteger mask) {
        return addFromFilter(mask, null);
    }

    public int addFromFilter(BigInteger mask, Consumer<String> callback) {
        int initialLength = size;
        BigInteger maskInverted = mask.xor(BigInteger.ONE.shiftLeft(Sudoku.SPACES).subtract(BigInteger.ONE));
        Sudoku2 puzzle = _config.filter(maskInverted);
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
    }

    public List<BigInteger> removeOverlapping(int cellIndex) {
        ArrayList<BigInteger> result = new ArrayList<>();

        for (ItemGroup group : _itemGroupsByBitCount) {
            group.items.removeIf((i) -> {
                boolean shouldRemove = i.testBit(Sudoku.SPACES - 1 - cellIndex);
                if (shouldRemove) {
                    result.add(i);
                    size--;
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

    public List<BigInteger> getCache() {
        ArrayList<BigInteger> result = new ArrayList<>();
        result.addAll(this._cache);
        return result;
    }
}
