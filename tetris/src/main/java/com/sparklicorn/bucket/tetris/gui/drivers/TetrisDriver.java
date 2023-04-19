package com.sparklicorn.bucket.tetris.gui.drivers;

import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.gui.components.TetrisFrame;

public class TetrisDriver {
	public static void main(String[] args) {
		new TetrisFrame(600, 800, 32, new TetrisGame(20, 10));
	}
}
