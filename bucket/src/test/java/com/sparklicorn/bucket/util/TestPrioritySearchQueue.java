package com.sparklicorn.bucket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestPrioritySearchQueue {
    private static final int SMALL_SIZE = 100;
    private static final int LARGE_SIZE = 10_000;

    private static final boolean DEBUG_OUTPUT = false;

    private static void print(String msg) {
        if (DEBUG_OUTPUT) {
            System.out.print(msg);
        }
    }

    private static void println(String msg) {
        if (DEBUG_OUTPUT) {
            System.out.println(msg);
        }
    }

    private static void printf(String format, Object... args) {
        if (DEBUG_OUTPUT) {
            System.out.printf(format, args);
        }
    }

    PrioritySearchQueue<Integer> queue;

    @BeforeAll
    private static void beforeAll() {
        println("TestPrioritySearchQueue");
    }

    @BeforeEach
    private void before() {
        queue = new PrioritySearchQueue<>();
    }

    private void test(List<Integer> offered, List<Integer> expectedInOrder) {
        int expectedSize = 0;
        Set<Integer> seen = new HashSet<>();

        print("When the queue is empty... ");
        assertTrue(queue.isEmpty());
        assertEquals(expectedSize, queue.size());
        assertEquals(seen.size(), queue.seenSize());
        assertNull(queue.peek());
        assertNull(queue.poll());
        assertFalse(queue.iterator().hasNext());
        assertFalse(queue.seenIterator().hasNext());
        println("Done.");

        print("When items are offered to the queue... ");
        for (int n : offered) {
            assertEquals(expectedSize, queue.size());
            assertEquals(seen.size(), queue.seenSize());
            assertEquals(seen.contains(n), queue.contains(n));

            if (!seen.contains(n) && expectedInOrder.contains(n)) {
                assertTrue(queue.canAccept(n));
                assertTrue(queue.offer(n));
                expectedSize++;
                seen.add(n);
                assertTrue(queue.hasSeen(n));
                assertEquals(expectedSize, queue.size());
                assertEquals(seen.size(), queue.seenSize());
            }

            assertFalse(queue.canAccept(n));
            assertFalse(queue.offer(n));
            assertEquals(seen.size(), queue.seenSize());
            assertEquals(expectedSize, queue.size());
        }
        println("Done.");

        print("When items are polled from the queue... ");
        for (int n : expectedInOrder) {
            assertTrue(queue.hasSeen(n));
            assertEquals(n, queue.peek());
            assertEquals(expectedSize, queue.size());
            assertEquals(seen.size(), queue.seenSize());

            assertEquals(n, queue.poll());
            expectedSize--;
            assertEquals(expectedSize, queue.size());
            assertEquals(seen.size(), queue.seenSize());

            assertTrue(queue.hasSeen(n));
            assertFalse(queue.offer(n));
            assertEquals(expectedSize, queue.size());
            assertEquals(seen.size(), queue.seenSize());
        }
        println("Done.");

        print("When no more items remain in the queue... ");
        assertNull(queue.peek());
        assertNull(queue.poll());
        assertFalse(queue.iterator().hasNext());
        assertTrue(queue.seenIterator().hasNext());
        println("Done.");
    }

    private List<Integer> duplicateRandomElements(Collection<Integer> collection, float duplicationRate, int passes) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Given collection may not be empty");
        }
        if (duplicationRate < 0f || duplicationRate > 1f) {
            throw new IllegalArgumentException("Duplication rate must be in [0,1]");
        }

        List<Integer> result = new ArrayList<>(collection);
        ThreadLocalRandom generator = ThreadLocalRandom.current();

        for (int pass = 0; pass < passes; pass++) {
            for (Integer e : collection) {
                if (generator.nextFloat() <= duplicationRate) {
                    result.add(e);
                }
            }
        }

        return result;
    }

    private List<Integer> sortSet(Set<Integer> set) {
        List<Integer> result = new ArrayList<>(set);
        result.sort((a, b) -> Integer.compare(a, b));
        return result;
    }

    private void test(int size, Function<Integer, Boolean> acceptanceCriteria) {
        printf("test(size = %d)\n", size);
        if (acceptanceCriteria != null) {
            queue = new PrioritySearchQueue<>(acceptanceCriteria);
            println("acceptanceCriteria set");
        }

        print("Generating random set... ");
        Set<Integer> randomSet = Shuffler.randomSet(
            size,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            (n) -> (acceptanceCriteria == null ? true : acceptanceCriteria.apply(n))
        );
        println("Done.");

        print("Sorting set as list... ");
        List<Integer> expectedInOrder = sortSet(randomSet);
        println("Done.");

        print("Duplicating random items in list... ");
        List<Integer> offered = duplicateRandomElements(expectedInOrder, 0.25f, 4);
        println("Done.");

        print("Shuffling list... ");
        Shuffler.shuffleList(offered);
        println("Done.");

        println("Testing queue... ");
        test(offered, expectedInOrder);
        println("Test complete.");
    }

    @Test
    public void testWithoutAcceptanceCriteria_small() {
        test(SMALL_SIZE, null);
    }

    @Test
    public void testWithoutAcceptanceCriteria_large() {
        test(LARGE_SIZE, null);
    }

    @Test
    public void test_small() {
        test(SMALL_SIZE, (n) -> n % 2 == 0);
    }

    @Test
    public void test_large() {
        test(LARGE_SIZE, (n) -> n % 2 == 0);
    }
}
