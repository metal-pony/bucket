package com.sparklicorn.bucket.util.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Events contain information about to some system event, including
 * the name of the event and a map of associated properties.
 * Events can be set in a frozen state with <code>event.freeze()</code>,
 * meaning that additional properties cannot be set on it.
 * Attempting to set or change properties on a frozen event will cause
 * an <code>EventFrozenException</code> (<code>RuntimeException</code>).
 */
public class Event {

	/**
	 * A RuntimeException that is thrown when attempting to add properties
	 * to a frozen Event.
	 */
	public static final class EventFrozenException extends RuntimeException {
		public EventFrozenException() {
			super();
		}

		public EventFrozenException(String message) {
			super(message);
		}
	}

	public final String name;

	private Map<String, Object> properties;

	private boolean frozen;

	/**
	 * Creates a new Event with the given name.
	 * @param name - Name of the event. This cannot be changed later.
	 */
	public Event(String name) {
		this(name, null);
	}

	/**
	 * Creates a new Event with the given name and copies the given properties
	 * into the event's properties map.
	 * @param name - Name of the event. This cannot be changed later.
	 * @param properties - Properties to set on the new event. Can be null.
	 */
	public Event(String name, Map<String,Object> properties) {
		this.name = name;
		this.frozen = false;
		this.properties = new HashMap<>();

		if (properties != null) {
			shallowCopyProperties(properties);
		}
	}

	// Copies entries from the given map to this event's properties.
	private void shallowCopyProperties(Map<String,Object> properties) {
		for (Entry<String,Object> p : properties.entrySet()) {
			this.properties.put(p.getKey(), p.getValue());
		}
	}

	/**
	 * Sets the event to a frozen state. A frozen event cannot add properties.
	 */
	public void freeze() {
		this.frozen = true;
	}

	/**
	 * Whether this event is in a frozen state. A frozen event cannot add properties.
	 * @return True if the event is frozen; otherwise false.
	 */
	public boolean isFrozen() {
		return this.frozen;
	}

	/**
	 * Attempts to add a property to the event.
	 * @param key - Name of the property.
	 * @param value - Property value.
	 * @return True if the property was successfully added; otherwise false.
	 * @throws EventFrozenException If the Event has been frozen, it cannot have additional
	 * properties set.
	 */
	public boolean addProperty(String key, Object value) throws EventFrozenException {
		if (isFrozen()) {
			throw new EventFrozenException("Cannot add property to frozen Event.");
		}

		properties.put(key, value);
		return true;
	}

	/**
	 * Gets the event property as an int.
	 * @param name - Name of the property.
	 * @return The event property as an int.
	 */
	public int getPropertyAsInt(String name) {
		return (int)(getPropertyAsType(name, Integer.class));
	}

	/**
	 * Gets the event property as a String.
	 * @param name - Name of the property.
	 * @return The event property as a String.
	 */
	public String getPropertyAsString(String name) {
		return getPropertyAsType(name, String.class);
	}

	/**
	 * Gets the event property as a double.
	 * @param name - Name of the property.
	 * @return The event property as a double.
	 */
	public double getPropertyAsDouble(String name) {
		return (double)getPropertyAsType(name, Double.class);
	}

	/**
	 * Gets the event property as a float.
	 * @param name - Name of the property.
	 * @return The event property as a float.
	 */
	public float getPropertyAsFloat(String name) {
		return (float)getPropertyAsType(name, Float.class);
	}

	/**
	 * Gets the event property as a boolean.
	 * @param name - Name of the property.
	 * @return The event property as a boolean.
	 */
	public boolean getPropertyAsBoolean(String name) {
		return (boolean)getPropertyAsType(name, Boolean.class);
	}

	/**
	 * Gets the event property as an Object.
	 * @param name - Name of the property.
	 * @return The event property as an Object.
	 */
	public Object getProperty(String name) {
		return (Object) properties.get(name);
	}

	/**
	 * Attempts to retrieve a property cast to the specified class.
	 * @param <T> - Type to cast the property to.
	 * @param name - Name of the property.
	 * @param _class - Class to attempt to cast the property to.
	 * @return The property cast as the specified Class.
	 */
	public <T> T getPropertyAsType(String name, Class<T> _class) {
		return _class.cast(properties.get(name));
	}
}
