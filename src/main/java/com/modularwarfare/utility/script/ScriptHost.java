package com.modularwarfare.utility.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
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
    
    // 检测 Java 版本
    private static final int JAVA_VERSION = getJavaVersion();
    private static final boolean USE_LEGACY_NASHORN = JAVA_VERSION <= 10;
    
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 8; // 默认假设为 Java 8
        }
    }
    
    // 创建 ClassFilter，兼容不同版本的 Nashorn
    private static Object createClassFilter() {
        try {
            if (USE_LEGACY_NASHORN) {
                // Java 8-10: 使用 jdk.nashorn.api.scripting.ClassFilter
                Class<?> classFilterClass = Class.forName("jdk.nashorn.api.scripting.ClassFilter");
                return Proxy.newProxyInstance(
                    ScriptHost.class.getClassLoader(),
                    new Class<?>[] { classFilterClass },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if (method.getName().equals("exposeToScripts") && args.length == 1) {
                                String tar = (String) args[0];
                                for (String str : allowList) {
                                    if (tar.startsWith(str)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                            return null;
                        }
                    }
                );
            } else {
                // Java 11+: 使用 org.openjdk.nashorn.api.scripting.ClassFilter
                Class<?> classFilterClass = Class.forName("org.openjdk.nashorn.api.scripting.ClassFilter");
                return Proxy.newProxyInstance(
                    ScriptHost.class.getClassLoader(),
                    new Class<?>[] { classFilterClass },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if (method.getName().equals("exposeToScripts") && args.length == 1) {
                                String tar = (String) args[0];
                                for (String str : allowList) {
                                    if (tar.startsWith(str)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                            return null;
                        }
                    }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static final Object classFilter = createClassFilter();
    
    // 创建 Nashorn 引擎，兼容不同版本
    private static ScriptEngine createNashornEngine() {
        try {
            if (USE_LEGACY_NASHORN) {
                // Java 8-10: 使用 jdk.nashorn.api.scripting.NashornScriptEngineFactory
                Class<?> factoryClass = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
                Object factory = factoryClass.newInstance();
                Method getScriptEngineMethod = factoryClass.getMethod("getScriptEngine", Class.forName("jdk.nashorn.api.scripting.ClassFilter"));
                return (ScriptEngine) getScriptEngineMethod.invoke(factory, classFilter);
            } else {
                // Java 11+: 使用 org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
                Class<?> factoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory");
                Object factory = factoryClass.newInstance();
                Method getScriptEngineMethod = factoryClass.getMethod("getScriptEngine", Class.forName("org.openjdk.nashorn.api.scripting.ClassFilter"));
                return (ScriptEngine) getScriptEngineMethod.invoke(factory, classFilter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
        ScriptEngine scriptEngine = createNashornEngine();
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
        ScriptEngine scriptEngine = createNashornEngine();
        String text = "";
        if (scriptEngine != null) {
            try {
                InputStream inputStream = ScriptHost.class.getClassLoader().getResourceAsStream("assets/modularwarfare/script/"+scriptLoc+".js");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,Charset.forName("UTF-8")));
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
        return Hashing.sha1().hashString(text, Charset.forName("UTF-8")).toString();
    }
}
