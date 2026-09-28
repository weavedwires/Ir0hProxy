package su.weavedwires.iroh.vpn.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class IrohSocksLinkTest {

    @Test
    public void parse_fullCredentials() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://alice:secret@node1");
        assertEquals("alice", link.getUser());
        assertEquals("secret", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("", link.getName());
    }

    @Test
    public void parse_withoutPassword() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://alice@node1");
        assertEquals("alice", link.getUser());
        assertEquals("", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_ticketOnly() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://node1");
        assertEquals("", link.getUser());
        assertEquals("", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("", link.getName());
    }

    @Test
    public void parse_withNameFragment() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://alice:secret@node1#My%20VPN");
        assertEquals("alice", link.getUser());
        assertEquals("secret", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("My VPN", link.getName());
    }

    @Test
    public void parse_nameWithoutCredentials() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://node1#Home");
        assertEquals("", link.getUser());
        assertEquals("", link.getPassword());
        assertEquals("node1", link.getTicket());
        assertEquals("Home", link.getName());
    }

    @Test
    public void parse_passwordContainingAt() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://alice:p@ss@node1");
        assertEquals("alice", link.getUser());
        assertEquals("p@ss", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_passwordContainingColon() {
        IrohSocksLink link = IrohSocksLink.parse("irohsocks://alice:pa:ss@node1");
        assertEquals("alice", link.getUser());
        assertEquals("pa:ss", link.getPassword());
        assertEquals("node1", link.getTicket());
    }

    @Test
    public void parse_trimsWhitespace() {
        IrohSocksLink link = IrohSocksLink.parse("  irohsocks://u:p@t  ");
        assertEquals("u", link.getUser());
        assertEquals("p", link.getPassword());
        assertEquals("t", link.getTicket());
    }

    @Test
    public void parse_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> IrohSocksLink.parse(null));
    }

    @Test
    public void parse_invalidSchemeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> IrohSocksLink.parse("http://u:p@t"));
    }

    @Test
    public void parse_emptyTicketThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> IrohSocksLink.parse("irohsocks://u:p@"));
    }

    @Test
    public void toString_roundTrips() {
        String raw = "irohsocks://alice:secret@node1";
        IrohSocksLink link = IrohSocksLink.parse(raw);
        assertEquals(raw, link.toString());
    }

    @Test
    public void toString_roundTripsWithName() {
        String raw = "irohsocks://alice:secret@node1#My%20VPN";
        IrohSocksLink link = IrohSocksLink.parse(raw);
        assertEquals(raw, link.toString());
    }

    @Test
    public void toString_roundTripsEncodedPassword() {
        String raw = "irohsocks://alice:p%40ss@node1";
        IrohSocksLink link = IrohSocksLink.parse(raw);
        assertEquals("p@ss", link.getPassword());
        assertEquals(raw, link.toString());
    }
}
