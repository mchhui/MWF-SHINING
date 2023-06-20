package mchhui.modularmovements.tactical.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakeModelBiped extends ModelBiped {
    public FakeModelBiped() {
        super(0.0F);
    }

    public FakeModelBiped(float modelSize) {
        super(modelSize, 0.0F, 64, 32);
    }

    public FakeModelBiped(float modelSize, float p_i1149_2_, int textureWidthIn, int textureHeightIn) {
        super(modelSize, p_i1149_2_, textureWidthIn, textureHeightIn);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scaleFactor, Entity entityIn) {
        // TODO Auto-generated method stub
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        ClientLitener.setRotationAngles(this, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                scaleFactor, entityIn);
    }
}
