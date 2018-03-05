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

package net.daporkchop.toobeetooteebot.web;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.UUID;

import static net.daporkchop.toobeetooteebot.TooBeeTooTeeBot.bot;

public class WebsocketServer extends WebSocketServer {
    public WebsocketServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
        bot.timer.schedule(new TimerTask() {
            @Override
            public void run() { //Automatically log inactive users out
                long maxIdleTime = System.currentTimeMillis() - 600000;
                Iterator<LoggedInPlayer> iterator = bot.namesToLoggedInPlayers.values().iterator();
                while (iterator.hasNext()) {
                    LoggedInPlayer next = iterator.next();
                    if (next.lastUsed < maxIdleTime) {
                        iterator.remove();
                        continue;
                    }
                }
            }
        }, 10000, 10000);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("connect ");
        bot.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bot.tabHeader != null && bot.tabFooter != null) {
                    String header = bot.tabHeader.getFullText();
                    String footer = bot.tabFooter.getFullText();
                    conn.send("tabDiff " + header + " " + footer);
                } else if (bot.tabHeader != null) {
                    String header = bot.tabHeader.getFullText();
                    conn.send("tabDiff " + header + "  ");
                } else if (bot.tabFooter != null) {
                    String footer = bot.tabFooter.getFullText();
                    conn.send("tabDiff   " + footer);
                }
                for (PlayerListEntry entry : bot.playerListEntries) {
                    conn.send("tabAdd  " + TooBeeTooTeeBot.getName(entry) + " " + entry.getPing() + " " + entry.getProfile().getIdAsString());
                }
            }
        }, 1000);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //System.out.println(conn.getResourceDescriptor() + " disconnected with code " + code + " and reason " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            if (message.startsWith("login   ")) {
                message = message.substring(8);
                String[] split = message.split(" ");
                String username = split[0];
                String passwordHash = split[1];
                if (bot.namesToRegisteredPlayers.containsKey(username)) {
                    RegisteredPlayer player = bot.namesToRegisteredPlayers.get(username);
                    passwordHash = Hashing.sha256().hashString(passwordHash, Charsets.UTF_8).toString();
                    if (passwordHash.equals(player.passwordHash)) {
                        LoggedInPlayer toAdd = new LoggedInPlayer(player, conn);
                        bot.namesToLoggedInPlayers.put(username, toAdd);
                        conn.send("loginOk " + username + " " + passwordHash);
                        return;
                    } else {
                        conn.send("loginErrInvalid password!");
                        return;
                    }
                } else {
                    if (bot.namesToTempAuths.containsKey(username)) {
                        NotRegisteredPlayer toAdd = bot.namesToTempAuths.get(username);
                        conn.send("loginErrThis account isn't registered! To register, please join 2b2t with the account and use <strong>/msg 2pork2bot register " + toAdd.tempAuthUUID + "</strong>! This registration information will expire after 10 minutes, but you can start the registration cycle again after that time expires.");
                        return;
                    }
                    NotRegisteredPlayer toAdd = new NotRegisteredPlayer();
                    toAdd.name = username;
                    toAdd.pwd = passwordHash;
                    toAdd.tempAuthUUID = UUID.randomUUID().toString();
                    bot.namesToTempAuths.put(toAdd.name, toAdd);
                    conn.send("loginErrThis account isn't registered! To register, please join 2b2t with the account and use <strong>/msg 2pork2bot register " + toAdd.tempAuthUUID + "</strong>! This registration information will expire after 10 minutes, but you can start the registration cycle again after that time expires.");
                    return;
                }
            } else if (message.startsWith("sendChat")) {
                message = message.substring(8);
                String[] split = message.split(" ");
                String text = split[0];
                String targetName = split[1];
                String username = split[2];
                String password = split[3];
                LoggedInPlayer player = bot.namesToLoggedInPlayers.getOrDefault(username, null);
                if (player == null) {
                    RegisteredPlayer registeredPlayer = bot.namesToRegisteredPlayers.getOrDefault(username, null);
                    if (registeredPlayer == null) {
                        conn.send("loginErrSomething is SERIOUSLY wrong. Please report this to DaPorkchop_ ASAP. A RegisteredPlayer was null! (or you broke the script lol)");
                        return;
                    } else {
                        LoggedInPlayer toAdd = new LoggedInPlayer(registeredPlayer, conn);
                        bot.namesToLoggedInPlayers.put(toAdd.player.name, toAdd);
                        player = toAdd;
                    }
                }

                if (player != null) { //doing another check in case it logged the user back in
                    //String pwdH = Hashing.sha256().hashString(password, Charsets.UTF_8).toString();
                    if (player.player.passwordHash.equals(password)) {
                        if (player.lastSentMessage + 5000 > System.currentTimeMillis()) {
                            conn.send("loginErrPlease slow down! Max. 1 message per 5 seconds!");
                            return;
                        }
                        player.lastSentMessage = System.currentTimeMillis();
                        conn.send("chat    " + ("§dTo " + targetName + ": " + text).replace("<", "&lt;").replace(">", "&gt;"));
                        conn.send("chatSent");
                        bot.queueMessage("/msg " + targetName + " " + username + ": " + text);
                        return;
                    } else {
                        conn.send("loginErrSomething is SERIOUSLY wrong. Please report this to DaPorkchop_ ASAP. Invalid password sent with chat! (or you broke the script lol)");
                        return;
                    }
                } else {
                    conn.send("loginErrSomething is SERIOUSLY wrong. Please report this to DaPorkchop_ ASAP. A message was sent for an invalid user! (or you broke the script lol)");
                    return;
                }
            }
        } catch (Exception e) {
            conn.send("loginErr" + e.getMessage());
            return;
        }
    }

    @Override
    public void onFragment(WebSocket conn, Framedata fragment) {
        System.out.println("received fragment: " + fragment);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
            conn.send("error   ");
        }
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket started!");
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    public void sendToAll(String text) {
        //System.out.println(text);
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }
}
