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

package net.daporkchop.toobeetooteebot.gui;

import net.daporkchop.lib.gui.GuiEngine;
import net.daporkchop.lib.gui.component.state.WindowState;
import net.daporkchop.lib.gui.component.type.Window;
import net.daporkchop.lib.gui.util.Alignment;
import net.daporkchop.lib.imaging.bitmap.PIcon;
import net.daporkchop.lib.imaging.bitmap.PImage;
import net.daporkchop.lib.imaging.bitmap.icon.DirectIconARGB;
import net.daporkchop.toobeetooteebot.Bot;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class Gui {
    protected static final PIcon ICON;

    static {
        PIcon icon = null;
        if (CONFIG.gui.enabled) {
            try (InputStream in = Gui.class.getResourceAsStream("/DaPorkchop_.png")) {
                BufferedImage img = ImageIO.read(in);
                icon = new DirectIconARGB(img.getWidth(), img.getHeight());
                PImage mutable = icon.unsafeMutableView();
                for (int x = 0; x < img.getWidth(); x++)    {
                    for (int y = 0; y < img.getHeight(); y++)   {
                        mutable.setARGB(x, y, img.getRGB(x, y));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ICON = icon;
    }

    protected final Deque<String> messageQueue = CONFIG.gui.enabled ? new ArrayDeque<>(CONFIG.gui.messageCount) : null;

    protected Window window;

    public void start() {
        if (!CONFIG.gui.enabled) {
            return;
        }

        this.window = GuiEngine.swing().newWindow(512, 512)
                .setTitle(String.format("Pork2b2tBot v%s", VERSION))
                .setIcon(ICON)
                .label("notImplementedLbl", "GUI is currently unimplemented!", lbl -> lbl
                        .orientRelative(0, 0, 1.0d, 1.0d)
                        .setTextPos(Alignment.CENTER)
                        .setTextColor(Color.RED))
                .addStateListener(WindowState.CLOSED, () -> {
                    SHOULD_RECONNECT = false;
                    if (Bot.getInstance().isConnected()) {
                        Bot.getInstance().getClient().getSession().disconnect("user disconnect");
                    }
                    this.window.release();
                })
                .show();
    }
}
