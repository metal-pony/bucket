package com.metal_pony.bucket.tetris.uitl.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import com.metal_pony.bucket.tetris.util.structs.Coord;
import com.metal_pony.bucket.tetris.util.structs.Coord.FinalCoord;

@DisplayName("Coord")
class TestCoord {

    @Nested
    @DisplayName("(FinalCoord)")
    class TestFinalCoord {
        @Test
        @DisplayName("cannot be modified once created")
        void testUnmodifiable() {
            FinalCoord f1 = new FinalCoord(0, 0);
            Coord f2 = new FinalCoord(5, 6);

            Exception exception = assertThrows(
                UnsupportedOperationException.class,
                () -> { f1.add(f2); }
            );
            assertEquals(FinalCoord.UNMODIFIABLE_ERR, exception.getMessage());
            exception = assertThrows(
                UnsupportedOperationException.class,
                () -> { f1.add(f2.row(), f2.col()); }
            );
            assertEquals(FinalCoord.UNMODIFIABLE_ERR, exception.getMessage());
            exception = assertThrows(
                UnsupportedOperationException.class,
                () -> { f1.set(f2); }
            );
            assertEquals(FinalCoord.UNMODIFIABLE_ERR, exception.getMessage());
            exception = assertThrows(
                UnsupportedOperationException.class,
                () -> { f1.set(f2.row(), f2.col()); }
            );
            assertEquals(FinalCoord.UNMODIFIABLE_ERR, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("when created with defaults")
    class WhenNew {
        @Test
        @DisplayName("is set to origin (0,0)")
        void isAtOrigin() {
            assertEquals(0, new Coord().row());
            assertEquals(0, new Coord().col());
        }
    }

    @ParameterizedTest(name = "[{index}] A({0},{1}) and B({2},{3}), Equal({4}) SqrDist({5})")
    @CsvFileSource(resources = "/Coords.csv", numLinesToSkip = 1)
    void testEqualityAndDistFromCsv(int ax, int ay, int bx, int by, boolean expectedEquality, int expectedSqrDist) {
        Coord a = new Coord(ax, ay);
        Coord b = new Coord(bx, by);
        if (expectedEquality) {
            assertCoordsEqual(a, b);
        } else {
            assertCoordsNotEqual(a, b);
        }
        assertEquals(expectedSqrDist, a.sqrDist(b));
        assertEquals(expectedSqrDist, b.sqrDist(a));

        // Repeat, this time use set() instead of constructor.
        a.set(bx, by);
        b.set(ax, ay);
        if (expectedEquality) {
            assertCoordsEqual(a, b);
        } else {
            assertCoordsNotEqual(a, b);
        }
        assertEquals(expectedSqrDist, a.sqrDist(b));
        assertEquals(expectedSqrDist, b.sqrDist(a));

        // Repeat, this time with set(other)
        a.set(new Coord(ax, ay));
        b.set(new Coord(bx, by));
        if (expectedEquality) {
            assertCoordsEqual(a, b);
        } else {
            assertCoordsNotEqual(a, b);
        }
        assertEquals(expectedSqrDist, a.sqrDist(b));
        assertEquals(expectedSqrDist, b.sqrDist(a));

        // Repeat, this time with default constructor and add(x, y)

        a.add();
    }

    @Nested
    @DisplayName("when adding another Coord")
    class WhenAdding {
        private void returnsSubject(Coord coord, Coord other) {
            assertTrue(coord.add(other.row(), other.col()) == coord);
            assertTrue(coord.add(other) == coord);
        }

        private void valuesAddAsExpected(Coord coord, Coord other) {
            int expectedRow = coord.row() + other.row();
            int expectedCol = coord.col() + other.col();

            coord.add(other.row(), other.col());

            assertEquals(expectedRow, coord.row());
            assertEquals(expectedCol, coord.col());
        }

        private void doesNotModifyOther(Coord coord, Coord other) {
            int expectedOtherRow = other.row();
            int expectedOtherCol = other.col();

            coord.add(other.row(), other.col());

            assertEquals(expectedOtherRow, other.row());
            assertEquals(expectedOtherCol, other.col());
        }

        @ParameterizedTest(name = "[{index}] A({0},{1}) and B({2},{3}) adds to the expected coordinates")
        @CsvFileSource(resources = "/Coords.csv", numLinesToSkip = 1)
        void assertCoordAddition(int ax, int ay, int bx, int by) {
            valuesAddAsExpected(new Coord(ax, ay), new Coord(bx, by));
            returnsSubject(new Coord(ax, ay), new Coord(bx, by));
            doesNotModifyOther(new Coord(ax, ay), new Coord(bx, by));
        }
    }

    private void assertCoordsEqual(Coord a, Coord b) {
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.hashCode() == b.hashCode());
        assertEquals(a.row(), b.row());
        assertEquals(a.col(), b.col());
    }

    private void assertCoordsNotEqual(Coord a, Coord b) {
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertFalse(a.hashCode() == b.hashCode()); // Not guaranteed
        assertFalse((a.row() == b.row()) && (a.col() == b.col()));
    }
}
