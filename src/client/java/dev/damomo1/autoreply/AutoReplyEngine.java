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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public final class AutoReplyEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("damomo-auto-reply/engine");
    private static final long MIN_TRIGGER_INTERVAL_MS = 300L;
    private static final int MULTI_REPLY_GAP_MS = 350;
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

        var buttonCommands = allowButtonEvents ? ButtonCommandExtractor.extract(message) : java.util.List.<String>of();
        if (!buttonCommands.isEmpty() && triggerFirstButtonRule(client, buttonCommands, now)) {
            return;
        }

        String plainText = message.getString();
        for (ReplyRule rule : ConfigManager.get().rules) {
            if (!rule.enabled || !rule.isValid() || rule.matchMode != RuleMatchMode.MESSAGE) {
                continue;
            }
            try {
                Matcher matcher = rule.pattern().matcher(plainText);
                if (matcher.find() && triggerRule(client, rule, matcher, now)) {
                    return;
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Skipped invalid reply rule {}: {}", rule.id, exception.getMessage());
            }
        }
    }

    private static boolean triggerFirstButtonRule(Minecraft client, List<String> commands, long now) {
        for (ReplyRule rule : ConfigManager.get().rules) {
            if (!rule.enabled || !rule.isValid() || rule.matchMode != RuleMatchMode.BUTTON_COMMAND) {
                continue;
            }
            try {
                for (String command : commands) {
                    Matcher matcher = rule.pattern().matcher(command);
                    if (matcher.find() && triggerRule(client, rule, matcher, now)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Skipped invalid button reply rule {}: {}", rule.id, exception.getMessage());
            }
        }
        return false;
    }

    private static boolean triggerRule(Minecraft client, ReplyRule rule, Matcher matcher, long now) {
        List<String> expandedReplies = new ArrayList<>();
        for (String template : rule.replies) {
            String reply = ReplyTemplate.expand(template, matcher).strip();
            if (!reply.isEmpty()) {
                expandedReplies.add(truncate(reply, 256));
            }
        }
        if (expandedReplies.isEmpty()) {
            return false;
        }
        lastTriggerAt = now;
        for (int index = 0; index < expandedReplies.size(); index++) {
            scheduleReply(client, rule.id, expandedReplies.get(index),
                    ConfigManager.get().replyDelayMs + index * MULTI_REPLY_GAP_MS);
        }
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
