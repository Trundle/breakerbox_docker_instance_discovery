package de.hammerhartes.andy.breakerbox;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DockerDiscovery}.
 */
@RunWith(Enclosed.class)
public class DockerDiscoveryTest {

    /**
     * Parametrized tests for {@link DockerDiscovery#stripHubAndVersion(String)}.
     */
    @RunWith(Parameterized.class)
    public static class StripHubAndVersionTest {

        @Parameterized.Parameters(name = "{0} returns {1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"debian:latest", "debian"},
                    {"debian:7.0", "debian"},
                    {"hub.example.org/debian:latest", "debian"},
                    {"hub.example.org/debian:7.0", "debian"},
                    {"hub.example.org/org/spam:latest", "org/spam"},
                    {"hub.example.org/org/spam:123", "org/spam"},
                    });
        }

        @Parameterized.Parameter(0)
        public String input;

        @Parameterized.Parameter(1)
        public String expected;

        @Test
        public void testStripHubAndVersion() {
            final String result = DockerDiscovery.stripHubAndVersion(input);
            assertEquals(expected, result);
        }
    }

}
