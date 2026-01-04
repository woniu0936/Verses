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

# 1. Update Version in Files
FILES_TO_UPDATE=("$PROP_FILE" "README.md" "README.zh-CN.md")

for FILE in "${FILES_TO_UPDATE[@]}"; do
    if [ -f "$FILE" ]; then
        echo "🔍 Checking $FILE..."
        
        if [[ "$FILE" == "$PROP_FILE" ]]; then
            # Update VERSION_NAME in gradle.properties
            CURRENT_VAL=$(grep "^VERSION_NAME=" "$FILE" | cut -d'=' -f2)
            if [ "$CURRENT_VAL" != "$VERSION" ]; then
                echo "📝 Updating VERSION_NAME to $VERSION in $FILE..."
                sed "s/^VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$FILE" > "$FILE.tmp"
                mv "$FILE.tmp" "$FILE"
            fi
        else
            # Update implementation line in READMEs
            # Matches: implementation("io.github.woniu0936:verses:VERSION")
            echo "📝 Updating dependency version to $VERSION in $FILE..."
            sed "s/\(io.github.woniu0936:verses:\)[^\"]*/\1$VERSION/" "$FILE" > "$FILE.tmp"
            mv "$FILE.tmp" "$FILE"
        fi
        
        # Verify update (basic check)
        if ! grep -q "$VERSION" "$FILE"; then
            echo "❌ Error: Failed to verify version $VERSION in $FILE"
            exit 1
        fi
        echo "✅ $FILE updated and verified."
    else
        echo "⚠️  Warning: $FILE not found, skipping."
    fi
done

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
git add "${FILES_TO_UPDATE[@]}"
if ! git diff --cached --quiet; then
    echo "📝 Changes detected in project files. Committing..."
    git commit -m "chore(release): prepare release $TAG_NAME"
    echo "✅ Commit created."
else
    echo "ℹ️  No changes detected (all files already match target version)."
fi

# 4. Tag and Push
echo "🏷️  Tagging $TAG_NAME..."
git tag -a "$TAG_NAME" -m "Release $TAG_NAME"

echo "📤 Pushing to GitHub..."
git push origin main
git push origin "$TAG_NAME"

# 5. Create GitHub Release via CLI (if available)
if command -v gh &> /dev/null; then
    echo "🚀 Creating GitHub Release via CLI..."
    PRERELEASE_FLAG=""
    if [[ "$VERSION" == *"-"* ]]; then
        PRERELEASE_FLAG="--prerelease"
    fi
    
    gh release create "$TAG_NAME" \
        --title "Verses $TAG_NAME" \
        --generate-notes \
        $PRERELEASE_FLAG \
        --latest
    echo "✅ GitHub Release created successfully."
else
    echo "ℹ️  GitHub CLI (gh) not found. Relying on GitHub Actions to create the release page."
fi

echo "✅ Success! Tag $TAG_NAME and changes have been pushed."
