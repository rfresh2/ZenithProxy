#!/bin/sh

git pull
./gradlew jarBuild --no-daemon
