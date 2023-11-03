package com.zenith.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zenith.Shared;
import com.zenith.database.dto.tables.pojos.Chats;
import org.redisson.api.RBoundedBlockingQueue;
import org.redisson.api.RedissonClient;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.zenith.Shared.OBJECT_MAPPER;

public class LiveChatTest {

//    @Test
    public void liveChatTest() throws JsonProcessingException {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
        final RedisClient redisClient = new RedisClient();
        RedissonClient redissonClient = redisClient.getRedissonClient();
        RBoundedBlockingQueue<String> queue = redissonClient.getBoundedBlockingQueue("ChatsQueue");
        queue.trySetCapacity(50);
//        queue.delete();
        final Chats chat = new Chats(OffsetDateTime.now(), "test chat", "rfresh2", UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"));
        String json = OBJECT_MAPPER.writeValueAsString(chat);
        System.out.println(json);
        queue.offer(json);
//        queue.offer("test");
    }
}
