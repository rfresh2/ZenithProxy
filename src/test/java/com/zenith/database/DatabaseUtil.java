package com.zenith.database;

import com.zenith.Shared;
import com.zenith.util.Wait;
import org.redisson.api.RLock;

public class DatabaseUtil {

//    @Test
    public void forceUnlock() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();

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

        Shared.loadConfig();
        Shared.loadLaunchConfig();

        RedisClient redisClient = new RedisClient();

        RLock lock = redisClient.getLock(lockName);

        while (!playerHasLock(redisClient, lockName, playerName)) {
            if (lock.forceUnlock()) {
                System.out.println("Unlocked");
            } else {
                System.out.println("Failed to unlock, stopping");
                break;
            }
            Wait.waitALittle(10 + ((int) (Math.random() * 10)));
        }
    }

    private boolean playerHasLock(final RedisClient redisClient, final String lockKey, final String playerName) {
        final String data = String.valueOf(redisClient.getRedissonClient().getBucket(lockKey + "_lock_info").get());
        System.out.println("Data: " + data);
        return data.contains(playerName);
    }
}
