#!/bin/sh

BUILD_SCRIPT="$(pwd)/$(dirname $0)/build.sh"
while true
do
  "$BUILD_SCRIPT"
	./gradlew run
	echo "Restarting. Press Ctrl+C to stop"
	sleep 3
done
