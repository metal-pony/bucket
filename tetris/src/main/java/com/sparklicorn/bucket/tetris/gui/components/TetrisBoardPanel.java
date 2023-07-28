package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.JPanel;

import com.sparklicorn.bucket.tetris.ITetrisGame;
import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.util.structs.Coord;
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

	protected class Cell {
		protected final int index;
		protected final int row;
		protected final int col;
		protected int shapeIndex;

		protected Cell(int index, int row, int col) {
			this.index = index;
			this.row = row;
			this.col = col;
		}

		protected Color color() {
			return COLORS_BY_SHAPE[shapeIndex % COLORS_BY_SHAPE.length];
		}

		protected void draw(Graphics g) {
			if (shapeIndex <= 0) {
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
	protected int[] blocks;
	protected Cell[] cells;
	// protected Cell[][] rows;
	// protected Cell[][] columns;

	protected boolean hidingBlocks;
	protected boolean shouldDraw;
	protected String message;
	protected ITetrisGame game;

	public TetrisBoardPanel() {
		this(DEFAULT_BLOCK_SIZE, null);
	}

	public TetrisBoardPanel(ITetrisGame game) {
		this(DEFAULT_BLOCK_SIZE, game);
	}

	public TetrisBoardPanel(int blockSize, ITetrisGame game) {
		this.game = game;

		if (game != null) {
			numRows = game.getNumRows();
			numCols = game.getNumCols();
		} else {
			numRows = TetrisGame.DEFAULT_NUM_ROWS;
			numCols = TetrisGame.DEFAULT_NUM_COLS;
		}

		hidingBlocks = false;
		shouldDraw = true;
		message = "";

		setBlockSize(blockSize);
		setGame(game);
		setBackground(Color.BLACK);
		// setLayout(null);
		setVisible(true);
	}

	protected Cell[] cells() {
		return cells;
	}

	protected void onGameEvent(Event e) {
		System.out.println(e.name);
		update(e);
		repaint();
	}

	protected void update(Event e) {
		updateBlocks();
		updatePiece();
	}

	public void setBlockSize(int newBlockSize) {
		blockSize = newBlockSize;
		if (blockSize < 1) {
			blockSize = DEFAULT_BLOCK_SIZE;
		}

		setPreferredSize(new Dimension(numCols * blockSize, numRows * blockSize));
	}

	protected Cell[] resetCells() {
		Cell[] newCells = new Cell[numRows * numCols];
		for (int i = 0; i < newCells.length; i++) {
			newCells[i] = new Cell(i, i / numCols, i % numCols);
		}
		return newCells;
	}

	public void setGame(ITetrisGame newGame) {
		//unregister old events if necessary
		if (game != null) {
			// TODO #69 game.unregisterAllEventListeners();
			Arrays.stream(TetrisEvent.values()).forEach((e) -> game.unregisterEventListener(e, this::onGameEvent));
		}

		game = newGame;
		if (game != null) {
			numRows = game.getNumRows();
			numCols = game.getNumCols();
		} else {
			numRows = TetrisGame.DEFAULT_NUM_ROWS;
			numCols = TetrisGame.DEFAULT_NUM_COLS;
		}

		cells = resetCells();
		blocks = new int[numRows * numCols];
		setPreferredSize(new Dimension(numCols * blockSize, numRows * blockSize));

		//register new event handlers for the new game
		if (game != null) {
			game.registerEventListener(TetrisEvent.LINE_CLEAR, this::onGameEvent);
			game.registerEventListener(TetrisEvent.BLOCKS, this::onGameEvent);
			game.registerEventListener(TetrisEvent.PIECE_CREATE, this::onGameEvent);
			game.registerEventListener(TetrisEvent.PIECE_ROTATE, this::onGameEvent);
			game.registerEventListener(TetrisEvent.PIECE_SHIFT, this::onGameEvent);
			game.registerEventListener(TetrisEvent.PIECE_PLACED, this::onGameEvent);
		}
	}

	public void hideBlocks() {
		hidingBlocks = true;
	}

	public void showBlocks() {
		hidingBlocks = false;
	}

	protected void mapBlocksToCells() {
		for (Cell cell : cells()) {
			cell.shapeIndex = blocks[cell.index];
		}
	}

	public void updateBlocks() {
		if (game != null) {
			blocks = game.getBlocksOnBoard(blocks);
			mapBlocksToCells();
		}
	}

	public void updatePiece() {
		if (game != null && game.isPieceActive()) {
			updatePiece(game.getPieceBlocks(), game.getCurrentShape());
		}
	}

	public void updatePiece(Coord[] pieceCoords, Shape shape) {
		if (pieceCoords != null) {
			for (Coord c : pieceCoords) {
				int cellIndex = c.row() * numCols + c.col();
				blocks[cellIndex] = shape.value;
				cells[cellIndex].shapeIndex = shape.value;
			}
		}
	}

	public void setShouldDraw(boolean isPaused) {
		shouldDraw = isPaused;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	protected void drawCells(Graphics g) {
		for (Cell cell : cells()) {
			cell.draw(g);
		}
	}

	protected void drawMessage(Graphics g) {
		g.setColor(Color.WHITE);
		int x = getPreferredSize().width / 4;
		int y = getPreferredSize().height / 3;
		g.drawString(message, x, y);
	}

	@Override public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (shouldDraw) {
			if (!hidingBlocks) {
				drawCells(g);
			}
			drawStats(g);
		} else if (!message.equals("")) {
			drawMessage(g);
		}
	}

	public void drawStats(Graphics g) {
		if (game != null) {
			g.setColor(Color.MAGENTA);
			g.setFont(new Font("Arial", Font.BOLD, 12));
			int y = 16;
			g.drawString("Score: " + game.getScore(), 8, y);
			y += 20;
			g.drawString("Pieces: " + game.getNumPiecesDropped(), 8, y);
			y += 20;
			g.drawString("Lines: " + game.getLinesCleared(), 8, y);
			y += 20;
			g.drawString("Level: " + game.getLevel(), 8, y);
		}
	}
}
