package com.modularwarfare.client.fpp.enhanced.configs;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@JsonAdapter(RenderType.RenderTypeJsonAdapter.class)
public enum RenderType {
    PLAYER("player"), ITEMLOOT("itemloot"), ITEMFRAME("itemframe");

    public String serializedName;

    private RenderType(String serializedName) {
        this.serializedName = serializedName;
    }

    public static class RenderTypeJsonAdapter extends TypeAdapter<RenderType> {

        @Override
        public void write(JsonWriter out, RenderType value) throws IOException {
            out.value(value.serializedName);
        }

        @Override
        public RenderType read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                throw new RuntimeException("wrong render type format");
            }
            String name = in.nextString();
            for (RenderType t : values()) {
                if (name.equals(t.serializedName)) {
                    return t;
                }
            }
            throw new RuntimeException("wrong render type:" + name);
        }

    }
}
