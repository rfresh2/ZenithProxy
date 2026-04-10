package com.zenith.command.api;

import com.zenith.discord.Embed;
import com.zenith.util.MentionUtil;
import lombok.Data;
import net.dv8tion.jda.api.entities.ISnowflake;

import java.util.List;
import java.util.Optional;

import static com.zenith.Globals.CONFIG;

@Data
public class DiscordCommandSource implements CommandSource {
    @Override
    public String name() {
        return "Discord";
    }

    @Override
    public String commandPrefix() {
        return CONFIG.discord.prefix;
    }

    @Override
    public boolean validateAccountOwner(CommandContext ctx) {
        if (!(ctx instanceof DiscordCommandContext context)) return false;
        var event = context.getMessageReceivedEvent();
        final boolean hasAccountOwnerRole = Optional.ofNullable(event.getMember())
            .orElseThrow(() -> new RuntimeException("Message does not have a valid member"))
            .getRoles()
            .stream()
            .map(ISnowflake::getId)
            .anyMatch(roleId -> roleId.equals(CONFIG.discord.accountOwnerRoleId));
        if (!hasAccountOwnerRole) {
            String accountOwnerRoleMention = "";
            try {
                accountOwnerRoleMention = MentionUtil.forRole(CONFIG.discord.accountOwnerRoleId);
            } catch (final Exception e) {
                // fall through
            }
            context.getEmbed()
                .addField("Error",
                          "User: " + Optional.ofNullable(event.getMember()).map(m -> m.getUser().getName()).orElse("Unknown")
                              + " is not authorized to execute this command! "
                              + "You must have the account owner role: " + accountOwnerRoleMention, false);
        }
        return hasAccountOwnerRole;
    }

    @Override
    public void logEmbed(final CommandContext ctx, final Embed embed) {
        CommandOutputHelper.logEmbedOutputToDiscord(embed);
    }

    @Override
    public void logMultiLine(final List<String> multiLine) {
        CommandOutputHelper.logMultiLineOutputToDiscord(multiLine);
    }
}
