package com.sparklicorn.bucket.games.sudoku.game.generators;

import java.util.HashSet;

import com.sparklicorn.bucket.games.sudoku.game.*;

public class GenerateConfigs {

    public static void main(String[] args) {
        final int numConfigs = (args != null && args.length > 0) ? Math.max(Integer.parseInt(args[0]), 1) : 1; //Number of configs to generate.
        final boolean normalize = (args != null && args.length > 1) ? Boolean.parseBoolean(args[1]) : false;

        HashSet<Board> set = new HashSet<>();
        while (set.size() < numConfigs) {
            Generator.generateConfigs().stream().forEach((board) -> {
                if (set.size() < numConfigs) {
                    if (normalize) {
                        board = SudokuUtility.normalize(board);
                    }

                    if (set.add(board)) {
                        System.out.println(board.getSimplifiedString());
                    }
                }
            });
        }
    }
}
