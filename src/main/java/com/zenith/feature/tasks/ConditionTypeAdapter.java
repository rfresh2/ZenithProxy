package com.zenith.feature.tasks;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.zenith.Globals.DEFAULT_LOG;

public class ConditionTypeAdapter extends TypeAdapter<Condition> {
    @Override
    public void write(final JsonWriter writer, final Condition condition) throws IOException {
        writer.beginObject();
        writer.name("type");
        writer.value(condition.getClass().getSimpleName());
        writer.name("data");
        switch (condition) {
            case EventCondition event -> {
                writer.beginObject();
                writer.name("eventName");
                var eventClass = event.getEvent();
                writer.value(eventClass.getName());
                writer.endObject();
            }
            case IntervalCondition interval -> {
                writer.beginObject();
                writer.name("startTime");
                writer.value(interval.getStartTime().toString());
                writer.name("interval");
                writer.value(interval.getInterval().toString());
                writer.endObject();
            }
            default -> {
                writer.nullValue();
            }
        }
        writer.endObject();
    }

    @Override
    public Condition read(final JsonReader reader) throws IOException {
        reader.beginObject();
        var typeName = reader.nextName();
        if (!typeName.equals("type")) {
            return null;
        }
        var typeValue = reader.nextString();
        var dataFieldName = reader.nextName();
        if (!dataFieldName.equals("data")) {
            return null;
        }
        Condition condition = null;
        switch (typeValue) {
            case "EventCondition" -> {
                reader.beginObject();
                var eventField = reader.nextName();
                if (!eventField.equals("eventName")) {
                    return null;
                }
                String eventClassName = reader.nextString();
                try {
                    Class<?> eventClass = Class.forName(eventClassName);
                    reader.endObject();
                    condition = new EventCondition(eventClass);
                } catch (Throwable e) {
                    reader.endObject();
                    DEFAULT_LOG.error("Unable to find event class: {} in EventCondition deserializer", eventClassName);
                }
            }
            case "IntervalCondition" -> {
                reader.beginObject();
                var startTimeField = reader.nextName();
                if (!startTimeField.equals("startTime")) {
                    return null;
                }
                String startTimeString = reader.nextString();
                Instant startTime = Instant.parse(startTimeString);
                var intervalField = reader.nextName();
                if (!intervalField.equals("interval")) {
                    return null;
                }
                String intervalString = reader.nextString();
                var interval = Duration.parse(intervalString);
                reader.endObject();
                condition = new IntervalCondition(startTime, interval);
            }
            default -> {
                reader.skipValue();
            }
        }
        reader.endObject();
        return condition;
    }
}
