package com.sparklicorn.tetrisai.drivers;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.util.ThreadPool;
import com.sparklicorn.tetrisai.game.AiTetris;
import com.sparklicorn.tetrisai.game.FixedShapeGenerator;
import com.sparklicorn.tetrisai.game.GenericRanker;
import com.sparklicorn.tetrisai.game.PolyFuncRanker;
import com.sparklicorn.tetrisai.structs.MutatingPolyFunc;
import com.sparklicorn.tetrisai.structs.PolyFunc.PolyFuncTerm;

public class DefaultTetrisRunner {
	public static void show() {
		GenericRanker ranker2 = new GenericRanker(
			new double[] { 3.589, -1.374, -17.409, -12.835, -10.748 }
		);

		PolyFuncRanker polyRanker = new PolyFuncRanker(
			new MutatingPolyFunc[] {
				new MutatingPolyFunc(new PolyFuncTerm(58.134255, 7.880358)),
				new MutatingPolyFunc(new PolyFuncTerm(30.695983, -4.800810)),
				new MutatingPolyFunc(new PolyFuncTerm(43.369132, -8.567011)),
				new MutatingPolyFunc(new PolyFuncTerm(-52.209781, 1.879746)),
				new MutatingPolyFunc(new PolyFuncTerm(30.562679, 5.511573))
			}
		);

		FixedShapeGenerator.init();

		AiTetris tetris = new AiTetris(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			true,
			ranker2,
			FixedShapeGenerator.get()
		);

		AiTetris tetris2 = new AiTetris(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			true,
			polyRanker,
			FixedShapeGenerator.get()
		);

		TetrisBoardPanel tetrisPanel = new TetrisBoardPanel(40, tetris);
		// TetrisBoardPanel tetris2Panel = new TetrisBoardPanel(40, tetris2);

		JPanel p = new JPanel();
		p.setLayout(new GridLayout(1, 2, 4, 4));
		p.add(tetrisPanel);
		// p.add(tetris2Panel);

		JFrame frame = new JFrame("Tetris Run Tester") {
			@Override public void dispose() {
				super.dispose();
				tetris.stop();
				tetris.shutdown();
				ThreadPool.shutdownNow();
				// tetris2.stop();
				// tetris2.shutdown();
			}
		};
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		frame.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					//runGames(tetris, tetris2, 1, true);

					placeNextPieces(tetris, tetris2, true);
				} else if (e.getKeyCode() == KeyEvent.VK_R) {
					tetris.stop();
					tetris.newGame();
					tetris.start(0);

					// tetris2.stop();
					// tetris2.newGame();
					// tetris2.start(0);
				} else if (e.getKeyCode() == KeyEvent.VK_G) {
					runGames(tetris, tetris2, 1, false);
				} else if (e.getKeyCode() == KeyEvent.VK_H) {
					runGames(tetris, tetris2, 1, true);
				}
			}
		});

		frame.add(p);
		frame.pack();
		frame.setVisible(true);

		//thread1.start();
		//thread2.start();
		runGames(tetris, tetris2, 1, false);
	}

	private static void placeNextPieces(AiTetris g1, AiTetris g2, boolean useLookAhead) {
		g1.placeBest(useLookAhead);
		g1.gameloop();

		// g2.placeBest(useLookAhead);
		// g2.gameloop();
	}

	private static void runGames(AiTetris g1, AiTetris g2, long sleepTime, boolean useLookAhead) {
		FixedShapeGenerator.init();
		new Thread(() -> {
			g1.run(sleepTime, useLookAhead);
		}).start();

		// new Thread(() -> {
		//     g2.run(sleepTime, useLookAhead);
		// }).start();
	}
}
