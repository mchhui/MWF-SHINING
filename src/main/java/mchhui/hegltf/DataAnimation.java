package mchhui.hegltf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Animation channels stored as packed float arrays after load.
 * Load path uses temporary {@link DataKeyframe} lists, then {@link #sortAndCompact()}.
 * Layout for TRS: interleaved [time, x, y, z, w] * count.
 */
public class DataAnimation {
    private static final float[] EMPTY = new float[0];
    private static final Comparator<DataKeyframe> KEY_CMP = new Comparator<DataKeyframe>() {
        @Override
        public int compare(DataKeyframe a, DataKeyframe b) {
            return Float.compare(a.time, b.time);
        }
    };

    /** Load-time builders; cleared by {@link #sortAndCompact()}. */
    public ArrayList<DataKeyframe> posChannel = new ArrayList<DataKeyframe>();
    public ArrayList<DataKeyframe> rotChannel = new ArrayList<DataKeyframe>();
    public ArrayList<DataKeyframe> sizeChannel = new ArrayList<DataKeyframe>();

    /** Packed TRS: [t,x,y,z,w] * count */
    public float[] posData = EMPTY;
    public int posCount;
    public float[] rotData = EMPTY;
    public int rotCount;
    public float[] sizeData = EMPTY;
    public int sizeCount;

    public float theata90 = (float) Math.toRadians(90);
    public String nodeName = null;

    public static final int STEP_TRANSLATION = 1;
    public static final int STEP_SCALE = 2;
    public static final int STEP_ROTATION = 4;

    public static Map<String, List<float[]>> currentStepRanges = null;

    private static final Transform CALC_TRANSFORM = new Transform();
    private static final Vector4f CALC_VEC = new Vector4f();
    private static final Quaternionf CALC_QUAT = new Quaternionf();

    public static boolean isStepInterval(String nodeName, float leftTime, int channelBit) {
        if (currentStepRanges == null || currentStepRanges.isEmpty())
            return false;
        List<float[]> nodeRanges = currentStepRanges.get(nodeName);
        if (nodeRanges != null) {
            for (float[] range : nodeRanges) {
                if (leftTime >= range[0] && leftTime < range[1] && ((int) range[2] & channelBit) != 0)
                    return true;
            }
        }
        List<float[]> globalRanges = currentStepRanges.get(null);
        if (globalRanges != null) {
            for (float[] range : globalRanges) {
                if (leftTime >= range[0] && leftTime < range[1] && ((int) range[2] & channelBit) != 0)
                    return true;
            }
        }
        return false;
    }

    public Transform findTransform(float time, Vector3f pos, Vector3f size, Quaternionf rot) {
        Transform transform = CALC_TRANSFORM;
        transform.pos.set(pos, 0);
        transform.size.set(size, 0);
        transform.rot.set(rot);

        sampleVecChannel(posData, posCount, time, transform.pos, CALC_VEC, STEP_TRANSLATION);
        sampleVecChannel(sizeData, sizeCount, time, transform.size, CALC_VEC, STEP_SCALE);
        sampleRotChannel(rotData, rotCount, time, transform.rot, CALC_QUAT);
        return transform;
    }

    /** Sort builders, pack floats, drop keyframe objects. */
    public void sortAndCompact() {
        posChannel.sort(KEY_CMP);
        rotChannel.sort(KEY_CMP);
        sizeChannel.sort(KEY_CMP);
        posData = packVecChannel(posChannel);
        posCount = posChannel.size();
        rotData = packVecChannel(rotChannel);
        rotCount = rotChannel.size();
        sizeData = packVecChannel(sizeChannel);
        sizeCount = sizeChannel.size();
        posChannel.clear();
        rotChannel.clear();
        sizeChannel.clear();
        posChannel.trimToSize();
        rotChannel.trimToSize();
        sizeChannel.trimToSize();
    }

    private static float[] packVecChannel(ArrayList<DataKeyframe> src) {
        int n = src.size();
        if (n == 0) {
            return EMPTY;
        }
        float[] d = new float[n * 5];
        for (int i = 0; i < n; i++) {
            DataKeyframe k = src.get(i);
            int o = i * 5;
            d[o] = k.time;
            if (k.vec != null) {
                d[o + 1] = k.vec.x;
                d[o + 2] = k.vec.y;
                d[o + 3] = k.vec.z;
                d[o + 4] = k.vec.w;
            }
        }
        return d;
    }

    private void sampleVecChannel(float[] data, int count, float time, Vector4f dest, Vector4f tmp,
        int channelBit) {
        if (count <= 0) {
            return;
        }
        int left = 0;
        int right = count - 1;
        if (time <= timeAt(data, left)) {
            setVec(data, left, dest);
            return;
        }
        if (time >= timeAt(data, right)) {
            setVec(data, right, dest);
            return;
        }
        while (true) {
            int mid = (left + right) >> 1;
            if (timeAt(data, mid) <= time) {
                left = mid;
            } else {
                right = mid;
            }
            if (left + 1 >= right) {
                break;
            }
        }
        float t0 = timeAt(data, left);
        float t1 = timeAt(data, right);
        float per = (time - t0) / (t1 - t0);
        if (per > 1) {
            per = 1;
        }
        if (isStepInterval(this.nodeName, t0, channelBit)) {
            per = 0;
        }
        setVec(data, left, dest);
        dest.mul(1 - per);
        setVec(data, right, tmp);
        tmp.mul(per);
        dest.add(tmp);
    }

    private void sampleRotChannel(float[] data, int count, float time, Quaternionf dest, Quaternionf quatTmp) {
        if (count <= 0) {
            return;
        }
        int left = 0;
        int right = count - 1;
        if (time <= timeAt(data, left)) {
            setQuat(data, left, dest);
            return;
        }
        if (time >= timeAt(data, right)) {
            setQuat(data, right, dest);
            return;
        }
        while (true) {
            int mid = (left + right) >> 1;
            if (timeAt(data, mid) <= time) {
                left = mid;
            } else {
                right = mid;
            }
            if (left + 1 >= right) {
                break;
            }
        }
        float t0 = timeAt(data, left);
        float t1 = timeAt(data, right);
        float per = (time - t0) / (t1 - t0);
        if (per > 1) {
            per = 1;
        }
        if (isStepInterval(this.nodeName, t0, STEP_ROTATION)) {
            per = 0;
        }
        setQuat(data, left, dest);
        setQuat(data, right, quatTmp);
        dest.normalize();
        quatTmp.normalize();
        dest.slerp(quatTmp, per);
    }

    private static float timeAt(float[] data, int i) {
        return data[i * 5];
    }

    private static void setVec(float[] data, int i, Vector4f dest) {
        int o = i * 5;
        dest.set(data[o + 1], data[o + 2], data[o + 3], data[o + 4]);
    }

    private static void setQuat(float[] data, int i, Quaternionf dest) {
        int o = i * 5;
        dest.set(data[o + 1], data[o + 2], data[o + 3], data[o + 4]);
    }

    public static class Transform {
        public final Vector4f pos = new Vector4f();
        public final Vector4f size = new Vector4f();
        public final Quaternionf rot = new Quaternionf();

        public Transform() {}

        public Transform(Vector3f pos, Vector3f size, Quaternionf rot) {
            this.pos.set(pos, 0);
            this.size.set(size, 0);
            this.rot.set(rot);
        }
    }

    /** Load-time only; discarded after {@link #sortAndCompact()}. */
    public static class DataKeyframe {
        public float time;
        public Vector4f vec;

        public DataKeyframe(float time, Vector4f vec) {
            this.time = time;
            this.vec = vec;
        }
    }
}
