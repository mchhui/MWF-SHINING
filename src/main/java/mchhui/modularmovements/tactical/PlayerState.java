package mchhui.modularmovements.tactical;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import mchhui.modularmovements.ModularMovements;
import mchhui.modularmovements.tactical.client.ClientLitener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PlayerState {
    public boolean isSliding = false;
    public boolean isSitting = false;
    public boolean isCrawling = false;
    //0:middle -1:left 1:right
    public byte probe = 0;
    public float probeOffset = 0;
    private long lastSyncTime = 0;


    private long lastSit;
    private long lastCrawl;
    private long lastProbe;
    
    public AxisAlignedBB lastAABB;
    public AxisAlignedBB lastModAABB;

    public void updateOffset() {
        double amplifer = (System.currentTimeMillis() - lastSyncTime) * (60 / 1000d);
        lastSyncTime = System.currentTimeMillis();
        if (probe == -1) {
            if (probeOffset > -1) {
                probeOffset -= 0.1 * amplifer;
            }
            if (probeOffset < -1) {
                probeOffset = -1;
            }
        }
        if (probe == 1) {
            if (probeOffset < 1) {
                probeOffset += 0.1 * amplifer;
            }
            if (probeOffset > 1) {
                probeOffset = 1;
            }
        }

        if (probe == 0) {
            if (Math.abs(probeOffset) <= 0.1 * amplifer) {
                probeOffset = 0;
            }
            if (probeOffset < 0) {
                probeOffset += 0.1 * amplifer;
            }
            if (probeOffset > 0) {
                probeOffset -= 0.1 * amplifer;
            }
        }
    }

    public boolean isStanding() {
        return !isCrawling && !isSitting;
    }

    @SideOnly(Side.CLIENT)
    public boolean canSit() {
        return ModularMovements.REMOTE_CONFIG.sit.enable && ((System.currentTimeMillis() - lastSit) > 300);
    }

    @SideOnly(Side.CLIENT)
    public boolean canCrawl() {
        return ModularMovements.REMOTE_CONFIG.crawl.enable && ((System.currentTimeMillis() - lastCrawl) > 500);
    }

    @SideOnly(Side.CLIENT)
    public boolean canProbe() {
        return ModularMovements.REMOTE_CONFIG.lean.enable;
    }

    public void enableSit() {
        isSitting = true;
        disableCrawling();
        this.lastSit = System.currentTimeMillis();
    }

    public void disableSit() {
        isSitting = false;
        this.lastSit = System.currentTimeMillis();
    }

    public void enableCrawling() {
        isCrawling = true;
        disableSit();
        this.lastCrawl = System.currentTimeMillis();  
    }

    public void disableCrawling() {
        isCrawling = false;
        this.lastCrawl = System.currentTimeMillis();  
    }

    public void resetProbe() {
        probe = 0;
        //Minecraft.getMinecraft().player.setPrimaryHand(Minecraft.getMinecraft().gameSettings.mainHand);
    }

    public void leftProbe() {
        probe = -1;
        this.lastProbe = System.currentTimeMillis();
        //Minecraft.getMinecraft().player.setPrimaryHand(EnumHandSide.LEFT);
    }

    public void rightProbe() {
        probe = 1;
        this.lastProbe = System.currentTimeMillis();
        //Minecraft.getMinecraft().player.setPrimaryHand(EnumHandSide.RIGHT);
    }

    public void readCode(int code) {
        probe = (byte) (code % 10 - 1);
        code /= 10;
        isCrawling = code % 10 != 0;
        code /= 10;
        isSitting = code % 10 != 0;
        code /= 10;
        isSliding = code % 10 != 0;
    }

    public void reset() {
        readCode(1);
    }

    public int writeCode() {
        return (isSliding ? 1 : 0) * 1000 + (isSitting ? 1 : 0) * 100 + (isCrawling ? 1 : 0) * 10 + probe + 1;
    }

    @SideOnly(Side.CLIENT)
    public static class ClientPlayerState extends PlayerState {
        private static Field equippedProgressMainHandField;

        public ClientPlayerState() {
            if (equippedProgressMainHandField == null) {
                equippedProgressMainHandField = ReflectionHelper.findField(ItemRenderer.class,
                        "equippedProgressMainHand", "field_187469_f");
            }
        }

        private void updateEquippedItem() {
            try {
                equippedProgressMainHandField.set(Minecraft.getMinecraft().getItemRenderer(), -0.4f);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void enableCrawling() {
            super.enableCrawling();
            ClientLitener.crawlingMousePosXMove = 0;
        }

        public void resetProbe() {
            super.resetProbe();
            if (Minecraft.getMinecraft().player.getPrimaryHand() != Minecraft.getMinecraft().gameSettings.mainHand) {
                Minecraft.getMinecraft().player.setPrimaryHand(Minecraft.getMinecraft().gameSettings.mainHand);
                updateEquippedItem();
            }
        }

        public void leftProbe() {
            super.leftProbe();
            if (Minecraft.getMinecraft().player.getPrimaryHand() != EnumHandSide.LEFT) {
                Minecraft.getMinecraft().player.setPrimaryHand(EnumHandSide.LEFT);
                updateEquippedItem();
            }
        }

        public void rightProbe() {
            super.rightProbe();
            if (Minecraft.getMinecraft().player.getPrimaryHand() != EnumHandSide.RIGHT) {
                Minecraft.getMinecraft().player.setPrimaryHand(EnumHandSide.RIGHT);
                updateEquippedItem();
            }
        }
    }
}
