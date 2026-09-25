package dev.eliasnvx.femboymod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.network.OpenCosmeticsMenuPayload;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client entry point, called by each loader's client initializer after {@link FemboyMod#init()}. */
public final class FemboyModClient {

    public static final KeyMapping OPEN_COSMETICS = new KeyMapping(
            "key.femboymod.cosmetics", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.INVENTORY);

    /** Inventory screen is 176x166; the button sits right of the recipe book button. */
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int BUTTON_X = 126;
    private static final int BUTTON_Y = 61;
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;

    private FemboyModClient() {
    }

    public static void init() {
        FemboyMenus.COSMETICS.listen(type -> MenuScreenRegistry.registerScreenFactory(type, CosmeticsScreen::new));

        KeyMappingRegistry.register(OPEN_COSMETICS);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (OPEN_COSMETICS.consumeClick()) {
                if (minecraft.player != null && minecraft.gui.screen() == null) {
                    requestCosmeticsScreen();
                }
            }
        });

        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (screen instanceof InventoryScreen) {
                int left = (screen.width - INVENTORY_WIDTH) / 2;
                int top = (screen.height - INVENTORY_HEIGHT) / 2;
                access.addRenderableWidget(Button.builder(Component.translatable("gui.femboymod.cosmetics_button"),
                                button -> requestCosmeticsScreen())
                        .bounds(left + BUTTON_X, top + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .tooltip(Tooltip.create(Component.translatable("gui.femboymod.cosmetics_button.tooltip")))
                        .build());
            }
        });

        ClientTooltipEvent.ITEM.register(FemboyModClient::appendTooltip);

        FemboyApi common = FemboyApi.get();
        FemboyClientApi clientApi = () -> common;
        AddonLoader.initClient(clientApi);
        FemboyMod.LOGGER.info("femboymod client initialized");
    }

    private static void requestCosmeticsScreen() {
        NetworkManager.sendToServer(OpenCosmeticsMenuPayload.INSTANCE);
    }

    private static void appendTooltip(ItemStack stack, List<Component> lines, net.minecraft.world.item.Item.TooltipContext context,
                                      net.minecraft.world.item.TooltipFlag flag) {
        Cosmetic cosmetic = stack.get(FemboyComponents.COSMETIC.get());
        if (cosmetic != null) {
            lines.add(Component.translatable("tooltip.femboymod.slot",
                    Component.translatable(CosmeticSlotType.translationKey(cosmetic.slot()))).withStyle(ChatFormatting.GRAY));
        }
        Colorway colorway = stack.get(FemboyComponents.COLORWAY.get());
        if (colorway != null) {
            Component name = colorway.pattern()
                    .flatMap(holder -> holder.unwrapKey())
                    .map(key -> (Component) Component.translatable(key.identifier().toLanguageKey("colorway")))
                    .orElseGet(() -> Component.translatable("colorway.femboymod.solid"));
            lines.add(Component.translatable("tooltip.femboymod.colorway", name).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Minecraft instance accessor kept here so common code never touches it on a server. */
    static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
