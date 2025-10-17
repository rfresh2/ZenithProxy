package com.zenith.feature.tasks;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ContinuationTypeAdapter extends TypeAdapter<Continuation> {
    @Override
    public void write(final JsonWriter writer, final Continuation continuation) throws IOException {
        writer.beginObject();
        writer.name("type");
        writer.value(continuation.getClass().getSimpleName());
        writer.name("data");
        switch (continuation) {
            case DurationContinuation dur -> {
                writer.beginObject();
                writer.name("endTimeEpochMs");
                writer.value(dur.getEndTimeEpochMs());
                writer.endObject();
            }
            case ForeverContinuation forever -> {
                writer.beginObject();
                writer.endObject();
            }
            case NContinuation ncont -> {
                writer.beginObject();
                writer.name("n");
                writer.value(ncont.getN());
                writer.name("count");
                writer.value(ncont.getCount());
                writer.endObject();
            }
            case OnceContinuation once -> {
                writer.beginObject();
                writer.endObject();
            }
            default -> {
                writer.nullValue();
            }
        }
        writer.endObject();
    }

    @Override
    public Continuation read(final JsonReader reader) throws IOException {
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
        Continuation continuation = null;
        switch (typeValue) {
            case "DurationContinuation" -> {
                reader.beginObject();
                var endTimeField = reader.nextName();
                if (!endTimeField.equals("endTimeEpochMs")) {
                    return null;
                }
                long endTimeEpochMs = reader.nextLong();
                reader.endObject();
                continuation = new DurationContinuation(endTimeEpochMs);
            }
            case "ForeverContinuation" -> {
                reader.beginObject();
                reader.endObject();
                continuation = new ForeverContinuation();
            }
            case "NContinuation" -> {
                reader.beginObject();
                var nField = reader.nextName();
                if (!nField.equals("n")) {
                    return null;
                }
                int n = reader.nextInt();
                var countField = reader.nextName();
                if (!countField.equals("count")) {
                    return null;
                }
                int count = reader.nextInt();
                reader.endObject();
                continuation = new NContinuation(n, count);
            }
            case "OnceContinuation" -> {
                reader.beginObject();
                reader.endObject();
                continuation = new OnceContinuation();
            }
            default -> {
                reader.skipValue();
            }
        }
        reader.endObject();
        return continuation;
    }
}
