package ui.swing;

import config.Config;
import config.ConfigSupport;

/**
 * Utility for loading configuration used by Swing entry points.
 */
final class SwingConfigSupport {
    /**
     * Utility class; not instantiable.
     */
    private SwingConfigSupport() {
    }

    /**
     * Loads the default configuration for Swing applications.
     *
     * @return loaded or fallback configuration
     */
    static Config loadConfig() {
        return ConfigSupport.loadDefaultConfig();
    }
}
