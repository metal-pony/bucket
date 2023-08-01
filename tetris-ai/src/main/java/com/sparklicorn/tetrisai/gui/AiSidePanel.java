package com.sparklicorn.tetrisai.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.sparklicorn.bucket.tetris.gui.components.TetrisSidePanel;
import com.sparklicorn.tetrisai.game.AiTetris;
import com.sparklicorn.tetrisai.game.GenericRanker;
import com.sparklicorn.tetrisai.game.GenericRanker.HeuristicWeight;

public class AiSidePanel extends TetrisSidePanel {
    protected class GenericRankerEditorPanel extends JPanel {
        static final float WEIGHT_INCREMENT = 0.01f;
        static final Font weightFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        GenericRanker ranker;

        GenericRankerEditorPanel(GenericRanker ranker) {
            super();

            this.ranker = ranker;

            setBackground(Color.MAGENTA);
            setLayout(new GridLayout(ranker.getHeuristicWeights().size(), 3));

            for (HeuristicWeight hw : ranker.getHeuristicWeights()) {
                add(createHeuristicRow(hw));
            }
        }

        JPanel createHeuristicRow(HeuristicWeight hw) {
            JPanel panel = new JPanel();
            panel.setBackground(Color.WHITE);

            JLabel nameLabel = new JLabel(hw.name());

            JLabel numberLabel = new JLabel();
            numberLabel.setFont(weightFont);
            // System.out.println("Creating row for heuristic: " + hw.toString());
            numberLabel.setText(String.format("%.2f", hw.weight()));

            JSlider weightSlider = new JSlider(JSlider.HORIZONTAL, -10000, 10000, Math.round(hw.weight() * 100f));
            weightSlider.addChangeListener((e) -> {
                if (weightSlider.getValueIsAdjusting()) {
                    return;
                }
                float newWeight = (float)weightSlider.getValue() / 100f;
                hw.setWeight(newWeight);
                numberLabel.setText(String.format("%.2f", newWeight));
            });
            weightSlider.setFocusable(false);

            panel.add(nameLabel);
            panel.add(numberLabel);
            panel.add(weightSlider);

            return panel;
        }
    }

    protected JButton runButton;
    protected JButton newGameButton;

    public AiSidePanel(AiTetris game) {
        this(game, true, true, true);
    }

    public AiSidePanel(AiTetris game, boolean showNextPiece, boolean showLevel, boolean showScore) {
        super(game);

        JPanel controlsPanel = new JPanel();
        controlsPanel.setBackground(Color.BLACK);
        controlsPanel.add(runButton = createRunButton());
        controlsPanel.add(newGameButton = createNewGameButton());

        // TODO Fix unsafe cast (ITetrisStateRanker -> GenericRanker)
        JPanel heuristicsPanel = new GenericRankerEditorPanel((GenericRanker) game.getRanker());

        add(controlsPanel);
        add(heuristicsPanel);
    }

    private JButton createRunButton() {
        JButton runButton = new JButton("RUN");
        runButton.setFocusable(false);
        runButton.addActionListener((e) -> {
            try {
                ((AiTetris) this.game).run(0L, false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return runButton;
    }

    private JButton createNewGameButton() {
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener((e) -> {
            AiTetris _game = (AiTetris) this.game;
            _game.stop();
            _game.newGame();
            _game.start(0L);
        });
        newGameButton.setFocusable(false);
        return newGameButton;
    }
}
