package com.zenith.network.client;

import com.google.gson.JsonObject;
import com.zenith.event.client.MsaDeviceCodeLoginEvent;
import com.zenith.util.WebBrowserHelper;
import com.zenith.util.math.MathHelper;
import lombok.Getter;
import lombok.Locked;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.proxy.ProxyHandler;
import net.lenni0451.commons.httpclient.proxy.ProxyType;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.logging.Slf4jConsoleLogger;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

import java.io.*;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;
import static com.zenith.util.config.Config.Authentication.AccountType.OFFLINE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
public class Authenticator {
    public static final Authenticator INSTANCE = new Authenticator();

    private ScheduledFuture<?> refreshTask;
    private int refreshTryCount = 0;
    @Getter(lazy = true) private final StepFullJavaSession deviceCodeAuthStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID)
        .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .deviceCode()
        .withDeviceToken("Win32")
        .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(true);
    @Getter(lazy = true) private final StepFullJavaSession deviceCodeAuthWithoutDeviceTokenStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID)
        .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .deviceCode()
        .withoutDeviceToken()
        .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(true);
    @Getter(lazy = true) private final StepFullJavaSession msaAuthStep = MinecraftAuth.builder()
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .credentials()
        .withDeviceToken("Win32")
        .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(true);
    @Getter(lazy = true) private final StepFullJavaSession prismDeviceCodeAuthStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId("c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb")
        .deviceCode()
        .withoutDeviceToken()
        .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(true);

    static {
        MinecraftAuth.LOGGER = new Slf4jConsoleLogger(AUTH_LOG);
    }

    public static final File AUTH_CACHE_FILE = new File("mc_auth_cache.json");

    public void clearAuthCache() {
        try {
            Files.deleteIfExists(AUTH_CACHE_FILE.toPath());
        } catch (IOException ex) {
            AUTH_LOG.error("Unable to delete msal cache file", ex);
        }
    }

    /**
     * Login Sequence:
     *
     * 1. Load auth cache
     * 2. If auth cache is present, attempt to refresh until refreshTryCount limit is reached
     * 2a. If refreshTryCount limit is reached, wipe cache and full login
     * 3. If auth cache is not present, full login
     *
     * caller process will stop login attempts after 3 tries
     * so the auth cache wipe could happen before or after that is reached depending on config
     *
     */

    @SneakyThrows
    public MinecraftProtocol login()  {
        if (CONFIG.authentication.accountType == OFFLINE) {
            AUTH_LOG.warn("Using offline account: '{}'. Offline accounts will not receive user support.", CONFIG.authentication.username);
            return createMinecraftProtocol(offlineLogin());
        }
        var cachedAuth = loadAuthCache()
            .flatMap(this::checkAuthCacheMatchesConfig);
        // throws on failed login
        var authSession = cachedAuth
            .map(this::useCacheOrRefreshLogin)
            .orElseGet(this::fullLogin);
        this.refreshTryCount = 0;
        saveAuthCacheAsync(authSession);
        updateConfig(authSession);
        if (this.refreshTask != null) this.refreshTask.cancel(true);
        if (CONFIG.authentication.authTokenRefresh) scheduleAuthCacheRefresh(authSession);
        return createMinecraftProtocol(authSession);
    }

    private FullJavaSession useCacheOrRefreshLogin(FullJavaSession session) {
        if (!CONFIG.authentication.alwaysRefreshOnLogin && shouldUseCachedSessionWithoutRefresh(session)) {
            AUTH_LOG.debug("Using cached auth session without refresh. expiry time: {}",
                           MathHelper.formatDuration(Duration.ofMillis(session.getMcProfile().getMcToken().getExpireTimeMs() - System.currentTimeMillis())));
            return session;
        }
        else return refreshOrFullLogin(session);
    }

    private FullJavaSession refreshOrFullLogin(FullJavaSession session) {
        var refreshSession = tryRefresh(session);
        if (refreshSession.isPresent()) {
            return refreshSession.get();
        } else {
            if (refreshTryCount < CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe) {
                AUTH_LOG.error("Failed to refresh auth attempt {}", refreshTryCount++);
                throw new RuntimeException("Unable to log in");
            } else {
                AUTH_LOG.error("Failed to refresh auth {} times, clearing cache", refreshTryCount);
                clearAuthCache();
                return fullLogin();
            }
        }
    }

    private boolean shouldUseCachedSessionWithoutRefresh(FullJavaSession session) {
        var playerCertificates = session.getPlayerCertificates();
        if (playerCertificates != null && playerCertificates.getExpireTimeMs() < System.currentTimeMillis()) return false;
        return session.getMcProfile().getMcToken().getExpireTimeMs() > System.currentTimeMillis();
    }

    private MinecraftProtocol createMinecraftProtocol(FullJavaSession authSession) {
        var javaProfile = authSession.getMcProfile();
        var gameProfile = new GameProfile(javaProfile.getId(), javaProfile.getName());
        gameProfile.setPlayerCertificates(authSession.getPlayerCertificates());
        var mcToken = javaProfile.getMcToken();
        var accessToken = mcToken != null ? mcToken.getAccessToken() : null;
        return new MinecraftProtocol(MinecraftCodec.CODEC, gameProfile, accessToken);
    }

    @SneakyThrows
    private FullJavaSession deviceCodeLogin() {
        return getDeviceCodeAuthStep().getFromInput(createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
    }

    @SneakyThrows
    private FullJavaSession withoutDeviceTokenLogin() {
        return getDeviceCodeAuthWithoutDeviceTokenStep().getFromInput(createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
    }

    @SneakyThrows
    private FullJavaSession msaLogin() {
        return getMsaAuthStep().getFromInput(createHttpClient(), new StepCredentialsMsaCode.MsaCredentials(CONFIG.authentication.email, CONFIG.authentication.password));
    }

    @SneakyThrows
    private FullJavaSession prismDeviceCodeLogin() {
        return getPrismDeviceCodeAuthStep().getFromInput(createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
    }

    public Optional<FullJavaSession> tryRefresh(final FullJavaSession session) {
        AUTH_LOG.debug("Performing token refresh..");
        try {
            return Optional.of(getAuthStep().refresh(createHttpClient(), session));
        } catch (Exception e) {
            AUTH_LOG.debug("Failed to refresh token", e);
            return Optional.empty();
        }
    }

    public StepFullJavaSession getAuthStep() {
        return switch (CONFIG.authentication.accountType) {
            case MSA -> getMsaAuthStep();
            case DEVICE_CODE -> getDeviceCodeAuthStep();
            case DEVICE_CODE_WITHOUT_DEVICE_TOKEN -> getDeviceCodeAuthWithoutDeviceTokenStep();
            case PRISM -> getPrismDeviceCodeAuthStep();
            case OFFLINE -> null;
        };
    }

    public FullJavaSession fullLogin() {
        return switch (CONFIG.authentication.accountType) {
            case MSA -> msaLogin();
            case DEVICE_CODE -> deviceCodeLogin();
            case DEVICE_CODE_WITHOUT_DEVICE_TOKEN -> withoutDeviceTokenLogin();
            case PRISM -> prismDeviceCodeLogin();
            case OFFLINE -> offlineLogin();
        };
    }

    private FullJavaSession offlineLogin() {
        return new FullJavaSession(new StepMCProfile.MCProfile(UUID.randomUUID(), CONFIG.authentication.username, null, null), null);
    }

    private void onDeviceCode(final StepMsaDeviceCode.MsaDeviceCode code) {
        AUTH_LOG.error("Login Here: {} with code: {}", code.getDirectVerificationUri(), code.getUserCode());
        EVENT_BUS.postAsync(new MsaDeviceCodeLoginEvent(code));
        if (CONFIG.authentication.openBrowserOnLogin) tryOpenBrowser(code.getDirectVerificationUri());
    }

    private void tryOpenBrowser(final String url) {
        try {
            WebBrowserHelper.openBrowser(url);
        } catch (final Exception e) {
            AUTH_LOG.debug("Failed to open browser", e);
        }
    }

    private void scheduleAuthCacheRefresh(FullJavaSession session) {
        var time = session.getMcProfile().getMcToken().getExpireTimeMs() - System.currentTimeMillis();
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
            MILLISECONDS);
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
            var refreshResult = tryRefresh(authCache.get());
            if (refreshResult.isEmpty()) {
                AUTH_LOG.error("Failed to perform background auth refresh");
                return;
            }
            var authSession = refreshResult.get();
            updateConfig(authSession);
            saveAuthCacheAsync(authSession);
            scheduleAuthCacheRefresh(authSession);
        } catch (Throwable e) {
            AUTH_LOG.error("Error refreshing auth token", e);
        }
    }

    public void saveAuthCache(final FullJavaSession session) {
        saveAuthCacheJson(getAuthStep().toJson(session));
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

    public void updateConfig(FullJavaSession javaSession) {
        var javaProfile = javaSession.getMcProfile();
        if (!CONFIG.authentication.username.equals(javaProfile.getName())) {
            CONFIG.authentication.username = javaProfile.getName();
            saveConfigAsync();
        }
    }

    public void saveAuthCacheAsync(final FullJavaSession session) {
        Thread.ofVirtual().name("Auth Cache Writer").start(() -> saveAuthCache(session));
    }

    public Optional<FullJavaSession> loadAuthCache() {
        if (!AUTH_CACHE_FILE.exists()) return Optional.empty();
        fixupAuthCacheIfPlayerCertsMissing();
        return readAuthCacheJson()
            .map(json -> getAuthStep().fromJson(json));
    }

    @Locked
    private Optional<JsonObject> readAuthCacheJson() {
        try (Reader reader = new FileReader(AUTH_CACHE_FILE)) {
            final JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return Optional.of(json);
        } catch (final NullPointerException e) {
            AUTH_LOG.debug("Unable to load auth cache!", e);
            if (e.getMessage().contains("com.google.gson.JsonObject")) {
                AUTH_LOG.warn("Auth cache incompatible with current auth type");
            }
            return Optional.empty();
        } catch (Exception e) {
            AUTH_LOG.debug("Unable to load auth cache!", e);
            return Optional.empty();
        }
    }

    private void fixupAuthCacheIfPlayerCertsMissing() {
        var jsonOptional = readAuthCacheJson();
        if (jsonOptional.isEmpty()) return;
        var json = jsonOptional.get();
        try {
            var playerCertificatesJson = json.getAsJsonObject("playerCertificates");
            if (playerCertificatesJson != null) return;
        } catch (Exception e) {
            AUTH_LOG.warn("Error reading auth cache while fixing up player certs in auth cache", e);
            return;
        }
        AUTH_LOG.info("Found auth cache without player certs, inserting dummy data fixup");
        try {
            var certsJson = new JsonObject();
            certsJson.addProperty("expireTimeMs", 0);
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var keypair = generator.generateKeyPair();
            var dummyPubKey = keypair.getPublic().getEncoded();
            var dummyPrivKey = keypair.getPrivate().getEncoded();
            certsJson.addProperty("publicKey", Base64.getEncoder().encodeToString(dummyPubKey));
            certsJson.addProperty("privateKey", Base64.getEncoder().encodeToString(dummyPrivKey));
            certsJson.addProperty("publicKeySignature", Base64.getEncoder().encodeToString("foo".getBytes()));
            certsJson.addProperty("legacyPublicKeySignature", Base64.getEncoder().encodeToString("bar".getBytes()));
            json.add("playerCertificates", certsJson);
            saveAuthCacheJson(json);
            AUTH_LOG.info("Auth cache fixup dummy data write completed");
        } catch (final Exception e) {
            AUTH_LOG.warn("Error writing auth cache while fixing up player certs in auth cache", e);
        }
    }

    private Optional<FullJavaSession> checkAuthCacheMatchesConfig(FullJavaSession authCacheSession) {
        if (!authCacheSession.getMcProfile().getName().equals(CONFIG.authentication.username)) {
            AUTH_LOG.info("Cached auth username does not match config username, clearing cache");
            clearAuthCache();
            return Optional.empty();
        }
        return Optional.of(authCacheSession);
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
