#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
BUILD_GRADLE="${PROJECT_ROOT}/app/build.gradle.kts"
RELEASE_BUNDLE="${PROJECT_ROOT}/app/build/outputs/bundle/release/app-release.aab"
MAPPING_FILE="${PROJECT_ROOT}/app/build/outputs/mapping/release/mapping.txt"
OUTPUT_BUNDLE="${PROJECT_ROOT}/app/app-release.aab"
OUTPUT_MAPPING="${PROJECT_ROOT}/app/mapping.txt"

print_usage() {
    echo "Usage: $(basename "$0") [--increment-major | --increment-minor | --increment-patch]"
    echo ""
    echo "Builds the release app bundle and copies app-release.aab and mapping.txt to app/."
    echo "Version bump flags are mutually exclusive; versionCode always increases by 1 when a flag is used."
}

read_version_code() {
    grep -E '^[[:space:]]*versionCode = [0-9]+' "${BUILD_GRADLE}" \
        | head -n 1 \
        | sed -E 's/.*versionCode = ([0-9]+).*/\1/'
}

read_version_name() {
    grep -E '^[[:space:]]*versionName = "[^"]+"' "${BUILD_GRADLE}" \
        | head -n 1 \
        | sed -E 's/.*versionName = "([^"]+)".*/\1/'
}

write_version_code() {
    local version_code="$1"
    sed -i -E "s/^([[:space:]]*versionCode = )[0-9]+/\1${version_code}/" "${BUILD_GRADLE}"
}

write_version_name() {
    local version_name="$1"
    sed -i -E "s/^([[:space:]]*versionName = )\"[^\"]+\"/\1\"${version_name}\"/" "${BUILD_GRADLE}"
}

increment_version_name() {
    local increment_kind="$1"
    local version_name="$2"
    local major minor patch

    IFS='.' read -r major minor patch <<< "${version_name}"
    major="${major:-0}"
    minor="${minor:-0}"
    patch="${patch:-0}"

    case "${increment_kind}" in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
        *)
            echo "Unknown increment kind: ${increment_kind}" >&2
            exit 1
            ;;
    esac

    echo "${major}.${minor}.${patch}"
}

INCREMENT_KIND=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --increment-major)
            if [[ -n "${INCREMENT_KIND}" ]]; then
                echo "Only one version increment flag may be used at a time." >&2
                exit 1
            fi
            INCREMENT_KIND="major"
            ;;
        --increment-minor)
            if [[ -n "${INCREMENT_KIND}" ]]; then
                echo "Only one version increment flag may be used at a time." >&2
                exit 1
            fi
            INCREMENT_KIND="minor"
            ;;
        --increment-patch)
            if [[ -n "${INCREMENT_KIND}" ]]; then
                echo "Only one version increment flag may be used at a time." >&2
                exit 1
            fi
            INCREMENT_KIND="patch"
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            print_usage
            exit 1
            ;;
    esac
    shift
done

if [[ ! -f "${BUILD_GRADLE}" ]]; then
    echo "Could not find ${BUILD_GRADLE}" >&2
    exit 1
fi

cd "${PROJECT_ROOT}"

if [[ -n "${INCREMENT_KIND}" ]]; then
    CURRENT_VERSION_CODE="$(read_version_code)"
    CURRENT_VERSION_NAME="$(read_version_name)"
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    NEW_VERSION_NAME="$(increment_version_name "${INCREMENT_KIND}" "${CURRENT_VERSION_NAME}")"

    write_version_code "${NEW_VERSION_CODE}"
    write_version_name "${NEW_VERSION_NAME}"

    echo "Version updated: ${CURRENT_VERSION_NAME} (${CURRENT_VERSION_CODE}) -> ${NEW_VERSION_NAME} (${NEW_VERSION_CODE})"
fi

echo "Running release build..."
./gradlew bundleRelease

if [[ ! -f "${RELEASE_BUNDLE}" ]]; then
    echo "Release bundle not found at ${RELEASE_BUNDLE}" >&2
    exit 1
fi

if [[ ! -f "${MAPPING_FILE}" ]]; then
    echo "Mapping file not found at ${MAPPING_FILE}" >&2
    exit 1
fi

cp "${RELEASE_BUNDLE}" "${OUTPUT_BUNDLE}"
cp "${MAPPING_FILE}" "${OUTPUT_MAPPING}"

echo "Copied ${OUTPUT_BUNDLE}"
echo "Copied ${OUTPUT_MAPPING}"
