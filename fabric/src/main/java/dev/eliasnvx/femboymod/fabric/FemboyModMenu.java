package dev.eliasnvx.femboymod.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.eliasnvx.femboymod.client.ConfigScreen;

/** Mod Menu integration (optional dependency): the config button opens our client options. */
public final class FemboyModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
