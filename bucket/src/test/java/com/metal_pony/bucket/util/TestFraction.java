package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.metal_pony.bucket.util.Fraction;

public class TestFraction {

    private Fraction fraction;

    @BeforeEach
    public void beforeEach() {
        fraction = new Fraction();
    }

    @Test
    public void testFraction_defaultConstructor() {
        System.out.println("Testing default values are properly set.");
        assertFalse(fraction.isAutoReducing());
        assertEquals(0, fraction.numerator(), "Default numerator should be 0.");
        assertEquals(1, fraction.denominator(), "Default denominator should be 1.");
    }

    @Test
    public void testReduceSign() {
        assertEquals(new Fraction(0, 1), new Fraction(0, 1).reduceSign());
        assertEquals(new Fraction(0, 1), new Fraction(0, -1).reduceSign());
        assertEquals(new Fraction(0, 1), new Fraction(0, 4).reduceSign());
        assertEquals(new Fraction(0, 1), new Fraction(0, -4).reduceSign());
        assertEquals(new Fraction(-1, 1), new Fraction(-1, 1).reduceSign());
        assertEquals(new Fraction(1, 1), new Fraction(-1, -1).reduceSign());
        assertEquals(new Fraction(-1, 1), new Fraction(1, -1).reduceSign());
        assertEquals(new Fraction(-3, 12), new Fraction(-3, 12).reduceSign());
        assertEquals(new Fraction(3, 12), new Fraction(-3, -12).reduceSign());
        assertEquals(new Fraction(12, 12), new Fraction(-12, -12).reduceSign());
        assertEquals(new Fraction(-4, 12), new Fraction(4, -12).reduceSign());
    }

    @Test
    public void testReduce() {
        assertEquals(new Fraction(0, 1), new Fraction(0, 1).reduce());
        assertEquals(new Fraction(0, 1), new Fraction(0, -1).reduce());
        assertEquals(new Fraction(0, 1), new Fraction(0, 4).reduce());
        assertEquals(new Fraction(0, 1), new Fraction(0, -4).reduce());
        assertEquals(new Fraction(-1, 1), new Fraction(-1, 1).reduce());
        assertEquals(new Fraction(1, 1), new Fraction(-1, -1).reduce());
        assertEquals(new Fraction(-1, 1), new Fraction(1, -1).reduce());
        assertEquals(new Fraction(-1, 4), new Fraction(-3, 12).reduce());
        assertEquals(new Fraction(1, 4), new Fraction(-3, -12).reduce());
        assertEquals(new Fraction(1, 1), new Fraction(-12, -12).reduce());
        assertEquals(new Fraction(-1, 3), new Fraction(4, -12).reduce());
        assertEquals(new Fraction(2, 3), new Fraction(20020, 30030).reduce());
    }

    // @Test
    public void testGettersAndSetters() {
        fail("nyi");
    }

    @Test
    public void testAdd() {
        testAdd(new Fraction(0, 1), new Fraction(0, 1), new Fraction(0, 1));
        testAdd(new Fraction(0, 1), new Fraction(1, 1), new Fraction(1, 1));
        testAdd(new Fraction(0, 1), new Fraction(-1, 1), new Fraction(-1, 1));
        testAdd(new Fraction(1, 1), new Fraction(0, 1), new Fraction(1, 1));
        testAdd(new Fraction(-1, 1), new Fraction(0, 1), new Fraction(-1, 1));
        testAdd(new Fraction(1, 2), new Fraction(1, 2), new Fraction(1, 1));
        testAdd(new Fraction(-1, 2), new Fraction(1, 2), new Fraction(0, 1));
        testAdd(new Fraction(1, 2), new Fraction(1, 3), new Fraction(5, 6));
        testAdd(new Fraction(1, 10), new Fraction(1, 100), new Fraction(11, 100));
        testAdd(new Fraction(48, 517), new Fraction(-431, 11), new Fraction(-222299, 5687));
    }

    private void testAdd(Fraction a, Fraction b, Fraction expected) {
        assertEquals(expected,  a.add(b), String.format("Expected %s + %s to equal %s.", a, b, expected));
    }

    // @Test
    public void testSub() {
        fail("nyi");
    }

    // @Test
    public void testMult() {
        fail("nyi");
    }

    // @Test
    public void testDiv() {
        fail("nyi");
    }
}
