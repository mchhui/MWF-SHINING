package com.modularwarfare.client.hud;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.RenderAmmoCountEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.utility.RayUtil;
import com.modularwarfare.utility.ReloadHelper;
import com.modularwarfare.utility.RenderHelperMW;
import mchhui.modularmovements.tactical.client.ClientLitener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.*;

public class GunUI {

    public static final ResourceLocation crosshair = new ResourceLocation("modularwarfare", "textures/gui/crosshair.png");

    public static final ResourceLocation reddot = new ResourceLocation("modularwarfare", "textures/gui/reddot.png");
    public static final ResourceLocation greendot = new ResourceLocation("modularwarfare", "textures/gui/greendot.png");
    public static final ResourceLocation bluedot = new ResourceLocation("modularwarfare", "textures/gui/bluedot.png");

    public static final ResourceLocation hitMarker = new ResourceLocation("modularwarfare", "textures/gui/hitmarker.png");
    public static final ResourceLocation hitMarkerHS = new ResourceLocation("modularwarfare", "textures/gui/hitmarkerhs.png");


    public static int hitMarkerTime = 0;
    public static boolean hitMarkerheadshot;

    public static float bulletSnapFade;

    private static RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();

    public static void addHitMarker(boolean headshot) {
        hitMarkerTime = 20;
        hitMarkerheadshot = headshot;
    }

    @SubscribeEvent
    public void onRenderPre(RenderGameOverlayEvent.Pre event) {
        GlStateManager.pushMatrix();
        Minecraft mc = Minecraft.getMinecraft();
        if (event.isCancelable()) {
            int width = event.getResolution().getScaledWidth();
            int height = event.getResolution().getScaledHeight();
            ItemStack stack = mc.player.getHeldItemMainhand();
            if (stack != null && stack.getItem() instanceof ItemGun) {
                switch (event.getType()) {
                    case CROSSHAIRS:
                        event.setCanceled(true);
                        break;
                    case ALL:
                        GlStateManager.pushMatrix();
                        if (ModConfig.INSTANCE.hud.ammo_count) {
                            RenderAmmoCountEvent ammoCountEvent = new RenderAmmoCountEvent(width, height);
                            MinecraftForge.EVENT_BUS.post(ammoCountEvent);
                            if(!ammoCountEvent.isCanceled()) {
                                GlStateManager.pushMatrix();
                                RenderPlayerAmmo(width, height);
                                GlStateManager.popMatrix();
                            }
                        }
                        RenderHitMarker(Tessellator.getInstance(), width, height);
                        RenderPlayerSnap(width, height);
                        if (mc.getRenderViewEntity().equals(mc.player) && mc.gameSettings.thirdPersonView == 0 && (ClientRenderHooks.isAimingScope||ClientRenderHooks.isAiming) && RenderParameters.collideFrontDistance <= 0.025f) {
                            if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemGun) {
                                final ItemStack gunStack = mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                                if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Sight) != null) {
                                    final ItemAttachment itemAttachment = (ItemAttachment) GunType.getAttachment(gunStack, AttachmentPresetEnum.Sight).getItem();
                                    if (itemAttachment != null) {
                                        if (itemAttachment.type.sight.modeType != null) {

                                            float gunRotX = RenderParameters.GUN_ROT_X_LAST + (RenderParameters.GUN_ROT_X - RenderParameters.GUN_ROT_X_LAST) * ClientProxy.renderHooks.partialTicks;
                                            float gunRotY = RenderParameters.GUN_ROT_Y_LAST + (RenderParameters.GUN_ROT_Y - RenderParameters.GUN_ROT_Y_LAST) * ClientProxy.renderHooks.partialTicks;

                                            float alpha = gunRotX;
                                            alpha = Math.abs(alpha);

                                            if (gunRotX > -1.5 && gunRotX < 1.5 && gunRotY > -0.75 && gunRotY < 0.75 && RenderParameters.GUN_CHANGE_Y == 0F) {
                                                GL11.glPushMatrix();

                                                GL11.glRotatef(gunRotX, 0, -1, 0);
                                                GL11.glRotatef(gunRotY, 0, 0, -1);

                                                if (itemAttachment.type.sight.modeType.isDot) {
                                                    switch (itemAttachment.type.sight.dotColorType) {
                                                        case RED:
                                                            mc.renderEngine.bindTexture(reddot);
                                                            break;
                                                        case BLUE:
                                                            mc.renderEngine.bindTexture(bluedot);
                                                            break;
                                                        case GREEN:
                                                            mc.renderEngine.bindTexture(greendot);
                                                            break;
                                                        default:
                                                            mc.renderEngine.bindTexture(reddot);
                                                            break;
                                                    }
                                                    GlStateManager.color(1.0f, 1.0f, 1.0f, 1 - alpha);
                                                    Gui.drawModalRectWithCustomSizedTexture(width / 2, height / 2, 2.0f, 2.0f, 1, 1, 16.0f, 16.0f);
                                                } else {
                                                    ResourceLocation overlayToRender = itemAttachment.type.sight.overlayType.resourceLocations.get(0);

                                                    float factor = 1;
                                                    if (width < 700) {
                                                        factor = 2;
                                                    }
                                                    int size = (32 * 2 / (int) (event.getResolution().getScaleFactor() * factor)) + ((int) (crouchSwitch) * 5);
                                                    float scale=Math.abs(playerRecoilYaw)+Math.abs(playerRecoilPitch);
                                                    scale*=((ModelAttachment) itemAttachment.type.model).config.sight.factorCrossScale;
                                                    size = (int) (((size * (1 + (scale > 0.8 ? scale : 0) * 0.2))) * ((ModelAttachment) itemAttachment.type.model).config.sight.rectileScale);
                                                    GL11.glTranslatef((width / 2), (height / 2), 0);
                                                    if(!itemAttachment.type.sight.plumbCrossHair) {
                                                        GlStateManager.rotate(CROSS_ROTATE,0,0,1);
                                                    }
                                                    GL11.glTranslatef(-size, -size, 0);
                                                    GL11.glTranslatef((VAL2 / 10), (VAL / 10), 0);
                                                    RenderHelperMW.renderImageAlpha(0, 0, overlayToRender, size * 2, size * 2, 1f - alpha);
                                                }

                                                GL11.glPopMatrix();
                                            }

                                        }
                                    }
                                }
                            }
                        }

                        boolean showCrosshair = ((adsSwitch < 0.6F) && (ClientProxy.gunEnhancedRenderer.controller.ADS < 0.5F));
                        if(ClientRenderHooks.getEnhancedAnimMachine(mc.player) != null){
                            if(ClientRenderHooks.getEnhancedAnimMachine(mc.player).reloading) {
                                showCrosshair = false;
                            }
                            if(ClientProxy.gunEnhancedRenderer.controller.INSPECT != 1F){
                                showCrosshair = false;
                            }
                        }

                        if(mc.getRenderViewEntity() != mc.player){
                            showCrosshair = false;
                        }
                        if (ModConfig.INSTANCE.hud.enable_crosshair && !ClientRenderHooks.getAnimMachine(mc.player).attachmentMode && showCrosshair && mc.gameSettings.thirdPersonView == 0 && !mc.player.isSprinting() && !ClientRenderHooks.getAnimMachine(mc.player).reloading && mc.player.getHeldItemMainhand().getItem() instanceof ItemGun) {
                            if(RenderParameters.collideFrontDistance <= 0.2f) {
                                GlStateManager.pushMatrix();

                                if(ModConfig.INSTANCE.hud.dynamic_crosshair) {
                                    float gunRotX = RenderParameters.GUN_ROT_X_LAST + (RenderParameters.GUN_ROT_X - RenderParameters.GUN_ROT_X_LAST) * smoothing;
                                    float gunRotY = RenderParameters.GUN_ROT_Y_LAST + (RenderParameters.GUN_ROT_Y - RenderParameters.GUN_ROT_Y_LAST) * smoothing;
                                    GL11.glRotatef(gunRotX, 0, -1, 0);
                                    GL11.glRotatef(gunRotY, 1, 0, 1);
                                }

                                final float accuracy = RayUtil.calculateAccuracyClient((ItemGun) mc.player.getHeldItemMainhand().getItem(), mc.player);
                                int move = Math.max(0, (int) (accuracy * 3.0f));
                                mc.renderEngine.bindTexture(crosshair);
                                int xPos = width / 2;
                                int yPos = height / 2;


                                GlStateManager.translate(xPos, yPos, 0f);
                                if(ModularWarfare.isLoadedModularMovements) {
                                    GL11.glRotatef(15F * ClientLitener.cameraProbeOffset, 0, 0, 1);
                                }
                                GlStateManager.enableBlend();
                                GlStateManager.color(1f, 1f, 1f, 1f);
                                GL11.glColor4f(1, 1, 1, 1);
                                Gui.drawModalRectWithCustomSizedTexture(0, 0, 1.0f, 1.0f, 1, 1, 16.0f, 16.0f);
                                Gui.drawModalRectWithCustomSizedTexture(0, 0 + move, 1.0f, 1.0f, 1, 4, 16.0f, 16.0f);
                                Gui.drawModalRectWithCustomSizedTexture(0, 0 - move - 3, 1.0f, 1.0f, 1, 4, 16.0f, 16.0f);
                                Gui.drawModalRectWithCustomSizedTexture(0 + move, 0, 1.0f, 1.0f, 4, 1, 16.0f, 16.0f);
                                Gui.drawModalRectWithCustomSizedTexture(0 - move - 3, 0, 1.0f, 1.0f, 4, 1, 16.0f, 16.0f);
                                GlStateManager.disableBlend();

                                GlStateManager.popMatrix();
                            }
                        }
                        GlStateManager.popMatrix();
                        break;
                    default:
                        break;

                }
            }
        }
        GL11.glColor4f(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private void RenderHitMarker(Tessellator tessellator, int i, int j) {

        Minecraft mc = Minecraft.getMinecraft();

        if (hitMarkerTime > 0) {
            GlStateManager.pushMatrix();

            if (!hitMarkerheadshot) {
                mc.renderEngine.bindTexture(hitMarker);
            } else {
                mc.renderEngine.bindTexture(hitMarkerHS);

            }

            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0f, 1.0f, 1.0f, Math.max((hitMarkerTime - 10.0f + ClientProxy.renderHooks.partialTicks) / 10.0f, 0.0f));

            double zLevel = 0D;

            tessellator.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);

            tessellator.getBuffer().pos(i / 2 - 4d, j / 2 + 5d, zLevel).tex(0D / 16D, 9D / 16D).endVertex();
            tessellator.getBuffer().pos(i / 2 + 5d, j / 2 + 5d, zLevel).tex(9D / 16D, 9D / 16D).endVertex();
            tessellator.getBuffer().pos(i / 2 + 5d, j / 2 - 4d, zLevel).tex(9D / 16D, 0D / 16D).endVertex();
            tessellator.getBuffer().pos(i / 2 - 4d, j / 2 - 4d, zLevel).tex(0D / 16D, 0D / 16D).endVertex();

            tessellator.draw();

            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();

            GlStateManager.popMatrix();
        }
    }

    private void RenderPlayerAmmo(int i, int j) {
        Minecraft mc = Minecraft.getMinecraft();

        ItemStack stack = mc.player.getHeldItem(EnumHand.MAIN_HAND);
        if (stack != null && stack.getItem() instanceof ItemGun) {

            if (stack.getTagCompound() != null) {
                ItemStack ammoStack = new ItemStack(stack.getTagCompound().getCompoundTag("ammo"));
                GunType type=((ItemGun)stack.getItem()).type;
                if(type.animationType.equals(WeaponAnimationType.BASIC)) {
                    ammoStack.setItemDamage(0);
                    if (ItemGun.hasAmmoLoaded(stack)) {
                        renderAmmo(stack, ammoStack, i, j, 0);
                    } else if (ItemGun.getUsedBullet(stack, ((ItemGun) (stack.getItem())).type) != null) {
                        renderBullets(stack, null, i, j, 0,null);
                    }
                }else {
                    if(ClientProxy.gunEnhancedRenderer.controller!=null) {
                        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(mc.player);
                        AnimationType reloadAni=anim.getReloadAnimationType();
                        if(type.acceptedAmmo!=null) {
                            ammoStack=ClientProxy.gunEnhancedRenderer.controller.getRenderAmmo(ammoStack);
                            ammoStack.setItemDamage(0);
                            if(reloadAni==AnimationType.RELOAD_FIRST||reloadAni==AnimationType.RELOAD_FIRST_QUICKLY||reloadAni==AnimationType.UNLOAD) {
                                ammoStack=ItemStack.EMPTY;
                            }
                            renderAmmo(stack, ammoStack, i, j, 0);
                        }else{
                            boolean flag=true;
                            ItemStack bulletStack = new ItemStack(stack.getTagCompound().getCompoundTag("bullet"));
                            if(anim.reloading) {
                                bulletStack=ClientProxy.gunEnhancedRenderer.controller.getRenderAmmo(bulletStack);
                                if(ClientProxy.gunEnhancedRenderer.controller.getPlayingAnimation() ==AnimationType.POST_UNLOAD) {
                                    //flag=false;
                                }
                            }
                            bulletStack.setItemDamage(0);
                            int offset = anim.getAmmoCountOffset(false);
                            if(!anim.reloading) {
                                offset=0;
                            }
                            if(flag) {
                                renderBullets(stack, null, i, j, offset,bulletStack);
                            }
                        }
                    }
                }
            }
        }
    }

    public void renderAmmo(ItemStack stack,ItemStack ammoStack,int i, int j,int countOffset) {
        Minecraft mc = Minecraft.getMinecraft();
        int x = 0;
        final int top = j - 38;
        final int left = 2;
        final int right = Math.min(left + 66, i / 2 - 60);
        final int bottom = top + 22;
        /** If gun use ammo/magazines **/
        if (ammoStack.getTagCompound() != null && ammoStack.getItem() instanceof ItemAmmo) {

            ItemAmmo itemAmmo = (ItemAmmo) ammoStack.getItem();
            Integer currentMagcount=null;
            if(ammoStack.getTagCompound().hasKey("magcount")) {
                currentMagcount=ammoStack.getTagCompound().getInteger("magcount");
            }
            int currentAmmoCount =ReloadHelper.getBulletOnMag(ammoStack,currentMagcount) + countOffset;

            GlStateManager.pushMatrix();

            GlStateManager.translate(i - 120, 0F, 0F);

            Gui.drawRect(left + right - 3, top, right * 2 - 18, bottom, Integer.MIN_VALUE);

            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            drawSlotInventory(mc.fontRenderer, ammoStack, left + 67, j - 35);
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();

            String color = TextFormatting.WHITE + "";
            if (currentAmmoCount < itemAmmo.type.ammoCapacity / 6) {
                color = TextFormatting.RED + "";
                mc.fontRenderer.drawString(String.valueOf(TextFormatting.YELLOW + "[R]" + TextFormatting.WHITE + " Reload"), 10, j - 30, 0xffffff);
            }

            String s = String.valueOf(color + currentAmmoCount) + "/" + itemAmmo.type.ammoCapacity;

            RenderHelperMW.renderTextWithShadow(String.valueOf(s), left + 83, j - 30, 0xffffff);

            x += 16 + mc.fontRenderer.getStringWidth(s);

            ItemGun gun = (ItemGun) stack.getItem();
            if (gun.type.getFireMode(stack) != null) {
                RenderHelperMW.renderCenteredTextWithShadow(String.valueOf(gun.type.getFireMode(stack)), left + 90, j - 50, 0xffffff);
            }

            GlStateManager.popMatrix();
        }
    }

    public void renderBullets(ItemStack stack, ItemStack ammoStack, int i, int j,int countOffset,ItemStack expectItemBullet) {
        Minecraft mc = Minecraft.getMinecraft();
        int x = 0;
        final int top = j - 38;
        final int left = 2;
        final int right = Math.min(left + 66, i / 2 - 60);
        final int bottom = top + 22;
        /** If gun use bullets **/
        ItemBullet itemBullet = null;
        if (expectItemBullet!=null && expectItemBullet.getItem() instanceof ItemBullet) {
            itemBullet = (ItemBullet) expectItemBullet.getItem();
        }
        if (itemBullet == null) {
            itemBullet = ItemGun.getUsedBullet(stack, ((ItemGun) (stack.getItem())).type);
        }
        if (itemBullet == null) {
            return;
        }

        int currentAmmoCount = stack.getTagCompound().getInteger("ammocount") + countOffset;
        //System.out.println(currentAmmoCount+":"+countOffset);
        int maxAmmo = (((ItemGun) (stack.getItem())).type.internalAmmoStorage == null) ? 0 : (((ItemGun) (stack.getItem())).type.internalAmmoStorage);

        GlStateManager.pushMatrix();

        GlStateManager.translate(i - 120, 0F, 0F);

        Gui.drawRect(left + right - 3, top, right * 2 - 18, bottom, Integer.MIN_VALUE);

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        drawSlotInventory(mc.fontRenderer, new ItemStack(itemBullet), left + 67, j - 35);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();

        String color = TextFormatting.WHITE + "";
        if (currentAmmoCount < maxAmmo / 6) {
            color = TextFormatting.RED + "";
            mc.fontRenderer.drawString(String.valueOf(TextFormatting.YELLOW + "[R]" + TextFormatting.WHITE + " Reload"), 10, j - 30, 0xffffff);
        }

        String s = String.valueOf(color + currentAmmoCount) + "/" + maxAmmo;

        mc.fontRenderer.drawStringWithShadow(String.valueOf(s), left + 83, j - 30, 0xffffff);
        x += 16 + mc.fontRenderer.getStringWidth(s);

        ItemGun gun = (ItemGun) stack.getItem();
        if (gun.type.getFireMode(stack) != null) {
            RenderHelperMW.renderCenteredTextWithShadow(String.valueOf(gun.type.getFireMode(stack)), left + 90, j - 50, 0xffffff);
        }

        GlStateManager.popMatrix();
    }

    public void RenderPlayerSnap(int i, int j) {
        GL11.glPushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        RenderHelperMW.renderImageAlpha(0, 0, new ResourceLocation(ModularWarfare.MOD_ID, "textures/gui/snapshadow.png"), i, j, this.bulletSnapFade);
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GL11.glPopMatrix();
    }

    private void drawSlotInventory(FontRenderer fontRenderer, ItemStack itemstack, int i, int j) {
        if (itemstack == null || itemstack.isEmpty())
            return;

        GL11.glPushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();

        itemRenderer.renderItemIntoGUI(itemstack, i, j);
        itemRenderer.renderItemOverlayIntoGUI(fontRenderer, itemstack, i, j, null); //May be something other than null
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GL11.glPopMatrix();
    }


}
