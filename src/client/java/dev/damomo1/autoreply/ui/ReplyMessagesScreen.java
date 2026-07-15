package dev.damomo1.autoreply.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ReplyMessagesScreen extends Screen {
    private static final int MUTED = 0xFFA7B0B7;
    private static final int CARD = 0xC8182028;
    private static final int CARD_HOVER = 0xE0222D36;
    private final RuleEditScreen parent;
    private final List<String> target;
    private final List<String> messages;
    private MessageList messageList;

    ReplyMessagesScreen(RuleEditScreen parent, List<String> target) {
        super(Component.translatable("damomo_auto_reply.replies.title"));
        this.parent = parent;
        this.target = target;
        this.messages = new ArrayList<>();
        for (String message : target) {
            if (message != null && !message.isBlank()) {
                messages.add(message);
            }
        }
    }

    @Override
    protected void init() {
        int listTop = 49;
        int listBottom = Math.max(listTop + 20, height - 39);
        messageList = addRenderableWidget(new MessageList(minecraft, width, listBottom - listTop, listTop, 56));
        messageList.reload();

        int buttonY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.cancel"), button -> onClose())
                .bounds(width / 2 - 154, buttonY, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.replies.add"), button ->
                        minecraft.setScreen(new ReplyMessageEditScreen(this, -1, "")))
                .bounds(width / 2 - 49, buttonY, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.save"), button -> saveAndClose())
                .bounds(width / 2 + 56, buttonY, 98, 20).build());
    }

    void saveMessage(int index, String value) {
        if (index >= 0 && index < messages.size()) {
            messages.set(index, value);
        } else {
            messages.add(value);
        }
    }

    private void saveAndClose() {
        target.clear();
        target.addAll(messages);
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 13, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("damomo_auto_reply.replies.subtitle"),
                width / 2, 29, MUTED);
    }

    private final class MessageList extends ObjectSelectionList<MessageEntry> {
        MessageList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            centerListVertically = false;
        }

        void reload() {
            clearEntries();
            for (int index = 0; index < messages.size(); index++) {
                addEntry(new MessageEntry(index));
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(680, Math.max(250, getWidth() - 44));
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.renderWidget(graphics, mouseX, mouseY, delta);
            if (children().isEmpty()) {
                int centerY = getY() + getHeight() / 2 - 5;
                graphics.drawCenteredString(font, Component.translatable("damomo_auto_reply.replies.empty"),
                        width / 2, centerY, MUTED);
            }
        }
    }

    private final class MessageEntry extends ObjectSelectionList.Entry<MessageEntry> {
        private final int index;

        MessageEntry(int index) {
            this.index = index;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getX() + 3;
            int y = getY() + 3;
            int w = getWidth() - 6;
            int h = getHeight() - 6;
            graphics.fill(x, y, x + w, y + h, hovered ? CARD_HOVER : CARD);
            graphics.drawString(font, Component.translatable("damomo_auto_reply.replies.item", index + 1),
                    x + 12, y + 8, 0xFF71D6B2, false);
            graphics.drawString(font, trim(messages.get(index), w - 86), x + 12, y + 27, 0xFFE4E9EB, false);

            int deleteX = x + w - 56;
            boolean deleteHovered = mouseX >= deleteX && mouseX < deleteX + 48
                    && mouseY >= y + 14 && mouseY < y + 34;
            graphics.fill(deleteX, y + 14, deleteX + 48, y + 34, deleteHovered ? 0xCC8B3E46 : 0xAA5E2D32);
            graphics.drawCenteredString(font, Component.translatable("damomo_auto_reply.delete"),
                    deleteX + 24, y + 20, 0xFFF3F5F6);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
                return false;
            }
            int cardX = getX() + 3;
            int cardWidth = getWidth() - 6;
            int deleteX = cardX + cardWidth - 56;
            int buttonY = getY() + 17;
            if (event.x() >= deleteX && event.x() < deleteX + 48
                    && event.y() >= buttonY && event.y() < buttonY + 20) {
                messages.remove(index);
                messageList.reload();
            } else {
                minecraft.setScreen(new ReplyMessageEditScreen(ReplyMessagesScreen.this, index, messages.get(index)));
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("damomo_auto_reply.replies.item", index + 1)
                    .append(": ").append(messages.get(index));
        }

        private String trim(String value, int maxWidth) {
            if (font.width(value) <= maxWidth) {
                return value;
            }
            return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
        }
    }
}
