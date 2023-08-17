package com.sparklicorn.bucket.util.event;

import java.util.ArrayList;
import java.util.HashMap;
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
	private static class WorkItem {
		final Consumer<Event> listener;
		final Event event;

		private WorkItem(Consumer<Event> listener, Event event) {
			this.listener = listener;
			this.event = event;
		}
	}

	private HashMap<String, ArrayList<Consumer<Event>>> eventListeners;

	private Thread executorThread;
	private AtomicBoolean isShutDown;
	private BlockingQueue<WorkItem> workQueue;

	/**
	 * Creates a new EventBus and immediately starts a worker thread.
	 */
	public EventBus() {
		eventListeners = new HashMap<>();

		workQueue = new LinkedBlockingQueue<>();
		isShutDown = new AtomicBoolean();

		executorThread = new Thread(() -> {
			while (!isShutDown.get()) {
				try {
					WorkItem work = workQueue.poll(2, TimeUnit.SECONDS);
					if (!isShutDown.get() && work != null) {
						work.listener.accept(work.event);
					}
				} catch (InterruptedException ex) {
					// The thread was interrupted, probably by disposeImmediately()
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
		if (isShutDown.get()) {
			return false;
		}

		if (eventName == null || listener == null) {
			throw new IllegalArgumentException();
		}

		boolean result = false;

		String _eventName = eventName.toUpperCase();

		if (eventListeners.containsKey(_eventName)) {
			boolean alreadyRegistered = false;
			for (Consumer<Event> el : eventListeners.get(_eventName)) {
				if (el == listener) {
					alreadyRegistered = true;
					break;
				}
			}

			if (!alreadyRegistered) {
				eventListeners.get(_eventName).add(listener);
				result = true;
			}
		} else {
			ArrayList<Consumer<Event>> list = new ArrayList<>();
			list.add(listener);
			eventListeners.put(_eventName, list);
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
		for (String s : eventListeners.keySet()) {
			unregisterEvent(s);
		}
	}

	/**
	 * Unregisters all listeners tied to the given event name.
	 * @param eventName - Name of the event to unregister.
	 * @return True if any listener was removed as result of this; otherwise false.
	 */
	public boolean unregisterEvent(String eventName) {
		if (eventListeners.containsKey(eventName)) {
			eventListeners.remove(eventName);
			return true;
		}

		return false;
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
			String eventName = event.name.toUpperCase();
			ArrayList<Consumer<Event>> _listeners = eventListeners.get(eventName);
			if (_listeners != null && !_listeners.isEmpty()) {
				for (Consumer<Event> el : _listeners) {
					workQueue.offer(new WorkItem(el, event));
				}
			}
		}
	}
}
