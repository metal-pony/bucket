package com.sparklicorn.bucket.ds;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestArrayList {

  ArrayList<Integer> list;

  @BeforeEach
  void before() {
    resetList();
  }

  private void resetList() {
    this.list = new ArrayList<>();
  }

  @Test
  void testConstructorCreatesEmptyList() {
    assertTrue(list.isEmpty());
  }

  @Test
  void testConstructorCapacity() {
    int[] capacityOutOfBounds = new int[] {
      // Capacity too smol
      Integer.MIN_VALUE,
      -1,
      0,
      ArrayList.DEFAULT_CAPCITY - 1,
      // Capacity too swol
      ArrayList.MAX_CAPCITY + 1,
      Integer.MAX_VALUE
    };
    for (int capacity : capacityOutOfBounds) {
      assertThrows(IllegalArgumentException.class, () -> {
        new ArrayList<>(capacity);
      });
    }

    int[] capacityIsOkay = new int[] {
      ArrayList.DEFAULT_CAPCITY,
      ArrayList.DEFAULT_CAPCITY << 1,
      ArrayList.DEFAULT_CAPCITY << 2,
      ArrayList.MAX_CAPCITY
    };
    for (int capacity : capacityIsOkay) {
      assertDoesNotThrow(() -> {
        new ArrayList<>(capacity);
      });
    }
  }

  @Test
  void testAdd_whenEmpty_returnsTrue() {
    assertTrue(list.add(1));
  }

  @Test
  void testAdd_whenNotAtMaxCapacity_returnsTrue() {
    assertTrue(list.add(1));
    assertTrue(list.add(1));
    assertTrue(list.add(1));
  }

  @Test
  void testShiftLeft() {
    Integer[] list = new Integer[] { 1, 3, 5, 7 };
    Integer[][] expectedShiftedLists = {
      new Integer[] { 3, 5, 7, null },
      new Integer[] { 3, 5, 7, null },
      new Integer[] { 1, 5, 7, null },
      new Integer[] { 1, 3, 7, null }
    };

    for (int i = 0; i < expectedShiftedLists.length; i++) {
      Integer[] actual = Arrays.copyOf(list, list.length);
      ArrayList.shiftLeft(actual, i);
      assertArrayEquals(expectedShiftedLists[i], actual);
    }
  }

  @Test
  void testShiftRight() {
    Integer[] list = new Integer[] { 1, 3, 5, 7 };
    Integer[][] expectedShiftedLists = {
      new Integer[] { null, 1, 3, 5 },
      new Integer[] { 1, null, 3, 5 },
      new Integer[] { 1, 3, null, 5 },
      new Integer[] { 1, 3, 5, null }
    };

    for (int i = 0; i < expectedShiftedLists.length; i++) {
      Integer[] actual = Arrays.copyOf(list, list.length);
      ArrayList.shiftRight(actual, i);
      assertArrayEquals(expectedShiftedLists[i], actual);
    }
  }

  @Test
  void testAddAtIndex() {
    final int listItemToAdd = 10;

    // When list is empty and index is 0
    list.add(0, listItemToAdd);
    resetList();

    // When index is negative, OR index > size, throws IndexOutOfBoundsException
    int[] badIndices = new int[] {
      -1, -100, Integer.MIN_VALUE,
      1, 100, Integer.MAX_VALUE
    };
    for (int index : badIndices) {
      assertThrows(IndexOutOfBoundsException.class, () -> {
        list.add(index, listItemToAdd);
      });
    }
    resetList();

    // Shifts existing items to the right
    for (int item = 0; item < 5; item++) {
      list.add(0, item);
    }
    assertArrayEquals(
      new Integer[] { 4, 3, 2, 1, 0 },
      list.toArray(new Integer[5])
    );
    for (int item = 5; item < 10; item++) {
      list.add(3, item);
    }
    assertArrayEquals(
      new Integer[] { 4, 3, 2, 9, 8, 7, 6, 5, 1, 0 },
      list.toArray(new Integer[10])
    );
    for (int item = 10; item < 15; item++) {
      list.add(list.size() - 1, item);
    }
    assertArrayEquals(
      new Integer[] { 4, 3, 2, 9, 8, 7, 6, 5, 1, 10, 11, 12, 13, 14, 0 },
      list.toArray(new Integer[15])
    );
  }

  @Test
  void testRemoveObject() {
    fail("nyi");
    // When item is not in list, returns false

    // When item is in list, returns true
  }

  @Test
  void testRemoveWithIndex() {
    fail("nyi");

  }

  @Test
  void testSize() {
    assertEquals(list.size(), 0);

    for (int n = 1; n < 10; n++) {
      list.add(1);
      assertEquals(list.size(), n);
    }
  }

  @Test
  void testIsEmpty() {
    assertTrue(list.isEmpty());

    for (int n = 1; n < 10; n++) {
      list.add(1);
      assertFalse(list.isEmpty());
    }

    list.clear();
    assertTrue(list.isEmpty());
  }

  @Test
  void testContains() {
    assertFalse(list.contains(0));

    list.add(0);
    assertTrue(list.contains(0));

    assertFalse(list.contains(123));
    list.add(123);
    assertTrue(list.contains(123));
  }

  @Test
  void testIterator() {
    fail("nyi");

  }

  @Test
  void testToArray() {
    fail("nyi");

  }

  @Test
  void testToArrayGivenArray() {
    fail("nyi");

  }

  @Test
  void testContainsAll() {
    fail("nyi");
    // When none of the given items are in the list, returns false

    // When some, not all, items are in the list, returns false

    // When all items are in the list, returns true
  }

  @Test
  void testAddAll() {
    fail("nyi");

    // When new capacity < MAXIMUM_CAPCITY, returns true

    // When new capacity > MAXIMUM_CAPACITY, throws out of memory error
  }

  @Test
  void testAddAllWithIndex() {
    fail("nyi");

    // When new capacity < MAXIMUM_CAPCITY, returns true

    // When new capacity > MAXIMUM_CAPACITY, throws out of memory error

    // Add some tests around index
  }

  @Test
  void testRemoveAll() {
    fail("nyi");

  }

  @Test
  void testRetainAll() {
    fail("nyi");

  }

  @Test
  void testClear() {
    assertDoesNotThrow(() -> list.clear());
    list.add(0);
    assertDoesNotThrow(() -> list.clear());
  }

  @Test
  void testGet() {
    // When list is empty, all indices are out of bounds
    int[] indicesOutOfBounds = new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
    for (int index : indicesOutOfBounds) {
      assertThrows(IndexOutOfBoundsException.class, () -> list.get(index));
    }

    list.add(2);
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    assertEquals(2, list.get(0));
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));

    list.add(3);
    assertEquals(2, list.get(0));
    assertEquals(3, list.get(1));

    list.add(5);
    assertEquals(2, list.get(0));
    assertEquals(3, list.get(1));
    assertEquals(5, list.get(2));
  }

  @Test
  void testSet() {
    // When list is empty, all indices are out of bounds
    int[] indicesOutOfBounds = new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
    for (int index : indicesOutOfBounds) {
      assertThrows(IndexOutOfBoundsException.class, () -> list.get(index));
    }

    list.add(1);
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    assertEquals(1, list.set(0, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));

    list.add(3);
    assertEquals(2, list.set(0, 4));
    assertEquals(3, list.set(1, 5));

    list.add(6);
    assertEquals(4, list.set(0, 6));
    assertEquals(5, list.set(1, 7));
    assertEquals(6, list.set(2, 8));
  }

  @Test
  void testIndexOf() {
    assertEquals(-1, list.indexOf(0));
    assertEquals(-1, list.indexOf(1));
    assertEquals(-1, list.indexOf(2));

    list.add(0);
    assertEquals(0, list.indexOf(0));

    list.add(0);
    assertEquals(0, list.indexOf(0));

    list.add(47);
    assertEquals(2, list.indexOf(47));

    list.add(5);
    list.add(11);
    list.add(47);

    assertEquals(0, list.indexOf(0));
    assertEquals(2, list.indexOf(47));
    assertEquals(3, list.indexOf(5));
    assertEquals(4, list.indexOf(11));
  }

  @Test
  void testLastIndexOf() {
    assertEquals(-1, list.lastIndexOf(0));
    assertEquals(-1, list.lastIndexOf(1));
    assertEquals(-1, list.lastIndexOf(2));

    list.add(0);
    assertEquals(0, list.lastIndexOf(0));

    list.add(0);
    assertEquals(1, list.lastIndexOf(0));

    list.add(47);
    assertEquals(2, list.lastIndexOf(47));

    list.add(5);
    list.add(11);
    list.add(47);

    assertEquals(1, list.lastIndexOf(0));
    assertEquals(5, list.lastIndexOf(47));
    assertEquals(3, list.lastIndexOf(5));
    assertEquals(4, list.lastIndexOf(11));
  }

  @Test
  void testListIterator() {
    fail("nyi");

  }

  @Test
  void testListIteratorWithIndex() {
    fail("nyi");

  }

  @Test
  void testSubList() {
    fail("nyi");

  }
}
