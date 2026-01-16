package com.modularwarfare.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.GenerateJsonModelsEvent;
import com.modularwarfare.api.HandleKeyEvent;
import com.modularwarfare.api.OnTickRenderEvent;
import com.modularwarfare.api.RenderHeldItemLayerEvent;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationMeleeType;
import com.modularwarfare.client.fpp.enhanced.animation.melee.AnimationMeleeController;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.MeleeRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderMelee;
import com.modularwarfare.client.input.KeyType;
import com.modularwarfare.client.objloader.api.model.ObjModelRenderer;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.common.melee.MeleeType.AnimationInfo;
import com.modularwarfare.common.network.PacketSwing;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import static com.modularwarfare.ModularWarfare.isLoadedModularMovements;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.HashSet;

public class ClientEventsMelee {

    public static float cemeraBobbing=0f;

    @SubscribeEvent
    public void onHandleKey(HandleKeyEvent event) {
        EntityPlayerSP entityPlayer = Minecraft.getMinecraft().player;
        if (entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemMelee) {
            final ItemStack itemStack = entityPlayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            final MeleeType meleeType = ((ItemMelee) itemStack.getItem()).type;
            if (event.keyType == KeyType.ClientReload) {
                meleeType.enhancedModel.config = ModularWarfare.getRenderConfig(meleeType, MeleeRenderConfig.class);
            } else if (event.keyType == KeyType.Inspect) {
                RenderMelee.controller.INSPECT = 0;
            }
        }
    }

    /**
     * Generate the .render.json file of the melee weapons
     */
    @SubscribeEvent
    public void onGenerateJsonModels(GenerateJsonModelsEvent event) {
        System.out.println("Generate JSON Melee");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (ItemMelee itemMelee : ModularWarfare.meleeTypes.values()) {
            MeleeType type = itemMelee.type;
            if (type.contentPack == null)
                continue;

            File contentPackDir = new File(ModularWarfare.CONTENT_DIR, type.contentPack);
            if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                if (ModularWarfare.DEV_ENV) {
                    final File dir = new File(contentPackDir, "/" + type.getAssetDir() + "/render");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                }
            }
        }
    }

//    @SubscribeEvent
//    public void onRenderTickEvent(OnTickRenderEvent event) {
//        if (RenderMelee.controller != null) {
//            if (Minecraft.getMinecraft().player != null) {
//                RenderMelee.controller.updateCurrentItem();
//                RenderMelee.controller.onTickRender(event.smooth);
//            }
//        }
//    }

    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (Minecraft.getMinecraft().player != null) {
                if(Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemMelee) {
                    MeleeType meleeType = ((ItemMelee) Minecraft.getMinecraft().player.getHeldItemMainhand().getItem()).type;
                    MeleeRenderConfig config = (MeleeRenderConfig) meleeType.enhancedModel.config;
                    if (RenderMelee.controller == null || RenderMelee.controller.getConfig() != config) {
                        RenderMelee.controller = new AnimationMeleeController(config,meleeType);
                    }  
                }
                if (RenderMelee.controller != null) {
                    RenderMelee.controller.updateCurrentItem();
                    RenderMelee.controller.onUpdate();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldEvent(RenderWorldLastEvent event) {
        if (RenderMelee.controller != null && ModularWarfare.DEV_ENV) {
            RenderMelee.controller.debugAttackAreaDraw();
        }
    }

    public static Class keyBindingClazz = null;
    public static Method unpressKey = null;

    public static void resetKeyBinding(KeyBinding keyBinding) {
        try {
            if (keyBindingClazz == null || unpressKey == null) {
                keyBindingClazz = KeyBinding.class;
                try {
                    unpressKey = keyBindingClazz.getDeclaredMethod("unpressKey");
                } catch (NoSuchMethodException exception) {

                }
                if (unpressKey == null) {
                    unpressKey = keyBindingClazz.getDeclaredMethod("func_74505_d");
                }
                unpressKey.setAccessible(true);
            } else {
                unpressKey.invoke(keyBinding);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onMouseEvent(InputEvent.MouseInputEvent event) {
        int k = Mouse.getEventButton();
        boolean down = Mouse.getEventButtonState();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (RenderMelee.controller == null)
            return;
        if (player != null && player.getHeldItemMainhand().getItem() instanceof ItemMelee) {
            if (k == 0 || k == 1) {
                GameSettings gs = Minecraft.getMinecraft().gameSettings;
                resetKeyBinding(gs.keyBindAttack);
            }
            if (k == 0 && !down) {
                if (RenderMelee.controller.attack()) {
                    AnimationInfo ai = ((ItemMelee) player.getHeldItemMainhand().getItem()).type
                            .getAnimationInfo(AnimationMeleeType.ATTACK, RenderMelee.controller.currentOrder);
                    ModularWarfare.NETWORK.sendToServer(new PacketSwing(ai.animationName));
                }
            } else if (k == 1 && !down) {
                if (RenderMelee.controller.attackHeavy()) {
                    AnimationInfo ai = ((ItemMelee) player.getHeldItemMainhand().getItem()).type
                            .getAnimationInfo(AnimationMeleeType.HEAVYATTACK, RenderMelee.controller.currentOrder);
                    ModularWarfare.NETWORK.sendToServer(new PacketSwing(ai.animationName));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void cemeraSetup(CameraSetup event) {
        if (Minecraft.getMinecraft().player != null) {
            if (Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemMelee) {
                ItemMelee melee = (ItemMelee)Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
                if (melee.type.enhancedModel != null) {
                    GlStateManager.rotate(90, 0, 1, 0);
                    GlStateManager.rotate((float)Math.toDegrees(RenderMelee.mwf_camera_rot.angle),
                        (float)-RenderMelee.mwf_camera_rot.x, (float)-RenderMelee.mwf_camera_rot.y,
                        (float)-RenderMelee.mwf_camera_rot.z);
                    GlStateManager.translate(RenderMelee.mwf_camera_pos.x / 10,
                        RenderMelee.mwf_camera_pos.y / 10, RenderMelee.mwf_camera_pos.z / 10);
                    GlStateManager.rotate(90, 0, -1, 0);
                    if (RenderMelee.mwf_camera_pos.x != 0 || RenderMelee.mwf_camera_pos.y != 0
                        || RenderMelee.mwf_camera_pos.z != 0 || RenderMelee.mwf_camera_rot.angle != 0) {
                        Minecraft.getMinecraft().renderGlobal.setDisplayListEntitiesDirty();
                    }
                }
            }
        }
        GlStateManager.rotate((float) cemeraBobbing * 5, 0, 0, 1);
    }

    // 轻击
    @SubscribeEvent
    public void onAttackLight(PlayerInteractEvent event) {
        // if(event instanceof PlayerInteractEvent.LeftClickBlock) {
        // if (event.getItemStack().getItem() instanceof ItemMelee) {
        // RenderMelee.controller.applyAnim(AnimationMeleeType.ATTACK);
        // }
        // }
        // if(event instanceof PlayerInteractEvent.LeftClickEmpty) {
        // if (event.getItemStack().getItem() instanceof ItemMelee) {
        //
        // RenderMelee.controller.attack();
        // System.out.println("Left Click!");
        // }
        // }
    }

    // 重击
    @SubscribeEvent
    public void onAttackHeavy(PlayerInteractEvent event) {
        // if(event instanceof PlayerInteractEvent.RightClickBlock || event instanceof
        // PlayerInteractEvent.RightClickItem) {
        // if (event.getItemStack().getItem() instanceof ItemMelee) {
        // RenderMelee.controller.applyAnim(AnimationMeleeType.HEAVYATTACK);
        // }
        // }
        // if(event instanceof PlayerInteractEvent.RightClickEmpty) {
        // if (event.getItemStack().getItem() instanceof ItemMelee) {
        // RenderMelee.controller.applyAnim(AnimationMeleeType.HEAVYATTACK);
        // }
        // }
    }

    // 取消伤害
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemMelee && event.isCancelable()) {
            event.setCanceled(true);
        }
    }
    // objsword参考
    // public void onAttack(AttackEntityEvent event) {
    // ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();
    // if (stack.hasTagCompound()) {
    // boolean disableAttack = false;
    // NBTTagCompound root = stack.getTagCompound();
    // if (root.hasKey("ObjSword")) {
    // root = root.getCompoundTag("ObjSword");
    // if (root.hasKey("disableAttack")) {
    // disableAttack = root.getBoolean("disableAttack");
    // }
    // if (disableAttack) {
    // event.setCanceled(true);
    // if (event.getEntityPlayer() instanceof EntityPlayerMP) {
    // ((EntityPlayerMP)
    // event.getEntityPlayer()).interactionManager.onBlockClicked(event.getEntityPlayer().getPosition().up(1),
    // EnumFacing.UP);
    // }
    // }
    // }
    // }

//    @SubscribeEvent
//    public void onRenderHeldLayer(RenderHeldItemLayerEvent event) {
//        if (!(event.stack.getItem() instanceof ItemMelee)) {
//            return;
//        }
//        BaseType type = ((BaseItem) event.stack.getItem()).baseType;
//        if (!type.hasModel()) {
//            return;
//        }
//
//        EnhancedModel model = type.enhancedModel;
//        MeleeRenderConfig config = (MeleeRenderConfig) type.enhancedModel.config;
//        GlStateManager.pushMatrix();
//        RenderPlayer renderplayer = (RenderPlayer) Minecraft.getMinecraft().getRenderManager()
//            .<AbstractClientPlayer>getEntityRenderObject(event.entitylivingbaseIn);
//    renderplayer.getMainModel().postRenderArm(0.0625F, EnumHandSide.RIGHT);
//
//    GlStateManager.translate(-0.06, 0.38, -0.02);
//
//    GL11.glRotatef(-90F, 0F, 1F, 0F);
//    GL11.glRotatef(90F, 0F, 0F, 1F);
//    GL11.glTranslatef(0.25F, 0.2F, -0.05F);
//    GL11.glScalef(1 / 16F, 1 / 16F, 1 / 16F);
//
//    GL11.glRotatef(config.extra.thirdPersonRotation.x, 1F, 0F, 0F);
//    GL11.glRotatef(config.extra.thirdPersonRotation.y, 0F, 1F, 0F);
//    GL11.glRotatef(config.extra.thirdPersonRotation.z, 0F, 0F, 1F);
//
//    GL11.glTranslatef(config.extra.thirdPersonOffset.x, config.extra.thirdPersonOffset.y,
//            config.extra.thirdPersonOffset.z);
//
//    GL11.glScalef(config.extra.thirdPersonScale, config.extra.thirdPersonScale, config.extra.thirdPersonScale);
//
//    model.updateAnimation(
//            (float) config.meleeAnimations.get(AnimationMeleeType.DEFAULT).get(0).getStartTime(config.FPS));
//
//    int skinId = 0;
//    if (event.stack.hasTagCompound()) {
//        if (event.stack.getTagCompound().hasKey("skinId")) {
//            skinId = event.stack.getTagCompound().getInteger("skinId");
//        }
//    }
//    String path = skinId > 0 ? type.modelSkins[skinId].getSkin() : type.modelSkins[0].getSkin();
//    RenderMelee meleeRender = ClientProxy.meleeRenderer;
//    meleeRender.bindTexture("melee", path);
//
//    boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
//    ObjModelRenderer.glowTxtureMode = true;
//
//    model.renderPartExcept(RenderParameters.partsWithAmmo);
//
//    ObjModelRenderer.glowTxtureMode = glowTxtureMode;
//        GlStateManager.popMatrix();
//    }
}
