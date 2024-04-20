package com.zenith.network.client;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.google.gson.JsonObject;
import com.zenith.event.proxy.MsaDeviceCodeLoginEvent;
import com.zenith.util.MCAuthLoggerBridge;
import com.zenith.util.WebBrowserHelper;
import com.zenith.util.math.MathHelper;
import lombok.Getter;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.minecraftauth.step.msa.StepLocalWebServer;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.util.MicrosoftConstants;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
public class Authenticator {
    private ScheduledFuture<?> refreshTask;
    private int refreshTryCount = 0;
    @Getter(lazy = true) private final StepFullJavaSession deviceCodeAuthStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID)
        .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .deviceCode()
        .withDeviceToken("Win32")
        .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false); // for chat signing stuff which we don't implement (yet)
    @Getter(lazy = true) private final StepFullJavaSession deviceCodeAuthWithoutDeviceTokenStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID)
        .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .deviceCode()
        .withoutDeviceToken()
        .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false); // for chat signing stuff which we don't implement (yet)
    @Getter(lazy = true) private final StepFullJavaSession msaAuthStep = MinecraftAuth.builder()
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .credentials()
        .withDeviceToken("Win32")
        .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false);
    @Getter(lazy = true) private final StepFullJavaSession localWebserverStep = MinecraftAuth.builder()
        .withTimeout(300)
        // meteor client id lol don't sue me
        .withClientId("4673b348-3efa-4f6a-bbb6-34e141cdc638").withScope(MicrosoftConstants.SCOPE2)
        .withRedirectUri("http://127.0.0.1")
        .localWebServer()
        .withDeviceToken("Win32")
        .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false);
    @Getter(lazy = true) private final StepFullJavaSession prismDeviceCodeAuthStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId("c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb")
        .deviceCode()
        .withoutDeviceToken()
        .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false);

    static {
        MinecraftAuth.LOGGER = new MCAuthLoggerBridge();
    }

    private static final File AUTH_CACHE_FILE = new File("mc_auth_cache.json");

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
        // return true if expiry time is at least 5 mins in the future
        return session.getMcProfile().getMcToken().getExpireTimeMs() - System.currentTimeMillis() > (5 * 60 * 1000);
    }

    private MinecraftProtocol createMinecraftProtocol(FullJavaSession authSession) {
        var javaProfile = authSession.getMcProfile();
        var gameProfile = new GameProfile(javaProfile.getId(), javaProfile.getName());
        var accessToken = javaProfile.getMcToken().getAccessToken();
        return new MinecraftProtocol(MinecraftCodec.CODEC, gameProfile, accessToken);
    }

    @SneakyThrows
    private FullJavaSession deviceCodeLogin() {
        return getDeviceCodeAuthStep().getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
    }

    @SneakyThrows
    private FullJavaSession withoutDeviceTokenLogin() {
        return getDeviceCodeAuthWithoutDeviceTokenStep().getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
    }

    @SneakyThrows
    private FullJavaSession msaLogin() {
        return getMsaAuthStep().getFromInput(MinecraftAuth.createHttpClient(), new StepCredentialsMsaCode.MsaCredentials(CONFIG.authentication.email, CONFIG.authentication.password));
    }

    @SneakyThrows
    private FullJavaSession localWebserverLogin() {
        return getLocalWebserverStep().getFromInput(MinecraftAuth.createHttpClient(), new StepLocalWebServer.LocalWebServerCallback(this::onLocalWebServer));
    }

    @SneakyThrows
    private FullJavaSession prismDeviceCodeLogin() {
        return getPrismDeviceCodeAuthStep().getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
    }

    private Optional<FullJavaSession> tryRefresh(final FullJavaSession session) {
        AUTH_LOG.debug("Performing token refresh..");
        try {
            return Optional.of(getAuthStep().refresh(MinecraftAuth.createHttpClient(), session));
        } catch (Exception e) {
            AUTH_LOG.debug("Failed to refresh token", e);
            return Optional.empty();
        }
    }

    private StepFullJavaSession getAuthStep() {
        return switch (CONFIG.authentication.accountType) {
            case MSA -> getMsaAuthStep();
            case DEVICE_CODE -> getDeviceCodeAuthStep();
            case LOCAL_WEBSERVER -> getLocalWebserverStep();
            case DEVICE_CODE_WITHOUT_DEVICE_TOKEN -> getDeviceCodeAuthWithoutDeviceTokenStep();
            case PRISM -> getPrismDeviceCodeAuthStep();
        };
    }

    private FullJavaSession fullLogin() {
        return switch (CONFIG.authentication.accountType) {
            case MSA -> msaLogin();
            case DEVICE_CODE -> deviceCodeLogin();
            case LOCAL_WEBSERVER -> localWebserverLogin();
            case DEVICE_CODE_WITHOUT_DEVICE_TOKEN -> withoutDeviceTokenLogin();
            case PRISM -> prismDeviceCodeLogin();
        };
    }

    private void onLocalWebServer(final StepLocalWebServer.LocalWebServer server) {
        AUTH_LOG.info("Login Here: {}", server.getAuthenticationUrl());
        if (CONFIG.authentication.openBrowserOnLogin) tryOpenBrowser(server.getAuthenticationUrl());
    }

    private void onDeviceCode(final StepMsaDeviceCode.MsaDeviceCode code) {
        AUTH_LOG.error("Login Here: {}", code.getDirectVerificationUri());
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
        var expireTimeDelayMs = Math.max(minRefreshDelayMs, time - minRefreshDelayMs - randomOffsetMs);
        var maxRefreshIntervalMs = (CONFIG.authentication.maxRefreshIntervalMins * 60L * 1000L) - randomOffsetMs;
        this.refreshTask = EXECUTOR.schedule(
            this::executeAuthCacheRefresh,
            Math.max(minRefreshDelayMs, Math.min(expireTimeDelayMs, maxRefreshIntervalMs)),
            MILLISECONDS);
        AUTH_LOG.debug("Auth cache refresh scheduled in {} minutes", this.refreshTask.getDelay(TimeUnit.MINUTES));
    }

    private void executeAuthCacheRefresh() {
        try {
            AUTH_LOG.debug("Running background auth token refresh..");
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

    private void saveAuthCache(final FullJavaSession session) {
        final JsonObject json = getAuthStep().toJson(session);
        try {
            final File tempFile = new File(AUTH_CACHE_FILE.getAbsolutePath() + ".tmp");
            if (tempFile.exists()) tempFile.delete();
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(json, out);
            }
            com.google.common.io.Files.move(tempFile, AUTH_CACHE_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save auth cache", e);
        }
        AUTH_LOG.debug("Auth cache saved!");
    }

    private void updateConfig(FullJavaSession javaSession) {
        var javaProfile = javaSession.getMcProfile();
        if (!CONFIG.authentication.username.equals(javaProfile.getName())) {
            CONFIG.authentication.username = javaProfile.getName();
            saveConfigAsync();
        }
    }

    private void saveAuthCacheAsync(final FullJavaSession session) {
        Thread.ofVirtual().name("Auth Cache Writer").start(() -> saveAuthCache(session));
    }

    private Optional<FullJavaSession> loadAuthCache() {
        if (!AUTH_CACHE_FILE.exists()) return Optional.empty();
        try (Reader reader = new FileReader(AUTH_CACHE_FILE)) {
            final JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return Optional.of(getAuthStep().fromJson(json));
        } catch (IOException e) {
            AUTH_LOG.debug("Unable to load auth cache!", e);
            return Optional.empty();
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
}
