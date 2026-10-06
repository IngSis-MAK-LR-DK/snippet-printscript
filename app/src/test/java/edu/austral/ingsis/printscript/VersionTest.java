package edu.austral.ingsis.printscript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void currentVersionIsExposed() {
        assertEquals("1.1", Version.current());
    }
}
