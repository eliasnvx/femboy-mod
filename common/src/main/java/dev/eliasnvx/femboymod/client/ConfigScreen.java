package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.config.ClientConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.social.HeadpatConsent;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Client options screen, laid out like vanilla's option screens: a scrolling list, so every option stays
 * reachable at any window size and GUI scale. Server options live in config/femboymod-common.json.
 */
public final class ConfigScreen extends OptionsSubScreen {

    /** A full-width row in vanilla's options list. */
    private static final int ROW_WIDTH = 310;
    private static final int ROW_HEIGHT = 20;

    public ConfigScreen(@Nullable Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("config.femboymod.title"));
    }

    @Override
    protected void addOptions() {
        ClientConfig c = FemboyConfig.client();
        addToggle("show_others_cosmetics", c.showOthersCosmetics(), v -> FemboyConfig.setClient(FemboyConfig.client().withShowOthersCosmetics(v)));
        addToggle("drip_hud", c.dripHud(), v -> FemboyConfig.setClient(FemboyConfig.client().withDripHud(v)));
        list.addBig(CycleButton.<ClientConfig.HudCorner>builder(
                        corner -> Component.translatable("config.femboymod.drip_hud_corner." + corner.getSerializedName()), c.hudCorner())
                .withValues(ClientConfig.HudCorner.values())
                .create(0, 0, ROW_WIDTH, ROW_HEIGHT, Component.translatable("config.femboymod.drip_hud_corner"),
                        (button, value) -> FemboyConfig.setClient(FemboyConfig.client().withHudCorner(value))));
        addToggle("uwu_chat", c.uwuChat(), v -> FemboyConfig.setClient(FemboyConfig.client().withUwuChat(v)));
        addToggle("nya_sound", c.nyaSound(), v -> FemboyConfig.setClient(FemboyConfig.client().withNyaSound(v)));
        addToggle("rgb_animations", c.rgbAnimations(), v -> FemboyConfig.setClient(FemboyConfig.client().withRgbAnimations(v)));
        list.addBig(CycleButton.<ClientConfig.Physics>builder(
                        p -> Component.translatable("config.femboymod.physics." + p.getSerializedName()), c.physics())
                .withValues(ClientConfig.Physics.values())
                .create(0, 0, ROW_WIDTH, ROW_HEIGHT, Component.translatable("config.femboymod.physics"),
                        (button, value) -> FemboyConfig.setClient(FemboyConfig.client().withPhysics(value))));
        addToggle("reactive_ears_and_tails", c.reactiveCosmetics(), v -> FemboyConfig.setClient(FemboyConfig.client().withReactiveCosmetics(v)));
        addToggle("first_person_ear_tips", c.earTips(), v -> FemboyConfig.setClient(FemboyConfig.client().withEarTips(v)));
        list.addBig(CycleButton.<HeadpatConsent>builder(
                        consent -> Component.translatable("config.femboymod.headpats_from_players." + consent.getSerializedName()), c.headpats())
                .withValues(HeadpatConsent.values())
                .withTooltip(consent -> Tooltip.create(Component.translatable("config.femboymod.headpats_from_players.tooltip")))
                .create(0, 0, ROW_WIDTH, ROW_HEIGHT, Component.translatable("config.femboymod.headpats_from_players"),
                        (button, value) -> {
                            FemboyConfig.setClient(FemboyConfig.client().withHeadpats(value));
                            FemboyModClient.sendHeadpatConsent();
                        }));
    }

    private void addToggle(String key, boolean value, Consumer<Boolean> onChange) {
        list.addBig(CycleButton.onOffBuilder(value)
                .create(0, 0, ROW_WIDTH, ROW_HEIGHT, Component.translatable("config.femboymod." + key), (button, v) -> onChange.accept(v)));
    }
}
