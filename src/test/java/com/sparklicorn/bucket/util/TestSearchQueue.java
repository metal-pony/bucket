package com.sparklicorn.bucket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSearchQueue {
    private SearchQueue<Integer> queue;

    private final Function<Integer,Boolean> EVENS_ONLY = (n) -> n % 2 == 0;

    @BeforeEach
    public void setup() {
        queue = new SearchQueue<>();
    }

    @Test
    public void test() {
        // When acceptance criteria is not set
        // When item is not yet seen
        int expectedSize = 0;
        int expectedSeenSize = 0;

        assertNull(queue.peek());
        assertNull(queue.poll());
        assertFalse(queue.iterator().hasNext());
        assertFalse(queue.seenIterator().hasNext());

        for (int n = -100; n < 100; n++) {
            assertEquals(expectedSize, queue.size());
            assertEquals(expectedSeenSize, queue.seenSize());
            assertFalse(queue.hasSeen(n));
            assertTrue(queue.canAccept(n));
            assertTrue(queue.offer(n));
            expectedSize++;
            expectedSeenSize++;
            assertFalse(queue.canAccept(n));
            assertTrue(queue.hasSeen(n));
            assertFalse(queue.offer(n));
            assertEquals(expectedSeenSize, queue.seenSize());
            assertEquals(expectedSize, queue.size());
        }

        for (int n = -100; n < 100; n++) {
            assertTrue(queue.hasSeen(n));
            assertEquals(n, queue.peek());
            assertEquals(expectedSize, queue.size());
            assertEquals(expectedSeenSize, queue.seenSize());

            assertEquals(n, queue.poll());
            expectedSize--;
            assertEquals(expectedSize, queue.size());
            assertEquals(expectedSeenSize, queue.seenSize());

            assertTrue(queue.hasSeen(n));
            assertFalse(queue.offer(n));
            assertEquals(expectedSize, queue.size());
            assertEquals(expectedSeenSize, queue.seenSize());
        }

        assertNull(queue.peek());
        assertNull(queue.poll());
        assertFalse(queue.iterator().hasNext());
        assertTrue(queue.seenIterator().hasNext());

        // When acceptance criteria is set
        queue = new SearchQueue<>(EVENS_ONLY);
        expectedSize = 0;
        expectedSeenSize = 0;

        assertNull(queue.peek());
        assertNull(queue.poll());
        assertFalse(queue.iterator().hasNext());
        assertFalse(queue.seenIterator().hasNext());

        for (int n = -100; n < 100; n++) {
            assertEquals(expectedSize, queue.size());
            assertEquals(expectedSeenSize, queue.seenSize());
            assertFalse(queue.hasSeen(n));
            if (n % 2 == 0) {
                assertTrue(queue.canAccept(n));
                assertTrue(queue.offer(n));
                expectedSize++;
                expectedSeenSize++;
                assertTrue(queue.hasSeen(n));
                assertEquals(expectedSize, queue.size());
                assertEquals(expectedSeenSize, queue.seenSize());
            } else {
                assertFalse(queue.canAccept(n));
                assertFalse(queue.offer(n));
            }
            assertFalse(queue.canAccept(n));
            assertFalse(queue.offer(n));
            assertEquals(expectedSeenSize, queue.seenSize());
            assertEquals(expectedSize, queue.size());
        }

        for (int n = -100; n < 100; n++) {
            if (n % 2 == 0) {
                assertTrue(queue.iterator().hasNext());
                assertTrue(queue.hasSeen(n));
                assertEquals(n, queue.peek());
                assertEquals(expectedSize, queue.size());
                assertEquals(expectedSeenSize, queue.seenSize());
                assertEquals(n, queue.poll());
                expectedSize--;
                assertEquals(expectedSize, queue.size());
                assertEquals(expectedSeenSize, queue.seenSize());
            } else {
                assertFalse(queue.hasSeen(n));
                if (n == 99) {
                    assertNull(queue.peek());
                    assertNull(queue.poll());
                } else {
                    assertFalse(n == queue.peek());
                }
                assertEquals(expectedSize, queue.size());
                assertEquals(expectedSeenSize, queue.seenSize());
            }

            assertFalse(queue.offer(n));
            assertFalse(queue.canAccept(n));
            assertEquals(expectedSize, queue.size());
            assertEquals(expectedSeenSize, queue.seenSize());
        }

        assertNull(queue.peek());
        assertNull(queue.poll());
        assertEquals(expectedSize, queue.size());
        assertEquals(expectedSeenSize, queue.seenSize());
        assertFalse(queue.iterator().hasNext());
        assertTrue(queue.seenIterator().hasNext());
        assertEquals(expectedSize, queue.size());
        assertEquals(expectedSeenSize, queue.seenSize());
    }
}
