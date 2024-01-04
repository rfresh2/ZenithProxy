package com.zenith.cache;

import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.ServerProfileCache;
import com.zenith.cache.data.bossbar.BossBarCache;
import com.zenith.cache.data.chat.ChatCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.cache.data.config.ConfigurationCache;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.cache.data.map.MapDataCache;
import com.zenith.cache.data.recipe.RecipeCache;
import com.zenith.cache.data.stats.StatisticsCache;
import com.zenith.cache.data.tab.TabListCache;
import com.zenith.cache.data.team.TeamCache;
import com.zenith.network.server.ServerConnection;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;

import static com.zenith.Shared.*;


@Getter
public class DataCache {
    protected final ChunkCache chunkCache = new ChunkCache();
    protected final TabListCache tabListCache = new TabListCache();
    protected final BossBarCache bossBarCache = new BossBarCache();
    protected final EntityCache entityCache = new EntityCache();
    protected final PlayerCache playerCache = new PlayerCache(entityCache);
    protected final ChatCache chatCache = new ChatCache();
    protected final ServerProfileCache profileCache = new ServerProfileCache();
    protected final StatisticsCache statsCache = new StatisticsCache();
    protected final MapDataCache mapDataCache = new MapDataCache();
    protected final RecipeCache recipeCache = new RecipeCache();
    protected final TeamCache teamCache = new TeamCache();
    protected final ConfigurationCache configurationCache = new ConfigurationCache();

    public Collection<CachedData> getAllData() {
        return Arrays.asList(profileCache, playerCache, chunkCache, statsCache, tabListCache, bossBarCache, entityCache, chatCache, mapDataCache, recipeCache, teamCache);
    }

    // get a limited selection of cache data
    // mainly we don't want to not send the proxy client's player cache
    public Collection<CachedData> getAllDataSpectator(final PlayerCache spectatorPlayerCache) {
        return Arrays.asList(spectatorPlayerCache, chunkCache, tabListCache, bossBarCache, entityCache, chatCache, mapDataCache, recipeCache, teamCache);
    }

    public boolean reset(boolean full) {
        CACHE_LOG.debug("Clearing " + (full ? "full" : "partial") +" cache...");

        try {
            this.getAllData().forEach(d -> d.reset(full));

            CACHE_LOG.debug("Cache cleared.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to clear cache", e);
        }
        return true;
    }

    public static void sendCacheData(final Collection<CachedData> cacheData, final ServerConnection connection) {
        cacheData.forEach(data -> {
            if (CONFIG.debug.server.cache.sendingmessages) {
                String msg = data.getSendingMessage();
                if (msg == null)    {
                    SERVER_LOG.debug("Sending data to client {}", data.getClass().getCanonicalName());
                } else {
                    SERVER_LOG.debug(msg);
                }
            }
            data.getPackets(connection::send);
        });
    }
}
