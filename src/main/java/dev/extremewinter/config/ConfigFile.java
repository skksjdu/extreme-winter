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
                var defaults = new WinterConfig();
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, GSON.toJson(defaults) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                return defaults;
            }
            String source = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(source).getAsJsonObject();
            int version = json.has("configVersion") ? json.get("configVersion").getAsInt() : 1;
            if (version < 1) throw new IllegalArgumentException("Unsupported configVersion");
            // Validate source-version defaults before changing the user's bytes.
            JsonObject validation = GSON.toJsonTree(version < 3 ? legacyDefaults() : new WinterConfig()).getAsJsonObject();
            json.entrySet().forEach(entry -> validation.add(entry.getKey(), entry.getValue()));
            if (!json.has("damageThreshold") && version < 3) validation.addProperty("damageThreshold", validation.get("slownessThreshold").getAsDouble());
            if (!json.has("minimumFreezingDamage")) validation.addProperty("minimumFreezingDamage", Math.min(version < 3 ? 1 : .5, validation.get("freezingDamage").getAsDouble()));
            if (!json.has("weatherMode") && json.has("persistentWeather") && !json.get("persistentWeather").getAsBoolean()) {
                validation.addProperty("weatherMode", "vanilla");
                if (version < 3) json.addProperty("weatherMode", "vanilla");
            }
            GSON.fromJson(validation, WinterConfig.class).validate();
            if (version < 3) {
                if (version == 1) {
                    migrateDefault(json, "baseLoss", .06, .32);
                    migrateDefault(json, "weatherPenalty", .04, .18);
                    migrateDefault(json, "nighttimePenalty", .03, .18);
                    migrateDefault(json, "waterPenalty", .6, 1.4);
                    migrateDefault(json, "damageIntervalSeconds", 5, 4);
                    migrateDefault(json, "freezingDamage", 1, 6);
                    migrateDefault(json, "maxSnowLayers", 3, 0);
                }
                migrateDefault(json, "baseLoss", .32, .04);
                migrateDefault(json, "weatherPenalty", .18, .015);
                migrateDefault(json, "nighttimePenalty", .18, .01);
                migrateDefault(json, "waterPenalty", 1.4, .20);
                migrateDefault(json, "shelteredRecoveryRate", .03, .30);
                migrateDefault(json, "snowIntervalTicks", 20, 80);
                migrateDefault(json, "freezeIntervalTicks", 40, 200);
                migrateDefault(json, "campfireExposureSeconds", 120, 600);
                migrateDefault(json, "soulCampfireExposureSeconds", 180, 900);
                if (matches(json, "minTemperature", 0) && matches(json, "coldThreshold", 70)
                        && matches(json, "slownessThreshold", 40) && matches(json, "fatigueThreshold", 20)
                        && matches(json, "damageThreshold", 40)) {
                    json.addProperty("slownessThreshold", 25);
                    json.addProperty("fatigueThreshold", 10);
                    json.addProperty("damageThreshold", 25);
                } else {
                    for (String field : new String[]{"slownessThreshold", "fatigueThreshold", "damageThreshold"})
                        if (!json.has(field)) json.add(field, validation.get(field));
                }
                if (matches(json, "freezingDamage", 6) && matches(json, "minimumFreezingDamage", 1)
                        && matches(json, "damageIntervalSeconds", 4)) {
                    json.addProperty("freezingDamage", 1);
                    json.addProperty("minimumFreezingDamage", .5);
                    json.addProperty("damageIntervalSeconds", 10);
                } else {
                    for (String field : new String[]{"freezingDamage", "minimumFreezingDamage", "damageIntervalSeconds"})
                        if (!json.has(field)) json.add(field, validation.get(field));
                }
                // Explicit zero snow caps, modded climate and unknown keys survive migration.
                json.addProperty("configVersion", 3);
                var migrated = GSON.fromJson(json, WinterConfig.class);
                migrated.validate();
                Path backup = path.resolveSibling(path.getFileName() + ".v" + version + ".bak");
                if (Files.exists(backup) && !Files.readString(backup, StandardCharsets.UTF_8).equals(source))
                    throw new IllegalArgumentException("Migration backup already contains another original: " + backup);
                if (!Files.exists(backup)) Files.copy(path, backup);
                GSON.toJsonTree(migrated).getAsJsonObject().entrySet().forEach(entry -> {
                    if (!json.has(entry.getKey())) json.add(entry.getKey(), entry.getValue());
                });
                Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
                LOGGER.info("Upgraded v{} configuration to v3; original saved at {}", version, backup);
                return migrated;
            }
            var config = GSON.fromJson(validation, WinterConfig.class);
            config.validate();
            return config;
        } catch (IOException | RuntimeException error) {
            LOGGER.error("Could not load {}. Using safe defaults; file was not changed. {}", path, error.getMessage());
            return new WinterConfig();
        }
    }
    private static WinterConfig legacyDefaults() {
        var c = new WinterConfig();
        c.slownessThreshold = 40; c.fatigueThreshold = 20; c.damageThreshold = 40;
        c.baseLoss = .32; c.weatherPenalty = .18; c.nighttimePenalty = .18; c.waterPenalty = 1.4;
        c.minimumFreezingDamage = 1; c.freezingDamage = 6; c.damageIntervalSeconds = 4;
        c.shelteredRecoveryRate = .03; c.snowIntervalTicks = 20; c.freezeIntervalTicks = 40;
        c.maxSnowLayers = 0; c.campfireExposureSeconds = 120; c.soulCampfireExposureSeconds = 180;
        return c;
    }
    private static boolean matches(JsonObject json, String field, double value) {
        return !json.has(field) || json.get(field).getAsDouble() == value;
    }
    private static void migrateDefault(JsonObject json, String field, double before, double after) {
        if (json.has(field) && json.get(field).getAsDouble() == before) json.addProperty(field, after);
    }
}
