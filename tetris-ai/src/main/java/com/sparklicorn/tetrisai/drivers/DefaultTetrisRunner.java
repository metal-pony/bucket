package com.sparklicorn.tetrisai.drivers;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import com.sparklicorn.bucket.util.ThreadPool;
import com.sparklicorn.tetrisai.game.AiTetris;
import com.sparklicorn.tetrisai.game.GenericRanker;
import com.sparklicorn.tetrisai.gui.AiSidePanel;
import com.sparklicorn.tetrisai.gui.AiTetrisPanel;
import com.sparklicorn.tetrisai.structs.GameStats;

public class DefaultTetrisRunner extends JFrame implements KeyListener {

	private static final long serialVersionUID = 0L;

	public static DefaultTetrisRunner createAndShow() {
		DefaultTetrisRunner runner = new DefaultTetrisRunner();
		runner.setVisible(true);
		return runner;
	}

	GenericRanker ranker;
	AiTetris tetris;

	AiTetrisPanel panel;
	AiSidePanel sidePanel;

	public DefaultTetrisRunner() {
		super("Tetris Runner");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addKeyListener(this);

		this.ranker = new GenericRanker();
		System.out.println("original ranker: " + ranker.addr());
		this.tetris = new AiTetris(
			AiTetris.DEFAULT_NUM_ROWS,
			AiTetris.DEFAULT_NUM_COLS,
			true,
			ranker,
			null
		);
		this.panel = new AiTetrisPanel(tetris);
		panel.setBorder(BorderFactory.createLineBorder(Color.MAGENTA, 2));

		boolean showNextPiece = true;
		boolean showLevel = false;
		boolean showScore = false;
		this.sidePanel = new AiSidePanel(tetris, showNextPiece, showLevel, showScore);

		setLayout(new GridBagLayout());
		getContentPane().setBackground(Color.BLACK);
		add(panel, new GridBagConstraints(
			1,0, 1,1, 0.0,0.0, GridBagConstraints.CENTER,
			GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0,0
		));
		add(sidePanel, new GridBagConstraints(
			2,0,
			1,1,
			0.0,0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 0, 0, 0),
			0,0
		));
		pack();
		setLocationRelativeTo(null);
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(true);
		outputHelp();
	}

	@Override
	public void dispose() {
		super.dispose();
		tetris.shutdown();
		ThreadPool.shutdownNow();
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			placeNextPiece(tetris, true);
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			tetris.stop();
			tetris.newGame();
			tetris.start(0);
		} else if (e.getKeyCode() == KeyEvent.VK_G) {
			asyncRunGame(tetris, 1, false);
		} else if (e.getKeyCode() == KeyEvent.VK_H) {
			asyncRunGame(tetris, 1, true);
		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			tetris.shutdown();
			dispose();
		}
	}

	public void outputHelp() {
		System.out.println("""
		KEY MAPPINGS:
			[ SPACE ]  Run piece placement
			[ R ]      Reset game
			[ G ]      Run game (look-ahead disabled in piece placement algorithm)
			[ H ]      Run game (look-ahead enabled)
			[ ESCAPE ] Leave this place and never return
		""");
	}

	private static void placeNextPiece(AiTetris g1, boolean useLookAhead) {
		g1.placeBest(false);
		g1.gameloop();
	}

	public Future<GameStats> runGame(long sleepTime, boolean useLookAhead) {
		return asyncRunGame(tetris, sleepTime, useLookAhead);
	}

	public static Future<GameStats> asyncRunGame(AiTetris g1, long sleepTime, boolean useLookAhead) {
		return ThreadPool.submit(() -> g1.run(sleepTime, useLookAhead));
	}
}
