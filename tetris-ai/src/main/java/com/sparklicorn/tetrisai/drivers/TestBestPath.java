package com.sparklicorn.tetrisai.drivers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import javax.swing.JFrame;

import com.sparklicorn.bucket.tetris.gui.components.Block;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.tetris.util.structs.Shape;
import com.sparklicorn.bucket.util.ThreadPool;
import com.sparklicorn.tetrisai.game.GenericRanker;
import com.sparklicorn.tetrisai.game.ITetrisStateRanker;
import com.sparklicorn.tetrisai.game.PolyFuncRanker;
import com.sparklicorn.tetrisai.structs.MutatingPolyFunc;
import com.sparklicorn.tetrisai.structs.PolyFunc.PolyFuncTerm;

public class TestBestPath extends TetrisBoardPanel {
	Block mousePointerBlock;
	int[] state;
	HashMap<String, ITetrisStateRanker> rankers;
	int button = 0;
	Shape shape;
	Shape next;
	ITetrisStateRanker primaryRanker;
	boolean useLookAhead;

	/**
	 * Creates a Test Panel with the given block size.
	 *
	 * @param blockSize Size of tetris blocks in pixels.
	 */
	public TestBestPath(int blockSize) {
		super(blockSize, null);

		rankers = new HashMap<>();
		mousePointerBlock = null;
		state = new int[200];
		shape = Shape.I;
		next = Shape.L;
		// piece = new Tetromino(shape, AiTetris.DEFAULT_ENTRY_COORD);
		useLookAhead = true;

		//Mouse events
		//Left click = place block in cell
		//Right click = remove block from cell
		addMouseListener(new MouseListener() {
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {
				//calculate row/column of click
				int row = Math.max(Math.min(e.getY() / Block.size, numRows - 1), 0);
				int col = Math.max(Math.min(e.getX() / Block.size, numCols - 1), 0);
				//place or remove block in row,column
				button = e.getButton();
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (e.isShiftDown()) {
						// piece.reset(shape, new Coord(row, col));
					} else {
						state[row * numCols + col] = 3;
					}
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					state[row * numCols + col] = 0;
				}
				repaint();
			}

			@Override public void mouseExited(MouseEvent e) {
				mousePointerBlock = null;
				repaint();
			}

			@Override public void mouseEntered(MouseEvent e) {
				// int row = Math.max(Math.min(e.getY() / Block.size, numRows - 1), 0);
				// int col = Math.max(Math.min(e.getX() / Block.size, numCols - 1), 0);
				//mousePointerBlock = blockData[row * numCols + col]; // TODO #12 Revamp
				repaint();
			}
			@Override public void mouseClicked(MouseEvent e) {}
		});

		addMouseMotionListener(new MouseMotionListener() {
			@Override public void mouseMoved(MouseEvent e) {
				// int row = Math.max(Math.min(e.getY() / Block.size, numRows - 1), 0);
				// int col = Math.max(Math.min(e.getX() / Block.size, numCols - 1), 0);
				//mousePointerBlock = blockData[row * numCols + col]; // TODO #12 Revamp
				repaint();
			}

			@Override public void mouseDragged(MouseEvent e) {
				int row = Math.max(Math.min(e.getY() / Block.size, numRows - 1), 0);
				int col = Math.max(Math.min(e.getX() / Block.size, numCols - 1), 0);

				if (button == MouseEvent.BUTTON1) {
					state[row * numCols + col] = 3;
				} else if (button == MouseEvent.BUTTON3) {
					state[row * numCols + col] = 0;
				}

				//mousePointerBlock = blockData[row * numCols + col]; // TODO #12 Revamp
				repaint();
			}
		});

		addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					//place current piece in location of
					// primary ranker's choosing
					// board = new Board(numRows, numCols, state);
					// board.setPiece(new Tetromino(piece));
					// Tetromino best = AiTetris.findBestPlacement(
					// board, piece, primaryRanker, next, useLookAhead,
					// AiTetris.DEFAULT_ENTRY_COORD
					// );
					// board.setPiece(best);
					// board.plotPiece();
					// state = board.blocks(state);

					repaint();
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					//change piece shape
					int s = shape.value + 1;
					if (s > Shape.NUM_SHAPES) {
						s = 1;
					}
					shape = Shape.getShape(s);
					// piece.reset(shape, piece.getLocation());
					// for (Coord c : piece.getBlockLocations()) {
					// 	int i = c.row() * numCols + c.col();
					// 	if (i >= 0 && i < numRows * numCols) {
					// 		state[i] = 0;
					// 	}
					// }

					repaint();
				} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					//change next shape
					int s = next.value + 1;
					if (s > Shape.NUM_SHAPES) {
						s = 1;
					}
					next = Shape.getShape(s);
					System.out.println("Next piece = " + next.name());
				} else if (e.getKeyCode() == KeyEvent.VK_L) {
					useLookAhead = !useLookAhead;
					System.out.println("usLookAhead = " + useLookAhead);
				}

			}
		});

		repaint();
	}

	/**
	 * Adds a Tetris piece placement ranker to a set of rankers with a given name.
	 *
	 * @param ranker Ranker to add.
	 * @param name Name of ranker.
	 */
	public void addRanker(ITetrisStateRanker ranker, String name) {
		if (rankers.isEmpty()) {
			primaryRanker = ranker;
		}
		rankers.put(name, ranker);
	}

	/**
	 * Sets the piece placement ranking algorithm.
	 *
	 * @param ranker Tetris piece placement ranking algorithm.
	 */
	public void setPrimaryRanker(ITetrisStateRanker ranker) {
		primaryRanker = ranker;
	}

	@Override public void paintComponent(Graphics g) {
		super.paintComponent(g);

		updateBlocks(state);
		// updatePiece(piece);

		if (mousePointerBlock != null) {
			if (!mousePointerBlock.isShown()) {
				mousePointerBlock.setColor(Color.LIGHT_GRAY);
				mousePointerBlock.setShown(true);
				mousePointerBlock.paintComponent(g);
				mousePointerBlock.setShown(false);
			}
		}

		int textRowOffset = 24;
		int y = textRowOffset;
		g.setColor(Color.WHITE);
		g.setFont(new Font("Consolas", Font.PLAIN, 20));
		for (String n : rankers.keySet()) {
			ITetrisStateRanker r = rankers.get(n);
			// double rank = r.rank(); // TODO #12 fix me later - need access to a game instance
			// g.drawString(String.format("%-16s: %.3f", n, rank), 4, y);
			y += textRowOffset;
		}
	}

	/**
	 * Runs the Tetris training algorithm.
	 */
	public static void showFrame() {
		// ITetrisStateRanker ranker = new GenericRanker(
		//     new double[]{ -0.016478, -0.046655, -0.519912, -0.453756, -0.945709 }
		// );

		GenericRanker ranker2 = new GenericRanker(
			new double[] {
				0.221009946,
				-0.15497386,
				-3.2856708,
				-0.7510359,
				-5.81206
			}
		);

		PolyFuncRanker polyRanker = new PolyFuncRanker(
			new MutatingPolyFunc[] {
				new MutatingPolyFunc(new PolyFuncTerm(0.221009946, 1.0)),
				new MutatingPolyFunc(new PolyFuncTerm(-0.15497386, 1.0)),
				new MutatingPolyFunc(new PolyFuncTerm(-3.2856708, 1.0)),
				new MutatingPolyFunc(new PolyFuncTerm(-0.7510359, 1.0)),
				new MutatingPolyFunc(new PolyFuncTerm(-5.81206, 1.0)),
			}
		);

		JFrame main = new JFrame() {
			@Override public void dispose() {
				super.dispose();
				ThreadPool.shutdownNow();
			}
		};

		TestBestPath panel = new TestBestPath(40);

		panel.addRanker(ranker2, "GenericRanker");
		panel.addRanker(polyRanker, "PolyRanker");
		main.setBackground(Color.BLACK);
		main.add(panel);
		main.setResizable(false);
		main.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		panel.setFocusable(true);
		main.pack();
		main.setVisible(true);
	}
}
