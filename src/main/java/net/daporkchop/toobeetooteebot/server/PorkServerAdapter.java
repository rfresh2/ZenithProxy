package net.daporkchop.toobeetooteebot.server;

import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;

public class PorkServerAdapter extends ServerAdapter {
    public TooBeeTooTeeBot bot;

    public PorkServerAdapter(TooBeeTooTeeBot tooBeeTooTeeBot) {
        bot = tooBeeTooTeeBot;
    }

    @Override
    public void sessionAdded(SessionAddedEvent event) {
        if (bot.isLoggedIn) {
            PorkClient newClient = new PorkClient(event.getSession(), bot.clients.size());
            bot.clients.add(newClient);
            bot.sessionToClient.put(event.getSession(), newClient);

            event.getSession().addListener(new PorkSessionAdapter(newClient, bot));
        } else {
            event.getSession().disconnect("Not logged in yet, please wait a moment!");
        }
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        PorkClient toRemove = bot.sessionToClient.remove(event.getSession());
        bot.clients.remove(toRemove.arrayIndex);
        if (bot.clients.size() > 0) {
            for (int i = toRemove.arrayIndex; i < bot.clients.size(); i++) {
                bot.clients.get(i).arrayIndex--;
            }
        }
    }
}
