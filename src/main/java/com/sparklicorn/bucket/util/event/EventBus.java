package com.sparklicorn.bucket.util.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * An EventBus maintains an index of event listeners (<code>Consumer</code>s
 * that perform some action given an <code>Event</code>). Listeners are registered
 * to a specific event name, and invoking <code>eventBus.throwEvent(event)</code>
 * queues all listeners registered to <code>event.name</code> for execution.
 * <br><br>
 * An EventBus also maintains a Thread used for listener execution. The thread continuously
 * polls for queued listeners until shutdown, executing each listener one at a time.
 * Call <code>dispose(false)</code> to stop the EventBus gracefully, allowing any listener
 * currently executing to continue until finished, or <code>dispose(true)</code> to interrupt
 * the thread.
 */
public class EventBus {
	public static final long MIN_POLLING_RATE = 1L;
	public static final long MAX_POLLING_RATE = TimeUnit.HOURS.toMillis(1L);
	public static final long DEFAULT_POLLING_RATE = TimeUnit.SECONDS.toMillis(1L);

	private static final String POLLING_RATE_OUT_OF_BOUNDS =
		"polling rate out of bounds";

	private static class WorkItem {
		final Consumer<Event> listener;
		final Event event;

		private WorkItem(Consumer<Event> listener, Event event) {
			this.listener = listener;
			this.event = event;
		}
	}

	private long pollingRate;
	private HashMap<String, ArrayList<Consumer<Event>>> eventListeners;

	private Thread executorThread;
	private AtomicBoolean isShutDown;
	private BlockingQueue<WorkItem> workQueue;

	/**
	 * Creates a new EventBus with the default polling rate and starts the worker thread.
	 * The thread will continue to poll the event queue until it is shut down by calling
	 * <code>dispose(force)</code>.
	 */
	public EventBus() {
		this(DEFAULT_POLLING_RATE);
	}

	/**
	 * Creates a new EventBus with the given polling rate and starts the worker thread.
	 * The thread will continue to poll the event queue until it is shut down by calling
	 * <code>dispose(force)</code>.
	 *
	 * @param pollingRate The maximum length of time the worker thread will spend waiting for events
	 * from the work queue. The thread checks
	 *
	 */
	public EventBus(long pollingRate) {
		if (pollingRate < MIN_POLLING_RATE || pollingRate > MAX_POLLING_RATE) {
			throw new IllegalArgumentException(POLLING_RATE_OUT_OF_BOUNDS);
		}

		eventListeners = new HashMap<>();
		workQueue = new LinkedBlockingQueue<>();
		isShutDown = new AtomicBoolean(false);
		this.pollingRate = pollingRate;

		executorThread = new Thread(() -> {
			while (!isShutDown.get()) {
				try {
					WorkItem work = workQueue.poll(this.pollingRate, TimeUnit.MILLISECONDS);
					if (!isShutDown.get() && work != null) {
						work.listener.accept(work.event);
					}
				} catch (InterruptedException ex) {
					// The thread was interrupted, probably by dispose(force: true)
					// Do nothing
				}
			}
		});
		executorThread.start();
	}

	/**
	 * Starts shutting down the EventBus. No new event listeners may be registered, no new
	 * events may be thrown, and no registered listeners that are queued to run will be executed.
	 * The work queues and constructs holding references to events and listeners will be cleared,
	 * and executor thread will shutdown.
	 * @param force - When true, the executor thread(s) will be interrupted; otherwise the thread,
	 * and any listener currently running on it, will be allowed to execute until completion.
	 */
	public synchronized void dispose(boolean force) {
		isShutDown.set(true);

		if (force) {
			executorThread.interrupt();
		}

		workQueue.clear();
		eventListeners.clear();
	}

	/**
	 * Registers the given event listener. Once registered, the listener will be executed
	 * every time <code>#throwEvent</code> is called with <code>eventName</code>.
	 * No-op if the listener is already registered for the given eventName, or
	 * if the eventBus has been disposed.
	 * @param eventName - Name of the event the listener should be attached to.
	 * @param listener - Listener function to register.
	 * @return True if the listener was registered; otherwise false.
	 */
	public boolean registerEventListener(String eventName, Consumer<Event> listener) {
		Event.validateEventName(eventName);

		if (listener == null) {
			throw new NullPointerException();
		}

		if (isShutDown.get()) {
			return false;
		}

		boolean result = false;

		if (eventListeners.containsKey(eventName)) {
			boolean alreadyRegistered = false;
			for (Consumer<Event> el : eventListeners.get(eventName)) {
				if (el == listener) {
					alreadyRegistered = true;
					break;
				}
			}

			if (!alreadyRegistered) {
				eventListeners.get(eventName).add(listener);
				result = true;
			}
		} else {
			ArrayList<Consumer<Event>> list = new ArrayList<>();
			list.add(listener);
			eventListeners.put(eventName, list);
			result = true;
		}

		return result;
	}

	/**
	 * Unregisters a specific listener for a specific event.
	 * Note that if the listener is currently running, it will continue to execute.
	 * @param eventName - Name of the event the listener is attached to.
	 * @param listener - Reference to the listener to unregister.
	 * @return True if the listener was successfully unregistered; otherwise false.
	 */
	public boolean unregisterEventListener(String eventName, Consumer<Event> listener) {
		boolean result = false;
		if (eventListeners.containsKey(eventName)) {
			result = eventListeners.get(eventName).remove(listener);
		}
		return result;
	}

	/**
	 * Unregisters all event listeners. Note that any listener that is currently
	 * running will continue to execute.
	 */
	public void unregisterAll() {
		HashSet<String> eventNames = new HashSet<>(eventListeners.keySet());
		for (String s : eventNames) {
			unregisterEvent(s);
		}
	}

	/**
	 * Unregisters all listeners tied to the given event name.
	 *
	 * @param eventName - Name of the event to unregister.
	 * @return True if any listener was removed as result of this; otherwise false.
	 */
	public boolean unregisterEvent(String eventName) {
		if (eventName == null) {
			throw new NullPointerException();
		}

		AtomicBoolean result = new AtomicBoolean(false);

		List<Consumer<Event>> listeners = eventListeners.get(eventName);
		if (listeners == null) {
			return result.get();
		}

		new ArrayList<>(listeners).forEach((listener) -> {
			unregisterEventListener(eventName, listener);
			result.set(true);
		});

		return result.get();
	}

	/**
	 * Causes any listeners registered to <code>event</code> to be executed.
	 * Note that the order of execution is arbitrary.
	 * Does nothing if the EventBus has been disposed.
	 * The given event will be frozen, i.e. no additional properties can be set on it.
	 * @param event - The event being thrown.
	 */
	public void throwEvent(Event event) {
		if (!isShutDown.get() && event != null) {
			event.freeze();
			ArrayList<Consumer<Event>> _listeners = eventListeners.get(event.name);
			if (_listeners != null && !_listeners.isEmpty()) {
				for (Consumer<Event> el : _listeners) {
					workQueue.offer(new WorkItem(el, event));
				}
			}
		}
	}
}
