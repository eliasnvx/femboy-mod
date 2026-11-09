package dev.eliasnvx.femboymod.client;

import com.mojang.serialization.Codec;
import dev.eliasnvx.femboymod.config.ClientConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.social.HeadpatConsent;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Client options screen, laid out like vanilla's option screens: a scrolling list, so every option stays
 * reachable at any window size and GUI scale. Server options live in config/femboymod-common.json.
 *
 * <p>1.20.1's {@code OptionsSubScreen} has no list of its own: like {@code VideoSettingsScreen} this builds an
 * {@link OptionsList} between a 32 px header and footer, with Done below it. The list only takes
 * {@link OptionInstance}s, so each option is one here; it is not registered with vanilla's {@code options.txt}, the
 * value lives in our config.
 */
public final class ConfigScreen extends OptionsSubScreen {

    /** Vanilla's option screen layout (VideoSettingsScreen): list margins, row height, the Done button. */
    private static final int LIST_MARGIN = 32;
    private static final int ROW_HEIGHT = 25;
    private static final int DONE_WIDTH = 200;
    private static final int DONE_HEIGHT = 20;
    private static final int DONE_BOTTOM = 27;

    private OptionsList list;

    public ConfigScreen(@Nullable Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("config.femboymod.title"));
    }

    @Override
    protected void init() {
        list = new OptionsList(minecraft, width, height, LIST_MARGIN, height - LIST_MARGIN, ROW_HEIGHT);
        ClientConfig c = FemboyConfig.client();
        addToggle("show_others_cosmetics", c.showOthersCosmetics(), v -> FemboyConfig.setClient(FemboyConfig.client().withShowOthersCosmetics(v)));
        addToggle("drip_hud", c.dripHud(), v -> FemboyConfig.setClient(FemboyConfig.client().withDripHud(v)));
        list.addBig(cycle("drip_hud_corner", ClientConfig.HudCorner.values(), ClientConfig.HudCorner.CODEC, c.hudCorner(), null,
                value -> FemboyConfig.setClient(FemboyConfig.client().withHudCorner(value))));
        addToggle("uwu_chat", c.uwuChat(), v -> FemboyConfig.setClient(FemboyConfig.client().withUwuChat(v)));
        addToggle("nya_sound", c.nyaSound(), v -> FemboyConfig.setClient(FemboyConfig.client().withNyaSound(v)));
        addToggle("rgb_animations", c.rgbAnimations(), v -> FemboyConfig.setClient(FemboyConfig.client().withRgbAnimations(v)));
        list.addBig(cycle("physics", ClientConfig.Physics.values(), ClientConfig.Physics.CODEC, c.physics(), null,
                value -> FemboyConfig.setClient(FemboyConfig.client().withPhysics(value))));
        addToggle("reactive_ears_and_tails", c.reactiveCosmetics(), v -> FemboyConfig.setClient(FemboyConfig.client().withReactiveCosmetics(v)));
        addToggle("first_person_ear_tips", c.earTips(), v -> FemboyConfig.setClient(FemboyConfig.client().withEarTips(v)));
        list.addBig(cycle("headpats_from_players", HeadpatConsent.values(), HeadpatConsent.CODEC, c.headpats(),
                Component.translatable("config.femboymod.headpats_from_players.tooltip"),
                value -> {
                    FemboyConfig.setClient(FemboyConfig.client().withHeadpats(value));
                    FemboyModClient.sendHeadpatConsent();
                }));
        addWidget(list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - DONE_WIDTH / 2, height - DONE_BOTTOM, DONE_WIDTH, DONE_HEIGHT).build());
    }

    private void addToggle(String key, boolean value, Consumer<Boolean> onChange) {
        list.addBig(OptionInstance.createBoolean("config.femboymod." + key, value, onChange));
    }

    /**
     * A full-width cycle row; each value reads {@code config.femboymod.<key>.<value>}. Just the value, like vanilla's
     * {@code OptionInstance.forOptionEnum()}: the cycle button puts "caption: " in front itself.
     */
    private static <T extends Enum<T> & StringRepresentable> OptionInstance<T> cycle(String key, T[] values, Codec<T> codec,
            T initial, @Nullable Component tooltip, Consumer<T> onChange) {
        return new OptionInstance<>("config.femboymod." + key,
                tooltip == null ? OptionInstance.noTooltip() : OptionInstance.cachedConstantTooltip(tooltip),
                (caption, value) -> Component.translatable("config.femboymod." + key + "." + value.getSerializedName()),
                new OptionInstance.Enum<>(List.of(values), codec), initial, onChange);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        basicListRender(graphics, list, mouseX, mouseY, partialTick); // background, list, title, widgets
    }
}
