package com.sparklicorn.bucket.util.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

public class TestEventBus {
  @Spy
  EventBus bus;

  String testEventName;
  Consumer<Event> testNoopListener;

  static final String[] invalidNames = new String[] {
    "", ".", "?", "\\", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "+", "=",
    "|", "'", "\"", ";", ":", ",", "<", ".", ">", "/", "~", "`",
    "0mustStartWithLetter", "1meow", "666pow",
    "_mustStartWithLetter", "-mustEhhNo", "045-___-definitelyNot",
    "($*%&#NGFSO(DU*FYP($*NOSIUDFGHOH&*^H*O",
    "may not contain whitespace", "dont-be\nridiculous\twith your  event\n names_69",
    "1", "12", "123", "1234aaa"
  };

  @BeforeEach
  void beforeEach() {
    bus = spy(new EventBus());
    testEventName = "test_event";
    testNoopListener = (event) -> {};
  }

  @AfterEach
  void after() {
    bus.dispose(true);
  }

  @Test
  void test_constructor() {
    new EventBus();
  }

  @Test
  void test_constructorWithInvalidPollingRate_throws() {
    long[] invalidPollingRates = new long[] {
      0L, -1L, -2L, -10L, -1000L,
      EventBus.MAX_POLLING_RATE + 1L, EventBus.MAX_POLLING_RATE + 2L,
      EventBus.MAX_POLLING_RATE + 10L, Long.MAX_VALUE
    };

    for (long invalidPollingRate : invalidPollingRates) {
      assertThrows(IllegalArgumentException.class, () -> {
        new EventBus(invalidPollingRate);
      });
    }
  }

  @Test
  void test_registerEventListener_returnsTrue() {
    assertTrue(bus.registerEventListener(testEventName, testNoopListener));
  }

  @Test
  void test_registerEventListener_whenEventNameIsNull_throws() {
    assertThrows(NullPointerException.class, () -> {
      bus.registerEventListener(null, testNoopListener);
    });
  }

  @Test
  void test_registerEventListener_whenListenerIsNull_throws() {
    assertThrows(NullPointerException.class, () -> {
      bus.registerEventListener(testEventName, null);
    });
  }

  @Test
  void test_registerEventListener_whenEventNameIsInvalid_throws() {
    for (String invalidName : invalidNames) {
      String message = String.format(
        "Expected event name \"%s\" to throw MalformedNameException",
        invalidName
      );

      assertThrows(Event.MalformedNameException.class, () -> {
        bus.registerEventListener(invalidName, testNoopListener);
      }, message);
    }
  }

  @Test
  void test_registerEventListener_whenShutdown_returnsFalse() {
    bus.dispose(false);
    assertFalse(bus.registerEventListener(testEventName, testNoopListener));
  }

  @Test
  void test_registerEventListener_whenListenerIsAlreadyRegistered_returnsFalse() {
    assertTrue(bus.registerEventListener(testEventName, testNoopListener));
    assertFalse(bus.registerEventListener(testEventName, testNoopListener));
  }

  @Test
  void test_unregisterAll_callsUnregisterForEveryRegisteredEvent() {
    String[] eventNames = new String[] {
      "TEST_EVENT_1", "TEST_EVENT_2", "TEST_EVENT_3"
    };

    for (String eventName: eventNames) {
      bus.registerEventListener(eventName, testNoopListener);
    }

    bus.unregisterAll();

    for (String eventName : eventNames) {
      verify(bus).unregisterEvent(eventName);
    }
  }

  @Test
  void test_unregisterAll_whenNoEventsAreRegistered_doesNotCallUnregisterEvent() {
    bus.unregisterAll();
    verify(bus, never()).unregisterEvent(Mockito.anyString());
  }

  @Test
  void test_unregisterEvent_returnsTrue() {
    bus.registerEventListener(testEventName, testNoopListener);
    assertTrue(bus.unregisterEvent(testEventName));
    verify(bus, times(1)).unregisterEventListener(testEventName, testNoopListener);
  }

  @Test
  void test_unregisterEvent_whenNameIsNull_throws() {
    assertThrows(NullPointerException.class, () -> {
      bus.unregisterEvent(null);
    });
  }

  @Test
  void test_unregisterEvent_whenEventHasNotBeenRegistered_returnsFalse() {
    bus.registerEventListener(testEventName, testNoopListener);
    assertFalse(bus.unregisterEvent(testEventName + "_BANANA"));
    verify(bus, never()).unregisterEventListener(Mockito.anyString(), Mockito.any());
  }
}
