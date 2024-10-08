package com.metal_pony.bucket.util;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A priority queue backed by an ArrayList heap.
 */
public class PriorityQueue<T extends Comparable<T>> extends AbstractQueue<T> {

	protected static int leftIndex(int i) { return (2 * i) + 1; }
	protected static int rightIndex(int i) { return 2 * (i + 1); }
	protected static int parentIndex(int i) { return (i - 1) / 2; }

	/**
	 * Swaps two elements in the given list.
	 */
	protected static <S> void swap(List<S> list, int i, int j) {
		S temp = list.get(i);
		list.set(i, list.get(j));
		list.set(j, temp);
	}

	private List<T> heap;

	/**
	 * Creates a new empty PriorityQueue.
	 */
	public PriorityQueue() {
		this.heap = new ArrayList<>();
	}

	private boolean hasLeft(int i) { return (i >= 0 && leftIndex(i) < heap.size()); }
	private boolean hasRight(int i) { return (i >= 0 && rightIndex(i) < heap.size()); }
	private boolean hasParent(int i) { return i > 0; }

	private int compareRightAndLeft(int i) {
		return compare(rightIndex(i), leftIndex(i));
	}

	private int compare(int i, int j) {
		return heap.get(i).compareTo(heap.get(j));
	}

	/**
	 * Gets the lesser of the two children.
	 * @param i Index of the parent node in the heap.
	 * @return Index of the right child, if exists and value is less than the left child's;
	 * or the index of the left child, if exists; or -1 if neither child exists.
	 */
	private int getLesserChildIndex(int i) {
		if (hasRight(i)) {
			return (compareRightAndLeft(i) < 0) ? rightIndex(i) : leftIndex(i);
		} else if (hasLeft(i)) {
			return leftIndex(i);
		}

		return -1;
	}

	private boolean propogateDown(int i) {
		boolean modified = false;
		boolean stillPropogating = true;

		while (stillPropogating) {
			stillPropogating = false;
			int childIndex = getLesserChildIndex(i);
			if (childIndex > -1) {
				if (compare(childIndex, i) < 0) {
					swap(heap, i, childIndex);
					i = childIndex;
					stillPropogating = true;
					modified = true;
				}
			}
		}

		return modified;
	}

	private boolean propogateUp(int i) {
		boolean modified = false;

		int parentIndex = parentIndex(i);
		while (hasParent(i) && compare(i, parentIndex) < 0) {
			swap(heap, i, parentIndex);
			i = parentIndex;
			parentIndex = parentIndex(i);
			modified = true;
		}

		return modified;
	}

	@Override public boolean offer(T e) {
		heap.add(e);
		propogateUp(heap.size() - 1);
		return true;
	}

	@Override public T poll() {
		if (isEmpty()) {
			return null;
		}

		T result = heap.get(0);
		heap.set(0, heap.get(size() - 1));
		heap.remove(size() - 1);
		if (size() > 1) {
			propogateDown(0);
		}

		return result;
	}

	@Override public T peek() {
		return isEmpty() ? null : heap.get(0);
	}

	@Override public Iterator<T> iterator() {
		return heap.iterator();
	}

	@Override public int size() {
		return heap.size();
	}
}
