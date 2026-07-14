package dev.damomo1.autoreply;

import dev.damomo1.autoreply.ui.RulesScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class DamomoAutoReplyModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RulesScreen::new;
    }
}
