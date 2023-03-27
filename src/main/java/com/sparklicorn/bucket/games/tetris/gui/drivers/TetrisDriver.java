package com.sparklicorn.bucket.games.tetris.gui.drivers;

import com.sparklicorn.bucket.games.tetris.TetrisGame;
import com.sparklicorn.bucket.games.tetris.gui.components.TetrisFrame;

public class TetrisDriver {
	public static void main(String[] args) {
		new TetrisFrame(600, 800, 32, new TetrisGame(20, 10));
	}
}
