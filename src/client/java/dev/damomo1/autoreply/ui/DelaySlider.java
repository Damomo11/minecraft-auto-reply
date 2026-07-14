package dev.damomo1.autoreply.ui;

import dev.damomo1.autoreply.config.ConfigManager;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

final class DelaySlider extends AbstractSliderButton {
    private static final int MAX_DELAY = 5000;

    DelaySlider(int x, int y, int width) {
        super(x, y, width, 20, Component.empty(), ConfigManager.get().replyDelayMs / (double) MAX_DELAY);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int milliseconds = roundedDelay();
        setMessage(Component.translatable("damomo_auto_reply.delay", milliseconds));
    }

    @Override
    protected void applyValue() {
        ConfigManager.get().replyDelayMs = roundedDelay();
        ConfigManager.save();
    }

    private int roundedDelay() {
        return (int) Math.round(value * MAX_DELAY / 100.0) * 100;
    }
}
