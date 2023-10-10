#!/bin/sh

BUILD_SCRIPT="$(pwd)/$(dirname $0)/scripts/build.sh"
RUN_SCRIPT="$(pwd)/$(dirname $0)/scripts/run.sh"
while true
do
  "$BUILD_SCRIPT"
  "$RUN_SCRIPT"
  echo "Restarting. Press Ctrl+C to stop"
  sleep 3
done
