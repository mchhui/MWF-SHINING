package com.modularwarfare.common.guns;


import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public enum AttachmentPresetEnum {

    @SerializedName("sight") Sight("sight"),
    @SerializedName("slide") Slide("slide"),
    @SerializedName("grip") Grip("grip"),
    @SerializedName("flashlight") Flashlight("flashlight"),
    @SerializedName("charm") Charm("charm"),
    @SerializedName("skin") Skin("skin"),
    @SerializedName("barrel") Barrel("barrel"),
    @SerializedName("stock") Stock("stock");

    public String typeName;

    AttachmentPresetEnum(String typeName) {
        this.typeName = typeName;
    }

    public static AttachmentPresetEnum getAttachment(String typeName) {
        return Arrays.stream(values())
                .filter(attachmentEnum -> attachmentEnum.typeName.equalsIgnoreCase(typeName))
                .findFirst()
                .orElse(AttachmentPresetEnum.Sight);
    }

    public String getName() {
        return this.typeName;
    }

}
