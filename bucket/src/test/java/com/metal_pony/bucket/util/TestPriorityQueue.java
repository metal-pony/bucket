package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.metal_pony.bucket.util.PriorityQueue;

public class TestPriorityQueue {

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

    private static final int SMALL_SIZE = 100;
    private static final int LARGE_SIZE = 100_000;

    private static ThreadLocalRandom rand = ThreadLocalRandom.current();

    private PriorityQueue<Integer> pq;
    private Integer[] expectedElements;

    @BeforeEach
    public void setup() {
        pq = new PriorityQueue<>();
        expectedElements = null;
    }

    private void setupRandomSet(int size, int origin, int bound) {
        print("Generating random numbers... ");
        expectedElements = new Integer[size];
        for (int i = 0; i < size; i++) {
            expectedElements[i] = rand.nextInt(origin, bound);
        }
        println("Done.");
    }

    private void testRandomNums(int size, int origin, int bound) {
        printf(
            "Testing queue with %d random integers, interval [%d, %d)%n",
            size, origin, bound
        );
        setupRandomSet(size, origin, bound);
        testQueueWithExpectedNumbers();
    }

    private void testQueueWithExpectedNumbers() {
        int size = expectedElements.length;

        printf("Testing queue with %d numbers.%n", size);

        assertEquals(0, pq.size());
        assertTrue(pq.isEmpty());
        assertNull(pq.peek());
        assertNull(pq.poll());

        print("Offering to queue... ");
        for (int i = 0; i < size; i++) {
            assertTrue(pq.offer(expectedElements[i]));
            assertEquals(i + 1, pq.size());
        }
        println("Done.");

        print("Sorting expected numbers... ");
        Arrays.sort(expectedElements);
        println("Done.");

        print("Polling from queue... ");
        for (int i = 0; i < size; i++) {
            Integer peeked = pq.peek();
            int expected = expectedElements[i];

            assertEquals(size - i, pq.size());

            Integer polled = pq.poll();

            assertEquals(expected, peeked);
            assertEquals(expected, polled);
        }
        println("Done.");

        assertTrue(pq.isEmpty());
        assertEquals(0, pq.size());
        println("Test complete!");
    }

    private void testRandomNums(int size) {
        this.testRandomNums(size, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void testQueuePollsNumbersAsExpected() {
        expectedElements = new Integer[] { 99, -56, 5, 8, 5, 5, 0, -2, 6, -4, 1 };
        testQueueWithExpectedNumbers();
    }

    @Test
    public void testRandomNums_smallSize() {
        testRandomNums(SMALL_SIZE);
    }

    @Test
    public void testRandomNums_smallSize_repeatedElements() {
        testRandomNums(SMALL_SIZE, 0, SMALL_SIZE / 4);
    }

    @Test
    public void testRandomNums_smallSize_repeatedNegativeElements() {
        testRandomNums(SMALL_SIZE, -(SMALL_SIZE / 4), 0);
    }

    @Test
    public void testRandomNums_smallSize_allZeros() {
        testRandomNums(SMALL_SIZE, 0, 1);
    }

    @Test
    public void testRandomNums_largeSize() {
        testRandomNums(LARGE_SIZE);
    }

    @Test
    public void testRandomNums_largeSize_repeatedElements() {
        testRandomNums(LARGE_SIZE, 0, LARGE_SIZE / 4);
    }

    @Test
    public void testRandomNums_largeSize_repeatedNegativeElements() {
        testRandomNums(LARGE_SIZE, -(LARGE_SIZE / 4), 0);
    }

    @Test
    public void testRandomNums_largeSize_allZeros() {
        testRandomNums(LARGE_SIZE, 0, 1);
    }
}
