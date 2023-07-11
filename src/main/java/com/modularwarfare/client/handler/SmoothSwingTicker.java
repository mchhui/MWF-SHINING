package com.modularwarfare.client.handler;

import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;

import java.util.Timer;
import java.util.TimerTask;

public class SmoothSwingTicker extends TimerTask {

    private final static double amountOfTicks = 60.0;
    private long lastTime = System.nanoTime();
    private double frameDelta = 0;

    @Override
    public void run() {
        long nowNano = System.nanoTime();
        double nanoPerFrame = 1000000000.0 / amountOfTicks;
        frameDelta += (nowNano - lastTime) / nanoPerFrame;
        lastTime = nowNano;
        int frameDeltaCount = (int)frameDelta;
        RenderParameters.SMOOTH_SWING += frameDeltaCount;
        frameDelta -= frameDeltaCount;
    }

    public static void startSmoothSwingTimer() {
        Timer timer = new Timer("SmoothSwingThread");
        TimerTask task = new SmoothSwingTicker();
        timer.schedule(task, 0, (int)(1000 / amountOfTicks));
    }
}
