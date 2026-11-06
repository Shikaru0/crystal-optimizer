package name.modid.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class CrystalOptimizerConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("crystaloptimizer.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;

    public void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("enabled"))
                enabled = json.get("enabled").getAsBoolean();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);

        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }
}