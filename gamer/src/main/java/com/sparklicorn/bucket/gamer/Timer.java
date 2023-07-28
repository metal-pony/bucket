package com.sparklicorn.bucket.gamer;

public class Timer {

    protected long updateInterval;

    protected long lastUpdate;

    protected Thread thread;

    protected long initialDelay;

    protected boolean isRunning;
    protected boolean isShutdown;

    protected Timer(long updateInterval, long initialDelay) {
        this.updateInterval = updateInterval;
        this.initialDelay = initialDelay;
        this.isRunning = false;
        this.isShutdown = false;

    }

    protected void tick() {

    }
}
