package mchhui.hegltf;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;

public final class GltfProxyMeshBuilder {
    private GltfProxyMeshBuilder() {
    }

    public static DataMesh buildBox(List<Vector3f> positions, boolean skinned, int jointIndex) {
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        if (positions == null || positions.isEmpty()) {
            min.set(-0.01f, -0.01f, -0.01f);
            max.set(0.01f, 0.01f, 0.01f);
        } else {
            for (Vector3f p : positions) {
                min.min(p);
                max.max(p);
            }
            if (!Float.isFinite(min.x) || min.x > max.x) {
                min.set(-0.01f, -0.01f, -0.01f);
                max.set(0.01f, 0.01f, 0.01f);
            }
        }

        Vector3f[] corners = new Vector3f[] {
            new Vector3f(min.x, min.y, min.z), new Vector3f(max.x, min.y, min.z),
            new Vector3f(max.x, max.y, min.z), new Vector3f(min.x, max.y, min.z),
            new Vector3f(min.x, min.y, max.z), new Vector3f(max.x, min.y, max.z),
            new Vector3f(max.x, max.y, max.z), new Vector3f(min.x, max.y, max.z)
        };
        int[][] tris = new int[][] {
            {0, 1, 2}, {0, 2, 3}, {4, 6, 5}, {4, 7, 6},
            {0, 4, 5}, {0, 5, 1}, {2, 6, 7}, {2, 7, 3},
            {0, 3, 7}, {0, 7, 4}, {1, 5, 6}, {1, 6, 2}
        };

        DataMesh mesh = new DataMesh();
        mesh.proxy = true;
        if (skinned) {
            mesh.unit = 5;
            mesh.geoCount = 8;
            mesh.geoBuffer = BufferUtils.createByteBuffer(8 * (3 * 4 + 2 * 4 + 3 * 4 + 4 * 4 + 4 * 4 + 1 * 4));
            Vector3f center = new Vector3f(min).add(max).mul(0.5f);
            for (int i = 0; i < 8; i++) {
                Vector3f p = corners[i];
                Vector3f n = new Vector3f(p).sub(center);
                if (n.lengthSquared() < 1e-8f) {
                    n.set(0, 1, 0);
                } else {
                    n.normalize();
                }
                mesh.geoBuffer.putFloat(p.x).putFloat(p.y).putFloat(p.z);
                mesh.geoBuffer.putFloat(0.5f).putFloat(0.5f);
                mesh.geoBuffer.putFloat(n.x).putFloat(n.y).putFloat(n.z);
                mesh.geoBuffer.putInt(jointIndex).putInt(0).putInt(0).putInt(0);
                mesh.geoBuffer.putFloat(1f).putFloat(0f).putFloat(0f).putFloat(0f);
                mesh.geoBuffer.putInt(i);
            }
            mesh.elementCount = tris.length * 3;
            mesh.elementBuffer = BufferUtils.createIntBuffer(mesh.elementCount);
            for (int[] t : tris) {
                mesh.elementBuffer.put(t[0]).put(t[1]).put(t[2]);
                for (int idx : t) {
                    Vector3f p = corners[idx];
                    mesh.geoList.add(p.x);
                    mesh.geoList.add(p.y);
                    mesh.geoList.add(p.z);
                    mesh.geoList.add(0.5f);
                    mesh.geoList.add(0.5f);
                    mesh.geoList.add(0f);
                    mesh.geoList.add(1f);
                    mesh.geoList.add(0f);
                }
            }
        } else {
            mesh.unit = 3;
            for (int[] t : tris) {
                emitTri(mesh.geoList, corners[t[0]], corners[t[1]], corners[t[2]]);
            }
        }
        return mesh;
    }

    private static void emitTri(List<Float> geo, Vector3f a, Vector3f b, Vector3f c) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (n.lengthSquared() < 1e-12f) {
            n.set(0, 1, 0);
        } else {
            n.normalize();
        }
        putVert(geo, a, n);
        putVert(geo, b, n);
        putVert(geo, c, n);
    }

    private static void putVert(List<Float> geo, Vector3f p, Vector3f n) {
        geo.add(p.x);
        geo.add(p.y);
        geo.add(p.z);
        geo.add(0.5f);
        geo.add(0.5f);
        geo.add(n.x);
        geo.add(n.y);
        geo.add(n.z);
    }

    public static int dominantJoint(List<Vector4i> joints, List<Vector4f> weights) {
        if (joints == null || joints.isEmpty() || weights == null || weights.isEmpty()) {
            return 0;
        }
        Vector4i j = joints.get(0);
        Vector4f w = weights.get(0);
        float best = w.x;
        int idx = j.x;
        if (w.y > best) {
            best = w.y;
            idx = j.y;
        }
        if (w.z > best) {
            best = w.z;
            idx = j.z;
        }
        if (w.w > best) {
            idx = j.w;
        }
        return Math.max(0, idx);
    }
}
