package com.sparklicorn.bucket.util.event;

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

	public static void validateEventName(String name) {
		if (!name.matches(Event.NAME_PATTERN)) {
			throw new MalformedNameException(MalformedNameException.BAD_EVENT_NAME);
		}
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
		this(name, null);
	}

	/**
	 * Creates a new Event with the given name and copies the given properties
	 * into the event's properties map.
	 *
	 * @param name - Name of the event. Cannot be null or empty.
	 * @param properties - Properties to set on the event.
	 */
	public Event(String name, Map<String,Object> properties) {
		if(name == null) {
			throw new NullPointerException("Name must not be null");
		}

		validateEventName(name);

		this.name = name;
		this.frozen = false;
		this.properties = new HashMap<>();

		if (properties != null) {
			properties.forEach((propName, value) -> this.addProperty(propName, value));
		}
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
	 * @param name - Name of the property.
	 * @param value - Property value.
	 * @return True if the property was successfully added; otherwise false.
	 * @throws EventFrozenException If the Event has been frozen, it cannot have additional
	 * properties set.
	 */
	public boolean addProperty(String name, Object value) throws EventFrozenException {
		if (isFrozen()) {
			throw new EventFrozenException(EventFrozenException.CANNOT_ADD_PROPERTY);
		}

		if (!name.matches(NAME_PATTERN)) {
			throw new MalformedNameException(MalformedNameException.BAD_PROPERTY_NAME);
		}

		properties.put(name, value);
		return true;
	}

	/**
	 * Gets the event property as an int.
	 *
	 * @param name - Name of the property.
	 * @return The event property as an int.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public int getPropertyAsInt(String name) {
		return (int)getPropertyAsType(name, Integer.class);
	}

	/**
	 * Gets the event property as a String.
	 *
	 * @param name - Name of the property.
	 * @return The event property as a String.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public String getPropertyAsString(String name) {
		return getPropertyAsType(name, String.class);
	}

	/**
	 * Gets the event property as a double.
	 *
	 * @param name - Name of the property.
	 * @return The event property as a double.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public double getPropertyAsDouble(String name) {
		return (double)getPropertyAsType(name, Double.class);
	}

	/**
	 * Gets the event property as a boolean.
	 *
	 * @param name - Name of the property.
	 * @return The event property as a boolean.
	 * @throws NullPointerException If the specified property is not found.
	 */
	public boolean getPropertyAsBoolean(String name) {
		return (boolean)getPropertyAsType(name, Boolean.class);
	}

	/**
	 * Gets the event property as an Object.
	 *
	 * @param name - Name of the property.
	 * @return The event property as an Object.
	 */
	public Object getProperty(String name) {
		return (Object) properties.get(name);
	}

	/**
	 * Attempts to retrieve a property cast to the specified class.
	 *
	 * @param <T> - Type to cast the property to.
	 * @param name - Name of the property.
	 * @param _class - Class to attempt to cast the property to.
	 * @return The property cast as the specified Class.
	 */
	public <T> T getPropertyAsType(String name, Class<T> _class) {
		return _class.cast(properties.get(name));
	}
}
