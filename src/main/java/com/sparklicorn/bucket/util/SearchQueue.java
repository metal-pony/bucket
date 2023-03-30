package com.sparklicorn.bucket.util;

import java.util.AbstractQueue;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;

/**
 * A queue backed by a LinkedList that keeps track of all previously-seen elements.
 * The queue optionally takes a function, <code>acceptanceCriteria</code>, used to
 * check if an element can be enqueued. If not specified, the check is waived.
 * New elements are only added if they pass the acceptanceCriteria function
 * and have not yet been seen by the queue.
 */
public class SearchQueue<T> extends AbstractQueue<T> {
    protected LinkedList<T> queue;
    protected HashSet<T> seen;
    protected Function<T,Boolean> acceptanceCriteria;

    /**
     * Creates a new SearchQueue, waiving the entry acceptance function.
     * i.e. all offerings not previously seen will be accepted into the queue.
     */
    public SearchQueue() {
        this((obj) -> true);
    }

    /**
     * Creates a new SearchQueue with the given acceptance function.
     * All offerings not previously seen will be accepted into the queue only if the acceptance
     * function allows.
     *
     * @param acceptanceCriteria A function that determines if an offering can be accepted
     * into the queue.
     */
    public SearchQueue(Function<T,Boolean> acceptanceCriteria) {
        this.seen = new HashSet<>();
        this.acceptanceCriteria = acceptanceCriteria;
    }

    @Override
    public boolean offer(T e) {
        return acceptanceCriteria.apply(e) && seen.add(e) && queue.add(e);
    }

    @Override
    public T poll() {
        return queue.removeFirst();
    }

    @Override
    public T peek() {
        return queue.getFirst();
    }

    @Override
    public Iterator<T> iterator() {
        return queue.iterator();
    }

    @Override
    public int size() {
        return queue.size();
    }

    /**
     * Determines whether the given element would be accepted into the queue.
     */
    public boolean canAccept(T e) {
        return acceptanceCriteria.apply(e) && !seen.contains(e);
    }

    /**
     * Returns whether the given element has been seen by the queue.
     */
    public boolean seen(T e) {
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
