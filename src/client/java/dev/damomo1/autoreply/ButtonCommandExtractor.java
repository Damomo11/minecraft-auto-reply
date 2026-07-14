package dev.damomo1.autoreply;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ButtonCommandExtractor {
    private ButtonCommandExtractor() {
    }

    public static List<String> extract(Component message) {
        Set<String> commands = new LinkedHashSet<>();
        message.visit((style, text) -> {
            ClickEvent event = style.getClickEvent();
            if (event instanceof ClickEvent.RunCommand runCommand) {
                addIfPresent(commands, runCommand.command());
            } else if (event instanceof ClickEvent.SuggestCommand suggestCommand) {
                addIfPresent(commands, suggestCommand.command());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return new ArrayList<>(commands);
    }

    private static void addIfPresent(Set<String> commands, String command) {
        if (command != null && !command.isEmpty()) {
            commands.add(command);
        }
    }
}
