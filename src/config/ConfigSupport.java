package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigSupport {
    private ConfigSupport() {
    }

    public static Config loadDefaultConfig() {
        Path configPath = locateConfigFile();
        try {
            return new ConfigLoader().load(configPath);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load " + configPath + ". Falling back to defaults. Reason: " + e.getMessage());
        }

        Path here = Path.of(".");
        return new Config(
                15, 10, 3, 1, 2, 4, 3, 4,
                3, 4, 5,
                4, 5, 7, 5,
                3, 2, 2, 1,
                here, here, here, here,
                here, here, here
        );
    }

    public static Path locateConfigFile() {
        Path[] candidates = new Path[]{
                Path.of("config.properties"),
                Path.of("SplendorProject", "config.properties")
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        return candidates[0];
    }
}
