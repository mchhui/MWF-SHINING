package com.modularwarfare.loader.blenderani;

import java.util.ArrayList;
import java.util.HashMap;
/**
 * @author Hueihuea
 * */
public class BlenderAnimation {
    public HashMap<String,BlenderBone> bones=new HashMap<String,BlenderBone>();
    public int length;
    
    public void updateAnimation(double frame) {
        bones.values().forEach((bone)->{
            bone.updateAnimation(frame);
        });
    }
}
