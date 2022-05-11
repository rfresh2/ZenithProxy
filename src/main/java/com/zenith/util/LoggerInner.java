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

package com.zenith.util;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import lombok.Getter;

import static com.zenith.util.Constants.*;

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
        AuthenticationService auth;
        if (CONFIG.authentication.doAuthentication) {
            if (CONFIG.authentication.accountType.equalsIgnoreCase("mojang")) {
                auth = new MojangAuthenticationService();
                auth.setUsername(CONFIG.authentication.email);
                auth.setPassword(CONFIG.authentication.password);
            } else if (CONFIG.authentication.accountType.equalsIgnoreCase("msa")) {
                try
                {
                    auth = new MsaAuthenticationService("1b3f2c18-6aee-48bc-8aa8-636537d84925");
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

        } catch (RequestException e)    {
            throw new RuntimeException(String.format(
                    "Unable to log in using credentials (%s)",
                    CONFIG.authentication.username), e);
        }
    }
}
