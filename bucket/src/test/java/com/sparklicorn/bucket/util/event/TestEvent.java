package com.sparklicorn.bucket.util.event;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sparklicorn.bucket.util.event.Event.EventFrozenException;
import com.sparklicorn.bucket.util.event.Event.MalformedNameException;

public class TestEvent {

  static final String eventName = "TEST_EVENT";

  static final String[] invalidNames = new String[] {
    "", ".", "?", "\\", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "+", "=",
    "|", "'", "\"", ";", ":", ",", "<", ".", ">", "/", "~", "`",
    "0mustStartWithLetter", "1meow", "666pow",
    "_mustStartWithLetter", "-mustEhhNo", "045-___-definitelyNot",
    "($*%&#NGFSO(DU*FYP($*NOSIUDFGHOH&*^H*O",
    "may not contain whitespace", "dont-be\nridiculous\twith your  event\n names_69",
    "1", "12", "123", "1234aaa"
  };

  Event event;

  @BeforeEach
  void before() {
    event = new Event(eventName);
  }

  @Test
  void testConstructor_setsName() {
    assertEquals(eventName, event.name);
  }

  @Test
  void testConstructor_whenNameIsNull_throws() {
    assertThrows(NullPointerException.class, () -> {
      new Event(null);
    });
  }

  @Test
  void testConstructor_whenNameIsInvalid_throws() {
    for (String invalidName : invalidNames) {
      String message = String.format(
        "Expected event name \"%s\" to throw MalformedNameException",
        invalidName
      );

      assertThrows(MalformedNameException.class, () -> {
        new Event(invalidName);
      }, message);
    }
  }

  @Test
  void testConstructor_whenGivenPropertyHasInvalidName_throws() {
    for (String invalidName : invalidNames) {
      String message = String.format(
        "Expected property name \"%s\" to throw MalformedNameException",
        invalidName
      );

      assertThrows(MalformedNameException.class, () -> {
        Event testEvent = new Event(eventName);
        testEvent.addProperty(invalidName, "doesn't matter");
      }, message);
    }
  }

  @Test
  void testFreeze() {
    assertFalse(event.isFrozen());
    event.freeze();
    assertTrue(event.isFrozen());
  }

  @Test
  void testAddProperty_returnsTrue() {
    assertTrue(event.addProperty("testProp", 1));
  }

  @Test
  void testAddProperty_replacesExistingProperty() {
    event.addProperty("testProp", 1);
    event.addProperty("testProp", 2);
    assertEquals(2, event.getPropertyAsInt("testProp"));
  }

  @Test
  void testAddProperty_whenNameIsNull_throws() {
    assertThrows(NullPointerException.class, () -> {
      event.addProperty(null, "doesn't matter");
    });
  }

  @Test
  void testAddProperty_whenNameIsInvalid_throws() {
    for (String invalidName : invalidNames) {
      String message = String.format(
        "Expected event name \"%s\" to throw MalformedNameException",
        invalidName
      );

      assertThrows(MalformedNameException.class, () -> {
        event.addProperty(invalidName, "meow");
      }, message);
    }
  }

  @Test
  void testAddProperty_whenFrozen_throws() {
    event.freeze();
    assertThrows(EventFrozenException.class, () -> {
      event.addProperty("testProp", 1);
    });
  }

  @Test
  void testGetProperty() {
    event.addProperty("testProp", this);
    assertEquals(this, event.getProperty("testProp"));
  }

  @Test
  void testGetProperty_whenPropertyIsNotSet_returnsNull() {
    assertNull(event.getProperty("testProp"));
  }

  @Test
  void testGetPropertyAsType() {
    event.addProperty("testProp", this);
    assertEquals(this, event.getPropertyAsType("testProp", TestEvent.class));
  }

  @Test
  void testGetPropertyAsType_whenPropertyIsNotSet_returnsNull() {
    assertNull(event.getPropertyAsType("testProp", TestEvent.class));
  }

  @Test
  void testGetPropertyAsType_whenPropertyIsNull_returnsNull() {
    assertNull(event.getPropertyAsType("testProp", TestEvent.class));
  }

  @Test
  void testGetPropertyAsType_whenPropertyIsNotGivenType_throws() {
    event.addProperty("testProp", 9);
    assertThrows(ClassCastException.class, () -> {
      event.getPropertyAsType("testProp", TestEvent.class);
    });
  }

  @Test
  void testGetPropertyMethods() {
    event.addProperty("intProp", 11);
    event.addProperty("stringProp", "meow");
    event.addProperty("doubleProp", Double.MAX_VALUE);
    event.addProperty("booleanProp", true);

    assertEquals(11, event.getPropertyAsInt("intProp"));
    assertEquals("meow", event.getPropertyAsString("stringProp"));
    assertEquals(Double.MAX_VALUE, event.getPropertyAsDouble("doubleProp"));
    assertEquals(true, event.getPropertyAsBoolean("booleanProp"));
  }

  @Test
  void testGetPropertyAsInt_whenPropertyIsNotSet_throws() {
    assertThrows(NullPointerException.class, () -> {
      event.getPropertyAsInt("testProp");
    });
  }

  @Test
  void testGetPropertyAsString_whenPropertyIsNotSet_throws() {
    assertThrows(NullPointerException.class, () -> {
      event.getPropertyAsInt("testProp");
    });
  }

  @Test
  void testGetPropertyAsDouble_whenPropertyIsNotSet_throws() {
    assertThrows(NullPointerException.class, () -> {
      event.getPropertyAsInt("testProp");
    });
  }

  @Test
  void testGetPropertyAsBoolean_whenPropertyIsNotSet_throws() {
    assertThrows(NullPointerException.class, () -> {
      event.getPropertyAsInt("testProp");
    });
  }
}
