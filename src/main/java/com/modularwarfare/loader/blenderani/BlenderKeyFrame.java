package com.modularwarfare.loader.blenderani;
/**
 * @author Hueihuea
 * */
public class BlenderKeyFrame {
    public int frame;
    public Vec3 position;
    public Vec3 rotation;
    public Vec3 scale;
    
    public static class Vec3{
        public float x;
        public float y;
        public float z;
        
        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return "("+x+","+y+","+z+")";
        }
    }
}
