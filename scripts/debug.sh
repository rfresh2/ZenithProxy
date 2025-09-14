#!/bin/bash
set -eux

./gradlew shadowJar
cd run || exit
ZENITH_DEV=TRUE ./../build/java_toolchain -Xmx300m -XX:+UseG1GC \
-XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders \
--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow \
-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y \
-jar ../build/libs/ZenithProxy.jar
