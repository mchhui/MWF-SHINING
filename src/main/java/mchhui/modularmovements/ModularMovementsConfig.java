package mchhui.modularmovements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.*;

public class ModularMovementsConfig {


    public Lean lean = new Lean();
    public Slide slide = new Slide();

    public Sit sit = new Sit();
    public Crawl crawl = new Crawl();

    public Cooldown cooldown = new Cooldown();
    public String version = ModularMovements.MOD_VERSION;

    public ModularMovementsConfig(File configFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (configFile.exists()) {
                JsonReader jsonReader = new JsonReader(new FileReader(configFile));
                ModularMovementsConfig config = gson.fromJson(jsonReader, ModularMovementsConfig.class);
                System.out.println("Comparing version " + config.version + " to " + ModularMovements.MOD_VERSION);
                if (config.version == null || !config.version.matches(ModularMovements.MOD_VERSION)) {
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8")) {
                        gson.toJson(this, writer);
                    }
                    ModularMovements.CONFIG = this;
                } else {
                    ModularMovements.CONFIG = config;
                }
            } else {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8")) {
                    gson.toJson(this, writer);
                }
                ModularMovements.CONFIG = this;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Lean {
        public boolean autoHold = false;
        public boolean mouseCorrection = true;
        public boolean withGunsOnly = false;
    }

    public static class Slide {
        public boolean enable = true;
        public float maxForce = 1.0f;
    }

    public static class Sit {
        public boolean autoHold = true;
    }

    public static class Crawl {
        public boolean blockView = false;
        public float blockAngle = 190;
    }

    public static class Cooldown {
        public float sitCooldown = 0.75f;
        public float crawlCooldown = 0.75f;
        public float leanCooldown = 0f;
    }
}