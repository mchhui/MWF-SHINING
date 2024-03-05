package mchhui.easyeffect;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;

import java.util.ArrayList;

public class EasyEffectRenderer {
    public ArrayList<EEObject> objects = new ArrayList<EEObject>();
    public float viewEntityRenderingPosX;
    public float viewEntityRenderingPosY;
    public float viewEntityRenderingPosZ;
    public float viewEntityRenderingYaw;
    public float viewEntityRenderingPitch;

    public void render(float partialTicks) {
        long time = System.currentTimeMillis();
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        viewEntityRenderingPosX = (float) (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks);
        viewEntityRenderingPosY = (float) (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks);
        viewEntityRenderingPosZ = (float) (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float roll = 0.0F;
        if (entity instanceof EntityAnimal) {
            EntityAnimal entityanimal = (EntityAnimal) entity;
            yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
        }
        viewEntityRenderingYaw = yaw;
        viewEntityRenderingPitch = pitch;

        for (int i = 0; i < objects.size(); i++) {
            EEObject obj = objects.get(i);
            if (obj !=null){
                obj.render(this, time, partialTicks);
                if (obj.isShutdown(time)) {
                objects.remove(i);
                i--;
                }
            }
        }
    }
}
