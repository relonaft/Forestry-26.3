#!/bin/bash
# javap against the 26.3 game and NeoForge jars: tools/jp.sh [-c] <fqcn>...
ROOT="$(cd "$(dirname "$0")/.." && pwd -W 2>/dev/null || pwd)"
CP="$ROOT/reference/jars/minecraft-patched-26.3.0.103.jar;$ROOT/reference/jars/neoforge-26.3.0.103-universal.jar;$ROOT/reference/jars/loader-12.0.12.jar"
MSYS2_ARG_CONV_EXCL='*' "$JAVA_HOME/bin/javap" -cp "$CP" -p "$@"
