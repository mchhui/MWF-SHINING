package mchhui.modularmovements.coremod;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@Deprecated
//@IFMLLoadingPlugin.SortingIndex(1001)
//@IFMLLoadingPlugin.Name("ModularMovementsPlugin")
public class ModularMovementsPlugin {
    
    
    public String[] getASMTransformerClass() {
        return new String[] { "mchhui.modularmovements.coremod.minecraft.EntityPlayerSP" ,"mchhui.modularmovements.coremod.minecraft.Entity"};
    }

    
    public String getModContainerClass() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public String getSetupClass() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public void injectData(Map<String, Object> data) {
        // TODO Auto-generated method stub

    }

    
    public String getAccessTransformerClass() {
        // TODO Auto-generated method stub
        return null;
    }

}
