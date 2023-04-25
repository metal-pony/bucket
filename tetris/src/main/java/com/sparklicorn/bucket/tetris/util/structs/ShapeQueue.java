package com.sparklicorn.bucket.tetris.util.structs;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.sparklicorn.bucket.util.Shuffler;

/**
 * A self-generating queue of tetris shapes, using the 7-bag system.
 * This structure only allows items to be polled, nothing may be offered to it.
 * A minimum number of elements can be specified, and if the queue size falls below this
 * threshold, it will automatically generate more as needed.
 */
public class ShapeQueue implements Queue<Shape> {
	public static final int DEFAULT_MIN_SIZE = 14;

	protected static final List<Integer> SHAPES = Shuffler.range(1, Shape.NUM_SHAPES + 1);

	protected final int minSize;
	protected final LinkedList<Integer> shapeIndexQueue;

	/**
	 * Creates a new ShapeQueue.
	 */
	public ShapeQueue() {
		this(DEFAULT_MIN_SIZE);
	}

	/**
	 * Creates a new ShapeQueue using the specified shuffler.
	 *
	 * @param minSize - The minimum number of elements in the queue at any given time.
	 * When the number of elements falls below this threshold, new elements are
	 * automatically added.
	 */
	public ShapeQueue(int minSize) {
		this.minSize = Math.max(DEFAULT_MIN_SIZE, minSize);
		this.shapeIndexQueue = new LinkedList<>();
	}

	/**
	 * Creates a copy of the given queue.
	 * Queue elements are shallow-copied into the new queue.
	 */
	public ShapeQueue(ShapeQueue other) {
		minSize = other.minSize;
		other.ensureCapacity(other.minSize);
		this.shapeIndexQueue = new LinkedList<>(other.shapeIndexQueue);
	}

	/**
	 * Generates new elements as necessary to ensure the queue contains the given capacity.
	 */
	public void ensureCapacity(int capacity) {
		while (size() < capacity) {
			shapeIndexQueue.addAll(Shuffler.shuffleList(SHAPES));
		}
	}

	/**
	 * Gets the next shape in the queue.
	 * More items may be added to the queue to ensure a minumum capcity.
	 */
	@Override
	public Shape poll() {
		ensureCapacity(minSize + 1);
		return Shape.getShape(shapeIndexQueue.poll());
	}

	/**
	 * Gets the shape at the head of the queue, but does not remove it.
	 * More items may be added to the queue to ensure a minumum capcity.
	 */
	@Override
	public Shape peek() {
		ensureCapacity(minSize + 1);
		return Shape.getShape(shapeIndexQueue.peek());
	}

	@Override
	public Shape remove() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}

		return poll();
	}

	@Override
	public Shape element() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}

		return peek();
	}

	@Override public int size() { return shapeIndexQueue.size(); }
	@Override public boolean isEmpty() { return shapeIndexQueue.isEmpty(); }
	@Override public void clear() { shapeIndexQueue.clear(); }

	/** Throws UnsupportedOperationException */
	@Override public boolean contains(Object o) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public Iterator<Shape> iterator() { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public Object[] toArray() { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public <T> T[] toArray(T[] a) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean containsAll(Collection<?> c) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean addAll(Collection<? extends Shape> c) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean add(Shape e) { throw new UnsupportedOperationException(); }
	/** Throws UnsupportedOperationException */
	@Override public boolean offer(Shape e) { throw new UnsupportedOperationException(); }
}
