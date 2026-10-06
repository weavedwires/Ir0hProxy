# Ir0h proxy

[English](README.md) | [Русский](README-ru.md)

An Android app that sets up a proxy tunnel on your phone via a SOCKS proxy using [iroh](https://www.iroh.computer/). In short: the app connects your phone to someone else's (or your own) exit node, and traffic reaches the internet from there.

The exit node **does not need a white (static) address**. It can be located anywhere: at home, if your home internet has fewer restrictions, or classically on a VPS. The protocol doesn't care — the connection is established via a ticket rather than `IP:port`.

## How to use

1. Get a link like this from the peer owner:

   ```
   irohsocks://user:password@ticket#name
   ```

   Links like this are also valid:

   ```
   irohsocks://ticket#name
   ```

2. Add the connection in one of the following ways:
   - **Paste from clipboard** — copy the link, the app parses it automatically.
   - **Open the link** — if `irohsocks://…` is opened as a link, the connection is added automatically.
   - **Add manually** — enter the ticket (and login/password/name if needed).

3. Connect and enjoy the free internet.

There can be several saved connections — switch between them in the list.

## Settings

- **Mode** — VPN or Proxy (VPN by default).
- **Host** — `localhost` (`127.0.0.1`, accept connections only from this phone) or `0.0.0.0` (accept from the local network).
- **Port** — local SOCKS5 port, `2081` by default.
- **DNS servers** — list of primary DNS (see below).

## How it works

Two connection schemes are possible in total:

```
tun2socks  ->  SOCKS  ->  (over iroh) relay  -> (over iroh) SOCKS server on the exit node
```

```
tun2socks  ->  SOCKS  ->  (over iroh) SOCKS server on the exit node
```

The appropriate scheme is chosen automatically, depending on whether a direct connection to the exit node is possible.

### What iroh is

iroh is a transport built on top of **QUIC**. Key points:

- The connection is **end-to-end and encrypted** (QUIC/TLS). Intermediate nodes cannot see the traffic contents.
- Addressing is done via a **ticket**, not `IP:port`. The ticket contains the node identifier (its public key), so the exit node does not need a white static address.
- iroh first tries to establish a **direct P2P connection** between the phone and the peer ([hole punching](https://www.iroh.computer/)).
- If a direct connection is not possible (strict NAT, network restrictions) — the connection may go **through a relay**. A relay is not mandatory and is not always used: it is needed to coordinate connection establishment and as a fallback path when the direct channel is unavailable.

So a relay in the path is a possible but not required element of the scheme.

## Important note about DNS

The primary DNS servers in settings are **not** a replacement for the device's system DNS. They are primary DNS, needed only to resolve the relay's address from a domain under restricted conditions. They are not used anywhere else.

The DNS actually used for traffic depends on the device and exit node settings.

The default primary DNS values are `77.88.8.8` and `77.88.8.1`, and can be changed.

## About DPI and fingerprints

An indirect connection can help bypass some restrictions:

- **Confuses DPI.** The traffic is QUIC with relay and hole punching, rather than the https-like signatures of AmnesiaWG or VLESS, which VPNs/Proxies are usually blocked by.
- **Uncharacteristic fingerprint.** There is no recognizable protocol pattern for blockers to latch onto.
- **Iroh is not a DPI target.** Blocking tools target known VPN/Proxy protocols, not iroh.

## Privacy and storage

The tunnel is encrypted by iroh's means, and is therefore **completely opaque** from the outside.

## Legal note

The developer does not encourage violating the laws of any state or the rules of companies. Before use, it is recommended to verify whether bypassing DPI is legal in your state and in your company. The developer bears no responsibility for any use of the program by the user, including for illegal targets. The code is free.

> With great power comes great responsibility.
