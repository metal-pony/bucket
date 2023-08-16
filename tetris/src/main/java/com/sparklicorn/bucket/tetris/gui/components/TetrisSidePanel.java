package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.tetris.util.structs.Shape;

//shows game stats and next piece panel
public class TetrisSidePanel extends JPanel {
	protected static final int DEFAULT_FONT_SIZE = 20;

	protected class NextPiecePanel extends JPanel {
		boolean show;

		NextPiecePanel() {
			show = true;
			setBackground(Color.BLACK);
			updatePreferredSize();
		}

		protected void updatePreferredSize() {
			this.setPreferredSize(
				new Dimension(
					6 * boardPanel.blockSize,
					2 * boardPanel.blockSize
				)
			);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (show) {
				drawTetrisPiece(8, 8, boardPanel.state.nextShapes.peek(), nextBlockSize, (Graphics2D) g);
			}
		}

		protected void drawTetrisPiece(int x, int y, Shape shape, int blockSize, Graphics2D g2DContext) {
			Coord[] coords = shape.getRotation(0);
			float row = 0;
			float col = 2;

			g2DContext.setColor(TetrisBoardPanel.colorForShape(shape));

			for (Coord c : coords) {
				int bx = (int) (col * blockSize + c.col() * blockSize + x);
				int by = (int) (row * blockSize + c.row() * blockSize + y);
				g2DContext.fill3DRect(bx, by, blockSize, blockSize, true);
			}
		}
	}

	/**
	 * The TetrisBoardPanel that this TetrisSidePanel is associated with.
	 */
	protected TetrisBoardPanel boardPanel;

	protected int fontSize;
	protected int nextBlockSize;
	protected Font statsFont;

	protected JLabel nextLabel;
	protected NextPiecePanel nextPiecePanel;
	protected boolean showNextPiece;

	protected JLabel scoreLabel;
	protected boolean showScore;

	protected JLabel levelLabel;
	protected boolean showLevel;

	public TetrisSidePanel(TetrisBoardPanel panel) {
		this(
			panel,
			DEFAULT_FONT_SIZE
		);
	}

	public TetrisSidePanel(TetrisBoardPanel panel, int fontSize) {
		super();

		this.boardPanel = panel;
		this.fontSize = fontSize;
		this.nextBlockSize = Math.round((float)panel.blockSize * 2f / 3f);
		this.showNextPiece = true;
		this.showScore = true;
		this.showLevel = true;
		statsFont = new Font("Consolas", Font.PLAIN, fontSize);

		nextPiecePanel = new NextPiecePanel();
		nextLabel = createLabel("Next");
		scoreLabel = createLabel(scoreLabelText(0L));
		levelLabel = createLabel(levelLabelText(0L));

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Color.BLACK);
		add(nextLabel);
		add(nextPiecePanel);
		add(scoreLabel);
		add(levelLabel);
	}

	private JLabel createLabel(String text) {
		JLabel label = new JLabel(text);
		label.setBackground(Color.BLACK);
		label.setFont(statsFont);
		label.setForeground(TetrisBoardPanel.UIColor);
		return label;
	}

	public void setBlockSize(int newBlockSize) {
		boardPanel.blockSize = newBlockSize;
		nextBlockSize = Math.round((float)boardPanel.blockSize * 2f / 3f);
		nextPiecePanel.updatePreferredSize();
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

	public void updateScoreLabel(long score) {
		if (showScore) {
			scoreLabel.setText(scoreLabelText(score));
		}
	}

	public void updateLevelLabel(long level) {
		if (showLevel) {
			levelLabel.setText(levelLabelText(level));
		}
	}

	protected String scoreLabelText(long score) {
		return String.format(
			"<html>Score<br><font color='#ffffff'>%8d</font></html>",
			score
		);
	}

	protected String levelLabelText(long level) {
		return String.format(
			"<html>Level<br><font color='#ffffff'>%8d</font></html>",
			level
		);
	}
}
