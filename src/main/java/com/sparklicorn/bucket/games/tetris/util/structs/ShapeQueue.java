package com.sparklicorn.bucket.games.tetris.util.structs;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.sparklicorn.bucket.util.Shuffler;

/**
 * A self-populating queue of tetrominos.
 * The shuffling function can be specified, along with a buffer for how many
 * shapes the queue should contain internally before invoking the generator.
 * This queue implements the 7-bag system.
 */
public class ShapeQueue implements Queue<Shape> {

	// TODO Change language as this concept is not a "buffer".
	public static final int DEFAULT_BUFFER_SIZE = 7;

	protected final int[] SHAPE_INDICES;
	protected Shuffler generator;
	protected final int bufferSize;
	protected final LinkedList<Integer> shapeIndexQueue;

	/**
	 * Creates a new ShapeQueue using the SevenBagGenerator by default.
	 */
	public ShapeQueue() {
		this(new Shuffler(), DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new ShapeQueue using the specified Shape generator.
	 * @param generator - Shuffler used to populate the queue.
	 * @param bufferSize -
	 */
	public ShapeQueue(Shuffler generator, int bufferSize) {
		this.generator = generator;
		this.bufferSize = Math.max(DEFAULT_BUFFER_SIZE, bufferSize);
		this.shapeIndexQueue = new LinkedList<>();

		this.SHAPE_INDICES = new int[Shape.NUM_SHAPES];
		for (int n = 1; n <= SHAPE_INDICES.length; n++) {
			SHAPE_INDICES[n-1] = n;
		}
	}

	/**
	 * Fills shapeIndexQueue with randomly shuffled shape indices
	 * using the 7-bag system until the queue is >= bufferSize.
	 */
	protected void populate() {
		while (shapeIndexQueue.size() < bufferSize) {
			// TODO Temporary shuffle fix.
			// TODO generator.shuffle results in the same initial order (bug).
			// TODO This may be because it's initialized at app start.
			Shuffler.shuffleInts(SHAPE_INDICES);
			// generator.shuffle(SHAPE_INDICES);
			for (int i : SHAPE_INDICES) {
				shapeIndexQueue.offer(i);
			}
		}
	}

	/**
	 * Gets the next shape in the queue. If the queue is empty,
	 * calls populate() to generate more shapes.
	 */
	@Override
	public Shape poll() {
		if (shapeIndexQueue.isEmpty()) {
			populate();
		}

		return Shape.getShape(shapeIndexQueue.poll());
	}

	/**
	 * Gets the next shape in the queue, but does not modify it.
	 * If empty, will populate with more shapes.
	 */
	@Override public Shape peek() {
		if (shapeIndexQueue.isEmpty()) {
			populate();
		}

		return Shape.getShape(shapeIndexQueue.peek());
	}

	@Override public Shape remove() { return poll(); }
	@Override public Shape element() { return peek(); }
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
