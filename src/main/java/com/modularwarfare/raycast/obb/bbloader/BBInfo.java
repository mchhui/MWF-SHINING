package com.modularwarfare.raycast.obb.bbloader;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class BBInfo {
    @SerializedName("groups")
    public ArrayList<Group> groups;
    @SerializedName("cubes")
    public ArrayList<Cube> cubes;
}
