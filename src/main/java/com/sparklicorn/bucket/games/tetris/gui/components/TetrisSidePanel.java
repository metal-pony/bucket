package com.sparklicorn.bucket.games.tetris.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import javax.swing.JPanel;

import com.sparklicorn.bucket.games.tetris.ITetrisGame;
import com.sparklicorn.bucket.games.tetris.TetrisEvent;
import com.sparklicorn.bucket.games.tetris.util.structs.Coord;
import com.sparklicorn.bucket.games.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.event.Event;

//shows game stats and next piece panel
public class TetrisSidePanel extends JPanel {

	protected int blockSize;
	protected ITetrisGame game;
	protected boolean drawStats;
	protected boolean drawNextPiece;

	protected Consumer<Event> eventListener;

	Font statsFont;
	Stroke stroke3 = new BasicStroke(3);

	public TetrisSidePanel(int bs, ITetrisGame g) {
		this.blockSize = bs;
		this.game = g;
		this.drawStats = false;
		this.drawNextPiece = false;

		try {
			InputStream fontfs = ClassLoader.getSystemClassLoader().getResourceAsStream("RobotoMono-Regular.ttf");
			statsFont = Font.createFont(Font.TRUETYPE_FONT, fontfs);
			fontfs.close();
		} catch (FontFormatException | IOException e1) {
			e1.printStackTrace();
			statsFont = new Font("Consolas", Font.PLAIN, 16);
		}

		setPreferredSize(new Dimension(blockSize * 10, game.getNumRows() * blockSize));
		setBackground(Color.BLACK);
		setLayout(null);
		setVisible(true);

		eventListener = (event) -> {
			repaint();
		};

		for (TetrisEvent e : TetrisEvent.values()) {
			g.registerEventListener(e, eventListener);
		}



	}

	public void setGame(ITetrisGame newGame) {
		this.game = newGame;
	}

	@Override protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;

		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		if (game != null) {
			g2.setColor(TetrisBoardPanel.UIColor);
			g2.setFont(statsFont.deriveFont(24f));
			@SuppressWarnings("unused")
			Stroke stroke = g2.getStroke();
			String format = "%8";

			int y = 64;
			int yOffset = 32;

			//draw Score
			g2.setColor(TetrisBoardPanel.UIColor);
			g2.drawString(String.format(format+"s", "Score"), 8, y);
			y += yOffset;
			g.setColor(Color.WHITE);
			g2.drawString(String.format(format+"d", game.getScore()), 8, y);
			y += yOffset;

			//g2.drawString("Pieces" + game.getNumPiecesDropped(), 8, y);
			//y += yOffset;
			//g2.drawString("Lines" + game.getLinesCleared(), 8, y);
			//y += yOffset;

			//draw Level
			y += yOffset;
			g2.setColor(TetrisBoardPanel.UIColor);
			g2.drawString(String.format(format+"s", "Level"), 8, y);
			y += yOffset;
			g.setColor(Color.WHITE);
			g2.drawString(String.format(format+"d", game.getLevel()), 8, y);

			//draw Next
			y += yOffset * 2;
			g2.setColor(TetrisBoardPanel.UIColor);
			g2.drawString(String.format(format+"s", "Next"), 8, y);
			y += yOffset / 2;

			//draw Next piece viewer

			if (
					!game.isGameOver()
					&& (game.hasStarted() && !game.isPaused()))
			{
				Shape next = game.getNextShape();
				Coord[] coords = next.getRotation(0);
				float row = 0;
				float col = 2;
				g.setColor(TetrisBoardPanel.COLORS_BY_SHAPE[next.value]);
				for (Coord c : coords) {
					int bx = (int) (col * blockSize + c.col() * blockSize + 48);
					int by = (int) (row * blockSize + c.row() * blockSize + y);
					g2.fill3DRect(bx, by, blockSize, blockSize, true);

				}
			}

		}

	}

}
