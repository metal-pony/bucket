package com.metal_pony.bucket.sudoku;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.metal_pony.bucket.util.ThreadPool;

public class PuzzleEntry {
    static final String RESOURCES_DIR = "resources";
    static final String PUZZLES_17_JSON_RESOURCE = "17-puzzle-records.json";
    static final String PUZZLES_17_RESOURCE = "sudoku-17.txt";

    static InputStream puzzles17Stream() {
        String rscName = String.format("/%s/%s", RESOURCES_DIR, PUZZLES_17_RESOURCE);
        return PuzzleEntry.class.getResourceAsStream(rscName);
    }

    String puzzle;
    Sudoku _puzzle;
    String solution;
    Sudoku _solution;

    String dc2, dc3, dc4;
    String ac2, ac3, ac4;
    String fp2, fp3, fp4;

    public PuzzleEntry(String puzzle) {
        this.puzzle = puzzle;
    }

    public PuzzleEntry(
        String puzzle,
        String solution,
        String dc2, String dc3, String dc4,
        String ac2, String ac3, String ac4,
        String fp2, String fp3, String fp4
    ) {
        this.puzzle = puzzle;
        this.solution = solution;
        this.dc2 = dc2;
        this.dc3 = dc3;
        this.dc4 = dc4;
        this.ac2 = ac2;
        this.ac3 = ac3;
        this.ac4 = ac4;
        this.fp2 = fp2;
        this.fp3 = fp3;
        this.fp4 = fp4;
    }

    public String puzzleStr() { return puzzle; }
    public Sudoku puzzle() {
        return (_puzzle == null) ? (_puzzle = new Sudoku(puzzle)) : _puzzle;
    }

    public String solutionStr() {
        if (solution == null || solution.isBlank()) {
            _solution = puzzle().solution();
            solution = _solution.toString();
        }
        return solution;
    }

    public Sudoku solution() {
        if (solution == null || solution.isBlank()) {
            _solution = puzzle().solution();
            solution = _solution.toString();
        } else if (_solution == null) {
            _solution = puzzle().solution();
        }
        return _solution;
    }

    public void clear() {
        solution = null;
        dc2 = null; dc3 = null; dc4 = null;
        ac2 = null; ac3 = null; ac4 = null;
        fp2 = null; fp3 = null; fp4 = null;
    }

    public String dc2() { return (dc2 == null) ? (dc2 = solution().dc2()) : dc2; }
    public String dc3() { return (dc3 == null) ? (dc3 = solution().dc3()) : dc3; }
    public String dc4() { return (dc4 == null) ? (dc4 = solution().dc4()) : dc4; }
    public String ac2() { return (ac2 == null) ? (ac2 = solution().ac2()) : ac2; }
    public String ac3() { return (ac3 == null) ? (ac3 = solution().ac3()) : ac3; }
    public String ac4() { return (ac4 == null) ? (ac4 = solution().ac4()) : ac4; }
    public String fp2() { return (fp2 == null) ? (fp2 = solution().fp2()) : fp2; }
    public String fp3() { return (fp3 == null) ? (fp3 = solution().fp3()) : fp3; }
    public String fp4() { return (fp4 == null) ? (fp4 = solution().fp4()) : fp4; }

    private static final String JSON_FORMAT = """
    {
      "puzzle":   "%s",
      "solution": "%s",
      "dc2":      "%s",
      "dc3":      "%s",
      "fp3":      "%s"
    }""";
    @Override
    public String toString() {
        return String.format(JSON_FORMAT, puzzleStr(), solutionStr(), dc2(), dc3(), fp3());
    }

    private static final String CSV_FORMAT = "%s,%s,%s,%s";//,%s";
    public String toCsv() {
        return String.format(CSV_FORMAT, puzzleStr(), solutionStr(), dc2(), dc3());//, fp3());
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
                PUZZLES_17_JSON_RESOURCE
            ))
        );
    }

    public static Stream<PuzzleEntry> allSudoku17AsStream() {
        Scanner scanner = new Scanner(
            PuzzleEntry.class.getResourceAsStream(String.format(
                "/%s/%s",
                RESOURCES_DIR,
                PUZZLES_17_RESOURCE
            ))
        );
        return Stream.generate(() -> {
            try {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (Sudoku.isValidStr(line)) {
                        return new PuzzleEntry(line);
                    } else {
                        System.err.println("failed to create sudoku from file line\n" + line);
                        return null;
                    }
                } else {
                    scanner.close();
                    return null;
                }
            } catch (IllegalStateException ex) {
                // The scanner may be closed
                return null;
            }
        });
    }

    public static void buildCSV(String outFilePath, int numThreads) {
        ThreadPool.setSizeAndStart(numThreads);
        Queue<Future<String>> entries = new LinkedList<>();

        try (
            InputStream sudoku17inStream = puzzles17Stream();
            Scanner sudoku17Scanner = new Scanner(sudoku17inStream);
        ) {
            while (sudoku17Scanner.hasNextLine()) {
                String line = sudoku17Scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                entries.offer(ThreadPool.submit(() -> new PuzzleEntry(line).toCsv()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ThreadPool.shutdown();

        try (
            PrintWriter fOut = new PrintWriter(outFilePath);
        ) {
            int n = 0;
            while (!entries.isEmpty()) {
                Future<String> entry = entries.poll();
                String entryStr = entry.get();
                System.out.printf("[%d] %s\n", n, entryStr);
                fOut.println(entryStr);
                n++;
            }
        } catch (FileNotFoundException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
