package siz.addon.modularprops.client.fpp.enhanced;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 自定义方块动画类型枚举
 */
@JsonAdapter(AnimationCustomBlockType.AnimationTypeJsonAdapter.class)
public enum AnimationCustomBlockType {
    // 第一人称动画
    DEFAULT("default"),
    DRAW("draw"),
    SPRINT("sprint"),
    INSPECT("inspect"),
    USE("use"),
    ATTACK("attack"),
    PLACE("place"),
    // 第三人称动画
    THIRD_DEFAULT("thirdDefault"),
    THIRD_PLACE("thirdPlace"),
    BREAK("break"),
    // 自定义动画
    CUSTOM1("custom1"),
    CUSTOM2("custom2"),
    CUSTOM3("custom3"),
    CUSTOM4("custom4"),
    CUSTOM5("custom5");

    public String serializedName;

    private AnimationCustomBlockType(String name) {
        serializedName = name;
    }

    public static class AnimationTypeJsonAdapter extends TypeAdapter<AnimationCustomBlockType> {

        public static AnimationCustomBlockType fromString(String modeName) {
            for (AnimationCustomBlockType animationType : values()) {
                if (animationType.serializedName.equalsIgnoreCase(modeName)) {
                    return animationType;
                }
            }
            throw new AnimationTypeException("wrong animation type: " + modeName);
        }

        @Override
        public AnimationCustomBlockType read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                throw new AnimationTypeException("wrong animation type format");
            }
            return fromString(in.nextString());
        }

        @Override
        public void write(JsonWriter out, AnimationCustomBlockType t) throws IOException {
            out.value(t.serializedName);
        }

        public static class AnimationTypeException extends RuntimeException {
            public AnimationTypeException(String str) {
                super(str);
            }
        }
    }
}

