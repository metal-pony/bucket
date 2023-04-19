package com.sparklicorn.bucket.tetris.gui.components;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;

public class Block extends JComponent {

	public static int size = TetrisBoardPanel.DEFAULT_BLOCK_SIZE;

	Color c;
	String texture;
	boolean visi;

	Block() {
		visi = false;
		texture = "";
		c = Color.WHITE;

		//this.setSize(size, size);
		this.setVisible(true);
	}

	public void setColor(Color c) {
		this.c = c;
	}

	public void setShown(boolean show) {
		visi = show;
	}

	public boolean isShown() {
		return visi;
	}

	@Override public void paintComponent(Graphics g) {
		if (visi) {
			g.setColor(c);
			g.fill3DRect(getX(), getY(), getWidth(), getHeight(), true);
		}
	}

	public void paintMe(Graphics g) {
		if (visi) {
			//System.out.println("blah");
			g.setColor(c);
			g.fill3DRect(getX(), getY(), getWidth(), getHeight(), true);
		}
	}

}
