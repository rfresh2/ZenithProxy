#!/bin/sh

while true
do
	# The ParNewGC only has marginally better YoungGen collection time at the expense of MT overhead.
	# -XX:+UseParNewGC -XX:ConcGCThreads=4 -XX:ParallelGCThreads=4
	java -server -Xmx180M -XX:NewSize=80M -XX:+UseSerialGC -jar build/libs/*.jar
	echo "Restarting. Press Ctrl+C to stop"
	sleep 3
done
