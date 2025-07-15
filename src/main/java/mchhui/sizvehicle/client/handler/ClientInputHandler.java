package mchhui.sizvehicle.client.handler;

import org.lwjgl.input.Keyboard;

import mchhui.sizvehicle.network.ClientSIZVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class ClientInputHandler {
    private static KeyBinding keyShift = new KeyBinding("key.sizvehicle.shift", Keyboard.KEY_SPACE, "key.categories.sizvehicle");
    private static KeyBinding keyBrake = new KeyBinding("key.sizvehicle.brake", Keyboard.KEY_LSHIFT, "key.categories.sizvehicle");
    private float playerInputPowerFactor = 0;
    private float playerInputAngleFactor = 0;
    private boolean playerInputBrake = false;
    private boolean playerInputShift = false;
    private long lastHandleTime;
    
    private long leftKeyPressTime = 0;
    private long rightKeyPressTime = 0;
    private long leftKeyReleaseTime = 0;
    private long rightKeyReleaseTime = 0;
    private boolean leftKeyPressed = false;
    private boolean rightKeyPressed = false;
    private float targetAngleFactor = 0;
    private float lastActiveAngleFactor = 0;
    
    private static final float STEERING_RAMP_UP_TIME = 300f;
    private static final float STEERING_DECAY_TIME = 150f;
    private static final float MAX_STEERING_FACTOR = 1.0f;
    private static final float STEERING_SMOOTHING = 0.15f;

    public ClientInputHandler() {
        ClientRegistry.registerKeyBinding(keyShift);
        ClientRegistry.registerKeyBinding(keyBrake);
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        long time = System.currentTimeMillis();
        if (time - this.lastHandleTime < 5) {
            return;
        }

        playerInputBrake = keyBrake.isKeyDown();
        playerInputShift = keyShift.isKeyDown();

        playerInputPowerFactor = 0;
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindForward.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            playerInputPowerFactor = +1;
        }
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindBack.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            playerInputPowerFactor -= 1;
        }
        
        boolean leftPressed = Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindLeft.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_LEFT);
        boolean rightPressed = Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindRight.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
        
        if (leftPressed && !leftKeyPressed) {
            leftKeyPressTime = time;
            leftKeyPressed = true;
        } else if (!leftPressed && leftKeyPressed) {
            leftKeyReleaseTime = time;
            lastActiveAngleFactor = playerInputAngleFactor;
            leftKeyPressed = false;
        }
        
        if (rightPressed && !rightKeyPressed) {
            rightKeyPressTime = time;
            rightKeyPressed = true;
        } else if (!rightPressed && rightKeyPressed) {
            rightKeyReleaseTime = time;
            lastActiveAngleFactor = playerInputAngleFactor;
            rightKeyPressed = false;
        }
        
        if (leftPressed && rightPressed) {
            targetAngleFactor = 0;
        } else if (leftPressed) {
            float holdTime = time - leftKeyPressTime;
            float strength = Math.min(holdTime / STEERING_RAMP_UP_TIME, 1.0f);
            targetAngleFactor = strength * MAX_STEERING_FACTOR;
        } else if (rightPressed) {
            float holdTime = time - rightKeyPressTime;
            float strength = Math.min(holdTime / STEERING_RAMP_UP_TIME, 1.0f);
            targetAngleFactor = -strength * MAX_STEERING_FACTOR;
        } else {
            // 计算基于时间的衰减
            long releaseTime = Math.max(leftKeyReleaseTime, rightKeyReleaseTime);
            float decayTime = time - releaseTime;
            float decayProgress = Math.min(decayTime / STEERING_DECAY_TIME, 1.0f);
            targetAngleFactor = lastActiveAngleFactor * (1.0f - decayProgress);
        }
        
        float diff = targetAngleFactor - playerInputAngleFactor;
        if (Math.abs(diff) > 0.001f) {
            float smoothingRate = STEERING_SMOOTHING;
            if (Math.abs(targetAngleFactor) < 0.01f) {
                smoothingRate = STEERING_SMOOTHING * 2.0f;
            }
            playerInputAngleFactor += diff * smoothingRate;
        } else {
            playerInputAngleFactor = targetAngleFactor;
        }
        
        playerInputAngleFactor = Math.max(-MAX_STEERING_FACTOR, Math.min(MAX_STEERING_FACTOR, playerInputAngleFactor));

        this.lastHandleTime = time;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.START) {
            return;
        }
        ClientSIZVehicle.uploadInput(playerInputPowerFactor, playerInputAngleFactor, playerInputBrake, playerInputShift);
    }
}
