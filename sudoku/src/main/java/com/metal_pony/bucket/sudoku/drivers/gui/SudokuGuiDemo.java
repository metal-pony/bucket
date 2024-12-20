package com.metal_pony.bucket.sudoku.drivers.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;

import com.metal_pony.bucket.sudoku.Sudoku;

public class SudokuGuiDemo {
	public static void show(Sudoku sudoku) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setBackground(Color.white);
		SudokuBoard panel = new SudokuBoard(sudoku);
		panel.setPreferredSize(new Dimension(600, 600));
		f.getContentPane().add(panel, BorderLayout.CENTER);

		JButton btn = new JButton("Solve");
		btn.addActionListener((event) -> {
			Sudoku solution = sudoku.firstSolution();

			if (solution == null) {
				System.out.println("null solution");
				return;
			}

			for (int i = 0; i < Sudoku.SPACES; i++) {
				panel.cells[i].digit = solution.getDigit(i);
			}
			panel.repaint();
		});
		f.getContentPane().add(btn, BorderLayout.SOUTH);

		f.pack();
		f.setVisible(true);

		System.out.println(sudoku.firstSolution().toString());
	}
}
