java -server -Xmx180M -XX:NewSize=80M -XX:+UseSerialGC -Djava.util.concurrent.ForkJoinPool.common.parallelism=16 -jar build/libs/Proxy.jar
