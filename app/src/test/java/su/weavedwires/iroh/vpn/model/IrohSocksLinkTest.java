package su.weavedwires.iroh.vpn.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class IrohSocksLinkTest {

    @Test
    public void parse_fullCredentials() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://alice:secret@node1");
        assertEquals("alice", link.getUser());
        assertEquals("secret", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertNull(link.getName());
    }

    @Test
    public void parse_withoutPassword() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://alice@node1");
        assertEquals("alice", link.getUser());
        assertNull(link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_ticketOnly() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://node1");
        assertNull(link.getUser());
        assertNull(link.getPassword());
        assertEquals("node1", link.getTicket());
        assertNull(link.getName());
    }

    @Test
    public void parse_withNameFragment() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://alice:secret@node1#My%20VPN");
        assertEquals("alice", link.getUser());
        assertEquals("secret", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("My VPN", link.getName());
    }

    @Test
    public void parse_nameWithoutCredentials() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://node1#Home");
        assertNull(link.getUser());
        assertNull(link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("Home", link.getName());
    }

    @Test
    public void parse_encodedAtPassword() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://alice:p%40ss@node1");
        assertEquals("alice", link.getUser());
        assertEquals("p@ss", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_passwordContainingColon() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://alice:pa:ss@node1");
        assertEquals("alice", link.getUser());
        assertEquals("pa:ss", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_encodedUser() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://al%40ice:pw@node1");
        assertEquals("al@ice", link.getUser());
        assertEquals("pw", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_encodedSlashInPassword() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://alice:pa%2Fss@node1");
        assertEquals("alice", link.getUser());
        assertEquals("pa/ss", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_encodedHashInName() {
        IrohSocksLink link = new IrohSocksLink("irohsocks://node1#a%23b");
        assertEquals("a#b", link.getName());
    }

    @Test
    public void parse_literalAtThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new IrohSocksLink("irohsocks://alice:p@ss@node1"));
    }

    @Test
    public void parse_literalSpaceThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new IrohSocksLink("irohsocks://alice:p ss@node1"));
    }

    @Test
    public void parse_schemeCaseInsensitive() {
        IrohSocksLink link = new IrohSocksLink("IROHSOCKS://alice:secret@node1");
        assertEquals("alice", link.getUser());
        assertEquals("secret", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_trimsWhitespace() {
        IrohSocksLink link = new IrohSocksLink("  irohsocks://u:p@t  ");
        assertEquals("u", link.getUser());
        assertEquals("p", link.getPassword());
        assertEquals("t", link.getTicket());
    }

    @Test
    public void parse_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> new IrohSocksLink((String) null));
    }

    @Test
    public void parse_invalidSchemeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new IrohSocksLink("http://u:p@t"));
    }

    @Test
    public void parse_emptyTicketThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new IrohSocksLink("irohsocks://u:p@"));
    }

    @Test
    public void toString_roundTrips() {
        String raw = "irohsocks://alice:secret@node1";
        IrohSocksLink link = new IrohSocksLink(raw);
        assertEquals(raw, link.toString());
    }

    @Test
    public void toString_roundTripsWithName() {
        String raw = "irohsocks://alice:secret@node1#My%20VPN";
        IrohSocksLink link = new IrohSocksLink(raw);
        assertEquals(raw, link.toString());
    }

    @Test
    public void toString_roundTripsEncodedPassword() {
        String raw = "irohsocks://alice:p%40ss@node1";
        IrohSocksLink link = new IrohSocksLink(raw);
        assertEquals("p@ss", link.getPassword());
        assertEquals(raw, link.toString());
    }

    @Test
    public void assemble_percentEncodesComponents() {
        IrohSocksLink link = new IrohSocksLink("My VPN", "alice", "p@ss", "node1");
        assertEquals("alice", link.getUser());
        assertEquals("p@ss", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("My VPN", link.getName());
        assertEquals("irohsocks://alice:p%40ss@node1#My%20VPN", link.toString());
    }
}
