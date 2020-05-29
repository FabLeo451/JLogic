#!/bin/bash

SCRIPT="$0"

# SCRIPT might be an arbitrarily deep series of symbolic links; loop until we
# have the concrete path
while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

# determine JLogic home; to do this, we strip from the path until we find
# bin, and then strip bin (there is an assumption here that there is no nested
# directory under bin also named bin)
JL_HOME=`dirname "$SCRIPT"`

# now make JL_HOME absolute
JL_HOME=`cd "$JL_HOME"; pwd`

while [ "`basename "$JL_HOME"`" != "bin" ]; do
  JL_HOME=`dirname "$JL_HOME"`
done
JL_HOME=`dirname "$JL_HOME"`

JL_BIN="$JL_HOME/bin"

cd "$JL_HOME"

