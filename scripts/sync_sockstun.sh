#!/usr/bin/env bash
# Build libhev-socks5-tunnel.so (tun2socks) for Android and copy it into
# app/src/main/jniLibs/<abi>/libhev-socks5-tunnel.so.
#
# The native source is hev-socks5-tunnel, vendored inside the sibling `sockstun`
# repo (app/src/main/jni/hev-socks5-tunnel). Locate it via $SOCKSTUN_DIR, or
# default to ../sockstun relative to this repo.
#
# The JNI layer registers natives on the class baked in at compile time via
# -DPKGNAME/-DCLSNAME. These MUST match the Java bridge class:
#   PKGNAME=su/weavedwires/iroh/vpn/nativ  CLSNAME=Tun2Socks
#
# NDK is auto-detected ($ANDROID_NDK_ROOT > $NDK_HOME > newest under
# ~/Android/Sdk/ndk).
#
# Usage: ./scripts/sync_sockstun.sh   (or SOCKSTUN_DIR=/path ./scripts/sync_sockstun.sh)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_JNI="$REPO_ROOT/app/src/main/jniLibs"

SOCKSTUN_DIR="${SOCKSTUN_DIR:-$REPO_ROOT/../sockstun}"
JNI_DIR="$SOCKSTUN_DIR/app/src/main/jni"

[[ -f "$JNI_DIR/Android.mk" ]] || { echo "error: no Android.mk at $JNI_DIR (set SOCKSTUN_DIR)" >&2; exit 1; }

if [[ -n "${ANDROID_NDK_ROOT:-}" ]]; then
    NDK="$ANDROID_NDK_ROOT"
elif [[ -n "${NDK_HOME:-}" ]]; then
    NDK="$NDK_HOME"
else
    NDK="$(ls -d "$HOME/Android/Sdk/ndk/"* 2>/dev/null | sort -V | tail -1)"
fi
[[ -n "$NDK" && -x "$NDK/ndk-build" ]] || { echo "error: NDK not found" >&2; exit 1; }

ABIS=(arm64-v8a armeabi-v7a x86 x86_64)
PKGNAME="su/weavedwires/iroh/vpn/nativ"
CLSNAME="TunNativeTool"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

(
    cd "$JNI_DIR"
    "$NDK/ndk-build" \
        NDK_PROJECT_PATH=. \
        APP_BUILD_SCRIPT=Android.mk \
        NDK_APPLICATION_MK=Application.mk \
        APP_ABI="${ABIS[*]}" \
        APP_CFLAGS="-O3 -DPKGNAME=$PKGNAME -DCLSNAME=$CLSNAME" \
        NDK_OUT="$TMP/obj" \
        NDK_LIBS_OUT="$TMP/libs" >&2
)

count=0
for abi in "${ABIS[@]}"; do
    src="$TMP/libs/$abi/libhev-socks5-tunnel.so"
    [[ -f "$src" ]] || { echo "error: missing $src" >&2; exit 1; }
    mkdir -p "$APP_JNI/$abi"
    cp "$src" "$APP_JNI/$abi/libhev-socks5-tunnel.so"
    echo ">> $src -> $APP_JNI/$abi/libhev-socks5-tunnel.so ($(du -h "$APP_JNI/$abi/libhev-socks5-tunnel.so" | cut -f1))"
    count=$((count + 1))
done

echo "done ($count binaries)"
