#!/usr/bin/env bash
# Copy ready-built dumbpipe Android binaries from the sibling dumbpipe repo
# into app/src/main/jniLibs/<abi>/, replacing the stale iroh-socks binaries.
#
# The binaries are produced by <dumbpipe>/build-release.sh into its ./dist/
# (names: dumbpipe-android-arm64-v8a, ...-armeabi-v7a, ...-x86_64, ...-x86).
# The dumbpipe repo is located via this repo's `dumbpipe` symlink, or via
# $DUMBPIPE_DIR.
#
# Every binary is copied as <abi>/dumbpipe.so: ProxyController looks the binary
# up as binary_name + ".so" (binary_name = "dumbpipe" in res/values/strings.xml),
# so the destination name MUST be dumbpipe.so or the app cannot start it.
#
# Usage: ./scripts/sync_dumbpipe.sh   (or DUMBPIPE_DIR=/path ./scripts/sync_dumbpipe.sh)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_JNI="$REPO_ROOT/app/src/main/jniLibs"

DUMBPIPE_DIR="${DUMBPIPE_DIR:-$REPO_ROOT/dumbpipe}"
DIST="$DUMBPIPE_DIR/dist"

[[ -d "$DIST" ]] || { echo "error: no dist dir at $DIST (run build-release.sh in the dumbpipe repo first)" >&2; exit 1; }

count=0
for src in "$DIST"/dumbpipe-android-*; do
    [[ -e "$src" ]] || continue

    abi="${src%*/dumbpipe-android-*}"; abi="${src##*/dumbpipe-android-}"

    case "$abi" in
        arm64-v8a | armeabi-v7a | x86_64 | x86) ;;
        *) echo "warning: skipping unknown ABI: $abi" >&2; continue ;;
    esac

    # remove binaries of the previous backends (iroh-socks / tun2socks)
    # and any leftovers copied under their original source names
    rm -f "$APP_JNI/$abi"/libiroh-socks.so "$APP_JNI/$abi"/libtun2socks.so \
          "$APP_JNI/$abi"/dumbpipe-android-*

    mkdir -p "$APP_JNI/$abi"
    cp "$src" "$APP_JNI/$abi/dumbpipe.so"
    echo ">> $src -> $APP_JNI/$abi/dumbpipe.so ($(du -h "$APP_JNI/$abi/dumbpipe.so" | cut -f1))"
    count=$((count + 1))
done

[[ $count -gt 0 ]] || { echo "error: no dumbpipe-android-* binaries found in $DIST" >&2; exit 1; }

echo "done ($count binaries)"