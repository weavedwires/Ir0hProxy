package su.weavedwires.iroh.vpn.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConnectionTest {

    @Test
    public void generatedIdsAreDistinct() {
        assertNotEquals(
                new Connection("name", "user", "password", "ticket").getId(),
                new Connection("name", "user", "password", "ticket").getId()
        );
    }

    @Test
    public void generatedIdsAreSequential() {
        long first = new Connection("name", "user", "password", "ticket").getId();
        long second = new Connection("name", "user", "password", "ticket").getId();
        assertEquals(first + 1, second);
    }

    @Test
    public void explicitIdAdvancesGenerator() {
        new Connection(100L, "name", "user", "password", "ticket");
        Connection generated = new Connection("name", "user", "password", "ticket");
        assertTrue(generated.getId() > 100L);
    }
}
