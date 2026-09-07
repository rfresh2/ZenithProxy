#!/bin/bash
set -eux

./gradlew shadowJar
cd run || exit
ZENITH_DEV=TRUE ./../build/java_toolchain -Xmx300m -Xms32m -XX:+UseG1GC \
-XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 \
-XX:G1PeriodicGCInterval=30000 -XX:TrimNativeHeapInterval=30000 \
-XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders \
--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow \
-jar ../build/libs/ZenithProxy.jar
