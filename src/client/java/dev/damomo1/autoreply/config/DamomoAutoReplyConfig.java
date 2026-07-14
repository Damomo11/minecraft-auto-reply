package dev.damomo1.autoreply.config;

import java.util.ArrayList;
import java.util.List;

public final class DamomoAutoReplyConfig {
    public boolean enabled = true;
    public int replyDelayMs = 500;
    public List<ReplyRule> rules = new ArrayList<>();

    public void normalize() {
        replyDelayMs = Math.max(0, Math.min(5000, replyDelayMs));
        if (rules == null) {
            rules = new ArrayList<>();
        }
        rules.removeIf(rule -> rule == null);
        for (ReplyRule rule : rules) {
            if (rule.id == null || rule.id.isBlank()) {
                rule.id = java.util.UUID.randomUUID().toString();
            }
            rule.name = rule.name == null ? "" : rule.name;
            rule.matchMode = rule.matchMode == null ? RuleMatchMode.MESSAGE : rule.matchMode;
            rule.regex = rule.regex == null ? "" : rule.regex;
            rule.reply = rule.reply == null ? "" : rule.reply;
        }
    }
}
