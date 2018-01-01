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

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.packetlib.Session;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.client.IPacketListener;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.web.PlayData;

import static net.daporkchop.toobeetooteebot.TooBeeTooTeeBot.bot;

public class ListenerPlayerListEntryPacket implements IPacketListener<ServerPlayerListEntryPacket> {
    @Override
    public void handlePacket(Session session, ServerPlayerListEntryPacket pck) {
        switch (pck.getAction()) {
            case ADD_PLAYER:
                LOOP:
                for (PlayerListEntry entry : pck.getEntries()) {
                    for (PlayerListEntry listEntry : bot.playerListEntries) {
                        if (listEntry.getProfile().getIdAsString().equals(entry.getProfile().getIdAsString())) {
                            continue LOOP;
                        }
                    }
                    bot.playerListEntries.add(entry);
                    if (bot.websocketServer != null) {
                        bot.websocketServer.sendToAll("tabAdd  " + TooBeeTooTeeBot.getName(entry) + " " + entry.getPing() + " " + entry.getProfile().getIdAsString());
                    }
                    if (Config.doStatCollection) {
                        String uuid = entry.getProfile().getId().toString();
                        if (bot.uuidsToPlayData.containsKey(uuid)) {
                            PlayData data = bot.uuidsToPlayData.get(uuid);
                            data.lastPlayed = System.currentTimeMillis();
                        } else {
                            PlayData data = new PlayData(uuid, TooBeeTooTeeBot.getName(entry));
                            bot.uuidsToPlayData.put(data.UUID, data);
                        }
                    }
                }
                break;
            case UPDATE_GAMEMODE:
                //ignore
                break;
            case UPDATE_LATENCY:
                for (PlayerListEntry entry : pck.getEntries()) {
                    String uuid = entry.getProfile().getId().toString();
                    for (PlayerListEntry toChange : bot.playerListEntries) {
                        if (uuid.equals(toChange.getProfile().getId().toString())) {
                            toChange.ping = entry.getPing();
                            if (bot.websocketServer != null) {
                                bot.websocketServer.sendToAll("tabPing " + toChange.getDisplayName() + " " + toChange.getPing());
                            }
                            if (Config.doStatCollection) {
                                for (PlayData playData : bot.uuidsToPlayData.values()) {
                                    int playTimeDifference = (int) (System.currentTimeMillis() - playData.lastPlayed);
                                    playData.playTimeByHour[0] += playTimeDifference;
                                    playData.playTimeByDay[0] += playTimeDifference;
                                    playData.lastPlayed = System.currentTimeMillis();
                                }
                            }
                            break;
                        }
                    }
                }
                break;
            case UPDATE_DISPLAY_NAME:
                //ignore
                break;
            case REMOVE_PLAYER:
                for (PlayerListEntry entry : pck.getEntries()) {
                    String uuid = entry.getProfile().getId().toString();
                    int removalIndex = -1;
                    for (int i = 0; i < bot.playerListEntries.size(); i++) {
                        PlayerListEntry player = bot.playerListEntries.get(i);
                        if (uuid.equals(player.getProfile().getId().toString())) {
                            removalIndex = i;
                            if (bot.websocketServer != null) {
                                bot.websocketServer.sendToAll("tabDel  " + TooBeeTooTeeBot.getName(player));
                            }
                            if (Config.doStatCollection) {
                                bot.uuidsToPlayData.get(uuid).lastPlayed = System.currentTimeMillis();
                            }
                            break;
                        }
                    }
                    if (removalIndex != -1) {
                        bot.playerListEntries.remove(removalIndex);
                    }
                }
                break;
        }
    }
}
