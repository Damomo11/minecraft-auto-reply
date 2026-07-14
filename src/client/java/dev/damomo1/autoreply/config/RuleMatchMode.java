package dev.damomo1.autoreply.config;

public enum RuleMatchMode {
    MESSAGE,
    BUTTON_COMMAND;

    public RuleMatchMode next() {
        return this == MESSAGE ? BUTTON_COMMAND : MESSAGE;
    }
}
