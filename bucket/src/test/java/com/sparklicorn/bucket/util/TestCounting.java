package com.sparklicorn.bucket.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TestCounting {

    @Test
    public void testFactorialsLong_whenInputIsNegative_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Counting.factorial(-1L));
    }

    @Test
    public void testFactorialsLong_whenInputIsTooHigh_throwsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Counting.factorial(Counting.FACTORIAL_MAX_LONG + 1L)
        );
    }

    @Test
    public void testFactorialsLong() {
        int[] expectedFactorials = new int[] {
            1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800
        };

        for (int i = 0; i < expectedFactorials.length; i++) {
            assertEquals((long) expectedFactorials[i], Counting.factorial((long)i));
        }
    }

    @Test
    public void testFactorialsInt() {
        int[] expectedFactorials = new int[] {
            1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800
        };

        for (int i = 0; i < expectedFactorials.length; i++) {
            assertEquals(expectedFactorials[i], Counting.factorial(i));
        }
    }

    @Test
    public void testFactorialsInt_whenInputIsNegative_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Counting.factorial(-1));
    }

    @Test
    public void testFactorialsInt_whenInputIsTooHigh_throwsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Counting.factorial(Counting.FACTORIAL_MAX + 1)
        );
    }

    @Test
    public void testPermsList_whenListIsNull_throwsException() {
        assertThrows(NullPointerException.class, () -> {
            Counting.perms(null);
        });
    }

    @Test
    public void testPermsList_whenListHasOneElement() {
        ArrayList<String> list = new ArrayList<>();
        list.add("meow");

        Set<List<String>> perms = Counting.perms(list);
        assertEquals(1, perms.size());
        assertTrue(perms.contains(list));

        list.add("pow");
        perms = Counting.perms(list);
        assertEquals(2, perms.size());
        assertTrue(perms.contains(list));
    }
}
