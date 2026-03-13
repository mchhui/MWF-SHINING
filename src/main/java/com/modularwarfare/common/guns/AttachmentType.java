package com.modularwarfare.common.guns;

import java.util.ArrayList;
import java.util.List;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.common.textures.TextureEnumType;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.common.type.BaseType;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;


public class AttachmentType extends BaseType {

    public AttachmentPresetEnum attachmentType;

    public Grip grip = new Grip();

    public Barrel barrel = new Barrel();

    public Sight sight = new Sight();
    
    public Stock stock = new Stock();

    public Laser laser = new Laser();

    public Pistolgrip pistolgrip = new Pistolgrip();
    
    public Handguard handguard = new Handguard();

    public boolean sameTextureAsGun = false;
    


    @Override
    public void loadExtraValues() {
        if (maxStackSize == null)
            maxStackSize = 1;

        loadBaseValues();

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            if (sight.customOverlayTextures != null && sight.customOverlayTextures.length > 0) {
                sight.overlayTypes.clear();
                for (String textureName : sight.customOverlayTextures) {
                    if (ModularWarfare.textureTypes.containsKey(textureName)) {
                        sight.overlayTypes.add(ModularWarfare.textureTypes.get(textureName));
                    } else {
                        TextureType defaultType = new TextureType();
                        defaultType.initDefaultTextures(TextureEnumType.Overlay);
                        sight.overlayTypes.add(defaultType);
                    }
                }
            } else {
                if (sight.customOverlayTexture != null) {
                    if (ModularWarfare.textureTypes.containsKey(sight.customOverlayTexture)) {
                        sight.overlayType = ModularWarfare.textureTypes.get(sight.customOverlayTexture);
                    }
                } else {
                    sight.overlayType = new TextureType();
                    sight.overlayType.initDefaultTextures(TextureEnumType.Overlay);
                }
                
                if (sight.overlayType != null) {
                    sight.overlayTypes.add(sight.overlayType);
                }
            }
            
            if (sight.customOverlayUnclippedTextures != null && sight.customOverlayUnclippedTextures.length > 0) {
                sight.overlayUnclippedTypes.clear();
                for (String textureName : sight.customOverlayUnclippedTextures) {
                    if (ModularWarfare.textureTypes.containsKey(textureName)) {
                        sight.overlayUnclippedTypes.add(ModularWarfare.textureTypes.get(textureName));
                    } else {
                        TextureType defaultType = new TextureType();
                        defaultType.initDefaultTextures(TextureEnumType.Overlay);
                        sight.overlayUnclippedTypes.add(defaultType);
                    }
                }
            }
        }
        loadWeaponSoundMap();
    }

    @Override
    public void reloadModel() {
        model = new ModelAttachment(ModularWarfare.getRenderConfig(this, AttachmentRenderConfig.class), this);
        ((ModelAttachment)model).config.init();
    }

    @Override
    public String getAssetDir() {
        return "attachments";
    }

    public static class Sight {
        //public WeaponScopeType scopeType = WeaponScopeType.DEFAULT;
        public WeaponDotColorType dotColorType = WeaponDotColorType.RED;
        public WeaponScopeModeType modeType = WeaponScopeModeType.NORMAL;

        public String customOverlayTexture;
        public transient TextureType overlayType;
        
        public String[] customOverlayTextures;
        public transient List<TextureType> overlayTypes = new ArrayList<>();
        
        public String[] customOverlayUnclippedTextures;
        public transient List<TextureType> overlayUnclippedTypes = new ArrayList<>();
        
        public boolean plumbCrossHair = false;
        
        public boolean usedDefaultOverlayModelTexture=true;

        public float aimSpeedFactor = 1.0f;
    }

    public static class Barrel {
        public boolean isSuppressor;
        public boolean hideFlash;

        public float recoilPitchFactor = 1.0f;
        public float recoilYawFactor = 1.0f;
        
    	public float accuracyFactor = 1.0f;
    }

    public static class Grip {
        public float recoilPitchFactor = 1.0f;
        public float recoilYawFactor = 1.0f;
    }

    public static class Pistolgrip {
        public float aimSpeedFactor = 1.0f;
        public float recoilPitchFactor = 1.0f;
        public float recoilYawFactor = 1.0f;
    }

    public static class Handguard {
        public float aimSpeedFactor = 1.0f;
        public float recoilPitchFactor = 1.0f;
        public float recoilYawFactor = 1.0f;
    }
    public static class Stock{
        public float aimSpeedFactor = 1.0f;
    	public float recoilPitchFactor = 1.0f;
        public float recoilYawFactor = 1.0f; 
        
    }

    public static class Laser {
        public float accuracyFactor = 1.0f;
        public float aimSpeedFactor = 1.0f;
        public float recoilPitchFactor = 1.0f;
        public float recoilYawFactor = 1.0f;
    }

}
