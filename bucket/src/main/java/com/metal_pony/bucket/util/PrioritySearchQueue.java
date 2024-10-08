package com.metal_pony.bucket.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Function;

/**
 * A queue backed by a Heap (ArrayList) that keeps track of all previously-seen elements.
 * The queue optionally takes a function, <code>acceptanceCriteria</code>, used to
 * check if an element can be enqueued. If not specified, the check is waived.
 * New elements are only added if they pass the acceptanceCriteria function
 * and have not yet been seen by the queue.
 */
public class PrioritySearchQueue<T extends Comparable<T>> extends PriorityQueue<T> {
    protected HashSet<T> seen;
    protected Function<T,Boolean> acceptanceCriteria;

    /**
     * Creates a new PrioritySearchQueue, with no acceptance criteria function.
     * All offered items that have not been previously seen will be accepted into the queue.
     */
    public PrioritySearchQueue() {
        this((obj) -> true);
    }

    /**
     * Creates a new PrioritySearchQueue with the given acceptance function.
     * All offerings not previously seen will be accepted into the queue only if the acceptance
     * function allows.
     *
     * @param acceptanceCriteria A function that determines if an offering can be accepted
     * into the queue.
     */
    public PrioritySearchQueue(Function<T,Boolean> acceptanceCriteria) {
        this.seen = new HashSet<>();
        this.acceptanceCriteria = acceptanceCriteria;
    }

    @Override
    public boolean offer(T e) {
        return canAccept(e) && seen.add(e) && super.offer(e);
    }

    /**
     * Determines whether the given element would be accepted into the queue.
     */
    public boolean canAccept(T e) {
        return acceptanceCriteria.apply(e) && !hasSeen(e);
    }

    /**
     * Returns whether the given element has been seen by the queue.
     */
    public boolean hasSeen(T e) {
        return seen.contains(e);
    }

    /**
     * Returns the number of elements the queue has seen in total.
     */
    public int seenSize() {
        return seen.size();
    }

    /**
     * Returns an iterator over ALL the elements in this collection has seen in total.
     * There are no guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a guarantee).
     */
    public Iterator<T> seenIterator() {
        return seen.iterator();
    }
}
