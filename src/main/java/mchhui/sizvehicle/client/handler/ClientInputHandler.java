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
        float step = (time - this.lastHandleTime) / 1000f;

        // 处理键盘输入

        playerInputBrake = keyBrake.isKeyDown();
        playerInputShift = keyShift.isKeyDown();

        playerInputPowerFactor = 0;
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindForward.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            playerInputPowerFactor = +1;
        }
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindBack.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            playerInputPowerFactor -= 1;
        }
        playerInputAngleFactor = 0;
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindLeft.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            playerInputAngleFactor += 1;
        }
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindRight.getKeyCode()) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            playerInputAngleFactor -= 1;
        }

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
