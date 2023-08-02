package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.JPanel;

import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.TetrisState;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.event.Event;

public class TetrisBoardPanel extends JPanel {
	protected static final int DEFAULT_BLOCK_SIZE = 24;

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

	protected int numRows;
	protected int numCols;
	protected int blockSize;
	protected Cell[] cells;

	protected boolean showBlocks;
	protected boolean shouldDraw;
	protected String message;

	protected TetrisGame game;
	protected TetrisState state;

	public TetrisBoardPanel(TetrisGame game) {
		this(DEFAULT_BLOCK_SIZE, game);
	}

	public TetrisBoardPanel(int blockSize, TetrisGame game) {
		if (game == null) {
			throw new IllegalArgumentException("game cannot be null");
		}

		showBlocks = true;
		shouldDraw = true;
		message = "";

		connectGame(game);
		setBlockSize(blockSize);
		setBackground(Color.BLACK);
		// setLayout(null);
		setVisible(true);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (shouldDraw) {
			if (showBlocks) {
				drawCells(g);
			}
			drawStats(g);
		} else if (!message.equals("")) {
			drawMessage(g);
		}
	}

	public void setBlockSize(int newBlockSize) {
		blockSize = newBlockSize;
		if (blockSize < 1) {
			blockSize = DEFAULT_BLOCK_SIZE;
		}

		setPreferredSize(new Dimension(numCols * blockSize, numRows * blockSize));
	}

	public TetrisGame connectGame(TetrisGame newGame) {
		if (newGame == null) {
			throw new IllegalArgumentException("newGame cannot be null");
		}

		TetrisGame oldGame = disconnectGame();
		game = newGame;
		state = game.getState();
		numRows = state.rows;
		numCols = state.cols;

		initCells();
		setPreferredSize(new Dimension(numCols * blockSize, numRows * blockSize));

		// Register new event handlers
		game.registerEventListener(TetrisEvent.LINE_CLEAR, this::onGameEvent);
		game.registerEventListener(TetrisEvent.BLOCKS, this::onGameEvent);
		game.registerEventListener(TetrisEvent.PIECE_CREATE, this::onGameEvent);
		game.registerEventListener(TetrisEvent.PIECE_ROTATE, this::onGameEvent);
		game.registerEventListener(TetrisEvent.PIECE_SHIFT, this::onGameEvent);
		game.registerEventListener(TetrisEvent.PIECE_PLACED, this::onGameEvent);

		return oldGame;
	}

	public TetrisGame disconnectGame() {
		if (game != null) {
			// Unregister all active event handlers
			Arrays.stream(TetrisEvent.values()).forEach(this::unregisterEvent);
		}

		return game;
	}

	public void hideBlocks() {
		showBlocks = false;
	}

	public void showBlocks() {
		showBlocks = true;
	}

	public void setShouldDraw(boolean isPaused) {
		shouldDraw = isPaused;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	protected void initCells() {
		cells = new Cell[numRows * numCols];
		for (int i = 0; i < cells.length; i++) {
			cells[i] = new Cell(i, i / numCols, i % numCols);
		}
	}

	protected void update(Event e) {
		this.state = e.getPropertyAsType("state", TetrisState.class);
		mapStateToCells();
	}

	protected void mapStateToCells() {
		for (Cell cell : cells) {
			cell.shape = Shape.getShape(state.board[cell.index]);
		}

		mapPieceStateToCells();
	}

	protected void mapPieceStateToCells() {
		if (!state.piece.isActive()) {
			return;
		}

		state.piece.forEachCell((row, col) -> cells[row * state.cols + col].shape = state.piece.shape());
	}

	protected void drawCells(Graphics g) {
		for (Cell cell : cells) {
			cell.draw(g);
		}
	}

	protected void drawMessage(Graphics g) {
		g.setColor(Color.WHITE);
		int x = getPreferredSize().width / 4;
		int y = getPreferredSize().height / 3;
		g.drawString(message, x, y);
	}

	protected void drawStats(Graphics g) {
		g.setColor(Color.MAGENTA);
		g.setFont(new Font("Arial", Font.BOLD, 12));
		int y;
		g.drawString("Score: " + state.score, 8, y=16);
		g.drawString("Level: " + state.level, 8, y+=20);
		g.drawString("Lines: " + state.linesCleared, 8, y+=20);
		g.drawString("Pieces: " + state.numPiecesDropped, 8, y+=20);
	}

	protected void onGameEvent(Event e) {
		update(e);
		repaint();
	}

	protected boolean unregisterEvent(TetrisEvent e) {
		return game.unregisterEventListener(e, this::onGameEvent);
	}
}
