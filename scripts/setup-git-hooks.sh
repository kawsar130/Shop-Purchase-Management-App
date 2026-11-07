#!/bin/bash
# -----------------------------------------------------------------------------
# Setup Tracked Git Hooks for ShopKeeper
# -----------------------------------------------------------------------------
# Configures Git to use version-controlled hooks located in .githooks/
# -----------------------------------------------------------------------------

set -e

HOOKS_DIR=".githooks"

if [ ! -d "$HOOKS_DIR" ]; then
    echo "❌ Error: $HOOKS_DIR directory not found."
    exit 1
fi

echo "🔧 Configuring Git to use tracked hooks in $HOOKS_DIR/..."
chmod +x "$HOOKS_DIR"/*
git config core.hooksPath "$HOOKS_DIR"

echo "✅ Git hooks successfully configured! Current hooksPath: $(git config core.hooksPath)"
echo "💡 The pre-push hook will now run unit tests and lint before every 'git push'."
