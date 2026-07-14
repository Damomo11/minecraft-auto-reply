package dev.damomo1.autoreply.config;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ReplyRule {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public boolean enabled = true;
    public RuleMatchMode matchMode = RuleMatchMode.MESSAGE;
    public String regex = "";
    public String reply = "";

    private transient Pattern compiledPattern;
    private transient String compiledSource;

    public ReplyRule() {
    }

    public ReplyRule(ReplyRule source) {
        this.id = source.id;
        this.name = source.name;
        this.enabled = source.enabled;
        this.matchMode = source.matchMode;
        this.regex = source.regex;
        this.reply = source.reply;
    }

    public Pattern pattern() throws PatternSyntaxException {
        if (compiledPattern == null || !regex.equals(compiledSource)) {
            compiledPattern = Pattern.compile(regex);
            compiledSource = regex;
        }
        return compiledPattern;
    }

    public boolean isValid() {
        if (regex.isBlank() || reply.isBlank()) {
            return false;
        }
        try {
            pattern();
            return true;
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }
}
