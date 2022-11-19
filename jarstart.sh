#!/bin/sh

BUILD_SCRIPT="$(pwd)/$(dirname $0)/build.sh"
while true
do
  "$BUILD_SCRIPT"
  java -server -Xmx280M -XX:NewSize=80M -XX:+UseSerialGC -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=16 build/libs/mc-proxy.jar
  echo "Restarting. Press Ctrl+C to stop"
  sleep 3
done
