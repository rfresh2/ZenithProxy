/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
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

package net.daporkchop.toobeetooteebot.client.impl;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.github.steveice10.packetlib.Session;
import net.daporkchop.toobeetooteebot.client.IPacketListener;
import net.daporkchop.toobeetooteebot.gui.GuiBot;

import static net.daporkchop.toobeetooteebot.TooBeeTooTeeBot.bot;

public class ListenerUpdateTimePacket implements IPacketListener<ServerUpdateTimePacket> {
    @Override
    public void handlePacket(Session session, ServerUpdateTimePacket pck) {
        if (!bot.isLoggedIn) {
            System.out.println("Logged in!");
            bot.isLoggedIn = true;
            if (GuiBot.INSTANCE != null) {
                GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Logged in!</html>");
                String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                if (split.length > 500) {
                    String toSet = "<html>";
                    for (int j = 1; j < split.length; j++) {
                        toSet += split[j] + "<br>";
                    }
                    toSet = toSet.substring(toSet.length() - 4) + "</html>";
                    GuiBot.INSTANCE.chatDisplay.setText(toSet);
                }
            }
            if (!bot.server.isListening()) {
                bot.server.bind(true);
                System.out.println("Started server!");
                if (GuiBot.INSTANCE != null) {
                    GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Started server!</html>");
                    String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                    if (split.length > 500) {
                        String toSet = "<html>";
                        for (int j = 1; j < split.length; j++) {
                            toSet += split[j] + "<br>";
                        }
                        toSet = toSet.substring(toSet.length() - 4) + "</html>";
                        GuiBot.INSTANCE.chatDisplay.setText(toSet);
                    }
                }
            }
        }
    }
}
