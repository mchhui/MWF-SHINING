package com.modularwarfare.loader.blenderani;

import java.util.ArrayList;

import net.minecraft.client.renderer.GlStateManager;

/**
 * @author Hueihuea
 * */
public class BlenderBone {
    public ArrayList<BlenderKeyFrame> keyframes = new ArrayList<BlenderKeyFrame>();
    public transient BlenderKeyFrame currentFrame;
    public transient BlenderKeyFrame nextFrame;
    public transient double frameTime;
    public static float x;
    public static float y;
    public static float z;

    public void updateAnimation(double frame) {
        frameTime = frame;
        currentFrame = null;
        nextFrame = null;
        for (BlenderKeyFrame keyframe : keyframes) {
            if (frame >= keyframe.frame) {
                currentFrame = keyframe;
            } else {
                nextFrame = keyframe;
                break;
            }
        }
    }

    public void setupAnimation() {
        if (currentFrame != null) {
            //reset to zero(欢迎来到坐标轴转转转酒吧,这是超级无敌复杂变换)
            BlenderKeyFrame firstFrame = keyframes.get(0);
            GlStateManager.translate(-firstFrame.position.x, -firstFrame.position.z, firstFrame.position.y);

            GlStateManager.translate(firstFrame.position.x, firstFrame.position.z, -firstFrame.position.y);

            GlStateManager.rotate(-firstFrame.rotation.z + x, 0, 1, 0);

            GlStateManager.rotate(+firstFrame.rotation.z + x, 0, 1, 0);
            GlStateManager.rotate(-firstFrame.rotation.y + y, 0, 0, -1);
            GlStateManager.rotate(-firstFrame.rotation.z + x, 0, 1, 0);

            GlStateManager.rotate(+firstFrame.rotation.z + x, 0, 1, 0);
            GlStateManager.rotate(+firstFrame.rotation.y + y, 0, 0, -1);
            GlStateManager.rotate(-firstFrame.rotation.x + z, 1, 0, 0);
            GlStateManager.rotate(-firstFrame.rotation.y + y, 0, 0, -1);
            GlStateManager.rotate(-firstFrame.rotation.z + x, 0, 1, 0);

            GlStateManager.rotate(+firstFrame.rotation.z + x, 0, 1, 0);
            GlStateManager.rotate(+firstFrame.rotation.y + y, 0, 0, -1);
            GlStateManager.rotate(+firstFrame.rotation.x + z, 1, 0, 0);
            GlStateManager.scale(1 / firstFrame.scale.x, 1 / firstFrame.scale.z, -1 / firstFrame.scale.y);
            GlStateManager.rotate(-firstFrame.rotation.x + z, 1, 0, 0);
            GlStateManager.rotate(-firstFrame.rotation.y + y, 0, 0, -1);
            GlStateManager.rotate(-firstFrame.rotation.z + x, 0, 1, 0);

            GlStateManager.translate(-firstFrame.position.x, -firstFrame.position.z, firstFrame.position.y);

            //set the current frame animation
            if (nextFrame == null) {
                GlStateManager.translate(firstFrame.position.x, firstFrame.position.z, -firstFrame.position.y);
                GlStateManager.rotate(+firstFrame.rotation.z + x, 0, 1, 0);
                GlStateManager.rotate(+firstFrame.rotation.y + y, 0, 0, -1);
                GlStateManager.rotate(+firstFrame.rotation.x + z, 1, 0, 0);
                GlStateManager.scale(currentFrame.scale.x, currentFrame.scale.z, -currentFrame.scale.y);

                float rotX = currentFrame.rotation.x;
                float rotY = currentFrame.rotation.y;
                float rotZ = currentFrame.rotation.z;
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-rotX, 1, 0, 0);
                GlStateManager.rotate(rotZ, 0, 1, 0);
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-rotX, 1, 0, 0);
                GlStateManager.rotate(-rotZ, 0, 1, 0);
                GlStateManager.rotate(rotY, 0, 0, -1);
                GlStateManager.rotate(rotZ, 0, 1, 0);
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-rotX, 1, 0, 0);
                GlStateManager.rotate(-rotZ, 0, 1, 0);
                GlStateManager.rotate(-rotY, 0, 0, -1);
                GlStateManager.translate(currentFrame.position.x / currentFrame.scale.x * firstFrame.scale.x,
                        currentFrame.position.z / currentFrame.scale.z * firstFrame.scale.z,
                        -currentFrame.position.y / -currentFrame.scale.y * -firstFrame.scale.y);
                GlStateManager.rotate(rotY, 0, 0, -1);
                GlStateManager.rotate(rotZ, 0, 1, 0);
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-firstFrame.rotation.x + z, 1, 0, 0);
                GlStateManager.rotate(-firstFrame.rotation.y + y, 0, 0, -1);
                GlStateManager.rotate(-firstFrame.rotation.z + x, 0, 1, 0);
                GlStateManager.translate(-firstFrame.position.x, -firstFrame.position.z, firstFrame.position.y);
            } else {
                float per = (float) ((frameTime - currentFrame.frame) / (nextFrame.frame - currentFrame.frame));
                float sizeX = currentFrame.scale.x + (nextFrame.scale.x - currentFrame.scale.x) * per;
                float sizeY = currentFrame.scale.y + (nextFrame.scale.y - currentFrame.scale.y) * per;
                float sizeZ = currentFrame.scale.z + (nextFrame.scale.z - currentFrame.scale.z) * per;
                GlStateManager.translate(firstFrame.position.x, firstFrame.position.z, -firstFrame.position.y);
                GlStateManager.rotate(+firstFrame.rotation.z + x, 0, 1, 0);
                GlStateManager.rotate(+firstFrame.rotation.y + y, 0, 0, -1);
                GlStateManager.rotate(+firstFrame.rotation.x + z, 1, 0, 0);
                GlStateManager.scale(sizeX, sizeZ, -sizeY);

                float rotX = currentFrame.rotation.x + (nextFrame.rotation.x - currentFrame.rotation.x) * per;
                float rotY = currentFrame.rotation.y + (nextFrame.rotation.y - currentFrame.rotation.y) * per;
                float rotZ = currentFrame.rotation.z + (nextFrame.rotation.z - currentFrame.rotation.z) * per;
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-rotX, 1, 0, 0);
                GlStateManager.rotate(rotZ, 0, 1, 0);
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-rotX, 1, 0, 0);
                GlStateManager.rotate(-rotZ, 0, 1, 0);
                GlStateManager.rotate(rotY, 0, 0, -1);
                GlStateManager.rotate(rotZ, 0, 1, 0);
                GlStateManager.rotate(rotX, 1, 0, 0);

                float posX = currentFrame.position.x + (nextFrame.position.x - currentFrame.position.x) * per;
                float posY = currentFrame.position.y + (nextFrame.position.y - currentFrame.position.y) * per;
                float posZ = currentFrame.position.z + (nextFrame.position.z - currentFrame.position.z) * per;

                GlStateManager.rotate(-rotX, 1, 0, 0);
                GlStateManager.rotate(-rotZ, 0, 1, 0);
                GlStateManager.rotate(-rotY, 0, 0, -1);
                GlStateManager.translate(posX / sizeX * firstFrame.scale.x, posZ / sizeZ * firstFrame.scale.z,
                        -posY / -sizeY * -firstFrame.scale.y);
                GlStateManager.rotate(rotY, 0, 0, -1);
                GlStateManager.rotate(rotZ, 0, 1, 0);
                GlStateManager.rotate(rotX, 1, 0, 0);

                GlStateManager.rotate(-firstFrame.rotation.x + z, 1, 0, 0);
                GlStateManager.rotate(-firstFrame.rotation.y + y, 0, 0, -1);
                GlStateManager.rotate(-firstFrame.rotation.z + x, 0, 1, 0);
                GlStateManager.translate(-firstFrame.position.x, -firstFrame.position.z, firstFrame.position.y);
            }
        }
    }
}
