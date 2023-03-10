package com.sparklicorn.bucket.games.tetris.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.function.Consumer;

import javax.swing.JPanel;

import com.sparklicorn.bucket.games.tetris.ITetrisGame;
import com.sparklicorn.bucket.games.tetris.TetrisEvent;
import com.sparklicorn.bucket.games.tetris.TetrisGame;
import com.sparklicorn.bucket.games.tetris.util.structs.Coord;
import com.sparklicorn.bucket.games.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.event.Event;

public class TetrisBoardPanel extends JPanel {

	protected static final int DEFAULT_BLOCK_SIZE = 24;

	//O = 1, I = 2, S = 3, Z = 4, L = 5, J = 6, T = 7;
	protected static final Color[] COLORS_BY_SHAPE = {
		Color.WHITE,
		Color.YELLOW,
		Color.CYAN,
		Color.GREEN,
		Color.RED,
		new Color(255, 165, 0),
		Color.BLUE,
		new Color(128,0,128)
	};

	protected static final Color UIColor = new Color(0.0f, 0.0f, 1.0f);

	//private int blockSize;
	protected int width, height, numRows, numCols;
	protected int blockSize;
	protected int[] blockData;
	protected boolean hidingBlocks;
	protected boolean shouldDraw;
	protected String message;

	protected Consumer<Event> boardListener;
	protected Consumer<Event> pieceListener;
	protected Consumer<Event> resumeListener;
	protected ITetrisGame game;

	public TetrisBoardPanel() {
		this(DEFAULT_BLOCK_SIZE, null);
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
		boardListener = (e) -> {
			updateBlocks();
			if (this.game.isPieceActive()) {
				updatePiece(this.game.getPieceBlocks(), this.game.getCurrentShape());
			}

			repaint();
		};
		pieceListener = (e) -> {
			updateBlocks();
			if (this.game.isPieceActive()) {
				updatePiece(this.game.getPieceBlocks(), this.game.getCurrentShape());
			}
			repaint();
		};

		setBlockSize(blockSize);
		setGame(game);
		setBackground(Color.BLACK);
		setLayout(null);
		setVisible(true);
	}

	public void setBlockSize(int newBlockSize) {
		blockSize = newBlockSize;
		if (blockSize < 1)
			blockSize = DEFAULT_BLOCK_SIZE;
		setPreferredSize(new Dimension(numCols * blockSize, numRows * blockSize));
	}

	public void setGame(ITetrisGame newGame) {
		//unregister old events if necessary
		if (game != null) {
			game.unregisterEventListener(TetrisEvent.LINE_CLEAR, boardListener);
			game.unregisterEventListener(TetrisEvent.BLOCKS, boardListener);
			game.unregisterEventListener(TetrisEvent.PIECE_CREATE, pieceListener);
			game.unregisterEventListener(TetrisEvent.PIECE_ROTATE, pieceListener);
			game.unregisterEventListener(TetrisEvent.PIECE_SHIFT, pieceListener);
			game.unregisterEventListener(TetrisEvent.PIECE_PLACED, pieceListener);
		}

		game = newGame;
		if (game != null) {
			numRows = game.getNumRows();
			numCols = game.getNumCols();
		} else {
			numRows = TetrisGame.DEFAULT_NUM_ROWS;
			numCols = TetrisGame.DEFAULT_NUM_COLS;
		}

		blockData = new int[numRows * numCols];
		setPreferredSize(new Dimension(numCols * blockSize, numRows * blockSize));

		//register new event handlers for the new game
		if (game != null) {
			game.registerEventListener(TetrisEvent.LINE_CLEAR, boardListener);
			game.registerEventListener(TetrisEvent.BLOCKS, boardListener);
			game.registerEventListener(TetrisEvent.PIECE_CREATE, pieceListener);
			game.registerEventListener(TetrisEvent.PIECE_ROTATE, pieceListener);
			game.registerEventListener(TetrisEvent.PIECE_SHIFT, pieceListener);
			game.registerEventListener(TetrisEvent.PIECE_PLACED, pieceListener);
		}
	}

	public void hideBlocks() {
		hidingBlocks = true;
	}

	public void stopHidingBlocks() {
		hidingBlocks = false;
	}

	public void updateBlocks() {
		blockData = game.getBlocksOnBoard(blockData);
	}

	public void updateBlocks(int[] blocks) {
		System.arraycopy(blocks, 0, blockData, 0, blocks.length);
	}

	public void updatePiece(int[] blockLocations, Shape shape) {
		updatePiece(blockLocations, shape);
	}

	public void updatePiece(int[] pieceBlocks, int shape) {
		if (pieceBlocks != null) {
			for (int i : pieceBlocks) {
				blockData[i] = shape;
			}
		}
	}

	public void updatePiece(Coord[] pieceCoords, Shape shape) {
		if (pieceCoords != null) {
			for (Coord c : pieceCoords) {
				int i = c.row() * numCols + c.col();
				blockData[i] = shape.value;
			}
		}
	}

	public void setShouldDraw(boolean isPaused) {
		shouldDraw = isPaused;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	@Override public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (shouldDraw) {
			if (!hidingBlocks) {
				for (int i = 0; i < blockData.length; i++) {
					if (blockData[i] > 0) {
						int x = (i % numCols) * blockSize;
						int y = (i / numCols) * blockSize;
						g.setColor(COLORS_BY_SHAPE[blockData[i]]);
						g.fill3DRect(x, y, blockSize, blockSize, true);
					}
				}
			}
			drawStats(g);
		} else if (!message.equals("")) {
			g.setColor(Color.WHITE);
			int x = getPreferredSize().width / 4,
				y = getPreferredSize().height / 3;
			g.drawString(message, x, y);
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
