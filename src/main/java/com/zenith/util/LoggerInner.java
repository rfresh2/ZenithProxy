package com.zenith.util;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService2;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import lombok.Getter;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.nonNull;

/**
 * because mcprotocollib is shit
 *
 * @author DaPorkchop_
 */
@Getter
public class LoggerInner {
    protected AuthenticationService auth;

    public LoggerInner()    {

    }

    private AuthenticationService getAuth() {
        if (nonNull(this.auth)) return this.auth;
        AuthenticationService auth;
        if (CONFIG.authentication.doAuthentication) {
            if (CONFIG.authentication.accountType.equalsIgnoreCase("mojang")) {
                auth = new MojangAuthenticationService();
                auth.setUsername(CONFIG.authentication.email);
                auth.setPassword(CONFIG.authentication.password);
            } else if (CONFIG.authentication.accountType.equalsIgnoreCase("msa")) {
                try
                {
                    auth = new MsaAuthenticationService2();
                    auth.setUsername(CONFIG.authentication.email);
                    auth.setPassword(CONFIG.authentication.password);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("No valid account type set. Must set either mojang or msa");
            }
        } else {
            auth = new MojangAuthenticationService();
        }
        return auth;
    }

    public MinecraftProtocol handleRelog()  {
        this.auth = this.getAuth();
        try {
            this.auth.login();
            if (CONFIG.authentication.accountType.equalsIgnoreCase("mojang")) {
                return new MinecraftProtocol(
                        auth.getSelectedProfile(), ((MojangAuthenticationService) auth).getClientToken(), auth.getAccessToken()
                );
            } else if (CONFIG.authentication.accountType.equalsIgnoreCase("msa")) {
                return new MinecraftProtocol(
                        auth.getSelectedProfile(), "", auth.getAccessToken()
                );
            } else {
                throw new RuntimeException("No valid account type set. Must set either mojang or msa");
            }

        } catch (RequestException | MicrosoftAuthenticationException e)    {
            this.auth = null;
            throw new RuntimeException(String.format(
                    "Unable to log in using credentials (%s)",
                    CONFIG.authentication.username), e);
        }
    }
}
