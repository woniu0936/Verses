#!/bin/bash

# Verses Release Script
# Usage: ./release.sh <version>
# Example: ./release.sh 1.0.0

set -e

if [ -z "$1" ]; then
    echo "❌ Error: No version specified."
    echo "Usage: ./release.sh <version>"
    exit 1
fi

VERSION=$1
PROP_FILE="gradle.properties"

echo "🚀 Preparing to release Verses v$VERSION..."

# 1. Update gradle.properties
echo "📝 Updating VERSION_NAME to $VERSION in $PROP_FILE..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$PROP_FILE"
else
    sed -i "s/VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$PROP_FILE"
fi

# 2. Run local build verification
echo "🏗️ Running local build verification..."
./gradlew :verses:assembleRelease > /dev/null

# 3. Git Commit & Tag
echo "📦 Committing and tagging..."
git add "$PROP_FILE"
git commit -m "chore(release): prepare release v$VERSION"
git tag -a "v$VERSION" -m "Release v$VERSION"

# 4. Push to GitHub
echo "📤 Pushing to GitHub..."
git push origin main
git push origin "v$VERSION"

echo "✅ Success! Tag v$VERSION has been pushed. GitHub Actions will handle the rest."
