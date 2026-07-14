package dev.damomo1.autoreply;

import com.mojang.authlib.GameProfile;
import dev.damomo1.autoreply.config.ConfigManager;
import dev.damomo1.autoreply.config.ReplyRule;
import dev.damomo1.autoreply.config.RuleMatchMode;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public final class AutoReplyEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("damomo-auto-reply/engine");
    private static final long MIN_TRIGGER_INTERVAL_MS = 300L;
    private static long lastTriggerAt;

    private AutoReplyEngine() {
    }

    public static void register() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receivedAt) -> {
            Minecraft client = Minecraft.getInstance();
            if (isLocalPlayer(client, sender)) {
                return;
            }
            accept(message, client, false);
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                accept(message, Minecraft.getInstance(), true);
            }
        });
    }

    private static boolean isLocalPlayer(Minecraft client, GameProfile sender) {
        return sender != null && client.player != null && sender.id().equals(client.player.getUUID());
    }

    private static void accept(Component message, Minecraft client, boolean allowButtonEvents) {
        if (!ConfigManager.get().enabled || client.player == null || client.getConnection() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastTriggerAt < MIN_TRIGGER_INTERVAL_MS) {
            return;
        }

        String plainText = message.getString();
        var buttonCommands = allowButtonEvents ? ButtonCommandExtractor.extract(message) : java.util.List.<String>of();
        for (ReplyRule rule : ConfigManager.get().rules) {
            if (!rule.enabled || !rule.isValid()) {
                continue;
            }
            try {
                if (rule.matchMode == RuleMatchMode.BUTTON_COMMAND) {
                    for (String command : buttonCommands) {
                        Matcher matcher = rule.pattern().matcher(command);
                        if (matcher.find() && trigger(client, rule, matcher, now)) {
                            return;
                        }
                    }
                } else {
                    Matcher matcher = rule.pattern().matcher(plainText);
                    if (matcher.find() && trigger(client, rule, matcher, now)) {
                        return;
                    }
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Skipped invalid reply rule {}: {}", rule.id, exception.getMessage());
            }
        }
    }

    private static boolean trigger(Minecraft client, ReplyRule rule, Matcher matcher, long now) {
        String reply = ReplyTemplate.expand(rule.reply, matcher).strip();
        if (reply.isEmpty()) {
            return false;
        }
        lastTriggerAt = now;
        scheduleReply(client, rule.id, truncate(reply, 256), ConfigManager.get().replyDelayMs);
        return true;
    }

    private static void scheduleReply(Minecraft originalClient, String ruleId, String reply, int delayMs) {
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> originalClient.execute(() -> {
            Minecraft client = Minecraft.getInstance();
            boolean ruleStillEnabled = ConfigManager.get().rules.stream()
                    .anyMatch(rule -> rule.id.equals(ruleId) && rule.enabled);
            if (!ConfigManager.get().enabled || !ruleStillEnabled || client.player == null || client.getConnection() == null) {
                return;
            }
            if (reply.startsWith("/") && reply.length() > 1) {
                client.getConnection().sendCommand(reply.substring(1));
            } else {
                client.getConnection().sendChat(reply);
            }
        }));
    }

    private static String truncate(String value, int maxCodePoints) {
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
}
