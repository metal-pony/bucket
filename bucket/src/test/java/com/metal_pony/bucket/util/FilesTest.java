package com.metal_pony.bucket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

public class FilesTest {

    @Test
    public void readString_whenInputStreamIsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            Files.readString(null);
        });
    }

    @Test
    public void readString_returnsAllContentInTheStream() {
        String expected = "This is all the content in the input stream";
        InputStream in = new ByteArrayInputStream(expected.getBytes());
        String actual = null;
        try {
            actual = Files.readString(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(expected, actual);
    }

    @Test
    public void readString_whenInputStreamIsEmpty_returnsEmptyString() {
        String expected = "";
        InputStream in = new ByteArrayInputStream(new byte[]{});
        String actual = null;
        try {
            actual = Files.readString(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(expected, actual);
    }
}
