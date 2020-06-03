/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.util;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import lombok.Getter;

import java.net.Proxy;
import java.util.UUID;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * because mcprotocollib is shit
 *
 * @author DaPorkchop_
 */
@Getter
public class LoggerInner {
    protected final AuthenticationService auth;

    public LoggerInner()    {
        if (CONFIG.authentication.doAuthentication) {
            this.auth = new AuthenticationService(UUID.randomUUID().toString(), Proxy.NO_PROXY);
            this.auth.setUsername(CONFIG.authentication.email);
            this.auth.setPassword(CONFIG.authentication.password);
        } else {
            this.auth = null;
        }
    }

    public MinecraftProtocol handleRelog()  {
        if (this.auth == null)  {
            return new MinecraftProtocol(CONFIG.authentication.username);
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
                        "Unable to log in using credentials %s:%s (%s)",
                        CONFIG.authentication.email,
                        CONFIG.authentication.password,
                        CONFIG.authentication.username), e);
            }
        }
    }
}
