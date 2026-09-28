package dev.eliasnvx.femboymod.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
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
import dev.eliasnvx.femboymod.client.gui.CosmeticsPanel;
import dev.eliasnvx.femboymod.client.render.entity.BugRenderer;
import dev.eliasnvx.femboymod.client.render.entity.CaffeinatedZombieRenderer;
import dev.eliasnvx.femboymod.client.render.entity.FashionCriticRenderer;
import dev.eliasnvx.femboymod.client.render.entity.HissyCatRenderer;
import dev.eliasnvx.femboymod.client.render.entity.PinkCreeperRenderer;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client entry point, called by each loader's client initializer after {@link FemboyMod#init()}. */
public final class FemboyModClient {

    public static final KeyMapping OPEN_COSMETICS = new KeyMapping(
            "key.femboymod.cosmetics", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
            KeyMapping.CATEGORY_INVENTORY);

    /** Inventory screen is 176x166; the button sits right of the recipe book button. */
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping(
            "key.femboymod.backpack", InputConstants.Type.KEYSYM, InputConstants.KEY_B, KeyMapping.CATEGORY_INVENTORY);


    /** Emote wheel (SPEC v1.2). */
    public static final KeyMapping EMOTES = new KeyMapping(
            "key.femboymod.emotes", InputConstants.Type.KEYSYM, InputConstants.KEY_G, KeyMapping.CATEGORY_MULTIPLAYER);

    private FemboyModClient() {
    }

    public static void init() {
        FemboyMenus.COSMETICS.listen(type -> MenuRegistry.registerScreenFactory(type, CosmeticsScreen::new));

        FemboyMenus.BACKPACK.listen(type -> MenuRegistry.registerScreenFactory(type, BackpackScreen::new));
        FemboyMenus.WARDROBE.listen(type -> MenuRegistry.registerScreenFactory(type, WardrobeScreen::new));
        FemboyBlocks.CLOTHING_RACK_ENTITY.listen(type -> BlockEntityRendererRegistry.register(type, ClothingRackRenderer::new));
        ClientTickEvent.CLIENT_POST.register(NyaSound::tick);
        KeyMappingRegistry.register(OPEN_COSMETICS);
        KeyMappingRegistry.register(EMOTES);
        dev.eliasnvx.femboymod.emote.EmoteClientHooks.onShow = EmoteClient::show;
        ClientTickEvent.CLIENT_POST.register(EmoteClient::tick);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (EMOTES.consumeClick()) {
                if (minecraft.screen == null && minecraft.player != null) {
                    minecraft.setScreen(new EmoteScreen());
                }
            }
        });
        KeyMappingRegistry.register(OPEN_BACKPACK);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (OPEN_COSMETICS.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) {
                    requestCosmeticsScreen();
                }
            }
            while (OPEN_BACKPACK.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) {
                    NetworkManager.sendToServer(OpenBackpackPayload.INSTANCE);
                }
            }
        });

        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
                access.addRenderableWidget(new CosmeticsPanel((AbstractContainerScreen<?>) screen, FemboyModClient::requestCosmeticsScreen));
            }
        });

        ClientTooltipEvent.ITEM.register(FemboyModClient::appendTooltip);
        ItemTints.register();
        ClientGuiEvent.RENDER_HUD.register(DripHud::render);

        FemboyConfig.loadClient();
        FemboyClientApiImpl clientApi = new FemboyClientApiImpl();
        FemboyClientApiHolder.install(clientApi);
        CosmeticModels.registerLayers();
        PinkCreeperRenderer.registerLayers();
        EntityRendererRegistry.register(FemboyEntities.PINK_CREEPER, PinkCreeperRenderer::new);
        BugRenderer.registerLayers();
        EntityRendererRegistry.register(FemboyEntities.BUG, BugRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.SEAT, NoopRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.COSPLAYER, dev.eliasnvx.femboymod.client.render.entity.CosplayerRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.STRAY_CAT, dev.eliasnvx.femboymod.client.render.entity.StrayCatRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.CAFFEINATED_ZOMBIE, CaffeinatedZombieRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.HISSY_CAT, HissyCatRenderer::new);
        EntityRendererRegistry.register(FemboyEntities.FASHION_CRITIC, FashionCriticRenderer::new);
        BuiltinCosmeticRenderers.register(clientApi.cosmeticRenderers());
        clientApi.chatTransformers().register(UwuChat.ID, new UwuChat());
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new UwuChat.Loader(),
                new ResourceLocation(FemboyMod.MOD_ID, "uwu_chat_rules"));
        ClientTickEvent.CLIENT_POST.register(GlowHostilesClient::tick);
        ClientTickEvent.CLIENT_POST.register(PhotoMode::tick);
        dev.eliasnvx.femboymod.item.PhoneItem.onClientUse = PhotoMode::start;
        ClientTickEvent.CLIENT_POST.register(dev.eliasnvx.femboymod.client.render.HeadphonesLight::tick);

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
                    .map(key -> (Component) Component.translatable(key.location().toLanguageKey("colorway")))
                    .orElseGet(() -> Component.translatable("colorway.femboymod.solid"));
            lines.add(Component.translatable("tooltip.femboymod.colorway", name).withStyle(ChatFormatting.GRAY));
        }
    }

    private static int dripOf(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        return level.registryAccess().registry(CosmeticStats.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(CosmeticStats.keyOf(stack.getItem())))
                .map(CosmeticStats::drip).orElse(0);
    }

    /** Minecraft instance accessor kept here so common code never touches it on a server. */
    static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
