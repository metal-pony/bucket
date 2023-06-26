package com.sparklicorn.bucket.tetris.uitl.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.tetris.util.structs.Move;

@DisplayName("Move")
public class TestMove {
	final static Move[] presets = new Move[] {
		Move.STAND,
		Move.UP,
		Move.DOWN,
		Move.LEFT,
		Move.RIGHT,
		Move.CLOCKWISE,
		Move.COUNTERCLOCKWISE
	};

	final int row = 47;
	final int col = 921;
	final int rotation = -24;
	Coord offset;
	Move subject;

	final int otherRow = 82;
	final int otherCol = -1243;
	final int otherRotation = 123;
	Coord otherOffset;
	Move other;

	@BeforeEach
	void beach() {
		offset = new Coord(row, col);
		subject = new Move(offset, rotation);

		otherOffset = new Coord(otherRow, otherCol);
		other = new Move(otherOffset, otherRotation);
	}

	@Test
	@DisplayName("default constructor has 0 offset and rotation")
	void defaultConstructor() {
		Move m = new Move();
		assertMoveValues(m, 0, 0, 0);
		assertMoveEquality(m, Move.STAND);
	}

	@Test
	@DisplayName("Move(Coord, rotation) copies Coord and rotation values")
	void move_givenCoordAndRotation_copiesCoordOffsetAndRotationValues() {
		assertMoveValues(subject, row, col, rotation);
	}

	@Test
	@DisplayName("")
	void move_givenOther_makesCopyOfOffsetAndRotationValues() {
		subject = new Move(other);
		assertMoveValues(subject, otherRow, otherCol, otherRotation);
		assertMoveEquality(subject, other);
		assertFalse(subject == other);
	}

	@Test
	@DisplayName("static preset Moves do not support modifications")
	void presetMovesCannotBeModified() {
		Class<UnsupportedOperationException> eClass = UnsupportedOperationException.class;
		for (Move m : presets) {
			assertThrows(eClass, () -> m.rotate(Move.CLOCKWISE));
			assertThrows(eClass, () -> m.add(Move.CLOCKWISE));
			assertThrows(eClass, () -> m.add(new Coord(), 0));
			assertThrows(eClass, () -> m.rotateClockwise());
			assertThrows(eClass, () -> m.rotateCounterClockwise());
		}
	}

	@Nested
	@DisplayName("addition")
	class TestMoveAddition {
		@Test
		@DisplayName("add(Move) adds to offset and rotation correctly")
		void addMove_addsToOffsetAndRotationCorrectly() {
			assertTrue(subject == subject.add(other));
			assertMoveValues(subject, row + otherRow, col + otherCol, rotation + otherRotation);
		}

		@Test
		@DisplayName("add(Move) does not modify the given Move")
		void addMove_doesNotModOther() {
			assertTrue(subject == subject.add(other));
			assertMoveValues(other, otherRow, otherCol, otherRotation);
		}

		@Test
		@DisplayName("add(Coord) adds to offset and rotation correctly")
		void addCoord_addsToOffsetAndRotationCorrectly() {
			assertTrue(subject == subject.add(otherOffset, otherRotation));
			assertMoveValues(subject, row + otherRow, col + otherCol, rotation + otherRotation);
		}

		@Test
		@DisplayName("add(Coord) does not modify the given Coord")
		void addCoord_doesNotModCoord() {
			assertTrue(subject == subject.add(otherOffset, otherRotation));
			assertMoveValues(other, otherRow, otherCol, otherRotation);
		}
	}

	@Nested
	@DisplayName("rotation")
	class TestMoveRotation {
		@Test
		@DisplayName("rotateClockwise() decrements rotation value")
		void rotateClockwise_decrementsRotation() {
			for (int n = 1; n < 10; n++) {
				assertTrue(subject == subject.rotateClockwise());
				assertEquals(rotation - n, subject.rotation());
			}
		}

		@Test
		@DisplayName("rotateCounterClockwise() increments rotation value")
		void rotateCounterClockwise_incrementsRotation() {
			for (int n = 1; n < 10; n++) {
				assertTrue(subject == subject.rotateCounterClockwise());
				assertEquals(rotation + n, subject.rotation());
			}
		}

		@Test
		@DisplayName("rotate(Move) adds rotation values correctly")
		void rotateMove_incrementsOrDecrementsRotationCorrectly() {
			for (int n = 1; n < 10; n++) {
				assertTrue(subject == subject.rotate(other));
				assertEquals(rotation + (n * otherRotation), subject.rotation());
				// Also, does not modify Other
				assertMoveValues(other, otherRow, otherCol, otherRotation);
			}
		}
	}

	@Nested
	@DisplayName("equals")
	class TestEquality {
		@Test
		@DisplayName("when other is the same object instance as this, returns true")
		void equals_whenOtherIsThis_returnsTrue() {
			assertTrue(subject.equals(subject));
		}

		@Test
		@DisplayName("when other is null, returns false")
		void equals_whenOtherIsNull_returnsFalse() {
			assertFalse(subject.equals(null));
		}

		@Test
		@DisplayName("when other is not an instance of Move, returns false")
		void equals_whenOtherIsNotAMove_returnsFalse() {
			assertFalse(subject.equals(new Object()));
		}

		@Test
		@DisplayName("when other has different offset values, returns false")
		void equals_whenOtherHasDifferentOffset_returnsFalse() {
			assertFalse(subject.equals(new Move(subject).add(new Coord(1, 1), 0)));
		}

		@Test
		@DisplayName("when other has different rotation value, returns false")
		void equals_whenOtherHasDifferentRotation_returnsFalse() {
			assertFalse(subject.equals(new Move(subject).rotateCounterClockwise()));
		}

		@Test
		@DisplayName("when other has the same offset and rotation values, returns true")
		void equals_whenOtherHasSameOffsetsAndRotation_returnsTrue() {
			assertTrue(subject.equals(new Move(subject)));
		}
	}

	private void assertMoveEquality(Move m1, Move m2) {
		assertTrue(m1.equals(m2));
		assertTrue(m2.equals(m1));
		assertEquals(m1.hashCode(), m2.hashCode());
	}

	private void assertMoveValues(Move move, int row, int col, int rotation) {
		assertEquals(row, move.offset().row());
		assertEquals(row, move.rowOffset());
		assertEquals(col, move.offset().col());
		assertEquals(col, move.colOffset());
		assertEquals(rotation, move.rotation());
	}
}
