package com.zenith.feature.tasks;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ActionTypeAdapter extends TypeAdapter<Action> {
    @Override
    public void write(final JsonWriter writer, final Action action) throws IOException {
        writer.beginObject();
        writer.name("type");
        writer.value(action.getClass().getSimpleName());
        writer.name("data");
        switch (action) {
            case CommandAction cmd -> {
                writer.beginObject();
                writer.name("command");
                writer.value(cmd.getCommand());
                writer.endObject();
            }
            default -> {
                writer.nullValue();
            }
        }
        writer.endObject();
    }

    @Override
    public Action read(final JsonReader reader) throws IOException {
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
        Action action = null;
        switch (typeValue) {
            case "CommandAction" -> {
                reader.beginObject();
                var commandField = reader.nextName();
                if (!commandField.equals("command")) {
                    throw new IOException("Invalid CommandAction field: " + commandField);
                }
                String command = reader.nextString();
                reader.endObject();
                action = new CommandAction(command);
            }
            default -> {
                reader.skipValue();
            }
        }
        reader.endObject();
        return action;
    }
}
