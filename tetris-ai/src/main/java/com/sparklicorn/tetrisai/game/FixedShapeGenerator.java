package com.sparklicorn.tetrisai.game;

import java.util.ArrayList;
import java.util.List;

import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.tetris.util.structs.ShapeQueue;
import com.sparklicorn.bucket.util.Shuffler;

//populates the queue with a fixed queue of 999,999 shapes.
//shapes are generated using 7-bag system and stored statically.
//must be initialized statically through init().
// When the queue is empty, it will repopulate with the same order
// or 999,999 shapes each time.
public class FixedShapeGenerator extends ShapeQueue {
	private static final int LENGTH = Shape.NUM_SHAPES * 1000;
	private static final List<Integer> SHARED_ITEMS = new ArrayList<>();

	/**
	 * Initializes the shape generator and creates a static lineup of shapes.
	 */
	public static void init() {
		while (SHARED_ITEMS.size() < LENGTH) {
			int[] SHAPES = { 1, 2, 3, 4, 5, 6, 7 };
			Shuffler.shuffle(SHAPES);
			for (int shape : SHAPES) {
				SHARED_ITEMS.add(shape);
			}
		}
	}

	public static FixedShapeGenerator get() {
		init();
		return new FixedShapeGenerator();
	}

	private FixedShapeGenerator() {
		super();
	}

	@Override
	public void ensureCapacity(int capacity) {
		if (size() < capacity) {
			shapeIndexQueue.addAll(SHARED_ITEMS);
		}
	}
}
