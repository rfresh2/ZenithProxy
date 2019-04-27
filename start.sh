#!/bin/bash

while true
do
	git pull
	./gradlew clean build
	java -Xmx512M -jar build/libs/*.jar
	echo "Press Ctrl+C to stop"
	sleep 3
done
