#!/bin/bash

# Verses Release Script
# Only handles tagging and pushing. No automatic commits or builds.
# Usage: ./release.sh <version>
# Example: ./release.sh 1.0.0

set -e

if [ -z "$1" ]; then
    echo "❌ Error: No version specified."
    echo "Usage: ./release.sh <version>"
    exit 1
fi

VERSION=$1
TAG_NAME="v$VERSION"
PROP_FILE="gradle.properties"

# Safety Check: Verify if version matches gradle.properties
if [ -f "$PROP_FILE" ]; then
    CURRENT_VERSION=$(grep "VERSION_NAME=" "$PROP_FILE" | cut -d'=' -f2)
    if [ "$CURRENT_VERSION" != "$VERSION" ]; then
        echo "⚠️  Warning: Version '$VERSION' does not match VERSION_NAME ($CURRENT_VERSION) in $PROP_FILE."
        read -p "❓ Proceed anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "❌ Aborted."
            exit 1
        fi
    fi
fi

echo "🚀 Preparing to release Verses $TAG_NAME..."

# Check if tag already exists
LOCAL_TAG_EXISTS=$(git tag -l "$TAG_NAME")
REMOTE_TAG_EXISTS=$(git ls-remote --tags origin "refs/tags/$TAG_NAME" 2>/dev/null || echo "")

LOCAL_TAG_DISPLAY="No"
if [ -n "$LOCAL_TAG_EXISTS" ]; then LOCAL_TAG_DISPLAY="Yes"; fi

REMOTE_TAG_DISPLAY="No"
if [ -n "$REMOTE_TAG_EXISTS" ]; then REMOTE_TAG_DISPLAY="Yes"; fi

if [ -n "$LOCAL_TAG_EXISTS" ] || [ -n "$REMOTE_TAG_EXISTS" ]; then
    echo "⚠️  Tag $TAG_NAME already exists (Local: $LOCAL_TAG_DISPLAY, Remote: $REMOTE_TAG_DISPLAY)."
    read -p "❓ Overwrite existing tag? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Aborted."
        exit 1
    fi
    
    if [ -n "$LOCAL_TAG_EXISTS" ]; then
        echo "🗑️ Deleting local tag $TAG_NAME..."
        git tag -d "$TAG_NAME"
    fi
    
    if [ -n "$REMOTE_TAG_EXISTS" ]; then
        echo "🌐 Deleting remote tag $TAG_NAME..."
        git push origin :refs/tags/"$TAG_NAME"
    fi
fi

# Tag and Push
echo "📦 Tagging..."
git tag -a "$TAG_NAME" -m "Release $TAG_NAME"

echo "📤 Pushing to GitHub..."
git push origin main
git push origin "$TAG_NAME"

echo "✅ Success! Tag $TAG_NAME and code have been pushed."