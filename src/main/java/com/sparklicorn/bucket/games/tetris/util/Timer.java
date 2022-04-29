package com.sparklicorn.bucket.games.tetris.util;

import java.util.concurrent.TimeUnit;

public class Timer implements Runnable {

	protected volatile boolean running;
	protected volatile boolean shuttingDown;
	protected boolean isWaiting;

	protected Thread thread;
	protected volatile Runnable runnable;

	protected volatile long delayNano; //time between timer loop ticks (nanos)

	protected long initialDelay;
	protected TimeUnit initialDelayUnit;

	protected volatile long lastTickTimeNano; //System time of last loop tick (nanos)

	protected volatile boolean repeats;

	protected Object delayChangeLock;
	protected boolean delayChanged;
	protected long newDelay;

	public Timer() {
		this(null, 1L, TimeUnit.SECONDS, false, 0L, TimeUnit.SECONDS);
	}

	public Timer(Runnable r) {
		this(r, 1L, TimeUnit.SECONDS, false, 0L, TimeUnit.SECONDS);
	}

	public Timer(Runnable r, long delay, TimeUnit delayUnit, boolean repeats) {
		this(r, delay, delayUnit, repeats, 0L, TimeUnit.SECONDS);
	}

	public Timer(Runnable r, long delay, TimeUnit delayUnit, boolean repeats,
			long initialDelay, TimeUnit initialDelayUnit)
	{
		this.runnable = r;

		this.delayNano = delayUnit.toNanos(delay);
		this.initialDelay = initialDelay;
		this.initialDelayUnit = initialDelayUnit;
		this.lastTickTimeNano = 0L;
		this.repeats = repeats;

		this.delayChangeLock = new Object();
		this.newDelay = 0L;
		this.delayChanged = false;

		this.running = false;
		this.shuttingDown = false;
		this.isWaiting = false;

		this.thread = new Thread(this);
		this.thread.start();
	}

	/**
	 * Starts this timer. If the timer is already running, <code>start</code>
	 * does nothing.
	 * @return True if the timer was successfully started, or false if the
	 * timer was unaffected by the call (it may already be running).
	 */
	public boolean start() {
		return start(initialDelay, initialDelayUnit);
	}

	/**
	 * Starts this timer. The specified <code>initialDelay</code> will
	 * override the timer's set initial delay this one time only.
	 * If the timer is already running, <code>start</code> does nothing.
	 * <br>
	 * <br>
	 * Note: There is a very small chance that <code>start</code> will block
	 * for a very small amount of time (nanoseconds) when {@link isRunning}
	 * returns false.
	 * @return True if the timer was successfully started, or false if the
	 * timer was unaffected by the call (it may already be running).
	 * @param initialDelay - The amount of time to delay the initial call
	 * to runnable.
	 * @param initialDelayUnit - The unit of time of initialDelay.
	 */
	public boolean start(long initialDelay, TimeUnit initialDelayUnit) {
		if (!running) {
			//Blocks until this.wait() is called in timer loop.
			//There is only a tintsy-tiny window where running can be false
			// but (this) lock is not available - when the timer loop
			// approaches the wait() statement.
			synchronized (this) {
				lastTickTimeNano = System.nanoTime() + initialDelayUnit.toNanos(initialDelay);
				running = true;
				this.notifyAll();
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns whether the time is currently active.
	 * @return True if the timer is running; otherwise false.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Stops the timer.
	 */
	public void stop() {
		running = false;
	}

	@Override
	protected void finalize() throws Throwable {
		shutdown();
	}

	/**
	 * Attempts to shut down the timer down gracefully by allowing the runnable to
	 * finish working if it is currently executing.
	 */
	public void shutdown() {
		running = false;
		shuttingDown = true;
		synchronized (this) {
			this.notifyAll();
		}
	}

	/**
	 * Shuts the timer down immediately.
	 */
	public void shutdownNow() {
		shutdown();
		thread.interrupt();
	}

	public void pushback(long delay, TimeUnit unit) {

	}

	public boolean resetTickDelay() {
		if (isWaiting) {
			lastTickTimeNano = System.nanoTime();
			return true;
		}
		return false;
	}

	/**
	 * Sets the time between timer ticks. If the timer is currently running,
	 * then the new delay will go into affect after the current tick.
	 * @param delay - The amount of time for the new delay.
	 * @param unit - The unit of time for the new delay.
	 */
	public void setDelay(long delay, TimeUnit unit) {
		synchronized(delayChangeLock) {
			if (running) {
				//delayChanged is a synchronized flag that the timer loop watches
				//to determine if the delay should change while the timer is active.
				//Once set, the delay will be adjusted upon the next timer loop iteration.
				newDelay = unit.toNanos(delay);
				delayChanged = true;
			} else {
				delayNano = unit.toNanos(delay);
				delayChanged = false;
			}
		}
	}

	/**
	 * Sets the initial delay of the timer. Note that this has no effect on
	 * the timer if it is currently running.
	 * @param initialDelay - The amount of time for the initial delay.
	 * @param unit - The unit of time for the initial delay.
	 */
	public void setInitialDelay(long initialDelay, TimeUnit unit) {
		this.initialDelay = initialDelay;
		this.initialDelayUnit = unit;
	}

	/**
	 * Sets whether this timer should repeat.
	 * Has no effect if the timer is already running.
	 * @param repeats - Whether the timer should repeat.
	 */
	public void setRepeats(boolean repeats) {
		if (!running) {
			this.repeats = repeats;
		}
	}

	/**
	 * Sets the runnable object that will execute each timer tick.
	 * Has no effect if the timer is already running.
	 * @param r - The runnable to fire when the timer finishes.
	 */
	public void setRunnable(Runnable r) {
		if (!running) {
			this.runnable = r;
		}
	}

	@Override public synchronized void run() {
		while (true) {

			while (!running) {
				if (shuttingDown) {
					return;
				} else {
					try {
						this.wait();
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}
			}

			synchronized(delayChangeLock) {
				if (delayChanged) {
					delayNano = newDelay;
					delayChanged = false;
					lastTickTimeNano = System.nanoTime();
				}
			}

			//sleep until delay time is up
			if (System.nanoTime() - lastTickTimeNano < delayNano) {
				isWaiting = true;
				while (running && System.nanoTime() - lastTickTimeNano < delayNano) {
					try {
						Thread.sleep(0);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}
				isWaiting = false;
			}

			//make sure we're still supposed to be running at this point
			if (running) {
				lastTickTimeNano += delayNano;

				try {
					runnable.run();
				} catch(Exception e) {
					e.printStackTrace();
				}

				//todo detect if timer can't keep up (if runnable is taking too long).

				if (!repeats) {
					running = false;
				}
			}

		}

	}

}
