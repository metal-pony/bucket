package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.util.event.Event;

public class TetrisFrame extends JFrame implements KeyListener, WindowListener {
	private static final long SHIFT_DELAY = TimeUnit.MILLISECONDS.toNanos(100L);
	private static final long DROP_DELAY = TimeUnit.MILLISECONDS.toNanos(25L);

	protected TetrisGame game;
	protected TetrisBoardPanel panel;
	protected TetrisSidePanel sidePanel;

	protected Thread pieceMover;

	protected boolean moveLeft, moveRight, fastDrop;
	protected boolean shutdown;
	protected volatile boolean _moveLeft, _moveRight, _fastDrop;
	protected volatile boolean _shutdown;

	private Map<Integer,Runnable> keyPressedListeners;
	private Map<Integer,Runnable> keyReleasedListeners;

	public TetrisFrame(int width, int height, int blockSize, TetrisGame _game) {
		this.game = _game;
		this.shutdown = false;
		this._shutdown = shutdown;
		this.keyPressedListeners = new HashMap<>();
		this.keyReleasedListeners = new HashMap<>();
		this.panel = new TetrisBoardPanel(blockSize, game);
		this.sidePanel = new TetrisSidePanel(game, blockSize, TetrisSidePanel.DEFAULT_FONT_SIZE, Math.round((float)blockSize * 2f / 3f));
		this.pieceMover = new Thread(() -> {
			long nextLeft = 0L;
			long nextRight = 0L;
			long nextDown = 0L;

			while (!shutdown) {
				long t = System.nanoTime();
				if (moveLeft && t >= nextLeft) {
					nextLeft = t + SHIFT_DELAY;
					this.game.shift(0, -1);
				} else if (moveRight && t >= nextRight) {
					nextRight = t + SHIFT_DELAY;
					this.game.shift(0, 1);
				} else if (fastDrop && t >= nextDown) {
					nextDown = t + DROP_DELAY;
					this.game.shift(1, 0);
				}

				try {
					Thread.sleep(1000L / 60L); // TODO test whether this should be positive
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		});

		panel.setShouldDraw(false);
		panel.setMessage("Press ENTER To Start A New Game");
		panel.setBorder(BorderFactory.createLineBorder(TetrisBoardPanel.UIColor, 3));

		game.registerEventListener(TetrisEvent.NEW_GAME, this::onNewGame);
		game.registerEventListener(TetrisEvent.START, this::onStartGame);
		game.registerEventListener(TetrisEvent.GAME_OVER, this::onGameOver);
		game.registerEventListener(TetrisEvent.LEVEL_CHANGE, this::onLevelChange);
		game.registerEventListener(TetrisEvent.PAUSE, this::onPause);
		game.registerEventListener(TetrisEvent.RESUME, this::onResume);

		keyPressedListeners.put(KeyEvent.VK_Z, () -> this.game.rotateClockwise());
		keyPressedListeners.put(KeyEvent.VK_X, () -> this.game.rotateCounterClockwise());
		keyPressedListeners.put(KeyEvent.VK_RIGHT, this::startDriftingRight);
		keyPressedListeners.put(KeyEvent.VK_LEFT, this::startDriftingLeft);
		keyPressedListeners.put(KeyEvent.VK_DOWN, this::startFastDropping);
		keyPressedListeners.put(KeyEvent.VK_ENTER, this::handleEnterKeyPress);
		keyPressedListeners.put(KeyEvent.VK_ESCAPE, this::dispose);

		keyReleasedListeners.put(KeyEvent.VK_RIGHT, this::stopDriftingRight);
		keyReleasedListeners.put(KeyEvent.VK_LEFT, this::stopDriftingLeft);
		keyReleasedListeners.put(KeyEvent.VK_DOWN, this::stopFastDropping);

		setLayout(new GridBagLayout());
		add(panel, new GridBagConstraints(
			1,0, 1,1, 0.0,0.0, GridBagConstraints.CENTER,
			GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0,0
		));
		add(sidePanel, new GridBagConstraints(
			2,0, 1,1, 0.0,0.0, GridBagConstraints.CENTER,
			GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,0
		));

		pieceMover.start();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setResizable(false);
		addKeyListener(this);
		addWindowListener(this);
		getContentPane().setBackground(Color.BLACK);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void startDriftingRight() {
		moveRight = true;
		moveLeft = false;
		_moveRight = moveRight;
		_moveLeft = moveLeft;
	}

	private void startDriftingLeft() {
		moveRight = false;
		moveLeft = true;
		_moveRight = moveRight;
		_moveLeft = moveLeft;
	}

	private void startFastDropping() {
		fastDrop = true;
		_fastDrop = fastDrop;
	}

	private void stopDriftingRight() {
		moveRight = false;
		_moveRight = moveRight;
	}

	private void stopDriftingLeft() {
		moveLeft = false;
		_moveLeft = moveLeft;
	}

	private void stopFastDropping() {
		fastDrop = false;
		_fastDrop = fastDrop;
	}

	public TetrisBoardPanel getPanel() {
		return panel;
	}

	/*******************************
	 * TETRIS GAME EVENTS HANDLERS *
	 *******************************/
	private void onNewGame(Event event) {
		panel.setShouldDraw(false);
		panel.setMessage("Press ENTER To Start A New Game");
	}

	private void onStartGame(Event event) {
		panel.setShouldDraw(true);
	}

	private void onGameOver(Event event) {
		panel.setMessage("Game Over\nPress ENTER To Start A New Game");
	}

	private void onLevelChange(Event event) {}

	private void onPause(Event event) {
		panel.hideBlocks();
		panel.setShouldDraw(false);
		panel.setMessage("Paused");
		panel.repaint();
	}

	private void onResume(Event event) {
		panel.showBlocks();
		panel.setShouldDraw(true);
		panel.repaint();
	}

	/***************************
	 * KEYBOARD EVENT HANDLERS *
	 ***************************/
	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyPressed(KeyEvent e) {
		if (keyPressedListeners.containsKey(e.getKeyCode())) {
			keyPressedListeners.get(e.getKeyCode()).run();
		}
	}
	@Override public void keyReleased(KeyEvent e) {
		if (keyReleasedListeners.containsKey(e.getKeyCode())) {
			keyReleasedListeners.get(e.getKeyCode()).run();
		}
	}

	private void handleEnterKeyPress() {
		TetrisState state = game.getState();
		if (state.isGameOver) {
			game.newGame();
		} else if (!state.hasStarted) {
			game.start(0L, true);
		} else if (state.isPaused) {
			game.resume();
		} else {
			game.pause();
		}
	}

	/*************************
	 * WINDOW EVENT HANDLERS *
	 *************************/
	@Override public void windowOpened(WindowEvent e) {}
	@Override public void windowClosing(WindowEvent e) {}
	@Override public void windowClosed(WindowEvent e) {
		game.shutdown();
		shutdown = true;
		_shutdown = true;
	 }
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowActivated(WindowEvent e) {}
	@Override public void windowDeactivated(WindowEvent e) {}
}
