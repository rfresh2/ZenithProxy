package com.zenith.discord.command;

import com.zenith.Proxy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.List;

import static com.zenith.util.Constants.CONFIG;

public class HelpCommand extends Command {
    private final List<Command> commands;

    public HelpCommand(Proxy proxy, List<Command> commands) {
        super(proxy, "help", "Displays helps commands for the proxy");
        this.commands = commands;
    }

    /**
     * Discord commands:
     *  .connect
     *      * Login player to server
     *  .disconnect
     *      * disconnect a player from the server
     *  .status
     *      * Get current status of player. e.g. are they in queue or ingame, what position in queue, how long online, etc.
     *  .help
     *      * Displays help commands for the proxy
     *  todo:
     *  .whitelist add/del <player>
     *      * Add/del a player to the proxy whitelist
     *  .discordWhitelist add/del <discord @>
     *      * Add/del a discord user from managing this player
     */
    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
                .title("ZenithProxy Help")
                .color(Color.CYAN);
        this.commands.stream()
                .forEach(command -> {
                    embedBuilder.addField(CONFIG.discord.prefix + command.name, command.description, false);
                });
        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder
                        .build())
                .build()
                .asRequest();
    }
}
