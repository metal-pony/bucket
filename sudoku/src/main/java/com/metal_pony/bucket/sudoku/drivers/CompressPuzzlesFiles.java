package com.metal_pony.bucket.sudoku.drivers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.metal_pony.bucket.sudoku.game.Board;
import com.metal_pony.bucket.sudoku.util.FileUtil;

public class CompressPuzzlesFiles {
    public static Predicate<Path> puzzleFileFilter = (pathname) -> (
        !pathname.toFile().isDirectory() &&
        !pathname.toFile().isHidden() &&
        pathname.toFile().getName().contains("puzzles") &&
        !pathname.toFile().getName().contains("compressed")
    );

    public static Consumer<Path> compressPuzzlesFile = (puzzleFilePath) -> {
        String from = puzzleFilePath.toString();
        String to = puzzleFilePath.getFileName().toString().replaceFirst("puzzles", "puzzles-compressed");
        Function<String,String> puzzleStrTransformer = (puzzleStr) -> new Board(puzzleStr).getCompressedString();
        System.out.printf("Compressing puzzle file \"%s\" -> \"%s\"\n", from, to);
        FileUtil.transformLinesInFile(from, to, puzzleStrTransformer, true);
    };

    public static void compressFilesInDir(Path dir) throws IOException {
        Files.list(dir).filter(puzzleFileFilter).forEach(compressPuzzlesFile);
    }

    // public static void main(String[] args) throws IOException {
    //     compressFilesInDir(Path.of("."));
    // }
}
