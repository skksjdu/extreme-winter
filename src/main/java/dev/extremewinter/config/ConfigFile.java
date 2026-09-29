package dev.extremewinter.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
            WinterConfig config = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), WinterConfig.class);
            if (config == null) throw new IllegalArgumentException("Configuration must be a JSON object");
            config.validate();
            return config;
        } catch (IOException | RuntimeException error) {
            // Preserve the user's file byte-for-byte. Never silently rewrite a malformed configuration.
            LOGGER.error("Could not load {}. Using safe defaults; file was not changed. {}", path, error.getMessage());
            return new WinterConfig();
        }
    }
}
