package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.config.ClientConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Client options screen (SPEC §10). Server options live in config/femboymod-common.json. */
public final class ConfigScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int ROW = 24;
    private static final int TOP = 40;
    private final @Nullable Screen parent;

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("config.femboymod.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - WIDTH) / 2;
        int y = TOP;
        ClientConfig c = FemboyConfig.client();
        addToggle(x, y, "show_others_cosmetics", c.showOthersCosmetics(), v -> FemboyConfig.setClient(FemboyConfig.client().withShowOthersCosmetics(v)));
        addToggle(x, y += ROW, "drip_hud", c.dripHud(), v -> FemboyConfig.setClient(FemboyConfig.client().withDripHud(v)));
        addToggle(x, y += ROW, "uwu_chat", c.uwuChat(), v -> FemboyConfig.setClient(FemboyConfig.client().withUwuChat(v)));
        addToggle(x, y += ROW, "nya_sound", c.nyaSound(), v -> FemboyConfig.setClient(FemboyConfig.client().withNyaSound(v)));
        addToggle(x, y += ROW, "rgb_animations", c.rgbAnimations(), v -> FemboyConfig.setClient(FemboyConfig.client().withRgbAnimations(v)));
        addRenderableWidget(CycleButton.<ClientConfig.Physics>builder(
                        p -> Component.translatable("config.femboymod.physics." + p.getSerializedName()))
                .withValues(ClientConfig.Physics.values())
                .withInitialValue(c.physics())
                .create(x, y += ROW, WIDTH, 20, Component.translatable("config.femboymod.physics"),
                        (button, value) -> FemboyConfig.setClient(FemboyConfig.client().withPhysics(value))));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds((width - WIDTH) / 2, y + ROW + 8, WIDTH, 20).build());
    }

    private void addToggle(int x, int y, String key, boolean value, java.util.function.Consumer<Boolean> onChange) {
        addRenderableWidget(CycleButton.onOffBuilder(value)
                .create(x, y, WIDTH, 20, Component.translatable("config.femboymod." + key), (button, v) -> onChange.accept(v)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TOP / 2 - 4, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
