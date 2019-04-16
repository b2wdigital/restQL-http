#!/usr/bin/env bash

export JAVA_AGENTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../java-agents"
export JARS_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../plugins"

if [[ ! -e $JAVA_AGENTS_DIR ]]; then
    mkdir $JAVA_AGENTS_DIR
elif [[ ! -d $JAVA_AGENTS_DIR ]]; then
    echo "$JAVA_AGENTS_DIR already exists but is not a directory" 1>&2
fi

if [[ ! -e $JARS_PATH ]]; then
    mkdir $JARS_PATH
elif [[ ! -d $JARS_PATH ]]; then
    echo "$JARS_PATH already exists but is not a directory" 1>&2
fi

export DIST_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

export JAVA_AGENTS=$(for i in $(ls $JAVA_AGENTS_DIR/*.jar); do echo "-javaagent:$JAVA_AGENTS_DIR/$(basename $i)"; done)
export MAIN_JAR="$DIST_PATH/lib/restql-http.jar"

export RUN_CLASSPATH="$MAIN_JAR:$JARS_PATH/*"
export JAVA_OPTS="-XX:MaxRAMPercentage=75 ${JAVA_OPTS:-}"
export JAVA_CMD="java ${JAVA_OPTS} $JAVA_AGENTS -cp $RUN_CLASSPATH restql.http.core"

echo "Running java with the following line:"
echo $JAVA_CMD

$JAVA_CMD
