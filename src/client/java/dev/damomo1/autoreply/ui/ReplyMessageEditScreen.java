package dev.damomo1.autoreply.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ReplyMessageEditScreen extends Screen {
    private static final int MUTED = 0xFFA7B0B7;
    private final ReplyMessagesScreen parent;
    private final int index;
    private final String initialValue;
    private EditBox messageField;
    private Button saveButton;

    ReplyMessageEditScreen(ReplyMessagesScreen parent, int index, String initialValue) {
        super(Component.translatable(index < 0
                ? "damomo_auto_reply.replies.edit.new" : "damomo_auto_reply.replies.edit.title"));
        this.parent = parent;
        this.index = index;
        this.initialValue = initialValue;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(540, width - 36);
        int fieldX = (width - panelWidth) / 2 + 14;
        int fieldWidth = panelWidth - 28;
        messageField = addRenderableWidget(new EditBox(font, fieldX, 75, fieldWidth, 20,
                Component.translatable("damomo_auto_reply.edit.reply.hint")));
        messageField.setMaxLength(256);
        messageField.setHint(Component.translatable("damomo_auto_reply.edit.reply.hint"));
        messageField.setValue(initialValue);

        int buttonY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.cancel"), button -> onClose())
                .bounds(width / 2 - 103, buttonY, 100, 20).build());
        saveButton = addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.save"), button -> save())
                .bounds(width / 2 + 3, buttonY, 100, 20).build());
        messageField.setResponder(value -> saveButton.active = !value.isBlank());
        saveButton.active = !initialValue.isBlank();
        setInitialFocus(messageField);
    }

    private void save() {
        parent.saveMessage(index, messageField.getValue());
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(540, width - 36);
        int panelX = (width - panelWidth) / 2;
        extractor.fill(panelX, 34, panelX + panelWidth, Math.max(35, height - 37), 0xB8141B22);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 14, 0xFFFFFFFF);
        extractor.text(font, Component.translatable("damomo_auto_reply.edit.reply"), panelX + 14, 62,
                0xFFE7ECEF, false);
        extractor.text(font, Component.translatable("damomo_auto_reply.replies.capture_help"), panelX + 14, 103,
                MUTED, false);
    }
}
