package com.zenith.database;

import com.zenith.Shared;
import org.redisson.api.RLock;

public class DatabaseUtil {

//    @Test
    public void forceUnlock() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();

        RedisClient redisClient = new RedisClient();

        RLock lock = redisClient.getLock(
            // replace with whatever database key to unlock
            "Connections"
        );
        if (lock.forceUnlock()) {
            System.out.println("Unlocked");
        } else {
            System.out.println("Failed to unlock");
        }
    }
}
