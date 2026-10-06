#!/usr/bin/env bash
# Cross-compile dumbpipe for Android and install the resulting binaries into
# app/src/main/jniLibs/<abi>/libdumbpipe.so (via sync_dumbpipe.sh).
#
# Four ABIs are built straight against the NDK, matching CI / .github/release.yml:
#   aarch64-linux-android   -> arm64-v8a
#   armv7-linux-androideabi -> armeabi-v7a
#   x86_64-linux-android    -> x86_64
#   i686-linux-android      -> x86
#
# The dumbpipe repo is located via this repo's `dumbpipe` symlink, or via
# $DUMBPIPE_DIR. The NDK is auto-detected ($ANDROID_NDK_ROOT > $NDK_HOME >
# newest under ~/Android/Sdk/ndk). API level defaults to 24; override with
# $ANDROID_API.
#
# Binaries are written to <dumbpipe>/dist/dumbpipe-android-<abi> and then copied
# into jniLibs by sync_dumbpipe.sh.
#
# Usage: ./scripts/build_dumbpipe.sh
#        DUMBPIPE_DIR=/path ANDROID_API=24 ./scripts/build_dumbpipe.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

DUMBPIPE_DIR="${DUMBPIPE_DIR:-$REPO_ROOT/dumbpipe}"
DIST="$DUMBPIPE_DIR/dist"
ANDROID_API="${ANDROID_API:-24}"

[[ -f "$DUMBPIPE_DIR/Cargo.toml" ]] || { echo "error: no dumbpipe repo at $DUMBPIPE_DIR (set DUMBPIPE_DIR)" >&2; exit 1; }

if [[ -n "${ANDROID_NDK_ROOT:-}" ]]; then
    NDK="$ANDROID_NDK_ROOT"
elif [[ -n "${NDK_HOME:-}" ]]; then
    NDK="$NDK_HOME"
else
    NDK="$(ls -d "$HOME/Android/Sdk/ndk/"* 2>/dev/null | sort -V | tail -1)"
fi
[[ -n "${NDK:-}" && -d "$NDK" ]] || { echo "error: NDK not found (set ANDROID_NDK_ROOT)" >&2; exit 1; }

HOST_TAG="linux-x86_64"
NDK_BIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG/bin"
[[ -x "$NDK_BIN/llvm-ar" ]] || { echo "error: NDK bin dir not found at $NDK_BIN" >&2; exit 1; }

# target:abi
TARGETS=(
    "aarch64-linux-android:arm64-v8a"
    "armv7-linux-androideabi:armeabi-v7a"
    "x86_64-linux-android:x86_64"
    "i686-linux-android:x86"
)

command -v rustup >/dev/null || { echo "error: rustup not found" >&2; exit 1; }
command -v perl >/dev/null || { echo "warning: perl not found; ring may fail to build" >&2; }

mkdir -p "$DIST"

for pair in "${TARGETS[@]}"; do
    target="${pair%%:*}"
    abi="${pair##*:}"

    rustup target list --installed | grep -qx "$target" || rustup target add "$target"

    # cc-rs wants the lowercase underscored target, cargo wants uppercase.
    cc_key="${target//-/_}"
    linker_key="${cc_key^^}"
    case "$target" in
        armv7-linux-androideabi) clang="armv7a-linux-androideabi${ANDROID_API}-clang" ;;
        *)                       clang="${target}${ANDROID_API}-clang" ;;
    esac
    [[ -x "$NDK_BIN/$clang" ]] || { echo "error: missing NDK clang $NDK_BIN/$clang" >&2; exit 1; }

    echo ">> building $target -> $abi"
    (
        cd "$DUMBPIPE_DIR"
        export "CARGO_TARGET_${linker_key}_LINKER=$NDK_BIN/$clang"
        export "CC_${cc_key}=$NDK_BIN/$clang"
        export "AR_${cc_key}=$NDK_BIN/llvm-ar"
        cargo build --locked --release --target "$target"
    )

    src="$DUMBPIPE_DIR/target/$target/release/dumbpipe"
    dst="$DIST/dumbpipe-android-$abi"
    [[ -f "$src" ]] || { echo "error: build output missing at $src" >&2; exit 1; }
    cp "$src" "$dst"
    echo ">> $src -> $dst ($(du -h "$dst" | cut -f1))"
done

echo "built ${#TARGETS[@]} binaries, syncing into jniLibs"
DUMBPIPE_DIR="$DUMBPIPE_DIR" "$SCRIPT_DIR/sync_dumbpipe.sh"
