./ZenithProxy -XX:MaxRAMPercentage=30 -XX:MinRAMPercentage=30 \
-XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+AlwaysPreTouch \
-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:MaxTenuringThreshold=1 \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8 -Dio.netty.allocator.maxOrder=9 -Dio.netty.eventLoopThreads=2
