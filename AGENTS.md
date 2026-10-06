# AGENTS.md

Android app (pure Java, no Kotlin) that exposes a local SOCKS5 proxy whose upstream is a remote `dumbpipe` (iroh) peer; optional VPN mode pipes traffic through tun2socks into that proxy. Single Gradle module `:app`; package/namespace/applicationId `su.weavedwires.iroh.vpn`; default branch `master`.

## Commands

```sh
./gradlew :app:testDebugUnitTest                 # JVM unit tests, no device/emulator
./gradlew :app:testDebugUnitTest --tests "su.weavedwires.iroh.vpn.model.IrohSocksLinkTest"
./gradlew assembleDebug
./gradlew assembleRelease                        # requires keystore.properties (local only)
```

- Java toolchain 17 for compilation; Gradle daemon runs on JVM 21 (pinned in `gradle/gradle-daemon-jvm.properties`). Gradle 9.8, AGP 9.4.1, minSdk 26.
- `compileSdk` uses the new AGP 9.4 DSL (`compileSdk { version = release(37) }`) — do not rewrite it as `compileSdk = 37`.
- Release signing reads the gitignored `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) plus `release.jks`; both are local-only. There is no CI in this repo.
- Unit tests are the only configured verification. No lint/format config.

## Native binaries are gitignored (build them before running)

`app/src/main/jniLibs/<abi>/*.so` are **not tracked** (`.gitignore`). `assemble*` succeeds without them, but the app fails at runtime with `native binary not found`.

- `libdumbpipe.so` — the Rust `dumbpipe` CLI, run as a subprocess.
- `libhev-socks5-tunnel.so` — tun2socks JNI library.
- Rebuild (both need an Android NDK; auto-detected from `$ANDROID_NDK_ROOT`/`$NDK_HOME`/`~/Android/Sdk/ndk`):
  - `./scripts/build_dumbpipe.sh` — cross-compiles Rust for 4 ABIs, then calls `sync_dumbpipe.sh`. Sibling repo from `$DUMBPIPE_DIR` or the gitignored `./dumbpipe` symlink (→ `../dumbpipe-over-chatmail`); needs `rustup` targets, API defaults to 24.
  - `./scripts/sync_sockstun.sh` — ndk-build. Sibling source from `$SOCKSTUN_DIR` or `../sockstun`.
  - `./scripts/sync_dumbpipe.sh` — only copies already-built binaries from `<dumbpipe>/dist`.
- Fresh clones have no symlinks: point `DUMBPIPE_DIR` / `SOCKSTUN_DIR` at the sibling checkouts.
- Keep the `lib` prefix: only `lib*.so` gets extracted from non-debuggable APKs, and `packaging.jniLibs.useLegacyPackaging = true` keeps them extracted to `nativeLibraryDir` so the executable can run.
- JNI class name is baked in at build time (`PKGNAME=su/weavedwires/iroh/vpn/nativ`, `CLSNAME=TunNativeTool`, see `sync_sockstun.sh`) and must match `TunNativeTool`. Do not remove its `TProxy*` native methods.

## dumbpipe runtime contract

`ProxyNativeTool` invokes `libdumbpipe.so connect-tcp --addr <host>:<port> [--dns-server a,b] <ticket>` (see `proxy/ProxyNativeTool.java`). The binary is resolved as `Constant.IROH_BINARY_NAME + ".so"` = `libdumbpipe.so` from `applicationInfo.nativeLibraryDir` (`IrohProxyApp.java:32`). Keep flags in sync with the `dumbpipe` CLI (sibling repo).

## Data model / deep links

- URI scheme `irohsocks://user:password@ticket#name`. `model/IrohSocksLink.java` is the parser/spec and is unit-tested; percent-encode components. Incoming `VIEW` intents and clipboard paste both go through it.
- Connections persist encrypted via `ConnectionStore` (`EncryptedSharedPreferences`, with a plain-prefs fallback); app settings use plain `SharedPreferences` (`Settings`). Default mode is `VPN`; `PROXY` skips VpnService/tun2socks.
- Entry points: `activity/MainActivity` (launcher + link handler), `proxy/ProxyService` (VpnService foreground service), `IrohProxyApp` (owns `NativeTool` singletons).

## Conventions

- UI text is Russian; all user-facing strings live in `res/values/strings.xml` — no hardcoded text, including toasts/snackbars.
