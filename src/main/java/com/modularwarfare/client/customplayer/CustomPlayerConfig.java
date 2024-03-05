package com.modularwarfare.client.customplayer;

import java.util.HashMap;

public class CustomPlayerConfig {
    public String name;
    public String model;
    public String tex;
    public HashMap<String, Animation> animations;
    public int FPS=24;

    public static class Animation {
        public double startTime = 0;
        public double endTime = 1;
        public double speed = 1;
        
        public double getStartTime(double FPS) {
            return startTime * 1/FPS;
        }

        public double getEndTime(double FPS) {
            return endTime * 1/FPS;
        }
        
        public double getSpeed(double FPS) {
            double a=(getEndTime(FPS)-getStartTime(FPS));
            if(a<=0) {
                a=1;
            }
            return speed/a;
        }
    }
}
