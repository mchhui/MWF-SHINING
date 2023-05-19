package com.modularwarfare.script;

import com.google.common.hash.Hashing;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.WeaponFireMode;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScriptHost {
    private static final ScriptAPI ScriptAPI = new ScriptAPI();
    private static final NBTSearcher NBTSearcher = new NBTSearcher();
    private static final String[] allowList = new String[]{
            //"java.lang.","mchhui.he.","net.minecraft."
            ArrayList.class.getName(), HashMap.class.getName(), WeaponFireMode.class.getName()};
    private static final ClassFilter classFilter = new ClassFilter() {

        @Override
        public boolean exposeToScripts(String tar) {
            for (String str : allowList) {
                if (tar.startsWith(str)) {
                    return true;
                }
            }
            // TODO Auto-generated method stub
            return false;
        }

    };
    public static ScriptHost INSTANCE = new ScriptHost();
    public static HashMap<ResourceLocation, ScriptClient> clients = new HashMap<ResourceLocation, ScriptClient>();

    public static String genHash(String text) {
        return Hashing.sha1().hashString(text, Charset.forName("UTF-8")).toString();
    }

    public boolean callScript(ResourceLocation scriptLoc, ItemStack stack, List<String> tooltip, String function) {
        if (clients.containsKey(scriptLoc)) {
            try {
                clients.get(scriptLoc).getInvocable().invokeFunction(function, stack, tooltip);
            } catch (NoSuchMethodException | ScriptException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void initScript(ResourceLocation scriptLoc, String text) {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        ScriptEngine scriptEngine = factory.getScriptEngine(classFilter);
        if (scriptEngine != null) {
            try {
                scriptEngine.eval("var WeaponFireMode=Java.type('" + WeaponFireMode.class.getName() + "');");
                scriptEngine.eval(text);
                scriptEngine.put("NBTSearcher", NBTSearcher);
                scriptEngine.put("ScriptAPI", ScriptAPI);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            if (scriptEngine instanceof Invocable) {
                clients.put(scriptLoc, new ScriptClient((Invocable) scriptEngine, genHash(text)));
            }
        }
    }

    public void initScriptFromResource(String scriptLoc) {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        ScriptEngine scriptEngine = factory.getScriptEngine(classFilter);
        String text = "";
        if (scriptEngine != null) {
            try {
                InputStream inputStream = ScriptHost.class.getClassLoader().getResourceAsStream("assets/modularwarfare/script/" + scriptLoc + ".js");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    text += temp;
                }
                bufferedReader.close();
                scriptEngine.eval("var WeaponFireMode=Java.type('" + WeaponFireMode.class.getName() + "');");
                scriptEngine.eval(text);
                scriptEngine.put("NBTSearcher", NBTSearcher);
                scriptEngine.put("ScriptAPI", ScriptAPI);
            } catch (ScriptException | IOException e) {
                e.printStackTrace();
            }
            if (scriptEngine instanceof Invocable) {
                clients.put(new ResourceLocation(ModularWarfare.MOD_ID, "script/" + scriptLoc + ".js"), new ScriptClient((Invocable) scriptEngine, genHash(text)));
            }
        }
    }

    public void reset() {
        clients.clear();
        initScriptFromResource("mwf/tooltip_main");
    }

    public static class ScriptClient {
        public Invocable invocable;
        public String hash;

        public ScriptClient(Invocable invocable, String hash) {
            this.invocable = invocable;
            this.hash = hash;
        }

        public Invocable getInvocable() {
            return this.invocable;
        }

        public String getHash() {
            return this.hash;
        }
    }
}
