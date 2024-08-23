package mchhui.hegltf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moe.komi.mwprotect.IGltfLoader;
import org.joml.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.modularwarfare.ModularWarfare;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import mchhui.hegltf.DataAnimation.DataKeyframe;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class GltfDataModel {
    protected static final GltfModelReader READER = new GltfModelReader();

    protected static final Comparator<DataKeyframe> COMPARATOR_ANI = (o1, o2) -> Float.compare(o1.time, o2.time);

    protected String lastPos = "unkown";

    // node名对动画
    public HashMap<String, DataAnimation> animations = new HashMap<String, DataAnimation>();

    // 其名对其对象
    public HashMap<String, DataMaterial> materials = new HashMap<String, DataMaterial>();
    public HashMap<String, DataNode> nodes = new HashMap<String, DataNode>();
    public HashMap<String, DataNode> rootNodes = new HashMap<String, DataNode>();
    public ArrayList<String> joints = new ArrayList<String>();
    public ArrayList<Matrix4f> inverseBindMatrices = new ArrayList<Matrix4f>();
    public String skeleton = "";

    public boolean loaded = false;

    public static GltfDataModel load(ResourceLocation loc) {
        GltfDataModel gltfDataModel = new GltfDataModel();
        InputStream inputStream;
        try {
            inputStream = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
            IGltfLoader loader;
            Class<? extends InputStream> isClass = inputStream.getClass();
            try {
                Method getGltfLoader = isClass.getMethod("getGltfLoader");
                loader = (IGltfLoader) getGltfLoader.invoke(inputStream);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                loader = new LegacyGltfLoader();
            }
            loader.loadGltf(gltfDataModel, inputStream, loc.toString());
        } catch (IOException e) {
            throw new RuntimeException("File not found:" + loc);
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return gltfDataModel;
    }

    public static int getIndice(ByteBuffer buf, int type) {
        if (type == GL11.GL_UNSIGNED_BYTE) {
            return buf.get() & 0xff;
        } else if (type == GL11.GL_UNSIGNED_SHORT) {
            return buf.getShort() & 0xffff;
        } else {
            return buf.getInt();
        }
    }

    public static void readAccessorToList(ByteBuffer buf, List list, int type) {
        readAccessorToList(buf, list, type, GL11.GL_FLOAT);
    }

    public static void readAccessorToList(ByteBuffer buf, List list, int type, int mode) {
        while (buf.hasRemaining()) {
            if (type == 2) {
                list.add(new Vector2f(buf.getFloat(), buf.getFloat()));
            } else if (type == 3) {
                list.add(new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat()));
            } else if (type == 4) {
                if (mode == GL11.GL_UNSIGNED_BYTE || mode == GL11.GL_BYTE) {
                    // 按理讲只需用0xff(1111 1111)但用多也无妨 哈哈哈
                    list.add(new Vector4i(buf.get() & 0xff, buf.get() & 0xff, buf.get() & 0xff, buf.get() & 0xff));
                } else if (mode == GL11.GL_UNSIGNED_SHORT || mode == GL11.GL_SHORT) {
                    list.add(new Vector4i(buf.getShort() & 0xffff, buf.getShort() & 0xffff, buf.getShort() & 0xffff,
                        buf.getShort() & 0xffff));
                } else if (mode == GL11.GL_UNSIGNED_INT || mode == GL11.GL_INT) {
                    // 其实没啥用 根本存不了 哈哈哈
                    list.add(new Vector4i(buf.getInt(), buf.getInt(), buf.getInt(), buf.getInt()));
                } else if (mode == GL11.GL_FLOAT) {
                    list.add(new Vector4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat()));
                } else {
                    throw new Error("意料之外的type:" + mode);
                }
            } else {
                throw new Error("意料之外的unit");
            }
        }
    }
    
    public void delete() {
        if(this.loaded) {
            this.nodes.forEach((k,v)->{
                v.meshes.forEach((n,m)->{
                    m.delete();
                });
            });
        }
    }
}
