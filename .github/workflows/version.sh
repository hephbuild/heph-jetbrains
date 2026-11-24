#!/usr/bin/env bash
set -euo pipefail

# Prints the plugin version.
#
#   version.sh nightly            -> <pluginVersion>-nightly.<UTC yyyymmddHHMMSS>
#   version.sh release <tag>      -> <tag with leading v stripped>
#
# pluginVersion in gradle.properties is the next version to release, so a
# nightly sorts above the last release and below the one it leads up to. The
# timestamp keeps nightlies unique and strictly increasing. Release mode fails
# when the tag differs from pluginVersion.

mode="${1:?usage: version.sh <nightly|release <tag>>}"

base=$(sed -n 's/^pluginVersion=//p' gradle.properties)
[ -n "$base" ] || { echo "pluginVersion not found in gradle.properties" >&2; exit 1; }

case "$mode" in
  nightly)
    echo "$base-nightly.$(date -u +%Y%m%d%H%M%S)"
    ;;
  release)
    tag="${2:?release mode needs a tag}"
    version="${tag#v}"
    [ "$version" = "$base" ] || { echo "tag $tag does not match pluginVersion $base" >&2; exit 1; }
    echo "$version"
    ;;
  *)
    echo "unknown mode: $mode" >&2
    exit 1
    ;;
esac
