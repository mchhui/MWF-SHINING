package mchhui.modularmovements.coremod;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.Name("ModularMovementsPlugin")
public class ModularMovementsPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "mchhui.modularmovements.coremod.minecraft.EntityPlayerSP" ,"mchhui.modularmovements.coremod.minecraft.Entity"};
    }

    @Override
    public String getModContainerClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSetupClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAccessTransformerClass() {
        // TODO Auto-generated method stub
        return null;
    }

}
