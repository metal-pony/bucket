package com.sparklicorn.tetrisai.drivers;

// import java.awt.event.KeyEvent;
// import java.awt.event.KeyListener;
// import java.util.Set;

// import com.sparklicorn.bucket.tetris.gui.components.TetrisBoardPanel;
// import com.sparklicorn.bucket.tetris.gui.components.TetrisFrame;
// import com.sparklicorn.bucket.tetris.util.structs.Position;
// import com.sparklicorn.tetrisai.game.AiTetris;

public class Main {
    public static void main(String[] args) {
        DefaultTetrisRunner.createAndShow();

        // BunchOfGames.trainAndShow();
        // GenericRankerEvolver.trainAndShow();
        // PolyFuncRankerEvolver.trainNewPolyFuncRankers();
        // TestBestPath.showFrame();

        // AiTetris game = new AiTetris();

        // TetrisFrame f = new TetrisFrame(600, 800, 32, game);
        // TetrisBoardPanel panel = f.getPanel();

        // f.addKeyListener(new KeyListener() {
		// 	@Override public void keyPressed(KeyEvent arg0) {
		// 		int key = arg0.getKeyCode();
		// 		switch (key) {
		// 		case KeyEvent.VK_ENTER:
		// 			if (!game.hasStarted()) {
		// 				game.start(0);
        //             } else if (!game.isGameOver()) {
        //                 Set<Position> possiblePlacements = game.getPossiblePlacements();
        //                 possiblePlacements.forEach((p) -> {

        //                 });
        //             } else {
        //                 game.start(0L);
        //             }
		// 			break;
		// 		case KeyEvent.VK_ESCAPE:
		// 			f.dispose();
		// 			break;
		// 		}
		// 	}

        //     @Override public void keyTyped(KeyEvent e) {}
        //     @Override public void keyReleased(KeyEvent e) {}
		// });
    }
}
