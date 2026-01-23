package com.zenith.database;

import com.zenith.util.Wait;
import org.redisson.api.RLock;

import java.util.concurrent.ThreadLocalRandom;

public class DatabaseUtil {

//    @Test
    public void forceUnlock() {
        RedisClient redisClient = new RedisClient();

        RLock lock = redisClient.getLock(
            // replace with whatever database key to unlock
            "Chats"
        );
        if (lock.forceUnlock()) {
            System.out.println("Unlocked");
        } else {
            System.out.println("Failed to unlock");
        }
    }

//    @Test
    public void forceUnlockToPlayer() {
        // configure these
        final String playerName = "rfresh2";
        final String lockName = "Deaths";

        RedisClient redisClient = new RedisClient();

        RLock lock = redisClient.getLock(lockName);

        while (!playerHasLock(redisClient, lockName, playerName)) {
            if (lock.forceUnlock()) {
                System.out.println("Unlocked");
            } else {
                System.out.println("Failed to unlock, stopping");
                break;
            }
            Wait.wait(ThreadLocalRandom.current().nextInt(10, 20));
        }
    }

    private boolean playerHasLock(final RedisClient redisClient, final String lockKey, final String playerName) {
        final String data = String.valueOf(redisClient.getRedissonClient().getBucket(lockKey + "_lock_info").get());
        System.out.println("Data: " + data);
        return data.contains(playerName);
    }
}
