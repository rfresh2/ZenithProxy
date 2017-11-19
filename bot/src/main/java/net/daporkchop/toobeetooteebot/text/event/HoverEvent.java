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

package net.daporkchop.toobeetooteebot.text.event;

import com.google.common.collect.Maps;
import net.daporkchop.toobeetooteebot.text.ITextComponent;

import java.util.Map;

public class HoverEvent {
    private final HoverEvent.Action action;
    private final ITextComponent value;

    public HoverEvent(HoverEvent.Action actionIn, ITextComponent valueIn) {
        this.action = actionIn;
        this.value = valueIn;
    }

    /**
     * Gets the action to perform when this event is raised.
     */
    public HoverEvent.Action getAction() {
        return this.action;
    }

    /**
     * Gets the value to perform the action on when this event is raised.  For example, if the action is "show item",
     * this would be the item to show.
     */
    public ITextComponent getValue() {
        return this.value;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
            HoverEvent hoverevent = (HoverEvent) p_equals_1_;

            if (this.action != hoverevent.action) {
                return false;
            } else {
                if (this.value != null) {
                    if (!this.value.equals(hoverevent.value)) {
                        return false;
                    }
                } else if (hoverevent.value != null) {
                    return false;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public String toString() {
        return "HoverEvent{action=" + this.action + ", value='" + this.value + '\'' + '}';
    }

    public int hashCode() {
        int i = this.action.hashCode();
        i = 31 * i + (this.value != null ? this.value.hashCode() : 0);
        return i;
    }

    public enum Action {
        SHOW_TEXT("show_text", true),
        SHOW_ITEM("show_item", true),
        SHOW_ENTITY("show_entity", true);

        private static final Map<String, Action> NAME_MAPPING = Maps.newHashMap();

        static {
            for (HoverEvent.Action hoverevent$action : values()) {
                NAME_MAPPING.put(hoverevent$action.getCanonicalName(), hoverevent$action);
            }
        }

        private final boolean allowedInChat;
        private final String canonicalName;

        Action(String canonicalNameIn, boolean allowedInChatIn) {
            this.canonicalName = canonicalNameIn;
            this.allowedInChat = allowedInChatIn;
        }

        /**
         * Gets a value by its canonical name.
         */
        public static HoverEvent.Action getValueByCanonicalName(String canonicalNameIn) {
            return NAME_MAPPING.get(canonicalNameIn);
        }

        /**
         * Indicates whether this event can be run from chat text.
         */
        public boolean shouldAllowInChat() {
            return this.allowedInChat;
        }

        /**
         * Gets the canonical name for this action (e.g., "show_achievement")
         */
        public String getCanonicalName() {
            return this.canonicalName;
        }
    }
}