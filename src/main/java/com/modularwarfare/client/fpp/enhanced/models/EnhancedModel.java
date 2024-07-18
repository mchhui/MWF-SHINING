package com.modularwarfare.client.fpp.enhanced.models;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.IMWModel;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.maths.MathUtils;

import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.NodeModel;
import mchhui.hegltf.DataAnimation;
import mchhui.hegltf.DataAnimation.Transform;
import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfDataModel;
import mchhui.hegltf.GltfRenderModel;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.hegltf.GltfRenderModel.NodeAnimationMapper;
import mchhui.hegltf.GltfRenderModel.NodeState;
import mchhui.hegltf.ShaderGltf;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Quaternion;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

public class EnhancedModel implements IMWModel {
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final HashMap<ResourceLocation, GltfDataModel> modelCache=new HashMap<ResourceLocation, GltfDataModel>();
    public EnhancedRenderConfig config;
    public BaseType baseType;
    public GltfRenderModel model;
    public boolean initCal = false;
    private HashMap<String, Matrix4f> invMatCache = new HashMap<String, Matrix4f>();

    public EnhancedModel(EnhancedRenderConfig config, BaseType baseType) {
        this.config = config;
        this.baseType = baseType;
        if(!modelCache.containsKey(getModelLocation())) {
            modelCache.put(getModelLocation(), GltfDataModel.load(getModelLocation()));
        }
        model = new GltfRenderModel(modelCache.get(getModelLocation()));
        updateAnimation(0);
        model.nodeStates.forEach((node,state)->{
            invMatCache.put(node, new Matrix4f(state.mat).invert());
        });
        
    }
    
    public static void clearCache() {
        modelCache.values().forEach((model)->{
            model.delete();
        });
        modelCache.clear();
    }

    public ResourceLocation getModelLocation() {
        return new ResourceLocation(ModularWarfare.MOD_ID,
            "gltf/" + baseType.getAssetDir() + "/" + this.config.modelFileName);
    }

    public void loadAnimation(EnhancedModel other,boolean skin) {
        if(model==null||other==null||other.model==null) {
            return;
        }
        model.loadAnimation(other.model,skin);
    }
    
    public void updateAnimation(float time,boolean skin) {
        invMatCache.clear();
        initCal = model.updateAnimation(time,skin||!initCal);
    }
    
    public Transform findLocalTransform(String name,float time) {
        if(model==null) {
            return null;
        }
        DataNode node=model.geoModel.nodes.get(name);
        if(node==null) {
            return null;
        }
        DataAnimation ani=model.geoModel.animations.get(name);
        if(ani==null) {
            return null;
        }
        return model.geoModel.animations.get(name).findTransform(time, node.pos, node.size, node.rot);
    }
    
    public void setAnimationCalBlender(NodeAnimationBlender blender) {
        model.setNodeAnimationCalBlender(blender);
    }
    
    public void setAnimationLoadMapper(NodeAnimationMapper mapper) {
        model.setNodeAnimationLoadMapper(mapper);
    }
    
    /**
     * 兼容旧版 请勿使用
     * */
    @Deprecated
    public void updateAnimation(float time) {
        updateAnimation(time, true);
    }

    public boolean existPart(String part) {
        return model.geoModel.nodes.containsKey(part);
    }
    
    /**
     * 兼容旧版 请勿使用
     * */
    @Deprecated
    public NodeModel getPart(String part) {
        DataNode node=model.geoModel.nodes.get(part);
        if(node==null) {
            return null;
        }
        return node.unsafeNode;
    }
    
    @Override
    public void renderPart(String part, float scale) {
        if (!initCal) {
            return;
        }
        model.renderPart(part);
    }

    public void renderPart(String part) {
        if (!initCal) {
            return;
        }
        model.renderPart(part);
    }

    public void renderPartExcept(HashSet<String> set) {
        if (!initCal) {
            return;
        }
        model.renderExcept(set);
    }

    public void renderPart(String[] only) {
        if (!initCal) {
            return;
        }
        model.renderOnly(only);
    }
    
    public Matrix4f getGlobalTransform(String name) {
        if (!initCal) {
            return new Matrix4f();
        }
        NodeState state = model.nodeStates.get(name);
        if(state==null) {
            return new Matrix4f();
        }
        return state.mat;
    }
    
    public Matrix4f getGlobalInverseTransform(String name) {
        if (!initCal) {
            return new Matrix4f();
        }
        Matrix4f invmat = invMatCache.get(name);
        if(invmat==null) {
            return new Matrix4f();
        }
        return invmat;
    }

    public void applyGlobalTransformToOther(String binding, Runnable run) {
        if (!initCal) {
            return;
        }
        NodeState state = model.nodeStates.get(binding);
        if(state==null) {
            return;
        }
        GlStateManager.pushMatrix();
        if (state != null) {
            GlStateManager.multMatrix(state.mat.get(MATRIX_BUFFER));
        }
        run.run();

        GlStateManager.popMatrix();
    }

    public void applyGlobalInverseTransformToOther(String binding, Runnable run) {
        if (!initCal) {
            return;
        }
        Matrix4f invmat = invMatCache.get(binding);
        if(invmat==null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(invmat.get(MATRIX_BUFFER));
        run.run();

        GlStateManager.popMatrix();
    }

}
