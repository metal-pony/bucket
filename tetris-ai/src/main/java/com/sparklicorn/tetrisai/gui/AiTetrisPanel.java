package com.sparklicorn.tetrisai.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sparklicorn.bucket.tetris.TetrisGame;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.tetris.util.structs.Coord;
import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.bucket.util.event.Event;
import com.sparklicorn.tetrisai.AiTetris;
import com.sparklicorn.tetrisai.structs.PlacementRank;

public class AiTetrisPanel extends TetrisBoardPanel {
    protected class AiCell extends TetrisBoardPanel.Cell {
        protected Double nextMoveRank;

        protected AiCell(int index, int row, int col) {
            super(index, row, col);
        }

        @Override
        protected Color color() {
            if (shape != null || nextMoveRank == null) {
                return colorWithAlhpa(super.color(), 85);
                // TODO - make normal block alpha a variable, maybe ui setting
                // return colorWithAlhpa(super.color(), (int)(255f * ((float)col / (float)numCols)));
            }

            int alpha = Math.round((float)((nextMoveRank - lowestRank)/(highestRank - lowestRank)) * 255f);
            return colorWithAlhpa(currentGamePieceColor(), alpha);
        }

        @Override
        protected void draw(Graphics g) {
            super.draw(g);

            if (shape != null || nextMoveRank == null) {
                return;
            }

			int x = col * blockSize;
			int y = row * blockSize;
			g.setColor(color());
			g.fill3DRect(x, y, blockSize, blockSize, true);
			g.setColor(Color.BLACK);
			g.fill3DRect(x + 2, y + 2, blockSize - 4, blockSize - 4, true);
        }
    }

    protected AiCell[] cells;
    protected AiTetris game;

    protected double lowestRank;
    protected double highestRank;

    public AiTetrisPanel(AiTetris game) {
        super(game);

        this.game = (AiTetris)super.game;
    }

    @Override
    protected void initCells() {
		super.cells = new AiCell[state.rows * state.cols];
		for (int i = 0; i < super.cells.length; i++) {
			super.cells[i] = new AiCell(i, i / state.cols, i % state.cols);
		}
        this.cells = (AiCell[]) super.cells;
	}

    @Override
    public AiTetris connectGame(TetrisGame game) {
        if (game != null && game instanceof AiTetris) {
            return connectGame((AiTetris) game);
        }
        throw new UnsupportedOperationException("AiTetrisPanel only supports AiTetris games");
    }

    public AiTetris connectGame(AiTetris game) {
        AiTetris prevGame = (AiTetris)super.connectGame(game);
        this.game = game;
        return prevGame;
    }

    @Override
    public void update(Event e) {
        // TODO inspect how accurate this logic will be post TetrisBoardPanel updates
        super.update(e);

        if (state.isPaused || state.isGameOver) {
            return;
        }

        if (state.piece.isActive()) {
            clearCellRanks();
            setPlacementRanks();
        } else {
            clearCellRanks();
        }
    }

    private void clearCellRanks() {
        for (AiCell cell : cells) {
            cell.nextMoveRank = null;
        }
    }

    private void setPlacementRanks() {
        // TODO change once getTopPlacements ref complete
        List<PlacementRank> placementRanks = AiTetris.getTopPlacements(state, game.getRanker(), new ArrayList<>(), 1f);
        if (placementRanks.isEmpty()) {
            // System.out.println("No placements found");
            return;
        }

        double first = placementRanks.get(0).rank();
        highestRank = first;
        lowestRank = first;

        for (PlacementRank pr : placementRanks) {
            Position placement = pr.placements().get(0);
            Coord[] cellCoords = state.getShapeCoordsAtPosition(placement);

            for (Coord cellCoord : cellCoords) {
                int index = cellCoord.row() * state.cols + cellCoord.col();
                double rank = pr.rank();

                // TODO Temp messaging while debugging index out of bounds err
                if (index >= cells.length) {
                    System.out.printf(
                        "index out of bounds: %d >= %d\n",
                        index, cells.length
                    );

                    System.out.println("Shape: " + state.piece.shape().name());
                    System.out.println("Position: " + placement);
                    System.out.println("Cell Coords: " + Arrays.toString(cellCoords));
                }

                AiCell cell = cells[index];
                if (cell.nextMoveRank == null || rank > cell.nextMoveRank) {
                    cell.nextMoveRank = rank;
                }

                if (rank > highestRank) {
                    highestRank = rank;
                }
                if (rank < lowestRank) {
                    lowestRank = rank;
                }
            }
        }

        // System.out.printf("""
        //     Lowest rank: %.2f
        //     Highest rank: %.2f
        // """, lowestRank, highestRank);
    }
}
