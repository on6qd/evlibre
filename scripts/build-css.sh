#!/usr/bin/env bash
# Build the Web UI CSS bundle using the Tailwind v4 standalone CLI plus the DaisyUI v5
# plugin pulled directly from the npm registry. No Node toolchain required at any point.
#
# All artifacts live under <repo>/.tools/ (gitignored). Re-run idempotently; downloads
# happen only the first time or when the pinned versions below are bumped.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="${ROOT}/.tools"
mkdir -p "$TOOLS_DIR/node_modules"

# Pin both upstreams. Bumping either is a deliberate change.
TAILWIND_VERSION="${TAILWIND_VERSION:-v4.1.14}"
DAISYUI_VERSION="${DAISYUI_VERSION:-5.1.27}"

case "$(uname -s)" in
    Darwin) OS=macos ;;
    Linux)  OS=linux ;;
    *) echo "unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
case "$(uname -m)" in
    x86_64|amd64)  ARCH=x64 ;;
    arm64|aarch64) ARCH=arm64 ;;
    *) echo "unsupported arch: $(uname -m)" >&2; exit 1 ;;
esac

# 1) Tailwind v4 standalone CLI (first-party, tailwindlabs/tailwindcss releases).
TAILWIND_BIN="${TOOLS_DIR}/tailwindcss-${TAILWIND_VERSION}-${OS}-${ARCH}"
if [[ ! -x "$TAILWIND_BIN" ]]; then
    URL="https://github.com/tailwindlabs/tailwindcss/releases/download/${TAILWIND_VERSION}/tailwindcss-${OS}-${ARCH}"
    echo "Downloading tailwindcss ${TAILWIND_VERSION} -> ${TAILWIND_BIN}"
    curl --fail --silent --show-error --location "$URL" -o "$TAILWIND_BIN"
    chmod +x "$TAILWIND_BIN"
fi

# 2) DaisyUI plugin (first-party, npm registry tarball).
DAISYUI_DIR="${TOOLS_DIR}/node_modules/daisyui"
DAISYUI_STAMP="${DAISYUI_DIR}/.version"
if [[ ! -f "$DAISYUI_STAMP" ]] || [[ "$(cat "$DAISYUI_STAMP" 2>/dev/null)" != "$DAISYUI_VERSION" ]]; then
    URL="https://registry.npmjs.org/daisyui/-/daisyui-${DAISYUI_VERSION}.tgz"
    TGZ="${TOOLS_DIR}/daisyui-${DAISYUI_VERSION}.tgz"
    echo "Downloading daisyui ${DAISYUI_VERSION} -> ${DAISYUI_DIR}"
    curl --fail --silent --show-error --location "$URL" -o "$TGZ"
    rm -rf "$DAISYUI_DIR"
    mkdir -p "$DAISYUI_DIR"
    tar -xzf "$TGZ" -C "$DAISYUI_DIR" --strip-components=1
    rm "$TGZ"
    echo "$DAISYUI_VERSION" > "$DAISYUI_STAMP"
fi

INPUT="${ROOT}/server/adapter-ui-web/src/main/resources/css/app.css"
OUTPUT="${ROOT}/server/adapter-ui-web/src/main/resources/static/css/app.css"

echo "Building ${OUTPUT}"
"$TAILWIND_BIN" --input "$INPUT" --output "$OUTPUT" --minify
echo "Done."
