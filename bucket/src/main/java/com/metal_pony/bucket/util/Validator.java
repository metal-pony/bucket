package com.metal_pony.bucket.util;

public final class Validator<T extends Comparable<T>> {
	public static final class ValidatorException extends RuntimeException {
		public ValidatorException(String message) {
			super(message);
		}
	}

	protected final T min, max;
	protected final String name;

	public Validator(T min, T max, String name) {
		if (min.compareTo(max) > 0) {
			T temp = min;
			min = max;
			max = temp;
		}

		this.min = min;
		this.max = max;
		this.name = name;
	}

	public void validate(T value) {
		if (value.compareTo(min) < 0) {
			throw new ValidatorException(String.format("%s cannot be less than %s", toString(), min));
		} else if (value.compareTo(max) > 0) {
			throw new ValidatorException(String.format("%s cannot be greater than %s", toString(), max));
		}
	}

	public T confine(T value) {
		if (value.compareTo(min) < 0) {
			return min;
		} else if (value.compareTo(max) > 0) {
			return max;
		} else {
			return value;
		}
	}

	public String toString() {
		return name;
	}
}
