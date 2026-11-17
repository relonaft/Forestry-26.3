#!/bin/bash
# Boot a dev server until "Done", a crash or a timeout, then stop it.
# Usage: tools/boot_server.sh <gradle run task> <port> <log file> [timeout seconds]
# A dev server run never stops on its own; the forked JVM is found by its port and killed.
TASK="$1"; PORT="$2"; LOG="$3"; LIMIT="${4:-900}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/port/Forestry" || exit 1
./gradlew "$TASK" --console=plain > "$LOG" 2>&1 &
GRADLE=$!
RESULT=timeout
for ((i = 0; i < LIMIT; i++)); do
	if grep -q 'Done (' "$LOG"; then RESULT=done; break; fi
	if grep -q 'BUILD FAILED\|Crash report\|Encountered an unexpected exception' "$LOG"; then RESULT=crash; break; fi
	if ! kill -0 "$GRADLE" 2>/dev/null; then RESULT=exited; break; fi
	sleep 1
done
PID=$(netstat -ano | grep ":$PORT .*LISTENING" | awk '{print $5}' | head -1)
if [ -n "$PID" ]; then taskkill //F //PID "$PID" > /dev/null 2>&1; fi
sleep 3
kill "$GRADLE" 2>/dev/null
echo "result: $RESULT after ${i}s"
