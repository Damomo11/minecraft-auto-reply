package dev.damomo1.autoreply.ui;

import dev.damomo1.autoreply.config.ConfigManager;
import dev.damomo1.autoreply.config.ReplyRule;
import dev.damomo1.autoreply.config.RuleMatchMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class RulesScreen extends Screen {
    private static final int ACCENT = 0xFF71D6B2;
    private static final int MUTED = 0xFFA7B0B7;
    private static final int CARD = 0xC8182028;
    private static final int CARD_HOVER = 0xE0222D36;
    private final Screen parent;
    private RuleList ruleList;
    private Button masterButton;

    public RulesScreen(Screen parent) {
        super(Component.translatable("damomo_auto_reply.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int controlsWidth = Math.min(392, width - 40);
        int controlX = (width - controlsWidth) / 2;
        int half = (controlsWidth - 6) / 2;

        masterButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            ConfigManager.get().enabled = !ConfigManager.get().enabled;
            ConfigManager.save();
            updateMasterButton();
        }).bounds(controlX, 51, half, 20).build());
        updateMasterButton();
        addRenderableWidget(new DelaySlider(controlX + half + 6, 51, half));

        int listTop = 82;
        int listBottom = Math.max(listTop + 20, height - 39);
        ruleList = addRenderableWidget(new RuleList(minecraft, width, listBottom - listTop, listTop, 70));
        ruleList.reload();

        int buttonY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("damomo_auto_reply.add"), button ->
                minecraft.setScreen(new RuleEditScreen(this, new ReplyRule(), true)))
                .bounds(width / 2 - 103, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 + 3, buttonY, 100, 20).build());
    }

    private void updateMasterButton() {
        boolean enabled = ConfigManager.get().enabled;
        Component state = Component.translatable(enabled ? "damomo_auto_reply.master.on" : "damomo_auto_reply.master.off");
        masterButton.setMessage(Component.literal(enabled ? "● " : "○ ").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY).append(state));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 13, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("damomo_auto_reply.subtitle"), width / 2, 29, MUTED);

        long enabled = ConfigManager.get().rules.stream().filter(rule -> rule.enabled).count();
        graphics.drawString(font, Component.translatable("damomo_auto_reply.status.rules", ConfigManager.get().rules.size(), enabled),
                (width - font.width(Component.translatable("damomo_auto_reply.status.rules", ConfigManager.get().rules.size(), enabled))) / 2,
                73, MUTED, false);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        minecraft.setScreen(parent);
    }

    void refreshRules() {
        if (ruleList != null) {
            ruleList.reload();
        }
    }

    private void confirmDelete(ReplyRule rule) {
        String displayName = rule.name.isBlank() ? rule.regex : rule.name;
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ConfigManager.get().rules.removeIf(candidate -> candidate.id.equals(rule.id));
                ConfigManager.save();
                refreshRules();
            }
            minecraft.setScreen(this);
        }, Component.translatable("damomo_auto_reply.delete"), Component.translatable("damomo_auto_reply.confirm_delete", displayName),
                Component.translatable("damomo_auto_reply.delete"), CommonComponents.GUI_CANCEL));
    }

    private final class RuleList extends ObjectSelectionList<RuleEntry> {
        RuleList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            centerListVertically = false;
        }

        void reload() {
            clearEntries();
            for (ReplyRule rule : ConfigManager.get().rules) {
                addEntry(new RuleEntry(rule));
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(720, Math.max(260, getWidth() - 44));
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
                int centerY = getY() + getHeight() / 2 - 12;
                graphics.drawCenteredString(font, Component.translatable("damomo_auto_reply.empty.title").withStyle(ChatFormatting.BOLD),
                        width / 2, centerY, 0xFFE6ECEF);
                graphics.drawCenteredString(font, Component.translatable("damomo_auto_reply.empty.description"),
                        width / 2, centerY + 17, MUTED);
            }
        }
    }

    private final class RuleEntry extends ObjectSelectionList.Entry<RuleEntry> {
        private final ReplyRule rule;

        RuleEntry(ReplyRule rule) {
            this.rule = rule;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getX() + 3;
            int y = getY() + 3;
            int w = getWidth() - 6;
            int h = getHeight() - 6;
            graphics.fill(x, y, x + w, y + h, hovered ? CARD_HOVER : CARD);
            graphics.fill(x, y, x + 3, y + h, rule.enabled ? ACCENT : 0xFF59636B);

            int textX = x + 13;
            int rightControls = 112;
            int textWidth = Math.max(70, w - rightControls - 20);
            String displayName = rule.name.isBlank()
                    ? Component.translatable("damomo_auto_reply.rule.untitled").getString()
                    : rule.name;
            graphics.drawString(font, trim(displayName, textWidth), textX, y + 9,
                    rule.enabled ? 0xFFF2F6F7 : 0xFFB0B8BD, false);
            String mode = Component.translatable(rule.matchMode == RuleMatchMode.BUTTON_COMMAND
                    ? "damomo_auto_reply.rule.mode.button" : "damomo_auto_reply.rule.mode.message").getString();
            String regexSummary = "[" + mode + "] " + (rule.regex.isBlank() ? "—" : rule.regex);
            graphics.drawString(font, trim(regexSummary, textWidth), textX, y + 27,
                    0xFF91C9BA, false);
            var nonEmptyReplies = rule.replies.stream().filter(reply -> reply != null && !reply.isBlank()).toList();
            String replyPreview = nonEmptyReplies.isEmpty() ? "—" : nonEmptyReplies.getFirst();
            String replyCount = nonEmptyReplies.size() > 1 ? " (×" + nonEmptyReplies.size() + ")" : "";
            String arrowReply = "→ " + replyPreview + replyCount;
            graphics.drawString(font, trim(arrowReply, textWidth), textX, y + 44, MUTED, false);

            int deleteX = x + w - 118;
            int toggleX = x + w - 70;
            drawSmallButton(graphics, deleteX, y + 21, 42, 20, Component.translatable("damomo_auto_reply.delete"),
                    mouseX, mouseY, 0xAA5E2D32, 0xCC8B3E46);
            drawSmallButton(graphics, toggleX, y + 21, 62, 20,
                    Component.translatable(rule.enabled ? "damomo_auto_reply.rule.action.disable" : "damomo_auto_reply.rule.action.enable"),
                    mouseX, mouseY, rule.enabled ? 0xAA71343A : 0xAA285D50,
                    rule.enabled ? 0xCC9B454D : 0xCC397C6C);
        }

        private void drawSmallButton(GuiGraphics graphics, int x, int y, int width, int height, Component text,
                                     int mouseX, int mouseY, int color, int hoverColor) {
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            graphics.fill(x, y, x + width, y + height, hovered ? hoverColor : color);
            graphics.drawCenteredString(font, text, x + width / 2, y + 6, 0xFFF3F5F6);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
                return false;
            }
            int cardX = getX() + 3;
            int cardWidth = getWidth() - 6;
            int deleteX = cardX + cardWidth - 118;
            int toggleX = cardX + cardWidth - 70;
            int buttonY = getY() + 24;
            boolean onButtonRow = event.y() >= buttonY && event.y() < buttonY + 20;
            if (onButtonRow && event.x() >= toggleX && event.x() < toggleX + 62) {
                rule.enabled = !rule.enabled;
                ConfigManager.save();
                updateMasterButton();
            } else if (onButtonRow && event.x() >= deleteX && event.x() < deleteX + 42) {
                confirmDelete(rule);
            } else {
                minecraft.setScreen(new RuleEditScreen(RulesScreen.this, rule, false));
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(rule.name.isBlank() ? rule.regex : rule.name);
        }

        private String trim(String value, int maxWidth) {
            if (font.width(value) <= maxWidth) {
                return value;
            }
            return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
        }
    }
}
