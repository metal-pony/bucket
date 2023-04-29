package com.sparklicorn.bucket.sudoku.game.solvers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.sparklicorn.bucket.sudoku.game.Board;
import com.sparklicorn.bucket.sudoku.game.SudokuUtility;
import com.sparklicorn.bucket.sudoku.game.solvers.Solver.BoardSolution;

public class TestSolver {
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
			"1".repeat(Board.NUM_CELLS),
			"2".repeat(Board.NUM_CELLS),
			"3".repeat(Board.NUM_CELLS),
			"4".repeat(Board.NUM_CELLS),
			"5".repeat(Board.NUM_CELLS),
			"6".repeat(Board.NUM_CELLS),
			"7".repeat(Board.NUM_CELLS),
			"8".repeat(Board.NUM_CELLS),
			"9".repeat(Board.NUM_CELLS),
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

  private static final List<Integer> INVALID_BOARD_SIZES, INVALID_CELL_INDICES;
	private static final List<int[]>
		VALID_BOARDS, VALID_COMPLETE_BOARDS, VALID_INCOMPLETE_BOARDS,
		INVALID_BOARDS, INVALID_COMPLETE_BOARDS, INVALID_INCOMPLETE_BOARDS;

  static {
		INVALID_CELL_INDICES = Collections.unmodifiableList(
			Arrays.asList(
				Integer.MIN_VALUE, -100, -10, -1, Board.NUM_CELLS, Board.NUM_CELLS + 1,
				Board.NUM_CELLS + 10, Board.NUM_CELLS + 100, Integer.MAX_VALUE
			)
		);
    INVALID_BOARD_SIZES = IntStream.range(0, Board.NUM_CELLS * 2)
			.filter((i) -> i != Board.NUM_CELLS)
			.boxed()
			.collect(Collectors.toUnmodifiableList());

    VALID_COMPLETE_BOARDS = VALID_CONFIGS.stream().map(Board::parseBoardString).toList();
    VALID_INCOMPLETE_BOARDS = Stream.of(
      Stream.of(SudokuUtility.emptyBoard()),
      VALID_CONFIGS.stream().map(configStr -> {
        int[] board = Board.parseBoardString(configStr);
        makeIncomplete(board);
        return board;
      })
    ).flatMap(i -> i).toList();

    VALID_BOARDS = Stream.of(
      VALID_COMPLETE_BOARDS.stream(),
      VALID_INCOMPLETE_BOARDS.stream()
    ).flatMap(i -> i).toList();

    // INVALID ITEMS
    INVALID_COMPLETE_BOARDS = INVALID_CONFIGS.stream().map(Board::parseBoardString).toList();
    INVALID_INCOMPLETE_BOARDS = INVALID_INCOMPLETE_BOARD_STRS.stream()
      .map(Board::parseBoardString)
      .toList();

    INVALID_BOARDS = Stream.of(
      INVALID_COMPLETE_BOARDS.stream(),
      INVALID_INCOMPLETE_BOARDS.stream()
    ).flatMap(i -> i).toList();
  }

  private static void makeIncomplete(int[] board) {
    // Erasing these cells yields a board where every row, column, and region are incomplete.
    Arrays.asList(0, 13, 26, 28, 41, 51, 56, 70, 75)
      .stream()
      .forEach(index -> board[index] = 0);
  }

  private static int[] arrCopy(int[] arr) {
    int[] copy = new int[arr.length];
    System.arraycopy(arr, 0, copy, 0, arr.length);
    return copy;
  }

  static void forEach(List<int[]> boardList, Consumer<? super int[]> action) {
    boardList.stream().map(TestSolver::arrCopy).forEach(action);
  }

  static class ListIterators<T> {
    private final List<List<T>> lists;
    ListIterators(List<T>[] boardLists) {
      lists = Collections.unmodifiableList(Arrays.asList(boardLists));
    }
    void doIt(Consumer<? super T> action) {
      lists.forEach(list -> list.forEach(action));
    }
  }

  @SafeVarargs
  static ListIterators<int[]> forEach(List<int[]>... lists) {
    return new ListIterators<>(lists);
  }

  static void forEachCell(Consumer<Integer> action) {
    for (int i = 0; i < Board.NUM_CELLS; i++) {
      action.accept(i);
    }
  }

  private static record BoardFixture(
    String solved, String unsolved,
    int[] puzzle, int[] solution
  ) {
    BoardFixture(String solved, String unsolved) {
      this(
        solved, unsolved,
        Board.parseBoardString(unsolved), Board.parseBoardString(solved)
      );
    }
  }

  private static BoardFixture testBoardFixture = new BoardFixture(
    "135697482498231765267548913812763549946852137753419826629185374384976251571324698",
    "1.56....2..8.3...5.6...89..8....3.4..4.....3..5.4....6..91...7.3...7.2..5....46.8"
  );

  // @Test
  // @Timeout(unit = TimeUnit.SECONDS,value = 10L)
  void testSolve() {
    // assertEquals(
    //   new Board(testBoardFixture.solved),
    //   Solver.solve(new Board(testBoardFixture.unsolved))
    // );
    fail("Need reimplementation.");
    // assertArrayEquals(
    //   Board.parseBoardString(testBoardFixture.solved),
    //   Solver.solve(Board.parseBoardString(testBoardFixture.unsolved))
    // );

    // for (int i = 0; i < GeneratedPuzzles.PUZZLES_24_1000.length; i++) {
    //   String puzzleStr = GeneratedPuzzles.PUZZLES_24_1000[i];
    //   int[] puzzle = Board.parseBoardString(puzzleStr);
    //   int[] solved = Solver.solve(puzzle);
    //   System.out.println(SudokuUtility.getSimplifiedString(solved));
    // }
  }

  // TODO #23 test all methods with int[] param when int[].length != NUM_CELLS

  // @Test
  void testGetSolutions() {
    // TODO #23 More test fixtures needed.
    fail("NYI");
  }

  @Test
  void testGetSolutions_whenBoardIsNull_throwsException() {
    assertThrows(NullPointerException.class, () -> Solver.getSolutions(null));
  }

  @Test
  void testGetSolutions_whenBoardIsInvalid_returnsEmptySet() {
    forEach(INVALID_BOARDS.subList(0, 10), board -> {
      assertEquals(0, Solver.getSolutions(arrCopy(board)).size());
    });
  }

  @Test
  void testResetEmptyCells_whenBoardIsNull_throwsException() {
    assertThrows(NullPointerException.class, () -> Solver.resetEmptyCells(null));
  }

  @Test
  void testResetEmptyCells_whenBoardIsEmpty_resetsEveryCellToAll() {
    int[] board = SudokuUtility.emptyBoard();
    int[] expected = SudokuUtility.emptyBoard();
    Arrays.fill(expected, Board.ALL);

    int[] actual = Solver.resetEmptyCells(board);
    assertTrue(board == actual);
    assertArrayEquals(expected, actual);
  }

  @Test
  void testResetEmptyCells_whenBoardIsPartiallyEmpty_resetsOnlyTheEmptyCells() {
    forEach(VALID_COMPLETE_BOARDS, INVALID_COMPLETE_BOARDS).doIt(board -> {
      int[] expected = arrCopy(board);
      for (int i = 0; i < expected.length; i++) {
        int j = expected[i];
        expected[i] = j > 0 ? j : Board.ALL;
      }

      int[] actual = Solver.resetEmptyCells(board);
      assertTrue(actual == board);
      assertArrayEquals(expected, actual);
    });
  }

  @Test
  void testResetEmptyCells_whenBoardIsFullOfDigits_resetsNothing() {
    forEach(VALID_COMPLETE_BOARDS, INVALID_COMPLETE_BOARDS).doIt(board -> {
      int[] original = arrCopy(board);
      int[] actual = Solver.resetEmptyCells(board);
      assertTrue(actual == board);
      assertArrayEquals(original, actual);
    });
  }

  @Test
  void testReduce2_whenBoardIsNull_throwsException() {
    assertThrows(NullPointerException.class, () -> Solver.reduce2(null, 0));
  }

  @Test
  void testReduce2_whenCellIndexIsOutOfBounds_throwsException() {
    INVALID_CELL_INDICES.forEach(cellIndex -> {
      assertThrows(IllegalArgumentException.class, () -> {
        Solver.reduce2(new BoardSolution(SudokuUtility.emptyBoard()), cellIndex);
      });
    });
  }

  @Test
  void testReduce2_whenCellContainsADigit_returnsFalse() {
    forEach(VALID_COMPLETE_BOARDS, INVALID_COMPLETE_BOARDS).doIt(board -> {
      forEachCell(cellIndex -> assertFalse(
        Solver.reduce2(new BoardSolution(board), cellIndex))
      );
    });
  }

  @Test
  void testReduce2_whenCellContainsNoCandidates_returnsFalse() {
    forEachCell(cellIndex -> assertFalse(
      Solver.reduce2(new BoardSolution(SudokuUtility.emptyBoard()), cellIndex)
    ));
  }

  /**
   * Encodes candidate digits into the bitwise representation Board uses.
   *
   * @param digits Integer representing candidates for a Sudoku cell.
   * Eg, <code>1234</code> represents the individual candidate digits <code>1, 2, 3, 4</code>
   * that the associated cell could reduce to.
   * @return
   */
  public static int c(int digits) {
    if (digits < 10) {
      return Board.encode(digits);
    }

    int result = 0;
    while (digits > 0) {
      int d = digits % 10;
      digits /= 10;
      result += Board.encode(d);
    }
    return result;
  }

  /**
   * Decodes Sudoku candidate bits into a representation easier consumed by humans.
   * Eg, <code>d(0b011011001) => 14578</code>, and each digit in the result
   * corresponds to a candidate for the associated cell.
   *
   * @param encoded
   * @return
   */
  public static int d(int encoded) {
    if (Board.isDigit(encoded)) {
      return Board.decode(encoded);
    }

    int result = 0;
    for (int digit = 1; digit <= Board.NUM_DIGITS; digit++) {
      if ((encoded & (1 << (digit - 1))) > 0) {
        result = (result * 10) + digit;
      }
    }
    return result;
  }

  /**
   * Builds a readable string representation of a board and its candidates.
   *
   * @param board
   * @return
   */
  public static String boardString(int[] board) {
    StringBuilder strb = new StringBuilder();

    for (int rowIndex = 0; rowIndex < Board.NUM_ROWS; rowIndex++) {
      Solver.forEachCellInRow(board, rowIndex, (index, value) -> {
        strb.append(value);

        // Add comma if not last cell in row
        if (index % Board.NUM_COLUMNS != Board.NUM_COLUMNS - 1) {
          strb.append(',');
        }
      });

      // Add newline if not last row
      if (rowIndex < Board.NUM_ROWS - 1) {
        strb.append(System.lineSeparator());
      }
    }

    return strb.toString();
  }

  @Test
  void testReduce2_whenAreaIncludesADigit_removesDigitFromAreaCandidates() {
    // 1 . 5 | 6 . . | . . 2
    // . . 8 | . 3 . | . . 5
    // . 6 . | . . 8 | 9 . .
    // ------+-------+------
    // 8 . . | . . 3 | . 4 .
    // . 4 . | . . . | . 3 .
    // . 5 . | 4 . . | . . 6
    // ------+-------+------
    // . . 9 | 1 . . | . 7 .
    // 3 . . | . 7 . | 2 . .
    // 5 . . | . . 4 | 6 . 8

    // Just first row of above board
    int[] board = Board.parseBoardString("1.56....2--------");
    Solver.resetEmptyCells(board);
    BoardSolution boardSolution = new BoardSolution(board);

    int A = 123456789;

    int[] expectedBoard = new int[] {
      1,        34789,    5,          6,        34789,    34789,        34789,    34789,    2,
      2346789,  2346789,  2346789,    12345789, 12345789, 12345789,     13456789, 13456789, 13456789,
      2346789,  2346789,  2346789,    12345789, 12345789, 12345789,     13456789, 13456789, 13456789,

      23456789, A,        12346789,   12345789, A,        A,            A,        A,        13456789,
      23456789, A,        12346789,   12345789, A,        A,            A,        A,        13456789,
      23456789, A,        12346789,   12345789, A,        A,            A,        A,        13456789,

      23456789, A,        12346789,   12345789, A,        A,            A,        A,        13456789,
      23456789, A,        12346789,   12345789, A,        A,            A,        A,        13456789,
      23456789, A,        12346789,   12345789, A,        A,            A,        A,        13456789,
    };
    for (int i = 0; i < expectedBoard.length; i++) {
      expectedBoard[i] = c(expectedBoard[i]);
    }

    boolean result = Solver.reduce2(boardSolution, 1);
    assertArrayEquals(expectedBoard, boardSolution.board());
    assertTrue(result);
  }

  @Test
  void testReduce_whenBoardIsNull_throwsException() {
    assertThrows(NullPointerException.class, () -> Solver.reduce((int[]) null));
    assertThrows(NullPointerException.class, () -> Solver.reduce((BoardSolution) null));
  }

  @Test
  void testReduce_whenBoardIsEmpty_returnsFalse() {
    int[] board = SudokuUtility.emptyBoard();
    assertFalse(Solver.reduce(board));
    Solver.resetEmptyCells(board);
    assertFalse(Solver.reduce(board));
  }

  // @Test
  void testReduce_whenBoardIsPartiallyEmpty() {
    // TODO #23 Create new test fixtures first
    fail("NYI");
  }

  @Test
  void testReduce_whenBoardIsFull_returnsFalse() {
    forEach(VALID_COMPLETE_BOARDS, INVALID_COMPLETE_BOARDS).doIt(board -> {
      assertFalse(Solver.reduce(board));
    });
  }

  @Test
  void testCopyBoard_whenBoardIsNull_throwsException() {
    assertThrows(NullPointerException.class, () -> Solver.copyBoard(null));
  }

  @Test
  void testCopyBoard_returnsACopyOfBoard() {
    forEach(VALID_BOARDS, INVALID_BOARDS).doIt(board -> {
      assertFalse(board == Solver.copyBoard(board));
      assertArrayEquals(board, Solver.copyBoard(board));
    });
  }

  @Test
  void testSearchForSolutions_whenBoardIsNull_throwsException() {
    assertThrows(
      NullPointerException.class,
      () -> Solver.searchForSolutions(null, (solution) -> {})
    );
  }

  @Test
  void testSearchForSolutions_whenCallbackIsNull_throwsException() {
    BoardSolution boardSolution = new BoardSolution(VALID_BOARDS.get(0));
    assertThrows(NullPointerException.class, () -> {
      Solver.searchForSolutions(boardSolution, (Function<int[],Boolean>) null);
    });
    assertThrows(NullPointerException.class, () -> {
      Solver.searchForSolutions(boardSolution, (Consumer<int[]>) null);
    });
  }

  @Test
  void testSearchForSolutions_whenBoardIsInvalid_doesNotInvokeCallback() {
    AtomicBoolean callbackFired = new AtomicBoolean();

    forEach(INVALID_BOARDS.subList(0, 10)).doIt(board -> {
      Solver.searchForSolutions(new BoardSolution(board), (solution) -> {
        callbackFired.set(true);
      });
    });

    assertFalse(callbackFired.get());
  }

  @Test
  void testSearchForSolutions_whenASolutionIsFound_invokesTheCallbackWithSolution() {
    // TODO #23 Amend with new test fixtures later

    AtomicInteger callbackCounter = new AtomicInteger();
    forEach(VALID_COMPLETE_BOARDS).doIt((boardInput) -> {
      int[] originalBoardInput = Solver.copyBoard(boardInput);
      Solver.searchForSolutions(new BoardSolution(boardInput), (solution) -> {
        callbackCounter.incrementAndGet();
        assertArrayEquals(originalBoardInput, solution);
        assertFalse(boardInput == solution);

        // Does not modify input
        assertArrayEquals(originalBoardInput, boardInput);

        // Tells algorithm to continue searching for more solutions
        return true;
      });
    });

    assertEquals(VALID_COMPLETE_BOARDS.size(), callbackCounter.get());
  }

  @Test
  void testSearchForSolutions_whenASolutionIsFound_whenCallbackReturnsTrue_returnsTrue() {
    assertTrue(Solver.searchForSolutions(
      new BoardSolution(VALID_COMPLETE_BOARDS.get(0)),
      (solution) -> true
    ));
  }

  @Test
  void testSearchForSolutions_whenASolutionIsFound_whenCallbackReturnsFalse_stopsSearchingAndReturnsFalse() {
    assertFalse(Solver.searchForSolutions(
      new BoardSolution(VALID_COMPLETE_BOARDS.get(0)),
      (solution) -> false
    ));

    // TODO #23 Add test with puzzle that has multiple solutions,
    // that the callback is only called once.
  }

  // @Test
  void testSearchForSolutions_whenBoardHasMultipleSolutions_whenCallbackReturnsTrue_invokesCallbackForEachSolutionAndReturnsTrue() {
    // TODO #23 More test fixtures needed.
    fail("NYI");
  }

  // @Test
  void testSearchForSolutions_whenBoardHasMultipleSolutions_whenCallbackReturnsFalse_invokesCallbackOnceAndReturnsFalse() {
    // TODO #23 More test fixtures needed.
    fail("NYI");
  }
}
