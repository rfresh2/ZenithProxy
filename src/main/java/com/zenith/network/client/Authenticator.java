package com.zenith.network.client;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaDeviceAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.microsoft.aad.msal4j.DeviceCode;
import com.zenith.event.proxy.MsaDeviceCodeLoginEvent;
import com.zenith.util.Config.Authentication.AccountType;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
public class Authenticator {
    protected AuthenticationService auth;
    protected ScheduledFuture<?> refreshTask;
    protected int tryCount = 0;

    private AuthenticationService getAuth() {
        if (nonNull(this.auth)) return this.auth;
        return getAuthenticationService();
    }

    public MinecraftProtocol handleRelog()  {
        this.auth = this.getAuth();
        try {
            this.auth.login();
            tryCount = 0;
            if (auth instanceof MsaDeviceAuthenticationService) {
                if (this.refreshTask != null) {
                    this.refreshTask.cancel(true);
                }
                if (CONFIG.authentication.msaDeviceCodeTokenRefresh)
                    scheduleDeviceCodeRefresh();
            }
            if (CONFIG.authentication.accountType == AccountType.MSA || CONFIG.authentication.accountType == AccountType.DEVICE_CODE) {
                if (!Objects.equals(CONFIG.authentication.username, auth.getSelectedProfile().getName())) {
                    CONFIG.authentication.username = auth.getSelectedProfile().getName();
                    saveConfigAsync();
                }
                return new MinecraftProtocol(
                    MinecraftCodec.CODEC, auth.getSelectedProfile(), auth.getAccessToken()
                );
            } else {
                throw new RuntimeException("No valid account type set.");
            }
        } catch (Exception e) {
            reset();
            throw new RuntimeException("Unable to log in", e);
        }
    }

    private void scheduleDeviceCodeRefresh() {
        if (this.auth instanceof MsaDeviceAuthenticationService deviceService) {
            deviceService.getExpiryDate()
                .ifPresent(date -> {
                    final long time = date.getTime() - System.currentTimeMillis();
                    if (time <= 0) {
                        CLIENT_LOG.debug("Device code refresh time is negative? {}", time);
                        return;
                    }
                    this.refreshTask = SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
                        try {
                            CLIENT_LOG.debug("Running background device code token refresh..");
                            deviceService.refreshMsalToken();
                            if (!Objects.equals(CONFIG.authentication.username, auth.getSelectedProfile().getName())) {
                                CONFIG.authentication.username = auth.getSelectedProfile().getName();
                                saveConfigAsync();
                            }
                            scheduleDeviceCodeRefresh();
                        } catch (Throwable e) {
                            CLIENT_LOG.error("Error refreshing device code token", e);
                        }
                    }, time, MILLISECONDS);
                    CLIENT_LOG.debug("Device code refresh scheduled in {} minutes", this.refreshTask.getDelay(TimeUnit.MINUTES));
                });
        }
    }

    public void onDeviceCode(final DeviceCode code) {
        CLIENT_LOG.error("Login Here: https://microsoft.com/link?otc=" + code.userCode());
        EVENT_BUS.postAsync(new MsaDeviceCodeLoginEvent(code));
    }

    private AuthenticationService getAuthenticationService() {
        if (CONFIG.authentication.accountType == AccountType.MSA) {
            final MsaAuthenticationService authenticationService = new MsaAuthenticationService();
            authenticationService.setUsername(CONFIG.authentication.email);
            authenticationService.setPassword(CONFIG.authentication.password);
            return authenticationService;
        } else if (CONFIG.authentication.accountType == AccountType.DEVICE_CODE) {
            try {
                MsaDeviceAuthenticationService msaDeviceAuthenticationService = new MsaDeviceAuthenticationService(
                    CONFIG.authentication.msaClientId);
                msaDeviceAuthenticationService.setDeviceCodeConsumer(this::onDeviceCode);
                return msaDeviceAuthenticationService;
            } catch (IOException e) {
                throw new RuntimeException("Device code auth failed", e);
            }
        } else {
            throw new RuntimeException("Invalid authentication type set.");
        }
    }

    public void reset() {
        if (this.auth instanceof MsaDeviceAuthenticationService) {
            if (tryCount < CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe) {
                CLIENT_LOG.error("Failed to login with device code attempt {}", tryCount++);
                return;
            } else {
                CLIENT_LOG.debug("Failed to login with device code {} times, clearing cache", tryCount);
                tryCount = 0;
            }
        }
        clearAuth();
    }

    public void clearAuth() {
        try {
            Files.deleteIfExists(Paths.get("msal_serialized_cache.json"));
        } catch (IOException ex) {
            CLIENT_LOG.error("Unable to delete msal cache file", ex);
        }
        this.auth = null;
    }
}
