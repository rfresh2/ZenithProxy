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

package net.daporkchop.toobeetooteebot.text.event;

import com.google.common.collect.Maps;

import java.util.Map;

public class ClickEvent {
    private final ClickEvent.Action action;
    private final String value;

    public ClickEvent(ClickEvent.Action theAction, String theValue) {
        super();
        this.action = theAction;
        this.value = theValue;
    }

    /**
     * Gets the action to perform when this event is raised.
     */
    public ClickEvent.Action getAction() {
        return this.action;
    }

    /**
     * Gets the value to perform the action on when this event is raised.  For example, if the action is "open URL",
     * this would be the URL to open.
     */
    public String getValue() {
        return this.value;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
            ClickEvent clickevent = (ClickEvent) p_equals_1_;

            if (this.action != clickevent.action) {
                return false;
            } else {
                if (this.value != null) {
                    if (!this.value.equals(clickevent.value)) {
                        return false;
                    }
                } else if (clickevent.value != null) {
                    return false;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public String toString() {
        return "ClickEvent{action=" + this.action + ", value='" + this.value + '\'' + '}';
    }

    public int hashCode() {
        int i = this.action.hashCode();
        i = 31 * i + (this.value != null ? this.value.hashCode() : 0);
        return i;
    }

    public enum Action {
        OPEN_URL("open_url", true),
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        SUGGEST_COMMAND("suggest_command", true),
        CHANGE_PAGE("change_page", true);

        private static final Map<String, Action> NAME_MAPPING = Maps.newHashMap();

        static {
            for (ClickEvent.Action clickevent$action : values()) {
                NAME_MAPPING.put(clickevent$action.getCanonicalName(), clickevent$action);
            }
        }

        private final boolean allowedInChat;
        /**
         * The canonical name used to refer to this action.
         */
        private final String canonicalName;

        Action(String canonicalNameIn, boolean allowedInChatIn) {
            this.canonicalName = canonicalNameIn;
            this.allowedInChat = allowedInChatIn;
        }

        /**
         * Gets a value by its canonical name.
         */
        public static ClickEvent.Action getValueByCanonicalName(String canonicalNameIn) {
            return NAME_MAPPING.get(canonicalNameIn);
        }

        /**
         * Indicates whether this event can be run from chat text.
         */
        public boolean shouldAllowInChat() {
            return this.allowedInChat;
        }

        /**
         * Gets the canonical name for this action (e.g., "run_command")
         */
        public String getCanonicalName() {
            return this.canonicalName;
        }
    }
}