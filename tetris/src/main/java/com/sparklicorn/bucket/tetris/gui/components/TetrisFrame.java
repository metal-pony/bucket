package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import com.sparklicorn.bucket.tetris.ITetrisGame;
import com.sparklicorn.bucket.tetris.TetrisEvent;

public class TetrisFrame extends JFrame {
	protected ITetrisGame game;
	protected TetrisBoardPanel panel;
	protected TetrisSidePanel sidePanel;

	protected Thread pieceMover;
	protected volatile boolean moveLeft, moveRight, fastDrop;
	protected volatile boolean shutdown;

	public TetrisFrame(int width, int height, int blockSize, ITetrisGame _game) {

		TetrisFrame _this = this;

		game = _game;
		shutdown = false;

		pieceMover = new Thread(() -> {

			long nextLeft = 0L;
			long nextRight = 0L;
			long nextDown = 0L;

			while (!shutdown) {
				long t = System.nanoTime();
				if (moveLeft && t >= nextLeft) {
					nextLeft = t + TimeUnit.MILLISECONDS.toNanos(100L);
					game.shift(0, -1);

				} else if (moveRight && t >= nextRight) {
					nextRight = t + TimeUnit.MILLISECONDS.toNanos(100L);
					game.shift(0, 1);

				} else if (fastDrop && t >= nextDown) {
					nextDown = t + TimeUnit.MILLISECONDS.toNanos(25L);
					game.shift(1, 0);

				}

				try {
					Thread.sleep(0);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		});

		pieceMover.start();

		//setSize(width, height);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setResizable(false);

		addWindowListener(new WindowListener() {
			@Override public void windowOpened(WindowEvent e) {}
			@Override public void windowClosing(WindowEvent e) {}
			@Override public void windowClosed(WindowEvent e) {
				game.shutdown();
				shutdown = true;
			}
			@Override public void windowIconified(WindowEvent e) {}
			@Override public void windowDeiconified(WindowEvent e) {}
			@Override public void windowActivated(WindowEvent e) {}
			@Override public void windowDeactivated(WindowEvent e) {}
		});

		panel = new TetrisBoardPanel(blockSize, game);
		panel.setShouldDraw(false);
		panel.setMessage("Press ENTER To Start A New Game");

		//handlers
		game.registerEventListener(TetrisEvent.NEW_GAME, (event) -> {
			panel.setShouldDraw(false);
			panel.setMessage("Press ENTER To Start A New Game");
			//System.out.println("NEW GAME!");
		});

		game.registerEventListener(TetrisEvent.START, (event) -> {
			panel.setShouldDraw(true);
			//System.out.println("GAME STARTED!");
		});

		game.registerEventListener(TetrisEvent.GAME_OVER, (event) -> {
			//panel.setShouldDraw(false);
			panel.setMessage("Game Over\nPress ENTER To Start A New Game");
			//System.out.println("GAME OVER");
			//System.out.println("LEVEL: " + game.getLevel());
			//System.out.println("SCORE: " + game.getScore());
			//System.out.println("LINES CLEARED: " + game.getLinesCleared());
		});

		game.registerEventListener(TetrisEvent.LEVEL_CHANGE, (event) -> {
			//System.out.println("LEVEL INCREASED! NEW LEVEL: " + game.getLevel());
		});

		game.registerEventListener(TetrisEvent.PAUSE, (event) -> {
			panel.hideBlocks();
			panel.setShouldDraw(false);
			panel.setMessage("Paused");
			panel.repaint();
		});

		game.registerEventListener(TetrisEvent.RESUME, (event) -> {
			panel.stopHidingBlocks();
			panel.setShouldDraw(true);
			panel.repaint();
		});

		//KEYBOARD INPUT
		addKeyListener(new KeyListener() {
			@Override public void keyPressed(KeyEvent arg0) {
				int key = arg0.getKeyCode();
				switch (key) {
				case KeyEvent.VK_Z:
					game.rotateClockwise();
					break;
				case KeyEvent.VK_X:
					game.rotateCounterClockwise();
					break;
				case KeyEvent.VK_RIGHT:
					moveRight = true;
					moveLeft = false;
					break;
				case KeyEvent.VK_LEFT:
					moveRight = false;
					moveLeft = true;
					break;
				case KeyEvent.VK_DOWN:
					fastDrop = true;
					//game.setGameLoopDelay(game.getGameLoopDelayByLevel(game.getLevel()) >> 3);
					break;
				case KeyEvent.VK_ENTER:
					if (game.isGameOver())			//game is over
						game.newGame();
					else if (!game.hasStarted())	//not over but hasn't started
						game.start(0);
					else if (game.isPaused())		//not over, has started, is paused
						game.resume();
					else							//not over, has started, not paused
						game.pause();
					break;
				case KeyEvent.VK_ESCAPE:
					_this.dispose();
					break;
				}
			}

			@Override public void keyReleased(KeyEvent arg0) {
				int key = arg0.getKeyCode();
				switch (key) {
				case KeyEvent.VK_RIGHT:
					moveRight = false;
					break;
				case KeyEvent.VK_LEFT:
					moveLeft = false;
					break;
				case KeyEvent.VK_DOWN:
					fastDrop = false;
					break;
				}
			}
			@Override public void keyTyped(KeyEvent arg0) {}
		});

		panel.setBorder(BorderFactory.createLineBorder(TetrisBoardPanel.UIColor, 3));
		sidePanel = new TetrisSidePanel(blockSize, game);

		this.setLayout(new GridBagLayout());
		this.add(panel, new GridBagConstraints(
				1,0, 1,1, 0.0,0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0,0));

		this.add(sidePanel, new GridBagConstraints(
				2,0, 1,1, 0.0,0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,0));

		// this.setUndecorated(true);
		// this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.getContentPane().setBackground(Color.BLACK);
		this.pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public TetrisBoardPanel getPanel() {
		return panel;
	}
}
