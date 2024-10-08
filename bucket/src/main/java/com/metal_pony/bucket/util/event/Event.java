package com.metal_pony.bucket.util.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Events contain information about to some system event, including
 * the name of the event and a map of associated properties.
 * Events can be set in a frozen state with <code>event.freeze()</code>,
 * meaning that additional properties cannot be set on it.
 * Attempting to set or change properties on a frozen event will cause
 * an <code>EventFrozenException</code> (<code>RuntimeException</code>).
 */
public class Event {

	public static final String NAME_PATTERN = "[a-zA-Z][a-zA-Z_0-9\\-]*";

	/**
	 * A RuntimeException thrown when attempting to add properties
	 * to a frozen Event.
	 */
	public static final class EventFrozenException extends RuntimeException {
		public static final String CANNOT_ADD_PROPERTY = "Cannot add property to frozen Event.";

		public EventFrozenException() { super(); }
		public EventFrozenException(String message) { super(message); }
	}

	/**
	 * A RuntimeException thrown when the provided event name or property name
	 * does not conform to the specified pattern.
	 */
	public static final class MalformedNameException extends RuntimeException {
		static final String BAD_NAME = """
			Name must start with a letter and may only contain
			lower and uppercase letters, numbers, and characters \"-_\"
		""";
		static final String BAD_EVENT_NAME = "Bad Event name: " + BAD_NAME;
		static final String BAD_PROPERTY_NAME = "Bad Event property name: " + BAD_NAME;

		public MalformedNameException() { super(); }
		public MalformedNameException(String message) { super(message); }
	}

	public final String name;

	private Map<String, Object> properties;

	private boolean frozen;

	/**
	 * Creates a new Event with the given name.
	 *
	 * @param name - Name of the event. Cannot be null or empty.
	 */
	public Event(String name) {
		if(name == null) {
			throw new NullPointerException("Name must not be null");
		}

		if (!name.matches(Event.NAME_PATTERN)) {
			throw new MalformedNameException(MalformedNameException.BAD_EVENT_NAME);
		}

		this.name = name;
		this.frozen = false;
	}

	/**
	 * Sets the event to a frozen state.
	 * A frozen event cannot add properties and cannot be unfrozen.
	 */
	public void freeze() {
		this.frozen = true;
	}

	/**
	 * Whether this event is in a frozen state. A frozen event cannot add properties.
	 *
	 * @return True if the event is frozen; otherwise false.
	 */
	public boolean isFrozen() {
		return this.frozen;
	}

	/**
	 * Attempts to add a property to the event.
	 *
	 * @param propName - Name of the property.
	 * @param value - Property value.
	 * @return True if the property was successfully added; otherwise false.
	 * @throws EventFrozenException If the Event has been frozen, it cannot have additional
	 * properties set.
	 */
	public boolean addProperty(String propName, Object value) throws EventFrozenException {
		if (isFrozen()) {
			throw new EventFrozenException(EventFrozenException.CANNOT_ADD_PROPERTY);
		}

		if (!propName.matches(NAME_PATTERN)) {
			throw new MalformedNameException(MalformedNameException.BAD_PROPERTY_NAME);
		}

		if (properties == null) {
			properties = new HashMap<>();
		}

		properties.put(propName, value);
		return true;
	}

	/**
	 * Returns whether the event has a property with the given name.
	 *
	 * @param propName Name of the property.
	 * @return True if the event has a property with the given name; otherwise false.
	 */
	public boolean hasProperty(String propName) {
		return properties != null && properties.containsKey(propName);
	}

	/**
	 * Gets the event property as an int.
	 *
	 * @param propName - Name of the property.
	 * @return The event property as an int.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public int getPropertyAsInt(String propName) {
		return (int)getPropertyAsType(propName, Integer.class);
	}

	/**
	 * Gets the event property as a long.
	 *
	 * @param propName Name of the property.
	 * @return The event property as a long.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public long getPropertyAsLong(String propName) {
		return (long)getPropertyAsType(propName, Long.class);
	}

	/**
	 * Gets the event property as a String.
	 *
	 * @param propName - Name of the property.
	 * @return The event property as a String.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public String getPropertyAsString(String propName) {
		return getPropertyAsType(propName, String.class);
	}

	/**
	 * Gets the event property as a double.
	 *
	 * @param propName - Name of the property.
	 * @return The event property as a double.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public double getPropertyAsDouble(String propName) {
		return (double)getPropertyAsType(propName, Double.class);
	}

	/**
	 * Gets the event property as a boolean.
	 *
	 * @param propName - Name of the property.
	 * @return The event property as a boolean.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public boolean getPropertyAsBoolean(String propName) {
		return (boolean)getPropertyAsType(propName, Boolean.class);
	}

	/**
	 * Gets the event property as an Object.
	 *
	 * @param propName - Name of the property.
	 * @return The event property as an Object.
	 */
	public Object getProperty(String propName) {
		if (properties == null) {
			return null;
		}
		return (Object) properties.get(propName);
	}

	/**
	 * Attempts to retrieve a property cast to the specified class.
	 *
	 * @param <T> - Type to cast the property to.
	 * @param propName - Name of the property.
	 * @param _class - Class to attempt to cast the property to.
	 * @return The property cast as the specified Class.
	 */
	public <T> T getPropertyAsType(String propName, Class<T> _class) {
		if (properties == null) {
			return null;
		}
		return _class.cast(properties.get(propName));
	}
}
