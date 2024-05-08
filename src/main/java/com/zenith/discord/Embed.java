package com.zenith.discord;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Color;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DISCORD_LOG;

/**
 * Mutable data class for discord embeds
 * Basically shadows EmbedCreateSpec while adding mutability
 */
@Data
@Accessors(chain = true, fluent = true)
public class Embed {
    @Nullable String title;
    @Nullable String description;
    @Nullable String url;
    @Nullable Instant timestamp;
    @Nullable Color color;
    @Nullable String image;
    @Nullable String thumbnail;
    @Nullable EmbedCreateFields.Footer footer;
    @Nullable EmbedCreateFields.Author author;
    @NonNull List<EmbedCreateFields.Field> fields = new ArrayList<>();
    @Nullable FileAttachment fileAttachment;

    public boolean isTitlePresent() {
        return title != null;
    }

    public boolean isColorPresent() {
        return color != null;
    }

    public boolean isDescriptionPresent() {
        return description != null;
    }

    public boolean isUrlPresent() {
        return url != null;
    }

    public Embed addField(String name, String value, boolean inline) {
        fields.add(EmbedCreateFields.Field.of(name, value, inline));
        return this;
    }

    public Embed addField(String name, Object value, boolean inline) {
        fields.add(EmbedCreateFields.Field.of(name, String.valueOf(value), inline));
        return this;
    }

    public Embed footer(String text, String iconUrl) {
        footer = EmbedCreateFields.Footer.of(text, iconUrl);
        return this;
    }

    public Embed primaryColor() {
        color = CONFIG.theme.primary.discord();
        return this;
    }

    public Embed errorColor() {
        color = CONFIG.theme.error.discord();
        return this;
    }

    public Embed successColor() {
        color = CONFIG.theme.success.discord();
        return this;
    }

    public Embed inQueueColor() {
        color = CONFIG.theme.inQueue.discord();
        return this;
    }

    public EmbedCreateSpec toSpec() {
        if (!validateEmbed(this)) {
            return EmbedCreateSpec.builder().build();
        }
        return EmbedCreateSpec.builder()
            .title(title == null ? Possible.absent() : Possible.of(title))
            .description(description == null ? Possible.absent() : Possible.of(description))
            .url(url == null ? Possible.absent() : Possible.of(url))
            .timestamp(timestamp == null ? Possible.absent() : Possible.of(timestamp))
            .color(color == null ? Possible.absent() : Possible.of(color))
            .image(image == null ? Possible.absent() : Possible.of(image))
            .thumbnail(thumbnail == null ? Possible.absent() : Possible.of(thumbnail))
            .footer(footer)
            .author(author)
            .fields(fields)
            .build();
    }

    public static Embed fromSpec(EmbedCreateSpec spec) {
        return new Embed()
            .title(spec.title().isAbsent() ? null : spec.title().get())
            .description(spec.description().isAbsent() ? null : spec.description().get())
            .url(spec.url().isAbsent() ? null : spec.url().get())
            .timestamp(spec.timestamp().isAbsent() ? null : spec.timestamp().get())
            .color(spec.color().isAbsent() ? null : spec.color().get())
            .image(spec.image().isAbsent() ? null : spec.image().get())
            .thumbnail(spec.thumbnail().isAbsent() ? null : spec.thumbnail().get())
            .footer(spec.footer())
            .author(spec.author())
            .fields(spec.fields());
    }

    public static Embed builder() {
        return new Embed();
    }

    public record FileAttachment(
        String name,
        byte[] data
    ) { }

    private boolean validateEmbed(Embed embed) {
        int charCount = 0;
        if (embed.isTitlePresent()) {
            charCount += embed.title().length();
            if (embed.title().length() > 256) {
                DISCORD_LOG.error("Embed title exceeds 256 characters: {}", embed.title());
                return false;
            }
        }
        if (embed.isDescriptionPresent()) {
            charCount += embed.description().length();
            if (embed.description().length() > 4096) {
                DISCORD_LOG.error("Embed description exceeds 4096 characters: {}", embed.description());
                return false;
            }
        }
        if (embed.fields().size() > 25) {
            DISCORD_LOG.error("Embed contains more than 25 fields");
            return false;
        }
        for (int i = 0; i < embed.fields().size(); i++) {
            var field = embed.fields().get(i);
            if (field.name().length() > 256) {
                DISCORD_LOG.error("Embed field name exceeds 256 characters: {}", field.name());
                return false;
            }
            if (field.value().length() > 1024) {
                DISCORD_LOG.error("Embed field value exceeds 1024 characters: {}", field.value());
                return false;
            }
            charCount += field.name().length() + field.value().length();
        }
        if (embed.footer() != null) {
            if (embed.footer().text().length() > 2048) {
                DISCORD_LOG.error("Embed footer text exceeds 2048 characters: {}", embed.footer().text());
                return false;
            }
            charCount += embed.footer().text().length();
        }
        if (embed.author() != null) {
            if (embed.author().name().length() > 256) {
                DISCORD_LOG.error("Embed author name exceeds 256 characters: {}", embed.author().name());
                return false;
            }
            charCount += embed.author().name().length();
        }
        if (charCount > 6000) {
            DISCORD_LOG.error("Embed character count exceeds 6000 characters");
            return false;
        }
        return true;
    }
}
