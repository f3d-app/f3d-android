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
#   --branch <name>     Git branch/tag to clone (default: master)
#   --clone-dir <path>  Directory to clone into (default: temporary, cleaned up on exit).
#                       If the directory already contains a git repo, no clone is
#                       performed and --repo/--branch are ignored.
#   --arch <abi>        Build only specific ABI (can be repeated; default: all four)
#
# Examples:
#   ./update_native_libs.sh
#   ./update_native_libs.sh --branch v3.4.1
#   ./update_native_libs.sh --arch arm64-v8a --arch x86_64
#   ./update_native_libs.sh --repo Meakk/f3d --branch my-feature
#   ./update_native_libs.sh --clone-dir ~/dev/f3d-src
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

ALL_ARCHS=(arm64-v8a armeabi-v7a x86_64 x86)

REPO="f3d-app/f3d"
BRANCH="master"
CLONE_DIR=""
ARCHS=()

usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --repo <owner/repo> GitHub repository (default: $REPO)"
    echo "  --branch <name>     Git branch/tag (default: $BRANCH)"
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
            shift 2
            ;;
        --branch)
            BRANCH="$2"
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

# ── Clone ────────────────────────────────────────────────────────────────────

if [[ -z "$CLONE_DIR" ]]; then
    CLONE_DIR="$(mktemp -d)"
    trap 'echo "Cleaning up $CLONE_DIR"; rm -rf "$CLONE_DIR"' EXIT
fi

if [[ -d "$CLONE_DIR/.git" ]]; then
    echo "Source directory $CLONE_DIR already contains a git repo, skipping clone."
else
    REPO_URL="https://github.com/${REPO}.git"
    echo "Cloning $REPO_URL ($BRANCH) into $CLONE_DIR ..."
    GIT_LFS_SKIP_SMUDGE=1 git clone --branch "$BRANCH" --depth 1 --single-branch --filter=blob:none "$REPO_URL" "$CLONE_DIR"
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

    docker run --rm -it \
        -e CMAKE_BUILD_PARALLEL_LEVEL \
        -u "$(id -u):$(id -g)" \
        -v "$CLONE_DIR":/src \
        "ghcr.io/f3d-app/f3d-android-${arch}" \
        sh -c "$CONFIG_CMD && $BUILD_CMD"

    # Copy .so into the Android project
    SO_SRC="$CLONE_DIR/build-${arch}/lib/libf3d-java.so"
    if [[ ! -f "$SO_SRC" ]]; then
        echo "Error: expected $SO_SRC not found after build."
        exit 1
    fi

    JNILIBS_DIR="$SCRIPT_DIR/app/src/main/jniLibs/$arch"
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

LIBS_DIR="$SCRIPT_DIR/app/libs"
mkdir -p "$LIBS_DIR"
cp "$JAR_SRC" "$LIBS_DIR/f3d.jar"

# ── Summary ──────────────────────────────────────────────────────────────────

echo ""
echo "Done. Updated architectures: ${ARCHS[*]}"
