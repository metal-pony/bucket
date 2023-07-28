package com.sparklicorn.bucket.gamer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

public abstract class Gamer {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Gamer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    protected long lastUpdate;
    protected long timerInterval;
    protected long updateInterval;

    protected Timer updateTimer;

    protected List<GameObject> gameObjects;

    protected boolean paused;

    protected Gamer() {
        lastUpdate = 0L;
        timerInterval = 0L;
        updateInterval = 0L;
        updateTimer = new Timer(timerInterval, 0L);
        gameObjects = new ArrayList<>();
        paused = false;
    }

    protected void init() {
        // TODO
    }

    protected void gameloop(long timeMs) {
        if (paused) {
            return;
        }

        fastUpdate(timeMs);
        // process collisions (chronologically?)

        if (timeMs - lastUpdate >= updateInterval) {
            update(timeMs);
            afterUpdate(timeMs);
        }
    }

    /**
     * Performs quick updates and calculations, such as calculating positions, velocities, etc.
     * These are performed at a higher frequency than the main update loop.
     *
     * @param timeMs
     */
    protected abstract void fastUpdate(long timeMs);

    protected abstract void update(long timeMs);

    protected abstract void afterUpdate(long timeMs);
}
