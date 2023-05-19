package com.modularwarfare.raycast.obb.bbloader;

import com.google.gson.annotations.SerializedName;

public class Group {
    @SerializedName("name")
    public String name;
    @SerializedName("parent")
    public String parent;
    @SerializedName("origin")
    public float[] origin;
}
