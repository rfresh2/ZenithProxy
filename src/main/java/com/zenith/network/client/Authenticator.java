package com.zenith.network.client;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.zenith.util.Config.Authentication.AccountType;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import lombok.Getter;

import java.util.Objects;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.saveConfig;
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
            if (CONFIG.authentication.accountType == AccountType.MSA) {
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

    private AuthenticationService getAuthenticationService() {
        if (CONFIG.authentication.doAuthentication) {
            if (CONFIG.authentication.accountType == AccountType.MSA) {
                final MsaAuthenticationService authenticationService = new MsaAuthenticationService();
                authenticationService.setUsername(CONFIG.authentication.email);
                authenticationService.setPassword(CONFIG.authentication.password);
                return authenticationService;
            } else {
                throw new RuntimeException("Invalid authentication type set.");
            }
        } else {
            return new MojangAuthenticationService();
        }
    }
}
