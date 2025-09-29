package com.metal_pony.bucket.sudoku.drivers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.metal_pony.bucket.sudoku.Sudoku;
import com.metal_pony.bucket.sudoku.SudokuSieve;
import com.metal_pony.bucket.sudoku.drivers.Main.DisjointSetsMap;
import com.metal_pony.bucket.sudoku.util.SudokuMask;
import com.metal_pony.bucket.util.Shuffler;

class SieveSearcher {
  class Choice {
    int[] indices;
    int current = -1; // Initialized to -1 so current() also returns -1
    List<Integer> seenContributions;
    List<SudokuMask> overlapping;

    // TODO Keep track of another overlapping; for disjoint sets
    DisjointSetsMap disjointOverlapping;

    Choice() {
      seenContributions = new ArrayList<>(8);
      overlapping = new ArrayList<>(64);
      disjointOverlapping = new DisjointSetsMap(grid);
    }

    void setChoices(int[] choices) {
      indices = choices;
      current = choices.length - 1;
      while (
        (current >= 0 && seenMask.testBit(choices[current])) //&&
        // TODO This logic is wrong
        //mask.bitCount() + disjointSetsMap.minToSatisfy(mask) >= maxClues
      ) {
        current--;
      }

      if (current >= 0) {
        mask.setBit(indices[current]);
        overlapping = sieve.removeOverlapping(indices[current], overlapping);
        // disjointSetsMap.splitOutOverlapping(mask, disjointOverlapping);
      }
    }

    int current() {
      return (current >= 0) ? indices[current] : -1;
    }

    void putItemsBack() {
      for (int i = overlapping.size() - 1; i >= 0; i--) {
        sieve.rawAdd(overlapping.remove(i));
      }

      // disjointOverlapping.dumpTo(disjointSetsMap);

      if (current >= 0) {
        mask.unsetBit(indices[current]);
      }
    }

    boolean hasNext() {
      return current > 0;
    }

    int next() {
      if (current < 0) return -1;

      putItemsBack();
      seenMask.setBit(indices[current]);
      seenContributions.add(indices[current]);

      while (
        (current >= 0 && seenMask.testBit(indices[current])) //&&
        // TODO This logic is wrong
        // mask.bitCount() + disjointSetsMap.minToSatisfy(mask) >= maxClues
      ) {
        current--;
      }
      if (current < 0) return -1;

      mask.setBit(indices[current]);
      sieve.removeOverlapping(indices[current], overlapping);
      // disjointSetsMap.splitOutOverlapping(mask, disjointOverlapping);
      return indices[current];
    }

    void releaseDead() {
      for (int i = seenContributions.size() - 1; i >= 0; i--)
        seenMask.unsetBit(seenContributions.remove(i));
    }

    public StringBuilder addToString(StringBuilder strb) {
      strb.append('(');
      for (int i = 0; i <= current; i++) {
        if (i > 0) strb.append(',');
        strb.append(indices[i]);
      }
      if (current < indices.length - 1) {
        strb.append('/');
        for (int i = current+1; i < indices.length; i++) {
          if (i > current) strb.append(',');
          strb.append(indices[i]);
        }
      }
      strb.append(')');
      return strb;
    }
  }

  Sudoku grid;
  SudokuSieve sieve;
  /** Cache of the original sieve items. DO NOT MODIFY THIS LIST.*/
  List<SudokuMask> origSieveItems;
  /** Cache of cell indices associated with each sieve item. DO NOT MODIFY THIS MAP.*/
  Map<SudokuMask,int[]> itemsIndices;

  // TODO Use disjointSetsMap
  DisjointSetsMap disjointSetsMap;

  /** The current mask being analyzed during the search.*/
  SudokuMask mask;
  SudokuMask seenMask = new SudokuMask();

  Stack<Choice> choiceStack;

  int maxClues;

  SieveSearcher(SudokuSieve sieve) {
    this(sieve, null);
  }

  SieveSearcher(SudokuSieve sieve, SudokuMask baseMask) {
    this.grid = sieve.config();
    this.origSieveItems = new ArrayList<>(sieve.size());
    this.sieve = new SudokuSieve(grid);
    this.itemsIndices = new HashMap<>(sieve.size());

    for (SudokuMask item : sieve.items()) {
      this.sieve.rawAdd(item);
      this.origSieveItems.add(item);
      int[] prev = itemsIndices.put(item, Shuffler.shuffle(item.toIndices()));
      if (prev != null)
        throw new RuntimeException("item already exists in indices map");
    }

    disjointSetsMap = new DisjointSetsMap(grid);
    disjointSetsMap.build(3);

    System.out.printf("SieveSearcher initialized with %d sieve items. Disjoint set built with %d items.\n", sieve.size(), disjointSetsMap.size());

    this.choiceStack = new Stack<>();
    this.mask = (baseMask == null) ? new SudokuMask() : new SudokuMask(baseMask);
    this.seenMask = (baseMask == null) ? new SudokuMask() : new SudokuMask(baseMask);
  }

  // Sample disjointSetsMap distribution (sieve level 2)
  // [ 1]: 55
  // [ 2]: 900
  // [ 3]: 5378
  // [ 4]: 12938
  // [ 5]: 15296
  // [ 6]: 10162
  // [ 7]: 4024
  // [ 8]: 929
  // [ 9]: 120
  // [10]: 7
  // Sample disjointSetsMap distribution (sieve level 3)
  // [ 1]: 362
  // [ 2]: 19780
  // [ 3]: 156182
  // [ 4]: 300873
  // [ 5]: 289510
  // [ 6]: 170763
  // [ 7]: 61649
  // [ 8]: 12990
  // [ 9]: 1450
  // [10]: 72

  public static class IntContainer {
    public static final int INT_DEFAULT = 81;
    int value;
    public IntContainer() { this(INT_DEFAULT); }
    public IntContainer(int n) { this.value = n; }
    public synchronized void setVal(int newVal) { value = newVal; }
  }

  void search(int max) {
    this.maxClues = max;
    long sieveSize = sieve.size();

    // This is functioning as the choice Stack.
    // Choice instances are created once (here).
    // When a cell is chosen, all remaining overlapping sieve items are taken from the sieve
    //    and held with the choice.
    // Then when a cell is removed as a choice, the items are put back into the sieve.
    Choice[] choices = new Choice[maxClues];
    for (int i = 0; i < maxClues; i++) choices[i] = new Choice();
    choices[0].setChoices(itemsIndices.get(sieve.first()));
    int depth = 1;

    long checked = 0L;
    long checkedSinceLastPrint = 0L;
    final long checkedToPrint = 10_000_000L;

    long satisfied = 0L; // Number of masks found that satisfied the sieve
    long puzzles = 0L; // Number of puzzles found directly from running this search

    // HashSet<SudokuMask> searchDownResults = new HashSet<>();
    // HashSet<String> searchDownSeen = new HashSet<>();
    // HashSet<SudokuMask> results = new HashSet<>();

    while (depth > 0) {
      checked++;
      checkedSinceLastPrint++;
      Choice top = choices[depth - 1];

      // Print progress if it's been a while.
      if (checkedSinceLastPrint > checkedToPrint) {
        checkedSinceLastPrint -= checkedToPrint;
        StringBuilder strb = new StringBuilder();
        for (Choice c : choices) {
          if (c.current() >= 0) {
            c.addToString(strb);
            strb.append(' ');
          }
        }
        System.out.printf("[%s] checked %12d; seen 0x%s; stack {%s}\n", LocalDateTime.now().toString(), checked, seenMask.toHexString(), strb.toString());
      }
      // String maskStr = grid.filterStr(mask);
      // System.out.printf("[%2d] %s%s\n", sieve.size(), " ".repeat(choiceStack.size()), maskStr);
      // System.out.printf("   [%4d] {%12d} >%s%s\n", sieve.size(), checked, " ".repeat(depth), grid.filterStr(mask));

      if (sieve.isEmpty()) {
        // String maskStr = grid.filterStr(mask);
        // System.out.println(maskStr);
        satisfied++;
        // System.out.printf("✅ [%4d] {%12d} >%s%s\n", sieve.size(), c, " ".repeat(depth), maskStr);
        // if (!seen.contains(maskStr)) {
        //   seen.add(maskStr);

        // searchDownResults.clear();
        // searchDownResults.add(new SudokuMask(mask));
        // searchDownSeen.clear();
        // searchDown(searchDownResults, searchDownSeen);

        int numBits = mask.bitCount();

        // for (SudokuMask _mask : searchDownResults) {
          int flag = grid.filter(mask).solutionsFlag();
          if (flag == 1) {
            puzzles++;
            System.out.printf(
              "⭐️ %d chkd : %d sieve : %d pzls [%d] %s\n",
              checked,
              sieveSize,
              puzzles,
              numBits,
              grid.filterStr(mask)
            );

            int newVal = Math.min(numBits, maxClues);
            if (newVal < maxClues) {
              // maxClues.setVal(newVal);
              maxClues = newVal;
            }
            // maxClues = Math.min(numBits, maxClues);

          } else {
            if (numBits < maxClues) {
              System.out.printf(
                "✅ %d chkd : %d sieve : %d pzls [%d] %s\n",
                checked,
                sieveSize,
                puzzles,
                numBits,
                grid.filterStr(mask)
              );

              final long _sieveSize = sieveSize;
              grid.filter(mask).searchForSolutions3(solution -> {
                SudokuMask diff = grid.diff2(solution);
                if (sieve.add(diff)) {
                  itemsIndices.put(diff, Shuffler.shuffle(diff.toIndices()));
                  System.out.printf("[%d] + %s\n", _sieveSize + 1, grid.filterStr(diff));
                  // sieveSize++;
                  return false;
                }
                return true;
              });
              sieveSize += sieve.size();
              if (sieve.size() == 0) {
                System.out.println("🚨 ?? no new sieve item found");
              }
              // Since a new sieve item was added, check from top of loop again now that sieve is not empty.
              continue;
            }

            // int n = maxClues;

            // System.out.printf("   [%4d] {%12d} >%s\n", numBits, checked, maskStr);
            // System.out.printf("   (%6d) {%d} [%d] {%8d} %s\n", puzzles, satisfied, numBits, dupes, grid.filterStr(_mask).toString());
          }
          // maxClues = Math.min(numBits, maxClues);
        // }

        top.putItemsBack();
        top.releaseDead();
        depth--;

        while (depth > 0 && choices[depth - 1].next() == -1) {
          choices[depth - 1].releaseDead();
          depth--;
        }

        continue;
      }

      // ELSE: sieve NOT empty
      if (depth < maxClues) {
        // choices[depth].setChoices(sieve.first().toIndices());
        choices[depth].setChoices(itemsIndices.get(sieve.first()));
        depth++;
      } else {
        while (depth > 0 && choices[depth - 1].next() == -1) {
          choices[depth - 1].releaseDead();
          depth--;
        }
      }
    }
  }

  void searchDown(Set<SudokuMask> resultSet, Set<String> searchDownSeen) {
    for (int i = 0; i < 81; i++) {
      if (mask.testBit(i)) {
        mask.unsetBit(i);

        String maskStr = mask.toString();
        if (!searchDownSeen.contains(maskStr)) {
          searchDownSeen.add(maskStr);

          // Check if mask still satisfies sieve
          boolean satisfied = true;
          for (SudokuMask item : origSieveItems) {
            if (!item.intersects(mask)) {
              satisfied = false;
              break;
            }
          }
          if (satisfied) {
            resultSet.add(new SudokuMask(mask.toString()));
            searchDown(resultSet, searchDownSeen);
          }
        }

        mask.setBit(i);
      }
    }
  }
}
