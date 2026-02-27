#!/bin/bash
#
# Clone F3D, build for Android using Docker, and copy native libraries.
#
# By default, clones f3d-app/f3d master from GitHub and builds for all four
# Android ABIs. Each architecture uses its own Docker image from
# ghcr.io/f3d-app/f3d-android-<abi>.
#
# Usage:
#   ./update_native_libs.sh [options]
#
# Options:
#   --repo <owner/repo> GitHub repository (default: f3d-app/f3d)
#   --ref <name>        Git ref (branch, tag, or commit SHA) to fetch. When
#                       specified, the resolved commit SHA is written to
#                       jniLibs-lock.json. When omitted, the script reads
#                       jniLibs-lock.json to fetch the pinned commit.
#   --clone-dir <path>  Directory to clone into (default: temporary, cleaned up on exit).
#                       If the directory already contains a git repo, no clone is
#                       performed and --repo/--ref are ignored.
#   --arch <abi>        Build only specific ABI (can be repeated; default: all four)
#
# Examples:
#   ./update_native_libs.sh
#   ./update_native_libs.sh --ref v3.4.1
#   ./update_native_libs.sh --arch arm64-v8a --arch x86_64
#   ./update_native_libs.sh --repo Meakk/f3d --ref my-feature
#   ./update_native_libs.sh --clone-dir ~/dev/f3d-src
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

ALL_ARCHS=(arm64-v8a armeabi-v7a x86_64 x86)

REPO="f3d-app/f3d"
REF=""
CLONE_DIR=""
ARCHS=()
REF_SPECIFIED=false
REPO_SPECIFIED=false

LOCK_FILE="$SCRIPT_DIR/jniLibs-lock.json"

usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --repo <owner/repo> GitHub repository (default: $REPO)"
    echo "  --ref <name>        Git ref: branch, tag, or commit SHA (writes commit to jniLibs-lock.json)"
    echo "  --clone-dir <path>  Clone directory (default: temporary)"
    echo "  --arch <abi>        ABI to build (repeatable; default: all)"
    echo ""
    echo "Supported ABIs: ${ALL_ARCHS[*]}"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --repo)
            REPO="$2"
            REPO_SPECIFIED=true
            shift 2
            ;;
        --ref)
            REF="$2"
            REF_SPECIFIED=true
            shift 2
            ;;
        --clone-dir)
            CLONE_DIR="$2"
            shift 2
            ;;
        --arch)
            ARCHS+=("$2")
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Error: unknown option '$1'"
            usage
            ;;
    esac
done

# Default to all architectures
if [[ ${#ARCHS[@]} -eq 0 ]]; then
    ARCHS=("${ALL_ARCHS[@]}")
fi

# Validate requested ABIs
for arch in "${ARCHS[@]}"; do
    case "$arch" in
        arm64-v8a|armeabi-v7a|x86_64|x86) ;;
        *)
            echo "Error: unsupported ABI '$arch'."
            echo "Supported: ${ALL_ARCHS[*]}"
            exit 1
            ;;
    esac
done

# ── Resolve commit / branch ───────────────────────────────────────────────────

if [[ "$REF_SPECIFIED" == false ]]; then
    # Read pinned commit from lock file
    if [[ ! -f "$LOCK_FILE" ]]; then
        echo "Error: --ref not specified and $LOCK_FILE not found."
        exit 1
    fi
    REF=$(jq -r '.commit' "$LOCK_FILE")
    if [[ "$REPO_SPECIFIED" == false ]]; then
        REPO=$(jq -r '.repo' "$LOCK_FILE")
    fi
    echo "Using pinned commit $REF (from $LOCK_FILE)"
fi

# ── Clone ────────────────────────────────────────────────────────────────────

if [[ -z "$CLONE_DIR" ]]; then
    CLONE_DIR="$(mktemp -d)"
    trap 'echo "Cleaning up $CLONE_DIR"; rm -rf "$CLONE_DIR"' EXIT
fi

if [[ -d "$CLONE_DIR/.git" ]]; then
    echo "Source directory $CLONE_DIR already contains a git repo, skipping clone."
else
    REPO_URL="https://github.com/${REPO}.git"
    echo "Fetching $REF from $REPO_URL into $CLONE_DIR ..."
    git init "$CLONE_DIR"
    git -C "$CLONE_DIR" remote add origin "$REPO_URL"
    GIT_LFS_SKIP_SMUDGE=1 git -C "$CLONE_DIR" fetch --depth 1 origin "$REF"
    git -C "$CLONE_DIR" checkout FETCH_HEAD
fi

# ── Write lock file ──────────────────────────────────────────────────────────

if [[ "$REF_SPECIFIED" == true ]]; then
    COMMIT=$(git -C "$CLONE_DIR" rev-parse HEAD)
    jq -n --arg repo "$REPO" --arg commit "$COMMIT" \
        '{repo: $repo, commit: $commit}' > "$LOCK_FILE"
    echo "Pinned commit $COMMIT to $LOCK_FILE"
fi

# ── Pull Docker images ───────────────────────────────────────────────────────

for arch in "${ARCHS[@]}"; do
    echo "Pulling ghcr.io/f3d-app/f3d-android-${arch} ..."
    docker pull "ghcr.io/f3d-app/f3d-android-${arch}"
done

# ── Build & copy for each architecture ───────────────────────────────────────

for arch in "${ARCHS[@]}"; do
    echo ""
    echo "========================================"
    echo " Building for $arch"
    echo "========================================"

    CONFIG_CMD="cmake -S /src -B /src/build-${arch} \
        -DCMAKE_BUILD_TYPE=Release \
        -DF3D_MODULE_EXR=ON \
        -DF3D_MODULE_UI=OFF \
        -DF3D_MODULE_WEBP=ON \
        -DF3D_PLUGINS_STATIC_BUILD=ON \
        -DF3D_PLUGIN_BUILD_ALEMBIC=ON \
        -DF3D_PLUGIN_BUILD_ASSIMP=ON \
        -DF3D_PLUGIN_BUILD_DRACO=ON \
        -DF3D_PLUGIN_BUILD_HDF=OFF \
        -DF3D_PLUGIN_BUILD_OCCT=ON \
        -DF3D_PLUGIN_BUILD_WEBIFC=ON \
        -DF3D_STRICT_BUILD=ON \
        -DF3D_BINDINGS_JAVA=ON"

    BUILD_CMD="cmake --build /src/build-${arch}"

    STRIP_CMD="/ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip --strip-all /src/build-${arch}/lib/libf3d-java.so"

    docker run --rm \
        -e CMAKE_BUILD_PARALLEL_LEVEL \
        -u "$(id -u):$(id -g)" \
        -v "$CLONE_DIR":/src \
        "ghcr.io/f3d-app/f3d-android-${arch}" \
        sh -c "$CONFIG_CMD && $BUILD_CMD && $STRIP_CMD"

    # Copy .so into the Android project
    SO_SRC="$CLONE_DIR/build-${arch}/lib/libf3d-java.so"
    if [[ ! -f "$SO_SRC" ]]; then
        echo "Error: expected $SO_SRC not found after build."
        exit 1
    fi

    JNILIBS_DIR="$SCRIPT_DIR/f3d/src/main/jniLibs/$arch"
    mkdir -p "$JNILIBS_DIR"
    cp "$SO_SRC" "$JNILIBS_DIR/libf3d-java.so"

    SO_SIZE=$(du -h "$JNILIBS_DIR/libf3d-java.so" | cut -f1)
    echo "Copied libf3d-java.so ($SO_SIZE) -> jniLibs/$arch/"
done

# ── Copy .jar ────────────────────────────────────────────────────────────────

# The jar is architecture-independent, copy from the first built arch
JAR_SRC="$CLONE_DIR/build-${ARCHS[0]}/java/f3d.jar"
if [[ ! -f "$JAR_SRC" ]]; then
    echo "Error: expected $JAR_SRC not found after build."
    exit 1
fi

LIBS_DIR="$SCRIPT_DIR/f3d/libs"
mkdir -p "$LIBS_DIR"
cp "$JAR_SRC" "$LIBS_DIR/f3d.jar"

# ── Summary ──────────────────────────────────────────────────────────────────

echo ""
echo "Done. Updated architectures: ${ARCHS[*]}"
