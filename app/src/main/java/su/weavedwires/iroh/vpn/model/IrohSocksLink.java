package su.weavedwires.iroh.vpn.model;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

public class IrohSocksLink {
    public static final String SCHEME = "irohsocks";
    private final URI link;

    public IrohSocksLink(Connection connection) {
        this(connection.getName(), connection.getUser(), connection.getPassword(), connection.getTicket());
    }

    public IrohSocksLink(String name, String user, String password, String ticket) {
        this.link = transform(assemble(name, user, password, ticket));
    }

    public IrohSocksLink(String link) {
        this.link = transform(link);
    }

    private static URI transform(String linkStr) {
        if (linkStr == null) {
            throw new IllegalArgumentException("link is null");
        }
        URI link;
        try {
            link = new URI(linkStr.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid link", e);
        }
        if (link.getScheme() == null || !SCHEME.equalsIgnoreCase(link.getScheme())) {
            throw new IllegalArgumentException("invalid scheme");
        }
        if (link.getHost() == null || link.getHost().isEmpty()) {
            throw new IllegalArgumentException("missing ticket");
        }
        return link;
    }

    private static String assemble(String name, String user, String password, String ticket) {
        String safeName = name == null ? "" : name;
        String safeUser = user == null ? "" : user;
        String safePassword = password == null ? "" : password;
        String safeTicket = ticket == null ? "" : ticket;
        String userInfo = (safeUser.isEmpty() && safePassword.isEmpty())
                ? null : safeUser + ":" + safePassword;
        String fragment = safeName.isEmpty() ? null : safeName;
        try {
            return new URI(SCHEME, userInfo, safeTicket, -1, null, null, fragment).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid connection", e);
        }
    }

    public String getName() {
        String fragment = link.getFragment();
        return (fragment == null || fragment.isEmpty()) ? null : fragment;
    }

    public String getUser() {
        String userInfo = link.getUserInfo();
        if (userInfo == null) {
            return null;
        }
        int colon = userInfo.indexOf(':');
        String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
        return user.isEmpty() ? null : user;
    }

    public String getPassword() {
        String userInfo = link.getUserInfo();
        if (userInfo == null) {
            return null;
        }
        int colon = userInfo.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String password = userInfo.substring(colon + 1);
        return password.isEmpty() ? null : password;
    }

    public String getTicket() {
        return link.getHost();
    }

    public Connection toConnection() {
        return new Connection(getName(), getUser(), getPassword(), getTicket());
    }

    @NonNull
    @Override
    public String toString() {
        return link.toString();
    }
}
