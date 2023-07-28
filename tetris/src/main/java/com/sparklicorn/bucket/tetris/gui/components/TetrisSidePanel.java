package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.sparklicorn.bucket.tetris.ITetrisGame;
import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.event.Event;

//shows game stats and next piece panel
public class TetrisSidePanel extends JPanel {
	protected static final int DEFAULT_FONT_SIZE = 20;
	protected static final int DEFAULT_NEXT_BLOCK_SIZE = 20;

	protected class NextPiecePanel extends JPanel {
		NextPiecePanel() {
			setBackground(Color.BLACK);
			this.setPreferredSize(
				new Dimension(
					game.getNumCols() * blockSize,
					2 * blockSize
				)
			);
		}

		@Override protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (game != null && game.hasStarted() && !game.isPaused()) {
				drawTetrisPiece(8, 8, game.getNextShape(), nextBlockSize, (Graphics2D) g);
			}
		}

		protected void drawTetrisPiece(int x, int y, Shape shape, int blockSize, Graphics2D g2DContext) {
			Coord[] coords = shape.getRotation(0);
			float row = 0;
			float col = 2;

			g2DContext.setColor(TetrisBoardPanel.COLORS_BY_SHAPE[shape.value]);

			for (Coord c : coords) {
				int bx = (int) (col * blockSize + c.col() * blockSize + x);
				int by = (int) (row * blockSize + c.row() * blockSize + y);
				g2DContext.fill3DRect(bx, by, blockSize, blockSize, true);
			}
		}
	}

	protected ITetrisGame game;
	protected int blockSize;
	protected int fontSize;
	protected int nextBlockSize;
	protected Font statsFont;

	protected JLabel nextLabel;
	protected JPanel nextPiecePanel;
	protected boolean showNextPiece;

	protected JLabel scoreLabel;
	protected long score;
	protected boolean showScore;

	protected JLabel levelLabel;
	protected long level;
	protected boolean showLevel;

	protected Consumer<Event> eventListener;

	public TetrisSidePanel(ITetrisGame game) {
		this(
			game,
			TetrisBoardPanel.DEFAULT_BLOCK_SIZE,
			DEFAULT_FONT_SIZE,
			DEFAULT_NEXT_BLOCK_SIZE
		);
	}

	public TetrisSidePanel(ITetrisGame game, int blockSize, int fontSize, int nextBlockSize) {
		super();

		this.game = game;
		this.blockSize = blockSize;
		this.fontSize = fontSize;
		this.nextBlockSize = nextBlockSize;
		this.showNextPiece = true;
		this.showScore = true;
		this.showLevel = true;

		score = (game != null) ? game.getScore() : 0L;
		level = (game != null) ? game.getLevel() : 0L;
		statsFont = new Font("Consolas", Font.PLAIN, fontSize);

		nextPiecePanel = new NextPiecePanel();
		nextLabel = createLabel("Next");
		scoreLabel = createLabel(scoreLabelText());
		levelLabel = createLabel(levelLabelText());

		// setPreferredSize(new Dimension(
		// 	game.getNumCols() * blockSize,
		// 	game.getNumRows() * blockSize
		// ));

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Color.BLACK);
		// setHorizontalAlignment(SwingConstants.LEFT);
		add(nextLabel);
		add(nextPiecePanel);
		add(scoreLabel);
		add(levelLabel);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		nextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		hookGameEvents();
	}

	private JLabel createLabel(String text) {
		JLabel label = new JLabel(text);
		label.setBackground(Color.BLACK);
		label.setFont(statsFont);
		label.setForeground(TetrisBoardPanel.UIColor);
		return label;
	}

	public boolean isShowingScore() {
		return showScore;
	}

	public void setShowScore(boolean val) {
		if (showScore && !val) {
			scoreLabel.setVisible(false);
		} else if (!showScore && val) {
			scoreLabel.setVisible(true);
		}
		showScore = val;
	}

	public boolean isShowingLevel() {
		return showLevel;
	}

	public void setShowLevel(boolean val) {
		if (showLevel && !val) {
			levelLabel.setVisible(false);
		} else if (!showLevel && val) {
			levelLabel.setVisible(true);
		}
		showLevel = val;
	}

	public boolean isShowingNextPiece() {
		return showNextPiece;
	}

	public void setShowNextPiece(boolean val) {
		if (showNextPiece && !val) {
			nextLabel.setVisible(false);
			nextPiecePanel.setVisible(false);
		} else if (!showNextPiece && val) {
			nextLabel.setVisible(true);
			nextPiecePanel.setVisible(true);
		}
		showNextPiece = val;
	}

	protected void onGameEvent(Event e) {
		updateScoreLabel();
		updateLevelLabel();
		repaint();
	}

	protected void updateScoreLabel() {
		if (!showScore) {
			return;
		}

		long currentScore = game.getScore();
		if (score != currentScore) {
			score = currentScore;
			scoreLabel.setText(scoreLabelText());
		}
	}

	protected void updateLevelLabel() {
		if (!showLevel) {
			return;
		}

		long currentLevel = game.getLevel();
		if (level != currentLevel) {
			level = currentLevel;
			levelLabel.setText(levelLabelText());
		}
	}

	protected String scoreLabelText() {
		return String.format(
			"<html>Score<br><font color='#ffffff'>%8d</font></html>",
			score
		);
	}

	protected String levelLabelText() {
		return String.format(
			"<html>Level<br><font color='#ffffff'>%8d</font></html>",
			level
		);
	}

	protected void hookGameEvents() {
		Arrays.stream(TetrisEvent.values()).forEach((e) -> game.registerEventListener(e, this::onGameEvent));
	}

	protected void unhookGameEvents() {
		Arrays.stream(TetrisEvent.values()).forEach((e) -> game.unregisterEventListener(e, this::onGameEvent));
	}

	public void setGame(ITetrisGame newGame) {
		unhookGameEvents();
		this.game = newGame;
		hookGameEvents();
	}
}
