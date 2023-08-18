package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.JPanel;

import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.util.structs.Piece;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.tetris.util.structs.ShapeQueue;
import com.sparklicorn.bucket.util.event.Event;

//! Requirement: Connected TetrisGame must use <= 1 eventBus worker thread. Multiple threads
//! may result in event hooks being called out of order, which can cause the panel's
//! state to be inconsistent with the game's state.
public class TetrisBoardPanel extends JPanel {
	public static final int DEFAULT_BLOCK_SIZE = 24;

	protected static final Color UIColor = new Color(0.0f, 0.0f, 1.0f);
	// O = 1, I = 2, S = 3, Z = 4, L = 5, J = 6, T = 7;
	protected static final Color[] COLORS_BY_SHAPE = {
		new Color(1f, 1f, 1f, 0.5f),
		Color.YELLOW,
		Color.CYAN,
		Color.GREEN,
		Color.RED,
		new Color(255, 165, 0),
		Color.BLUE,
		new Color(128,0,128)
	};

	protected static Color colorForShape(Shape shape) {
		int shapeIndex = (shape == null) ? 0 : shape.value;
		return COLORS_BY_SHAPE[shapeIndex % COLORS_BY_SHAPE.length];
	}

	protected static Color colorWithAlhpa(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    protected Color currentGamePieceColor() {
		return colorForShape(state.piece.shape());
    }

	protected class Cell {
		protected final int index;
		protected final int row;
		protected final int col;
		protected Shape shape;

		protected Cell(int index, int row, int col) {
			this.index = index;
			this.row = row;
			this.col = col;
		}

		protected Color color() {
			return colorForShape(shape);
		}

		protected void draw(Graphics g) {
			if (shape == null) {
				return;
			}

			int x = col * blockSize;
			int y = row * blockSize;
			g.setColor(color());
			g.fill3DRect(x, y, blockSize, blockSize, true);
		}
	}

	protected int blockSize;
	protected Cell[] cells;

	protected String message;

	protected TetrisGame game;
	protected TetrisState state;

	protected JPanel tetrisPanel;
	protected TetrisSidePanel sidePanel;

	protected boolean drawStats;

	public TetrisBoardPanel() {
		this((TetrisState)null, DEFAULT_BLOCK_SIZE, false);
	}

	public TetrisBoardPanel(TetrisGame game) {
		this(game, DEFAULT_BLOCK_SIZE, false);
	}

	public TetrisBoardPanel(TetrisState state) {
		this(state, DEFAULT_BLOCK_SIZE, false);
	}

	public TetrisBoardPanel(TetrisState state, int blockSize, boolean useSidePanel) {
		this.drawStats = true;

		tetrisPanel = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				drawCells(g);
				drawStats(g);
				drawMessage(g);
			}
		};

		message = "";

		setState(state);
		setBackground(Color.BLACK);
		tetrisPanel.setBackground(Color.BLACK);

		setLayout(new GridBagLayout());
		add(tetrisPanel, new GridBagConstraints(
			1,0, 1,1, 0.0,0.0, GridBagConstraints.CENTER,
			GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0,0
		));

		if (useSidePanel) {
			sidePanel = new TetrisSidePanel(this);
			add(sidePanel, new GridBagConstraints(
				2,0, 1,1, 0.0,0.0, GridBagConstraints.NORTH,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,0
			));
		}

		setVisible(true);
	}

	public TetrisBoardPanel(TetrisGame game, int blockSize, boolean useSidePanel) {
		this(game.getState(), blockSize, useSidePanel);

		connectGame(game);
	}

	public boolean drawStats() {
		return drawStats;
	}

	public void drawStats(boolean drawStats) {
		this.drawStats = drawStats;
	}

	public void setBlockSize(int newBlockSize) {
		blockSize = newBlockSize;
		if (blockSize < 1) {
			blockSize = DEFAULT_BLOCK_SIZE;
		}

		tetrisPanel.setPreferredSize(new Dimension(
			((state == null) ? DEFAULT_BLOCK_SIZE : state.cols) * blockSize,
			((state == null) ? DEFAULT_BLOCK_SIZE : state.rows) * blockSize
		));

		if (sidePanel != null) {
			sidePanel.setBlockSize(newBlockSize);
		}
	}

	public TetrisState setState(TetrisState state) {
		TetrisState oldState = this.state;
		this.state = state;

		initCells();
		setBlockSize(blockSize);

		if (state != null) {
			mapStateToCells();
			mapPieceStateToCells();
			repaint();
		}

		return oldState;
	}

	public TetrisGame connectGame(TetrisGame newGame) {
		if (newGame == null) {
			throw new IllegalArgumentException("newGame cannot be null");
		}

		TetrisGame oldGame = disconnectGame();
		game = newGame;
		state = game.getState();

		initCells();
		setBlockSize(blockSize);

		// Register new event handlers
		game.registerEventListener(TetrisEvent.NEW_GAME, this::onNewGame);
		game.registerEventListener(TetrisEvent.START, this::onStart);
		game.registerEventListener(TetrisEvent.STOP, this::onStop);
		game.registerEventListener(TetrisEvent.RESET, this::onReset);
		game.registerEventListener(TetrisEvent.PAUSE, this::onPause);
		game.registerEventListener(TetrisEvent.RESUME, this::onResume);
		game.registerEventListener(TetrisEvent.GAME_OVER, this::onGameOver);
		game.registerEventListener(TetrisEvent.GRAVITY_ENABLED, this::onGravityEnabled);
		game.registerEventListener(TetrisEvent.GRAVITY_DISABLED, this::onGravityDisabled);

		game.registerEventListener(TetrisEvent.GAMELOOP, this::onGameloop);

		game.registerEventListener(TetrisEvent.LINE_CLEAR, this::onLineClear);
		game.registerEventListener(TetrisEvent.LEVEL_CHANGE, this::onLevelUpdate);
		game.registerEventListener(TetrisEvent.SCORE_UPDATE, this::onScoreUpdate);
		game.registerEventListener(TetrisEvent.BLOCKS, this::onBlocksUpdate);
		game.registerEventListener(TetrisEvent.PIECE_CREATE, this::onPieceCreate);
		game.registerEventListener(TetrisEvent.PIECE_ROTATE, this::onPieceRotate);
		game.registerEventListener(TetrisEvent.PIECE_SHIFT, this::onPieceShift);
		game.registerEventListener(TetrisEvent.PIECE_PLACED, this::onPiecePlaced);

		return oldGame;
	}

	public TetrisGame disconnectGame() {
		if (game != null) {
			// Unregister all active event handlers
			Arrays.stream(TetrisEvent.values()).forEach(game::unregisterEvent);
		}

		return game;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	protected void initCells() {
		int rows = (state == null) ? TetrisState.DEFAULT_NUM_ROWS : state.rows;
		int cols = (state == null) ? TetrisState.DEFAULT_NUM_COLS : state.cols;
		cells = new Cell[rows * cols];
		for (int i = 0; i < cells.length; i++) {
			cells[i] = new Cell(i, i / cols, i % cols);
		}
	}

	protected void drawCells(Graphics g) {
		if (state == null || state.isPaused || !state.hasStarted) {
			return;
		}

		for (Cell cell : cells) {
			cell.draw(g);
		}
	}

	protected void drawMessage(Graphics g) {
		if (
			state == null ||
			message.isEmpty() ||
			(!state.isPaused && state.isRunning())
		) {
			return;
		}

		g.setColor(Color.WHITE);
		int x = tetrisPanel.getPreferredSize().width / 4;
		int y = tetrisPanel.getPreferredSize().height / 3;
		g.drawString(message, x, y);
	}

	protected void drawStats(Graphics g) {
		if (state == null || !drawStats || state.isPaused || !state.hasStarted) {
			return;
		}

		g.setColor(Color.MAGENTA);
		g.setFont(new Font("Arial", Font.BOLD, 12));
		int y;
		g.drawString("Score: " + state.score, 8, y=16);
		g.drawString("Level: " + state.level, 8, y+=20);
		g.drawString("Lines: " + state.linesCleared, 8, y+=20);
		g.drawString("Pieces: " + state.numPiecesDropped, 8, y+=20);
	}

	protected void onStateUpdate(Event e) {
		this.state = e.getPropertyAsType("state", TetrisState.class);
		update(e);
		repaint();
	}

	public void update(Event e) {
		mapStateToCells();
		mapPieceStateToCells();
	}

	public void mapStateToCells() {
		if (state == null) {
			return;
		}

		for (Cell cell : cells) {
			cell.shape = Shape.getShape(state.board[cell.index]);
		}
	}

	public void mapPieceStateToCells() {
		if (state == null || !state.piece.isActive()) {
			return;
		}

		state.piece.forEachCell((coord) -> cells[coord.row() * state.cols + coord.col()].shape = state.piece.shape());
	}

	/*******************************
	 * TETRIS GAME EVENTS HANDLERS *
	 *******************************/

	protected void onNewGame(Event e) {
		onStateUpdate(e);
	}

	protected void onStart(Event e) {
		onStateUpdate(e);
	}

	protected void onStop(Event e) {
		onStateUpdate(e);
	}

	protected void onPause(Event e) {
		state.isPaused = true;
		setMessage("Paused");
		repaint();
	}

	protected void onResume(Event e) {
		state.isPaused = false;
		setMessage("");
		repaint();
	}

	protected void onReset(Event e) {
		onStateUpdate(e);
	}

	protected void onGameOver(Event e) {
		onStateUpdate(e);
	}

	protected void onGravityEnabled(Event e) {
		repaint();
	}

	protected void onGravityDisabled(Event e) {
		repaint();
	}

	protected void onGameloop(Event e) {
		onStateUpdate(e);
	}

	protected void onScoreUpdate(Event e) {
		state.score = e.getPropertyAsType("score", Long.class);
		if (sidePanel != null) {
			sidePanel.updateScoreLabel(state.score);
		}
		repaint();
	}

	protected void onLevelUpdate(Event e) {
		state.level = e.getPropertyAsType("level", Long.class);
		if (sidePanel != null) {
			sidePanel.updateLevelLabel(state.level);
		}
		repaint();
	}

	protected void onLineClear(Event e) {
		state.linesCleared = e.getPropertyAsType("linesCleared", Long.class);
		state.linesUntilNextLevel = e.getPropertyAsType("linesUntilNextLevel", Integer.class);
		repaint();
	}

	protected void onBlocksUpdate(Event e) {
		state.board = e.getPropertyAsType("blocks", int[].class);
		update(e);
		repaint();
	}

	protected void onPieceCreate(Event e) {
		state.piece = e.getPropertyAsType("piece", Piece.class);
		state.nextShapes = e.getPropertyAsType("nextShapes", ShapeQueue.class);
		update(e);
		repaint();
	}

	protected void onPieceRotate(Event e) {
		state.piece = e.getPropertyAsType("piece", Piece.class);
		update(e);
		repaint();
	}

	protected void onPieceShift(Event e) {
		state.piece = e.getPropertyAsType("piece", Piece.class);
		update(e);
		repaint();
	}

	protected void onPiecePlaced(Event e) {
		state.piece = e.getPropertyAsType("piece", Piece.class);
		state.numPiecesDropped = e.getPropertyAsLong("numPiecesDropped");
		update(e);
		repaint();
	}
}
