package com.metal_pony.bucket.util;

public class Fraction {

    public static final String ZERO_DENOMINATOR_ERROR =
        "Fraction denominator cannot be 0.";

    private int n, d;
    private boolean autoReduce;

    public Fraction(int numerator, int denominator, boolean autoReduce) {
        set(numerator, denominator);
        this.autoReduce = autoReduce;
    }

    public Fraction() {
        this(0, 1, false);
    }

    public Fraction(Fraction other) {
        this(other.n, other.d, false);
    }

    public Fraction(int numerator, int denominator) {
        this(numerator, denominator, false);
    }

    public int numerator() {
        return this.n;
    }

    public int denominator() {
        return this.d;
    }

    public Fraction numerator(int newNumerator) {
        return set(newNumerator, d);
    }

    public Fraction denominator(int newDenominator) {
        return set(n, newDenominator);
    }

    public Fraction set(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException(ZERO_DENOMINATOR_ERROR);
        }

        this.n = numerator;
        this.d = denominator;

        if (autoReduce) {
            reduce();
        }

        return this;
    }

    public boolean isAutoReducing() {
        return this.autoReduce;
    }

    public void setAutoReduce(boolean autoReduce) {
        this.autoReduce = autoReduce;
    }

    /**
     * Gets the value of this Fraction as a floating-point number.
     * @return float
     */
    public float floatValue() {
        return (float)n / (float)d;
    }

    /**
     * Gets the value of this Fraction as a double.
     * @return double
     */
    public double doubleValue() {
        return (double)n / (double) d;
    }

    @Override
    public String toString() {
        return String.format("%d/%d", n, d);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Fraction) {
            Fraction _this = new Fraction(this).reduce();
            Fraction _other = new Fraction((Fraction) obj).reduce();
            return _this.n == _other.n && _this.d == _other.d;
            // alternatively...
            // return this.doubleValue() - _other.doubleValue() < (1e-10);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(doubleValue());
    }

    /**
     * If the denominator is negative, flips the signs of the numerator and denominator.
     * @return self
     */
    public Fraction reduceSign() {
        if (d < 0) {
            n = -n;
            d = -d;
        }

        return this;
    }

    /**
     * Flips the sign if the denominator is negative, then attempts to reduce the fraction.
     * @return self
     */
    public Fraction reduce() {
        reduceSign();

        if (d > 1 && n % d == 0) {
            n /= d;
            d = 1;
        }

        if (d > 1) {
            for (long factor : Math.getPrimeFactors(d)) {
                if (n % factor == 0) {
                    n /= factor;
                    d /= factor;
                }
            }
        }

        return this;
    }

    private Fraction add(int otherN, int otherD) {
        n = n*otherD + d*otherN;
        d = d*otherD;

        if (autoReduce) {
            reduce();
        }

        return this;
    }

    /**
     * Adds the other Fraction to this one.
     * @param other Fraction
     * @return self
     */
    public Fraction add(Fraction other) {
        return add(other.n, other.d);
    }

    /**
     * Subtracts the other Fraction from this one.
     * @param other Fraction
     * @return self
     */
    public Fraction sub(Fraction other) {
        return add(-other.n, other.d);
    }

    private Fraction mult(int otherN, int otherD) {
        n *= otherN;
        d *= otherD;

        if (autoReduce) {
            reduce();
        }

        return this;
    }

    /**
     * Multiplies the other Fraction to this one.
     * @param other Fraction
     * @return self
     */
    public Fraction mult(Fraction other) {
        return mult(other.n, other.d);
    }

    /**
     * Divides this Fraction by the given.
     * @param other Fraction
     * @return self
     */
    public Fraction div(Fraction other) {
        return mult(other.d, other.n);
    }
}
