package safx.client.particle;

import java.util.Comparator;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import safx.SAConfig;
import safx.client.particle.list.ParticleList;
import safx.client.particle.list.ParticleList.ParticleListIterator;
import safx.client.render.GLStateSnapshot;

public class SAParticleManager {
	
    public static double interpPosX;
    public static double interpPosY;
    public static double interpPosZ;

	protected ParticleList<SAParticleSystem> list_systems = new ParticleList<>();
	protected ParticleList<ISAParticle> list = new ParticleList<>();
	protected ParticleList<ISAParticle> list_nosort = new ParticleList<>();
	protected ComparatorParticleDepth compare = new ComparatorParticleDepth();
	
	public void addEffect(ISAParticle effect)
    {
        if (effect == null) return;
        if(effect instanceof SAParticleSystem) {
        	list_systems.add((SAParticleSystem) effect);
        } else {
        	if (effect.doNotSort()) {
        		list_nosort.add(effect);
        	} else {
        		list.add(effect);
        	}
        }
    }
	
	public void tickParticles() {
		if(Minecraft.getMinecraft().isGamePaused()) return;
		
		Entity viewEnt = Minecraft.getMinecraft().getRenderViewEntity();
		
		Iterator<SAParticleSystem> sysit = list_systems.iterator();
		while(sysit.hasNext()) {
			SAParticleSystem p = sysit.next();
			
			p.updateTick();
			if(p.shouldRemove()) {
				sysit.remove();
			}
		}
		
		ParticleListIterator<ISAParticle> it = list.iterator();
		while(it.hasNext()) {
			ISAParticle p = it.next();
			
			p.updateTick();
			if(p.shouldRemove()) {
				it.remove();
			} else {
				if(viewEnt!=null) {
					p.setDepth(this.distanceToPlane(viewEnt, p.getPos()));
				}
			}
		}
		
		Iterator<ISAParticle> it2 = list_nosort.iterator();
		while(it2.hasNext()) {
			ISAParticle p = it2.next();
			
			p.updateTick();
			if(p.shouldRemove()) {
				it2.remove();
			}
		}
		//list.debugPrintList();
		
		if(SAConfig.cl_sortPassesPerTick>0) {
			this.doSorting();
		}
	}

	public void doSorting() {
		this.list.doBubbleSort(SAConfig.cl_sortPassesPerTick, compare);
	}
	
	/*public ParticleList<ISAParticle> getList() {
		return list;
	}*/

	/**
	 * 
	 * @param playerIn renderViewEntity
	 * @param partialTick
	 */
	public void renderParticles(Entity playerIn, float partialTicks)
    {
        float f1 = MathHelper.cos(playerIn.rotationYaw * 0.017453292F);
        float f2 = MathHelper.sin(playerIn.rotationYaw * 0.017453292F);
        float f3 = -f2 * MathHelper.sin(playerIn.rotationPitch * 0.017453292F);
        float f4 = f1 * MathHelper.sin(playerIn.rotationPitch * 0.017453292F);
        float f5 = MathHelper.cos(playerIn.rotationPitch * 0.017453292F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        
        interpPosX = playerIn.lastTickPosX + (playerIn.posX - playerIn.lastTickPosX) * (double)partialTicks;
        interpPosY = playerIn.lastTickPosY + (playerIn.posY - playerIn.lastTickPosY) * (double)partialTicks;
        interpPosZ = playerIn.lastTickPosZ + (playerIn.posZ - playerIn.lastTickPosZ) * (double)partialTicks;
        
        /*Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        Frustum frust = new Frustum();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
        frust.setPosition(d0, d1, d2);*/
        
        GlStateManager.disableCull();
        this.list.forEach(p -> {	
        	//if(frust.isBoundingBoxInFrustum(p.getRenderBoundingBox(partialTicks, entity)))
        	p.doRender(bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
        });
        
        this.list_nosort.forEach(p -> {	
        	//if(frust.isBoundingBoxInFrustum(p.getRenderBoundingBox(partialTicks, entity)))
        	p.doRender(bufferbuilder, playerIn, partialTicks, f1, f5, f2, f3, f4);
        });
        GlStateManager.color(1f, 1f, 1f, 1f);
        //GlStateManager.enableCull();


    }

	
	public double distanceToPlane(Entity viewEntity, Vec3d pos) {
		//Formula from: http://geomalgorithms.com/a04-_planes.html
		Vec3d n = viewEntity.getLookVec();
		double dot1 = -n.dotProduct(pos.subtract(viewEntity.getPositionVector()));
		double dot2 = n.dotProduct(n);
		double f = dot1/dot2;
		
		Vec3d pos2 = pos.add(n.scale(f));
		return pos.squareDistanceTo(pos2);
	}
	
	public static class ComparatorParticleDepth implements Comparator<ISAParticle> {

		@Override
		public int compare(ISAParticle p1, ISAParticle p2) {
			if(p1.doNotSort() && p2.doNotSort()) {
				return 0;
			}
			//Entity view = Minecraft.getMinecraft().getRenderViewEntity();
			//if(view!=null) {
				double dist1=p1.getDepth();
				double dist2=p2.getDepth();
				//double dist1 = p1.getPos().squareDistanceTo(view.posX, view.posY, view.posZ);
				//double dist2 = p2.getPos().squareDistanceTo(view.posX, view.posY, view.posZ);
				
				if(dist1<dist2) {
					return 1;
				} else if(dist1>dist2) {
					return -1;
				} else {
					return 0;
				}
			//}
			//return 0;
		}
		

	}
}
