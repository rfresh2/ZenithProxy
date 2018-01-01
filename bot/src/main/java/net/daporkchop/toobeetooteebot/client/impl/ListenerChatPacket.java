/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2017 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.toobeetooteebot.client.impl;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.packetlib.Session;
import net.daporkchop.toobeetooteebot.client.IPacketListener;
import net.daporkchop.toobeetooteebot.gui.GuiBot;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.util.ChatUtils;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.TextFormat;

import java.util.Iterator;

import static net.daporkchop.toobeetooteebot.TooBeeTooTeeBot.bot;

public class ListenerChatPacket implements IPacketListener<ServerChatPacket> {
    @Override
    public void handlePacket(Session session, ServerChatPacket pck) {
        String messageJson = pck.getMessage().toJsonString();
        String legacyColorCodes = ChatUtils.getOldText(messageJson);
        String msg = TextFormat.clean(legacyColorCodes);

        if (Config.processChat) {
            if (msg.startsWith("To ")) {
                //don't bother processing sent DMs
                return;
            }
            try {
                String[] split = msg.split(" ");
                if (!msg.startsWith("<") && split[1].startsWith("whispers")) {
                    bot.processMsg(split[0], msg.substring(split[0].length() + split[1].length() + 2));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                //ignore kek
            }
        }
        if (msg.startsWith("!")) { //command from connected user
            if (msg.startsWith("!toggleafk")) { //useful when manually moving bot around
                Config.doAntiAFK = !Config.doAntiAFK;
                System.out.println("! Toggled AntiAFK! Current state: " + (Config.doAntiAFK ? "on" : "off"));
                bot.queueMessage("! Toggled AntiAFK! Current state: " + (Config.doAntiAFK ? "on" : "off"));
            }
            return;
        }
        System.out.println("[CHAT] " + msg);

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>" + msg.replace("<", "&lt;").replace(">", "&gt;") + "</html>");
            String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
            if (split.length > 500) {
                String toSet = "<html>";
                for (int i = 1; i < split.length; i++) {
                    toSet += split[i] + "<br>";
                }
                toSet = toSet.substring(toSet.length() - 4) + "</html>";
                GuiBot.INSTANCE.chatDisplay.setText(toSet);
            }
        }
        if (bot.websocketServer != null && !(msg.contains("whispers") || msg.startsWith("to"))) {
            bot.websocketServer.sendToAll("chat    " + legacyColorCodes.replace("<", "&lt;").replace(">", "&gt;"));
        }

        Iterator<PorkClient> iterator = bot.clients.iterator();
        while (iterator.hasNext()) {
            PorkClient client = iterator.next();
            if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) { //thx 0x kek
                client.session.send(new ServerChatPacket(legacyColorCodes));
            }
        }
    }
}
