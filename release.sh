#!/bin/bash

set -e

# This script is used to release the Optimizely Java SDK.

# Usage:
# ./release.sh <release_version>

if [ -z "$1" ]; then
  echo "Usage: ./release.sh <release_version>"
  exit 1
fi

RELEASE_VERSION=$1

# Create a new tag
git tag -a "$RELEASE_VERSION" -m "Release $RELEASE_VERSION"

# Push the tag to the master branch
git push origin "$RELEASE_VERSION"
