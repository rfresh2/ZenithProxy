call build\java_toolchain.bat --enable-preview -server -XX:MaxRAMPercentage=30 -XX:MinRAMPercentage=30^
 -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch^
 -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5^
 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90^
 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1^
 -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dio.netty.allocator.maxOrder=9^
 -jar build\libs\ZenithProxy.jar
