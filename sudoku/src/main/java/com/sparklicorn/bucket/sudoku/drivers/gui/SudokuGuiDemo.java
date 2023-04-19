package com.sparklicorn.bucket.sudoku.drivers.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;

import com.sparklicorn.bucket.sudoku.game.Board;
// import com.sparklicorn.bucket.sudoku.game.generators.Generator;
import com.sparklicorn.bucket.sudoku.game.solvers.Solver;

public class SudokuGuiDemo {
	// public static void main(String[] args) {
	// 	int numClues = 27;
	// 	if (args != null && args.length >= 1) {
	// 		numClues = Integer.parseInt(args[0]);
	// 	}

	// 	Board puzzle = Generator.generatePuzzle(numClues);
	// 	System.out.println(puzzle == null ?
	// 		"Error. Timed out while generating puzzle." :
	// 		puzzle.toString()
	// 	);

	// 	if (puzzle != null) {
	// 		show(puzzle);
	// 	}
	// }

	public static void show(Board puzzle) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setBackground(Color.white);
		SudokuBoard panel = new SudokuBoard(puzzle);
		panel.setPreferredSize(new Dimension(600, 600));
		f.getContentPane().add(panel, BorderLayout.CENTER);

		JButton btn = new JButton("Solve");
		btn.addActionListener((event) -> {
			Board solution = Solver.solve(panel.board);

			if (solution == null) {
				System.out.println("null solution");
				return;
			}

			for (int i = 0; i < Board.NUM_CELLS; i++) {
				panel.cells[i].digit = solution.getValueAt(i);
			}
			panel.repaint();
		});
		f.getContentPane().add(btn, BorderLayout.SOUTH);

		f.pack();
		f.setVisible(true);

		System.out.println(Solver.solve(puzzle).getSimplifiedString());
	}
}
