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

# Safety Check: Ensure we are on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "⚠️  Warning: You are currently on branch '$CURRENT_BRANCH', but the script will push to 'main'."
    read -p "❓ Proceed anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Aborted."
        exit 1
    fi
fi

# 1. Update gradle.properties
if [ -f "$PROP_FILE" ]; then
    CURRENT_VERSION=$(grep "^VERSION_NAME=" "$PROP_FILE" | cut -d'=' -f2)
    echo "🔍 Current version in $PROP_FILE: $CURRENT_VERSION"
    
    if [ "$CURRENT_VERSION" != "$VERSION" ]; then
        echo "📝 Updating VERSION_NAME to $VERSION..."
        # Use a temporary file for more robust cross-platform replacement
        sed "s/^VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$PROP_FILE" > "$PROP_FILE.tmp"
        mv "$PROP_FILE.tmp" "$PROP_FILE"
        
        # Verify the update
        NEW_VERSION=$(grep "^VERSION_NAME=" "$PROP_FILE" | cut -d'=' -f2)
        if [ "$NEW_VERSION" != "$VERSION" ]; then
            echo "❌ Error: Failed to update version in $PROP_FILE (Still $NEW_VERSION)."
            exit 1
        fi
        echo "✅ Version successfully updated to $NEW_VERSION."
    else
        echo "ℹ️  Version in $PROP_FILE is already $VERSION."
    fi
else
    echo "❌ Error: $PROP_FILE not found."
    exit 1
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
echo "📦 Checking for changes to commit..."
git add "$PROP_FILE"
if ! git diff --cached --quiet; then
    echo "📝 Changes detected in $PROP_FILE. Committing..."
    git commit -m "chore(release): prepare release $TAG_NAME"
    echo "✅ Commit created."
else
    echo "ℹ️  No changes detected in $PROP_FILE (already matches target version in HEAD)."
fi

# 4. Tag and Push
echo "🏷️  Tagging $TAG_NAME..."
git tag -a "$TAG_NAME" -m "Release $TAG_NAME"

echo "📤 Pushing to GitHub..."
git push origin main
git push origin "$TAG_NAME"

echo "✅ Success! Tag $TAG_NAME and changes have been pushed."
