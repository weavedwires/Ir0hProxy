package su.weavedwires.iroh.vpn.model;

import java.net.URI;
import java.net.URISyntaxException;

public class IrohSocksLink {

    public static final String SCHEME = "irohsocks";

    private final String name;
    private final String user;
    private final String password;
    private final String ticket;

    protected IrohSocksLink(String name, String user, String password, String ticket) {
        this.name = name == null ? "" : name;
        this.user = user == null ? "" : user;
        this.password = password == null ? "" : password;
        this.ticket = ticket == null ? "" : ticket;
    }

    public static IrohSocksLink parse(String link) {
        if (link == null) {
            throw new IllegalArgumentException("link is null");
        }
        String trimmed = link.trim();
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid link", e);
        }
        if (!SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException("invalid scheme");
        }

        String rawAuthority = uri.getRawAuthority() == null ? "" : uri.getRawAuthority();
        int at = rawAuthority.lastIndexOf('@');

        String user = "";
        String password = "";
        String ticket;
        if (at >= 0) {
            String userInfo = decode(rawAuthority.substring(0, at));
            int colon = userInfo.indexOf(':');
            user = colon < 0 ? userInfo : userInfo.substring(0, colon);
            password = colon < 0 ? "" : userInfo.substring(colon + 1);
            ticket = decode(rawAuthority.substring(at + 1));
        } else {
            ticket = decode(rawAuthority);
        }

        if (ticket.isEmpty()) {
            throw new IllegalArgumentException("empty ticket");
        }

        String name = uri.getRawFragment() == null ? "" : decode(uri.getRawFragment());

        return new IrohSocksLink(name, user, password, ticket);
    }

    private static String decode(String raw) {
        try {
            return new URI("#" + raw).getFragment();
        } catch (URISyntaxException e) {
            return raw;
        }
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getTicket() {
        return ticket;
    }

    public Connection toConnection() {
        return new Connection(name, user, password, ticket);
    }

    @Override
    public String toString() {
        String userInfo = (user.isEmpty() && password.isEmpty()) ? null : user + ":" + password;
        String fragment = name.isEmpty() ? null : name;
        try {
            return new URI(SCHEME, userInfo, ticket, -1, null, null, fragment).toString();
        } catch (URISyntaxException e) {
            StringBuilder sb = new StringBuilder(SCHEME).append("://");
            if (userInfo != null) {
                sb.append(userInfo).append('@');
            }
            sb.append(ticket);
            if (fragment != null) {
                sb.append('#').append(fragment);
            }
            return sb.toString();
        }
    }
}
