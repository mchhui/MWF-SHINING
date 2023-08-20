package mchhui.hegltf;

import java.util.ArrayList;

import org.joml.*;
import org.joml.Math;
import org.lwjgl.util.vector.Quaternion;

import net.minecraft.util.math.MathHelper;

public class DataAnimation {
    public ArrayList<DataKeyframe> posChannel = new ArrayList<DataAnimation.DataKeyframe>();
    public ArrayList<DataKeyframe> rotChannel = new ArrayList<DataAnimation.DataKeyframe>();
    public ArrayList<DataKeyframe> sizeChannel = new ArrayList<DataAnimation.DataKeyframe>();
    public float theata90 = (float)Math.toRadians(90);

    public Transform findTransform(float time,Vector3f pos,Vector3f size,Quaternionf rot) {
        Transform transform = new Transform(pos,size,rot);
        int left = 0;
        int right = 0;
        int mid;
        ArrayList<DataKeyframe> channel = posChannel;
        Vector4f vec = transform.pos;
        Vector4f vecTemp = new Vector4f();
        for (int i = 0; i < 2; i++) {
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
                vec=channel.get(left).vec;
                vecTemp=channel.get(right).vec;
                Quaternionf q0=new Quaternionf(vec.x,vec.y,vec.z,vec.w);
                Quaternionf q1=new Quaternionf(vecTemp.x,vecTemp.y,vecTemp.z,vecTemp.w);
                transform.rot=interpolationRot(q0,q1,per);
            }
        }
        return transform;
    }

    public Quaternionf interpolationRot(Quaternionf q0, Quaternionf q1, float t) {
        float theata = (float)Math.acos(q0.dot(q1));
        if (theata >= theata90 || -theata >= theata90) {
            q1.set(-q1.x, -q1.y, -q1.z, -q1.w);
            theata = q0.dot(q1);
        }
        float sinTheata = MathHelper.sin(theata);
        if (sinTheata == 0) {
            return new Quaternionf(q0.x + (q1.x - q0.x) * t, q0.y + (q1.y - q0.y) * t, q0.z + (q1.z - q0.z) * t,
                q0.w + (q1.w - q0.w) * t).normalize();
        }
        float c1 = (float)(MathHelper.sin(theata * (1 - t)) / sinTheata);
        float c2 = (float)(MathHelper.sin(theata * t) / sinTheata);
        return new Quaternionf(c1 * q0.x + c2 * q1.x, c1 * q0.y + c2 * q1.y, c1 * q0.z + c2 * q1.z,
            c1 * q0.w + c2 * q1.w).normalize();
    }

    public static class Transform {
        public Vector4f pos=new Vector4f();
        public Vector4f size=new Vector4f();
        public Quaternionf rot=new Quaternionf();
        
        public Transform(Vector3f pos,Vector3f size,Quaternionf rot) {
            this.pos.set(pos, 0);
            this.size.set(pos, 0);
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
