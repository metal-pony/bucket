package com.metal_pony.bucket.sudoku;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.Gson;

public class PuzzleEntry {
    static final String RESOURCES_DIR = "resources";
    static final String PUZZLES_17_RESOURCE = "17-puzzle-records.json";

    String puzzle;
    String solution;
    String fingerprint2;
    String fingerprint3;
    String fingerprint4;

    public PuzzleEntry(
        String puzzle,
        String solution,
        String fingerprint2,
        String fingerprint3,
        String fingerprint4
    ) {
        this.puzzle = puzzle;
        this.solution = solution;
        this.fingerprint2 = fingerprint2;
        this.fingerprint3 = fingerprint3;
        this.fingerprint4 = fingerprint4;
    }

    public Sudoku puzzle() {
        return new Sudoku(puzzle);
    }

            solution = _solution.toString();
        }
        return solution;
    }

    /**
     * Gets the solution's fingerprint (level 2).
     * Computed if not already cached.
     * @return Solution's fingerprint.
     */
    public String fingerprint2() {
        if (fingerprint2 == null) {
            fingerprint2 = new Sudoku(solution()).fingerprint(2);
        }
        return fingerprint2;
    }

    /**
     * Gets the solution's fingerprint (level 3).
     * Computed if not already cached.
     * @return Solution's fingerprint.
     */
    public String fingerprint3() {
        if (fingerprint3 == null) {
            fingerprint3 = new Sudoku(solution()).fingerprint(3);
        }
        return fingerprint3;
    }

    /**
     * Gets the solution's fingerprint (level 4).
     * Computed if not already cached. Computation may block for several seconds.
     * @return Solution's fingerprint.
     */
    public String fingerprint4() {
        if (fingerprint4 == null) {
            fingerprint4 = new Sudoku(solution()).fingerprint(4);
        }
        return fingerprint4;
    }

    @Override
    public String toString() {
        return String.format(
            """
            {
              "puzzle":       "%s",
              "solution":     "%s",
              "fingerprint2": "%s",
              "fingerprint3": "%s",
              "fingerprint4": "%s"
            }""",
            puzzle, solution(), fingerprint2(), fingerprint3(), fingerprint4()
        );
    }

    /**
     * Reads Puzzle entries from the given inputstream.
     * @param inStream
     * @return Array of PuzzleEntries read from the stream.
     */
    public static PuzzleEntry[] readFromJsonInStream(InputStream inStream) {
        try (Reader reader = new InputStreamReader(inStream)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, PuzzleEntry[].class);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            return new PuzzleEntry[0];
        }
    }

    /**
     * Reads all puzzle entries from 'resources/17-puzzle-records.json'.
     * @return Array of all PuzzleEntry.
     */
    public static PuzzleEntry[] all17() {
        return PuzzleEntry.readFromJsonInStream(
            PuzzleEntry.class.getResourceAsStream(String.format(
                "/%s/%s",
                RESOURCES_DIR,
                PUZZLES_17_RESOURCE
            ))
        );
    }
}
