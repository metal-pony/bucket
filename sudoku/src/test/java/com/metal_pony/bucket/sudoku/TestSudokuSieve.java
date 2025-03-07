package com.metal_pony.bucket.sudoku;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.metal_pony.bucket.sudoku.Sudoku;
import com.metal_pony.bucket.sudoku.SudokuSieve;
import com.metal_pony.bucket.util.Counting;

public class TestSudokuSieve {

    private final String configFixtureStr = "218574639573896124469123578721459386354681792986237415147962853695318247832745961";
    private Sudoku configFixture;
    private SudokuSieve sieve;

    @BeforeEach
    void before() {
        configFixture = new Sudoku(configFixtureStr);
        sieve = new SudokuSieve(configFixture.getBoard());
    }

    @Test
    void testRemoveOverlapping_thenAddItemsBack() {
        final int EXPECTED_SIEVE_SIZE = 56;
        final int EXPECTED_REMOVED_ITEMS_SIZE = 8;

        populateSieveForAllDigitCombos(2);
        assertEquals(sieve.size(), EXPECTED_SIEVE_SIZE);
        // System.out.println(sieve.toString());
        List<BigInteger> removed = sieve.removeOverlapping(0);
        // System.out.println("Removed:");
        // System.out.println(configFixtureStr);
        // removed.forEach(r -> {
        //     System.out.println(configFixture.filter(r).toString());
        // });
        assertEquals(removed.size(), EXPECTED_REMOVED_ITEMS_SIZE);
        assertEquals(sieve.size(), EXPECTED_SIEVE_SIZE - EXPECTED_REMOVED_ITEMS_SIZE);

        // Attempt to add the items back
        removed.forEach(item -> sieve.add(item));
        removed.clear();
        assertEquals(removed.size(), 0);
        assertEquals(sieve.size(), EXPECTED_SIEVE_SIZE);
    }

    @Test
    void testIsDerivative() {
        // Always true
        assertTrue(sieve.isDerivative(BigInteger.ZERO));

        // Returns true if the item is a derivative
        BigInteger item = new BigInteger("306954992322430055219200");
        sieve.rawAdd(item);
        for (int t = 0; t < 100; t++) {
            BigInteger clearlyDerivative = item.or(Counting.random(item, new Random()));
            assertTrue(sieve.isDerivative(clearlyDerivative));
        }
    }

    @Test
    void testIsDerivate_whenAddingDuplicate_returnsTrue() {
        BigInteger[] expectedSieveItems = new BigInteger[] {
            new BigInteger("306954992322430055219200"),
            new BigInteger("9288709664931840"),
            new BigInteger("3151872"),
            new BigInteger("1511157274518286468383040"),
            new BigInteger("1649267453952"),
            new BigInteger("9165803807047680"),
            new BigInteger("343597386240"),
            new BigInteger("11258999152312320"),
            new BigInteger("71442432"),
            new BigInteger("3458764556770410496"),
            new BigInteger("18711025025408"),
            new BigInteger("906694367033157887197184"),
            new BigInteger("85002776835641929891840"),
            new BigInteger("5387583584"),
            new BigInteger("2286993044144131"),
            new BigInteger("2375090357084813787136"),
            new BigInteger("13194173645824"),
            new BigInteger("188894661003635668811800"),
            new BigInteger("456003513502100115984648"),
            new BigInteger("332635868477652992257"),
            new BigInteger("1770887431076134011552"),
            new BigInteger("1285683059475306671767552"),
            new BigInteger("6917529027678962706"),
            new BigInteger("623465938509745721704448"),
            new BigInteger("46116860185079194641"),
            new BigInteger("90715224165318656"),
            new BigInteger("313905469185801060352"),
            new BigInteger("56751409310457911050240"),
            new BigInteger("642247033199690103390208"),
            new BigInteger("23833193662228564279302"),
            new BigInteger("312561793925627461783552"),
            new BigInteger("442722210901249822980"),
            new BigInteger("81609584732805024252452"),
            new BigInteger("681238871132469039473185"),
            new BigInteger("158348076961214400921612"),
            new BigInteger("42663861689921679983636"),
            new BigInteger("14906266688228606427268"),
            new BigInteger("1213815652731362392213572"),
            new BigInteger("609371475647174927261701"),
            new BigInteger("379265121277624569565984"),
            new BigInteger("321492179682895215071490"),
            new BigInteger("340319388946031560365328"),
            new BigInteger("95704059171078599213602"),
            new BigInteger("230215483705507665445416"),
            new BigInteger("114531268434214944507440"),
            new BigInteger("172442542110778310950922"),
            new BigInteger("29000731837792516456578"),
            new BigInteger("1227910117880926302242882"),
            new BigInteger("163512156372221582688392"),
            new BigInteger("1362421542415355368474696"),
            new BigInteger("757977365331167903522825"),
            new BigInteger("47827941100928861750416"),
            new BigInteger("1246737327144062647536720"),
            new BigInteger("1218979732142369573980352"),
            new BigInteger("614535555058182109028481"),
            new BigInteger("1813444941101315894814785")
        };

        populateSieveForAllDigitCombos(2);

        for (BigInteger dupe : expectedSieveItems) {
            assertTrue(sieve.isDerivative(dupe));
        }
    }

    private void populateSieveForAllDigitCombos(int level) {
        for (int r = Sudoku.DIGIT_COMBOS_MAP[level].length - 1; r >= 0; r--) {
            BigInteger pMask = configFixture.maskForDigits(Sudoku.DIGIT_COMBOS_MAP[level][r]);
            sieve.addFromFilter(pMask);
        }
    }

    @Nested
    class Static {

        @Test
        void testMaskSieve() {
            assertEquals(
                SudokuSieve.maskString(BigInteger.ONE),
                "0".repeat(80) + "1"
            );
            assertEquals(
                SudokuSieve.maskString(new BigInteger("511")),
                "0".repeat(72) + "1".repeat(9)
            );
            assertEquals(
                SudokuSieve.maskString(new BigInteger("2413129272746388704198656")),
                "1".repeat(9) + "0".repeat(72)
            );
            assertEquals(
                SudokuSieve.maskString(BigInteger.ZERO.setBit(80)),
                "1" + "0".repeat(80)
            );
        }
    }
}
