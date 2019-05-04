package net.daporkchop.toobeetooteebot.util;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import lombok.Getter;

import java.net.Proxy;
import java.util.UUID;

/**
 * because mcprotocollib is shit
 *
 * @author DaPorkchop_
 */
@Getter
public class LoggerInner implements Constants {
    protected final AuthenticationService auth;

    public LoggerInner()    {
        if (CONFIG.getBoolean("authentication.doAuthentication")) {
            this.auth = new AuthenticationService(UUID.randomUUID().toString(), Proxy.NO_PROXY);
            this.auth.setUsername(CONFIG.getString("authentication.email", "john.doe@example.com"));
            this.auth.setPassword(CONFIG.getString("authentication.password", "hackme"));
        } else {
            this.auth = null;
        }
    }

    public MinecraftProtocol handleRelog()  {
        if (this.auth == null)  {
            return new MinecraftProtocol(CONFIG.getString("authentication.username", "Steve"));
        } else {
            try {
                this.auth.login();
                return new MinecraftProtocol(
                        this.auth.getSelectedProfile(),
                        this.auth.getClientToken(),
                        this.auth.getAccessToken()
                );
            } catch (RequestException e)    {
                throw new RuntimeException(String.format(
                        "Unable to log in using credentials %s:%s",
                        CONFIG.getString("authentication.username"),
                        CONFIG.getString("authentication.password")), e);
            }
        }
    }
}
