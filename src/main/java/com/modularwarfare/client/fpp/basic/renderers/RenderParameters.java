package com.modularwarfare.client.fpp.basic.renderers;

import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;

import java.util.Arrays;
import java.util.HashSet;

public class RenderParameters {

    public static float adsSwitch = 0f;

    public static float sprintSwitch = 0f;
    public static float crouchSwitch = 0f;
    public static float reloadSwitch = 1f;
    public static float attachmentSwitch = 0f;

    public static float triggerPullSwitch;

    public static String lastModel = "";

    //Default minecraft smoothing tick
    public static float smoothing;

    public static float GUN_BALANCING_X = 0;
    public static float GUN_BALANCING_Y = 0;

    public static float GUN_CHANGE_Y = 0;

    public static float GUN_ROT_X = 0;
    public static float GUN_ROT_Y = 0;
    public static float GUN_ROT_Z = 0;

    public static float GUN_ROT_X_LAST = 0;
    public static float GUN_ROT_Y_LAST = 0;
    public static float GUN_ROT_Z_LAST = 0;

    public static float collideFrontDistance;

    // Recoil variables
    /**
     * The recoil applied to the player view by shooting
     */
    public static float playerRecoilPitch;
    public static float playerRecoilYaw;
    public static float prevPitch = 0;

    public static float rate;
    public static boolean phase;


    /**
     * The amount of compensation applied to recoil in order to bring it back to normal
     */
    public static float antiRecoilPitch;
    public static float antiRecoilYaw;

    /**
     * SWAY
     **/
    public static float SMOOTH_SWING;
    public static float VAL;
    public static float VAL2;
    public static float VALROT;
    public static float VALSPRINT;
    public static float VALSPRINT2;

    public static float CROSS_ROTATE = 0;

    public static HashSet<String> partsWithAmmo = new HashSet<>(Arrays.asList("flashModel", "leftArmModel", "leftArmLayerModel", "leftArmSlimModel", "leftArmLayerSlimModel", "rightArmModel", "rightArmLayerModel", "rightArmSlimModel", "rightArmLayerSlimModel"));
    public static HashSet<String> partsWithoutAmmo = new HashSet<>(Arrays.asList("flashModel", "leftArmModel", "leftArmLayerModel", "leftArmSlimModel", "leftArmLayerSlimModel", "rightArmModel", "rightArmLayerModel", "rightArmSlimModel", "rightArmLayerSlimModel", "ammoModel"));

    public RenderParameters() {
        for (int i = 0; i < RenderGunEnhanced.BULLET_MAX_RENDER; i++) {
            partsWithAmmo.add("bulletModel_" + i);
        }
        for (int i = 0; i < RenderGunEnhanced.BULLET_MAX_RENDER; i++) {
            partsWithoutAmmo.add("bulletModel_" + i);
        }
    }

    // Resets render modifiers
    public static void resetRenderMods() {
        RenderParameters.reloadSwitch = 0f;
        RenderParameters.sprintSwitch = 0f;
        RenderParameters.adsSwitch = 0f;
        RenderParameters.crouchSwitch = 0f;
        ClientRenderHooks.isAimingScope = false;
        ClientRenderHooks.isAiming = false;
    }

}
