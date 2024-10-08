package com.metal_pony.bucket.util;

import java.util.Comparator;

/**
 * Data structure wrapping a data of a variable type and a comparator for it.
 * Implements <code>Comparable</code> by deferring to the given comparator's <code>compare</code> method,
 * allowing this structure to be used as data within a <code>PriorityQueue</code>.
 */
public record PQEntry<S>(S data, Comparator<S> comparator) implements Comparable<PQEntry<S>> {
	@Override public int compareTo(PQEntry<S> o) {
		return comparator.compare(data, o.data);
	}
}
