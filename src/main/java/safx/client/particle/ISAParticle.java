package safx.client.particle;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public interface ISAParticle {

	public Vec3d getPos();

	double getPosX();

	double getPosY();

	double getPosZ();
	
	public boolean shouldRemove();
	public void updateTick();
	
	public void doRender(BufferBuilder buffer, Entity entityIn, float partialTickTime, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ);
	   
	public AxisAlignedBB getRenderBoundingBox(float ptt, Entity viewEnt);
	
	public default boolean doNotSort() {
		return false;
	}
	
	public double getDepth();
	
	public void setDepth(double depth);
	public void setItemAttached();
}
