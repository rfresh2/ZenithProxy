package com.zenith.network.client;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaDeviceAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.microsoft.aad.msal4j.DeviceCode;
import com.zenith.event.proxy.MsaDeviceCodeLoginEvent;
import com.zenith.util.Config.Authentication.AccountType;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import lombok.Getter;

import java.io.IOException;
import java.util.Objects;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

@Getter
public class Authenticator {
    private static final String CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb"; // prism launcher client id lol don't sue me
    protected AuthenticationService auth;

    private AuthenticationService getAuth() {
        if (nonNull(this.auth)) return this.auth;
        return getAuthenticationService();
    }

    public MinecraftProtocol handleRelog()  {
        this.auth = this.getAuth();
        try {
            this.auth.login();
            if (CONFIG.authentication.accountType == AccountType.MSA) {
                if (!Objects.equals(CONFIG.authentication.username, auth.getSelectedProfile().getName())) {
                    CONFIG.authentication.username = auth.getSelectedProfile().getName();
                    saveConfig();
                }
                return new MinecraftProtocol(
                    auth.getSelectedProfile(), "", auth.getAccessToken()
                );
            } else if (CONFIG.authentication.accountType == AccountType.DEVICE_CODE) {
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
        } catch (RequestException | MicrosoftAuthenticationException e) {
            this.auth = null;
            throw new RuntimeException("Unable to log in", e);
        }
    }

    public void onDeviceCode(final DeviceCode code) {
        CLIENT_LOG.error("Please go to " + code.verificationUri() + " and enter " + code.userCode() + " to authenticate.");
        EVENT_BUS.dispatch(new MsaDeviceCodeLoginEvent(code));
    }

    private AuthenticationService getAuthenticationService() {
        if (CONFIG.authentication.doAuthentication) {
            if (CONFIG.authentication.accountType == AccountType.MSA) {
                final MsaAuthenticationService authenticationService = new MsaAuthenticationService();
                authenticationService.setUsername(CONFIG.authentication.email);
                authenticationService.setPassword(CONFIG.authentication.password);
                return authenticationService;
            } else if (CONFIG.authentication.accountType == AccountType.DEVICE_CODE) {
                try {
                    MsaDeviceAuthenticationService msaDeviceAuthenticationService = new MsaDeviceAuthenticationService(
                        CLIENT_ID);
                    msaDeviceAuthenticationService.setDeviceCodeConsumer(this::onDeviceCode);
                    return msaDeviceAuthenticationService;
                } catch (IOException e) {
                    throw new RuntimeException("Device code auth failed", e);
                }
            } else {
                throw new RuntimeException("Invalid authentication type set.");
            }
        } else {
            return new MojangAuthenticationService();
        }
    }
}
