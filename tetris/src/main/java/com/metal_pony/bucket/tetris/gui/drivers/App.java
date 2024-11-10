package com.metal_pony.bucket.tetris.gui.drivers;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Tetris");

        Group group = new Group();
        Rectangle rect = new Rectangle(0, 0, 100, 100);
        group.getChildren().add(rect);
        Scene scene = new Scene(group, 500, 500, Color.BLUE);
        stage.setScene(scene);

        stage.show();
    }
}
