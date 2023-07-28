package com.sparklicorn.tetrisai.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
// import java.util.Arrays;
import java.util.List;

import com.sparklicorn.bucket.tetris.TetrisEvent;
import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
import com.sparklicorn.bucket.tetris.util.structs.Coord;
// import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.bucket.util.event.Event;
import com.sparklicorn.tetrisai.game.AiTetris;
import com.sparklicorn.tetrisai.structs.PlacementRank;

public class AiTetrisPanel extends TetrisBoardPanel {

    static Color colorWithAlhpa(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    Color currentGamePieceColor() {
        return COLORS_BY_SHAPE[game().getCurrentShape().value % COLORS_BY_SHAPE.length];
    }

    protected class AiCell extends TetrisBoardPanel.Cell {
        protected Double nextMoveRank;

        protected AiCell(int index, int row, int col) {
            super(index, row, col);
        }

        @Override
        protected Color color() {
            if (shapeIndex > 0 || nextMoveRank == null) {
                return colorWithAlhpa(super.color(), 85);
                // return colorWithAlhpa(super.color(), (int)(255f * ((float)col / (float)numCols)));
            }

            int alpha = Math.round((float)((nextMoveRank - lowestRank)/(highestRank - lowestRank)) * 255f);
            return colorWithAlhpa(currentGamePieceColor(), alpha);
            // return super.color();
        }

        @Override
        protected void draw(Graphics g) {
            super.draw(g);

            if (shapeIndex > 0 || nextMoveRank == null) {
                return;
            }

            // shapeIndex = game.getCurrentShape().value;
			int x = col * blockSize;
			int y = row * blockSize;
			g.setColor(color());
			g.fill3DRect(x, y, blockSize, blockSize, true);
			g.setColor(Color.BLACK);
			g.fill3DRect(x + 2, y + 2, blockSize - 4, blockSize - 4, true);
        }
    }

    protected double lowestRank;
    protected double highestRank;
    // protected double[] placementRanks;

    public AiTetrisPanel(AiTetris game) {
        super(game);

        // highestCurrentRank = Double.MIN_VALUE;
        // this.placementRanks = new double[numRows * numCols];
    }

    @Override
    protected AiCell[] cells() {
        return (AiCell[]) cells;
    }

    protected AiTetris game() {
        return (AiTetris) game;
    }

    @Override
    protected Cell[] resetCells() {
		Cell[] newCells = new AiCell[numRows * numCols];
		for (int i = 0; i < newCells.length; i++) {
			newCells[i] = new AiCell(i, i / numCols, i % numCols);
		}
		return newCells;
	}

    @Override
    protected void update(Event e) {
        super.update(e);

        if (game.isPaused() || game.isGameOver()) {
            return;
        }

        // set placement ranks if event name is PIECE_CREATE
        if (e.name.equals(TetrisEvent.PIECE_CREATE.name())) {
            setPlacementRanks();
        } else if (e.name.equals(TetrisEvent.PIECE_PLACED.name())) {
            clearCellRanks();
        }
    }

    private void clearCellRanks() {
        System.out.println("Clearing cell ranks");
        for (AiCell cell : cells()) {
            cell.nextMoveRank = null;
        }
    }

    private void setPlacementRanks() {
        List<PlacementRank> placementRanks = AiTetris.getTopPlacements(game(), new ArrayList<>(), 1f);
        highestRank = Double.MIN_VALUE;
        lowestRank = Double.MAX_VALUE;
        AiTetris _game = game();
        AiCell[] _cells = cells();
        for (PlacementRank pr : placementRanks) {
            Coord[] cellCoords = _game.populateBlockPositions(
                _game.getPieceBlocks(),
                pr.placements().get(0)
            );

            for (Coord cellCoord : cellCoords) {
                int index = cellCoord.row() * numCols + cellCoord.col();
                double rank = pr.rank();

                AiCell cell = _cells[index];
                if (cell.nextMoveRank == null || rank > cell.nextMoveRank) {
                    cell.nextMoveRank = rank;
                    // System.out.printf(
                    //     "(%d, %d) -> %d -> %.2f\n",
                    //     cellCoord.row(), cellCoord.col(),
                    //     index, rank
                    // );
                }

                if (rank > highestRank) {
                    highestRank = rank;
                }
                if (rank < lowestRank) {
                    lowestRank = rank;
                }
            }
        }

        // System.out.println(placementRanks.toString());
        // System.out.println(Arrays.toString(game().getRanks(placementRanks)));
        // // copy placement ranks to cells and find highest and lowest ranks
        // for (int i = 0; i < placementRanks.length; i++) {
        //     cells()[i].nextMoveRank = placementRanks[i];
        //     // System.out.printf("%.2f ", placementRanks[i]);
        //     //every i divisible by numCols, print newline
        //     // if ((i + 1) % numCols == 0) {
        //     //     System.out.println();
        //     // }
        // }

        System.out.printf("""
            Lowest rank: %.2f
            Highest rank: %.2f
        """, lowestRank, highestRank);

        // highestCurrentRank = Double.MIN_VALUE;
        // clearCellRanks();
        // Coord[] coords = new Coord[4];
        // for (int i = 0 ; i < coords.length; i++) {
        //     coords[i] = new Coord();
        // }
        // for (Position placement : game().getPossiblePlacements()) {
        //     double rank = game().getPlacementRank(placement);

        //     game().populateBlockPositions(coords, placement);

        //     for (Coord coord : coords) {
        //         AiCell cell = cells()[coord.row() * game.getNumCols() + coord.col()];
        //         if (cell.nextMoveRank < rank) {
        //             cell.nextMoveRank = rank;
        //         }
        //         System.out.printf(
        //             "Updating cell rank at %s to %.4f\n",
        //             coord.toString(),
        //             rank
        //         );
        //     }
        //     if (rank > highestCurrentRank) {
        //         highestCurrentRank = rank;
        //     }
        // }
    }
}
