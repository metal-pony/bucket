package com.sparklicorn.bucket.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A visual representation of the Shuffler at work.
 */
public class ShufflerTester {
    public static void show() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle("Shuffle Tester");
        frame.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) {}
            @Override public void keyPressed(KeyEvent e) {}
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                }
            }
        });

        final int width = 512;
        final int height = 512;
        final int dividerWidth = 4;
        int[] pix = new int[width * height];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width / 2; c++) {
                pix[r * width + c] = 1;
            }
        }

        int[] shuffledPix = Arrays.copyOf(pix, pix.length);
        Shuffler.shuffle(shuffledPix);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width*2 + dividerWidth, height);

                g.setColor(Color.GRAY);
                g.fillRect(width, 0, dividerWidth, height);

                g.setColor(Color.BLACK);
                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        if (pix[r * width + c] > 0) {
                            g.drawLine(r, c, r, c);
                        }
                    }
                }

                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        if (shuffledPix[r * width + c] > 0) {
                            g.drawLine(width + dividerWidth + c, r, width + dividerWidth + c, r);
                        }
                    }
                }
            }
        };

        panel.setPreferredSize(new Dimension(width*2 + dividerWidth, height));
        panel.setBackground(Color.GRAY);

        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
