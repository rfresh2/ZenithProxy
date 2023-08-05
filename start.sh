#!/bin/sh

BUILD_SCRIPT="$(pwd)/$(dirname $0)/build.sh"
RUN_SCRIPT="$(pwd)/$(dirname $0)/run.sh"
while true
do
  "$BUILD_SCRIPT"
  "$RUN_SCRIPT"
  echo "Restarting. Press Ctrl+C to stop"
  sleep 3
done
