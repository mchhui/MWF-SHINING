package com.modularwarfare.raycast.obb;

import com.modularwarfare.common.vector.Matrix4f;
import com.modularwarfare.common.vector.Vector3f;

import java.util.ArrayList;

public class OBBModelScene {
    public ArrayList<OBBModelBone> rootBones = new ArrayList<OBBModelBone>();
    private Matrix4f matrix = new Matrix4f();

    public void resetMatrix() {
        matrix = new Matrix4f();
    }

    public void translate(float x, float y, float z) {
        matrix.translate(new Vector3f(x, y, z));
    }

    public void translate(double x, double y, double z) {
        matrix.translate(new Vector3f(x, y, z));
    }

    public void rotate(float angle, float x, float y, float z) {
        matrix.rotate(angle, new Vector3f(x, y, z));
    }

    public void rotateDegree(float angle, float x, float y, float z) {
        matrix.rotate(angle / 180 * 3.14159f, new Vector3f(x, y, z));
    }

    public void scale(float x, float y, float z) {
        matrix.scale(new Vector3f(x, y, z));
    }

    public void computePose(OBBModelObject obbModelObject) {
        for (int i = 0; i < rootBones.size(); i++) {
            rootBones.get(i).computePose(obbModelObject, new Matrix4f(matrix));
        }
    }

    public void updatePose(OBBModelObject obbModelObject) {
        for (int i = 0; i < rootBones.size(); i++) {
            rootBones.get(i).updatePose(obbModelObject);
        }
    }
}
