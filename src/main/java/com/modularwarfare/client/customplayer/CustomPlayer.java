package com.modularwarfare.client.customplayer;

import java.util.HashMap;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.customplayer.CustomPlayerConfig.Animation;

import mchhui.hegltf.GltfDataModel;
import mchhui.hegltf.GltfRenderModel;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class CustomPlayer {
    private static HashMap<String, GltfRenderModel> models = new HashMap<String, GltfRenderModel>();
    private CustomPlayerConfig config;

    private float IDLE;
    private float WALK;
    private float SPRINT;
    private float LEAN_RIGHT;
    private float LEAN_LEFT;
    private float ATTACK_IDLE;
    private float GUN_IDLE;
    private float SNEAK_IDLE;
    private float CRAWL_IDLE;
    private float SNEAK;
    private float CRAWL;
    private float SLIDE;
    private float DRAW;
    private float AIM;
    private float SHOT;
    private float RELOAD;
    private float SWING;

    private long lastSyncTime = 0;

    private static HashMap<String, CustomPlayer> playerMap = new HashMap<String, CustomPlayer>();

    public static CustomPlayer getCustomPlayer(String name) {
        CustomPlayer player = playerMap.get(name);
        if (player == null) {
            player = new CustomPlayer();
            playerMap.put(name, player);
        }
        return player;
    }

    public void bind(CustomPlayerConfig config) {
        this.config = config;
    }

    public boolean updateAnimation(GltfRenderModel model, EntityPlayer player, float ptick) {
        if (config == null) {
            return false;
        }
        if (lastSyncTime == 0) {
            lastSyncTime = System.currentTimeMillis();
            return false;
        }
        // config.FPS = 60;
        long time = System.currentTimeMillis();
        float stepTick = (time - lastSyncTime) / (1000f / 60);
        Animation ani = config.animations.get("default");
        IDLE += ani.getSpeed(config.FPS) * stepTick;
        if (IDLE > 1) {
            IDLE = 0;
        }
        // System.out.println(IDLE);

        ani = config.animations.get("walk");
        if (WALK == 1) {
            WALK = 0;
        }
        if (player.distanceWalkedModified - player.prevDistanceWalkedModified > 0.1f) {
            WALK += ani.getSpeed(config.FPS) * stepTick;
        } else {
            WALK = 0;
        }
        if (WALK > 1) {
            WALK = 1;
        }

        ani = config.animations.get("sprint");
        if (SPRINT == 1) {
            SPRINT = 0;
        }
        if (player.distanceWalkedModified != player.prevDistanceWalkedModified && player.isSprinting()) {
            SPRINT += ani.getSpeed(config.FPS) * stepTick;
            WALK = 0;
        } else {
            SPRINT = 0;
        }
        if (SPRINT > 1) {
            SPRINT = 1;
        }

        ani = config.animations.get("sneak_idle");
        if (SNEAK_IDLE == 1) {
            SNEAK_IDLE = 0;
        }
        if (player.isSneaking()) {
            SNEAK_IDLE += ani.getSpeed(config.FPS) * stepTick;
        } else {
            SNEAK_IDLE = 0;
        }
        if (SNEAK_IDLE > 1) {
            SNEAK_IDLE = 1;
        }

        ani = config.animations.get("sneak");
        if (SNEAK == 1) {
            SNEAK = 0;
        }
        if ((player.posX != player.prevPosX || player.posZ != player.prevPosZ) && player.isSneaking()) {
            SNEAK += ani.getSpeed(config.FPS) * stepTick;
        } else {
            SNEAK = 0;
        }
        if (SNEAK > 1) {
            SNEAK = 1;
        }
        double frame = 0;
        if (SNEAK > 0) {
            ani = config.animations.get("sneak");
            frame = ani.getStartTime(config.FPS) + SNEAK * (ani.getEndTime(config.FPS) - ani.getStartTime(config.FPS));
        } else if (SNEAK_IDLE > 0) {
            ani = config.animations.get("sneak_idle");
            frame =
                ani.getStartTime(config.FPS) + SNEAK_IDLE * (ani.getEndTime(config.FPS) - ani.getStartTime(config.FPS));
        } else if (SPRINT > 0) {
            ani = config.animations.get("sprint");
            frame = ani.getStartTime(config.FPS) + SPRINT * (ani.getEndTime(config.FPS) - ani.getStartTime(config.FPS));
        } else if (WALK > 0) {
            ani = config.animations.get("walk");
            frame = ani.getStartTime(config.FPS) + WALK * (ani.getEndTime(config.FPS) - ani.getStartTime(config.FPS));
        } else {
            ani = config.animations.get("default");
            frame = ani.getStartTime(config.FPS) + IDLE * (ani.getEndTime(config.FPS) - ani.getStartTime(config.FPS));
        }
        model.updateAnimation((float)frame, true);
        lastSyncTime = time;
        return true;
    }

    public void render(EntityPlayer player, float ptick) {
        if (config == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager()
            .bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "skins/customplayer/" + config.tex));
        GltfRenderModel model = models.get(config.model);
        if (model == null) {
            model = new GltfRenderModel(
                GltfDataModel.load(new ResourceLocation(ModularWarfare.MOD_ID, "gltf/customplayer/" + config.model)));
            models.put(config.model, model);
        }
        if (updateAnimation(model, player, ptick)) {
            model.renderAll();
        }
    }
}
