package com.sparklicorn.bucket.tetris.uitl.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sparklicorn.bucket.tetris.util.structs.Move;

@DisplayName("Move")
public class TestMove {

    // TODO FinalMove


    @Test
    @DisplayName("default constructor has offsets and rotation of 0")
    void defaultConstructor() {

    }

    private void assertMoveEquality(Move m1, Move m2) {
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
