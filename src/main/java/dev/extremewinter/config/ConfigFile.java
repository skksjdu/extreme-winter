package dev.extremewinter.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("extreme_winter/config");

    private ConfigFile() { }

    public static WinterConfig load(Path path) {
        try {
            if (!Files.exists(path)) {
                WinterConfig defaults = new WinterConfig();
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, GSON.toJson(defaults) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                return defaults;
            }
            String source = Files.readString(path, StandardCharsets.UTF_8);
            WinterConfig config = GSON.fromJson(source, WinterConfig.class);
            if (config == null) throw new IllegalArgumentException("Configuration must be a JSON object");
            JsonObject json = JsonParser.parseString(source).getAsJsonObject();
            if (!json.has("damageThreshold")) config.damageThreshold = config.slownessThreshold;
            if (!json.has("minimumFreezingDamage")) config.minimumFreezingDamage = Math.min(1, config.freezingDamage);
            config.validate();
            if (!json.has("configVersion")) {
                // Upgrade only known 1.0 defaults. Custom values and unknown fields are retained.
                migrateDefault(json, "baseLoss", 0.06, 0.32);
                migrateDefault(json, "weatherPenalty", 0.04, 0.18);
                migrateDefault(json, "nighttimePenalty", 0.03, 0.18);
                migrateDefault(json, "waterPenalty", 0.6, 1.4);
                migrateDefault(json, "damageIntervalSeconds", 5, 4);
                migrateDefault(json, "freezingDamage", 1, 6);
                migrateDefault(json, "maxSnowLayers", 3, 64);
                if (!json.has("damageThreshold")) json.addProperty("damageThreshold", config.damageThreshold);
                if (!json.has("minimumFreezingDamage")) json.addProperty("minimumFreezingDamage", config.minimumFreezingDamage);
                json.addProperty("configVersion", 2);
                config = GSON.fromJson(json, WinterConfig.class);
                config.validate();
                Path backup = path.resolveSibling(path.getFileName() + ".v1.bak");
                if (!Files.exists(backup)) Files.copy(path, backup);
                Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
                LOGGER.info("Upgraded 1.0 configuration; original saved at {}", backup);
            }
            return config;
        } catch (IOException | RuntimeException error) {
            // Preserve the user's file byte-for-byte. Never silently rewrite a malformed configuration.
            LOGGER.error("Could not load {}. Using safe defaults; file was not changed. {}", path, error.getMessage());
            return new WinterConfig();
        }
    }

    private static void migrateDefault(JsonObject json, String field, double before, double after) {
        if (json.has(field) && json.get(field).getAsDouble() == before) json.addProperty(field, after);
    }
}
