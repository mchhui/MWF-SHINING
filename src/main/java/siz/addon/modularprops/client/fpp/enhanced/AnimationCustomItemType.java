package siz.addon.modularprops.client.fpp.enhanced;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 自定义道具动画类型枚举
 */
@JsonAdapter(AnimationCustomItemType.AnimationTypeJsonAdapter.class)
public enum AnimationCustomItemType {
    DEFAULT("default"),
    DRAW("draw"),
    SPRINT("sprint"),
    INSPECT("inspect"),
    USE("use"),
    ATTACK("attack"),
    CUSTOM1("custom1"),
    CUSTOM2("custom2"),
    CUSTOM3("custom3"),
    CUSTOM4("custom4"),
    CUSTOM5("custom5"),
    
    THIRDDEFAULT("thirdDefault");

    public String serializedName;

    private AnimationCustomItemType(String name) {
        serializedName = name;
    }

    public static class AnimationTypeJsonAdapter extends TypeAdapter<AnimationCustomItemType> {

        public static AnimationCustomItemType fromString(String modeName) {
            for (AnimationCustomItemType animationType : values()) {
                if (animationType.serializedName.equalsIgnoreCase(modeName)) {
                    return animationType;
                }
            }
            throw new AnimationTypeException("wrong animation type: " + modeName);
        }

        @Override
        public AnimationCustomItemType read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                throw new AnimationTypeException("wrong animation type format");
            }
            return fromString(in.nextString());
        }

        @Override
        public void write(JsonWriter out, AnimationCustomItemType t) throws IOException {
            out.value(t.serializedName);
        }

        public static class AnimationTypeException extends RuntimeException {
            public AnimationTypeException(String str) {
                super(str);
            }
        }
    }
}

