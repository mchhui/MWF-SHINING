package com.modularwarfare.client.gui;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.adsSwitch;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;
import org.lwjgl.util.vector.Vector3f;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.configs.GunRenderConfig;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.Attachment;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.AttachmentGroup;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.Transform;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.gui.TextureButton.TypeEnum;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.model.ModelGun;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.client.shader.ProjectionHelper;
import com.modularwarfare.common.guns.AmmoType;
import com.modularwarfare.common.guns.AttachmentPresetEnum;
import com.modularwarfare.common.guns.AttachmentType;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.ItemSpray;
import com.modularwarfare.common.guns.SprayType;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.guns.WeaponFireMode;
import com.modularwarfare.common.guns.WeaponScopeModeType;
import com.modularwarfare.common.handler.data.VarBoolean;
import com.modularwarfare.common.network.PacketAimingRequest;
import com.modularwarfare.common.network.PacketGunAddAttachment;
import com.modularwarfare.common.network.PacketGunUnloadAttachment;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.ColorUtils;
import com.modularwarfare.utility.MWSound;
import com.modularwarfare.utility.OptifineHelper;
import com.modularwarfare.utility.ReloadHelper;
import com.modularwarfare.utility.RenderHelperMW;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.ConfigManager;

public class GuiGunModify extends GuiScreen {
	public String title = "Config GUI";
	public ItemStack currentModify;
	public double autoRotate = 0;
	public double rotateY = 0;
	public double rotateZ = 0;
	public boolean leftClicked = false;
	public int lastMouseX;
	public int lastMouseY;
	private final int sideButtonIdOffset=20;
	private final int subSlotIdOffset=100;
	private final int backpackIdOffset=200;
	private int slotPreLine=6;
	private List<Integer> attachmentSlotList=null;
	private TextureButton selectedSideButton=null;
	private static final ProjectionHelper projectionHelper = new ProjectionHelper();
	public static final ResourceLocation point = new ResourceLocation("modularwarfare", "textures/modifygui/point.png");
	public static final ResourceLocation arrow_down = new ResourceLocation("modularwarfare", "textures/modifygui/arrow_down.png");
	public static final ResourceLocation arrow_left = new ResourceLocation("modularwarfare", "textures/modifygui/arrow_left.png");
	public static final ResourceLocation arrow_right = new ResourceLocation("modularwarfare", "textures/modifygui/arrow_right.png");
	public static final ResourceLocation arrow_up = new ResourceLocation("modularwarfare", "textures/modifygui/arrow_up.png");
	public static final ResourceLocation barrel = new ResourceLocation("modularwarfare", "textures/modifygui/barrel.png");
	public static final ResourceLocation charm = new ResourceLocation("modularwarfare", "textures/modifygui/charm.png");
	public static final ResourceLocation flashlight = new ResourceLocation("modularwarfare", "textures/modifygui/flashlight.png");
	public static final ResourceLocation grip = new ResourceLocation("modularwarfare", "textures/modifygui/grip.png");
	public static final ResourceLocation sight = new ResourceLocation("modularwarfare", "textures/modifygui/sight.png");
	public static final ResourceLocation sprays = new ResourceLocation("modularwarfare", "textures/modifygui/sprays.png");
	public static final ResourceLocation stock = new ResourceLocation("modularwarfare", "textures/modifygui/stock.png");
	public static final ResourceLocation slot = new ResourceLocation("modularwarfare", "textures/modifygui/slot.png");
	public static final ResourceLocation quit = new ResourceLocation("modularwarfare", "textures/modifygui/quit.png");
	public static final ResourceLocation statu = new ResourceLocation("modularwarfare", "textures/modifygui/statu.png");
	public static final ResourceLocation decal_1 = new ResourceLocation("modularwarfare", "textures/modifygui/decal_1.png");
	public static final ResourceLocation slot_topbg = new ResourceLocation("modularwarfare", "textures/modifygui/slot_topbg.png");
	public static TextureButton QUITBUTTON,PAGEBUTTON;
	private static double subPageX;
	private static double subPageY;
	private static double subPageWidth;
	private static double subPageHeight;
	public static boolean isEnglishLanguage=true;
	public static boolean clickOnce=false;
	// public FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
	// public FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
	public GuiGunModify() {
		updateItemModifying();
		initGui();
		isEnglishLanguage=Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode().indexOf("en_")!=-1;
	}
	public void updateItemModifying() {
		ItemStack itemMainhand=Minecraft.getMinecraft().player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
		boolean refresh=false;
		if(this.currentModify!=null&&!this.currentModify.equals(itemMainhand)) {
			refresh=true;
		}
		this.currentModify = itemMainhand;
		if(refresh) {
			initGui();
		}
	}
	@Override
	public void initGui() {
        RenderGunEnhanced rge = ((RenderGunEnhanced) ClientRenderHooks.customRenderers[0]);
        // model.updateAnimation(rge.controller.getTime(),"");
		BaseType type = ((BaseItem) this.currentModify.getItem()).baseType;
		if (((GunType) type).animationType == WeaponAnimationType.ENHANCED) {
			rge.controller.reset(true);
		}
	    
		mc=Minecraft.getMinecraft();
		ScaledResolution scaledresolution = new ScaledResolution(mc);
		double sFactor = mc.displayWidth / 1920d;
		double scaleFactor=1.0d* sFactor / scaledresolution.getScaleFactor();
		this.buttonList.clear();
		int buttonSize=90/scaledresolution.getScaleFactor();
		QUITBUTTON=new TextureButton(1000,scaledresolution.getScaledWidth()-buttonSize*1.5f,scaledresolution.getScaledHeight()-buttonSize*1.5f,buttonSize,buttonSize,quit).setType(TypeEnum.Button);
		
		this.buttonList.add(QUITBUTTON);
		if(PAGEBUTTON==null) {
			PAGEBUTTON=new TextureButton(1001,0,0,buttonSize/4,buttonSize,decal_1).setType(TypeEnum.SideButtonVert);
		}
		PAGEBUTTON.width=buttonSize/4;
		PAGEBUTTON.height=buttonSize;
		subPageX=0;
		subPageY=0;
		subPageWidth=(PAGEBUTTON.state==-1?256:0)*scaleFactor;
		subPageHeight=512*scaleFactor;
		PAGEBUTTON.x=subPageX+subPageWidth;
		PAGEBUTTON.y=0;
		
		this.buttonList.add(PAGEBUTTON);
		this.buttonList.add(new TextureButton(0,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Barrel).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(1,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Charm).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(2,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Flashlight).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(3,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Grip).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(4,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Sight).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(5,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Skin).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(6,9999,9999,buttonSize,buttonSize,slot).setAttachment(AttachmentPresetEnum.Stock).setType(TypeEnum.Slot));
		this.buttonList.add(new TextureButton(0+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		this.buttonList.add(new TextureButton(1+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		this.buttonList.add(new TextureButton(2+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		this.buttonList.add(new TextureButton(3+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		this.buttonList.add(new TextureButton(4+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		this.buttonList.add(new TextureButton(5+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		this.buttonList.add(new TextureButton(6+sideButtonIdOffset,9999,9999,buttonSize/3,buttonSize,decal_1).setType(TypeEnum.SideButton));
		
		updateButtonItem();
	}
	public List<Integer> checkAttach(EntityPlayer player, GunType gunType, AttachmentPresetEnum attachmentEnum) {
        List<Integer> attachments = new ArrayList<>();
        //attachments.add(-1);
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack itemStack = player.inventory.getStackInSlot(i);
            if (attachmentEnum != AttachmentPresetEnum.Skin) {
                if (itemStack != null && itemStack.getItem() instanceof ItemAttachment) {
                    ItemAttachment itemAttachment = (ItemAttachment) itemStack.getItem();
                    AttachmentType attachType = itemAttachment.type;
                    if (attachType.attachmentType.equals(attachmentEnum)) {
                        if (gunType.acceptedAttachments.get(attachType.attachmentType) != null && gunType.acceptedAttachments.get(attachType.attachmentType).size() >= 1) {
                            if (gunType.acceptedAttachments.get(attachType.attachmentType).contains(attachType.internalName)) {
                                attachments.add(i);
                            }
                        }
                    }
                }
            } else {
                if (itemStack != null && itemStack.getItem() instanceof ItemSpray) {
                    ItemSpray itemSpray = (ItemSpray) itemStack.getItem();
                    SprayType attachType = itemSpray.type;
                    for (int j = 0; j < gunType.modelSkins.length; j++) {
                        if (gunType.modelSkins[j].internalName.equalsIgnoreCase(attachType.skinName)) {
                            attachments.add(i);
                        }
                    }
                }
            }
        }
        return attachments;
    }
	public void joinSubPageButtons(EntityPlayer player,ScaledResolution scaledresolution) {
		//make subPage button
		TextureButton currentButton=this.getButton(selectedSideButton.id-this.sideButtonIdOffset);
		BaseType type = ((BaseItem) this.currentModify.getItem()).baseType;
		GunType gunType = (GunType) type;
		attachmentSlotList=checkAttach(mc.player,gunType,currentButton.getAttachmentType());
		int buttonSize=90/scaledresolution.getScaleFactor();
		boolean isSkin=currentButton.getAttachmentType().equals(AttachmentPresetEnum.Skin);
		if(!isSkin) {
			this.buttonList.add(new TextureButton(-1,9999,9999,buttonSize,buttonSize,slot).setAttachment(currentButton.getAttachmentType()).setType(TypeEnum.SubSlot));
		}
		for(Integer inventoryId:attachmentSlotList) {
			this.buttonList.add(new TextureButton(subSlotIdOffset+inventoryId,9999,9999,buttonSize,buttonSize,slot).setAttachment(currentButton.getAttachmentType()).setType(TypeEnum.SubSlot).setItemStack(player.inventory.getStackInSlot(inventoryId)));
		}
	}
	public boolean gunHasMultiSkins() {
		BaseType type = ((BaseItem) this.currentModify.getItem()).baseType;
		GunType gunType = (GunType) type;
		return gunType.modelSkins!=null&&gunType.modelSkins.length>1;
	}
	public void updateButtonItem() {
		BaseType type = ((BaseItem) this.currentModify.getItem()).baseType;
		GunType gunType = (GunType) type;
		List<AttachmentPresetEnum> keys = new ArrayList<>(gunType.acceptedAttachments.keySet());
        if (gunType.modelSkins.length > 1) {
            keys.add(AttachmentPresetEnum.Skin);
        }
        for(GuiButton button:this.buttonList) {
        	if(button instanceof TextureButton) {
				TextureButton tb=(TextureButton) button;
				if(tb.getAttachmentType()==AttachmentPresetEnum.Skin&&gunHasMultiSkins()) {
					int skinId = 0;
                    if (this.currentModify.hasTagCompound()) {
                        if (this.currentModify.getTagCompound().hasKey("skinId")) {
                            skinId = this.currentModify.getTagCompound().getInteger("skinId");
                        }
                    }
                    ItemSpray itemSpray=null;
                    for(Item item:Item.REGISTRY) {
                    	if (item instanceof ItemSpray) {
                    		ItemSpray itemS = (ItemSpray) item;
                    		SprayType attachType = itemS.type;
                    		if(gunType.modelSkins[skinId].internalName.equalsIgnoreCase(attachType.skinName)) {
                    			itemSpray=itemS;
                    			break;
                    		}
                    	}
                    }
                    if(itemSpray!=null) {
                    	ItemStack skinItem=new ItemStack(itemSpray,1);
                        tb.setItemStack(skinItem);
                    }
                    
//					if (itemStack != null && itemStack.getItem() instanceof ItemSpray) {
//	                    ItemSpray itemSpray = (ItemSpray) itemStack.getItem();
//	                    SprayType attachType = itemSpray.type;
//	                    for (int j = 0; j < gunType.modelSkins.length; j++) {
//	                        if (gunType.modelSkins[j].internalName.equalsIgnoreCase(attachType.skinName)) {
//	                            attachments.add(i);
//	                        }
//	                    }
//	                }
				}
        	}
        }
		for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
			ItemStack itemStack = GunType.getAttachment(this.currentModify, attachment);
			if (keys.contains(attachment)&&itemStack != null && !itemStack.isEmpty()) {
				for(GuiButton button:this.buttonList) {
					if(button instanceof TextureButton) {
						TextureButton tb=(TextureButton) button;
						if(tb.getType().equals(TextureButton.TypeEnum.Slot)&&tb.getAttachmentType()==attachment) {
							tb.setItemStack(itemStack);
						}
					}
				}
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		clickOnce=false;
		updateItemModifying();
		EntityRenderer renderer = mc.entityRenderer;
		double sFactor = mc.displayWidth / 1920d;
		float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16F;
		ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
		EntityLivingBase entitylivingbaseIn = Minecraft.getMinecraft().player;
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
//        float zFar=2*farPlaneDistance;
		GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(),0.0D, 100.0D, 4000.0D);
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
//		Project.gluPerspective(70, (float) mc.displayWidth / (float) mc.displayHeight, 0.00001F, zFar);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();

		int color = ColorUtils.getARGB(100, 100, 100, 120);
//		GlStateManager.enableDepth();
		this.drawGradientRect(0, 0, width, height, color, color);
		autoRotate += 1d;
		// this.drawDefaultBackground();
		// color=ColorUtils.getARGB(10, 10, 10, 120);
		// this.drawGradientRect(0, 0, this.width, this.height,color, color);

//		GlStateManager.disableTexture2D();
//        GlStateManager.enableBlend();
//        GlStateManager.disableAlpha();
//        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//        GlStateManager.shadeModel(7425);

		// GlStateManager.pushMatrix();
		double scale = 300.0d;
		// RenderHelper.enableStandardItemLighting();
		// RenderHelper.enableGUIStandardItemLighting();
		// RenderHelper.disableStandardItemLighting();
		// RenderHelper.disableStandardItemLighting();
		// GlStateManager.translate(1, 1, 0);

		float f3 = entitylivingbaseIn.prevRotationPitch
				+ (entitylivingbaseIn.rotationPitch - entitylivingbaseIn.prevRotationPitch) * partialTicks;
		float f4 = entitylivingbaseIn.prevRotationYaw
				+ (entitylivingbaseIn.rotationYaw - entitylivingbaseIn.prevRotationYaw) * partialTicks;

		renderer.enableLightmap();
		// Setup lighting
		GlStateManager.disableLighting();
		GlStateManager.pushMatrix();
		GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(f4, 0.0F, 1.0F, 0.0F);
		// GlStateManager.rotate((float) -autoRotate, 0, 1, 0);
		/**
		 *  RenderHelper.enableStandardItemLighting();
		 * */
		GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
		// GlStateManager.glLight(16384, 4611, setColorBuffer(LIGHT0_POS.x,
		// LIGHT0_POS.y, LIGHT0_POS.z, 0.0D));
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		GlStateManager.popMatrix();

		// Do lighting
		// int i = 15;//this.mc.world.getCombinedLight(new
		// BlockPos(entitylivingbaseIn.posX, entitylivingbaseIn.posY + (double)
		// entitylivingbaseIn.getEyeHeight(), entitylivingbaseIn.posZ), 0)
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		
		
		// GlStateManager.disableCull();
		BaseType type = ((BaseItem) this.currentModify.getItem()).baseType;
		ItemStack itemstack = this.currentModify;
		GlStateManager.pushMatrix();
		if (((GunType) type).animationType == WeaponAnimationType.BASIC) {
			renderBasicModel(entitylivingbaseIn, type, scale, itemstack, sFactor, scaledresolution, f4);
		} else if (((GunType) type).animationType == WeaponAnimationType.ENHANCED) {
			renderEnhancedModel(entitylivingbaseIn, type, scale, itemstack, sFactor, scaledresolution, f4);
		}
		// state fix
		// GlStateManager.scale(1, 1, 1);
		// double dd=(centerOffsetY*scaledresolution.getScaleFactor())/scale;
		// GlStateManager.translate(0,dd*2, 0);
		// GlStateManager.rotate((float) rotateZ*2, -1, 0, 0);
		projectionHelper.updateMatrices();
		GlStateManager.popMatrix();
		renderSlotStuff(mouseX,mouseY,entitylivingbaseIn, type, scale,sFactor, itemstack, scaledresolution, f4);
		if(PAGEBUTTON.state==-1) {
			drawSubPage(mouseY, mouseY, partialTicks,sFactor,scaledresolution);
		}
		
		QUITBUTTON.drawButton(mc, mouseX, mouseY, partialTicks);
		PAGEBUTTON.drawButton(mc, mouseX, mouseY, partialTicks);
		
		
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.popMatrix();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.popMatrix();

	}
	public void renderSlotStuff(int mouseX, int mouseY,EntityLivingBase entitylivingbaseIn,BaseType type,double scale,double sFactor,ItemStack itemstack,ScaledResolution scaledresolution,float partialTicks) {
		
		GL11.glTranslatef(0, 0, 1000);
		GlStateManager.disableBlend();
		GlStateManager.disableLighting();
		GlStateManager.disableTexture2D();
		GL11.glPointSize(10);
		GL11.glColor3f(0, 1, 0);
		GunType gunType = (GunType) type;
		boolean isBasic=gunType.animationType == WeaponAnimationType.BASIC;
		
		GunEnhancedRenderConfig config=null;
		GunRenderConfig configBasic=null;
		if(isBasic) {
			ModelGun model = (ModelGun) gunType.model;
			configBasic = model.config;
		}else {
			config = (GunEnhancedRenderConfig) gunType.enhancedModel.config;
		}

		ArrayList<float[]> placeList = new ArrayList();
		float num_segments = 200f;
		float cx = (float) (scaledresolution.getScaledWidth_double() / 2);
		float cy = (float) (scaledresolution.getScaledHeight_double() / 2) - (5 * scaledresolution.getScaleFactor());
		float rx = (float) (560*sFactor/scaledresolution.getScaleFactor());
		float ry = (float) (280*sFactor/scaledresolution.getScaleFactor());
		float theta = (float) (2 * 3.1415926 / num_segments);
		float c = (float) Math.cos(theta);// precalculate the sine and cosine
		float s = (float) Math.sin(theta);
		float t;
		float x = 1;// we start at angle = 0
		float y = 0;
		// glBegin(GL_LINE_LOOP);
		for (int ii = 0; ii < num_segments; ii++) {
			// apply radius and offset
			// drawPoint(x*rx+cx,y*ry+cy);
			placeList.add(new float[] { x * rx + cx, y * ry + cy });
			// apply the rotation matrix
			t = x;
			x = c * x - s * y;
			y = s * t + c * y;
		}
		
		for(GuiButton button:this.buttonList) {
			if(button instanceof TextureButton&&((TextureButton) button).getType().equals(TextureButton.TypeEnum.SideButton)) {
				button.visible=false;
			}
		}
		ArrayList<float[]> slotPlaceList = new ArrayList();
		List<AttachmentPresetEnum> keys = new ArrayList<>(gunType.acceptedAttachments.keySet());
        if (gunType.modelSkins.length > 1) {
            keys.add(AttachmentPresetEnum.Skin);
        }
		for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
			ItemStack itemStack = GunType.getAttachment(itemstack, attachment);
			if (keys.contains(attachment)) {
				//itemStack != null && itemStack.getItem() != Items.AIR
				//AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
				Vector3f partTranslate = new Vector3f(0,0,0);
				if(isBasic) {
//					for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
//						GlStateManager.pushMatrix();
//						ItemStack itemStack = GunType.getAttachment(itemstack, attachment);
//					
//					
//					if (configBasic.attachmentGroup.containsKey(attachment.typeName)) {
//						AttachmentGroup ag = configBasic.attachmentGroup.get(attachment.typeName);
//						if (ag != null) {
//							partTranslate = new Vector3f(ag.translate);
//						}
//					}
					ArrayList<Vector3f> array=configBasic.attachments.attachmentPointMap.get(attachment);
					if(array!=null&&array.size()>1) {
						partTranslate = array.get(0);
					}
				}else {
					if (config.attachmentGroup.containsKey(attachment.typeName)) {
						AttachmentGroup ag = config.attachmentGroup.get(attachment.typeName);
						if (ag != null) {
							partTranslate = new Vector3f(ag.translate);
						}
					}
					if (itemStack != null && itemStack.getItem() != Items.AIR) {
						AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
						if (config.attachment.containsKey(attachmentType.internalName)) {
	                        if (config.attachment.get(attachmentType.internalName).binding.equals("gunModel")) {
	                        	if (config.attachmentGroup.containsKey(attachment.typeName)) {
	        						AttachmentGroup ag = config.attachmentGroup.get(attachment.typeName);
	        						if (ag != null) {
	        							partTranslate = new Vector3f(ag.translate);
	        						}
	        					}
	                        }else {
//	                        	Minecraft.getMinecraft().player.sendMessage(new TextComponentString("test4:有配件的绑定模型不是gunModel"));
//	                        	Minecraft.getMinecraft().player.sendMessage(new TextComponentString(config.attachment.get(attachmentType.internalName).binding));
	                        	partTranslate.x =config.attachment.get(attachmentType.internalName).attachmentGuiOffset.x;
	                        	partTranslate.y =config.attachment.get(attachmentType.internalName).attachmentGuiOffset.y;
	                        	partTranslate.z =config.attachment.get(attachmentType.internalName).attachmentGuiOffset.z;
	                        }
	                    }
					}
				}
//				if (attachment.typeName.equals("stock")) {
//					///////// warning magic number!!!!!!!!!114514
//					partTranslate.x = (float) -4.59d;
//					partTranslate.y = (float) -0.87d;
//					partTranslate.z = (float) 0;
//				}
				
				Vec3d partVec3d = projectionHelper.project(partTranslate.x, partTranslate.y, partTranslate.z);
				float[] nearestPoint = new float[] { 9999, 9999 };
				Vec3d scaledPartVec3d = new Vec3d(partVec3d.x / scaledresolution.getScaleFactor(),
						scaledresolution.getScaledHeight_double()
								- (partVec3d.y / scaledresolution.getScaleFactor()),
						0);
				for (float[] point : placeList) {
					double vtpD = scaledPartVec3d.distanceTo(new Vec3d(point[0], point[1], 0));

					double slotSize = 61d/scaledresolution.getScaleFactor();
					// Distance Check
					if (scaledPartVec3d.distanceTo(new Vec3d(nearestPoint[0], nearestPoint[1], 0)) > vtpD) {
						boolean canJoin = true;
						AxisAlignedBB slotArea = new AxisAlignedBB(point[0], point[1], 0, point[0], point[1],
								1);
						slotArea = slotArea.grow(slotSize, slotSize, 0);
						// collision Test
						for (float[] otherSlotPlace : slotPlaceList) {
							AxisAlignedBB otherSlotArea = new AxisAlignedBB(otherSlotPlace[0],
									otherSlotPlace[1], 0, otherSlotPlace[0], otherSlotPlace[1], 1);
							otherSlotArea = otherSlotArea.grow(slotSize, slotSize, 0);
							if (otherSlotArea.intersects(slotArea)) {
								canJoin = false;
							}
						}
						if (canJoin) {
							nearestPoint = point;
						}
					}
				}
				//GL11.glColor3f(0, 1, 0);
				//GL11.glPointSize(10);
				//drawPoint(scaledPartVec3d.x, scaledPartVec3d.y);
				mc.renderEngine.bindTexture(point);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				GlStateManager.enableTexture2D();
				double pointSize=3;
				RenderHelperMW.drawTexturedRect(scaledPartVec3d.x-pointSize/2, scaledPartVec3d.y-pointSize/2, pointSize, pointSize);
				GlStateManager.disableTexture2D();
				GL11.glColor3f(0.8f, 0.8f, 0.8f);
				GL11.glLineWidth(2);
				GL11.glEnable(GL11.GL_LINE_SMOOTH);
				GlStateManager.enableBlend();
				drawLine(scaledPartVec3d.x, scaledPartVec3d.y, nearestPoint[0]+8/scaledresolution.getScaleFactor(), nearestPoint[1]+8/scaledresolution.getScaleFactor());
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GlStateManager.disableBlend();
				GL11.glLineWidth(1);
				GlStateManager.enableTexture2D();
				mc.renderEngine.bindTexture(slot_topbg);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				double topBGsize=30/scaledresolution.getScaleFactor();
				RenderHelperMW.drawTexturedRect(nearestPoint[0]-(45/scaledresolution.getScaleFactor()), nearestPoint[1]-(75/scaledresolution.getScaleFactor()), topBGsize*2, topBGsize);
				double iconSize=25/scaledresolution.getScaleFactor();
				bindAttachmentTexture(attachment);
				RenderHelperMW.drawTexturedRect(nearestPoint[0]-(40/scaledresolution.getScaleFactor()), nearestPoint[1]-(71.5/scaledresolution.getScaleFactor()), iconSize, iconSize);
				
				GlStateManager.disableTexture2D();
				GL11.glColor3f(1, 1, 1);
				//GL11.glPointSize(50);
				
				for(GuiButton button:this.buttonList) {
					if(button instanceof TextureButton) {
						TextureButton tb=(TextureButton) button;
						if(tb.getType().equals(TextureButton.TypeEnum.Slot)&&tb.getAttachmentType()==attachment) {
							tb.x=nearestPoint[0]-(tb.width/2);//*sFactor/scaledresolution.getScaleFactor()
							tb.y=nearestPoint[1]-(tb.height/2);
							tb.visible=true;
							TextureButton sideButton=getButton(tb.id+sideButtonIdOffset);
							sideButton.hidden=true;
							sideButton.x=nearestPoint[0]+(tb.width)/1.9d;
							sideButton.y=nearestPoint[1]-(tb.height/2);
						}
					}
				}
				//drawPoint(nearestPoint[0], nearestPoint[1]);
				slotPlaceList.add(nearestPoint);
				// exceptParts.addAll(config.attachmentGroup.get(attachment.typeName).hidePart);
			}
		}

		// glEnd();
		//GL11.glColor3f(1, 0, 0);
		// drawPoint(scaledresolution.getScaledWidth_double()/2,scaledresolution.getScaledHeight_double()/2);
		GlStateManager.enableTexture2D();
		for(GuiButton button:this.buttonList) {
			if(button instanceof TextureButton) {
				TextureButton tb=(TextureButton) button;
				if(tb.getType().equals(TextureButton.TypeEnum.SideButton))
					button.drawButton(mc, mouseX, mouseY, partialTicks);
			}
		}
		for(GuiButton button:this.buttonList) {
			if(button instanceof TextureButton) {
				TextureButton tb=(TextureButton) button;
				if(tb.getType().equals(TextureButton.TypeEnum.Slot))
					button.drawButton(mc, mouseX, mouseY, partialTicks);
			}
		}
		drawButtonSubPage(mouseX, mouseY, partialTicks);
	}
	public void drawSubPage(int mouseX, int mouseY,float partialTicks,double sFactor,ScaledResolution scaledresolution) {
		BaseType type = ((BaseItem) this.currentModify.getItem()).baseType;
		GunType gunType = (GunType) type;
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableTexture2D();
		mc.renderEngine.bindTexture(statu);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		RenderHelperMW.drawTexturedRect(subPageX , subPageY , subPageWidth,subPageHeight+512);//临时增加测试
		int color=0xFFFFFF;
		double fontScale=2d*sFactor/scaledresolution.getScaleFactor();
		GlStateManager.pushMatrix();
		GlStateManager.scale(fontScale, fontScale, fontScale);
		RenderHelperMW.renderCenteredText(this.currentModify.getDisplayName(), (int)((subPageX+subPageWidth/2)/fontScale), (int)(subPageY/fontScale), color);
		ArrayList<String> toolTips=new ArrayList();
		toolTips.add("Weapon status");
		String baseDisplayLine = "%bFire Mode: %g%s";
        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
        baseDisplayLine = baseDisplayLine.replaceAll("%g", TextFormatting.GRAY.toString());
        toolTips.add(String.format(baseDisplayLine, GunType.getFireMode(currentModify) != null ? GunType.getFireMode(currentModify) : gunType.fireModes[0]));
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        String damageLine = "%bDamage: %g%s";
        damageLine = damageLine.replaceAll("%b", TextFormatting.BLUE.toString());
        damageLine = damageLine.replaceAll("%g", TextFormatting.RED.toString());
        if (gunType.numBullets > 1) {
        	toolTips.add(String.format(damageLine, gunType.gunDamage + " x " + gunType.numBullets));
        } else {
        	toolTips.add(String.format(damageLine, gunType.gunDamage));
        }
        String accuracyLine = "%bAccuracy: %g%s";
        accuracyLine = accuracyLine.replaceAll("%b", TextFormatting.BLUE.toString());
        accuracyLine = accuracyLine.replaceAll("%g", TextFormatting.RED.toString());
        toolTips.add(String.format(accuracyLine, decimalFormat.format((1 / gunType.bulletSpread) * 100) + "%"));
		if (gunType.acceptedAttachments != null) {
            if (!gunType.acceptedAttachments.isEmpty()) {
            	toolTips.add("Accepted attachments");
            	for (AttachmentPresetEnum attachmentType : AttachmentPresetEnum.values()) {
            		if(attachmentType!=AttachmentPresetEnum.Skin) {
            			toolTips.add(TextFormatting.BLUE.toString() + attachmentType.typeName+":");
            			for (ArrayList<String> strings : gunType.acceptedAttachments.values()) {
                            for (int i = 0; i < strings.size(); i++) {
                                try {
                                	ItemAttachment itemAtt=ModularWarfare.attachmentTypes.get(strings.get(i));
                                	if(itemAtt.type.attachmentType==attachmentType) {
                                		final String attachment = itemAtt.type.displayName;
                                        if (attachment != null) {
                                        	toolTips.add(" " + attachment);
                                        }
                                	}
                                } catch (NullPointerException error) {
                                }
                            }
                        }
            		}else if(gunType.modelSkins!=null&&gunType.modelSkins.length>1) {
            			toolTips.add(TextFormatting.BLUE.toString() + attachmentType.typeName+":");
            			for (int j = 1; j < gunType.modelSkins.length; j++) {
            				toolTips.add(" " + gunType.modelSkins[j].internalName);
                        }
            		}
            	}
            }
        }
		if (gunType.acceptedAmmo != null) {
			toolTips.add("" + TextFormatting.BLUE.toString() + "Accepted mags:");
            if (gunType.acceptedAmmo.length > 0) {
                for (String internalName : gunType.acceptedAmmo) {
                    if (ModularWarfare.ammoTypes.containsKey(internalName)) {
                        final String magName = ModularWarfare.ammoTypes.get(internalName).type.displayName;
                        if (magName != null) {
                        	toolTips.add("" + magName);
                        }
                    }
                }
            }
        }

        if (gunType.acceptedBullets != null) {
        	toolTips.add("" + TextFormatting.BLUE.toString() + "Accepted bullets:");

            if (gunType.acceptedBullets.length > 0) {
                for (String internalName : gunType.acceptedBullets) {
                    if (ModularWarfare.bulletTypes.containsKey(internalName)) {
                        final String magName = ModularWarfare.bulletTypes.get(internalName).type.displayName;
                        if (magName != null) {
                        	toolTips.add("" + magName);
                        }
                    }
                }
            }
        }
        
		double indexY=1;
		int limitWidth=128;
		for(String str:toolTips) {
			int strW=mc.fontRenderer.getStringWidth(str);
			while(strW>limitWidth) {
        		str=str.substring(0, str.length()-1);
        		strW=mc.fontRenderer.getStringWidth(str);
        	}
			RenderHelperMW.renderText(str, (int)(0), (int)(indexY*10), color);
			indexY++;
		}
		GlStateManager.popMatrix();
		//
	}
	public void drawButtonSubPage(int mouseX, int mouseY,float partialTicks) {
			//pos update
			int indexX=0;
			int indexY=0;
			int maxX=0;
			TextureButton parentButton=null;
			if(selectedSideButton!=null)
			{
				parentButton=this.getButton(selectedSideButton.id-this.sideButtonIdOffset);
				//skin cannot be remove,begin from first line
				boolean isSkin=parentButton.getAttachmentType()==AttachmentPresetEnum.Skin;
				indexY=isSkin?1:2;
				for(GuiButton button2:this.buttonList) {
					if(button2 instanceof TextureButton) {
						TextureButton tb2=(TextureButton) button2;
						if(tb2.getType().equals(TextureButton.TypeEnum.SubSlot)) {
							if(tb2.getItemStack().isEmpty()) {
								tb2.x=parentButton.x;
								tb2.y=parentButton.y+parentButton.height*1.05D;
								
							}else {
								tb2.x=parentButton.x+(parentButton.width*1.05D*indexX);
								tb2.y=parentButton.y+(parentButton.height*1.05D*indexY);
								indexX++;
								if(maxX<indexX)
								{
									maxX=indexX;
								}
								if(indexX%6==0)
								{
									indexX=0;
									indexY++;
								}
							}
						}
					}
				}
			}
			if(indexX%6==0)
			{
				indexY--;
			}
			GlStateManager.disableDepth();
			//background
			if(parentButton!=null) {
				GlStateManager.color(0, 0, 0, 1);
				GlStateManager.disableTexture2D();
				RenderHelperMW.drawTexturedRect(parentButton.x , parentButton.y+parentButton.height , parentButton.width*1.05D*maxX, parentButton.height*1.07D*indexY);
				GlStateManager.enableTexture2D();
			}
			
			for(GuiButton button:this.buttonList) {
				if(button instanceof TextureButton) {
					TextureButton tb=(TextureButton) button;
					if(tb.getType().equals(TextureButton.TypeEnum.SubSlot))
						tb.drawButton(mc, mouseX, mouseY, partialTicks);
				}
			}
			GlStateManager.enableDepth();
	}
	public TextureButton getButton(int id) {
		for(GuiButton button:this.buttonList) {
			if(button.id==id)
				return (TextureButton) button;
		}
		return null;
	}
	public void bindAttachmentTexture(AttachmentPresetEnum attachment) {
		switch(attachment) {
		case Barrel:
			mc.renderEngine.bindTexture(barrel);
			break;
		case Charm:
			mc.renderEngine.bindTexture(charm);
			break;
		case Flashlight:
			mc.renderEngine.bindTexture(flashlight);
			break;
		case Grip:
			mc.renderEngine.bindTexture(grip);
			break;
		case Sight:
			mc.renderEngine.bindTexture(sight);
			break;
		case Skin:
			mc.renderEngine.bindTexture(sprays);
			break;
		case Slide:
			
			break;
		case Stock:
			mc.renderEngine.bindTexture(stock);
			break;
		default:
			break;
		}
	}
	public void renderBasicModel(EntityLivingBase entitylivingbaseIn,BaseType type,double scale,ItemStack itemstack,double sFactor,ScaledResolution scaledresolution,float partialTicks) {
		// this.translateToHand(EnumHandSide.RIGHT);
		// GlStateManager.translate(-0.06, 0.38, -0.02);

		if (false) {
			if (ClientRenderHooks.customRenderers[type.id] != null) {
				ClientRenderHooks.customRenderers[type.id].renderItem(CustomItemRenderType.ENTITY, null, itemstack,
						entitylivingbaseIn.world, entitylivingbaseIn, partialTicks);
			}
		} else {
			ItemGun gun = (ItemGun) itemstack.getItem();
			GunType gunType = gun.type;
			ModelGun model = (ModelGun) gunType.model;
			float modelScale = (model != null) ? model.config.extra.modelScale : 1f;
			GlStateManager.disableCull();
			RenderHelper.enableStandardItemLighting();
			GlStateManager.enableDepth();
			GlStateManager.translate(0, 0, 600);
			double centerOffsetY = 0;
			double centerOffsetX = 0;
			centerOffsetX = model.config.extra.modelGuiRotateCenter.x;
			centerOffsetY=model.config.extra.modelGuiRotateCenter.y;
			double basicS=300;
			scale = basicS*model.config.extra.modelGuiScale * sFactor / scaledresolution.getScaleFactor();
			GlStateManager.translate(
					(scaledresolution.getScaledWidth() / 2) ,
					(scaledresolution.getScaledHeight() / 2) , 0);
			GlStateManager.scale(scale, scale, -scale);
			GlStateManager.scale(modelScale * 0.8, modelScale * 0.8, modelScale * 0.8);
			GlStateManager.rotate(180, 0, 0, 1);
			GlStateManager.rotate((float) rotateY, 0, 1, 0);
			GlStateManager.rotate((float) rotateZ, 1, 0, 0);
			GlStateManager.color(1, 1, 1);
			GlStateManager.translate(centerOffsetX,centerOffsetY,0);
			float worldScale = 1F / 16F;
			if (model != null) {
				int skinId = 0;
				if (itemstack.hasTagCompound()) {
					if (itemstack.getTagCompound().hasKey("skinId")) {
						skinId = itemstack.getTagCompound().getInteger("skinId");
					}
				}

				String path = skinId > 0 ? gunType.modelSkins[skinId].getSkin() : gunType.modelSkins[0].getSkin();
				ClientRenderHooks.customRenderers[1].bindTexture("guns", path);
				model.renderPart("gunModel", worldScale);
				model.renderPart("slideModel", worldScale);
				model.renderPart("boltModel", worldScale);
				model.renderPart("defaultBarrelModel", worldScale);
				model.renderPart("defaultStockModel", worldScale);
				model.renderPart("defaultGripModel", worldScale);
				model.renderPart("defaultGadgetModel", worldScale);
				if (ItemGun.hasAmmoLoaded(itemstack)) {
					model.renderPart("ammoModel", worldScale);
				}

				boolean hasScopeAttachment = false;
				GlStateManager.pushMatrix();
				for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
					GlStateManager.pushMatrix();
					ItemStack itemStack = GunType.getAttachment(itemstack, attachment);
					if (itemStack != null && itemStack.getItem() != Items.AIR) {
						AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
						ModelAttachment attachmentModel = (ModelAttachment) attachmentType.model;
						if (attachmentType.attachmentType == AttachmentPresetEnum.Sight)
							hasScopeAttachment = true;
						
						
						boolean drawEdge=false;
						for(GuiButton button:this.buttonList) {
							TextureButton tb=(TextureButton) button;
							if(tb.getType().equals(TextureButton.TypeEnum.Slot)&&tb.isMouseOver()&&tb.getAttachmentType()==attachment) {
								drawEdge=true;
							}
						}
//						
						if (attachmentModel != null) {
							//draw attachment edge
							if(drawEdge) {//
								//GL11.glPolygonOffset(-1.0f, -1.0f);
								GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
								GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
								GlStateManager.glLineWidth(10f);
								GlStateManager.pushMatrix();
								//GlStateManager.translate(0, -0.03d, 0);
								//GlStateManager.scale(1.02d, 1.02d, 1.02d);
								
								GlStateManager.color(1, 1, 1);
								GlStateManager.disableLighting();
								GlStateManager.disableTexture2D();
								GlStateManager.disableDepth();
								renderAttachModel(attachmentModel, attachment, model, attachmentType, worldScale, itemStack, skinId, path);
								GlStateManager.popMatrix();
								GlStateManager.enableDepth();
								GlStateManager.enableLighting();
								GlStateManager.enableTexture2D();
								GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
							}
							renderAttachModel(attachmentModel, attachment, model, attachmentType, worldScale, itemStack, skinId, path);
						}
					}
					GlStateManager.popMatrix();
				}
				if (!hasScopeAttachment)
					model.renderPart("defaultScopeModel", worldScale);

				GlStateManager.popMatrix();
			}
			//GlStateManager.popMatrix();
		}
		RenderHelper.disableStandardItemLighting();
	}
	public void renderAttachModel(ModelAttachment attachmentModel,AttachmentPresetEnum attachment,ModelGun model,AttachmentType attachmentType,float worldScale,ItemStack itemStack,int skinId,String path) {
		Vector3f adjustedScale = new Vector3f(attachmentModel.config.extra.modelScale,
				attachmentModel.config.extra.modelScale, attachmentModel.config.extra.modelScale);
		GL11.glScalef(adjustedScale.x, adjustedScale.y, adjustedScale.z);
		if (model.config.attachments.attachmentPointMap != null
				&& model.config.attachments.attachmentPointMap.size() >= 1) {
			if (model.config.attachments.attachmentPointMap.containsKey(attachment)) {
				Vector3f attachmentVecTranslate = model.config.attachments.attachmentPointMap
						.get(attachment).get(0);
				Vector3f attachmentVecRotate = model.config.attachments.attachmentPointMap
						.get(attachment).get(1);
				GL11.glTranslatef(
						attachmentVecTranslate.x / attachmentModel.config.extra.modelScale,
						attachmentVecTranslate.y / attachmentModel.config.extra.modelScale,
						attachmentVecTranslate.z / attachmentModel.config.extra.modelScale);

				GL11.glRotatef(attachmentVecRotate.x, 1F, 0F, 0F); // ROLL LEFT-RIGHT
				GL11.glRotatef(attachmentVecRotate.y, 0F, 1F, 0F); // ANGLE LEFT-RIGHT
				GL11.glRotatef(attachmentVecRotate.z, 0F, 0F, 1F); // ANGLE UP-DOWN
			}
		}
		if (model.config.attachments.positionPointMap != null) {
			for (String internalName : model.config.attachments.positionPointMap.keySet()) {
				if (internalName.equals(attachmentType.internalName)) {
					Vector3f trans = model.config.attachments.positionPointMap.get(internalName)
							.get(0);
					Vector3f rot = model.config.attachments.positionPointMap.get(internalName)
							.get(1);
					GL11.glTranslatef(
							trans.x / attachmentModel.config.extra.modelScale * worldScale,
							trans.y / attachmentModel.config.extra.modelScale * worldScale,
							trans.z / attachmentModel.config.extra.modelScale * worldScale);

					GL11.glRotatef(rot.x, 1F, 0F, 0F); // ROLL LEFT-RIGHT
					GL11.glRotatef(rot.y, 0F, 1F, 0F); // ANGLE LEFT-RIGHT
					GL11.glRotatef(rot.z, 0F, 0F, 1F); // ANGLE UP-DOWN
				}
			}
		}
		skinId = 0;
		if (itemStack.hasTagCompound()) {
			if (itemStack.getTagCompound().hasKey("skinId")) {
				skinId = itemStack.getTagCompound().getInteger("skinId");
			}
		}
		if (attachmentType.sameTextureAsGun) {
			ClientRenderHooks.customRenderers[3].bindTexture("guns", path);
		} else {
			path = skinId > 0 ? attachmentType.modelSkins[skinId].getSkin()
					: attachmentType.modelSkins[0].getSkin();
			ClientRenderHooks.customRenderers[3].bindTexture("attachments", path);
		}
		attachmentModel.renderAttachment(worldScale);
	}
	public void renderEnhancedModel(EntityLivingBase entitylivingbaseIn,BaseType type,double scale,ItemStack itemstack,double sFactor,ScaledResolution scaledresolution,float partialTicks) {

		// ScaledResolution scaledresolution = new
		// ScaledResolution(Minecraft.getMinecraft());
		// System.out.println(scaledresolution.getScaledWidth()+","+mc.displayWidth);
	    
		GunType gunType = (GunType) type;
		EnhancedModel model = type.enhancedModel;
		GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) gunType.enhancedModel.config;

		if (config.animations.containsKey(AnimationType.DEFAULT)) {

			RenderGunEnhanced rge = ((RenderGunEnhanced) ClientRenderHooks.customRenderers[0]);
			// model.updateAnimation(rge.controller.getTime(),"");
			//rge.controller.reset(true);
			rge.controller.updateCurrentItem();
			rge.controller.DRAW = 1.0d;
			// AnimationController.ADS = 1.0d;
			rge.controller.updateActionAndTime();
			model.updateAnimation(rge.controller.getTime(),true);
			HashSet<String> exceptParts = new HashSet<String>();
			exceptParts.addAll(config.defaultHidePart);
			// exceptParts.addAll(DEFAULT_EXCEPT);

			EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine((EntityPlayer) entitylivingbaseIn);
			/**
			 * ATTACHMENT AIM
			 */
			ItemAttachment sight = null;
			float renderInsideGunOffset = 5;
			if (GunType.getAttachment(itemstack, AttachmentPresetEnum.Sight) != null) {
				sight = (ItemAttachment) GunType.getAttachment(itemstack, AttachmentPresetEnum.Sight).getItem();
				Attachment sightConfig = config.attachment.get(sight.type.internalName);
				if (sightConfig != null) {
					// System.out.println("test");
					float ads = (float) rge.controller.ADS;
//                     mat.translate((Vector3f) new Vector3f(sightConfig.sightAimPosOffset).scale(ads));
//                     mat.rotate(ads * sightConfig.sightAimRotOffset.y * 3.14f / 180, new Vector3f(0, 1, 0));
//                     mat.rotate(ads * sightConfig.sightAimRotOffset.x * 3.14f / 180, new Vector3f(1, 0, 0));
//                     mat.rotate(ads * sightConfig.sightAimRotOffset.z * 3.14f / 180, new Vector3f(0, 0, 1));
					renderInsideGunOffset = sightConfig.renderInsideGunOffset;
				}
			}

			for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
				ItemStack itemStack = GunType.getAttachment(itemstack, attachment);
				if (itemStack != null && itemStack.getItem() != Items.AIR) {
					AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
					String binding = "gunModel";
					if (config.attachmentGroup.containsKey(attachment.typeName)) {
						if (config.attachmentGroup.get(attachment.typeName).hidePart != null) {
							exceptParts.addAll(config.attachmentGroup.get(attachment.typeName).hidePart);
						}
					}
					if (config.attachment.containsKey(attachmentType.internalName)) {
						if (config.attachment.get(attachmentType.internalName).hidePart != null) {
							exceptParts.addAll(config.attachment.get(attachmentType.internalName).hidePart);
						}
					}
				}
			}

			for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
				ItemStack itemStack = GunType.getAttachment(itemstack, attachment);
				if (itemStack != null && itemStack.getItem() != Items.AIR) {
					AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
					String binding = "gunModel";
					if (config.attachmentGroup.containsKey(attachment.typeName)) {
						if (config.attachmentGroup.get(attachment.typeName).showPart != null) {
							exceptParts.removeAll(config.attachmentGroup.get(attachment.typeName).showPart);
						}
					}
					if (config.attachment.containsKey(attachmentType.internalName)) {
						if (config.attachment.get(attachmentType.internalName).showPart != null) {
							exceptParts.removeAll(config.attachment.get(attachmentType.internalName).showPart);
						}
					}
				}
			}

			exceptParts.addAll(RenderGunEnhanced.DEFAULT_EXCEPT);

			HashSet<String> exceptPartsRendering = exceptParts;
			int skinId = 0;
			if (itemstack.hasTagCompound()) {
				if (itemstack.getTagCompound().hasKey("skinId")) {
					skinId = itemstack.getTagCompound().getInteger("skinId");
				}
			}

			String gunPath = skinId > 0 ? gunType.modelSkins[skinId].getSkin() : gunType.modelSkins[0].getSkin();
			// ClientRenderHooks.customRenderers[0].bindTexture("guns", gunPath);
//             String[] RIGHT_HAND_PART=new String[]{
//                     "rightArmModel", "rightArmLayerModel"
//             };
//             String[] RIGHT_SLIM_HAND_PART=new String[]{
//                     "rightArmSlimModel", "rightArmLayerSlimModel"
//             };
//             if(!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
//                 model.renderPart(RIGHT_HAND_PART);
//             }else {
//                 model.renderPart(RIGHT_SLIM_HAND_PART);
//             }
			GlStateManager.disableCull();
			GlStateManager.enableDepth();
			// GlStateManager.disableDepth();
			// GlStateManager.translate(130, 100, 130);
			// GlStateManager.enableLighting();
			// GlStateManager.disableLighting();
			// RenderHelper.enableStandardItemLighting();
			// RenderHelper.disableStandardItemLighting();
			GlStateManager.translate(0, 0, 600);

			// GlStateManager.rotate(180, 0, 1, 0);

			
			scale = 70*config.extra.modelGuiScale * sFactor / scaledresolution.getScaleFactor();
			double centerOffsetY = config.extra.modelGuiRotateCenter.x;//-8
			double centerOffsetX = config.extra.modelGuiRotateCenter.y;//12
			// GlStateManager.translate(-25, 15, 0);
			GlStateManager.translate(
					(scaledresolution.getScaledWidth() / 2) ,//+ (centerOffsetX * scaledresolution.getScaleFactor())
					(scaledresolution.getScaledHeight() / 2) , 0);//+ (centerOffsetY * scaledresolution.getScaleFactor())
			GlStateManager.scale(scale, scale, -scale);
			// GlStateManager.translate(25, 10, 0);
			GlStateManager.rotate(180, 1, 0, 0);
			GlStateManager.rotate(180, 0, 1, 0);
			// GlStateManager.rotate((float) -90, 0, 1, 0);
			GlStateManager.rotate((float) rotateY, 0, 1, 0);
			// GlStateManager.rotate((float) autoRotate, 0, 0, 1);
			GlStateManager.rotate((float) rotateZ, 1, 0, 0);
			GlStateManager.translate(centerOffsetY,centerOffsetX,0);
			GlStateManager.disableCull();
			final ItemAttachment sightRendering = sight;
			float worldScale = 1F;
			boolean applySprint = false;
			rge.applySprintHandTransform(model, config.sprint.basicSprint, rge.controller.getTime(),
					rge.controller.getSprintTime(), (float) rge.controller.SPRINT, "sprint_righthand", applySprint,
					() -> {
						if (true) {// isRenderHand0
							if (sightRendering != null) {
								String binding = "gunModel";
								if (config.attachment.containsKey(sightRendering.type.internalName)) {
									binding = config.attachment.get(sightRendering.type.internalName).binding;
								}

								model.applyGlobalTransformToOther(binding, () -> {
									rge.renderAttachment(config, AttachmentPresetEnum.Sight.typeName,
											sightRendering.type.internalName, () -> {
												rge.writeScopeGlassDepth(sightRendering.type,
														(ModelAttachment) sightRendering.type.model,
														rge.controller.ADS > 0, worldScale,
														sightRendering.type.sight.modeType.isPIP);
											});
								});
							}

							/**
							 * gun
							 */
							rge.bindTexture("guns", gunPath);
							model.renderPartExcept(exceptPartsRendering);
							// model.renderPart(controller.getTime(),"flashModel", "gunModel");

							/**
							 * selecotr
							 */
							WeaponFireMode fireMode = GunType.getFireMode(itemstack);
							if (fireMode == WeaponFireMode.SEMI) {
								model.renderPart("selector_semi");
							} else if (fireMode == WeaponFireMode.FULL) {
								model.renderPart("selector_full");
							} else if (fireMode == WeaponFireMode.BURST) {
								model.renderPart("selector_brust");
							}

							/**
							 * ammo and bullet
							 */
							boolean flagDynamicAmmoRendered = false;
							ItemStack stackAmmo = new ItemStack(itemstack.getTagCompound().getCompoundTag("ammo"));
							ItemStack orignalAmmo = stackAmmo;
							stackAmmo = rge.controller.getRenderAmmo(stackAmmo);
							ItemStack renderAmmo = stackAmmo;
							ItemStack prognosisAmmo = ClientTickHandler.reloadEnhancedPrognosisAmmoRendering;

							ItemStack bulletStack = ItemStack.EMPTY;
							int currentAmmoCount = 0;

							VarBoolean defaultBulletFlag = new VarBoolean();
							defaultBulletFlag.b = true;
							boolean defaultAmmoFlag = true;

							if (gunType.acceptedBullets != null) {
								currentAmmoCount = itemstack.getTagCompound().getInteger("ammocount");
								if (anim.reloading) {
									currentAmmoCount += anim.getAmmoCountOffset(true);
								}
								bulletStack = new ItemStack(itemstack.getTagCompound().getCompoundTag("bullet"));
								if (anim.reloading) {
									bulletStack = ClientProxy.gunEnhancedRenderer.controller.getRenderAmmo(bulletStack);
								}
							} else {
								Integer currentMagcount = null;
								if (stackAmmo != null && !stackAmmo.isEmpty() && stackAmmo.hasTagCompound()) {
									if (stackAmmo.getTagCompound().hasKey("magcount")) {
										currentMagcount = stackAmmo.getTagCompound().getInteger("magcount");
									}
									currentAmmoCount = ReloadHelper.getBulletOnMag(stackAmmo, currentMagcount);
									bulletStack = new ItemStack(stackAmmo.getTagCompound().getCompoundTag("bullet"));
								}
							}
							int currentAmmoCountRendering = currentAmmoCount;

							if (bulletStack != null) {
								if (bulletStack.getItem() instanceof ItemBullet) {
									BulletType bulletType = ((ItemBullet) bulletStack.getItem()).type;
									if (bulletType.isDynamicBullet && bulletType.model != null) {
										int skinIdBullet = 0;
										if (bulletStack.hasTagCompound()) {
											if (bulletStack.getTagCompound().hasKey("skinId")) {
												skinIdBullet = bulletStack.getTagCompound().getInteger("skinId");
											}
										}
										if (bulletType.sameTextureAsGun) {
											rge.bindTexture("guns", gunPath);
										} else {
											String pathAmmo = skinIdBullet > 0
													? bulletType.modelSkins[skinIdBullet].getSkin()
													: bulletType.modelSkins[0].getSkin();
											rge.bindTexture("bullets", pathAmmo);
										}
										for (int bullet = 0; bullet < currentAmmoCount
												&& bullet < rge.BULLET_MAX_RENDER; bullet++) {
											int renderBullet = bullet;
											model.applyGlobalTransformToOther("bulletModel_" + bullet, () -> {
												rge.renderAttachment(config, "bullet", bulletType.internalName, () -> {
													bulletType.model.renderPart("bulletModel", worldScale);
												});
											});
										}
										model.applyGlobalTransformToOther("bulletModel", () -> {
											rge.renderAttachment(config, "bullet", bulletType.internalName, () -> {
												bulletType.model.renderPart("bulletModel", worldScale);
											});
										});
										defaultBulletFlag.b = false;
									}
								}
							}

							ItemStack[] ammoList = new ItemStack[] { stackAmmo, orignalAmmo, prognosisAmmo };
							String[] binddings = new String[] { "ammoModel", "ammoModelPre", "ammoModelPost" };
							if (false) {
								for (int x = 0; x < 3; x++) {
									ItemStack stackAmmoX = ammoList[x];
									if (stackAmmoX == null || stackAmmoX.isEmpty()) {
										continue;
									}
									if (!model.existPart(binddings[x])) {
										continue;
									}
									if (stackAmmoX.getItem() instanceof ItemAmmo) {
										ItemAmmo itemAmmo = (ItemAmmo) stackAmmoX.getItem();
										AmmoType ammoType = itemAmmo.type;
										if (ammoType.isDynamicAmmo && ammoType.model != null) {
											int skinIdAmmo = 0;
											int baseAmmoCount = 0;

											if (stackAmmoX.hasTagCompound()) {
												if (stackAmmoX.getTagCompound().hasKey("skinId")) {
													skinIdAmmo = stackAmmoX.getTagCompound().getInteger("skinId");
												}
												if (stackAmmoX.getTagCompound().hasKey("magcount")) {
													baseAmmoCount = (stackAmmoX.getTagCompound().getInteger("magcount")
															- 1) * ammoType.ammoCapacity;
												}
											}
											int baseAmmoCountRendering = baseAmmoCount;

											if (ammoType.sameTextureAsGun) {
												rge.bindTexture("guns", gunPath);
											} else {
												String pathAmmo = skinIdAmmo > 0
														? ammoType.modelSkins[skinIdAmmo].getSkin()
														: ammoType.modelSkins[0].getSkin();
												rge.bindTexture("ammo", pathAmmo);
											}

											if (rge.controller.shouldRenderAmmo()) {
												model.applyGlobalTransformToOther("ammoModel", () -> {
													GlStateManager.pushMatrix();
													if (renderAmmo.getTagCompound().hasKey("magcount")) {
														if (config.attachment.containsKey(itemAmmo.type.internalName)) {
															if (config.attachment.get(
																	itemAmmo.type.internalName).multiMagazineTransform != null) {
																if (renderAmmo.getTagCompound().getInteger(
																		"magcount") <= config.attachment.get(
																				itemAmmo.type.internalName).multiMagazineTransform
																						.size()) {
																	// be careful, don't mod the config
																	Transform ammoTransform = config.attachment.get(
																			itemAmmo.type.internalName).multiMagazineTransform
																					.get(renderAmmo.getTagCompound()
																							.getInteger("magcount")
																							- 1);
																	Transform renderTransform = ammoTransform;
																	if (anim.reloading && (anim
																			.getReloadAnimationType() == AnimationType.RELOAD_FIRST_QUICKLY)) {
																		float magAlpha = (float) rge.controller.RELOAD;
																		renderTransform = new Transform();
																		ammoTransform = config.attachment.get(
																				itemAmmo.type.internalName).multiMagazineTransform
																						.get(prognosisAmmo
																								.getTagCompound()
																								.getInteger("magcount")
																								- 1);
																		Transform beginTransform = config.attachment
																				.get(itemAmmo.type.internalName).multiMagazineTransform
																						.get(orignalAmmo
																								.getTagCompound()
																								.getInteger("magcount")
																								- 1);

																		renderTransform.translate.x = beginTransform.translate.x
																				+ (ammoTransform.translate.x
																						- beginTransform.translate.x)
																						* magAlpha;
																		renderTransform.translate.y = beginTransform.translate.y
																				+ (ammoTransform.translate.y
																						- beginTransform.translate.y)
																						* magAlpha;
																		renderTransform.translate.z = beginTransform.translate.z
																				+ (ammoTransform.translate.z
																						- beginTransform.translate.z)
																						* magAlpha;

																		renderTransform.rotate.x = beginTransform.rotate.x
																				+ (ammoTransform.rotate.x
																						- beginTransform.rotate.x)
																						* magAlpha;
																		renderTransform.rotate.y = beginTransform.rotate.y
																				+ (ammoTransform.rotate.y
																						- beginTransform.rotate.y)
																						* magAlpha;
																		renderTransform.rotate.z = beginTransform.rotate.z
																				+ (ammoTransform.rotate.z
																						- beginTransform.rotate.z)
																						* magAlpha;

																		renderTransform.scale.x = beginTransform.scale.x
																				+ (ammoTransform.scale.x
																						- beginTransform.scale.x)
																						* magAlpha;
																		renderTransform.scale.y = beginTransform.scale.y
																				+ (ammoTransform.scale.y
																						- beginTransform.scale.y)
																						* magAlpha;
																		renderTransform.scale.z = beginTransform.scale.z
																				+ (ammoTransform.scale.z
																						- beginTransform.scale.z)
																						* magAlpha;
																	}
																	GlStateManager.translate(
																			renderTransform.translate.x,
																			renderTransform.translate.y,
																			renderTransform.translate.z);
																	GlStateManager.scale(renderTransform.scale.x,
																			renderTransform.scale.y,
																			renderTransform.scale.z);
																	GlStateManager.rotate(renderTransform.rotate.y, 0,
																			1, 0);
																	GlStateManager.rotate(renderTransform.rotate.x, 1,
																			0, 0);
																	GlStateManager.rotate(renderTransform.rotate.z, 0,
																			0, 1);
																}
															}
														}
													}
													rge.renderAttachment(config, "ammo", ammoType.internalName, () -> {
														ammoType.model.renderPart("ammoModel", worldScale);
														if (defaultBulletFlag.b) {
															if (renderAmmo.getTagCompound().hasKey("magcount")) {
																for (int i = 1; i <= ammoType.magazineCount; i++) {
																	int count = ReloadHelper.getBulletOnMag(renderAmmo,
																			i);
																	for (int bullet = 0; bullet < count
																			&& bullet < rge.BULLET_MAX_RENDER; bullet++) {
																		// System.out.println((ammoType.ammoCapacity*(i-1))+bullet);
																		ammoType.model.renderPart("bulletModel_"
																				+ ((ammoType.ammoCapacity * (i - 1))
																						+ bullet),
																				worldScale);
																	}
																}
															} else {
																for (int bullet = 0; bullet < currentAmmoCountRendering
																		&& bullet < rge.BULLET_MAX_RENDER; bullet++) {
																	ammoType.model.renderPart(
																			"bulletModel_"
																					+ (baseAmmoCountRendering + bullet),
																			worldScale);
																}
															}

															defaultBulletFlag.b = false;
														}
													});
													GlStateManager.popMatrix();
												});
												model.applyGlobalTransformToOther("bulletModel", () -> {
													rge.renderAttachment(config, "bullet", ammoType.internalName,
															() -> {
																ammoType.model.renderPart("bulletModel", worldScale);
															});
												});
												flagDynamicAmmoRendered = true;
												defaultAmmoFlag = false;
											}
										}
									}
								}
							}

							/**
							 * default bullet and ammo
							 */

							rge.bindTexture("guns", gunPath);

							if (defaultBulletFlag.b) {
								for (int bullet = 0; bullet < currentAmmoCount
										&& bullet < rge.BULLET_MAX_RENDER; bullet++) {
									model.renderPart("bulletModel_" + bullet);
								}
								model.renderPart("bulletModel");
							}

							if (rge.controller.shouldRenderAmmo() && defaultAmmoFlag) {
								model.renderPart("ammoModel");
							}

							/**
							 * attachment
							 */

							for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
								ItemStack itemStack = GunType.getAttachment(itemstack, attachment);
								if (itemStack != null && itemStack.getItem() != Items.AIR) {
									AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;

									ModelAttachment attachmentModel = (ModelAttachment) attachmentType.model;
									if (ScopeUtils.isIndsideGunRendering) {
										if (attachment == AttachmentPresetEnum.Sight) {
											if (config.attachment.containsKey(attachmentType.internalName)) {
												if (!config.attachment
														.get(attachmentType.internalName).renderInsideSightModel) {
													continue;
												}
											} else {
												continue;
											}
										}
									}

									if (attachmentModel != null) {
										String binding = "gunModel";
										if (config.attachment.containsKey(attachmentType.internalName)) {
											binding = config.attachment.get(attachmentType.internalName).binding;
										}
										model.applyGlobalTransformToOther(binding, () -> {
											boolean drawEdge=false;
											for(GuiButton button:this.buttonList) {
												TextureButton tb=(TextureButton) button;
												if(tb.getType().equals(TextureButton.TypeEnum.Slot)&&tb.isMouseOver()&&tb.getAttachmentType()==attachment) {
													drawEdge=true;
												}
											}
											//draw attachment edge
											if(drawEdge) {
												//GL11.glPolygonOffset(-1.0f, -1.0f);
												GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
												GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
												GlStateManager.glLineWidth(10f);
												GlStateManager.pushMatrix();
												//GlStateManager.translate(0, -0.03d, 0);
												//GlStateManager.scale(1.02d, 1.02d, 1.02d);
												
												//GlStateManager.translate(0, 1, 0);
												
												GlStateManager.color(1, 1, 1,1);
												GlStateManager.disableLighting();
												GlStateManager.disableTexture2D();
												GlStateManager.disableDepth();
												if(true) {
													rge.renderAttachment(config, attachment.typeName,
															attachmentType.internalName, () -> {
																attachmentModel.renderAttachment(worldScale);
																if (attachment == AttachmentPresetEnum.Sight) {
																	rge.renderScopeGlass(attachmentType, attachmentModel,
																			rge.controller.ADS > 0, worldScale);
																}
															});
												}
												GlStateManager.color(1, 1, 1,1);
												GlStateManager.popMatrix();
												GlStateManager.enableDepth();
												GlStateManager.enableLighting();
												GlStateManager.enableTexture2D();
												GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
												GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
												
												//GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_FILL);
								                //GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_FILL);
											}
											if (attachmentType.sameTextureAsGun) {
												rge.bindTexture("guns", gunPath);
											} else {
												int attachmentsSkinId = 0;
												if (itemStack.hasTagCompound()) {
													if (itemStack.getTagCompound().hasKey("skinId")) {
														attachmentsSkinId = itemStack.getTagCompound()
																.getInteger("skinId");
													}
												}
												String attachmentsPath = attachmentsSkinId > 0
														? attachmentType.modelSkins[attachmentsSkinId].getSkin()
														: attachmentType.modelSkins[0].getSkin();
												rge.bindTexture("attachments", attachmentsPath);
											}
											rge.renderAttachment(config, attachment.typeName,
													attachmentType.internalName, () -> {
														attachmentModel.renderAttachment(worldScale);
														if (attachment == AttachmentPresetEnum.Sight) {
															rge.renderScopeGlass(attachmentType, attachmentModel,
																	rge.controller.ADS > 0, worldScale);
														}
													});
											
										});
									}
								}
							}
							/**
							 * flashmodel
							 */
						}
					});

			// basic model
			// model.renderPartExcept(exceptParts);

			if (sightRendering != null) {
				if (!ScopeUtils.isIndsideGunRendering) {
					if (!OptifineHelper.isShadersEnabled()) {
						rge.copyMirrorTexture();
						ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, true, 1);
						rge.eraseScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model,
								rge.controller.ADS > 0, 1);// worldScale
					} else if (sightRendering.type.sight.modeType.isPIP) {
						if (false) {// isRenderHand0
							// nothing to do
							// ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, false, 1);
						}
					} else {
						if (true) {// isRenderHand0
							GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);

							GL11.glDepthRange(0, 1);
							rge.copyMirrorTexture();
							ClientProxy.scopeUtils.renderPostScope(partialTicks, true, false, true, 1);
							rge.eraseScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model,
									rge.controller.ADS > 0, 1);// worldScale
							rge.writeScopeSoildDepth(rge.controller.ADS > 0);

							GL11.glPopAttrib();
						} else {
							ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, true, 1);
						}
					}
				}
			}
			rge.controller.ADS = 0;
		}

	}

	public void drawLine(double x1, double y1, double x2, double y2) {
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(x1, y1, 0);
		GL11.glVertex3d(x2, y2, 0);
		GL11.glEnd();
	}

	public void drawPoint(double x, double y) {
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex3d(x, y, 0);
		GL11.glEnd();
	}
	@Override
	protected void actionPerformed(GuiButton button) throws IOException
    {
		if(clickOnce)
			return;
		clickOnce=true;
		ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
		if(button instanceof TextureButton) {
			TextureButton tButton=(TextureButton) button;
			if(tButton.getType().equals(TextureButton.TypeEnum.SideButton)) {
				if(tButton.state==0) {
					selectedSideButton=null;
					tButton.state=-1;
					this.initGui();
				}else {
					if(selectedSideButton!=null) {
						this.initGui();
					}
					selectedSideButton=this.getButton(tButton.id);
					selectedSideButton.state=0;
					this.joinSubPageButtons(mc.player,scaledresolution);
				}				
			}
			else if(tButton.getType().equals(TextureButton.TypeEnum.SubSlot)) {
				//unload button
				if(tButton.id==-1) {
					ModularWarfare.NETWORK.sendToServer(new PacketGunUnloadAttachment(tButton.getAttachmentType().getName(), false));
					this.actionPerformed(this.selectedSideButton);
				}else {
					int inventoryID=tButton.id-this.subSlotIdOffset;
					if (GunType.getAttachment(this.currentModify, tButton.getAttachmentType()) != mc.player.inventory.getStackInSlot(inventoryID)) {
                        ModularWarfare.NETWORK.sendToServer(new PacketGunAddAttachment(inventoryID));
                        this.actionPerformed(this.selectedSideButton);
                    }
					
				}
				
			}else if(button==QUITBUTTON) {
				ModularWarfare.PROXY.playSound(new MWSound(mc.player.getPosition(), "attachment.open", 1f, 1f));
				Minecraft.getMinecraft().displayGuiScreen(null);
			}else if(button==PAGEBUTTON) {
				PAGEBUTTON.state=PAGEBUTTON.state==-1?0:-1;
				this.updateItemModifying();
				this.initGui();
			}
			//tButton.
		}
    }

	@Override
	public void onGuiClosed() {

	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		updateRotate();
	}

	public void updateRotate() {
		// rotateY;
		// rotateZ;
		int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		int k = Mouse.getEventButton();
		// lastMouseX=Mouse.getEventX();
		// System.out.println(Mouse.getEventDX() + " " + Mouse.getEventDY() + " " +
		// Mouse.getEventButton());
		if (Mouse.getEventButton() == 0) {
			if (Mouse.getEventButtonState()) {
				this.leftClicked = true;
			} else {
				this.leftClicked = false;
			}
		}
		if (this.leftClicked) {
			this.rotateY += Mouse.getEventDX() * 0.2d;
			this.rotateZ += Mouse.getEventDY() * 0.2d;
		}
		this.rotateZ = Math.max(-30, this.rotateZ);
		this.rotateZ = Math.min(30, this.rotateZ);
	}

	@Override
	protected void mouseClicked(int x, int y, int mouseEvent) throws IOException {
		super.mouseClicked(x, y, mouseEvent);
	}

	@Override
	protected void mouseReleased(int x, int y, int mouseEvent) {
		super.mouseReleased(x, y, mouseEvent);
	}

	@Override
	protected void keyTyped(char eventChar, int eventKey) {
		// esc
		if (eventKey == 1||eventKey==50) {
			try {
				actionPerformed(QUITBUTTON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	@Override
	public boolean doesGuiPauseGame()
    {
        return false;
    }

}
