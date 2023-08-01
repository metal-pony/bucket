package com.sparklicorn.tetrisai.drivers;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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

		ranker = new GenericRanker();
		tetris = new AiTetris();
		tetris.setRanker(ranker);
		panel = new AiTetrisPanel(tetris);
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

	public AiTetrisPanel getPanel() {
		return panel;
	}

	public AiSidePanel getSidePanel() {
		return sidePanel;
	}

	public AiTetris getGame() {
		return tetris;
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
			placeNextPiece(true);
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			tetris.stop();
			tetris.newGame();
			tetris.start(0);
		} else if (e.getKeyCode() == KeyEvent.VK_G) {
			runGame(0L, false);
		} else if (e.getKeyCode() == KeyEvent.VK_H) {
			runGame(0L, true);
		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
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

	private void placeNextPiece(boolean useLookAhead) {
		tetris.placeBest(false);
		tetris.gameloop();
	}

	public GameStats runGame(long sleepTime, boolean useLookAhead) {
		try {
			if (!tetris.isRunning()) {
				return tetris.run(sleepTime, useLookAhead);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
