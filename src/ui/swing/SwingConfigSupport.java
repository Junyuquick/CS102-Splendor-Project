package ui.swing;

import config.Config;
import config.ConfigSupport;

final class SwingConfigSupport {
    private SwingConfigSupport() {
    }

    static Config loadConfig() {
        return ConfigSupport.loadDefaultConfig();
    }
}
