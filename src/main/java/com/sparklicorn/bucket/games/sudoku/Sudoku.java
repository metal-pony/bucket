package com.sparklicorn.bucket.games.sudoku;

import java.util.Arrays;
import java.util.List;

import com.sparklicorn.bucket.games.sudoku.game.Board;
import com.sparklicorn.bucket.games.sudoku.game.SudokuUtility;
import com.sparklicorn.bucket.games.sudoku.game.generators.SudokuGeneratorService;
import com.sparklicorn.bucket.games.sudoku.game.solvers.Solver;

public class Sudoku {

    /**
     * Minimum number of clues allowed for puzzle generation.
     */
    public static final int MIN_PUZZLE_CLUES = 24;

    /**
     * Maximum number of clues allowed for puzzle generation.
     */
    public static final int MAX_PUZZLE_CLUES = 81;

    /**
     * Default number of clues for puzzle generation.
     */
    public static final int DEFAULT_PUZZLE_CLUES = 30;

    /**
     * Generates and returns a Sudoku with the default number of clues (30).
     */
    public static Sudoku generate() {
        return Sudoku.generate(DEFAULT_PUZZLE_CLUES);
    }

    /**
     * Generates and returns a Sudoku with the given number of clues.
     *
     * @param clues Number of spaces that should be pre-filled. [min=24, max=81]
     */
    public static Sudoku generate(int clues) {
        int _clues = com.sparklicorn.bucket.util.Math.inBounds(clues, MIN_PUZZLE_CLUES, MAX_PUZZLE_CLUES);
        int[] config = SudokuGeneratorService.config();
        // System.out.printf("generated config %s\n", SudokuUtility.getSimplifiedString(config));
        int[] puzzle = SudokuGeneratorService.generatePuzzle(config, _clues);
        // System.out.printf("generated puzzle %s\n", SudokuUtility.getSimplifiedString(puzzle));
        return new Sudoku(config, puzzle);
    }

    /**
     * Builds and returns a JSON string representing the given array of Sudokus.
     *
     * @param sudokus Array of Sudokus to represent.
     */
    public static String toJsonString(Sudoku[] sudokus) {
        if (sudokus == null) {
            return "null";
        }

        if (sudokus.length == 0) {
            return "[]";
        }

        StringBuilder strb = new StringBuilder("[\n");
        for (int i = 0; i < sudokus.length; i++) {
            Sudoku sudoku = sudokus[i];
            strb.append(sudoku.toJsonString(1));
            if (i < sudokus.length - 1) {
                strb.append(',');
            }
            strb.append('\n');
        }
        strb.append(']');

        return strb.toString();
    }

    // TODO Use digit values in these arrays instead of the candidates bits
    final int[] solution;
    final int[] puzzle;
    final int clues;

    /**
     * Creates a new Sudoku with the given solution and puzzle. A validation step will be
     * run on both, which can be skipped via <code>skipValidation</code> as it can be
     * expensive for puzzles with few clues.
     *
     * @param solution
     * @param puzzle
     * @param skipValidation Whether to skip the validation process.
     */
    public Sudoku(int[] solution, int[] puzzle, boolean skipValidation) {
        if (!skipValidation) {
            if (!Board.isSolved(solution)) {
                throw new IllegalArgumentException("Given solution is not valid.");
            }

            int[] solved = validatePuzzle(puzzle);
            if (!Arrays.equals(solved, solution)) {
                throw new IllegalArgumentException("The solved puzzle does not match the given solution.");
            }
        }

        this.solution = SudokuUtility.copyArr(solution);
        this.puzzle = SudokuUtility.copyArr(puzzle);
        this.clues = Board.countClues(puzzle);
    }

    /**
     * Creates a new Sudoku with the given solution and puzzle. A validation step will be
     * run on both, which may be expensive for puzzles with few clues.
     *
     * @param solution
     * @param puzzle
     */
    public Sudoku(int[] solution, int[] puzzle) {
        this(solution, puzzle, false);
    }

    /**
     * Creates a new Sudoku from the given puzzle. The puzzle will be validated, which
     * includes solving in order to verify there is a single solution only.
     *
     * @param puzzle
     */
    public Sudoku(int[] puzzle) {
        this(validatePuzzle(puzzle), puzzle, true);
    }

    @Override
    public String toString() {
        return toJsonString(0);
    }

    /**
     * Returns a JSON string representation of this Sudoku, indented the specified amount.
     * Note: A single indentation is specified here as two spaces.
     *
     * @param indentation Number of indentations to prepend the output.
     */
    public String toJsonString(int indentation) {
        String indent = "  ".repeat(indentation);

        return String.format(
            """
                %s{
                  %s"solution": "%s",
                  %s"puzzle": "%s",
                  %s"clues": "%d"
                %s}""",
            indent,
            indent, Board.fromCandidates(solution).getSimplifiedString(),
            indent, Board.fromCandidates(puzzle).getSimplifiedString(),
            indent, clues,
            indent
        );
    }

    /**
     * Tests whether the puzzle is valid, throwing an exception if it is null, empty,
     * has too few clues, or does not solve into a single solution. The solution is
     * returned if the puzzle is valid.
     *
     * The validation process attempts to get all solutions for the puzzle, which may
     * be an expensive operation for puzzles with few clues.
     *
     * @param puzzle
     * @return The puzzle's solution
     * @throws NullPointerException if <code>puzzle</code> is null.
     * @throws IllegalArgumentException if <code>puzzle</code> is incorrect size,
     * contains too few or too many clues, or does not have one and only one solution.
     */
    static int[] validatePuzzle(int[] puzzle) {
        if (puzzle == null) {
            throw new NullPointerException("Sudoku puzzle array cannot be null");
        }

        if (puzzle.length != Board.NUM_CELLS) {
            throw new IllegalArgumentException(String.format(
                "Sudoku puzzle array must have length %d",
                Board.NUM_CELLS
            ));
        }

        int clues = Board.countClues(puzzle);
        if (clues < MIN_PUZZLE_CLUES) {
            throw new IllegalArgumentException(String.format(
                "Sudoku puzzle array has too few clues (%d, min is %d)",
                clues,
                MIN_PUZZLE_CLUES
            ));
        } else if (clues > MAX_PUZZLE_CLUES) {
            throw new IllegalArgumentException(String.format(
                "Sudoku puzzle array has too many clues (%d, max is %d)",
                clues,
                MAX_PUZZLE_CLUES
            ));
        }

        List<int[]> solutions = Solver.getSolutions(puzzle);
        if (solutions.isEmpty()) {
            throw new IllegalArgumentException("Sudoku puzzle has no solution.");
        }
        if (solutions.size() > 1) {
            throw new IllegalArgumentException("Sudoku puzzle has multiple solutions.");
        }

        return solutions.get(0);
    }
}
