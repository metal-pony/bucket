package com.sparklicorn.bucket.games.sudoku.drivers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import java.util.Arrays;

import com.sparklicorn.bucket.games.sudoku.game.Board;
import com.sparklicorn.bucket.games.sudoku.util.FileUtil;

public class CompressPuzzlesFiles {

    public static class PuzzlesFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isDirectory() &&
                !pathname.isHidden() &&
                pathname.getName().contains("puzzles") &&
                !pathname.getName().contains("compressed");
        }
    }

    public static void main(String[] args) {
        try {
            File pwd = new File(".").getCanonicalFile();
            Arrays.stream(
                pwd.listFiles(new PuzzlesFileFilter())
            ).forEach((puzzlesFile) -> {
                String fromPath = puzzlesFile.getAbsolutePath();
                String toPath = String.format(
                    "%s/%s",
                    pwd.getAbsolutePath(),
                    puzzlesFile.getName().replaceFirst("puzzles", "puzzles-compressed")
                );
                System.out.printf("Compressing puzzle file \"%s\" -> \"%s\"\n", fromPath, toPath);
                FileUtil.transformLinesInFile(
                    fromPath,
                    toPath,
                    (line) -> {
                        return new Board(line).getCompressedString();
                    },
                    true
                );
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
