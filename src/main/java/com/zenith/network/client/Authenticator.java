package com.zenith.network.client;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaDeviceAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.microsoft.aad.msal4j.DeviceCode;
import com.zenith.event.proxy.MsaDeviceCodeLoginEvent;
import com.zenith.util.Config.Authentication.AccountType;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

@Getter
public class Authenticator {
    protected AuthenticationService auth;

    private AuthenticationService getAuth() {
        if (nonNull(this.auth)) return this.auth;
        return getAuthenticationService();
    }

    public MinecraftProtocol handleRelog()  {
        this.auth = this.getAuth();
        try {
            this.auth.login();
            if (CONFIG.authentication.accountType == AccountType.MSA || CONFIG.authentication.accountType == AccountType.DEVICE_CODE) {
                if (!Objects.equals(CONFIG.authentication.username, auth.getSelectedProfile().getName())) {
                    CONFIG.authentication.username = auth.getSelectedProfile().getName();
                    saveConfig();
                }
                return new MinecraftProtocol(
                    auth.getSelectedProfile(), "", auth.getAccessToken()
                );
            } else {
                throw new RuntimeException("No valid account type set.");
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(Paths.get("msal_serialized_cache.json"));
            } catch (IOException ex) {
                CLIENT_LOG.error("Unable to delete msal cache file", ex);
            }
            this.auth = null;
            throw new RuntimeException("Unable to log in", e);
        }
    }

    public void onDeviceCode(final DeviceCode code) {
        CLIENT_LOG.error("Please go to " + code.verificationUri() + " and enter " + code.userCode() + " to authenticate.");
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
}
