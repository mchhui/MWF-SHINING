package mchhui.hegltf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.util.vector.Quaternion;

import net.minecraft.util.math.MathHelper;

public class DataAnimation {
    public ArrayList<DataKeyframe> posChannel = new ArrayList<DataAnimation.DataKeyframe>();
    public ArrayList<DataKeyframe> rotChannel = new ArrayList<DataAnimation.DataKeyframe>();
    public ArrayList<DataKeyframe> sizeChannel = new ArrayList<DataAnimation.DataKeyframe>();
    public float theata90 = (float)Math.toRadians(90);
    public String nodeName = null;

    public static final int STEP_TRANSLATION = 1;
    public static final int STEP_SCALE       = 2;
    public static final int STEP_ROTATION    = 4;

    public static Map<String, List<float[]>> currentStepRanges = null;

    public static boolean isStepInterval(String nodeName, float leftTime, int channelBit) {
        if (currentStepRanges == null || currentStepRanges.isEmpty()) return false;
        List<float[]> nodeRanges = currentStepRanges.get(nodeName);
        if (nodeRanges != null) {
            for (float[] range : nodeRanges) {
                if (leftTime >= range[0] && leftTime < range[1] && ((int) range[2] & channelBit) != 0) return true;
            }
        }
        List<float[]> globalRanges = currentStepRanges.get(null);
        if (globalRanges != null) {
            for (float[] range : globalRanges) {
                if (leftTime >= range[0] && leftTime < range[1] && ((int) range[2] & channelBit) != 0) return true;
            }
        }
        return false;
    }

    public Transform findTransform(float time,Vector3f pos,Vector3f size,Quaternionf rot) {
        Transform transform = new Transform(pos,size,rot);
        int left = 0;
        int right = 0;
        int mid;
        ArrayList<DataKeyframe> channel = posChannel;
        Vector4f vec = transform.pos;
        Vector4f vecTemp = new Vector4f();
        for (int i = 0; i < 2; i++) {
            final int channelBit = (i == 0) ? STEP_TRANSLATION : STEP_SCALE;
            if (i == 1) {
                channel = sizeChannel;
                vec = transform.size;
            }
            left = 0;
            right = channel.size() - 1;
            if (channel.size() > 0) {
                if (time <= channel.get(left).time) {
                    vec.set(channel.get(left).vec);
                } else if (time >= channel.get(right).time) {
                    vec.set(channel.get(right).vec);
                } else {
                    while (true) {
                        mid = (left + right) >> 1;
                        if (channel.get(mid).time <= time) {
                            left = mid;
                        } else {
                            right = mid;
                        }
                        if (left + 1 >= right) {
                            break;
                        }
                    }
                    float per = (time - channel.get(left).time) / (channel.get(right).time - channel.get(left).time);
                    if(per>1) {
                        per=1;
                    }
                    if (isStepInterval(this.nodeName, channel.get(left).time, channelBit)) {
                        per = 0;
                    }
                    vec.set(channel.get(left).vec);
                    vec.mul(1 - per);
                    vecTemp.set(channel.get(right).vec);
                    vecTemp.mul(per);
                    vec.add(vecTemp);
                }
            }
        }
        channel=rotChannel;
        left = 0;
        right = channel.size() - 1;
        if (channel.size() > 0) {
            if (time <= channel.get(left).time) {
                vec=channel.get(left).vec;
                transform.rot=new Quaternionf(vec.x,vec.y,vec.z,vec.w);
            } else if (time >= channel.get(right).time) {
                vec=channel.get(right).vec;
                transform.rot=new Quaternionf(vec.x,vec.y,vec.z,vec.w);
            } else {
                while (true) {
                    mid = (left + right) >> 1;
                    if (channel.get(mid).time <= time) {
                        left = mid;
                    } else {
                        right = mid;
                    }
                    if (left + 1 >= right) {
                        break;
                    }
                }
                float per = (time - channel.get(left).time) / (channel.get(right).time - channel.get(left).time);
                if(per>1) {
                    per=1;
                }
                if (isStepInterval(this.nodeName, channel.get(left).time, STEP_ROTATION)) {
                    per = 0;
                }
                vec=channel.get(left).vec;
                vecTemp=channel.get(right).vec;
                Quaternionf q0=new Quaternionf(vec.x,vec.y,vec.z,vec.w).normalize();
                Quaternionf q1=new Quaternionf(vecTemp.x,vecTemp.y,vecTemp.z,vecTemp.w).normalize();
                transform.rot=q0.slerp(q1, per);
            }
        }
        return transform;
    }

    public static class Transform {
        public Vector4f pos=new Vector4f();
        public Vector4f size=new Vector4f();
        public Quaternionf rot=new Quaternionf();
        
        public Transform(Vector3f pos,Vector3f size,Quaternionf rot) {
            this.pos.set(pos, 0);
            this.size.set(size, 0);
            this.rot.set(rot);
        }
    }

    public static class DataKeyframe {
        public float time;
        public Vector4f vec;

        public DataKeyframe(float time, Vector4f vec) {
            this.time = time;
            this.vec = vec;
        }
    }
}
