/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
