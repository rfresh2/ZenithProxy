package com.zenith.network.client;

import com.google.gson.JsonObject;
import com.zenith.event.client.MsaDeviceCodeLoginEvent;
import com.zenith.util.WebBrowserHelper;
import lombok.Getter;
import lombok.Locked;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.proxy.ProxyHandler;
import net.lenni0451.commons.httpclient.proxy.ProxyType;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaCredentials;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.util.MinecraftAuth4To5Migrator;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.zenith.Globals.*;
import static com.zenith.util.config.Config.Authentication.AccountType.OFFLINE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
public class Authenticator {
    public static final Authenticator INSTANCE = new Authenticator();

    private ScheduledFuture<?> refreshTask;
    private int refreshTryCount = 0;

    public static final File AUTH_CACHE_FILE = new File("mc_auth_cache.json");

    public void clearAuthCache() {
        try {
            Files.deleteIfExists(AUTH_CACHE_FILE.toPath());
        } catch (IOException ex) {
            AUTH_LOG.error("Unable to delete msal cache file", ex);
        }
    }

    public MinecraftProtocol login()  {
        if (CONFIG.authentication.accountType == OFFLINE) {
            AUTH_LOG.warn("Using offline account: '{}'. Offline accounts will not receive user support.", CONFIG.authentication.username);
            final UUID offlineUuid = switch (CONFIG.authentication.offlineUUIDMode) {
                case FIXED -> CONFIG.authentication.offlineUUID != null
                    ? CONFIG.authentication.offlineUUID
                    : UUID.randomUUID();
                case RANDOM -> UUID.randomUUID();
                case GENERATED -> UUID.nameUUIDFromBytes(
                    (CONFIG.authentication.offlineUUIDPrefix + CONFIG.authentication.username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            };
            return createMinecraftProtocol(
                new MinecraftProfile(
                    offlineUuid,
                    CONFIG.authentication.username),
                null,
                null);
        }
        var authSession = loadAuthCache()
            // todo: validate JavaAuthManager from cache matches configured auth type?
            .flatMap(this::checkAuthCacheMatchesConfiguredUsername)
            .orElseGet(this::loginJavaAuthManager);
        authSession.getMinecraftToken().getUpToDateUnchecked();
        authSession.getMinecraftProfile().getUpToDateUnchecked();
        authSession.getMinecraftPlayerCertificates().getUpToDateUnchecked();
        this.refreshTryCount = 0;
        saveAuthCacheAsync(authSession);
        updateConfig(authSession);
        if (this.refreshTask != null) this.refreshTask.cancel(true);
        if (CONFIG.authentication.authTokenRefresh) scheduleAuthCacheRefresh(authSession);
        return createMinecraftProtocol(authSession.getMinecraftProfile().getCached(), authSession.getMinecraftToken().getCached(), authSession.getMinecraftPlayerCertificates().getCached());
    }

    @SneakyThrows
    private JavaAuthManager loginJavaAuthManager() {
        var builder = JavaAuthManager.create(createHttpClient());
        return switch (CONFIG.authentication.accountType) {
            case MSA -> builder
                .login(CredentialsMsaAuthService::new, new MsaCredentials(CONFIG.authentication.email, CONFIG.authentication.password));
            case DEVICE_CODE, DEVICE_CODE_WITHOUT_DEVICE_TOKEN -> builder
                .login(DeviceCodeMsaAuthService::new, (Consumer<MsaDeviceCode>) this::onDeviceCodeLogin);
            case PRISM -> builder
                .msaApplicationConfig(new MsaApplicationConfig("c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb", MsaConstants.SCOPE_OFFLINE_ACCESS))
                .login(DeviceCodeMsaAuthService::new, (Consumer<MsaDeviceCode>) this::onDeviceCodeLogin);
            case OFFLINE -> throw new RuntimeException("can't login offline account");
        };
    }

    private MinecraftProtocol createMinecraftProtocol(MinecraftProfile minecraftProfile, MinecraftToken minecraftToken, MinecraftPlayerCertificates minecraftPlayerCertificates) {
        var gameProfile = new GameProfile(minecraftProfile.getId(), minecraftProfile.getName());
        gameProfile.setPlayerCertificates(minecraftPlayerCertificates);
        var accessToken = minecraftToken != null ? minecraftToken.getToken() : null;
        return new MinecraftProtocol(MinecraftCodec.CODEC, gameProfile, accessToken);
    }

    private void tryOpenBrowser(final String url) {
        try {
            WebBrowserHelper.openBrowser(url);
        } catch (final Exception e) {
            AUTH_LOG.debug("Failed to open browser", e);
        }
    }

    private void scheduleAuthCacheRefresh(JavaAuthManager session) {
        var time = session.getMinecraftToken().getCached().getExpireTimeMs() - System.currentTimeMillis();
        if (time <= 0) {
            AUTH_LOG.debug("Auth token refresh time is negative? {}", time);
            return;
        }
        // random offset to prevent multiple instances possibly refreshing at the same time
        var randomOffsetMs = ThreadLocalRandom.current().nextInt(5) * 60L * 1000L;
        // fail-safe to avoid spamming refreshes
        var minRefreshDelayMs = 30L * 1000L;
        var expireTimeDelayMs = Math.max(minRefreshDelayMs, time + randomOffsetMs);
        var maxRefreshIntervalMs = (CONFIG.authentication.maxRefreshIntervalMins * 60L * 1000L) - randomOffsetMs;
        this.refreshTask = EXECUTOR.schedule(
            this::executeAuthCacheRefresh,
            Math.max(minRefreshDelayMs, Math.min(expireTimeDelayMs, maxRefreshIntervalMs)),
            MILLISECONDS
        );
        AUTH_LOG.debug("Auth cache refresh scheduled in {} minutes", this.refreshTask.getDelay(TimeUnit.MINUTES));
    }

    private void executeAuthCacheRefresh() {
        try {
            AUTH_LOG.info("Running background auth token refresh..");
            var authCache = loadAuthCache();
            if (authCache.isEmpty()) {
                AUTH_LOG.error("No auth cache found to background refresh");
                return;
            }
            var javaAuthManager = authCache.get();
            try {
                javaAuthManager.getMinecraftToken().refresh();
                javaAuthManager.getMinecraftProfile().refresh();
                javaAuthManager.getMinecraftPlayerCertificates().refresh();
                AUTH_LOG.info("Refreshed profile: {} [{}]", javaAuthManager.getMinecraftProfile().getCached().getName(), javaAuthManager.getMinecraftProfile().getCached().getId());
                updateConfig(javaAuthManager);
                saveAuthCacheAsync(javaAuthManager);
            } catch (final Exception e) {
                AUTH_LOG.error("Failed while refreshing auth cache", e);
            }
            scheduleAuthCacheRefresh(javaAuthManager);
        } catch (Throwable e) {
            AUTH_LOG.error("Error refreshing auth token", e);
        }
    }

    public record AuthCacheRefreshResult(
        boolean success,
        String reason
    ) {}

    public AuthCacheRefreshResult refreshAuthCacheNow() {
        try {
            AUTH_LOG.info("Running auth token refresh..");
            var authCache = loadAuthCache();
            if (authCache.isEmpty()) {
                return new AuthCacheRefreshResult(false, "No auth cache found to refresh");
            }
            var javaAuthManager = authCache.get();
            try {
                javaAuthManager.getMinecraftToken().refresh();
                javaAuthManager.getMinecraftProfile().refresh();
                javaAuthManager.getMinecraftPlayerCertificates().refresh();
                AUTH_LOG.info("Refreshed profile: {} [{}]", javaAuthManager.getMinecraftProfile().getCached().getName(), javaAuthManager.getMinecraftProfile().getCached().getId());
                updateConfig(javaAuthManager);
                saveAuthCacheAsync(javaAuthManager);
                return new AuthCacheRefreshResult(true, "Success");
            } catch (final Exception e) {
                AUTH_LOG.error("Failed while refreshing auth cache", e);
                return new AuthCacheRefreshResult(false, "Failed while refreshing auth cache");
            }
        } catch (Throwable e) {
            AUTH_LOG.error("Error refreshing auth token", e);
            return new AuthCacheRefreshResult(false, "Error refreshing auth token: %s:%s".formatted(e.getClass().getSimpleName(), e.getMessage()) );
        }
    }

    public void saveAuthCache(final JavaAuthManager session) {
        saveAuthCacheJson(JavaAuthManager.toJson(session));
    }

    @Locked
    private void saveAuthCacheJson(JsonObject json) {
        try {
            final File tempFile = File.createTempFile(AUTH_CACHE_FILE.getName(), null);
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(json, out);
            }
            com.google.common.io.Files.move(tempFile, AUTH_CACHE_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save auth cache", e);
        }
        AUTH_LOG.debug("Auth cache saved!");
    }

    public void updateConfig(JavaAuthManager javaSession) {
        var javaProfile = javaSession.getMinecraftProfile().getCached();
        if (!CONFIG.authentication.username.equals(javaProfile.getName())) {
            CONFIG.authentication.username = javaProfile.getName();
            saveConfigAsync();
        }
    }

    public void saveAuthCacheAsync(final JavaAuthManager session) {
        Thread.ofVirtual().name("Auth Cache Writer").start(() -> saveAuthCache(session));
    }

    public Optional<JavaAuthManager> loadAuthCache() {
        if (!AUTH_CACHE_FILE.exists()) return Optional.empty();
        return readAuthCacheJson()
            .map(this::upgradeAuthCache4To5)
            .map(json -> JavaAuthManager.fromJson(createHttpClient(), json));
    }

    @Locked
    private Optional<JsonObject> readAuthCacheJson() {
        try (Reader reader = new FileReader(AUTH_CACHE_FILE)) {
            final JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return Optional.of(json);
        } catch (Exception e) {
            AUTH_LOG.debug("Unable to load auth cache!", e);
            return Optional.empty();
        }
    }

    private JsonObject upgradeAuthCache4To5(JsonObject json) {
        try {
            if (json.get("_saveVersion") != null) return json;
            var convertedJson = MinecraftAuth4To5Migrator.migrateJavaSave(json);
            var javaAuthManager = JavaAuthManager.fromJson(createHttpClient(), convertedJson);
            javaAuthManager.getMinecraftProfile().getUpToDateUnchecked();
            saveAuthCache(javaAuthManager);
            return JavaAuthManager.toJson(javaAuthManager);
        } catch (Exception e) {
            AUTH_LOG.warn("Failed upgrading auth cache!", e);
            return json;
        }
    }

    private Optional<JavaAuthManager> checkAuthCacheMatchesConfiguredUsername(JavaAuthManager authCacheSession) {
        var profileHolder = authCacheSession.getMinecraftProfile();
        var name = Optional.ofNullable(profileHolder.getCached())
            .map(MinecraftProfile::getName)
            .orElse(null);
        if (name == null || !name.equals(CONFIG.authentication.username)) {
            AUTH_LOG.info("Cached auth username does not match config username, clearing cache");
            clearAuthCache();
            return Optional.empty();
        }
        return Optional.of(authCacheSession);
    }

    private void onDeviceCodeLogin(MsaDeviceCode code) {
        AUTH_LOG.error("Login Here: {} with code: {}", code.getDirectVerificationUri(), code.getUserCode());
        EVENT_BUS.postAsync(new MsaDeviceCodeLoginEvent(code));
        if (CONFIG.authentication.openBrowserOnLogin) tryOpenBrowser(code.getDirectVerificationUri());
    }

    public HttpClient createHttpClient() {
        var client = MinecraftAuth.createHttpClient();
        if (CONFIG.authentication.useClientConnectionProxy) {
            var type = switch (CONFIG.client.connectionProxy.type) {
                case SOCKS5 -> ProxyType.SOCKS5;
                case SOCKS4 -> ProxyType.SOCKS4;
                case HTTP -> ProxyType.HTTP;
            };
            var user = CONFIG.client.connectionProxy.user.isEmpty() ? null : CONFIG.client.connectionProxy.user;
            var pass = CONFIG.client.connectionProxy.password.isEmpty() ? null : CONFIG.client.connectionProxy.password;
            client.setProxyHandler(new ProxyHandler(
                type,
                CONFIG.client.connectionProxy.host,
                CONFIG.client.connectionProxy.port,
                user,
                pass
            ));
        }
        return client;
    }
}
