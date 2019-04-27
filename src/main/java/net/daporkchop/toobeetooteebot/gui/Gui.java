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

package net.daporkchop.toobeetooteebot.gui;

import net.daporkchop.lib.graphics.bitmap.icon.PIcon;
import net.daporkchop.lib.graphics.bitmap.image.buffer.WrapperBufferedImage;
import net.daporkchop.lib.gui.GuiEngine;
import net.daporkchop.lib.gui.component.state.WindowState;
import net.daporkchop.lib.gui.component.type.Window;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.util.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author DaPorkchop_
 */
public class Gui implements Constants {
    public static final boolean ENABLED = CONFIG.getBoolean("gui.enabled", false);

    protected static final PIcon ICON;

    static {
        PIcon icon = null;
        if (ENABLED)    {
            try (InputStream in = Gui.class.getResourceAsStream("/DaPorkchop_.png")) {
                BufferedImage img = ImageIO.read(in);
                icon = new WrapperBufferedImage(img);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ICON = icon;
    }

    protected final Deque<String> messageQueue = ENABLED ? new ArrayDeque<>(CONFIG.getInt("gui.messageCount", 512)) : null;

    protected Window window;

    public void start()   {
        if (ENABLED)    {
            this.window = GuiEngine.swing().newWindow(512, 512)
                    .setTitle(String.format("Pork2b2tBot v%s", VERSION))
                    .setIcon(ICON)
                    .addStateListener(WindowState.CLOSING, () -> {
                        SHOULD_RECONNECT.set(false);
                        if (Bot.getInstance().isConnected()) {
                            Bot.getInstance().getClient().getSession().disconnect("user disconnect");
                        }
                    })
                    .show();
        }
    }
}
