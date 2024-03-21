package com.zenith.discord;

import com.zenith.Shared;
import io.netty.resolver.DefaultAddressResolverGroup;
import reactor.netty.http.client.HttpClient;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DISCORD;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionProxyTest {

//    @Test
    public void proxiedHttpClientTest() {
        Shared.loadConfig();
        var baseClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE).compress(true).followRedirect(true).secure();
        var proxy = DISCORD.getProxiedHttpClient(baseClient);
        String ip = proxy.get()
            .uri("https://ident.me/")
            .responseContent()
            .aggregate()
            .asString()
            .block();
        assertEquals(CONFIG.discord.connectionProxy.host, ip);
    }

}
