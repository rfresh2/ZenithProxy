/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
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

package net.daporkchop.toobeetooteebot.server.handler.outgoing;

import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.Session;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class LoginSuccessOutgoingHandler implements HandlerRegistry.OutgoingHandler<LoginSuccessPacket, PorkServerConnection> {
    @Override
    public LoginSuccessPacket apply(@NonNull LoginSuccessPacket packet, @NonNull PorkServerConnection session) {
       logger.debug("User UUID: ${0}\nBot UUID: ${1}", packet.getProfile().getId().toString(), CACHE.getProfileCache().getProfile().getId().toString());
        return new LoginSuccessPacket(CACHE.getProfileCache().getProfile());
    }

    @Override
    public Class<LoginSuccessPacket> getPacketClass() {
        return LoginSuccessPacket.class;
    }
}
