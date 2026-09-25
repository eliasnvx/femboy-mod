package dev.eliasnvx.femboymod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.addon.AddonLoader;
import dev.eliasnvx.femboymod.api.internal.FemboyClientApiHolder;
import dev.eliasnvx.femboymod.client.render.BuiltinCosmeticRenderers;
import dev.eliasnvx.femboymod.client.chat.UwuChat;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.client.render.ClothingRackRenderer;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.eliasnvx.femboymod.client.render.entity.BugRenderer;
import dev.eliasnvx.femboymod.client.render.entity.CaffeinatedZombieRenderer;
import dev.eliasnvx.femboymod.client.render.entity.FashionCriticRenderer;
import dev.eliasnvx.femboymod.client.render.entity.HissyCatRenderer;
import dev.eliasnvx.femboymod.client.render.entity.PinkCreeperRenderer;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.network.OpenBackpackPayload;
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
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping(
            "key.femboymod.backpack", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, KeyMapping.Category.INVENTORY);

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

        FemboyMenus.BACKPACK.listen(type -> MenuScreenRegistry.registerScreenFactory(type, BackpackScreen::new));
        FemboyMenus.WARDROBE.listen(type -> MenuScreenRegistry.registerScreenFactory(type, WardrobeScreen::new));
        FemboyBlocks.CLOTHING_RACK_ENTITY.listen(type -> BlockEntityRendererRegistry.register(type, ClothingRackRenderer::new));
        ClientTickEvent.CLIENT_POST.register(NyaSound::tick);
        KeyMappingRegistry.register(OPEN_COSMETICS);
        KeyMappingRegistry.register(OPEN_BACKPACK);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (OPEN_COSMETICS.consumeClick()) {
                if (minecraft.player != null && minecraft.gui.screen() == null) {
                    requestCosmeticsScreen();
                }
            }
            while (OPEN_BACKPACK.consumeClick()) {
                if (minecraft.player != null && minecraft.gui.screen() == null) {
                    NetworkManager.sendToServer(OpenBackpackPayload.INSTANCE);
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
        ClientGuiEvent.RENDER_HUD.register(DripHud::render);

        FemboyConfig.loadClient();
        FemboyClientApiImpl clientApi = new FemboyClientApiImpl();
        FemboyClientApiHolder.install(clientApi);
        CosmeticModels.registerLayers();
        PinkCreeperRenderer.registerLayers();
        EntityRendererRegistry.register(FemboyEntities.PINK_CREEPER, PinkCreeperRenderer::new);
        BugRenderer.registerLayers();
        EntityRendererRegistry.register(FemboyEntities.BUG, BugRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.CAFFEINATED_ZOMBIE, CaffeinatedZombieRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.HISSY_CAT, HissyCatRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.FASHION_CRITIC, FashionCriticRenderer::new);
        BuiltinCosmeticRenderers.register(clientApi.cosmeticRenderers());
        clientApi.chatTransformers().register(UwuChat.ID, new UwuChat());
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new UwuChat.Loader(),
                Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "uwu_chat_rules"));
        ClientTickEvent.CLIENT_POST.register(GlowHostilesClient::tick);

        AddonLoader.initClient(clientApi);
        clientApi.freeze();
        FemboyMod.LOGGER.info("femboymod client initialized");
    }

    private static void requestCosmeticsScreen() {
        NetworkManager.sendToServer(OpenCosmeticsMenuPayload.INSTANCE);
    }

    private static void appendTooltip(ItemStack stack, List<Component> lines, net.minecraft.world.item.Item.TooltipContext context,
                                      net.minecraft.world.item.TooltipFlag flag) {
        Cosmetic cosmetic = stack.get(FemboyComponents.COSMETIC.get());
        if (cosmetic != null) {
            int drip = dripOf(stack);
            if (drip > 0) {
                lines.add(Component.translatable("tooltip.femboymod.drip", drip).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            lines.add(Component.translatable("tooltip.femboymod.slot",
                    Component.translatable(CosmeticSlotType.translationKey(cosmetic.slot()))).withStyle(ChatFormatting.GRAY));
        }
        Colorway colorway = dev.eliasnvx.femboymod.cosmetic.Colorways.effective(stack).orElse(null);
        if (colorway != null) {
            Component name = colorway.pattern()
                    .flatMap(holder -> holder.unwrapKey())
                    .map(key -> (Component) Component.translatable(key.identifier().toLanguageKey("colorway")))
                    .orElseGet(() -> Component.translatable("colorway.femboymod.solid"));
            lines.add(Component.translatable("tooltip.femboymod.colorway", name).withStyle(ChatFormatting.GRAY));
        }
    }

    private static int dripOf(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        return level.registryAccess().lookup(CosmeticStats.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(CosmeticStats.keyOf(stack.getItem())))
                .map(CosmeticStats::drip).orElse(0);
    }

    /** Minecraft instance accessor kept here so common code never touches it on a server. */
    static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
