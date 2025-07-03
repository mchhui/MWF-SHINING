package mchhui.sizvehicle.client.handler;

import org.lwjgl.input.Keyboard;

import mchhui.sizvehicle.network.ClientSIZVehicle;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class ClientInputHandler {
    private float playerInputPowerFactor = 0;
    private float playerInputAngleFactor = 0;
    private boolean playerInputBrake = false;
    private long lastHandleTime;
    private float powerCharge = 0;
    private float angleCharge = 0;
    private long powerLastChargeTime;
    private long angleLastChargeTime;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        long time = System.currentTimeMillis();
        if (time - this.lastHandleTime < 5) {
            return;
        }
        float step = (time - this.lastHandleTime) / 1000f;

        // 处理键盘输入

        playerInputBrake = Keyboard.isKeyDown(Keyboard.KEY_SPACE);

        if (time - powerLastChargeTime > 10) {
            powerCharge += Keyboard.isKeyDown(Keyboard.KEY_W) ? step : 0;
            powerCharge -= Keyboard.isKeyDown(Keyboard.KEY_S) ? step : 0;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_S)) {
            powerLastChargeTime = time;
        } else if (time - powerLastChargeTime > 200) {
            if (powerCharge > 0) {
                powerCharge = (powerCharge > step) ? powerCharge - step : 0;
            }
            if (powerCharge < 0) {
                powerCharge = (powerCharge < -step) ? powerCharge + step : 0;
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_D)) {
            angleLastChargeTime = time;
        } else if (time - angleLastChargeTime > 200) {
            if (angleCharge > 0) {
                angleCharge = (angleCharge > step) ? angleCharge - step : 0;
            }
            if (angleCharge < 0) {
                angleCharge = (angleCharge < -step) ? angleCharge + step : 0;
            }
        }

        // 限制数值范围
        powerCharge = Math.max(-1, Math.min(1, powerCharge));
        angleCharge = Math.max(-1, Math.min(1, angleCharge));

        playerInputPowerFactor = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            playerInputPowerFactor = +1;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            playerInputPowerFactor -= 1;
        }
        playerInputAngleFactor = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            playerInputAngleFactor += 1;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            playerInputAngleFactor -= 1;
        }

        this.lastHandleTime = time;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.START) {
            return;
        }
        ClientSIZVehicle.uploadInput(playerInputPowerFactor, playerInputAngleFactor, playerInputBrake);
    }

    public static float easeInOut(float x) {
        if (x < 0) {
            return -easeInOut(-x);
        }
        x = Math.max(0, Math.min(1, x));
        return x < 0.5f ? 2 * x * x : 1 - (float)Math.pow(-2 * x + 2, 2) / 2;
    }
}
