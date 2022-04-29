package com.sparklicorn.bucket.games.sudoku.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadLocalRandom;

import com.sparklicorn.bucket.games.sudoku.puzzles.GeneratedPuzzles;

import org.junit.jupiter.api.Test;

public class TestBoard {

	@Test
	public void testGetCompressedString() {
		assertEquals("-", new Board().getCompressedString());

		assertEquals(
			"59a1b218e5c6.4.97f3.48.29.6b5.7a8e32a93.14f2.7a8",
			new Board("59..1...218......5....6.4.97.......3.48.29.6...5.7..8......32..93.14.......2.7..8").getCompressedString()
		);

		assertEquals(
			"Zr2g",
			new Board("........................................................................2........").getCompressedString()
		);
	}


	private static final String[] VALID_CONFIGS = new String[] {
		"793458261218963754456271893634712589185649327927385146541836972872194635369527418",
		"523419687916837245478562391234678159681945732795321864352796418169284573847153926",
		"542198637197326548836574192653741829481932765729685314215467983368259471974813256",
		"756182349839564721142937856418395267263478915975621438694853172387216594521749683",
		"136548972897126534254397816548671293973482165621953487485769321719234658362815749",
		"178926453493571682526834791781359246964182375235467918357298164849613527612745839",
		"526893714731564928489217635354629871298751346617438259963175482875942163142386597",
		"581349726327651984649827531493582167216473859875196342934215678162738495758964213",
		"264387159359261478781495632416528397978643215523719846895132764632974581147856923",
		"745263819268914357139875642317642598692158734854739126476321985921587463583496271",
		"247583169951267834836149572498375216362914785175826943629738451583491627714652398",
		"824397165537126948961854732713962854258431679649578321375289416186745293492613587",
		"572694318619823457438571926723189645964352871851746239347965182195238764286417593",
		"593817264247563918681924375472639851856142793139785426715498632364271589928356147",
		"562134789739825641148796523381672954497513268625948317813267495254389176976451832",
		"426895731851376492973124865194568273768231549235749186517983624342617958689452317",
		"397126854684579231215384697759832416826451973143697528562743189971268345438915762",
		"271643895638592714495178263162785349347219586589436127956321478713864952824957631",
		"872314695351269784496875123134957862589621437627438951243796518918543276765182349",
		"234198675817456932695723481486971253321564798759832164973215846168347529542689317",
		"365194872978236145412857639857623491196485723234719586743562918689371254521948367",
		"315692874268174593794385612527419386186237459943856127452961738879543261631728945",
		"296173485413685279587429361871354926325968714964217853642891537738546192159732648",
		"657149328924853167183762954471936285835217496269584713592478631716395842348621579",
		"819732564275946138634158927521673849397481652486295713742569381963814275158327496"
	};

	private static final String[] INVALID_CONFIGS = new String[] {
		"793452616463363797356278899144756149885659587827265142142858923877194635219323411",
		"551419636918831271778567394231657319647245832797385865822496412189284573644953926",
		"542198675767346745816677192653793834482931768329685314215427983398259511944812256",
		"155772339851284897542931856418393267463876911975321432194863675487226594521746986",
		"136848952599196234254393896578377293577482135621253487485772461715234618669819416",
		"198916451493571372526815793881839246264182334245467958517297264839613527659743867",
		"627894774731562928489313635257612871698651346611438929953155482875944773152386492",
		"589349849324851984649827537494589147216673859715116347935215678122238165726263673",
		"664388159359261468781595722456588397977643212493719742595138764632924581147816323",
		"845963116228654399239865646315231792593156789874739187576424285921857473183426471",
		"287533141935267834836849396498375292462994771675825945669738156514481627711252318",
		"824392865537121748661254739512968874852431649954248335371589456116787299692713637",
		"512894318619825457438571726723967761964358383151746789347965829195638964242412523",
		"523817265247593916681954375472619891856341793138785426715448632369271499328458267",
		"465131119739825245149796563327677954887513428625948217863667491853349882926451732",
		"426199231851774512677124895111586973168839545235769882253963474342637958689362447",
		"397116854389479338215124897739832415826451976553797521568763189971262245684436642",
		"132664833618512717495178463162783947454293549585436125941226878913892983627957675",
		"872387699331229785436872613234957166389671438627418951242958515214943576467185549",
		"634198677865792931695741343486575217121584796599932324873215846168823525442689317",
		"368144772985216998412857699817623421292455733634371666783552914689375254511948387",
		"395862857868974593994335612227419382189237417923855147452161765877549261631438646",
		"296573416453635279554427861861315924925968781116217843817899457738536634939722248",
		"385112328924873167186862959471965243465219453279684713592778638726395741548691354",
		"819784564975946128118153927541734849397281652486225714635369336967734286158327295",
	};

	@Test
	public void testGetNumEmptySpaces() {
		Board b = new Board();

		assertEquals(81, b.getNumEmptySpaces());

		b = new Board("59..1...218......5....6.4.97.......3.48.29.6...5.7..8......32..93.14.......2.7..8");
		assertEquals(52, b.getNumEmptySpaces());

		b = new Board("11111111111111111111111111111111111111111111111111111111111111111111111111111111.");
		assertEquals(1, b.getNumEmptySpaces());

		b = new Board(".....1111111111111111111111111111111111111111111111111111111111111111111111111111");
		assertEquals(5, b.getNumEmptySpaces());

		b = new Board(".................................................................................");
		assertEquals(81, b.getNumEmptySpaces());
	}

	@Test
	public void testGetNumClues() {
		Board b = new Board();

		assertEquals(0, b.getNumClues());

		b = new Board("59..1...218......5....6.4.97.......3.48.29.6...5.7..8......32..93.14.......2.7..8");
		assertEquals(29, b.getNumClues());

		b = new Board("11111111111111111111111111111111111111111111111111111111111111111111111111111111.");
		assertEquals(80, b.getNumClues());

		b = new Board(".....1111111111111111111111111111111111111111111111111111111111111111111111111111");
		assertEquals(76, b.getNumClues());

		b = new Board(".................................................................................");
		assertEquals(0, b.getNumClues());
	}

	@Test
	public void testClearBoard() {
		Board b = new Board();
		b.clear();
		assertArrayEquals(new int[81], b.getValues(new int[81]));

		b = new Board("59..1...218......5....6.4.97.......3.48.29.6...5.7..8......32..93.14.......2.7..8");
		b.clear();
		assertArrayEquals(new int[81], b.getValues(new int[81]));

		b = new Board("11111111111111111111111111111111111111111111111111111111111111111111111111111111.");
		b.clear();
		assertArrayEquals(new int[81], b.getValues(new int[81]));

		b = new Board(".....1111111111111111111111111111111111111111111111111111111111111111111111111111");
		b.clear();
		assertArrayEquals(new int[81], b.getValues(new int[81]));

		b = new Board(".................................................................................");
		b.clear();
		assertArrayEquals(new int[81], b.getValues(new int[81]));

		b = new Board(new int[81]);
		b.clear();
		assertArrayEquals(new int[81], b.getValues(new int[81]));
	}

	@Test
	public void testGetValues() {
		Board b = new Board();
		assertArrayEquals(new int[81], b.getValues(new int[Board.NUM_CELLS]));

		b = new Board("59..1...218......5....6.4.97.......3.48.29.6...5.7..8......32..93.14.......2.7..8");
		assertArrayEquals(new int[] {
			5, 9, 0, 0, 1, 0, 0, 0, 2, 1, 8, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 6, 0, 4, 0, 9, 7,
			0, 0, 0, 0, 0, 0, 0, 3, 0, 4, 8, 0, 2, 9, 0, 6, 0, 0, 0, 5, 0, 7, 0, 0, 8, 0, 0, 0,
			0, 0, 0, 3, 2, 0, 0, 9, 3, 0, 1, 4, 0, 0, 0, 0, 0, 0, 0, 2, 0, 7, 0, 0, 8
		}, b.getValues(new int[Board.NUM_CELLS]));

		b = new Board("11111111111111111111111111111111111111111111111111111111111111111111111111111111.");
		assertArrayEquals(new int[] {
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0
		}, b.getValues(new int[Board.NUM_CELLS]));

		b = new Board(".....1111111111111111111111111111111111111111111111111111111111111111111111111111");
		assertArrayEquals(new int[] {
			0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
		}, b.getValues(new int[Board.NUM_CELLS]));

		b = new Board(".................................................................................");
		assertArrayEquals(new int[81], b.getValues(new int[Board.NUM_CELLS]));

		b = new Board(new int[Board.NUM_CELLS]);
		assertArrayEquals(new int[Board.NUM_CELLS], b.getValues(new int[81]));
	}

	@Test
	public void testGetValueAt() {
		Board b = new Board();
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(0, b.getValueAt(i));
		}

		String bStr = "590010002180000005000060409700000003048029060005070080000003200930140000000207008";
		b = new Board(bStr);
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(bStr.charAt(i) - '0', b.getValueAt(i));
		}

		bStr = "111111111111111111111111111111111111111111111111111111111111111111111111111111110";
		b = new Board(bStr);
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(bStr.charAt(i) - '0', b.getValueAt(i));
		}

		bStr = "000001111111111111111111111111111111111111111111111111111111111111111111111111111";
		b = new Board(bStr);
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(bStr.charAt(i) - '0', b.getValueAt(i));
		}

		bStr = "000000000000000000000000000000000000000000000000000000000000000000000000000000000";
		b = new Board(bStr);
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(bStr.charAt(i) - '0', b.getValueAt(i));
		}

		b = new Board(new int[Board.NUM_CELLS]);
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(0, b.getValueAt(i));
		}
	}

	@Test
	public void testSetValueAt() {
		Board b = new Board();
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			for (int v = 0; v <= 9; v++) {
				b.setValueAt(i, v);
				assertEquals(v, b.getValueAt(i));
			}
		}
	}

	@Test
	public void testEquals() {
		//Test comparison with empty board.
		Board emptyBoard = new Board();

		//Null reference.
		assertEquals(false, emptyBoard.equals(null));

		//Reflexive property.
		assertEquals(true, emptyBoard.equals(emptyBoard));

		//Symmetric property.
		Board x = new Board("793458261218963754456271893634712589185649327927385146541836972872194635369527418");
		Board y = new Board("793458261218963754456271893634712589185649327927385146541836972872194635369527418");
		Board z = new Board("523419687916837245478562391234678159681945732795321864352796418169284573847153926");
		assertEquals(true, x.equals(y));
		assertEquals(true, y.equals(x));
		assertEquals(false, x.equals(z));
		assertEquals(false, z.equals(x));
		assertEquals(false, y.equals(z));
		assertEquals(false, z.equals(y));

		//Transitive property.
		z = new Board("793458261218963754456271893634712589185649327927385146541836972872194635369527418");
		assertEquals(true, x.equals(y));
		assertEquals(true, y.equals(z));
		assertEquals(true, x.equals(z));

		z = new Board("523419687916837245478562391234678159681945732795321864352796418169284573847153926");
		//Consitent property.
		for (int i = 0; i < 1000; i++) {
			assertEquals(true, x.equals(y));
			assertEquals(false, x.equals(z));
		}

		//Reference equality.
		assertEquals(false, x == y);
		assertEquals(false, x == z);
		assertEquals(false, z == y);
		y = x;
		assertEquals(true, x == y);

		//Test equality of several random boards.
		Board b = new Board();
		for (int i = 0; i < VALID_CONFIGS.length; i++) {
			b = new Board(VALID_CONFIGS[i]);
			for (int j = i + 1; j < VALID_CONFIGS.length; j++) {
				assertEquals(false, b.equals(new Board(VALID_CONFIGS[j])));
			}
		}
	}

	@Test
	public void testCopy() {
		//Test empty board copy.
		Board b = new Board();
		Board b2 = b.copy();
		//b and b2 should be separate Board instances.
		assertEquals(false, b == b2);
		//But they should still be equal.
		assertEquals(true, b.equals(b2));
		assertEquals(true, b2.equals(b));
		//Each value on the boards should be the same.
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(true, b.getValueAt(i) == b2.getValueAt(i));
			assertEquals(true, b.getMaskAt(i) == b2.getMaskAt(i));
		}

		String bStr = "590010002180000005000060409700000003048029060005070080000003200930140000000207008";
		b = new Board(bStr);
		b2 = b.copy();
		//b and b2 should be separate Board instances.
		assertEquals(false, b == b2);
		//But they should still be equal.
		assertEquals(true, b.equals(b2));
		assertEquals(true, b2.equals(b));
		//Each value on the boards should be the same.
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(true, b.getValueAt(i) == b2.getValueAt(i));
			assertEquals(true, b.getMaskAt(i) == b2.getMaskAt(i));
		}

		bStr = "111111111111111111111111111111111111111111111111111111111111111111111111111111110";
		b = new Board(bStr);
		b2 = b.copy();
		//b and b2 should be separate Board instances.
		assertEquals(false, b == b2);
		//But they should still be equal.
		assertEquals(true, b.equals(b2));
		assertEquals(true, b2.equals(b));
		//Each value on the boards should be the same.
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(true, b.getValueAt(i) == b2.getValueAt(i));
			assertEquals(true, b.getMaskAt(i) == b2.getMaskAt(i));
		}

		bStr = "000001111111111111111111111111111111111111111111111111111111111111111111111111111";
		b = new Board(bStr);
		b2 = b.copy();
		//b and b2 should be separate Board instances.
		assertEquals(false, b == b2);
		//But they should still be equal.
		assertEquals(true, b.equals(b2));
		assertEquals(true, b2.equals(b));
		//Each value on the boards should be the same.
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			assertEquals(true, b.getValueAt(i) == b2.getValueAt(i));
			assertEquals(true, b.getMaskAt(i) == b2.getMaskAt(i));
		}
	}

	@Test
	public void testIsValid() {
		// Empty board (valid)
		Board b = new Board();
		assertTrue(b.isValid());

		// Valid configurations
		for (String c : VALID_CONFIGS) {
			b = new Board(c);
			assertTrue(b.isValid());
		}

		// Invalid configurations
		for (String c : INVALID_CONFIGS) {
			b = new Board(c);
			assertFalse(b.isValid());
		}

		// Pre-generated puzzles (all valid)
		for (int i = 0; i < Math.min(100, GeneratedPuzzles.PUZZLES_24_1000.length); i++) {
			b = new Board(GeneratedPuzzles.PUZZLES_24_1000[i]);
			assertTrue(b.isValid());
		}
	}

	@Test
	public void testIsRowValid() {
		//Validity is determined by whether there are duplicate values.

		//Test empty rows (valid).
		Board b = new Board();
		for (int r = 0; r < 9; r++) {
			assertEquals(true, b.isRowValid(r));
		}

		//Test almost empty rows (valid).
		//Set the first column to all 1s.
		for (int r = 0; r < 9; r++) {
			b.setValueAt(r*9, 1);
			assertEquals(true, b.isRowValid(r));
		}

		//Test complete rows (valid).
		//There are 9! = 362880 permutations of the digits 1-9, but we won't test the all!
		int[][] perms = new int[][] {
			{1, 2, 3, 4, 5, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 8, 9},
			{3, 2, 1, 4, 5, 6, 7, 8, 9},
			{4, 2, 3, 1, 5, 6, 7, 8, 9},
			{5, 2, 3, 4, 1, 6, 7, 8, 9},
			{6, 2, 3, 4, 5, 1, 7, 8, 9},
			{7, 2, 3, 4, 5, 6, 1, 8, 9},
			{8, 2, 3, 4, 5, 6, 7, 1, 9},
			{9, 2, 3, 4, 5, 6, 7, 8, 1},
			{1, 2, 3, 4, 5, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 9, 8},
			{3, 2, 1, 4, 5, 6, 9, 8, 7},
			{4, 2, 3, 1, 5, 9, 7, 8, 6},
			{9, 2, 3, 4, 1, 6, 7, 8, 5},
			{6, 2, 3, 9, 5, 1, 7, 8, 4},
			{7, 2, 9, 4, 5, 6, 1, 8, 3},
			{8, 9, 3, 4, 5, 6, 7, 1, 2},
			{9, 2, 3, 4, 5, 6, 7, 8, 1},
		};

		//Set every row to the same perm array.
		for (int[] p : perms) {
			for (int r = 0; r < 9; r++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt(r*9 + i, p[i]);
				}
				assertEquals(true, b.isRowValid(r));
			}
		}

		//Test valid Sudoku configurations.
		for (String c : VALID_CONFIGS) {
			b = new Board(c);
			for (int r = 0; r < 9; r++) {
				assertEquals(true, b.isRowValid(r));
			}
		}

		//Test invalid configurations.
		for (String c : INVALID_CONFIGS) {
			b = new Board(c);
			for (int r = 0; r < 9; r++) {
				assertEquals(false, b.isRowValid(r), "Row " + r + " in board\n" + b.toString());
			}
		}

		//Test complete rows (invalid).
		//All of these combinations have duplicate values.
		int[][] invalid_combinations = new int[][] {
			{1, 2, 3, 4, 4, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 8, 2},
			{3, 2, 5, 4, 5, 6, 7, 8, 9},
			{4, 2, 7, 1, 7, 6, 7, 8, 9},
			{5, 3, 3, 3, 3, 6, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 8, 8},
			{1, 2, 3, 4, 5, 6, 1, 8, 9},
			{8, 2, 1, 4, 5, 6, 7, 1, 9},
			{9, 2, 3, 8, 5, 6, 7, 8, 1},
			{8, 2, 3, 4, 5, 6, 3, 8, 9},
			{8, 1, 3, 4, 0, 6, 3, 9, 8},
			{8, 2, 1, 4, 0, 6, 3, 8, 7},
			{8, 2, 3, 1, 0, 9, 3, 8, 6},
			{8, 2, 3, 4, 1, 6, 3, 8, 5},
			{8, 2, 3, 9, 5, 1, 3, 8, 4},
			{8, 2, 9, 4, 5, 6, 3, 8, 3},
			{8, 9, 3, 0, 0, 6, 7, 1, 7},
			{8, 2, 3, 4, 5, 6, 7, 8, 0},
		};

		for (int[] p : invalid_combinations) {
			for (int r = 0; r < 9; r++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt(r*9 + i, p[i]);
				}
				assertEquals(false, b.isRowValid(r));
			}
		}
	}

	@Test
	public void testIsColValid() {
		//Test empty rows (valid).
		Board b = new Board();
		for (int c = 0; c < 9; c++) {
			assertEquals(true, b.isColValid(c));
		}

		//Test almost empty columns (valid).
		//Set the top row to all 1s.
		for (int c = 0; c < 9; c++) {
			b.setValueAt(c, 1);
			assertEquals(true, b.isColValid(c));
		}

		//Test complete columns (valid).
		int[][] perms = new int[][] {
			{1, 2, 3, 4, 5, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 8, 9},
			{3, 2, 1, 4, 5, 6, 7, 8, 9},
			{4, 2, 3, 1, 5, 6, 7, 8, 9},
			{5, 2, 3, 4, 1, 6, 7, 8, 9},
			{6, 2, 3, 4, 5, 1, 7, 8, 9},
			{7, 2, 3, 4, 5, 6, 1, 8, 9},
			{8, 2, 3, 4, 5, 6, 7, 1, 9},
			{9, 2, 3, 4, 5, 6, 7, 8, 1},
			{1, 2, 3, 4, 5, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 9, 8},
			{3, 2, 1, 4, 5, 6, 9, 8, 7},
			{4, 2, 3, 1, 5, 9, 7, 8, 6},
			{9, 2, 3, 4, 1, 6, 7, 8, 5},
			{6, 2, 3, 9, 5, 1, 7, 8, 4},
			{7, 2, 9, 4, 5, 6, 1, 8, 3},
			{8, 9, 3, 4, 5, 6, 7, 1, 2},
			{9, 2, 3, 4, 5, 6, 7, 8, 1},
		};

		for (int[] p : perms) {
			for (int c = 0; c < 9; c++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt(c + i*9, p[i]);
				}
				assertEquals(true, b.isColValid(c));
			}
		}

		//Test valid Sudoku configurations.
		for (String c : VALID_CONFIGS) {
			b = new Board(c);
			for (int col = 0; col < 9; col++) {
				assertEquals(true, b.isColValid(col));
			}
		}

		//Test invalid configurations.
		for (String c : INVALID_CONFIGS) {
			b = new Board(c);
			for (int col = 0; col < 9; col++) {
				assertEquals(false, b.isColValid(col));
			}
		}

		//Test complete rows (invalid).
		int[][] invalid_combinations = new int[][] {
			{1, 2, 3, 4, 4, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 8, 2},
			{3, 2, 5, 4, 5, 6, 7, 8, 9},
			{4, 2, 7, 1, 7, 6, 7, 8, 9},
			{5, 3, 3, 3, 3, 6, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 8, 8},
			{1, 2, 3, 4, 5, 6, 1, 8, 9},
			{8, 2, 1, 4, 5, 6, 7, 1, 9},
			{9, 2, 3, 8, 5, 6, 7, 8, 1},
			{8, 2, 3, 4, 5, 6, 3, 8, 9},
			{8, 1, 3, 4, 0, 6, 3, 9, 8},
			{8, 2, 1, 4, 0, 6, 3, 8, 7},
			{8, 2, 3, 1, 0, 9, 3, 8, 6},
			{8, 2, 3, 4, 1, 6, 3, 8, 5},
			{8, 2, 3, 9, 5, 1, 3, 8, 4},
			{8, 2, 9, 4, 5, 6, 3, 8, 3},
			{8, 9, 3, 0, 0, 6, 7, 1, 7},
			{8, 2, 3, 4, 5, 6, 7, 8, 0},
		};

		for (int[] p : invalid_combinations) {
			for (int c = 0; c < 9; c++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt(c + i*9, p[i]);
				}
				assertEquals(false, b.isColValid(c));
			}
		}
	}

	@Test
	public void testIsRegionValid() {
		//Test empty regions (valid).
		Board b = new Board();
		for (int c = 0; c < 9; c++) {
			assertEquals(true, b.isRegionValid(c));
		}

		/* *****************************
		int gr = region / 3;
		int gc = region % 3;
		for (int i = 0; i < 9; i++) {
			int digit = getValueAt(gr*27 + gc*3 + (i/3)*9 + (i%3));
		}
		********************************/

		//Test almost empty regions (valid).
		//Set first value in each region to 1.
		for (int i = 0; i < 9; i++) {
			b.setValueAt((i%3)*3 + (i/3)*27, 1);
			assertEquals(true, b.isRegionValid(i));
		}

		//Test complete regions (valid).
		int[][] perms = new int[][] {
			{1, 2, 3, 4, 5, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 8, 9},
			{3, 2, 1, 4, 5, 6, 7, 8, 9},
			{4, 2, 3, 1, 5, 6, 7, 8, 9},
			{5, 2, 3, 4, 1, 6, 7, 8, 9},
			{6, 2, 3, 4, 5, 1, 7, 8, 9},
			{7, 2, 3, 4, 5, 6, 1, 8, 9},
			{8, 2, 3, 4, 5, 6, 7, 1, 9},
			{9, 2, 3, 4, 5, 6, 7, 8, 1},
			{1, 2, 3, 4, 5, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 9, 8},
			{3, 2, 1, 4, 5, 6, 9, 8, 7},
			{4, 2, 3, 1, 5, 9, 7, 8, 6},
			{9, 2, 3, 4, 1, 6, 7, 8, 5},
			{6, 2, 3, 9, 5, 1, 7, 8, 4},
			{7, 2, 9, 4, 5, 6, 1, 8, 3},
			{8, 9, 3, 4, 5, 6, 7, 1, 2},
			{9, 2, 3, 4, 5, 6, 7, 8, 1},
		};

		for (int[] p : perms) {
			for (int r = 0; r < 9; r++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt((r/3)*27 + (r%3)*3 + (i/3)*9 + (i%3), p[i]);
				}
				assertEquals(true, b.isRegionValid(r));
			}
		}

		//Test each of these valid Sudoku configurations.
		for (String c : VALID_CONFIGS) {
			b = new Board(c);
			for (int r = 0; r < 9; r++) {
				assertEquals(true, b.isRegionValid(r));
			}
		}

		//Test these invalid configurations.
		for (String c : INVALID_CONFIGS) {
			b = new Board(c);
			for (int r = 0; r < 9; r++) {
				assertEquals(false, b.isRegionValid(r));
			}
		}

		//Test complete regions (invalid).
		int[][] invalid_combinations = new int[][] {
			{1, 2, 3, 4, 4, 6, 7, 8, 9},
			{2, 1, 3, 4, 5, 6, 7, 8, 2},
			{3, 2, 5, 4, 5, 6, 7, 8, 9},
			{4, 2, 7, 1, 7, 6, 7, 8, 9},
			{5, 3, 3, 3, 3, 6, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 8, 8},
			{1, 2, 3, 4, 5, 6, 1, 8, 9},
			{8, 2, 1, 4, 5, 6, 7, 1, 9},
			{9, 2, 3, 8, 5, 6, 7, 8, 1},
			{8, 2, 3, 4, 5, 6, 3, 8, 9},
			{8, 1, 3, 4, 0, 6, 3, 9, 8},
			{8, 2, 1, 4, 0, 6, 3, 8, 7},
			{8, 2, 3, 1, 0, 9, 3, 8, 6},
			{8, 2, 3, 4, 1, 6, 3, 8, 5},
			{8, 2, 3, 9, 5, 1, 3, 8, 4},
			{8, 2, 9, 4, 5, 6, 3, 8, 3},
			{8, 9, 3, 0, 0, 6, 7, 1, 7},
			{8, 2, 3, 4, 5, 6, 7, 8, 0},
		};

		for (int[] p : invalid_combinations) {
			for (int r = 0; r < 9; r++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt((r/3)*27 + (r%3)*3 + (i/3)*9 + (i%3), p[i]);
				}
				assertEquals(false, b.isRegionValid(r));
			}
		}
	}

	@Test
	public void testIsFull() {
		//Test empty board fullness.
		Board b = new Board();
		assertEquals(false, b.isFull());

		//Test mostly empty board fullness.
		b.setValueAt(0, 1);
		assertEquals(false, b.isFull());

		//Test mostly full board.
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		for (int i = 0; i < 80; i++) {
			b.setValueAt(i, rand.nextInt(1, 10));
		}
		//The last board value is empty.
		assertEquals(false, b.isFull());

		//Test full board.
		b.setValueAt(80, 1);
		assertEquals(true, b.isFull());
	}

	@Test
	public void testSolved() {

		//Test empty board.
		Board b = new Board();
		assertEquals(false, b.isSolved());

		//Test each of these solved Sudoku configurations.
		for (String c : VALID_CONFIGS) {
			b = new Board(c);
			assertEquals(true, b.isSolved());

			//Remove the first number on the board.
			b.setValueAt(0, 0);
			assertEquals(false, b.isSolved());
		}

		//Test these invalid configurations.
		for (String c : INVALID_CONFIGS) {
			assertEquals(false, (new Board(c)).isSolved());
		}
	}
}
