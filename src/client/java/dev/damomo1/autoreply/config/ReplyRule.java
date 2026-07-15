package dev.damomo1.autoreply.config;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ReplyRule {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public boolean enabled = true;
    public RuleMatchMode matchMode = RuleMatchMode.MESSAGE;
    public String regex = "";
    public List<String> replies = new ArrayList<>();

    private transient Pattern compiledPattern;
    private transient String compiledSource;

    public ReplyRule() {
        replies.add("");
    }

    public ReplyRule(ReplyRule source) {
        this.id = source.id;
        this.name = source.name;
        this.enabled = source.enabled;
        this.matchMode = source.matchMode;
        this.regex = source.regex;
        this.replies = new ArrayList<>(source.replies);
    }

    public Pattern pattern() throws PatternSyntaxException {
        if (compiledPattern == null || !regex.equals(compiledSource)) {
            compiledPattern = Pattern.compile(regex);
            compiledSource = regex;
        }
        return compiledPattern;
    }

    public boolean isValid() {
        if (regex.isBlank() || replies.stream().noneMatch(reply -> reply != null && !reply.isBlank())) {
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
