package ui.swing;

import config.Config;
import config.ConfigSupport;

/**
 * Small facade for loading the configuration used by Swing entry points.
 */
final class SwingConfigSupport {
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
