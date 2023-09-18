package com.modularwarfare.client.fpp.basic.animations.anims;

import com.modularwarfare.api.WeaponAnimation;
import com.modularwarfare.client.fpp.basic.animations.AnimStateMachine;
import com.modularwarfare.client.fpp.basic.animations.ReloadType;
import com.modularwarfare.client.fpp.basic.animations.StateEntry;
import com.modularwarfare.client.fpp.basic.animations.StateEntry.MathType;
import com.modularwarfare.client.fpp.basic.animations.StateType;
import com.modularwarfare.client.model.ModelGun;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

public class AnimationTopRifle extends WeaponAnimation {

    public AnimationTopRifle() {
        ammoLoadOffset = new Vector3f(0, -1.5F, 0);
    }

    @Override
    public void onGunAnimation(float tiltProgress, AnimStateMachine animation) {
        //Translate X - Forwards/Backwards
        GL11.glTranslatef(0.0F * tiltProgress, 0F, 0F);
        //Translate Y - Up/Down
        GL11.glTranslatef(0F, 0.0F * tiltProgress, 0F);
        //Translate Z - Left/Right
        GL11.glTranslatef(0F, 0F, -0.2F * tiltProgress);
        //Rotate X axis - Rolls Left/Right
        GL11.glRotatef(10F * tiltProgress, 1F, 0F, 0F);
        //Rotate Y axis - Angle Left/Right
        GL11.glRotatef(-10F * tiltProgress, 0F, 1F, 0F);
        //Rotate Z axis - Angle Up/Down
        GL11.glRotatef(15F * tiltProgress, 0F, 0F, 1F);
    }

    @Override
    public void onAmmoAnimation(ModelGun gunModel, float ammoProgress, int reloadAmmoCount, AnimStateMachine animation) {
        //System.out.println(ammoProgress);
        float multiAmmoPosition = ammoProgress * 1;
        int bulletNum = MathHelper.floor(multiAmmoPosition);
        float bulletProgress = multiAmmoPosition - bulletNum;
        float modelScale = gunModel.config.extra.modelScale;

        GL11.glRotatef(bulletProgress * 55F, 0F, 1F, 0F);
        GL11.glRotatef(bulletProgress * 95F, 0F, 0F, 1F);
        GL11.glTranslatef(bulletProgress * -0.1F * 1 / modelScale, bulletProgress * 1F * 1 / modelScale, bulletProgress * 0.5F * 1 / modelScale);
    }

    @Override
    public ArrayList<StateEntry> getReloadStates(ReloadType reloadType, int reloadCount) {
        ArrayList<StateEntry> states = new ArrayList<>();
        states.add(new StateEntry(StateType.TILT, 0.15f, 0f, MathType.ADD));
        if (reloadType == ReloadType.UNLOAD || reloadType == ReloadType.FULL)
            states.add(new StateEntry(StateType.UNLOAD, 0.35f, 0f, MathType.ADD));
        if (reloadType == ReloadType.LOAD || reloadType == ReloadType.FULL)
            states.add(new StateEntry(StateType.LOAD, 0.35f, 1f, MathType.SUB, reloadCount));
        states.add(new StateEntry(StateType.UNTILT, 0.15f, 1f, MathType.SUB));
        return states;
    }

}
