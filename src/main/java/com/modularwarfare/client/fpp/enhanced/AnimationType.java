package com.modularwarfare.client.fpp.enhanced;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

@JsonAdapter(AnimationType.AnimationTypeJsonAdapter.class)
public enum AnimationType {
    DEFAULT("default"),
    DEFAULT_EMPTY("defaultEmpty"),
    DRAW("draw"),
    DRAW_EMPTY("drawEmpty"),
    TAKEDOWN("takedown"),
    TAKEDOWN_EMPTY("takedownEmpty"),
    AIM("aim"),
    INSPECT("inspect"),
    INSPECT_EMPTY("inspectEmpty"),
    PRE_LOAD("preLoad"),
    LOAD("load"),
    POST_LOAD("postLoad"),
    PRE_UNLOAD("preUnload"),
    UNLOAD("unload"),
    POST_UNLOAD("postUnload"),
    PRE_RELOAD("preReload"),
    PRE_RELOAD_EMPTY("preReloadEmpty"),
    RELOAD_FIRST("reloadFirst"),
    RELOAD_FIRST_EMPTY("reloadFirstEmpty"),
    RELOAD_SECOND("reloadSecond"),
    RELOAD_SECOND_EMPTY("reloadSecondEmpty"),
    RELOAD_FIRST_QUICKLY("reloadFirstQuickly"),
    RELOAD_SECOND_QUICKLY("reloadSecondQuickly"),
    POST_RELOAD("postReload"),
    POST_RELOAD_EMPTY("postReloadEmpty"),
    PRE_FIRE("preFire"),
    FIRE("fire"),
    FIRE_LAST("fireLast"),
    POST_FIRE("postFire"),
    POST_FIRE_EMPTY("postFireEmpty"),
    PRE_FIRE_ADS("preFireADS"),
    FIRE_ADS("fireADS"),
    FIRE_LAST_ADS("fireLastADS"),
    POST_FIRE_ADS("postFireADS"),
    POST_FIRE_ADS_EMPTY("postFireADSEmpty"),
    MODE_CHANGE("modeChange"),
    SPRINT("sprint"),
    CUSTOM("custom"),
    //以下是内部机以外的动画 可用于自定义动画
    CUSTOM1("custom1"),
    CUSTOM2("custom2"),
    CUSTOM3("custom3"),
    CUSTOM4("custom4"),
    CUSTOM5("custom5"),
    CUSTOM6("custom6"),
    CUSTOM7("custom7"),
    CUSTOM8("custom8"),
    PRIMARY_SKILL("primarySkill"),
    SECONDARY_SKILL("secondarySkill");

    public String serializedName;
    private AnimationType(String name) {
        serializedName=name;
    }
    
    public boolean showFlashModel() {
        if(this==FIRE) {
            return true;
        }
        if(this==FIRE_LAST) {
            return true;
        }
        return false;
    }
    
    public static class AnimationTypeJsonAdapter extends TypeAdapter<AnimationType>{

        @Override
        public AnimationType read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                throw new AnimationTypeException("wrong animation type format");
            }
            return fromString(in.nextString());
        }

        @Override
        public void write(JsonWriter out, AnimationType t) throws IOException {
            out.value(t.serializedName);
        }
        
        public static class AnimationTypeException extends RuntimeException{
            public AnimationTypeException(String str) {
                super(str);
            }
        }

        public static AnimationType fromString(String modeName) {
            for (AnimationType animationType : values()) {
                if (animationType.serializedName.equalsIgnoreCase(modeName)) {
                    return animationType;
                }
            }
            throw new AnimationTypeException("wrong animation type:"+modeName);
        }
        
    }
}
