package net.daporkchop.toobeetooteebot.text.event;

import com.google.common.collect.Maps;

import java.util.Map;

public class ClickEvent {
    private final ClickEvent.Action action;
    private final String value;

    public ClickEvent(ClickEvent.Action theAction, String theValue) {
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