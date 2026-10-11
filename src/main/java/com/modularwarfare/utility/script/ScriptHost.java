package com.modularwarfare.utility.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.common.hash.Hashing;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.WeaponFireMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ScriptHost {
    public static ScriptHost INSTANCE = new ScriptHost();
    public static HashMap<ResourceLocation, ScriptClient> clients = new HashMap<ResourceLocation, ScriptClient>();

    private static final ScriptAPI ScriptAPI = new ScriptAPI();
    private static final NBTSearcher NBTSearcher = new NBTSearcher();
    private static final String[] allowList = new String[] {
            //"java.lang.","mchhui.he.","net.minecraft."
            ArrayList.class.getName(), HashMap.class.getName(), WeaponFireMode.class.getName() };
    private static boolean isClassAllowed(String tar) {
        for (String str : allowList) {
            if (tar.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private static ScriptEngine createScriptEngine() {
        ClassLoader loader = ScriptHost.class.getClassLoader();
        try {
            Class<?> factoryClass;
            try {
                // Cleanroom supplies standalone Nashorn; Forge/Java 8 uses the legacy package.
                factoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory", true, loader);
            } catch (ClassNotFoundException e) {
                factoryClass = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory", true, loader);
            }
            Class<?> filterClass = Class.forName(factoryClass.getPackage().getName() + ".ClassFilter", true, loader);
            Object filter = Proxy.newProxyInstance(filterClass.getClassLoader(), new Class<?>[] { filterClass },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "exposeToScripts":
                                return isClassAllowed((String) args[0]);
                            case "equals":
                                return proxy == args[0];
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "toString":
                                return "MWF Nashorn ClassFilter";
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
            return (ScriptEngine) factoryClass.getMethod("getScriptEngine", filterClass)
                    .invoke(factoryClass.getDeclaredConstructor().newInstance(), filter);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create MWF Nashorn engine (Cleanroom: org.openjdk.nashorn; Forge: jdk.nashorn)", e);
        }
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
        ScriptEngine scriptEngine = createScriptEngine();
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
        ScriptEngine scriptEngine = createScriptEngine();
        String text = "";
        if (scriptEngine != null) {
            try {
                InputStream inputStream = ScriptHost.class.getClassLoader().getResourceAsStream("assets/modularwarfare/script/"+scriptLoc+".js");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,StandardCharsets.UTF_8));
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    text += temp+"\n";
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
                clients.put(new ResourceLocation(ModularWarfare.MOD_ID, "script/"+scriptLoc+".js"), new ScriptClient((Invocable) scriptEngine, genHash(text)));
            }
        }
    }

    public void reset() {
        clients.clear();
        initScriptFromResource("mwf/tooltip_main");
    }

    public static String genHash(String text) {
        return Hashing.sha1().hashString(text, StandardCharsets.UTF_8).toString();
    }
}
