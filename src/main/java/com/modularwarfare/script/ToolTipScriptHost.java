package com.modularwarfare.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.modularwarfare.common.guns.WeaponFireMode;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ToolTipScriptHost {
    public static HashMap<ResourceLocation, Invocable> context=new HashMap<ResourceLocation, Invocable>();
    
    private static final ScriptAPI ScriptAPI=new ScriptAPI();
    private static final NBTSearcher NBTSearcher=new NBTSearcher();
    private static final String[] allowList=new String[] {
            //"java.lang.","mchhui.he.","net.minecraft."
            ArrayList.class.getName(),
            HashMap.class.getName(),
            WeaponFireMode.class.getName()
    };
    private static final ClassFilter classFilter=new ClassFilter() {

        @Override
        public boolean exposeToScripts(String tar) {
            for(String str:allowList) {
                if(tar.startsWith(str)) {
                    return true;
                }
            }
            // TODO Auto-generated method stub
            return false;
        }
        
    };
    
    public static boolean call(ResourceLocation scriptLoc,ItemStack stack, List<String> tooltip) {
        if(context.containsKey(scriptLoc)) {
            try {
                context.get(scriptLoc).invokeFunction("updateTooltip",stack,tooltip);
            } catch (NoSuchMethodException | ScriptException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }else {
            return false;
        }
        return true;
    }
    
    public static void callScript(ResourceLocation scriptLoc,ItemStack stack, List<String> tooltip) {
        if(call(scriptLoc, stack, tooltip)) {
            return;
        }
        ScriptEngineManager engineManager = new ScriptEngineManager();
        NashornScriptEngineFactory factory=new NashornScriptEngineFactory();
        ScriptEngine scriptEngine = factory.getScriptEngine(classFilter);
        if (scriptEngine != null) {
            try {
                IResource scriptFile = Minecraft.getMinecraft().getResourceManager().getResource(scriptLoc);
                scriptEngine.eval("var WeaponFireMode=Java.type('"+WeaponFireMode.class.getName()+"');");
                scriptEngine.eval(new InputStreamReader(scriptFile.getInputStream()));
                scriptEngine.put("NBTSearcher", NBTSearcher);
                scriptEngine.put("ScriptAPI", ScriptAPI);
            } catch (ScriptException | IOException e) {
                e.printStackTrace();
            }
            if (scriptEngine instanceof Invocable) {
                context.put(scriptLoc, (Invocable) scriptEngine);
            }
            call(scriptLoc, stack, tooltip);
        }
    }
}
