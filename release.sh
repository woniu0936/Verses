#!/bin/bash

# Verses Release Script
# Updates version, commits changes (if any), and handles tagging/pushing.
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

# 1. Update gradle.properties
if [ -f "$PROP_FILE" ]; then
    CURRENT_VERSION=$(grep "VERSION_NAME=" "$PROP_FILE" | cut -d'=' -f2)
    
    if [ "$CURRENT_VERSION" != "$VERSION" ]; then
        echo "📝 Updating VERSION_NAME from $CURRENT_VERSION to $VERSION..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$PROP_FILE"
        else
            sed -i "s/VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$PROP_FILE"
        fi
    else
        echo "ℹ️  Version in $PROP_FILE is already $VERSION."
    fi
fi

# 2. Handle Tag Overwrite
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

# 3. Git Commit (only if there are changes)
git add "$PROP_FILE"
if ! git diff --cached --quiet; then
    echo "📦 Committing version update..."
    git commit -m "chore(release): prepare release $TAG_NAME"
else
    echo "ℹ️  No changes to commit (gradle.properties already up to date)."
fi

# 4. Tag and Push
echo "🏷️  Tagging $TAG_NAME..."
git tag -a "$TAG_NAME" -m "Release $TAG_NAME"

echo "📤 Pushing to GitHub..."
git push origin main
git push origin "$TAG_NAME"

echo "✅ Success! Tag $TAG_NAME and changes have been pushed."
