package com.sparklicorn.bucket.games.sudoku.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.sparklicorn.bucket.games.sudoku.puzzles.GeneratedPuzzles;

import static com.sparklicorn.bucket.games.sudoku.game.Board.*;

import org.junit.jupiter.api.Test;

public class TestBoard {
	private static final List<String> VALID_CONFIGS = Collections.unmodifiableList(
		Arrays.asList(
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
		)
	);

	/**
	 * Invalid sudoku configurations for which each row, column, and region are invalid.
	 */
	private static final List<String> INVALID_CONFIGS = Collections.unmodifiableList(
		Arrays.asList(
			"1".repeat(NUM_CELLS),
			"2".repeat(NUM_CELLS),
			"3".repeat(NUM_CELLS),
			"4".repeat(NUM_CELLS),
			"5".repeat(NUM_CELLS),
			"6".repeat(NUM_CELLS),
			"7".repeat(NUM_CELLS),
			"8".repeat(NUM_CELLS),
			"9".repeat(NUM_CELLS),
			"351455586472639636685596444317322146969734888138246877644187298477468155571466585",
			"722248217656686717921466294219282954457946868458124444859934696674116475283348331",
			"681245697387396681414429797683283748738518981674921766569524419225283127755345524",
			"974848792437536742117692135152154265124911548961936284259698711216918187264529254",
			"646187537646381419978876461497699371354898413958639487836294583568342837787315386",
			"667869621283393587584914291282569891298724972667123872699833479256627694127291351",
			"672251775262947244541562151116259627642126719422864388814468368888212975445912349",
			"819966355451552283818159249851922255944741955116624393789985495514969461751148797",
			"986977166813744886296423871712648331966459465578824269759343339488234498581995469",
			"784668549626342261921218972755647279245656957614553365929591517645325592611168247",
			"568582678994221597979334468612487964871855297733896517338241742271312256627749845",
			"227671842965886826116595923272555711122566216872972771637173529382254263964359933",
			"571393985167263895814131199562115242115256981582875673876622725765329172213127586",
			"937149625862831182833632816232886312816665295399388972761492328456434266937218574",
			"121242239555362258887768935945789169746745643576312713585542967315897811675461138",
			"867118297447454186857721472993139121529325485643888668264552937559794314215796558",
			"584659235346174698425776356771315865975722526369313728759792168246982623238592299",
			"837781497515822875186635672488518156646144753381145369624144395997683774727823633",
			"496446147763126927868821349911898355411852813337252933899833538215263446744414821",
			"668173796263613722686811447443599445898493461732226344468151217161764885757993478",
			"641835977433975299817111275953375982825534521931176452749753468924757494987393361",
			"613956539849347696742113897548678849183215517632643462716113492613794893957423915",
			"138861497842356531395914929131164634754642171427817189365217252486949679649938531",
			"266186713283759518423446319186247675625131369731376331541297952126582612112613558",
			"397988455958533717373659944981418213688314229676398213979931523332996186595233431",
			"176768573242892269312192626398456838433742531634723876353243414968924318771326181",
			"865375117175899218534513771647252438386322788635848599292216181292999757911266145",
			"888129256591923761866814971759869198198187564455794534194278414363359232185574531",
			"447984172584261195194672812816729166219261354815449381238494458393951146645276528",
			"689889798448192432487259195938684688884182923364997872913736687241867621897599617",
			"413772354232714923579381969825347824877786279162182877592634939862368557221386696",
			"717324156316314339977652755639554943164883895315827517389765332847714279255795559"
		)
	);

	/**
	 * Invalid sudoku boards for which each row, column, and region are invalid.
	 */
	private static final List<String> INVALID_INCOMPLETE_BOARD_STRS = Collections.unmodifiableList(
		Arrays.asList(
			".7.2.2.161.73.91.....11.8.....34...42.2.3.84.2.2.....6.7.9.9.1.9.....3.3.9..3...9",
			"..9.3.83.7.9...7.8...33.......7..33..3.3....73..475.4..3.4.5.3....43.3.73.....66.",
			".1.3.1.4..623.65.432.....3.1.....414..11.....1....1....1.54.5....78..8.2.72.4.21.",
			"...21.2.82..8...8...25.5.5...1..33.688..5.1.68.15...5..4.2..1.1.8.214..44....3.3.",
			"....88...71.6..3.63.3...35...15..8.87.....7..1.5.5....51..8..5....6.8..6.55...6..",
			".3.1..3..11...6..16.7..61..2..12..9.4.6.6..9.43.2.2......3..5.588..6.5.1..7..3.3.",
			"..3.7.43......21.1.3.7...3.8..85.4...5165...1..1....1.65.6.2..3.66......6.6.2.3..",
			".18..8..5......644..85.8......9...19.12.3.1..25..9...51...3.77....566...1....664.",
			"..1..2..226.2.......177.2......7.211266.........22.......5.4.511.9..1.....9.44.1.",
			"...3.411..3....88.3...3....2..4..4..21..1...4.1.734.1..3.7...34..2.7.2.8..2..88..",
			"...6..6.1..74..73..73339.1.8..8.7...8..7..8.....4...44.8..39939..3..3....8....8.4",
			".9..3.9...1....91.3.13.......1..466713.55.......7.4..7...3.6.6.3...3...2.3..772..",
			"....3..3.9..9.43.7.9.8.4.8...1..5.5...11...3.95...5..3.8..77....8.8..6......7.6.7",
			"...7.75..32.1.81..2....825...5...33..5.9..9.8..99...7.12....2...79.7.2..3.1.7..78",
			"....69.6...59..69.1.5413......43..3.353...........3.93.6....6..75...5..87...66..8",
			".5.1...5..7...1..7..59...5...1..4.1....8..8..1.1.54.8.9...33..795.9..7..8...5.8..",
			".243..611.74..17....1..1...7..37...81......1.3.3.3..8..5..5.618..5..7..732..5.13.",
			"...4.84.5.88.....5..3.4.3..3...23.....952..5..89..8.5.3..88..7.7..5....7.3....3..",
			".7...544....5..2427.8.8....8.1.....87.91..9...8.66...9.1.18.....8..8.2.5..1..5.59",
			"..99.36....11.9.5.212...6..3.98....93...8.9.3717.4...2....955....29.3..24.4.4..5.",
			"..8..8.8898...8.88..6.6....7.74..2...7.19.2.99.7..4..75..16...5.5...65...7.....11",
			"..722..........99.4775.....3...53..99.4....9.4..5.4....3..3.9...3.3.4..6....57.76",
			"..5...56..665.....3..9.9.6.....7...7.232..5.79.9.76.......262..3..5.5..3.23.....3",
			".86.6....44....8.8...11....6.....8.86..8..6....4.8..4..8...8......1.82.2776.8..4.",
			".6.6...5.9.....5.9.7996.......36.6....477..5..43..3..5......4.4.75359..19.9..96.1",
			"...8.9..94.4......4...8.55.5..8...81.61.8.69...5..1..116.4...4......56.5..1.1569.",
			".4..9.98.9....8.8..4.49....1.81........8..8....8..8..8.4.4....84..4..9..9.....9..",
			".6.1..1.2.79..1.1..27.2..3.1...1..7.464.....7..2..1..223.1...3.2.5..5..3..9.1.1..",
			"....499...576..3.6..565..3......8.8.93....6229..8.9...2..24..8.13732.6...1.....16",
			"..4.6.65....8.89..4.1..1.9...9...9.8..97..758...77....45..4..2...27.12...59.7...9",
			"...7...449..7..44..89.9.........3.3.988...4.3.9...3..9...494..1..3..3....38....31",
			"...4979....6....969.9.7........4.294.434..2..883.......8...3..8.5..4...495...31.1",
			"....8.8.4.335....4867.5.6......44.7..1..6.67.1...15...22.5..3.38.7.7.....1..757..",
			".67..62...87..8..2....6.22.7...7....6.47..4.268.....6682..58...2..7..5.5.3..5.32.",
			"..6...6..44........4.5.577.....96..6662.....48...9..843...35..63..5..3....2...672",
			".7.9.6..9.....688.3.3........559.....735..67.3...3...6....9118.2...16..6..2.2.8..",
			"...3.3...4..4.7..844....78...8..27.2.6.52.58...8.....88..7...37.6.77...3.8..77...",
			".86.8..6.66.2.1...5.61..6......48.4..8....622..8.8...6...2...27...3.73.6577..7.7.",
			".6..1.17.1136.197.....4.4....6....6..63...9.6...66.....7...67..12..66..12....88.1",
			"...3..34233..........2114.....6.633...62....2.22....3.88..3....326.31.........66.",
			"..4.225.1.55...........1.1......11..6.9.1...96343...9.88...6......31.5.3.3..6.319",
			".4.3...39.64..6.9....99..1...3.66..1...9..9..3....1.1.7...921.9.2.2..1..763...3..",
			"...22.7.....8..8.78.8....5.3..8....3.4.1...51.44.17..58...7.7....5.77..1..5.99.7."
		)
	);

	private static final List<Integer>
		INVALID_BOARD_SIZES, INVALID_CELL_INDICES, INVALID_DIGITS,
		AREA_INDICES, INVALID_AREA_INDICES, DIGITS;
	private static final List<int[]>
		VALID_BOARDS, VALID_COMPLETE_BOARDS, VALID_INCOMPLETE_BOARDS,
		VALID_AREA_VALUES, VALID_COMPLETE_AREA_VALUES, VALID_INCOMPLETE_AREA_VALUES,
		INVALID_BOARDS, INVALID_COMPLETE_BOARDS, INVALID_INCOMPLETE_BOARDS,
		INVALID_AREA_VALUES, INVALID_COMPLETE_AREA_VALUES, INVALID_INCOMPLETE_AREA_VALUES;

	static {
		AREA_INDICES = Collections.unmodifiableList(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
		INVALID_AREA_INDICES = Collections.unmodifiableList(
			Arrays.asList(
				Integer.MIN_VALUE, -100, -10, -1, 9, 10, NUM_CELLS, NUM_CELLS + 1,
				NUM_CELLS + 10, NUM_CELLS + 100, Integer.MAX_VALUE
			)
		);
		DIGITS = Collections.unmodifiableList(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
		INVALID_CELL_INDICES = Collections.unmodifiableList(
			Arrays.asList(
				Integer.MIN_VALUE, -100, -10, -1, NUM_CELLS, NUM_CELLS + 1,
				NUM_CELLS + 10, NUM_CELLS + 100, Integer.MAX_VALUE
			)
		);
		INVALID_DIGITS = Collections.unmodifiableList(
			Arrays.asList(Integer.MIN_VALUE, -10, -1, 10, 100, Integer.MAX_VALUE)
		);

		INVALID_BOARD_SIZES = IntStream.range(0, NUM_CELLS * 2)
			.filter((i) -> i != NUM_CELLS)
			.boxed()
			.collect(Collectors.toUnmodifiableList());

		VALID_COMPLETE_BOARDS = VALID_CONFIGS.stream().map(Board::parseBoardString).toList();

		VALID_COMPLETE_AREA_VALUES = VALID_CONFIGS.stream()
			.<int[]>mapMulti((configStr, consumer) -> {
				int[] board = parseBoardString(configStr);
				Arrays.asList(getRows(board)).forEach(consumer::accept);
				Arrays.asList(getColumns(board)).forEach(consumer::accept);
				Arrays.asList(getRegions(board)).forEach(consumer::accept);
			}).toList();

		VALID_INCOMPLETE_BOARDS = Stream.of(
			Stream.of(emptyBoard()),
			VALID_CONFIGS.stream().map(configStr -> {
				int[] board = parseBoardString(configStr);
				makeIncomplete(board);
				return board;
			})
		).flatMap(i -> i).toList();

		VALID_INCOMPLETE_AREA_VALUES = VALID_CONFIGS.stream()
			.<int[]>mapMulti((configStr, consumer) -> {
				int[] board = parseBoardString(configStr);
				makeIncomplete(board);
				Arrays.asList(getRows(board)).forEach(consumer::accept);
				Arrays.asList(getColumns(board)).forEach(consumer::accept);
				Arrays.asList(getRegions(board)).forEach(consumer::accept);
			}).toList();

		VALID_BOARDS = Stream.of(
			VALID_COMPLETE_BOARDS.stream(),
			VALID_INCOMPLETE_BOARDS.stream()
		).flatMap(i -> i).toList();

		VALID_AREA_VALUES = Stream.of(
			VALID_COMPLETE_AREA_VALUES.stream(),
			VALID_INCOMPLETE_AREA_VALUES.stream()
		).flatMap(i -> i).toList();

		// INVALID ITEMS

		INVALID_COMPLETE_BOARDS = INVALID_CONFIGS.stream().map(Board::parseBoardString).toList();

		INVALID_COMPLETE_AREA_VALUES = INVALID_CONFIGS.stream()
			.<int[]>mapMulti((configStr, consumer) -> {
				int[] board = parseBoardString(configStr);
				Arrays.asList(getRows(board)).forEach(consumer::accept);
				Arrays.asList(getColumns(board)).forEach(consumer::accept);
				Arrays.asList(getRegions(board)).forEach(consumer::accept);
			}).toList();

		INVALID_INCOMPLETE_BOARDS = INVALID_INCOMPLETE_BOARD_STRS.stream()
			.map(Board::parseBoardString)
			.toList();

		INVALID_INCOMPLETE_AREA_VALUES = INVALID_INCOMPLETE_BOARD_STRS.stream()
			.<int[]>mapMulti((boardStr, consumer) -> {
				int[] board = parseBoardString(boardStr);
				Arrays.asList(getRows(board)).forEach(consumer::accept);
				Arrays.asList(getColumns(board)).forEach(consumer::accept);
				Arrays.asList(getRegions(board)).forEach(consumer::accept);
			}).toList();

		INVALID_BOARDS = Stream.of(
			INVALID_COMPLETE_BOARDS.stream(),
			INVALID_INCOMPLETE_BOARDS.stream()
		).flatMap(i -> i).toList();

		INVALID_AREA_VALUES = Stream.of(
			INVALID_COMPLETE_AREA_VALUES.stream(),
			INVALID_INCOMPLETE_AREA_VALUES.stream()
		).flatMap(i -> i).toList();
	}

	private static class ConsumerSpy implements Consumer<Integer> {
		private List<Integer> calledWith;

		ConsumerSpy() {
			calledWith = new ArrayList<>();
		}

		@Override
		public void accept(Integer t) {
			calledWith.add(t);
		}

		public int numCalls() {
			return this.calledWith.size();
		}

		public boolean calledWith(Integer... args) {
			return calledWith.containsAll(Arrays.asList(args));
		}
	}

	private static int[] emptyBoard() {
		return new int[NUM_CELLS];
	}

	private static void makeIncomplete(int[] board) {
		// Erasing these cells yields a board where every row, column, and region are incomplete.
		Arrays.asList(0, 13, 26, 28, 41, 51, 56, 70, 75)
			.stream()
			.forEach(index -> board[index] = 0);
	}

	private static void validateValues(int[] values) {
		if (values == null || values.length != NUM_DIGITS) {
			fail(String.format(
				"Failed to set board values. values[%s] = %s",
				(values == null) ? "-" : Integer.toString(values.length),
				(values == null) ? "null" : Arrays.toString(values)
			));
		}
	}

	private static void setRowValues(int[] board, int rowIndex, int[] values) {
		validateValues(values);
		System.arraycopy(values, 0, board, rowIndex * NUM_DIGITS, NUM_DIGITS);
	}

	private static void setColumnValues(int[] board, int columnIndex, int[] values) {
		validateValues(values);
		for (int valuesIndex = 0; valuesIndex < values.length; valuesIndex++) {
			board[COL_INDICES[columnIndex][valuesIndex]] = values[valuesIndex];
		}
	}

	private static void setRegionValues(int[] board, int regionIndex, int[] values) {
		validateValues(values);
		for (int valuesIndex = 0; valuesIndex < values.length; valuesIndex++) {
			board[REGION_INDICES[regionIndex][valuesIndex]] = values[valuesIndex];
		}
	}

	private static String boardRowStr(int[] board, int rowIndex) {
		return Arrays.toString(
			Arrays.stream(getRow(board, rowIndex))
				.map(Board::decode)
				.toArray()
		);
	}

	private static String boardColStr(int[] board, int colIndex) {
		return Arrays.toString(
			Arrays.stream(getColumn(board, colIndex))
				.map(Board::decode)
				.toArray()
		);
	}

	private static String boardRegionStr(int[] board, int regionIndex) {
		return Arrays.toString(
			Arrays.stream(getRegion(board, regionIndex))
				.map(Board::decode)
				.toArray()
		);
	}

	@Test
	void testGetCompressedString() {
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

	@Test void testRemoveCandidate() {
		assertThrows(IllegalArgumentException.class, () -> removeCandidate(ALL, 0));
		assertThrows(IllegalArgumentException.class, () -> removeCandidate(ALL, 10));

		for (int i = 1; i <= NUM_DIGITS; i++) {
			assertEquals(ALL - (1 << (i - 1)), removeCandidate(ALL, i));
		}
	}

	@Test
	void testGetNumEmptySpaces() {
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
	void testGetNumClues() {
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
	void testClearBoard() {
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
	void testGetValues() {
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
	void testGetValueAt() {
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
	void testSetValueAt() {
		Board b = new Board();
		for (int i = 0; i < Board.NUM_CELLS; i++) {
			for (int v = 0; v <= 9; v++) {
				b.setValueAt(i, v);
				assertEquals(v, b.getValueAt(i));
			}
		}
	}

	@Test
	void testEquals() {
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
		for (int i = 0; i < VALID_CONFIGS.size(); i++) {
			b = new Board(VALID_CONFIGS.get(i));
			for (int j = i + 1; j < VALID_CONFIGS.size(); j++) {
				assertEquals(false, b.equals(new Board(VALID_CONFIGS.get(j))));
			}
		}
	}

	@Test
	void testCopy() {
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
	void testIsValid() {
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
	void testIsRowValid() {
		VALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(rowIndex -> {
				assertTrue(Board.fromCandidates(board).isRowValid(rowIndex));
			});
		});

		INVALID_COMPLETE_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(rowIndex -> {
				Board b = Board.fromCandidates(board);

				assertFalse(b.isRowValid(rowIndex), String.format(
					"Expected row to be invalid: %s%nboard:%n%s",
					boardRowStr(board, rowIndex),
					Board.toString(board)
				));
			});
		});

		INVALID_AREA_VALUES.forEach(areaValues -> {
			int[] board = emptyBoard();
			AREA_INDICES.forEach(areaIndex -> {
				setRowValues(board, areaIndex, areaValues);
				assertFalse(Board.fromCandidates(board).isRowValid(areaIndex), String.format(
					"Expected row to be invalid: %s%nboard:%n%s",
					boardRowStr(board, areaIndex),
					Board.toString(board)
				));
			});
		});
	}

	@Test
	void testIsColValid() {
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
	void testIsRegionValid() {
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
		// VALID_AREA_VALUES.forEach(areaValues -> {

		// });

		for (int[] p : VALID_AREA_VALUES) {
			for (int row = 0; row < 9; row++) {
				for (int valuesIndex = 0; valuesIndex < 9; valuesIndex++) {
					b.setValueAt((row/3)*27 + (row%3)*3 + (valuesIndex/3)*9 + (valuesIndex%3), decode(p[valuesIndex]));
				}
				assertEquals(true, b.isRegionValid(row));
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
		for (int[] p : INVALID_AREA_VALUES) {
			for (int r = 0; r < 9; r++) {
				for (int i = 0; i < 9; i++) {
					b.setValueAt((r/3)*27 + (r%3)*3 + (i/3)*9 + (i%3), decode(p[i]));
				}
				assertEquals(false, b.isRegionValid(r));
			}
		}
	}

	@Test
	void testIsFull() {
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
	void testSolved() {

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

	/****************************
	 *      STATIC METHODS      *
	 ****************************/

	@Test
	void testEncode_whenDigitIsInvalid_throwsException() {
		INVALID_DIGITS.forEach(invalidDigit -> assertThrows(
			IllegalArgumentException.class, () -> encode(invalidDigit),
			String.format("Expected digit %d to be invalid and throw exception", invalidDigit)
		));
	}

	@Test
	void testEncode() {
		int[] digitsToEncodedMap = { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256 };
		IntStream.range(1, NUM_DIGITS + 1)
		.forEach(digit -> assertEquals(digitsToEncodedMap[digit], encode(digit)));
	}

	@Test
	void testForEachCellCandidate_whenAnyArgIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> forEachCellCandidate(null, 0, (i) -> {}));
		Consumer<Integer> nullConsumer = null;
		assertThrows(NullPointerException.class, () -> forEachCellCandidate(emptyBoard(), 0, nullConsumer));
	}

	@Test
	void testForEachCellCandidate_whenBoardIsWrongSize_throwsException() {
		Consumer<Integer> func = (i) -> {};
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> forEachCellCandidate(new int[invalidBoardSize], 0, func)
		));
	}

	@Test
	void testForEachCellCandidate_whenCellIndexOutOfBounds_throwsException() {
		Consumer<Integer> func = (i) -> {};
		INVALID_CELL_INDICES.forEach(invalidCellIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> forEachCellCandidate(emptyBoard(), invalidCellIndex, func)
		));
	}

	@Test
	void testForEachCellCandidate_whenCellHasNoCandidates_doesNotInvokeCallback() {
		ConsumerSpy callbackSpy = new ConsumerSpy();
		forEachCellCandidate(emptyBoard(), 0, callbackSpy);
		assertEquals(0, callbackSpy.numCalls());
	}

	@Test
	void testForEachCellCandidate_whenCellIsDigit_invokesCallbackWithDigit() {
		ConsumerSpy callbackSpy = new ConsumerSpy();
		int cellIndex = 35;
		int expectedDigit = 7;
		int[] board = emptyBoard();
		board[cellIndex] = encode(expectedDigit);

		forEachCellCandidate(board, cellIndex, callbackSpy);
		assertEquals(1, callbackSpy.numCalls());
		assertTrue(callbackSpy.calledWith(expectedDigit));
	}

	@Test
	void testForEachCellCandidate_whenCellHasMultipleCandidates_invokesCallbackWithEachCandidateDigit() {
		ConsumerSpy callbackSpy = new ConsumerSpy();
		int cellIndex = 35;
		int expectedCandidates = 0b101010101; // Odd digits
		int[] expectedDigits = { 1, 3, 5, 7, 9 };
		int[] board = emptyBoard();
		board[cellIndex] = expectedCandidates;

		forEachCellCandidate(board, cellIndex, callbackSpy);
		assertEquals(expectedDigits.length, callbackSpy.numCalls());
		Arrays.stream(expectedDigits).forEach(expectedDigit -> assertTrue(
			callbackSpy.calledWith(expectedDigit)
		));
	}

	@Test
	void testIsValid_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isValid(null));
	}

	@Test
	void testIsValid_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isValid(new int[invalidBoardSize])
		));
	}

	@Test
	void testIsValid_whenARowIsInvalid_returnsFalse() {
		INVALID_AREA_VALUES.forEach(invalidAreaValues -> AREA_INDICES.forEach(
			rowIndex -> {
				int[] board = emptyBoard();
				setRowValues(board, rowIndex, invalidAreaValues);
				assertFalse(isValid(board));
			})
		);
	}

	@Test
	void testIsValid_whenAColumnIsInvalid_returnsFalse() {
		INVALID_AREA_VALUES.forEach(invalidAreaValues -> AREA_INDICES.forEach(
			colIndex -> {
				int[] board = emptyBoard();
				setColumnValues(board, colIndex, invalidAreaValues);
				assertFalse(isValid(board));
			})
		);
	}

	@Test
	void testIsValid_whenARegionIsInvalid_returnsFalse() {
		INVALID_AREA_VALUES.forEach(invalidAreaValues -> AREA_INDICES.forEach(
			regionIndex -> {
				int[] board = emptyBoard();
				setRegionValues(board, regionIndex, invalidAreaValues);
				assertFalse(isValid(board));
			})
		);
	}

	@Test
	void testIsValid_whenBoardIsEmpty_returnsTrue() {
		assertTrue(isValid(emptyBoard()));
	}

	@Test
	void testIsValid_whenAllRowsAndColumnsAndRegionsAreValid_returnsTrue() {
		VALID_BOARDS.forEach(validBoard -> assertTrue(isValid(validBoard)));
	}

	@Test
	void testIsRowValid_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isRowValid(null, 0));
	}

	@Test
	void testIsRowValid_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isRowValid(new int[invalidBoardSize], 0)
		));
	}

	@Test
	void testIsRowValid_whenIndexIsOutOfBounds_throwsException() {
		INVALID_AREA_INDICES.forEach(invalidAreaIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> isRowValid(emptyBoard(), invalidAreaIndex)
		));
	}

	@Test
	void testIsRowValid_whenRowIsEmpty_returnsTrue() {
		AREA_INDICES.forEach(rowIndex -> assertTrue(isRowValid(emptyBoard(), rowIndex)));
	}

	@Test
	void testIsRowValid_whenRowContainsNoDuplicateDigits_returnsTrue() {
		VALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(rowIndex -> {
				assertTrue(isRowValid(board, rowIndex));
			});
		});

		int[] board = emptyBoard();
		VALID_AREA_VALUES.forEach(areaValues -> {
			AREA_INDICES.forEach(rowIndex -> {
				setRowValues(board, rowIndex, areaValues);
				assertTrue(isRowValid(board, rowIndex));
			});
		});
	}

	@Test
	void testIsRowValid_whenRowContainsDuplicateDigits_returnsFalse() {
		INVALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(rowIndex -> {
				assertFalse(isRowValid(board, rowIndex));
			});
		});

		int[] board = emptyBoard();
		INVALID_AREA_VALUES.forEach(areaValues -> {
			AREA_INDICES.forEach(rowIndex -> {
				setRowValues(board, rowIndex, areaValues);
				assertFalse(isRowValid(board, rowIndex));
			});
		});
	}

	@Test
	void testIsRowFull_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isRowFull(null, 0));
	}

	@Test
	void testIsRowFull_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isRowFull(new int[invalidBoardSize], 0)
		));
	}

	@Test
	void testIsRowFull_whenIndexIsOutOfBounds_throwsException() {
		INVALID_AREA_INDICES.forEach(invalidAreaIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> isRowFull(emptyBoard(), invalidAreaIndex)
		));
	}

	@Test
	void testIsRowFull_whenRowIsEmpty_returnsFalse() {
		AREA_INDICES.forEach(rowIndex -> assertFalse(isRowFull(emptyBoard(), rowIndex)));
	}

	@Test
	void testIsRowFull_whenRowIsPartiallyFull_returnsFalse() {
		Stream.of(
			VALID_INCOMPLETE_BOARDS.stream(),
			INVALID_INCOMPLETE_BOARDS.stream()
		).flatMap(i -> i)
		.forEach(incompleteBoard -> {
			AREA_INDICES.forEach(rowIndex -> assertFalse(isRowFull(incompleteBoard, rowIndex)));
		});
	}

	@Test
	void testIsRowFull_whenRowIsFull_returnsTrue() {
		Stream.of(
			VALID_COMPLETE_BOARDS.stream(),
			INVALID_COMPLETE_BOARDS.stream()
		).flatMap(i -> i)
		.forEach(board -> {
			AREA_INDICES.forEach(rowIndex -> assertTrue(isRowFull(board, rowIndex)));
		});
	}

	@Test
	void testIsColValid_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isColValid(null, 0));
	}

	@Test
	void testIsColValid_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isColValid(new int[invalidBoardSize], 0)
		));
	}

	@Test
	void testIsColValid_whenIndexIsOutOfBounds_throwsException() {
		INVALID_AREA_INDICES.forEach(invalidAreaIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> isColValid(emptyBoard(), invalidAreaIndex)
		));
	}

	@Test
	void testIsColValid_whenColIsEmpty_returnsTrue() {
		AREA_INDICES.forEach(colIndex -> assertTrue(isColValid(emptyBoard(), colIndex)));
	}

	@Test
	void testIsColValid_whenColContainsNoDuplicateDigits_returnsTrue() {
		VALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(colIndex -> {
				assertTrue(isColValid(board, colIndex));
			});
		});

		int[] board = emptyBoard();
		VALID_AREA_VALUES.forEach(areaValues -> {
			AREA_INDICES.forEach(colindex -> {
				setColumnValues(board, colindex, areaValues);
				assertTrue(isColValid(board, colindex));
			});
		});
	}

	@Test
	void testIsColValid_whenColContainsDuplicateDigits_returnsFalse() {
		INVALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(colIndex -> {
				assertFalse(isColValid(board, colIndex));
			});
		});

		int[] board = emptyBoard();
		INVALID_AREA_VALUES.forEach(areaValues -> {
			AREA_INDICES.forEach(colIndex -> {
				setColumnValues(board, colIndex, areaValues);
				assertFalse(isColValid(board, colIndex));
			});
		});
	}

	@Test
	void testIsColFull_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isColFull(null, 0));
	}

	@Test
	void testIsColFull_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isColFull(new int[invalidBoardSize], 0)
		));
	}

	@Test
	void testIsColFull_whenIndexIsOutOfBounds_throwsException() {
		INVALID_AREA_INDICES.forEach(invalidAreaIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> isColFull(emptyBoard(), invalidAreaIndex)
		));
	}

	@Test
	void testIsColFull_whenColIsEmpty_returnsFalse() {
		AREA_INDICES.forEach(colIndex -> assertFalse(isColFull(emptyBoard(), colIndex)));
	}

	@Test
	void testIsColFull_whenColIsPartiallyFull_returnsFalse() {
		Stream.of(
			VALID_INCOMPLETE_BOARDS.stream(),
			INVALID_INCOMPLETE_BOARDS.stream()
		).flatMap(i -> i)
		.forEach(incompleteBoard -> {
			AREA_INDICES.forEach(colIndex -> assertFalse(isColFull(incompleteBoard, colIndex)));
		});
	}

	@Test
	void testIsColFull_whenColIsFull_returnsTrue() {
		Stream.of(
			VALID_COMPLETE_BOARDS.stream(),
			INVALID_COMPLETE_BOARDS.stream()
		).flatMap(i -> i)
		.forEach(board -> {
			AREA_INDICES.forEach(colIndex -> assertTrue(isColFull(board, colIndex)));
		});
	}

	@Test
	void testIsRegionValid_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isRegionValid(null, 0));
	}

	@Test
	void testIsRegionValid_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isRegionValid(new int[invalidBoardSize], 0)
		));
	}

	@Test
	void testIsRegionValid_whenIndexIsOutOfBounds_throwsException() {
		INVALID_AREA_INDICES.forEach(invalidAreaIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> isRegionValid(emptyBoard(), invalidAreaIndex)
		));
	}

	@Test
	void testIsRegionValid_whenRegionIsEmpty_returnsTrue() {
		AREA_INDICES.forEach(regionIndex -> assertTrue(isRegionValid(emptyBoard(), regionIndex)));
	}

	@Test
	void testIsRegionValid_whenRegionContainsNoDuplicateDigits_returnsTrue() {
		VALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(regionIndex -> {
				assertTrue(isRegionValid(board, regionIndex));
			});
		});

		int[] board = emptyBoard();
		VALID_AREA_VALUES.forEach(areaValues -> {
			AREA_INDICES.forEach(regionIndex -> {
				setRegionValues(board, regionIndex, areaValues);
				assertTrue(isRegionValid(board, regionIndex));
			});
		});
	}

	@Test
	void testIsRegionValid_whenRegionContainsDuplicateDigits_returnsFalse() {
		INVALID_BOARDS.forEach(board -> {
			AREA_INDICES.forEach(regionIndex -> {
				assertFalse(isRegionValid(board, regionIndex));
			});
		});

		int[] board = emptyBoard();
		INVALID_AREA_VALUES.forEach(areaValues -> {
			AREA_INDICES.forEach(regionIndex -> {
				setRegionValues(board, regionIndex, areaValues);
				assertFalse(isRegionValid(board, regionIndex));
			});
		});
	}

	@Test
	void testIsRegionFull_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isRegionFull(null, 0));
	}

	@Test
	void testIsRegionFull_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isRegionFull(new int[invalidBoardSize], 0)
		));
	}

	@Test
	void testIsRegionFull_whenIndexIsOutOfBounds_throwsException() {
		INVALID_AREA_INDICES.forEach(invalidAreaIndex -> assertThrows(
			IllegalArgumentException.class,
			() -> isRegionFull(emptyBoard(), invalidAreaIndex)
		));
	}

	@Test
	void testIsRegionFull_whenRegionIsEmpty_returnsFalse() {
		AREA_INDICES.forEach(regionIndex -> assertFalse(isRegionFull(emptyBoard(), regionIndex)));
	}

	@Test
	void testIsRegionFull_whenRegionIsPartiallyFull_returnsFalse() {
		Stream.of(
			VALID_INCOMPLETE_BOARDS.stream(),
			INVALID_INCOMPLETE_BOARDS.stream()
		)
		.flatMap(i -> i)
		.forEach(board -> AREA_INDICES.forEach(regionIndex -> assertFalse(
			isRegionFull(board, regionIndex)
		)));
	}

	@Test
	void testIsRegionFull_whenRegionIsFull_returnsTrue() {
		Stream.of(
			VALID_COMPLETE_BOARDS.stream(),
			INVALID_COMPLETE_BOARDS.stream()
		)
		.flatMap(i -> i)
		.forEach(board -> {
			AREA_INDICES.forEach(regionIndex -> assertTrue(
				isRegionFull(board, regionIndex)
			));
		});
	}

	@Test
	void testIsFull_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isFull(null));
	}

	@Test
	void testIsFull_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isFull(new int[invalidBoardSize])
		));
	}

	@Test
	void testIsFull_whenBoardIsEmpty_returnsFalse() {
		assertFalse(isFull(emptyBoard()));
	}

	@Test
	void testIsFull_whenBoardIsPartiallyFull_returnsFalse() {
		Stream.of(
			VALID_INCOMPLETE_BOARDS.stream(),
			INVALID_INCOMPLETE_BOARDS.stream()
		)
		.flatMap(i -> i)
		.forEach(board -> assertFalse(isFull(board)));
	}

	@Test
	void testIsFull_whenBoardIsFull_returnsTrue() {
		Stream.of(
			VALID_COMPLETE_BOARDS.stream(),
			INVALID_COMPLETE_BOARDS.stream()
		)
		.flatMap(i -> i)
		.forEach(board -> assertTrue(isFull(board)));
	}

	@Test
	void testIsSolved_whenBoardIsNull_throwsException() {
		assertThrows(NullPointerException.class, () -> isSolved(null));
	}

	@Test
	void testIsSolved_whenBoardIsWrongSize_throwsException() {
		INVALID_BOARD_SIZES.forEach(invalidBoardSize -> assertThrows(
			IllegalArgumentException.class,
			() -> isSolved(new int[invalidBoardSize])
		));
	}

	@Test
	void testIsSolved_whenBoardIsEmpty_returnsFalse() {
		assertFalse(isSolved(emptyBoard()));
	}

	@Test
	void testIsSolved_whenBoardIsPartiallyFull_returnsFalse() {
		Stream.of(
			VALID_INCOMPLETE_BOARDS.stream(),
			INVALID_INCOMPLETE_BOARDS.stream()
		)
		.flatMap(i -> i)
		.forEach(board -> assertFalse(isSolved(board)));
	}

	@Test
	void testIsSolved_whenBoardIsFull_whenBoardIsInvalid_returnsFalse() {
		INVALID_COMPLETE_BOARDS.forEach(board -> assertFalse(isSolved(board)));
	}

	@Test
	void testIsSolved_whenBoardIsFull_whenBoardIsValid_returnsTrue() {
		VALID_COMPLETE_BOARDS.forEach(board -> assertTrue(isSolved(board)));
	}
}
