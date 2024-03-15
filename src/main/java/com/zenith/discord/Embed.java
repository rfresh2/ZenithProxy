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

    public EmbedCreateSpec toSpec() {
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
}
