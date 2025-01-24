package org.production;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    void testApp() {
        assertTrue(true, "The test always passes, basic check");
    }

    @Test
    void testExample() {
        int expected = 12;
        int actual = 10 + 2;
        assertEquals(expected, actual, "The result of the addition should be equal to 12");
    }
}
