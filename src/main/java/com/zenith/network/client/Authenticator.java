package com.zenith.network.client;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.google.gson.JsonObject;
import com.zenith.event.proxy.MsaDeviceCodeLoginEvent;
import com.zenith.util.MCAuthLoggerBridge;
import com.zenith.util.WebBrowserHelper;
import lombok.Getter;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.minecraftauth.step.msa.StepLocalWebServer;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
public class Authenticator {
    private ScheduledFuture<?> refreshTask;
    private int refreshTryCount = 0;
    private final StepFullJavaSession deviceCodeAuthStep = MinecraftAuth.builder()
        .withTimeout(300)
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID)
        .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .deviceCode()
        .withDeviceToken("Win32")
        .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false); // for chat signing stuff which we don't implement (yet)
    private final StepFullJavaSession msaAuthStep = MinecraftAuth.builder()
        .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
        .credentials()
        .withDeviceToken("Win32")
        .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
        .buildMinecraftJavaProfileStep(false);
    private final StepFullJavaSession localWebserverStep = MinecraftAuth.builder()
        .withTimeout(300)
        // meteor client id lol don't sue me
        .withClientId("4673b348-3efa-4f6a-bbb6-34e141cdc638").withScope(MicrosoftConstants.SCOPE2)
        .withRedirectUri("http://127.0.0.1")
        .localWebServer()
        .withDeviceToken("Win32")
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

    public MinecraftProtocol login()  {
        try {
            var cachedAuth = loadAuthCache();
            var authSession = cachedAuth.isPresent()
                // throws on failed login
                ? refreshOrFullLogin(cachedAuth.get())
                : fullLogin();
            this.refreshTryCount = 0;
            saveAuthCacheAsync(authSession);
            updateConfig(authSession);
            if (this.refreshTask != null) this.refreshTask.cancel(true);
            if (CONFIG.authentication.authTokenRefresh) scheduleAuthCacheRefresh(authSession);
            return createMinecraftProtocol(authSession);
        } catch (final Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    private StepFullJavaSession.FullJavaSession refreshOrFullLogin(StepFullJavaSession.FullJavaSession session) {
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

    private MinecraftProtocol createMinecraftProtocol(StepFullJavaSession.FullJavaSession authSession) {
        var javaProfile = authSession.getMcProfile();
        var gameProfile = new GameProfile(javaProfile.getId(), javaProfile.getName());
        var accessToken = javaProfile.getMcToken().getAccessToken();
        return new MinecraftProtocol(MinecraftCodec.CODEC, gameProfile, accessToken);
    }

    private StepFullJavaSession.FullJavaSession deviceCodeLogin() {
        try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            return deviceCodeAuthStep.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(this::onDeviceCode));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private StepFullJavaSession.FullJavaSession msaLogin() {
        try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            return msaAuthStep.getFromInput(httpClient, new StepCredentialsMsaCode.MsaCredentials(CONFIG.authentication.email, CONFIG.authentication.password));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private StepFullJavaSession.FullJavaSession localWebserverLogin() {
        try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            return localWebserverStep.getFromInput(httpClient, new StepLocalWebServer.LocalWebServerCallback(this::onLocalWebServer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<StepFullJavaSession.FullJavaSession> tryRefresh(final StepFullJavaSession.FullJavaSession session) {
        try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            return Optional.of(getAuthStep().refresh(httpClient, session));
        } catch (Exception e) {
            AUTH_LOG.debug("Failed to refresh token", e);
            return Optional.empty();
        }
    }

    private StepFullJavaSession getAuthStep() {
        return switch (CONFIG.authentication.accountType) {
            case MSA -> msaAuthStep;
            case DEVICE_CODE -> deviceCodeAuthStep;
            case LOCAL_WEBSERVER -> localWebserverStep;
        };
    }

    private StepFullJavaSession.FullJavaSession fullLogin() {
        return switch (CONFIG.authentication.accountType) {
            case MSA -> msaLogin();
            case DEVICE_CODE -> deviceCodeLogin();
            case LOCAL_WEBSERVER -> localWebserverLogin();
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

    private void scheduleAuthCacheRefresh(StepFullJavaSession.FullJavaSession session) {
        var time = session.getMcProfile().getMcToken().getExpireTimeMs() - System.currentTimeMillis();
        if (time <= 0) {
            AUTH_LOG.debug("Auth token refresh time is negative? {}", time);
            return;
        }
        this.refreshTask = SCHEDULED_EXECUTOR_SERVICE.schedule(
            this::executeAuthCacheRefresh,
            Math.min(time, Duration.ofHours(6).toMillis()),
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

    private void saveAuthCache(final StepFullJavaSession.FullJavaSession session) {
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

    private void updateConfig(StepFullJavaSession.FullJavaSession javaSession) {
        var javaProfile = javaSession.getMcProfile();
        if (!CONFIG.authentication.username.equals(javaProfile.getName())) {
            CONFIG.authentication.username = javaProfile.getName();
            saveConfigAsync();
        }
    }

    private void saveAuthCacheAsync(final StepFullJavaSession.FullJavaSession session) {
        Thread.ofVirtual().name("Auth Cache Writer").start(() -> saveAuthCache(session));
    }

    private Optional<StepFullJavaSession.FullJavaSession> loadAuthCache() {
        if (!AUTH_CACHE_FILE.exists()) return Optional.empty();
        try (Reader reader = new FileReader(AUTH_CACHE_FILE)) {
            final JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return Optional.of(getAuthStep().fromJson(json));
        } catch (IOException e) {
            AUTH_LOG.debug("Unable to load auth cache!", e);
            return Optional.empty();
        }
    }
}
