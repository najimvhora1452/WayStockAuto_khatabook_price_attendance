#!/usr/bin/env sh

# Execute gradle directly if available (e.g. AndroidIDE / CI / system gradle)
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

# Fallback to gradle-wrapper jar if present
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$CLASSPATH" ]; then
    exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "ERROR: Gradle executable not found." >&2
exit 1
