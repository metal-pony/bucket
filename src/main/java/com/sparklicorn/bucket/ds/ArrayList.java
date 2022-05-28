package com.sparklicorn.bucket.ds;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A basic list backed by an array.
 * The array will automatically expand or shrink upon adding or removing to conserve memory
 * depending on the ratio of elements to capacity of this list.
 */
public class ArrayList<E> implements List<E>, Serializable {

  /**
   * Default capacity of a list if otherwise not supplied.
   * This also serves as the minimum capacity.
   */
  public static final int DEFAULT_CAPCITY = 16;

  /**
   * Maximum number of elements an ArrayList can hold.
   */
  public static final int MAX_CAPCITY = Integer.MAX_VALUE >> 2;

  /**
   * Ratio of size to capacity at which the array will expand.
   */
  public static final double GROWTH_THRESHOLD = 0.75;

  /**
   * Ratio of size to capacity at which the array will shrink.
   */
  public static final double SHRINK_THRESHOLD = 0.25;

  private E[] list;
  private int size;

  private transient int mods;

  /**
   * Creates a new ArrayList with a default capacity.
   */
  public ArrayList() {
    this(DEFAULT_CAPCITY);
  }

  /**
   * Creates a new ArrayList with the given initial capacity.
   *
   * @param capacity Initial capacity. Must be >= DEFAULT_CAPACITY.
   */
  @SuppressWarnings("unchecked")
  public ArrayList(int capacity) {
    if (capacity < DEFAULT_CAPCITY || capacity > MAX_CAPCITY) {
      throw new IllegalArgumentException(String.format(
        "Error: Initial capacity (%d) out of bounds [%d, %d]",
        capacity,
        DEFAULT_CAPCITY,
        MAX_CAPCITY
      ));
    }

    this.list = (E[]) new Object[capacity];
    this.size = 0;
    this.mods = 0;
  }

  @SuppressWarnings("unchecked")
  private void ensureCapacity() {
    if (size > list.length * GROWTH_THRESHOLD) {
      if (list.length >= MAX_CAPCITY) {
        throw new OutOfMemoryError(String.format(
          "Max capacity is %d",
          MAX_CAPCITY
        ));
      }

      int newCapacity = Math.min(list.length << 1, MAX_CAPCITY);
      if (newCapacity > list.length) {
        E[] oldList = list;
        list = (E[]) new Object[newCapacity];
        System.arraycopy(oldList, 0, list, 0, oldList.length);
        mods++;
      }

    } else if (size < list.length * SHRINK_THRESHOLD) {
      int newCapacity = Math.max(list.length >> 1, DEFAULT_CAPCITY);
      if (newCapacity < list.length) {
        E[] oldList = list;
        list = (E[]) new Object[newCapacity];
        System.arraycopy(oldList, 0, list, 0, newCapacity);
        mods++;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof List)) {
      return false;
    }

    if (obj instanceof ArrayList) {
      ArrayList<?> objArrayList = (ArrayList<?>) obj;
      return (
        size == objArrayList.size &&
        list.length == objArrayList.list.length &&
        Arrays.equals(list, objArrayList.list)
      );
    }

    List<?> objList = (List<?>) obj;
    if (size != objList.size()) {
      return false;
    }

    int index = 0;
    for (Object otherElement : objList) {
      Object thisElement = this.list[index++];

      if (
        (otherElement != thisElement) &&
        (otherElement != null && otherElement.equals(thisElement))
      ) {
        return false;
      }
    }

    return true;
    // TODO check for either list modifcation
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (Object element : list) {
      result += 47 * ((element == null) ? 0 : element.hashCode());
    }

    return result;
  }

  @Override
  public String toString() {
    return Arrays.toString(trimToSize());
  }

  @SuppressWarnings("unchecked")
  public E[] trimToSize() {
    E[] trimmedList = (E[]) new Object[size];
    System.arraycopy(list, 0, trimmedList, 0, size);

    return trimmedList;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean contains(Object o) {
    return indexOf(o) >= 0;
  }

  @Override
  public Iterator<E> iterator() {
    return new Iter();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object[] toArray() {
    return toArray((E[]) new Object[size]);
  }

  @Override
  public <T> T[] toArray(T[] a) {
    System.arraycopy(list, 0, a, 0, size);
    return a;
  }

  @Override
  public boolean add(E e) {
    ensureCapacity();

    list[size] = e;
    size++;
    mods++;

    return true;
  }

  @Override
  public boolean remove(Object o) {
    int index = indexOf(o);

    if (index < 0) {
      return false;
    }

    ensureCapacity();
    shiftLeft(list, index);
    size--;
    mods++;

    return true;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object obj : c) {
      if (!contains(obj)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    for (E element : c) {
      add(element);
    }

    return true;
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    // TODO shift everything, then add by iterating over collection
    int i = index;
    for (E element: c) {
      add(i, element);
      i++;
    }

    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean hasChanged = false;

    for (int i = size - 1; i >= 0; i--) {
      if (c.contains(list[i])) {
        remove(i);
        hasChanged = true;
      }
    }

    return hasChanged;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    boolean hasChanged = false;

    for (int i = size - 1; i >= 0; i--) {
      if (!c.contains(list[i])) {
        remove(i);
        hasChanged = true;
      }
    }

    return hasChanged;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void clear() {
    list = (E[]) new Object[DEFAULT_CAPCITY];
    size = 0;
    mods++;
  }

  @Override
  public E get(int index) {
    validateIndexWithinListSize(index, this);
    return list[index];
  }

  @Override
  public E set(int index, E element) {
    validateIndexWithinListSize(index, this);

    E previous = list[index];
    list[index] = element;
    return previous;
  }

  @Override
  public void add(int index, E element) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException(String.format(
        "index %d out of bounds",
        index
      ));
    }

    ensureCapacity();
    shiftRight(list, index);
    list[index] = element;
    size++;
    mods++;
  }

  @Override
  public E remove(int index) {
    validateIndexWithinListSize(index, this);

    ensureCapacity();
    E result = list[index];
    shiftLeft(list, index);
    size--;
    mods++;

    return result;
  }

  @Override
  public int indexOf(Object o) {
    for (int i = 0; i < size; i++) {
      if (Objects.equals(o, list[i])) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    for (int i = size - 1; i >= 0; i--) {
      if (Objects.equals(o, list[i])) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public ListIterator<E> listIterator() {
    return new ListIter(0);
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    return new ListIter(index);
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    validateIndexWithinListSize(fromIndex, this);
    validateIndexWithinListSize(toIndex, this);
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException(String.format(
        "fromIndex (%d) must be <= toIndex (%d)",
        fromIndex,
        toIndex
      ));
    }

    int subSize = toIndex - fromIndex;
    int subCapacity = 1;
    // Implied this will never violate MAXIMUM_CAPACITY.
    while ((subCapacity <<= 1) < subSize);

    @SuppressWarnings("unchecked")
    E[] subList = (E[]) new Object[subCapacity];
    System.arraycopy(list, fromIndex, subList, 0, subSize);

    ArrayList<E> sub = new ArrayList<>();
    sub.list = subList;
    sub.size = subSize;

    return sub;
  }

  private static void validateIndexWithinListSize(int index, ArrayList<?> list) {
    if (index < 0 || index >= list.size) {
      throw new IndexOutOfBoundsException(index);
    }
  }

  static <T> void shiftLeft(T[] list, int fromIndex) {
    int lastIndex = list.length - 1;
    if (fromIndex < 0 || fromIndex > lastIndex) {
      throw new IndexOutOfBoundsException(fromIndex);
    }

    fromIndex = Math.max(fromIndex, 1);
    System.arraycopy(list, fromIndex, list, fromIndex - 1, list.length - fromIndex);
    list[lastIndex] = null;
  }

  static <T> void shiftRight(T[] list, int index) {
    int lastIndex = list.length - 1;
    if (index < 0 || index > lastIndex) {
      throw new IndexOutOfBoundsException(index);
    }

    if (index < lastIndex) {
      System.arraycopy(list, index, list, index + 1, lastIndex - index);
    }
    list[index] = null;
  }

  private class Iter implements Iterator<E> {
    int index;
    int expectedMods;

    Iter() {
      this.index = 0;
      this.expectedMods = mods;
    }

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public E next() {
      if (expectedMods != mods) {
        throw new RuntimeException("List has been modified");
      }

      if (index >= size) {
        throw new NoSuchElementException();
      }

      return list[index++];
    }
  }

  private class ListIter extends Iter implements ListIterator<E> {

    ListIter(int index) {
      super();
      this.index = index;
    }

    @Override
    public boolean hasPrevious() {
      return index > 0;
    }

    @Override
    public E previous() {
      if (expectedMods != mods) {
        throw new RuntimeException("List has been modified");
      }

      if (index < 0) {
        throw new NoSuchElementException();
      }

      return list[index--];
    }

    @Override
    public int nextIndex() {
      return index;
    }

    @Override
    public int previousIndex() {
      return index - 1;
    }

    @Override
    public void remove() {
      if (expectedMods != mods) {
        throw new RuntimeException("List has been modified");
      }

      ArrayList.this.remove(index);
      index--;
      expectedMods = mods;
    }

    @Override
    public void set(E e) {
      if (expectedMods != mods) {
        throw new RuntimeException("List has been modified");
      }

      ArrayList.this.set(index - 1, e); // TODO this can't be correct if index is supposed to be the next position
    }

    @Override
    public void add(E e) {
      if (expectedMods != mods) {
        throw new RuntimeException("List has been modified");
      }

      ArrayList.this.add(index, e);
      index++;
      expectedMods = mods;
    }
  }
}
