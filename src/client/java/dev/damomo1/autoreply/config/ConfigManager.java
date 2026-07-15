package dev.damomo1.autoreply.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("damomo-auto-reply/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("damomo-auto-reply.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("momo-chat.json");
    private static DamomoAutoReplyConfig config = new DamomoAutoReplyConfig();

    private ConfigManager() {
    }

    public static DamomoAutoReplyConfig get() {
        return config;
    }

    public static void load() {
        Path sourcePath = Files.exists(CONFIG_PATH) ? CONFIG_PATH : LEGACY_CONFIG_PATH;
        if (!Files.exists(sourcePath)) {
            config = new DamomoAutoReplyConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(sourcePath)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            boolean migratedReplies = migrateLegacyReplies(rootElement);
            DamomoAutoReplyConfig loaded = GSON.fromJson(rootElement, DamomoAutoReplyConfig.class);
            config = loaded == null ? new DamomoAutoReplyConfig() : loaded;
            config.normalize();
            if (sourcePath.equals(LEGACY_CONFIG_PATH)) {
                LOGGER.info("Migrating legacy configuration from {} to {}", LEGACY_CONFIG_PATH, CONFIG_PATH);
            } else if (migratedReplies) {
                LOGGER.info("Migrating {} to the multi-message reply format", CONFIG_PATH);
            }
            if (sourcePath.equals(LEGACY_CONFIG_PATH) || migratedReplies) {
                save();
            }
        } catch (Exception exception) {
            LOGGER.error("Could not load {}, keeping a fresh configuration", sourcePath, exception);
            config = new DamomoAutoReplyConfig();
        }
    }

    private static boolean migrateLegacyReplies(JsonElement rootElement) {
        if (!rootElement.isJsonObject()) {
            return false;
        }
        JsonObject root = rootElement.getAsJsonObject();
        JsonArray rules = root.has("rules") && root.get("rules").isJsonArray()
                ? root.getAsJsonArray("rules") : null;
        if (rules == null) {
            return false;
        }
        boolean migrated = false;
        for (JsonElement element : rules) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject rule = element.getAsJsonObject();
            if (!rule.has("replies") && rule.has("reply") && rule.get("reply").isJsonPrimitive()) {
                JsonArray replies = new JsonArray();
                replies.add(rule.get("reply").getAsString());
                rule.add("replies", replies);
                migrated = true;
            }
            migrated |= rule.remove("reply") != null;
        }
        return migrated;
    }

    public static void save() {
        config.normalize();
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save {}", CONFIG_PATH, exception);
        }
    }
}
