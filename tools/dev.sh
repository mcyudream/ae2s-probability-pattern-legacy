#!/usr/bin/env bash
set -euo pipefail
probability_pattern_project=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
probability_pattern_local_jdk="$probability_pattern_project/../../.dev/jdk-25.0.5+8"
if [[ -n "${PROBABILITY_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$PROBABILITY_JAVA_HOME"
elif [[ -x "$probability_pattern_local_jdk/bin/java" ]]; then
    export JAVA_HOME="$probability_pattern_local_jdk"
fi
probability_pattern_proxy_args=()
probability_pattern_proxy="${HTTPS_PROXY:-${https_proxy:-}}"
if [[ "$probability_pattern_proxy" =~ ^https?://([^/:@]+):([0-9]+)/?$ ]]; then
    probability_pattern_proxy_args+=("-Dhttps.proxyHost=${BASH_REMATCH[1]}" "-Dhttps.proxyPort=${BASH_REMATCH[2]}")
    probability_pattern_proxy_args+=("-Dhttp.proxyHost=${BASH_REMATCH[1]}" "-Dhttp.proxyPort=${BASH_REMATCH[2]}")
fi
cd -- "$probability_pattern_project"
exec ./gradlew "${probability_pattern_proxy_args[@]}" "$@"
