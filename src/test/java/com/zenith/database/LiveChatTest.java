package com.zenith.database;

import com.zenith.Globals;
import com.zenith.database.dto.records.ChatsRecord;
import org.redisson.api.RReliableTopic;
import org.redisson.api.RedissonClient;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.zenith.Globals.OBJECT_MAPPER;

public class LiveChatTest {

//    @Test
    public void liveChatTest() {
        var c = Globals.CONFIG;
        final RedisClient redisClient = new RedisClient();
        RedissonClient redissonClient = redisClient.getRedissonClient();
        RReliableTopic topic = redissonClient.getReliableTopic("ChatsTopic");
        final ChatsRecord chat = new ChatsRecord(OffsetDateTime.now(), "test chat", "rfresh2", UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"));
        String json = OBJECT_MAPPER.writeValueAsString(chat);
        System.out.println(json);
        topic.publish(json);
    }
}
