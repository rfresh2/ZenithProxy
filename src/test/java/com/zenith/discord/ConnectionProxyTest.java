package com.zenith.discord;

import com.zenith.Shared;
import reactor.netty.http.client.HttpClient;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DISCORD_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionProxyTest {

//    @Test
    public void proxiedHttpClientTest() {
        Shared.loadConfig();
        HttpClient proxy = DISCORD_BOT.getProxiedHttpClient();
        String ip = proxy.get()
            .uri("https://ident.me/")
            .responseContent()
            .aggregate()
            .asString()
            .block();
        assertEquals(CONFIG.discord.connectionProxy.host, ip);
    }

}
