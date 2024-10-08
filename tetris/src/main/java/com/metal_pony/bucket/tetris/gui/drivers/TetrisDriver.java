package com.metal_pony.bucket.tetris.gui.drivers;

import com.metal_pony.bucket.tetris.TetrisGame;
import com.metal_pony.bucket.tetris.gui.components.TetrisFrame;

public class TetrisDriver {
	public static void main(String[] args) {
		new TetrisFrame(600, 800, 32, new TetrisGame(20, 10));
	}
}
