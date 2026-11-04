set -eux

VERSION=$1

# Find current branch name (matches MC version)
MC_VERSION=$(git rev-parse --abbrev-ref HEAD)

TAG="$VERSION+$MC_VERSION.pre"

# Delete any existing releases with the same tag
JAVA_TAG="$VERSION+java.$MC_VERSION.pre"
LINUX_TAG="$VERSION+linux.$MC_VERSION.pre"

gh release delete "$JAVA_TAG" -y || true
gh release delete "$LINUX_TAG" -y || true

# delete any existing tags with the same name
# both on local and remote
git tag -d "$JAVA_TAG" || true
git tag -d "$LINUX_TAG" || true
git tag -d "$TAG" || true
git push --delete origin "$JAVA_TAG" || true
git push --delete origin "$LINUX_TAG" || true
git push --delete origin "$TAG" || true

# Create new pre-release tag
git tag "$TAG"

git push origin "$TAG"
