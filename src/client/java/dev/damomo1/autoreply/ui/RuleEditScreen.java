package dev.damomo1.autoreply.ui;

import dev.damomo1.autoreply.ReplyTemplate;
import dev.damomo1.autoreply.config.ConfigManager;
import dev.damomo1.autoreply.config.ReplyRule;
import dev.damomo1.autoreply.config.RuleMatchMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

public final class RuleEditScreen extends Screen {
    private static final int ACCENT = 0xFF71D6B2;
    private static final int MUTED = 0xFFA7B0B7;
    private static final int ERROR = 0xFFFF7B72;
    private final RulesScreen parent;
    private final ReplyRule working;
    private final boolean isNew;
    private EditBox nameField;
    private EditBox regexField;
    private Button repliesButton;
    private EditBox testField;
    private Button modeButton;
    private Button enabledButton;
    private Button saveButton;
    private Component testResult = Component.translatable("damomo_auto_reply.edit.test.empty");
    private int testResultColor = MUTED;
    private int panelX;
    private int panelWidth;

    RuleEditScreen(RulesScreen parent, ReplyRule source, boolean isNew) {
        super(Component.translatable(isNew ? "damomo_auto_reply.edit.title.new" : "damomo_auto_reply.edit.title"));
        this.parent = parent;
        this.working = new ReplyRule(source);
        this.isNew = isNew;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(540, width - 36);
        panelX = (width - panelWidth) / 2;
        int fieldX = panelX + 14;
        int fieldWidth = panelWidth - 28;

        nameField = createField(fieldX, 41, fieldWidth, 64, "damomo_auto_reply.edit.name.hint", working.name);
        regexField = createField(fieldX, 74, fieldWidth, 512, "damomo_auto_reply.edit.regex.hint", working.regex);
        repliesButton = addRenderableWidget(Button.builder(Component.empty(), button ->
                minecraft.gui.setScreen(new ReplyMessagesScreen(this, working.replies)))
                .bounds(fieldX, 107, fieldWidth, 20).build());
        updateRepliesButton();

        int toggleWidth = 100;
        modeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            working.matchMode = working.matchMode.next();
            updateModeButton();
            validate();
        }).bounds(fieldX, 143, fieldWidth - toggleWidth - 6, 20).build());
        enabledButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            working.enabled = !working.enabled;
            updateEnabledButton();
        }).bounds(fieldX + fieldWidth - toggleWidth, 143, toggleWidth, 20).build());
        testField = createField(fieldX, 179, fieldWidth, 512, testHintKey(), "");
        updateModeButton();
        updateEnabledButton();

        nameField.setResponder(value -> {
            working.name = value;
            validate();
        });
        regexField.setResponder(value -> {
            working.regex = value;
            validate();
        });
        testField.setResponder(value -> validate());

        int buttonY = height - 28;
        if (isNew) {
            addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.cancel"), button -> onClose())
                    .bounds(width / 2 - 103, buttonY, 100, 20).build());
            saveButton = addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.save"), button -> save())
                    .bounds(width / 2 + 3, buttonY, 100, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.delete").withStyle(ChatFormatting.RED), button -> confirmDelete())
                    .bounds(width / 2 - 154, buttonY, 98, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.cancel"), button -> onClose())
                    .bounds(width / 2 - 49, buttonY, 98, 20).build());
            saveButton = addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.save"), button -> save())
                    .bounds(width / 2 + 56, buttonY, 98, 20).build());
        }
        validate();
        setInitialFocus(nameField);
    }

    private EditBox createField(int x, int y, int width, int maxLength, String hintKey, String value) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.translatable(hintKey));
        field.setMaxLength(maxLength);
        field.setHint(Component.translatable(hintKey));
        field.setValue(value);
        return addRenderableWidget(field);
    }

    private void updateEnabledButton() {
        enabledButton.setMessage(Component.literal(working.enabled ? "● " : "○ ")
                .withStyle(working.enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                .append(Component.translatable(working.enabled
                        ? "damomo_auto_reply.rule.toggle.enabled" : "damomo_auto_reply.rule.toggle.disabled")));
    }

    private void updateRepliesButton() {
        long count = working.replies.stream().filter(reply -> reply != null && !reply.isBlank()).count();
        repliesButton.setMessage(Component.translatable("damomo_auto_reply.edit.replies.button", count));
    }

    private void updateModeButton() {
        boolean buttonMode = working.matchMode == RuleMatchMode.BUTTON_COMMAND;
        modeButton.setMessage(Component.translatable(buttonMode
                ? "damomo_auto_reply.edit.mode.button" : "damomo_auto_reply.edit.mode.message"));
        if (testField != null) {
            testField.setHint(Component.translatable(testHintKey()));
        }
    }

    private String testHintKey() {
        return working.matchMode == RuleMatchMode.BUTTON_COMMAND
                ? "damomo_auto_reply.edit.test.hint.button" : "damomo_auto_reply.edit.test.hint";
    }

    private void validate() {
        boolean baseValid = !working.regex.isBlank()
                && working.replies.stream().anyMatch(reply -> reply != null && !reply.isBlank());
        try {
            var pattern = working.pattern();
            saveButton.active = baseValid;
            if (testField.getValue().isBlank()) {
                testResult = Component.translatable("damomo_auto_reply.edit.test.empty");
                testResultColor = MUTED;
                return;
            }

            Matcher matcher = pattern.matcher(testField.getValue());
            if (!matcher.find()) {
                testResult = Component.translatable("damomo_auto_reply.edit.test.no_match");
                testResultColor = ERROR;
                return;
            }
            var previews = working.replies.stream()
                    .filter(reply -> reply != null && !reply.isBlank())
                    .map(reply -> ReplyTemplate.expand(reply, matcher))
                    .toList();
            testResult = Component.translatable("damomo_auto_reply.edit.test.match.multiple",
                    previews.size(), String.join(" → ", previews));
            testResultColor = ACCENT;
        } catch (PatternSyntaxException exception) {
            saveButton.active = false;
            testResult = Component.translatable("damomo_auto_reply.edit.invalid", readableRegexError(exception));
            testResultColor = ERROR;
        } catch (IllegalArgumentException exception) {
            saveButton.active = false;
            testResult = Component.literal(exception.getMessage());
            testResultColor = ERROR;
        }
    }

    private String readableRegexError(PatternSyntaxException exception) {
        String description = exception.getDescription();
        return description == null ? exception.getMessage() : description;
    }

    private void save() {
        working.name = nameField.getValue().strip();
        working.regex = regexField.getValue();
        if (isNew) {
            ConfigManager.get().rules.add(new ReplyRule(working));
        } else {
            for (int index = 0; index < ConfigManager.get().rules.size(); index++) {
                if (ConfigManager.get().rules.get(index).id.equals(working.id)) {
                    ConfigManager.get().rules.set(index, new ReplyRule(working));
                    break;
                }
            }
        }
        ConfigManager.save();
        parent.refreshRules();
        minecraft.gui.setScreen(parent);
    }

    private void confirmDelete() {
        String displayName = working.name.isBlank() ? working.regex : working.name;
        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ConfigManager.get().rules.removeIf(rule -> rule.id.equals(working.id));
                ConfigManager.save();
                parent.refreshRules();
                minecraft.gui.setScreen(parent);
            } else {
                minecraft.gui.setScreen(this);
            }
        }, Component.translatable("damomo_auto_reply.delete"), Component.translatable("damomo_auto_reply.confirm_delete", displayName),
                Component.translatable("damomo_auto_reply.delete"), CommonComponents.GUI_CANCEL));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(panelX, 27, panelX + panelWidth, Math.max(28, height - 37), 0xB8141B22);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 14, 0xFFFFFFFF);

        int labelX = panelX + 14;
        extractor.text(font, Component.translatable("damomo_auto_reply.edit.name"), labelX, 30, 0xFFE7ECEF, false);
        extractor.text(font, Component.translatable(working.matchMode == RuleMatchMode.BUTTON_COMMAND
                ? "damomo_auto_reply.edit.regex.button" : "damomo_auto_reply.edit.regex"), labelX, 63, 0xFFE7ECEF, false);
        extractor.text(font, Component.translatable("damomo_auto_reply.edit.replies"), labelX, 96, 0xFFE7ECEF, false);
        String help = Component.translatable(working.matchMode == RuleMatchMode.BUTTON_COMMAND
                ? "damomo_auto_reply.edit.help.button" : "damomo_auto_reply.edit.help").getString();
        extractor.text(font, trim(help, panelWidth - 28), labelX, 130, MUTED, false);
        extractor.text(font, Component.translatable("damomo_auto_reply.edit.test"), labelX, 168, 0xFFE7ECEF, false);

        int resultY = Math.min(203, height - 39);
        extractor.text(font, trim(testResult.getString(), panelWidth - 28), labelX, resultY, testResultColor, false);
    }

    private String trim(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
