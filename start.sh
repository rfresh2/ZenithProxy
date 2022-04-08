#!/bin/sh

BUILD_SCRIPT="$(pwd)/$(dirname $0)/build.sh"
while true
do
  "$BUILD_SCRIPT"
	# The ParNewGC only has marginally better YoungGen collection time at the expense of MT overhead.
	# -XX:+UseParNewGC -XX:ConcGCThreads=4 -XX:ParallelGCThreads=4
	java -server -Xmx180M -XX:NewSize=80M -D java.util.concurrent.ForkJoinPool.common.parallelism=16 -XX:+UseSerialGC -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=16 build/libs/ZenithProxy.jar
	echo "Restarting. Press Ctrl+C to stop"
	sleep 3
done
