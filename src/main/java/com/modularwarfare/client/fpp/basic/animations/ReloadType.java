package com.modularwarfare.client.fpp.basic.animations;

public enum ReloadType {

    UNLOAD(0),
    LOAD(1),
    FULL(2);

    public int i;

    ReloadType(int i) {
        this.i = i;
    }

    public static ReloadType getTypeFromInt(int i) {
        for (ReloadType type : values()) {
            if (type.i == i) {
                return type;
            }
        }
        return null;
    }

}
