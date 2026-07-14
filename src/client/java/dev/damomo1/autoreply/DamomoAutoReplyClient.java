package dev.damomo1.autoreply;

import dev.damomo1.autoreply.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

public final class DamomoAutoReplyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        AutoReplyEngine.register();
    }
}
